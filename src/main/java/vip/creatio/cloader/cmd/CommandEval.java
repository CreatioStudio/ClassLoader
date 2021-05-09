package vip.creatio.cloader.cmd;

import com.mojang.brigadier.tree.LiteralCommandNode;
import vip.creatio.basic.cmd.*;
import vip.creatio.basic.tools.FormatMsgManager;
import vip.creatio.cloader.bukkit.CLoader;
import vip.creatio.cloader.ccl.FileManager;
import jdk.jshell.*;
import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;
import jdk.jshell.spi.ExecutionEnv;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.stream.Stream;

/**
 * The encapsulation of JDK 9 JShell
 */
public final class CommandEval {

    private static final FormatMsgManager msg = CLoader.getMsgSender();

    private static final String name = "eval";
    private static final String description = "Evaluating Java code in game directly.";
    private static final List<String> aliases = Arrays.asList("jshell", "repl");

    private static final Map<String, String> PREFIX = Map.of("%prefix%", msg.getSingle("MAIN.EVAL.PREFIX"));

    private static JShell shell = provideJShell();
    private static Map<CommandSender, LinkedList<String>> HISTORY = new HashMap<>();
    private static LinkedList<String> GLOBAL_HISTORY = new LinkedList<>();

    //No default constructor
    private CommandEval() {}

    private static void sendStatic(String path, CommandSender sender, String... args) {
        msg.sendStatic(PREFIX, sender, path, args);
    }

    private static void outputStatic(String path, CommandSender sender, String... args) {
        msg.sendStatic(PREFIX, sender, "MAIN.EVAL.OUTPUT", msg.getList(path, args));
    }

    private static void output(CommandSender sender, String msg) {
        CommandEval.msg.sendStatic(PREFIX, sender, "MAIN.EVAL.OUTPUT", msg);
    }

    // Only send if sender is a player
    private static void sendRestricted(String path, CommandSender sender, String... args) {
        if (sender instanceof Player) {
            msg.sendStatic(PREFIX, sender, path, args);
        }
    }

    private static void addToHistory(CommandSender sender, String input) {
        LinkedList<String> personal = HISTORY.computeIfAbsent(sender, k -> new LinkedList<>());

        if (personal.size() > 75) personal.removeLast();
        if (GLOBAL_HISTORY.size() > 150) GLOBAL_HISTORY.removeLast();

        personal.addFirst(input);
        GLOBAL_HISTORY.addFirst(input);
    }

    public static void register(CommandRegister register) {
        LiteralCommandNode<?> node = Argument.of(name)


                .then(Argument.of("/vars").executes(c -> {
                    Stream<VarSnippet> vars = shell.variables();
                    if (vars.findAny().isEmpty()) sendRestricted("MAIN.EVAL.LIST_EMPTY", c.getSender());
                    else vars.forEach(v -> sendRestricted("MAIN.EVAL.LIST_SINGLE", c.getSender(), v.source()));
                    addToHistory(c.getSender(), "/vars");
                }))


                .then(Argument.of("/methods").executes(c -> {
                    Stream<MethodSnippet> mths = shell.methods();
                    if (mths.findAny().isEmpty()) sendRestricted("MAIN.EVAL.LIST_EMPTY", c.getSender());
                    else mths.forEach(m -> sendRestricted("MAIN.EVAL.LIST_SINGLE", c.getSender(), m.source()));
                    addToHistory(c.getSender(), "/methods");
                }))


                .then(Argument.of("/imports").executes(c -> {
                    Stream<ImportSnippet> imports = shell.imports();
                    if (imports.findAny().isEmpty()) sendRestricted("MAIN.EVAL.LIST_EMPTY", c.getSender());
                    else imports.forEach(m -> sendRestricted("MAIN.EVAL.LIST_SINGLE", c.getSender(), m.source()));
                    addToHistory(c.getSender(), "/imports");
                }))


                .then(Argument.of("/types").executes(c -> {
                    Stream<TypeDeclSnippet> types = shell.types();
                    if (types.findAny().isEmpty()) sendRestricted("MAIN.EVAL.LIST_EMPTY", c.getSender());
                    else types.forEach(m -> sendRestricted("MAIN.EVAL.LIST_SINGLE", c.getSender(), m.source()));
                    addToHistory(c.getSender(), "/types");
                }))


                .then(Argument.of("/history")
                        .then(Argument.of("global").executes(c -> {
                            for (String s : GLOBAL_HISTORY) {
                                sendRestricted("MAIN.EVAL.LIST_SINGLE", c.getSender(), s);
                            }
                        }))
                        .executes(c -> {
                    List<String> personal = HISTORY.get(c.getSender());
                    if (personal != null && personal.size() > 0) {
                        for (String s : personal) {
                            sendRestricted("MAIN.EVAL.LIST_SINGLE", c.getSender(), s);
                        }
                    } else {
                        outputStatic("MAIN.EVAL.LIST_EMPTY", c.getSender());
                    }})
                        .fallbacksInvalidInput(FallbackAction.DEFAULT_INVALID_INPUT))


                .then(Argument.of("/drop"))
                .then(Argument.of("/edit"))
                .then(Argument.of("/help"))
                .then(Argument.of("/reload"))
                .then(Argument.of("/reset"))
                .then(Argument.of("/set"))
                .then(Argument.of("/!"))


                .then(Argument.arg("Java Expr", ArgumentTypes.ofGreedyString()).executes(c -> {
                    return execute(c.getSender(), c.getLabel(), c.getArgument("Java Expr", String.class), false);
                }).fallbacksFailure(c -> {
                    throw FallbackAction.NO_MESSAGE;
                }))


                .fallbacksInvalidInput((c, e) -> sendStatic("COMMAND.USAGE.EVAL", c.getSender(), c.getLabel()))
                .fallbacksNoPermission(c -> sendStatic("MAIN.NO_PERM", c))
                .restricted(true)
                .requires(c -> (c instanceof ConsoleCommandSender)
                        || ((c instanceof Player || c instanceof BlockCommandSender)
                        && c.isOp() && CLoader.getInstance().allowPlayersExecuteDos()))
                .build();
        register.register(node, description, aliases);
    }

    private static void reloadJShell() {
        if (shell != null) shell.close();
        shell = provideJShell();
    }

    private static JShell provideJShell() {
        //JShell shell = JShell.builder().executionEngine(new EngineProvider(), null).build();
        JShell shell = JShell.builder().build();
        Arrays.stream(FileManager.getDependencies()).map(File::getAbsolutePath).forEach(shell::addToClasspath);
        return shell;
    }

    private static boolean execute(CommandSender sender, String label, String cmdLine, boolean recursive) {
        if (!recursive) sendRestricted("MAIN.EVAL.INPUT_FIRST", sender, cmdLine);

        String code = cmdLine;
        if (cmdLine.charAt(cmdLine.length() - 1) != ';') code += ';';

        List<SnippetEvent> events = shell.eval(code);
        for (SnippetEvent e : events) {
            Snippet snippet = e.snippet();
            switch (e.status()) {
                case VALID:
                    if (snippet != null)
                        output(sender, snippet.source());

                    if (!recursive) addToHistory(sender, cmdLine);

                    break;
                case REJECTED:
                    msg.sendStatic(sender, "MAIN.EVAL.ERR");
                    shell.diagnostics(e.snippet()).filter(Diag::isError).forEach(d -> {
                        msg.send(sender, "      &e" + d.getMessage(Locale.ROOT));
                        if (snippet != null)
                            msg.send(sender, "      &7" + snippet.source());
                        if (d.getEndPosition() - d.getStartPosition() > 1) msg.send(sender, "      &7"
                                + " ".repeat((int) (d.getStartPosition() - d.getPosition()))
                                + "^"
                                + "-".repeat((int) (d.getEndPosition() - d.getStartPosition() - 1))
                                + "^");
                        else msg.send(sender, "      &7"
                                + " ".repeat((int) (d.getStartPosition() - d.getPosition())));
                    });
                    return false;
                case RECOVERABLE_DEFINED:
                case RECOVERABLE_NOT_DEFINED:
                    System.out.println(e);
            }
            //System.out.println(e);
        }
        return true;
    }

    private static class EngineProvider implements ExecutionControlProvider {

        @Override
        public String name() {
            return "ClassLoaderEngineProvider";
        }

        @Override
        public ExecutionControl generate(ExecutionEnv env, Map<String, String> parameters) {
            return new ClassLoaderControl();
        }
    }

    private static class ClassLoaderControl implements ExecutionControl {

        @Override
        public void load(ClassBytecodes[] cbcs) throws ClassInstallException, NotImplementedException, EngineTerminationException {

        }

        @Override
        public void redefine(ClassBytecodes[] cbcs) throws ClassInstallException, NotImplementedException, EngineTerminationException {
            throw new NotImplementedException("work in process...");
        }

        @Override
        public String invoke(String className, String methodName) throws RunException, EngineTerminationException, InternalException {
            return null;
        }

        @Override
        public String varValue(String className, String varName) throws RunException, EngineTerminationException, InternalException {
            return null;
        }

        @Override
        public void addToClasspath(String path) throws EngineTerminationException, InternalException {

        }

        @Override
        public void stop() throws EngineTerminationException, InternalException {
            throw new NotImplementedException("stop: Not supported.");
        }

        @Override
        public Object extensionCommand(String command, Object arg) throws RunException, EngineTerminationException, InternalException {
            throw new NotImplementedException("Unknown command: " + command);
        }

        @Override
        public void close() {

        }
    }
}

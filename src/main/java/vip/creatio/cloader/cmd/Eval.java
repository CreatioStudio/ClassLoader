package vip.creatio.cloader.cmd;

import vip.creatio.clib.basic.tools.FormatMsgManager;
import vip.creatio.cloader.bukkit.CLoader;
import vip.creatio.cloader.ccl.FileManager;
import jdk.jshell.*;
import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;
import jdk.jshell.spi.ExecutionEnv;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.stream.Stream;

/**
 * The encapsulation of JDK 9 JShell
 */
public final class Eval {

    private static final FormatMsgManager msg = CLoader.getMsgSender();

    private static final String name = "eval";
    private static final String description = "Evaluating Java code in game directly.";
    private static final List<String> aliases = Arrays.asList("jshell", "repl");

    private static JShell shell = provideJShell();
    private static Map<CommandSender, LinkedList<String>> HISTORY = new HashMap<>();
    private static LinkedList<String> GLOBAL_HISTORY = new LinkedList<>();

    //No default constructor
    private Eval() {}

    private static void sendStatic(String path, CommandSender sender, String... args) {
        msg.sendStatic(sender, path, args);
    }

    private static void outputStatic(String path, CommandSender sender, String... args) {
        msg.sendStatic(sender, "MAIN.EVAL.OUTPUT", msg.fromPath(path, args));
    }

    private static void output(CommandSender sender, String msg) {
        Eval.msg.sendStatic(sender, "MAIN.EVAL.OUTPUT", msg);
    }

    // Only send if sender is a player
    private static void sendRestricted(String path, CommandSender sender, String... args) {
        if (sender instanceof Player) {
            msg.sendStatic(sender, path, args);
        }
    }

    private static void addToHistory(CommandSender sender, String input) {
        LinkedList<String> personal = HISTORY.computeIfAbsent(sender, k -> new LinkedList<>());

        if (personal.size() > 75) personal.removeLast();
        if (GLOBAL_HISTORY.size() > 150) GLOBAL_HISTORY.removeLast();

        personal.addFirst(input);
        GLOBAL_HISTORY.addFirst(input);
    }

    public static PluginCommand register() {
        PluginCommand cmd = CommandRegister.create(name);
        cmd.setDescription(description);
        cmd.setAliases(aliases);
        cmd.setUsage("");
        cmd.setPermission(null);
        cmd.setPermissionMessage(null);
        cmd.setExecutor(new Executor());
        return cmd;
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

    private static class Executor implements CommandExecutor {

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            return exec(sender, command, label, args, false);
        }

        private boolean exec(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args, boolean recursive) {
            if ((sender instanceof ConsoleCommandSender)
                    || ((sender instanceof Player || sender instanceof BlockCommandSender)
                    && sender.isOp() && CLoader.getInstance().allowPlayersExecuteDos())) {
                // Var check
                if (args.length < 1) {
                    sendStatic("COMMAND.USAGE.EVAL", sender, command.getName());
                    return true;
                }

                // Get command line
                StringBuilder sb = new StringBuilder();
                for (String str : args) {
                    sb.append(str).append(' ');
                }
                sb.deleteCharAt(sb.length() - 1);
                String source = sb.toString().trim();

                if (!recursive) sendRestricted("MAIN.EVAL.INPUT_FIRST", sender, source);

                // JShell command impl
                if (source.startsWith("/")) {
                    args[0] = args[0].substring(1);
                    switch (args[0].toLowerCase(Locale.ROOT)) {
                        case "vars":
                            Stream<VarSnippet> vars = shell.variables();
                            vars.forEach(v -> sendRestricted("MAIN.EVAL.LIST_SINGLE", sender, v.source()));
                            break;
                        case "methods":
                            Stream<MethodSnippet> mths = shell.methods();
                            mths.forEach(m -> sendRestricted("MAIN.EVAL.LIST_SINGLE", sender, m.source()));
                            break;
                        case "imports":
                            Stream<ImportSnippet> imports = shell.imports();
                            imports.forEach(m -> sendRestricted("MAIN.EVAL.LIST_SINGLE", sender, m.source()));
                            break;
                        case "history":
                            if (args.length == 2 && (args[1].equalsIgnoreCase("global") || args[1].equalsIgnoreCase("g"))) {
                                for (String s : GLOBAL_HISTORY) {
                                    sendRestricted("MAIN.EVAL.LIST_SINGLE", sender, s);
                                }
                            } else if (args.length == 1) {
                                List<String> personal = HISTORY.get(sender);
                                if (personal != null && personal.size() > 0) {
                                    for (String s : personal) {
                                        sendRestricted("MAIN.EVAL.LIST_SINGLE", sender, s);
                                    }
                                } else {
                                    outputStatic("MAIN.EVAL.LIST_EMPTY", sender);
                                }
                            } else {
                                outputStatic("MAIN.EVAL.UNKNOWN_ARG", sender, args[2]);
                                return false;
                            }
                            break;
                        case "!":
                            LinkedList<String> personal = HISTORY.get(sender);
                            if (personal != null && personal.size() > 0) {
                                for (String s : personal) {
                                    if (!s.startsWith("/"))
                                        onCommand(sender, command, label, s.split(" "));
                                }
                            }
                            return false;
                        default:
                            outputStatic("MAIN.EVAL.UNKNOWN_ARG", sender, args[2]);
                            return false;
                    }

                    addToHistory(sender, source);
                } else {
                    String code = source;
                    if (source.charAt(source.length() - 1) != ';') code += ';';

                    List<SnippetEvent> events = shell.eval(code);
                    for (SnippetEvent e : events) {
                        Snippet snippet = e.snippet();
                        switch (e.status()) {
                            case VALID:
                                if (snippet != null)
                                    output(sender, snippet.source());

                                if (!recursive) addToHistory(sender, source);

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
                }
            } else {
                if (sender.isOp()) {
                    sendStatic("MAIN.CONSOLE_ONLY", sender);
                } else {
                    sendStatic("MAIN.NO_PERM", sender);
                }
            }
            return true;
        }
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

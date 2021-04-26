package vip.creatio.cloader.cmd;

import vip.creatio.clib.basic.tools.FormatMsgManager;
import vip.creatio.cloader.bukkit.CLoader;
import vip.creatio.cloader.ccl.FileManager;
import vip.creatio.cloader.ccl.module.ClassFile;
import vip.creatio.cloader.ccl.module.ExternalModule;
import vip.creatio.cloader.ccl.module.Module;
import vip.creatio.cloader.ccl.module.ModuleType;
import vip.creatio.cloader.exception.CommandFileNotFoundException;
import vip.creatio.cloader.exception.CommandNumberFormatException;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vip.creatio.common.ArrayUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public final class Main implements CommandBuilder {

    private static final FormatMsgManager msg = CLoader.getMsgSender();

    private static final String name = "classloader";
    private static final String description = "Main command of ClassLoader.";
    private static final List<String> aliases = Arrays.asList("cloader", "cl", "clsloader", "cls");

    private static final String[]
        TC = new String[]{"reload", "class", "source", "sysinfo", "script", "module"},
            TC_RLD = new String[]{"class", "all", "config"},
            TC_CLS = new String[]{"list", "load", "unload", "reload", "enable", "disable", "run", "info"},
            TC_SRC = new String[]{"compile", "list", "enable", "disable", "info"},
            TC_SCP = new String[]{"compile", "list", "run", "enable", "disable", "info"},
            TC_MOD = new String[]{"download", "remove"}
    ;

    //No default constructor
    private Main() {}

    public static PluginCommand register() {
        PluginCommand cmd = CommandRegister.create(name);
        cmd.setDescription(description);
        cmd.setAliases(aliases);
        cmd.setUsage("");
        cmd.setPermission(null);
        cmd.setPermissionMessage(null);
        cmd.setExecutor(new Executor());
        cmd.setTabCompleter(new Completer());
        return cmd;
    }

    private static class Executor implements CommandExecutor {

        @Override
        public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command command, final @NotNull String s, final @NotNull String[] args) {

            try {
                //Perm check
                if (!sender.hasPermission("classloader.admin")) {
                    msg.sendStatic(sender, "MAIN.NO_PERM");
                    return true;
                }
                //Sender check
                if (sender instanceof BlockCommandSender && !CLoader.getInstance().allowCommandBlockExecute()) {
                    msg.sendStatic(sender, "MAIN.PLAYER_ONLY");
                    return true;
                }


                //max index args can have
                int l = args.length - 1;

                //cloader
                if (l >= 0) {

                    //cloader reload (rld rl)
                    if (args[0].equalsIgnoreCase(TC[0])
                            || args[0].equalsIgnoreCase("rld")
                            || args[0].equalsIgnoreCase("rl")) {

                        reload0(sender, args);
                        return true;

                    }
                    //cloader class (cls cl)
                    else if (args[0].equalsIgnoreCase(TC[1])
                            || args[0].equalsIgnoreCase("cls")
                            || args[0].equalsIgnoreCase("cl")) {

                        class0(sender, args);
                        return true;

                    }

                    //cloader source (src)
                    else if (args[0].equalsIgnoreCase(TC[2])
                            || args[0].equalsIgnoreCase("src")) {

                        source0(sender, args);
                        return true;

                    }

                    //cloader sysinfo (sys)
                    else if (args[0].equalsIgnoreCase(TC[3])
                            || args[0].equalsIgnoreCase("sys")) {

                        msg.send(sender, sysInfo0());
                        return true;

                    }

                    //cloader script (spt s)
                    else if (args[0].equalsIgnoreCase(TC[4])
                            || args[0].equalsIgnoreCase("spt")
                            || args[0].equalsIgnoreCase("s")) {

                        script0(sender, args);
                        return true;

                    }

                    //cloader module (mod)
                    else if (args[0].equalsIgnoreCase(TC[5])
                            || args[0].equalsIgnoreCase("mod")) {

                        module0(sender, args);
                        return true;

                    }
                }
                msg.sendStatic(sender, "COMMAND.USAGE.CLSLOADER");
                return true;
            } catch (CommandNumberFormatException e) {
                switch (e.require) {
                    case Int:
                        msg.sendStatic(sender, "MAIN.ERROR.NOT_INT", e.input);
                        return true;
                    case Positive:
                        msg.sendStatic(sender, "MAIN.ERROR.NOT_POSITIVE", e.input);
                        return true;
                    case NotNegative:
                        msg.sendStatic(sender, "MAIN.ERROR.IS_NEGATIVE", e.input);
                        return true;
                    case NotZero:
                        msg.sendStatic(sender, "MAIN.ERROR.IS_ZERO", e.input);
                        return true;
                }
            } catch (CommandFileNotFoundException e) {
                msg.sendStatic(sender, "MAIN.FILE.DOESNT_EXIST", e.input);
            }
            return true;
        }

        private void reload0(CommandSender sender, String[] args) {
            int l = args.length - 1;
            //cloader reload all
            if (l == 0 || args[1].equalsIgnoreCase(TC_RLD[1])) {
                msg.sendStatic(sender, "MAIN.RELOAD.CONFIG");
                CLoader.getInstance().loadAllConfig();
                //TODO: Make this
                class_reload0(sender, args);
                return;
            }
            //cloader reload class
            else if (args[1].equalsIgnoreCase(TC_RLD[0])) {
                //TODO: Make this
                class_reload0(sender, args);
                return;
            }
            //load config
            else if (args[1].equalsIgnoreCase(TC_RLD[2])) {
                msg.sendStatic(sender, "MAIN.RELOAD.CONFIG");
                CLoader.getInstance().loadAllConfig();
                return;
            }
            //USAGE
            msg.sendStatic(sender, "COMMAND.USAGE.RELOAD");
        }




        private void source0(CommandSender sender, String[] args) {
            int l = args.length - 1;

            if (l >= 1) {
                //cloader source compile
                if (args[1].equalsIgnoreCase(TC_SRC[0])) {

                    source_compile0(sender, args);
                    return;

                }
                //cloader source list
                if (args[1].equalsIgnoreCase(TC_SRC[1])) {

                    source_list0(sender, args);
                    return;

                }
                //cloader source enable
                if (args[1].equalsIgnoreCase(TC_SRC[2])) {

                    source_enable0(sender, args);
                    return;

                }
                //cloader source disable
                if (args[1].equalsIgnoreCase(TC_SRC[3])) {

                    source_disable0(sender, args);
                    return;

                }
                //cloader source info
                if (args[1].equalsIgnoreCase(TC_SRC[4])) {

                    source_info0(sender, args);
                    return;

                }
            }
            msg.sendStatic(sender, "COMMAND.USAGE.SOURCE.USAGE");
        }

        private void source_compile0(CommandSender sender, String[] args) {
            int l = args.length - 1;

            if (l >= 2) {

                //Find folder
                List<File> files = new ArrayList<>();

                for (String path : args[2].split(";")) {
                    try {
                        files.addAll(Arrays.asList(parseSourceFile0(path)));
                    } catch (CommandFileNotFoundException e) {
                        msg.sendStatic(sender, "MAIN.FILE.DOESNT_EXIST", e.input);
                    }
                }

                CLoader.getInstance().getThreadPool().execute(() -> {
                    String[] msg = Module.getJava().compileSrc(files.toArray(new File[0]), ArrayUtil.subArray(args, 3));
                    Main.msg.send(sender, msg);
                });
                return;
            }
            msg.sendStatic(sender, "COMMAND.USAGE.SOURCE.COMPILE");
        }

        private void source_list0(CommandSender sender, String[] args) {
            int l = args.length - 1;

            if (l >= 2) {

                //Parse page num
                int page = (l >= 3) ? parseInt(args[3], true) : 1;

                //Find folder
                File[] files = parseSourceFile0(args[2]);

                //cloader class list *file* *page*
                msg.send(sender, JsonUtil.listItems(
                        page,
                        15,
                        Arrays.asList(files),
                        (src) -> {
                            for (File f : src) {
                                if (f.isDirectory()) return true;
                            }
                            return false;
                        },
                        msg.fromPath("MAIN.FILE.dotJAVA")[0],
                        msg.fromPath("MAIN.FILE.dotJAVA")[0] + " in folder " + args[2],
                        (index, item) -> new Json.PlainText("§l" + JsonUtil.Listable.fill(index) + ". ")
                                .addExtra(new Json.PlainText().addExtra(
                                        JsonUtil.dyePath(FileManager.relativePath(FileManager.SOURCE, item)))
                                        .setClickEvent(new Clickable.RunCommand("/cloader source info "
                                                + FileManager.relativePath(FileManager.SOURCE, item)))
                                        .setHoverEvent(new Hoverable.ShowText(new Json.PlainText("§eView file info")))),
                        new JsonUtil.StandardBottom("/cloader source list " + args[2] + ' ')));
                return;
            }
            msg.sendStatic(sender, "COMMAND.USAGE.SOURCE.LIST");
        }

        private void source_enable0(CommandSender sender, String[] args) {
            int l = args.length - 1;

            List<File> success = new ArrayList<>();

            for (int i = 2; i < args.length; i++) {
                File f;

                if (args[i].equals("*")) {
                    for (File ff : FileManager.SOURCE.listFiles((a, b) -> b.startsWith("-"))) {
                        ff.renameTo(new File(ff.getParentFile(), ff.getName().substring(1)));
                    }
                    msg.sendStatic(sender, "MAIN.FILE.ENABLE", msg.fromPath("MAIN.FILE.ALL"));
                    continue;
                }

                try {
                    f = getFileFromSource(FileManager.SOURCE, args[i]);
                } catch (CommandFileNotFoundException e) {
                    msg.sendStatic(sender, "MAIN.FILE.DOESNT_EXIST", e.input);
                    continue;
                }

                success.add(f);

                File f1 = new File(f.getParent(), f.getName().substring(1));
                if (f.getName().startsWith("-")) f.renameTo(f1);
            }

            msg.sendStatic(sender, "MAIN.FILE.ENABLE", Message.listFileCrafter0(3, success.toArray(new File[0])));
        }

        private void source_disable0(CommandSender sender, String[] args) {
            int l = args.length - 1;

            List<File> success = new ArrayList<>();

            for (int i = 2; i < args.length; i++) {
                File f;

                if (args[i].equals("*")) {
                    for (File ff : FileManager.SOURCE.listFiles((a, b) -> !b.startsWith("-"))) {
                        ff.renameTo(new File(ff.getParentFile(), '-' + ff.getName()));
                    }
                    msg.sendStatic(sender, "MAIN.FILE.DISABLE", Message.fromPath("MAIN.FILE.ALL"));
                    continue;
                }

                try {
                    f = getFileFromSource(FileManager.SOURCE, args[i]);
                } catch (CommandFileNotFoundException e) {
                    msg.sendStatic(sender, "MAIN.FILE.DOESNT_EXIST", e.input);
                    continue;
                }

                success.add(f);

                File f1 = new File(f.getParent(), "-" + f.getName());
                if (!f.getName().startsWith("-")) f.renameTo(f1);
            }

            msg.sendStatic(sender, "MAIN.FILE.DISABLE", Message.listFileCrafter0(3, success.toArray(new File[0])));
        }

        private void source_info0(CommandSender sender, String[] args) {
            int l = args.length - 1;

            if (l == 2) {
                //Find folder
                File[] files = parseSourceFile0(args[2]);
                if (files.length != 1) throw new CommandFileNotFoundException(args[2]);

                Message.send(sender, sourceFileInfo0(files[0]));
                return;
            }
            msg.sendStatic(sender, "COMMAND.USAGE.SOURCE.INFO");
        }




        private void class0(CommandSender sender, String[] args) {
            int l = args.length - 1;

            if (l >= 1) {
                //cloader class list
                if (args[1].equalsIgnoreCase(TC_CLS[0])) {

                    class_list0(sender, args);
                    return;

                }
                //cloader class load
                else if (args[1].equalsIgnoreCase(TC_CLS[1])) {

                    class_load0(sender, args);
                    return;

                }
                //cloader class unload
                else if (args[1].equalsIgnoreCase(TC_CLS[2])) {

                    class_unload0(sender, args);
                    return;

                }
                //cloader class reload
                else if (args[1].equalsIgnoreCase(TC_CLS[3])) {

                    class_reload0(sender, args);
                    return;

                }
                //cloader class enable
                else if (args[1].equalsIgnoreCase(TC_CLS[4])) {

                    class_enable0(sender, args);
                    return;

                }
                //cloader class disable
                else if (args[1].equalsIgnoreCase(TC_CLS[5])) {

                    class_disable0(sender, args);
                    return;

                }
                //cloader class run
                else if (args[1].equalsIgnoreCase(TC_CLS[6])) {

                    class_run0(sender, args);
                    return;

                }
                //cloader class info
                else if (args[1].equalsIgnoreCase(TC_CLS[7])) {

                    class_info0(sender, args);
                    return;

                }
            }
            msg.sendStatic(sender, "COMMAND.USAGE.CLASS.USAGE");
        }

        private void class_list0(CommandSender sender, String[] args) {
            int l = args.length - 1;

            if (l >= 2) {

                Class<?>[] list;
                try {
                    list = getClasses(args[2]);
                } catch (ClassNotFoundException e) {
                    msg.sendStatic(sender, "MAIN.CLASS.NOT_FOUND", args[2]);
                    return;
                }

                //Parse page num
                int page = (l >= 3) ? parseInt(args[3], true) : 1;

                //List loaded class
                Message.send(sender, JsonUtil.listItems(
                        page,
                        15,
                        Arrays.asList(list),
                        Message.fromPath("MAIN.FILE.dotCLASS")[0],
                        Message.fromPath("MAIN.CLASS.LOADED")[0],
                        (index, item) -> new Json.PlainText("§l" + JsonUtil.Listable.fill(index) + ". ")
                                .addExtra(new Json.PlainText().addExtra(new Json.PlainText(item.getTypeName()))
                                        .setClickEvent(new Clickable.RunCommand("/cloader class info " + item.getTypeName()))
                                        .setHoverEvent(new Hoverable.ShowText(new Json.PlainText("§eView class info")))),
                        new JsonUtil.StandardBottom("/cloader class list ")));
                return;
            }
            msg.sendStatic(sender, "COMMAND.USAGE.CLASS.LIST");
        }

        private void class_load0(CommandSender sender, String[] args) {
            int l = args.length - 1;

            if (l >= 2) {

                List<File> files = new ArrayList<>();

                for (int i = 2; i < args.length; i++) {
                    //Find folder
                    try {
                        files.addAll(Arrays.asList(getClassFiles(args[i])));
                    } catch (CommandFileNotFoundException e) {
                        msg.sendStatic(sender, "MAIN.CLASS.NOT_FOUND", e.input);
                    }
                }

                //Load class
                String[] msg = Module.getJava().load(files.toArray(new File[0]));
                Message.send(sender, msg);
            }
        }

        private void class_unload0(CommandSender sender, String[] args) {
            int l = args.length - 1;

            if (l >= 2) {

                List<Class<?>> cls = new ArrayList<>();

                for (int i = 2; i < args.length; i++) {
                    try {
                        cls.addAll(Arrays.asList(getClasses(args[i])));
                    } catch (ClassNotFoundException e) {
                        msg.sendStatic(sender, "MAIN.CLASS.NOT_FOUND", args[i]);
                    }
                }
                String[] msg = Module.getJava().unload(cls.toArray(new Class[0]));
                Message.send(sender, msg);
                return;
            }
            msg.sendStatic(sender, "COMMAND.USAGE.CLASS.UNLOAD.UNLOADED");
        }

        private void class_reload0(CommandSender sender, String[] args) {
            int l = args.length - 1;

            if (l >= 2) {
                List<Class<?>> cls = new ArrayList<>();

                for (int i = 2; i < args.length; i++) {
                    try {
                        cls.addAll(Arrays.asList(getClasses(args[i])));
                    } catch (ClassNotFoundException e) {
                        msg.sendStatic(sender, "MAIN.CLASS.NOT_FOUND", args[i]);
                    }
                }

                msg.sendStatic(sender, "MAIN.CLASS.RELOAD.START", Message.listClassCrafter0(3, cls.toArray(new Class[0])));

                String[] msg = Module.getJava().reload(cls.toArray(new Class[0]));
                Message.send(sender, msg);
                return;
            }
            msg.sendStatic(sender, "COMMAND.USAGE.CLASS.RELOAD");
        }

        private void class_enable0(CommandSender sender, String[] args) {
            int l = args.length - 1;

            if (l >= 2) {
                List<File> enabled = new ArrayList<>();
                for (int i = 2; i < args.length; i++) {
                    File f;

                    if (args[i].equals("*")) {
                        for (File ff : FileManager.COMPILED.listFiles((a, b) -> b.startsWith("-"))) {
                            ff.renameTo(new File(ff.getParentFile(), ff.getName().substring(1)));
                            enabled.add(ff);
                        }
                        Module.getJava().load(FileManager.getClasses());
                        msg.sendStatic(sender, "MAIN.FILE.ENABLE", Message.fromPath("MAIN.FILE.ALL"));
                        continue;
                    }

                    try {
                        f = getFileFromClass(FileManager.COMPILED, args[i]);
                    } catch (CommandFileNotFoundException e) {
                        msg.sendStatic(sender, "MAIN.FILE.DOESNT_EXIST", e.input);
                        continue;
                    }

                    File f1 = new File(f.getParent(), f.getName().substring(1));
                    if (f.getName().startsWith("-")) f.renameTo(f1);

                    enabled.add(f);

                    File[] files;

                    if (f1.isDirectory()) {
                        files = FileManager.getFiles(f1, ".class");
                    } else {
                        try {
                            ClassFile ff = new ClassFile(f1);
                            files = ff.getRelatedClass();
                        } catch (FileNotFoundException | InvalidClassException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    if (files.length > 0) Module.getJava().load(files);
                }

                msg.sendStatic(sender, "MAIN.FILE.ENABLE", Message.listFileCrafter0(3, enabled.toArray(new File[0])));
                return;
            }
            msg.sendStatic(sender, "COMMAND.USAGE.CLASS.ENABLE");
        }

        private void class_disable0(CommandSender sender, String[] args) {
            int l = args.length - 1;

            if (l >= 2) {
                List<File> disabled = new ArrayList<>();
                for (int i = 2; i < args.length; i++) {
                    File f;

                    if (args[i].equals("*")) {
                        Module.getJava().unload(Module.getJava().getExternalClasses().toArray(new Class[0]));
                        for (File ff : FileManager.COMPILED.listFiles((a, b) -> !b.startsWith("-"))) {
                            ff.renameTo(new File(ff.getParentFile(), '-' + ff.getName()));
                            disabled.add(ff);
                        }
                        msg.sendStatic(sender, "MAIN.FILE.DISABLE", Message.fromPath("MAIN.FILE.ALL"));
                        continue;
                    }

                    try {
                        f = getFileFromClass(FileManager.COMPILED, args[i]);
                    } catch (CommandFileNotFoundException e) {
                        continue;
                    }

                    if (f.isDirectory()) {
                        String pkg = args[i].substring(0, args[i].length() - 1);
                        List<Class<?>> lst = new ArrayList<>();
                        for (Class<?> c : Module.getJava().getExternalClasses()){
                            if (c.getTypeName().startsWith(pkg)) lst.add(c);
                        }
                        if (lst.size() > 0) Module.getJava().unload(lst.toArray(new Class[0]));
                    } else {
                        Module.getJava().unload(Module.getJava().getExternalClass(args[i]));
                    }

                    File f1 = new File(f.getParent(), "-" + f.getName());
                    if (!f.getName().startsWith("-")) f.renameTo(f1);
                    disabled.add(f);
                }

                msg.sendStatic(sender, "MAIN.FILE.DISABLE", Message.listFileCrafter0(3, disabled.toArray(new File[0])));
                return;
            }
            msg.sendStatic(sender, "COMMAND.USAGE.CLASS.DISABLE");
        }

        private void class_run0(CommandSender sender, String[] args) {
            int l = args.length - 1;

            if (l >= 3) {
                Class<?> cls = Module.getJava().getExternalClass(args[2]);
                if (cls != null) {
                    for (Method m : Module.getJava().getMethods(cls)) {
                        if (m.getName().equals(args[3])) {
                            msg.sendStatic(sender, "MAIN.CLASS.RUN.EXECUTED", m.getName());
                            Message.switchPrintln(sender);
                            String[] msg;
                            if (m.getParameterCount() > 0 && m.getParameterTypes()[0] == CommandSender.class) {
                                msg =  Module.getJava().run(cls, m, sender, (Object) Arrays.copyOfRange(args, 4, args.length));
                            } else {
                                msg = Module.getJava().run(cls, m, (Object) Arrays.copyOfRange(args, 4, args.length));
                            }
                            Message.initPrintln();
                            if (msg.length > 0) Message.send(sender, msg);
                            return;
                        }
                    }
                    msg.sendStatic(sender, "MAIN.CLASS.METHOD_NOT_FOUND", args[3]);
                    return;
                }
                msg.sendStatic(sender, "MAIN.CLASS.NOT_FOUND", args[2]);
                return;
            }
            msg.sendStatic(sender, "COMMAND.USAGE.CALL");
        }

        private void class_info0(CommandSender sender, String[] args) {
            int l = args.length - 1;

            if (l == 2) {
                Class<?> cls = Module.getJava().getExternalClass(args[2]);
                if (cls != null) {

                    Message.send(sender, classFileInfo0(cls));

                    return;
                } else {
                    msg.sendStatic(sender, "MAIN.CLASS.NOT_FOUND", args[2]);
                    return;
                }
            }
            msg.sendStatic(sender, "COMMAND.USAGE.CLASS.INFO");
        }



        //TODO: make this!
        private void script0(CommandSender sender, String[] args) {
            int l = args.length - 1;


        }



        //TODO: module removal logic, Scala installation problem. Jython's good.
        private void module0(CommandSender sender, String[] args) {
            int l = args.length - 1;

            if (l >= 2) {

                //cls module download
                if (args[1].equalsIgnoreCase(TC_MOD[0])) {
                    CLoader.getInstance().getThreadPool().execute(() -> {
                        for (ModuleType type : ModuleType.externals()) {
                            if (args[2].equalsIgnoreCase(type.getName())) {
                                if (Module.getInstalled().contains(type)) {
                                    msg.sendStatic(sender, "MAIN.MODULE.ALREADY_INSTALLED", type.getName());
                                    return;
                                }
                                //download
                                if (l == 4 && args[3].equals("-ver")) {
                                    Module.install(sender, type, args[4]);
                                    return;
                                }
                                //get list
                                int pg;
                                if (l >= 3) pg = parseInt(args[3], true);
                                else {
                                    pg = 1;
                                    msg.sendStatic(sender, "MAIN.MODULE.QUERY");
                                }
                                try {
                                    String[] version = ExternalModule.acquireVersionList(type);
                                    Message.send(sender, JsonUtil.listItems(
                                            pg,
                                            20,
                                            Arrays.asList(version),
                                            "/null/",
                                            Message.fromPath("MAIN.MODULE.ALL_VERSIONS", type.getName())[0],
                                            (index, item) -> new Json.PlainText("§l" + JsonUtil.Listable.fill(index) + ". ")
                                                        .addExtra(new Json.PlainText().addExtra(new Json.PlainText(type.getName() + ' ' + item))
                                                                .setClickEvent(new Clickable.RunCommand("/cl module download " + args[2] + " -ver " + item))
                                                                .setHoverEvent(new Hoverable.ShowText(new Json.PlainText("§9Download this version")))),
                                            new JsonUtil.StandardBottom("/cl module download " + args[2] + ' ')));
                                    return;
                                } catch (IOException e) {
                                    Message.sendStatic("MAIN.MODULE.QUERY_FAILED",sender);
                                    return;
                                }
                            }
                        }
                        msg.sendStatic(sender, "MAIN.MODULE.NOT_FOUND", args[2]);
                    });
                    return;
                }

                //cls module remove
                if (args[1].equalsIgnoreCase(TC_MOD[1])) {

                }
            }
            msg.sendStatic(sender, "COMMAND.USAGE.MODULE.USAGE");
        }
    }

    private static class Completer implements TabCompleter {

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, @NotNull String[] args) {
            if (!sender.isOp()) return null;
            int l = args.length - 1;

            //cloader *
            if (l == 0) return Arrays.asList(TC);

            //cloader reload
            if (TC[0].equalsIgnoreCase(get(args, 0))) return (l == 1) ? Arrays.asList(TC_RLD) : CommandRegister.COMPLETER_EMPTY;

            //cloader class
            if (TC[1].equalsIgnoreCase(get(args, 0))) {

                //cloader class list
                if (TC_CLS[0].equalsIgnoreCase(get(args, 1))) {
                    List<String> lst = getAllCompiled();
                    lst.removeAll(FileManager.getAllClasses());
                    if (l == 2) return lst;

                    if (l == 3) return CommandRegister.COMPLETER_SMALL_INT;

                    else return CommandRegister.COMPLETER_EMPTY;
                }

                //cloader class load
                if (TC_CLS[1].equalsIgnoreCase(get(args, 1))) {
                    return (l >= 2) ? getUnloadCompiled0() : CommandRegister.COMPLETER_EMPTY;
                }

                //cloader class unload
                if (TC_CLS[2].equalsIgnoreCase(get(args, 1))) {
                    List<String> str = getLoadedCompiled0();
                    return (l >= 2) ? str : CommandRegister.COMPLETER_EMPTY;
                }

                //cloader class reload
                if (TC_CLS[3].equalsIgnoreCase(get(args, 1))) {
                    List<String> str = getLoadedCompiled0();
                    return (l >= 2) ? str : CommandRegister.COMPLETER_EMPTY;
                }

                //cloader class enable
                if (TC_CLS[4].equalsIgnoreCase(get(args, 1))) {
                    return (l >= 2) ? getDisabledCompiled0() : CommandRegister.COMPLETER_EMPTY;
                }

                //cloader class disable
                if (TC_CLS[5].equalsIgnoreCase(get(args, 1))) {
                    return (l >= 2) ? getAllCompiled() : CommandRegister.COMPLETER_EMPTY;
                }

                //cloader class run
                if (TC_CLS[6].equalsIgnoreCase(get(args, 1))) {
                    //cloader class run *class name*
                    if (l == 2) {
                        return FileManager.getLoadedClasses();
                    }

                    //cloader class run *class name* *method name*
                    if (l == 3) {
                        String[] str = Module.getJava().listMethods(args[2]);
                        return Arrays.asList(str);
                    }

                    return CommandRegister.COMPLETER_EMPTY;
                }

                //cloader class info
                if (TC_CLS[7].equalsIgnoreCase(get(args, 1))) {
                    return (l == 2) ? FileManager.getLoadedClasses() : CommandRegister.COMPLETER_EMPTY;
                }

                return (l == 1) ? Arrays.asList(TC_CLS) : CommandRegister.COMPLETER_EMPTY;
            }

            //cloader source
            if (TC[2].equalsIgnoreCase(get(args, 0))) {
                //cloader source compile
                if (TC_SRC[0].equalsIgnoreCase(get(args, 1))) {
                    //cloader source compile *path*
                    if (l == 2) {
                        int i = args[2].lastIndexOf(";");
                        String preword = i > 0 ? args[2].substring(0, i + 1) : "";
                        List<String> lst = sourceFileFolderPath0();
                        for (int j = 0; j < lst.size(); j++) {
                            lst.set(j, preword + lst.get(j));
                        }
                        return lst;
                    }
                    //cloader source compile *path* *options*
                    else {
                        return Collections.singletonList("[options]");
                    }
                }

                //cloader source list
                if (TC_SRC[1].equalsIgnoreCase(get(args, 1))) {
                    if (l == 2) return sourceFolderPath0();

                    if (l == 3) return CommandRegister.COMPLETER_SMALL_INT;

                    else return CommandRegister.COMPLETER_EMPTY;
                }

                //cloader source enable
                if (TC_SRC[2].equalsIgnoreCase(get(args, 1))) {
                    return (l >= 2) ? sourceDisabledPath0() : CommandRegister.COMPLETER_EMPTY;
                }

                //cloader source disable
                if (TC_SRC[3].equalsIgnoreCase(get(args, 1))) {
                    return (l >= 2) ? sourceFileFolderPath0() : CommandRegister.COMPLETER_EMPTY;
                }

                //cloader class info
                if (TC_SRC[4].equalsIgnoreCase(get(args, 1))) {
                    List<String> f = sourceFilePath0();
                    f.remove("*");
                    return (l == 2) ? f : CommandRegister.COMPLETER_EMPTY;
                }

                return (l == 1) ? Arrays.asList(TC_SRC) : CommandRegister.COMPLETER_EMPTY;
            }

            //cloader script
            if (TC[4].equalsIgnoreCase(get(args, 0))) {

                return (l == 1) ? Arrays.asList(TC_SCP) : CommandRegister.COMPLETER_EMPTY;

            }

            //cloader module
            if (TC[5].equalsIgnoreCase(get(args, 0))) {

                //cloader module download
                if (TC_MOD[0].equalsIgnoreCase(get(args, 1))) {
                    List<String> list = new ArrayList<>();
                    for (ModuleType type : Module.getNotInstalled()) {
                        list.add(type.getName());
                    }
                    return (l == 2) ? list : CommandRegister.COMPLETER_EMPTY;
                }

                //cloader module remove
                if (TC_MOD[1].equalsIgnoreCase(get(args, 1))) {
                    List<String> list = new ArrayList<>();
                    for (ModuleType type : Module.getInstalled()) {
                        list.add(type.getName());
                    }
                    return (l == 2) ? list : CommandRegister.COMPLETER_EMPTY;
                }

                //if (l == 2) return Arrays.asList(TC_MOD_DWL);


                return (l == 1) ? Arrays.asList(TC_MOD) : CommandRegister.COMPLETER_EMPTY;
            }
            return CommandRegister.COMPLETER_EMPTY;
        }
    }

    private static String get(String[] args, int index) {
        return args.length < index + 1 ? null : args[index];
    }

    private static int parseInt(String src, boolean positive) {
        try {
            int i = Integer.parseInt(src);
            if (positive && i < 1)
                throw new CommandNumberFormatException(src, CommandNumberFormatException.NumberFormat.Positive);
            return i;
        } catch (NumberFormatException e) {
            throw new CommandNumberFormatException(src, CommandNumberFormatException.NumberFormat.Int);
        }
    }

    private static List<String> sourceFileFolderPath0() {
        File[] file = FileManager.getFileAndFolders(FileManager.SOURCE, ".java");
        return filePathConvert0(file);
    }

    private static List<String> sourceFilePath0() {
        File[] file = FileManager.getFiles(FileManager.SOURCE, ".java");
        return filePathConvert0(file);
    }

    private static List<String> sourceFolderPath0() {
        File[] file = FileManager.getFolders(FileManager.SOURCE);
        return filePathConvert0(file);
    }

    private static List<String> sourceDisabledPath0() {
        File[] file = FileManager.getDisabled(FileManager.SOURCE, ".java");
        return filePathConvert0(file);
    }

    private static List<String> filePathConvert0(File... files) {
        String[] str = new String[files.length + 1];
        str[0] = "*";
        int i = 1;
        for (File f : files) {
            String src = f.getAbsolutePath()
                    .substring(f.getAbsolutePath().lastIndexOf("classes" + File.separatorChar + "src"))
                    .substring(11);
            str[i++] = src.replace(File.separatorChar, '/');
        }
        return new ArrayList<>(Arrays.asList(str));
    }




    private static @NotNull List<String> getUnloadCompiled0() {
        List<String> str = new ArrayList<>();
        str.add("*");
        str.addAll(FileManager.getUnloadCompiled());
        return str;
    }

    private static @NotNull List<String> getLoadedCompiled0() {
        List<String> str = new ArrayList<>();
        str.add("*");
        str.addAll(FileManager.getLoadedCompiled());
        return str;
    }

    private static @NotNull List<String> getAllCompiled() {
        List<String> str = new ArrayList<>();
        str.add("*");
        str.addAll(FileManager.getAllCompiled());
        return str;
    }

    private static @NotNull List<String> getDisabledCompiled0() {
        List<String> str = new ArrayList<>();
        str.add("*");
        str.addAll(FileManager.getDisabledCompiled());
        return str;
    }




    private static @NotNull Class<?>[] getClasses(@NotNull String name) throws ClassNotFoundException {
        if (name.equals("*")) return Module.getJava().getExternalClasses().toArray(new Class[0]);
        String s = name.contains("*") ? name.substring(0, name.lastIndexOf("*")) : name;
        List<Class<?>> lst = new ArrayList<>();
        for (Class<?> c : Module.getJava().getExternalClasses()) {
            String n = c.getTypeName();
            if (n.startsWith(s)) lst.add(c);
        }
        if (lst.isEmpty()) throw new ClassNotFoundException(name);
        return lst.toArray(new Class[0]);
    }

    private static @NotNull File[] getClassFiles(@NotNull String name) {
        try {
            File base = FileManager.getClassFile(name);
            if (base.isDirectory()) {
                return FileManager.getFiles(base, ".class");
            } else {
                ClassFile cf = new ClassFile(base);
                return cf.getRelatedClass();
            }
        } catch (FileNotFoundException | InvalidClassException e) {
            throw new CommandFileNotFoundException(name);
        }
    }



    private static @NotNull File[] parseSourceFile0(String path) {
        File f = getFileFromSource(FileManager.SOURCE, path);
        if (f.isDirectory()) return FileManager.getFiles(f, ".java");
        else return new File[]{f};
    }




    private static @NotNull Json[] classFileInfo0(@NotNull Class<?> clazz) {
        File f;
        try {
            f = ClassFile.getClassFile(clazz);
        } catch (InvalidClassException e) {
            throw new RuntimeException(e);
        }
        Json[] info = fileInfo0(f, "Java bytecode (.class)");
        int cursor = info.length;
        Json[] text = new Json[cursor + 4];

        System.arraycopy(info, 0, text, 0, cursor);
        text[cursor++] = new Json.PlainText(Message.fromPath("MAIN.FILE.INFO.CLASS.NAME",
                clazz.getCanonicalName())[0]);
        text[cursor++] = new Json.PlainText(Message.fromPath("MAIN.FILE.INFO.CLASS.INTERFACE")[0])
                .addExtra(enumItem0(3, clazz.getInterfaces(), Class::getSimpleName));
        text[cursor++] = new Json.PlainText(Message.fromPath("MAIN.FILE.INFO.CLASS.FIELD")[0])
                .addExtra(enumItem0(3, clazz.getDeclaredFields(), Field::toGenericString));;
        text[cursor] = new Json.PlainText(Message.fromPath("MAIN.FILE.INFO.CLASS.METHOD")[0])
                .addExtra(enumItem0(3, clazz.getDeclaredMethods(), Method::toGenericString));;
        return text;
    }

    private static @NotNull Json[] sourceFileInfo0(@NotNull File src) {
        Json[] info = fileInfo0(src, "Java source file (.java)");
        int cursor = info.length;
        Json[] text = new Json[cursor + 1];

        System.arraycopy(info, 0, text, 0, cursor);
        text[cursor] = new Json.PlainText(Message.fromPath("MAIN.FILE.INFO.JAVA.CLASS", FileManager.getCompiledName(src))[0]);
        return text;
    }

    private static <T> @NotNull Json[] enumItem0(int max, @NotNull T[] item, Message.Messager<T> stringStrategy) {
        List<Json> json = new ArrayList<>();
        for (int i = 0; i < Math.min(item.length, max); i++) {
            json.add(new Json.PlainText(Message.fromPath("MAIN.FILE.INFO.CLASS.LIST", stringStrategy.getMessage(item[i]))[0]));
        }
        if (max < item.length) json.add(new Json.PlainText(Message.fromPath("MAIN.FILE.INFO.CLASS.LIST", "...+" + (item.length - max))[0]));
        return json.toArray(new Json[0]);
    }

    private static @NotNull File getFile(File rt, String path) {
        File f = new File(rt, path.startsWith("/") ? path.substring(1) : path);
        if (!f.exists()) throw new CommandFileNotFoundException(path);
        return f;
    }




    private static @NotNull File getFileFromClass(File rt, String path) {
        if (path.equals("*")) return rt;
        path = path.replace('.', File.separatorChar);
        if (path.endsWith("*")) path = path.substring(0, path.length() - 2);
        else path = path.concat(".class");
        File f = new File(rt, path);
        if (f.exists()) return f;
        throw new CommandFileNotFoundException(path);
    }

    private static @NotNull File getFileFromSource(File rt, String path) {
        if (path.equals("*")) return rt;
        path = path.substring(1).replace('/', File.separatorChar);
        File f = new File(rt, path);
        if (f.exists()) return f;
        throw new CommandFileNotFoundException(path);
    }

    private static @NotNull Json[] fileInfo0(@NotNull File file, @Nullable String type) {
        Json[] text = new Json[6];
        text[0] = new Json.PlainText(Message.fromPath("MAIN.FILE.INFO.HEADER", file.getName())[0]);
        text[1] = new Json.PlainText(Message.fromPath("MAIN.FILE.INFO.TYPE", type == null ? FileManager.type(file) : type)[0]);
        text[2] = new Json.PlainText(Message.fromPath("MAIN.FILE.INFO.PATH", FileManager.relativePath(FileManager.ROOT, file))[0]);
        text[3] = new Json.PlainText(Message.fromPath("MAIN.FILE.INFO.SIZE", "" + file.length())[0]);
        text[4] = new Json.PlainText(Message.fromPath("MAIN.FILE.INFO.LAST_MODIFIED", FileManager.lastModifiedDate(file))[0]);
        String md5;
        //if file bigger than 16 MB, refuse to calc MD5
        if (file.length() > 16_777_216) {
            md5 = "/file too large: " + (file.length() >> 20) + " MB/";
        } else {
            try {
                md5 = FileManager.md5Checksum(file);
            } catch (IOException e) {
                md5 = "/failed to get: " + e.getMessage() + "/";
            }
        }
        text[5] = new Json.PlainText(Message.fromPath("MAIN.FILE.INFO.MD5", md5)[0]);
        return text;
    }

    private static @NotNull Json[] sysInfo0() {
        Json[] text = new Json[13];
        text[0] = new Json.PlainText(Message.fromPath("MAIN.CLASS.SYSINFO.START")[0]);
        text[1] = new Json.PlainText(Message.fromPath("MAIN.CLASS.SYSINFO.JAVA", System.getProperty("java.version"))[0]);
        text[2] = new Json.PlainText(Message.fromPath("MAIN.CLASS.SYSINFO.OS", System.getProperty("os.name"))[0]);
        text[3] = Json.empty();

        double[] sys = CLoader.getInstance().getRuntimeExecutor().systemCpuUsed();
        double[] proc = CLoader.getInstance().getRuntimeExecutor().processCpuUsed();

        NumberFormat d1 = StringUtil.getD1();
        NumberFormat d2 = StringUtil.getD2();

        text[4] = new Json.PlainText(Message.fromPath("MAIN.CLASS.SYSINFO.CPU")[0]);
        text[5] = new Json.PlainText(Message.fromPath("MAIN.CLASS.SYSINFO.CPU_SYSTEM", d1.format(sys[0]), d1.format(sys[1]), d1.format(sys[2]), d1.format(sys[3]))[0]);
        text[6] = new Json.PlainText(Message.fromPath("MAIN.CLASS.SYSINFO.CPU_PROCESS", d1.format(proc[0]), d1.format(proc[1]), d1.format(proc[2]), d1.format(proc[3]))[0]);
        text[7] = Json.empty();
        text[8] = new Json.PlainText(Message.fromPath("MAIN.CLASS.SYSINFO.MEMORY")[0]);
        sys = new double[]{((FileManager.OSINFO.getTotalPhysicalMemorySize() - FileManager.OSINFO.getFreePhysicalMemorySize()) >> 20) / 1024.0,
                (FileManager.OSINFO.getTotalPhysicalMemorySize() >> 20) / 1024.0};
        proc = new double[]{(Runtime.getRuntime().totalMemory() >> 20) / 1024.0,
                (Runtime.getRuntime().maxMemory() >> 20) / 1024.0};
        text[9] = new Json.PlainText(Message.fromPath(
                "MAIN.CLASS.SYSINFO.MEMORY_SYSTEM", d2.format(sys[0]), d2.format(sys[1]), d1.format(((sys[1] - sys[0]) / sys[1]) * 100))[0]);
        text[10] = new Json.PlainText(Message.fromPath(
                "MAIN.CLASS.SYSINFO.MEMORY_PROCESS", d2.format(proc[0]), d2.format(proc[1]), d1.format(((proc[1] - proc[0]) / proc[1]) * 100))[0]);
        text[11] = Json.empty();
        sys = new double[]{((FileManager.DISK.getTotalSpace() - FileManager.DISK.getFreeSpace()) >> 20) / 1024.0,
                (FileManager.DISK.getTotalSpace() >> 20) / 1024.0};
        text[12] = new Json.PlainText(Message.fromPath("MAIN.CLASS.SYSINFO.DISK", d2.format(sys[0]), d2.format(sys[1]), d1.format(((sys[1] - sys[0]) / sys[1]) * 100))[0]);

        return text;
    }
}

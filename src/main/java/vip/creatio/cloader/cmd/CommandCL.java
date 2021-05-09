package vip.creatio.cloader.cmd;

import com.mojang.brigadier.tree.LiteralCommandNode;
import vip.creatio.basic.chat.*;
import vip.creatio.basic.cmd.*;
import vip.creatio.basic.tools.FormatMsgManager;
import vip.creatio.cloader.bukkit.CLoader;
import vip.creatio.cloader.ccl.FileManager;
import vip.creatio.cloader.ccl.module.ClassFile;
import vip.creatio.cloader.ccl.module.Module;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vip.creatio.common.util.ArrayUtil;
import vip.creatio.common.util.FileUtil;
import vip.creatio.common.util.StringFormatUtil;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public final class CommandCL {

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
    private CommandCL() {}

    public static void register(CommandRegister register) {
        LiteralCommandNode<?> node = Argument.of(name)

                .then(Argument.of("reload")

                        .then(Argument.of("all").executes(c -> {
                            msg.sendStatic(c.getSender(), "MAIN.RELOAD.CONFIG");
                            CLoader.getInstance().loadAllConfig();
                            //TODO: Make this
                            //class_reload0(c.getSender(), args);
                        }))

                        .then(Argument.of("class").executes(c -> {
                            //TODO: Make this
                            //class_reload0(sender, args);
                        }))

                        .then(Argument.of("config").executes(c -> {
                            msg.sendStatic(c.getSender(), "MAIN.RELOAD.CONFIG");
                            CLoader.getInstance().loadAllConfig();
                        })))

                .then(Argument.of("class")

                        .then(Argument.of("list")
                                .then(Argument.arg("page", ArgumentTypes.ofInt(1))
                                        .then(Argument.arg("range", ArgumentTypes.ofClasses(Module.getJava().getExternalClasses()))
                                                .executes(CommandCL::classList)))
                                .fallbacksInvalidInput((c, e) -> {
                                    if (e.getType() == ClassArgumentType.CLASS_NOT_FOUND) {
                                        if (e instanceof SyntaxException) {
                                            msg.sendStatic(c.getSender(), "MAIN.CLASS.NOT_FOUND", ((SyntaxException) e).getArguments()[0].toString());
                                        }
                                    } else {
                                        msg.sendStatic(c.getSender(), "COMMAND.USAGE.CLASS.LIST");
                                    }
                                })
                        )

                        .then(Argument.of("load")
                                .then(Argument.arg("files", ArgumentTypes.ofMultiple(ArgumentTypes.ofClassFiles(FileManager.COMPILED, f -> !f.getName().startsWith("-"))))
                                        .executes(CommandCL::classLoad))
                                .fallbacksInvalidInput((c, e) -> {
                                    if (e.getType() == FileArgumentType.NOT_EXIST) {
                                        if (e instanceof SyntaxException) {
                                            msg.sendStatic(c.getSender(), "MAIN.FILE.DOESNT_EXIST", ((SyntaxException) e).getArguments()[0].toString());
                                        }
                                    } else {
                                        msg.sendStatic(c.getSender(), "COMMAND.USAGE.CLASS.LOAD");
                                    }
                                })
                        )

                        .then(Argument.of("unload")
                                .then(Argument.arg("classes", ArgumentTypes.ofMultiple(ArgumentTypes.ofClasses(Module.getJava().getExternalClasses())))
                                        .executes(CommandCL::classUnload))
                                .fallbacksInvalidInput((c, e) -> {
                                    if (e.getType() == ClassArgumentType.CLASS_NOT_FOUND) {
                                        if (e instanceof SyntaxException) {
                                            msg.sendStatic(c.getSender(), "MAIN.CLASS.NOT_FOUND", ((SyntaxException) e).getArguments()[0].toString());
                                        }
                                    } else {
                                        msg.sendStatic(c.getSender(), "COMMAND.USAGE.CLASS.UNLOAD");
                                    }
                                })
                        )

                        .then(Argument.of("reload").then(Argument.arg("classes", ArgumentTypes.ofMultiple(ArgumentTypes.ofClasses(Module.getJava().getExternalClasses())))
                                .executes(CommandCL::classReload))
                                .fallbacksInvalidInput((c, e) -> {
                                    if (e.getType() == ClassArgumentType.CLASS_NOT_FOUND) {
                                        if (e instanceof SyntaxException) {
                                            msg.sendStatic(c.getSender(), "MAIN.CLASS.NOT_FOUND", ((SyntaxException) e).getArguments()[0].toString());
                                        }
                                    } else {
                                        msg.sendStatic(c.getSender(), "COMMAND.USAGE.CLASS.RELOAD");
                                    }
                                })
                        )

                        .then(Argument.of("enable")
                                .then(Argument.arg("files", ArgumentTypes.ofMultiple(ArgumentTypes.ofClassFiles(FileManager.COMPILED, f -> f.getName().startsWith("-"))))
                                        .executes(CommandCL::classEnable))
                                .fallbacksInvalidInput((c, e) -> {
                                    if (e.getType() == ClassPathArgumentType.NOT_EXIST) {
                                        if (e instanceof SyntaxException) {
                                            msg.sendStatic(c.getSender(), "MAIN.FILE.DOESNT_EXIST", ((SyntaxException) e).getArguments()[0].toString());
                                        }
                                    } else {
                                        msg.sendStatic(c.getSender(), "COMMAND.USAGE.CLASS.ENABLE");
                                    }
                                })
                        )

                        .then(Argument.of("disable")
                                .then(Argument.arg("files", ArgumentTypes.ofMultiple(ArgumentTypes.ofClassFiles(FileManager.COMPILED, f -> !f.getName().startsWith("-"))))
                                        .executes(CommandCL::classDisable))
                                .fallbacksInvalidInput((c, e) -> {
                                    if (e.getType() == ClassPathArgumentType.NOT_EXIST) {
                                        if (e instanceof SyntaxException) {
                                            msg.sendStatic(c.getSender(), "MAIN.FILE.DOESNT_EXIST", ((SyntaxException) e).getArguments()[0].toString());
                                        }
                                    } else {
                                        msg.sendStatic(c.getSender(), "COMMAND.USAGE.CLASS.DISABLE");
                                    }
                                })
                        )

                        .then(Argument.of("run").then(Argument.arg("class", ArgumentTypes.ofClass(Module.getJava().getExternalClasses()))
                                .then(Argument.arg("method", ArgumentTypes.ofWord())
                                        .then(Argument.arg("args", ArgumentTypes.ofMultipleString()).executes((CommandAction) CommandCL::classRun))
                                        .suggests((c, b) -> {
                                            Class<?>[] cls = c.getArgument("class", Class[].class);
                                            if (cls.length < 1) return b.buildFuture();
                                            Module.getJava().getMethods(cls[0]).forEach(m -> b.suggest(m.getName()));
                                            return b.buildFuture();
                                        }))
                                        .executes((CommandAction) CommandCL::classRun)
                                )
                                .fallbacksInvalidInput((c, e) -> {
                                    if (e instanceof SyntaxException) {
                                        if (e.getType() == ClassArgumentType.CLASS_NOT_FOUND) {
                                            msg.sendStatic(c.getSender(), "MAIN.CLASS.NOT_FOUND", ((SyntaxException) e).getArguments()[0].toString());
                                        } else if (e.getType() == ClassArgumentType.SINGLE_ONLY) {
                                            msg.sendStatic(c.getSender(), "MAIN.FILE.SINGLE_ONLY");
                                        }
                                    } else {
                                        msg.sendStatic(c.getSender(), "COMMAND.USAGE.CLASS.RUN");
                                    }
                                }))

                        .then(Argument.of("info")
                                .then(Argument.arg("class", ArgumentTypes.ofClass(Module.getJava().getExternalClasses()))
                                        .executes(CommandCL::classInfo))
                                .fallbacksInvalidInput((c, e) -> {
                                    if (e instanceof SyntaxException) {
                                        if (e.getType() == ClassArgumentType.CLASS_NOT_FOUND) {
                                            msg.sendStatic(c.getSender(), "MAIN.CLASS.NOT_FOUND", ((SyntaxException) e).getArguments()[0].toString());
                                        } else if (e.getType() == ClassArgumentType.SINGLE_ONLY) {
                                            msg.sendStatic(c.getSender(), "MAIN.FILE.SINGLE_ONLY");
                                        }
                                    } else {
                                        msg.sendStatic(c.getSender(), "COMMAND.USAGE.CLASS.INFO");
                                    }
                                })
                        )
                )

                .then(Argument.of("source")

                        .then(Argument.of("compile")
                                .then(Argument.arg("files", ArgumentTypes.ofMultiple(ArgumentTypes.ofJavaFiles(FileManager.SOURCE, f -> !f.getName().startsWith("-"))))
                                        .executes(CommandCL::sourceCompile))
                                .fallbacksInvalidInput((c, e) -> {
                                    if (e.getType() == ClassPathArgumentType.NOT_EXIST) {
                                        if (e instanceof SyntaxException) {
                                            msg.sendStatic(c.getSender(), "MAIN.FILE.DOESNT_EXIST", ((SyntaxException) e).getArguments()[0].toString());
                                        }
                                    } else {
                                        msg.sendStatic(c.getSender(), "COMMAND.USAGE.SOURCE.COMPILE");
                                    }
                                })
                        )

                        .then(Argument.of("list")
                                .then(Argument.arg("page", ArgumentTypes.ofInt(1))
                                        .then(Argument.arg("range", ArgumentTypes.ofFiles(FileManager.SOURCE, f -> !f.getName().startsWith("-") && f.isDirectory(), true))
                                                .executes(CommandCL::sourceList)))
                                .fallbacksInvalidInput((c, e) -> {
                                    if (e.getType() == FileArgumentType.NOT_EXIST) {
                                        if (e instanceof SyntaxException) {
                                            msg.sendStatic(c.getSender(), "MAIN.FILE.DOESNT_EXIST", ((SyntaxException) e).getArguments()[0].toString());
                                        }
                                    } else {
                                        msg.sendStatic(c.getSender(), "COMMAND.USAGE.SOURCE.LIST");
                                    }
                                })
                        )

                        .then(Argument.of("info")
                                .then(Argument.arg("file", ArgumentTypes.ofFile(FileManager.SOURCE))
                                        .executes(CommandCL::sourceInfo))
                                .fallbacksInvalidInput((c, e) -> {
                                    if (e instanceof SyntaxException) {
                                        if (e.getType() == FileArgumentType.NOT_EXIST) {
                                            msg.sendStatic(c.getSender(), "MAIN.FILE.DOESNT_EXIST", ((SyntaxException) e).getArguments()[0].toString());
                                        } else if (e.getType() == FileArgumentType.SINGLE_ONLY) {
                                            msg.sendStatic(c.getSender(), "MAIN.FILE.SINGLE_ONLY");
                                        }
                                    } else {
                                        msg.sendStatic(c.getSender(), "COMMAND.USAGE.SOURCE.INFO");
                                    }
                                })
                        )
                )


                .then(Argument.of("sysinfo")
                        .executes(c -> msg.send(c.getSender(), sysInfo()))
                )


                .requires(s -> s.hasPermission("classloader.admin"))
                .requiresSenderType(t -> !(t == SenderType.COMMAND_BLOCK && !CLoader.getInstance().allowCommandBlockExecute()))

                .fallbacksNoPermission(s -> msg.sendStatic(s, "MAIN.NO_PERM"))
                .fallbacksInvalidSender(s -> msg.sendStatic(s, "MAIN.PLAYER_ONLY"))
                .build();
        register.register(node, description, aliases);
    }


    // class

    private static void classList(Context c) {

        Class<?>[] list = c.getArgument("range", Class[].class);

        //Parse page num
        int page = c.getArgument("page", int.class);

        //List loaded class
        msg.send(c.getSender(), newList(
                Arrays.asList(list),
                page,
                msg.getList("MAIN.FILE.dotCLASS")[0],
                msg.getList("MAIN.FILE.dotCLASS")[0],
                (index, item) -> Component.of("§l" + StringFormatUtil.toAlignedString(index, 5) + ". ")
                        .append(Component.of(item.getTypeName())
                                .withClickEvent(ClickEvent.runCmd("/cloader class info " + item.getTypeName()))
                                .withHoverEvent(HoverEvent.showText(Component.of("§eView class info"))))
        ));
    }

    private static void classLoad(Context c) {
        @SuppressWarnings("unchecked")
        List<File[]> arg = (List<File[]>) c.getArgument("files", List.class);

        List<File> files = ArrayUtil.flat(arg);

        //Load class
        String[] msg = Module.getJava().load(files.toArray(new File[0]));
        CommandCL.msg.send(c.getSender(), msg);
    }

    private static void classUnload(Context c) {
        @SuppressWarnings("unchecked")
        List<Class<?>[]> f = (List<Class<?>[]>) c.getArgument("classes", List.class);

        Class<?>[] cls = ArrayUtil.flatToArray(f, Class[]::new);

        String[] msg = Module.getJava().unload(cls);
        CommandCL.msg.send(c.getSender(), msg);
    }

    private static void classReload(Context c) {
        @SuppressWarnings("unchecked")
        List<Class<?>[]> f = (List<Class<?>[]>) c.getArgument("classes", List.class);

        Class<?>[] cls = ArrayUtil.flatToArray(f, Class[]::new);

        StringFormatUtil.formatItems(msg.getSingle("MAIN.FILE.CLASS"), cls);

        String[] msg = Module.getJava().reload(cls);
        CommandCL.msg.send(c.getSender(), msg);
    }

    private static void classEnable(Context c) {
        @SuppressWarnings("unchecked")
        List<File[]> arg = (List<File[]>) c.getArgument("files", List.class);

        List<File> files = ArrayUtil.flat(arg);

        for (File file : files) {
            file.renameTo(new File(file.getParentFile(), file.getName().substring(1)));
            Module.getJava().load(file);
        }

        msg.sendStatic(c.getSender(), "MAIN.FILE.ENABLE", StringFormatUtil.formatItems(msg.getSingle("MAIN.FILE.FILE"), files));
    }

    private static void classDisable(Context c) {
        @SuppressWarnings("unchecked")
        List<File[]> arg = (List<File[]>) c.getArgument("files", List.class);

        List<File> files = ArrayUtil.flat(arg);

        for (File file : files) {
            Module.getJava().unload(Module.getJava().getExternalClass(FileUtil.toClassName(FileManager.COMPILED, file)));
            file.renameTo(new File(file.getParentFile(), '-' + file.getName()));
        }

        msg.sendStatic(c.getSender(), "MAIN.FILE.DISABLE", StringFormatUtil.formatItems(msg.getSingle("MAIN.FILE.FILE"), files));
    }

    @SuppressWarnings("unchecked")
    private static boolean classRun(Context c) {
        Class<?>[] cls = c.getArgument("class", Class[].class);
        String method = c.getArgument("method", String.class);
        for (Method m : Module.getJava().getMethods(cls[0])) {
            if (m.getName().equals(method)) {
                msg.sendStatic(c.getSender(), "MAIN.CLASS.RUN.EXECUTED", m.getName());
                //Message.switchPrintln(sender); TODO: impl this
                String[] message;
                String[] args;
                try {
                    args = ((List<String>) c.getArgument("args", List.class)).toArray(new String[0]);
                } catch (IllegalArgumentException e) {
                    args = new String[0];
                }

                //TODO: confusing
                if (m.getParameterCount() > 0 && m.getParameterTypes()[0] == CommandSender.class) {
                    message =  Module.getJava().run(cls[0], m, c.getSender(), (Object) args);
                } else {
                    message = Module.getJava().run(cls[0], m, (Object[]) args);
                }
                //Message.initPrintln();
                if (message.length > 0) msg.send(c.getSender(), message);
                return true;
            }
        }
        msg.sendStatic(c.getSender(), "MAIN.CLASS.METHOD_NOT_FOUND", method);
        return false;
    }

    private static void classInfo(Context c) {
        Class<?>[] cls = c.getArgument("class", Class[].class);
        msg.send(c.getSender(), getClassFileInfo(cls[0]));
    }



    // source

    private static void sourceCompile(Context c) {
        @SuppressWarnings("unchecked")
        List<File[]> arg = (List<File[]>) c.getArgument("files", List.class);

        List<File> files = ArrayUtil.flat(arg);

        CLoader.getInstance().getThreadPool().execute(() -> {
            String[] msg = Module.getJava().compileSrc(files.toArray(new File[0]));
            CommandCL.msg.send(c.getSender(), msg);
        });
    }

    private static void sourceList(Context c) {
        int page = c.getArgument("page", int.class);
        File[] files = c.getArgument("range", File[].class);

        msg.send(c.getSender(), newList(
                Arrays.asList(files),
                page,
                msg.getList("MAIN.FILE.dotJAVA")[0],
                msg.getList("MAIN.FILE.dotJAVA")[0],
                (index, item) -> {
                    Component relative = dyePath(FileManager.relativePath(FileManager.SOURCE, item));
                    return Component.of("§l" + StringFormatUtil.toAlignedString(index, 5) + ". ")
                            .append(relative
                                    .withClickEvent(ClickEvent.runCmd("/cloader source info " + relative.getString()))
                                    .withHoverEvent(HoverEvent.showText(Component.of("§eView file info"))));
                }));
    }

    //TODO: merge into a central file system
//    private static void sourceEnable(Context c) {
//        int l = args.length - 1;
//
//        List<File> success = new ArrayList<>();
//
//        for (int i = 2; i < args.length; i++) {
//            File f;
//
//            if (args[i].equals("*")) {
//                for (File ff : FileManager.SOURCE.listFiles((a, b) -> b.startsWith("-"))) {
//                    ff.renameTo(new File(ff.getParentFile(), ff.getName().substring(1)));
//                }
//                msg.sendStatic(sender, "MAIN.FILE.ENABLE", msg.getList("MAIN.FILE.ALL"));
//                continue;
//            }
//
//            try {
//                f = getFileFromSource(FileManager.SOURCE, args[i]);
//            } catch (CommandFileNotFoundException e) {
//                msg.sendStatic(sender, "MAIN.FILE.DOESNT_EXIST", e.input);
//                continue;
//            }
//
//            success.add(f);
//
//            File f1 = new File(f.getParent(), f.getName().substring(1));
//            if (f.getName().startsWith("-")) f.renameTo(f1);
//        }
//
//        msg.sendStatic(sender, "MAIN.FILE.ENABLE", Message.listFileCrafter0(3, success.toArray(new File[0])));
//    }
//
//    private static void sourceDisable(Context c) {
//        int l = args.length - 1;
//
//        List<File> success = new ArrayList<>();
//
//        for (int i = 2; i < args.length; i++) {
//            File f;
//
//            if (args[i].equals("*")) {
//                for (File ff : FileManager.SOURCE.listFiles((a, b) -> !b.startsWith("-"))) {
//                    ff.renameTo(new File(ff.getParentFile(), '-' + ff.getName()));
//                }
//                msg.sendStatic(sender, "MAIN.FILE.DISABLE", Message.fromPath("MAIN.FILE.ALL"));
//                continue;
//            }
//
//            try {
//                f = getFileFromSource(FileManager.SOURCE, args[i]);
//            } catch (CommandFileNotFoundException e) {
//                msg.sendStatic(sender, "MAIN.FILE.DOESNT_EXIST", e.input);
//                continue;
//            }
//
//            success.add(f);
//
//            File f1 = new File(f.getParent(), "-" + f.getName());
//            if (!f.getName().startsWith("-")) f.renameTo(f1);
//        }
//
//        msg.sendStatic(sender, "MAIN.FILE.DISABLE", Message.listFileCrafter0(3, success.toArray(new File[0])));
//    }

    private static void sourceInfo(Context c) {
        File[] file = c.getArgument("file", File[].class);
        msg.send(c.getSender(), getSourceFileInfo(file[0]));
    }




    private static <T> Component[] newList(Collection<T> src, int page, String nullHolder, String title, BiFunction<Integer, T, Component> listFormat) {
        Components.List<T> list = new Components.List<>(src);
        list.nullFormat = () -> Component.of(msg.getList("MAIN.LIST.NOT_FOUND", nullHolder)[0]);
        list.outOfBoundFormat = (enter, max) -> Component.of(msg.getList("MAIN.LIST.OUT_OF_BOUND",
                enter.toString(), max.toString())[0]);
        list.titleFormat = (pg, last) -> Component.of(msg.getList("MAIN.LIST.HEADER", StringFormatUtil.intToString(pg), title)[0]);
        list.bottomFormat = (pg, last) -> Component.of(msg.getList("MAIN.LIST.FOOTER", Integer.toString(src.size()))[0]);
        list.listFormat = listFormat;
        list.page = page;
        return list.craft();
    }

    public static Component dyePath(String path) {
        String[] str = new String[2];
        str[0] = path.substring(0, path.lastIndexOf("/") + 1);
        str[1] = path.substring(path.lastIndexOf("/") + 1);
        return Component.of(str[0]).withColor(0xFCFCFC).append(Component.of(str[1]).withColor(ChatFormat.YELLOW));
    }

    private static FileFilter classFileFilter() {
        return f -> !f.isDirectory() && f.getName().endsWith(".class");
    }

    private static FileFilter classFileFilter(Predicate<String> fileNameFilter) {
        return f -> !f.isDirectory() && fileNameFilter.and(s -> s.endsWith(".class")).test(f.getName());
    }

//    private static class Executor implements CommandExecutor {
//
//        @Override
//        public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command command, final @NotNull String s, final @NotNull String[] args) {
//
//            try {
//                //Perm check
//                if (!sender.hasPermission("classloader.admin")) {
//                    msg.sendStatic(sender, "MAIN.NO_PERM");
//                    return true;
//                }
//                //Sender check
//                if (sender instanceof BlockCommandSender && !CLoader.getInstance().allowCommandBlockExecute()) {
//                    msg.sendStatic(sender, "MAIN.PLAYER_ONLY");
//                    return true;
//                }
//
//
//                //max index args can have
//                int l = args.length - 1;
//
//                //cloader
//                if (l >= 0) {
//
//                    //cloader reload (rld rl)
//                    if (args[0].equalsIgnoreCase(TC[0])
//                            || args[0].equalsIgnoreCase("rld")
//                            || args[0].equalsIgnoreCase("rl")) {
//
//                        reload0(sender, args);
//                        return true;
//
//                    }
//                    //cloader class (cls cl)
//                    else if (args[0].equalsIgnoreCase(TC[1])
//                            || args[0].equalsIgnoreCase("cls")
//                            || args[0].equalsIgnoreCase("cl")) {
//
//                        class0(sender, args);
//                        return true;
//
//                    }
//
//                    //cloader source (src)
//                    else if (args[0].equalsIgnoreCase(TC[2])
//                            || args[0].equalsIgnoreCase("src")) {
//
//                        source0(sender, args);
//                        return true;
//
//                    }
//
//                    //cloader sysinfo (sys)
//                    else if (args[0].equalsIgnoreCase(TC[3])
//                            || args[0].equalsIgnoreCase("sys")) {
//
//                        msg.send(sender, sysInfo());
//                        return true;
//
//                    }
//
//                    //cloader script (spt s)
//                    else if (args[0].equalsIgnoreCase(TC[4])
//                            || args[0].equalsIgnoreCase("spt")
//                            || args[0].equalsIgnoreCase("s")) {
//
//                        script0(sender, args);
//                        return true;
//
//                    }
//
//                    //cloader module (mod)
//                    else if (args[0].equalsIgnoreCase(TC[5])
//                            || args[0].equalsIgnoreCase("mod")) {
//
//                        module0(sender, args);
//                        return true;
//
//                    }
//                }
//                msg.sendStatic(sender, "COMMAND.USAGE.CLSLOADER");
//                return true;
//            } catch (CommandNumberFormatException e) {
//                switch (e.require) {
//                    case Int:
//                        msg.sendStatic(sender, "MAIN.ERROR.NOT_INT", e.input);
//                        return true;
//                    case Positive:
//                        msg.sendStatic(sender, "MAIN.ERROR.NOT_POSITIVE", e.input);
//                        return true;
//                    case NotNegative:
//                        msg.sendStatic(sender, "MAIN.ERROR.IS_NEGATIVE", e.input);
//                        return true;
//                    case NotZero:
//                        msg.sendStatic(sender, "MAIN.ERROR.IS_ZERO", e.input);
//                        return true;
//                }
//            } catch (CommandFileNotFoundException e) {
//                msg.sendStatic(sender, "MAIN.FILE.DOESNT_EXIST", e.input);
//            }
//            return true;
//        }
//
//        private void reload0(CommandSender sender, String[] args) {
//            int l = args.length - 1;
//            //cloader reload all
//            if (l == 0 || args[1].equalsIgnoreCase(TC_RLD[1])) {
//                msg.sendStatic(sender, "MAIN.RELOAD.CONFIG");
//                CLoader.getInstance().loadAllConfig();
//                //TODO: Make this
//                class_reload0(sender, args);
//                return;
//            }
//            //cloader reload class
//            else if (args[1].equalsIgnoreCase(TC_RLD[0])) {
//                //TODO: Make this
//                class_reload0(sender, args);
//                return;
//            }
//            //load config
//            else if (args[1].equalsIgnoreCase(TC_RLD[2])) {
//                msg.sendStatic(sender, "MAIN.RELOAD.CONFIG");
//                CLoader.getInstance().loadAllConfig();
//                return;
//            }
//            //USAGE
//            msg.sendStatic(sender, "COMMAND.USAGE.RELOAD");
//        }
//
//
//
//
//        private void source0(CommandSender sender, String[] args) {
//            int l = args.length - 1;
//
//            if (l >= 1) {
//                //cloader source compile
//                if (args[1].equalsIgnoreCase(TC_SRC[0])) {
//
//                    source_compile0(sender, args);
//                    return;
//
//                }
//                //cloader source list
//                if (args[1].equalsIgnoreCase(TC_SRC[1])) {
//
//                    source_list0(sender, args);
//                    return;
//
//                }
//                //cloader source enable
//                if (args[1].equalsIgnoreCase(TC_SRC[2])) {
//
//                    source_enable0(sender, args);
//                    return;
//
//                }
//                //cloader source disable
//                if (args[1].equalsIgnoreCase(TC_SRC[3])) {
//
//                    source_disable0(sender, args);
//                    return;
//
//                }
//                //cloader source info
//                if (args[1].equalsIgnoreCase(TC_SRC[4])) {
//
//                    source_info0(sender, args);
//                    return;
//
//                }
//            }
//            msg.sendStatic(sender, "COMMAND.USAGE.SOURCE.USAGE");
//        }
//
//        private void source_compile0(CommandSender sender, String[] args) {
//            int l = args.length - 1;
//
//            if (l >= 2) {
//
//                //Find folder
//                List<File> files = new ArrayList<>();
//
//                for (String path : args[2].split(";")) {
//                    try {
//                        files.addAll(Arrays.asList(parseSourceFile0(path)));
//                    } catch (CommandFileNotFoundException e) {
//                        msg.sendStatic(sender, "MAIN.FILE.DOESNT_EXIST", e.input);
//                    }
//                }
//
//                CLoader.getInstance().getThreadPool().execute(() -> {
//                    String[] msg = Module.getJava().compileSrc(files.toArray(new File[0]), ArrayUtil.subArray(args, 3));
//                    Main.msg.send(sender, msg);
//                });
//                return;
//            }
//            msg.sendStatic(sender, "COMMAND.USAGE.SOURCE.COMPILE");
//        }
//
//        private void source_list0(CommandSender sender, String[] args) {
//            int l = args.length - 1;
//
//            if (l >= 2) {
//
//                //Parse page num
//                int page = (l >= 3) ? parseInt(args[3], true) : 1;
//
//                //Find folder
//                File[] files = parseSourceFile0(args[2]);
//
//                //cloader class list *file* *page*
//                msg.send(sender, ComponentUtil.listItems(
//                        page,
//                        15,
//                        Arrays.asList(files),
//                        (src) -> {
//                            for (File f : src) {
//                                if (f.isDirectory()) return true;
//                            }
//                            return false;
//                        },
//                        msg.getList("MAIN.FILE.dotJAVA")[0],
//                        msg.getList("MAIN.FILE.dotJAVA")[0] + " in folder " + args[2],
//                        (index, item) -> new Component.PlainText("§l" + ComponentUtil.Listable.fill(index) + ". ")
//                                .addExtra(new Component.PlainText().addExtra(
//                                        ComponentUtil.dyePath(FileManager.relativePath(FileManager.SOURCE, item)))
//                                        .setClickEvent(new Clickable.RunCommand("/cloader source info "
//                                                + FileManager.relativePath(FileManager.SOURCE, item)))
//                                        .setHoverEvent(new Hoverable.ShowText(new Component.PlainText("§eView file info")))),
//                        new ComponentUtil.StandardBottom("/cloader source list " + args[2] + ' ')));
//                return;
//            }
//            msg.sendStatic(sender, "COMMAND.USAGE.SOURCE.LIST");
//        }
//
//        private void source_enable0(CommandSender sender, String[] args) {
//            int l = args.length - 1;
//
//            List<File> success = new ArrayList<>();
//
//            for (int i = 2; i < args.length; i++) {
//                File f;
//
//                if (args[i].equals("*")) {
//                    for (File ff : FileManager.SOURCE.listFiles((a, b) -> b.startsWith("-"))) {
//                        ff.renameTo(new File(ff.getParentFile(), ff.getName().substring(1)));
//                    }
//                    msg.sendStatic(sender, "MAIN.FILE.ENABLE", msg.getList("MAIN.FILE.ALL"));
//                    continue;
//                }
//
//                try {
//                    f = getFileFromSource(FileManager.SOURCE, args[i]);
//                } catch (CommandFileNotFoundException e) {
//                    msg.sendStatic(sender, "MAIN.FILE.DOESNT_EXIST", e.input);
//                    continue;
//                }
//
//                success.add(f);
//
//                File f1 = new File(f.getParent(), f.getName().substring(1));
//                if (f.getName().startsWith("-")) f.renameTo(f1);
//            }
//
//            msg.sendStatic(sender, "MAIN.FILE.ENABLE", Message.listFileCrafter0(3, success.toArray(new File[0])));
//        }
//
//        private void source_disable0(CommandSender sender, String[] args) {
//            int l = args.length - 1;
//
//            List<File> success = new ArrayList<>();
//
//            for (int i = 2; i < args.length; i++) {
//                File f;
//
//                if (args[i].equals("*")) {
//                    for (File ff : FileManager.SOURCE.listFiles((a, b) -> !b.startsWith("-"))) {
//                        ff.renameTo(new File(ff.getParentFile(), '-' + ff.getName()));
//                    }
//                    msg.sendStatic(sender, "MAIN.FILE.DISABLE", Message.fromPath("MAIN.FILE.ALL"));
//                    continue;
//                }
//
//                try {
//                    f = getFileFromSource(FileManager.SOURCE, args[i]);
//                } catch (CommandFileNotFoundException e) {
//                    msg.sendStatic(sender, "MAIN.FILE.DOESNT_EXIST", e.input);
//                    continue;
//                }
//
//                success.add(f);
//
//                File f1 = new File(f.getParent(), "-" + f.getName());
//                if (!f.getName().startsWith("-")) f.renameTo(f1);
//            }
//
//            msg.sendStatic(sender, "MAIN.FILE.DISABLE", Message.listFileCrafter0(3, success.toArray(new File[0])));
//        }
//
//        private void source_info0(CommandSender sender, String[] args) {
//            int l = args.length - 1;
//
//            if (l == 2) {
//                //Find folder
//                File[] files = parseSourceFile0(args[2]);
//                if (files.length != 1) throw new CommandFileNotFoundException(args[2]);
//
//                Message.send(sender, sourceFileInfo0(files[0]));
//                return;
//            }
//            msg.sendStatic(sender, "COMMAND.USAGE.SOURCE.INFO");
//        }
//
//
//
//
//        private void class0(CommandSender sender, String[] args) {
//            int l = args.length - 1;
//
//            if (l >= 1) {
//                //cloader class list
//                if (args[1].equalsIgnoreCase(TC_CLS[0])) {
//
//                    class_list0(sender, args);
//                    return;
//
//                }
//                //cloader class load
//                else if (args[1].equalsIgnoreCase(TC_CLS[1])) {
//
//                    class_load0(sender, args);
//                    return;
//
//                }
//                //cloader class unload
//                else if (args[1].equalsIgnoreCase(TC_CLS[2])) {
//
//                    class_unload0(sender, args);
//                    return;
//
//                }
//                //cloader class reload
//                else if (args[1].equalsIgnoreCase(TC_CLS[3])) {
//
//                    class_reload0(sender, args);
//                    return;
//
//                }
//                //cloader class enable
//                else if (args[1].equalsIgnoreCase(TC_CLS[4])) {
//
//                    class_enable0(sender, args);
//                    return;
//
//                }
//                //cloader class disable
//                else if (args[1].equalsIgnoreCase(TC_CLS[5])) {
//
//                    class_disable0(sender, args);
//                    return;
//
//                }
//                //cloader class run
//                else if (args[1].equalsIgnoreCase(TC_CLS[6])) {
//
//                    class_run0(sender, args);
//                    return;
//
//                }
//                //cloader class info
//                else if (args[1].equalsIgnoreCase(TC_CLS[7])) {
//
//                    class_info0(sender, args);
//                    return;
//
//                }
//            }
//            msg.sendStatic(sender, "COMMAND.USAGE.CLASS.USAGE");
//        }
//
//        private void class_list0(CommandSender sender, String[] args) {
//            int l = args.length - 1;
//
//            if (l >= 2) {
//
//                Class<?>[] list;
//                try {
//                    list = getClasses(args[2]);
//                } catch (ClassNotFoundException e) {
//                    msg.sendStatic(sender, "MAIN.CLASS.NOT_FOUND", args[2]);
//                    return;
//                }
//
//                //Parse page num
//                int page = (l >= 3) ? parseInt(args[3], true) : 1;
//
//                //List loaded class
//                Message.send(sender, ComponentUtil.listItems(
//                        page,
//                        15,
//                        Arrays.asList(list),
//                        Message.fromPath("MAIN.FILE.dotCLASS")[0],
//                        Message.fromPath("MAIN.CLASS.LOADED")[0],
//                        (index, item) -> new Component.PlainText("§l" + ComponentUtil.Listable.fill(index) + ". ")
//                                .addExtra(new Component.PlainText().addExtra(new Component.PlainText(item.getTypeName()))
//                                        .setClickEvent(new Clickable.RunCommand("/cloader class info " + item.getTypeName()))
//                                        .setHoverEvent(new Hoverable.ShowText(new Component.PlainText("§eView class info")))),
//                        new ComponentUtil.StandardBottom("/cloader class list ")));
//                return;
//            }
//            msg.sendStatic(sender, "COMMAND.USAGE.CLASS.LIST");
//        }
//
//        private void class_load0(CommandSender sender, String[] args) {
//            int l = args.length - 1;
//
//            if (l >= 2) {
//
//                List<File> files = new ArrayList<>();
//
//                for (int i = 2; i < args.length; i++) {
//                    //Find folder
//                    try {
//                        files.addAll(Arrays.asList(getClassFiles(args[i])));
//                    } catch (CommandFileNotFoundException e) {
//                        msg.sendStatic(sender, "MAIN.CLASS.NOT_FOUND", e.input);
//                    }
//                }
//
//                //Load class
//                String[] msg = Module.getJava().load(files.toArray(new File[0]));
//                Message.send(sender, msg);
//            }
//        }
//
//        private void class_unload0(CommandSender sender, String[] args) {
//            int l = args.length - 1;
//
//            if (l >= 2) {
//
//                List<Class<?>> cls = new ArrayList<>();
//
//                for (int i = 2; i < args.length; i++) {
//                    try {
//                        cls.addAll(Arrays.asList(getClasses(args[i])));
//                    } catch (ClassNotFoundException e) {
//                        msg.sendStatic(sender, "MAIN.CLASS.NOT_FOUND", args[i]);
//                    }
//                }
//                String[] msg = Module.getJava().unload(cls.toArray(new Class[0]));
//                Message.send(sender, msg);
//                return;
//            }
//            msg.sendStatic(sender, "COMMAND.USAGE.CLASS.UNLOAD.UNLOADED");
//        }
//
//        private void class_reload0(CommandSender sender, String[] args) {
//            int l = args.length - 1;
//
//            if (l >= 2) {
//                List<Class<?>> cls = new ArrayList<>();
//
//                for (int i = 2; i < args.length; i++) {
//                    try {
//                        cls.addAll(Arrays.asList(getClasses(args[i])));
//                    } catch (ClassNotFoundException e) {
//                        msg.sendStatic(sender, "MAIN.CLASS.NOT_FOUND", args[i]);
//                    }
//                }
//
//                msg.sendStatic(sender, "MAIN.CLASS.RELOAD.START", Message.listClassCrafter0(3, cls.toArray(new Class[0])));
//
//                String[] msg = Module.getJava().reload(cls.toArray(new Class[0]));
//                Message.send(sender, msg);
//                return;
//            }
//            msg.sendStatic(sender, "COMMAND.USAGE.CLASS.RELOAD");
//        }
//
//        private void class_enable0(CommandSender sender, String[] args) {
//            int l = args.length - 1;
//
//            if (l >= 2) {
//                List<File> enabled = new ArrayList<>();
//                for (int i = 2; i < args.length; i++) {
//                    File f;
//
//                    if (args[i].equals("*")) {
//                        for (File ff : FileManager.COMPILED.listFiles((a, b) -> b.startsWith("-"))) {
//                            ff.renameTo(new File(ff.getParentFile(), ff.getName().substring(1)));
//                            enabled.add(ff);
//                        }
//                        Module.getJava().load(FileManager.getClasses());
//                        msg.sendStatic(sender, "MAIN.FILE.ENABLE", Message.fromPath("MAIN.FILE.ALL"));
//                        continue;
//                    }
//
//                    try {
//                        f = getFileFromClass(FileManager.COMPILED, args[i]);
//                    } catch (CommandFileNotFoundException e) {
//                        msg.sendStatic(sender, "MAIN.FILE.DOESNT_EXIST", e.input);
//                        continue;
//                    }
//
//                    File f1 = new File(f.getParent(), f.getName().substring(1));
//                    if (f.getName().startsWith("-")) f.renameTo(f1);
//
//                    enabled.add(f);
//
//                    File[] files;
//
//                    if (f1.isDirectory()) {
//                        files = FileManager.getFiles(f1, ".class");
//                    } else {
//                        try {
//                            ClassFile ff = new ClassFile(f1);
//                            files = ff.getRelatedClass();
//                        } catch (FileNotFoundException | InvalidClassException e) {
//                            throw new RuntimeException(e);
//                        }
//                    }
//
//                    if (files.length > 0) Module.getJava().load(files);
//                }
//
//                msg.sendStatic(sender, "MAIN.FILE.ENABLE", Message.listFileCrafter0(3, enabled.toArray(new File[0])));
//                return;
//            }
//            msg.sendStatic(sender, "COMMAND.USAGE.CLASS.ENABLE");
//        }
//
//        private void class_disable0(CommandSender sender, String[] args) {
//            int l = args.length - 1;
//
//            if (l >= 2) {
//                List<File> disabled = new ArrayList<>();
//                for (int i = 2; i < args.length; i++) {
//                    File f;
//
//                    if (args[i].equals("*")) {
//                        Module.getJava().unload(Module.getJava().getExternalClasses().toArray(new Class[0]));
//                        for (File ff : FileManager.COMPILED.listFiles((a, b) -> !b.startsWith("-"))) {
//                            ff.renameTo(new File(ff.getParentFile(), '-' + ff.getName()));
//                            disabled.add(ff);
//                        }
//                        msg.sendStatic(sender, "MAIN.FILE.DISABLE", Message.fromPath("MAIN.FILE.ALL"));
//                        continue;
//                    }
//
//                    try {
//                        f = getFileFromClass(FileManager.COMPILED, args[i]);
//                    } catch (CommandFileNotFoundException e) {
//                        continue;
//                    }
//
//                    if (f.isDirectory()) {
//                        String pkg = args[i].substring(0, args[i].length() - 1);
//                        List<Class<?>> lst = new ArrayList<>();
//                        for (Class<?> c : Module.getJava().getExternalClasses()){
//                            if (c.getTypeName().startsWith(pkg)) lst.add(c);
//                        }
//                        if (lst.size() > 0) Module.getJava().unload(lst.toArray(new Class[0]));
//                    } else {
//                        Module.getJava().unload(Module.getJava().getExternalClass(args[i]));
//                    }
//
//                    File f1 = new File(f.getParent(), "-" + f.getName());
//                    if (!f.getName().startsWith("-")) f.renameTo(f1);
//                    disabled.add(f);
//                }
//
//                msg.sendStatic(sender, "MAIN.FILE.DISABLE", Message.listFileCrafter0(3, disabled.toArray(new File[0])));
//                return;
//            }
//            msg.sendStatic(sender, "COMMAND.USAGE.CLASS.DISABLE");
//        }
//
//        private void class_run0(CommandSender sender, String[] args) {
//            int l = args.length - 1;
//
//            if (l >= 3) {
//                Class<?> cls = Module.getJava().getExternalClass(args[2]);
//                if (cls != null) {
//                    for (Method m : Module.getJava().getMethods(cls)) {
//                        if (m.getName().equals(args[3])) {
//                            msg.sendStatic(sender, "MAIN.CLASS.RUN.EXECUTED", m.getName());
//                            Message.switchPrintln(sender);
//                            String[] msg;
//                            if (m.getParameterCount() > 0 && m.getParameterTypes()[0] == CommandSender.class) {
//                                msg =  Module.getJava().run(cls, m, sender, (Object) Arrays.copyOfRange(args, 4, args.length));
//                            } else {
//                                msg = Module.getJava().run(cls, m, (Object) Arrays.copyOfRange(args, 4, args.length));
//                            }
//                            Message.initPrintln();
//                            if (msg.length > 0) Message.send(sender, msg);
//                            return;
//                        }
//                    }
//                    msg.sendStatic(sender, "MAIN.CLASS.METHOD_NOT_FOUND", args[3]);
//                    return;
//                }
//                msg.sendStatic(sender, "MAIN.CLASS.NOT_FOUND", args[2]);
//                return;
//            }
//            msg.sendStatic(sender, "COMMAND.USAGE.CALL");
//        }
//
//        private void class_info0(CommandSender sender, String[] args) {
//            int l = args.length - 1;
//
//            if (l == 2) {
//                Class<?> cls = Module.getJava().getExternalClass(args[2]);
//                if (cls != null) {
//
//                    Message.send(sender, classFileInfo0(cls));
//
//                    return;
//                } else {
//                    msg.sendStatic(sender, "MAIN.CLASS.NOT_FOUND", args[2]);
//                    return;
//                }
//            }
//            msg.sendStatic(sender, "COMMAND.USAGE.CLASS.INFO");
//        }
//
//
//
//        //TODO: make this!
//        private void script0(CommandSender sender, String[] args) {
//            int l = args.length - 1;
//
//
//        }
//
//
//
//        //TODO: module removal logic, Scala installation problem. Jython's good.
//        private void module0(CommandSender sender, String[] args) {
//            int l = args.length - 1;
//
//            if (l >= 2) {
//
//                //cls module download
//                if (args[1].equalsIgnoreCase(TC_MOD[0])) {
//                    CLoader.getInstance().getThreadPool().execute(() -> {
//                        for (ModuleType type : ModuleType.externals()) {
//                            if (args[2].equalsIgnoreCase(type.getName())) {
//                                if (Module.getInstalled().contains(type)) {
//                                    msg.sendStatic(sender, "MAIN.MODULE.ALREADY_INSTALLED", type.getName());
//                                    return;
//                                }
//                                //download
//                                if (l == 4 && args[3].equals("-ver")) {
//                                    Module.install(sender, type, args[4]);
//                                    return;
//                                }
//                                //get list
//                                int pg;
//                                if (l >= 3) pg = parseInt(args[3], true);
//                                else {
//                                    pg = 1;
//                                    msg.sendStatic(sender, "MAIN.MODULE.QUERY");
//                                }
//                                try {
//                                    String[] version = ExternalModule.acquireVersionList(type);
//                                    Message.send(sender, ComponentUtil.listItems(
//                                            pg,
//                                            20,
//                                            Arrays.asList(version),
//                                            "/null/",
//                                            Message.fromPath("MAIN.MODULE.ALL_VERSIONS", type.getName())[0],
//                                            (index, item) -> new Component.PlainText("§l" + ComponentUtil.Listable.fill(index) + ". ")
//                                                        .addExtra(new Component.PlainText().addExtra(new Component.PlainText(type.getName() + ' ' + item))
//                                                                .setClickEvent(new Clickable.RunCommand("/cl module download " + args[2] + " -ver " + item))
//                                                                .setHoverEvent(new Hoverable.ShowText(new Component.PlainText("§9Download this version")))),
//                                            new ComponentUtil.StandardBottom("/cl module download " + args[2] + ' ')));
//                                    return;
//                                } catch (IOException e) {
//                                    Message.sendStatic("MAIN.MODULE.QUERY_FAILED",sender);
//                                    return;
//                                }
//                            }
//                        }
//                        msg.sendStatic(sender, "MAIN.MODULE.NOT_FOUND", args[2]);
//                    });
//                    return;
//                }
//
//                //cls module remove
//                if (args[1].equalsIgnoreCase(TC_MOD[1])) {
//
//                }
//            }
//            msg.sendStatic(sender, "COMMAND.USAGE.MODULE.USAGE");
//        }
//    }

//    private static List<String> sourceFileFolderPath0() {
//        File[] file = FileManager.getFileAndFolders(FileManager.SOURCE, ".java");
//        return filePathConvert0(file);
//    }
//
//    private static List<String> sourceFilePath0() {
//        File[] file = FileManager.getFiles(FileManager.SOURCE, ".java");
//        return filePathConvert0(file);
//    }
//
//    private static List<String> sourceFolderPath0() {
//        File[] file = FileManager.getFolders(FileManager.SOURCE);
//        return filePathConvert0(file);
//    }
//
//    private static List<String> sourceDisabledPath0() {
//        File[] file = FileManager.getDisabled(FileManager.SOURCE, ".java");
//        return filePathConvert0(file);
//    }
//
//    private static List<String> filePathConvert0(File... files) {
//        String[] str = new String[files.length + 1];
//        str[0] = "*";
//        int i = 1;
//        for (File f : files) {
//            String src = f.getAbsolutePath()
//                    .substring(f.getAbsolutePath().lastIndexOf("classes" + File.separatorChar + "src"))
//                    .substring(11);
//            str[i++] = src.replace(File.separatorChar, '/');
//        }
//        return new ArrayList<>(Arrays.asList(str));
//    }




//    private static @NotNull Class<?>[] getClasses(@NotNull String name) throws ClassNotFoundException {
//        if (name.equals("*")) return Module.getJava().getExternalClasses().toArray(new Class[0]);
//        String s = name.contains("*") ? name.substring(0, name.lastIndexOf("*")) : name;
//        List<Class<?>> lst = new ArrayList<>();
//        for (Class<?> c : Module.getJava().getExternalClasses()) {
//            String n = c.getTypeName();
//            if (n.startsWith(s)) lst.add(c);
//        }
//        if (lst.isEmpty()) throw new ClassNotFoundException(name);
//        return lst.toArray(new Class[0]);
//    }
//
//    private static @NotNull File[] getClassFiles(@NotNull String name) {
//        try {
//            File base = FileManager.getClassFile(name);
//            if (base.isDirectory()) {
//                return FileManager.getFiles(base, ".class");
//            } else {
//                ClassFile cf = new ClassFile(base);
//                return cf.getRelatedClass();
//            }
//        } catch (FileNotFoundException | InvalidClassException e) {
//            throw new CommandFileNotFoundException(name);
//        }
//    }



//    private static @NotNull File[] parseSourceFile0(String path) {
//        File f = getFileFromSource(FileManager.SOURCE, path);
//        if (f.isDirectory()) return FileManager.getFiles(f, ".java");
//        else return new File[]{f};
//    }




    private static @NotNull Component[] getClassFileInfo(@NotNull Class<?> clazz) {
        File f;
        try {
            f = ClassFile.getClassFile(clazz);
        } catch (InvalidClassException e) {
            throw new RuntimeException(e);
        }
        Component[] info = fileInfo(f, "Java bytecode (.class)");
        int cursor = info.length;
        Component[] text = new Component[cursor + 4];

        System.arraycopy(info, 0, text, 0, cursor);
        text[cursor++] = Component.of(msg.getSingle("MAIN.FILE.INFO.CLASS.NAME",
                clazz.getCanonicalName()));
        text[cursor++] = Component.of(msg.getSingle("MAIN.FILE.INFO.CLASS.INTERFACE"))
                .append(enumItem(3, clazz.getInterfaces(), Class::getSimpleName));
        text[cursor++] = Component.of(msg.getSingle("MAIN.FILE.INFO.CLASS.FIELD"))
                .append(enumItem(3, clazz.getDeclaredFields(), Field::toGenericString));
        text[cursor] = Component.of(msg.getSingle("MAIN.FILE.INFO.CLASS.METHOD"))
                .append(enumItem(3, clazz.getDeclaredMethods(), Method::toGenericString));
        return text;
    }

    private static @NotNull Component[] getSourceFileInfo(@NotNull File src) {
        Component[] info = fileInfo(src, "Java source file (.java)");
        int cursor = info.length;
        Component[] text = new Component[cursor + 1];

        System.arraycopy(info, 0, text, 0, cursor);
        text[cursor] = Component.of(msg.getSingle("MAIN.FILE.INFO.JAVA.CLASS", FileManager.getCompiledName(src)));
        return text;
    }

    private static <T> @NotNull Component[] enumItem(int max, @NotNull T[] item, Function<T, String> stringStrategy) {
        List<Component> json = new ArrayList<>();
        for (int i = 0; i < Math.min(item.length, max); i++) {
            json.add(Component.of(msg.getSingle("MAIN.FILE.INFO.CLASS.LIST", stringStrategy.apply(item[i]))));
        }
        if (max < item.length) json.add(Component.of(msg.getSingle("MAIN.FILE.INFO.CLASS.LIST", "...+" + (item.length - max))));
        return json.toArray(new Component[0]);
    }

//    private static @NotNull File getFile(File rt, String path) {
//        File f = new File(rt, path.startsWith("/") ? path.substring(1) : path);
//        if (!f.exists()) throw new CommandFileNotFoundException(path);
//        return f;
//    }




//    private static @NotNull File getFileFromClass(File rt, String path) {
//        if (path.equals("*")) return rt;
//        path = path.replace('.', File.separatorChar);
//        if (path.endsWith("*")) path = path.substring(0, path.length() - 2);
//        else path = path.concat(".class");
//        File f = new File(rt, path);
//        if (f.exists()) return f;
//        throw new CommandFileNotFoundException(path);
//    }
//
//    private static @NotNull File getFileFromSource(File rt, String path) {
//        if (path.equals("*")) return rt;
//        path = path.substring(1).replace('/', File.separatorChar);
//        File f = new File(rt, path);
//        if (f.exists()) return f;
//        throw new CommandFileNotFoundException(path);
//    }

    private static @NotNull Component[] fileInfo(@NotNull File file, @Nullable String type) {
        Component[] text = new Component[6];
        text[0] = Component.of(msg.getSingle("MAIN.FILE.INFO.HEADER", file.getName()));
        text[1] = Component.of(msg.getSingle("MAIN.FILE.INFO.TYPE", type == null ? FileManager.type(file) : type));
        text[2] = Component.of(msg.getSingle("MAIN.FILE.INFO.PATH", FileManager.relativePath(FileManager.ROOT, file)));
        text[3] = Component.of(msg.getSingle("MAIN.FILE.INFO.SIZE", "" + file.length()));
        text[4] = Component.of(msg.getSingle("MAIN.FILE.INFO.LAST_MODIFIED", FileManager.lastModifiedDate(file)));
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
        text[5] = Component.of(msg.getSingle("MAIN.FILE.INFO.MD5", md5));
        return text;
    }

    private static @NotNull Component[] sysInfo() {
        Component[] text = new Component[14];
        text[0] = Component.of(msg.getSingle("MAIN.CLASS.SYSINFO.START"));
        text[1] = Component.of(msg.getSingle("MAIN.CLASS.SYSINFO.JAVA", System.getProperty("java.version")));
        text[2] = Component.of(msg.getSingle("MAIN.CLASS.SYSINFO.OS", System.getProperty("os.name")));
        text[3] = Component.of(msg.getSingle("MAIN.CLASS.SYSINFO.PID", ManagementFactory.getRuntimeMXBean().getName().split("@")[0]));
        text[4] = Component.create();

        double[] sys = CLoader.getInstance().getRuntimeExecutor().systemCpuUsed();
        double[] proc = CLoader.getInstance().getRuntimeExecutor().processCpuUsed();

        NumberFormat d1 = StringFormatUtil.getDigit1Format();
        NumberFormat d2 = StringFormatUtil.getDigit2Format();

        text[5] = Component.of(msg.getSingle("MAIN.CLASS.SYSINFO.CPU"));
        text[6] = Component.of(msg.getSingle("MAIN.CLASS.SYSINFO.CPU_SYSTEM", d1.format(sys[0]), d1.format(sys[1]), d1.format(sys[2]), d1.format(sys[3])));
        text[7] = Component.of(msg.getSingle("MAIN.CLASS.SYSINFO.CPU_PROCESS", d1.format(proc[0]), d1.format(proc[1]), d1.format(proc[2]), d1.format(proc[3])));
        text[8] = Component.create();
        text[9] = Component.of(msg.getSingle("MAIN.CLASS.SYSINFO.MEMORY"));
        sys = new double[]{((FileManager.OSINFO.getTotalPhysicalMemorySize() - FileManager.OSINFO.getFreePhysicalMemorySize()) >> 20) / 1024.0,
                (FileManager.OSINFO.getTotalPhysicalMemorySize() >> 20) / 1024.0};
        proc = new double[]{(Runtime.getRuntime().totalMemory() >> 20) / 1024.0,
                (Runtime.getRuntime().maxMemory() >> 20) / 1024.0};
        text[10] = Component.of(msg.getSingle(
                "MAIN.CLASS.SYSINFO.MEMORY_SYSTEM", d2.format(sys[0]), d2.format(sys[1]), d1.format(((sys[1] - sys[0]) / sys[1]) * 100)));
        text[11] = Component.of(msg.getSingle(
                "MAIN.CLASS.SYSINFO.MEMORY_PROCESS", d2.format(proc[0]), d2.format(proc[1]), d1.format(((proc[1] - proc[0]) / proc[1]) * 100)));
        text[12] = Component.create();
        sys = new double[]{((FileManager.DISK.getTotalSpace() - FileManager.DISK.getFreeSpace()) >> 20) / 1024.0,
                (FileManager.DISK.getTotalSpace() >> 20) / 1024.0};
        text[13] = Component.of(msg.getSingle("MAIN.CLASS.SYSINFO.DISK", d2.format(sys[0]), d2.format(sys[1]), d1.format(((sys[1] - sys[0]) / sys[1]) * 100)));

        return text;
    }
}

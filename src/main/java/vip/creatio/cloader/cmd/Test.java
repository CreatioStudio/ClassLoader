package vip.creatio.cloader.cmd;

import vip.creatio.cloader.ccl.FileManager;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import org.bukkit.command.PluginCommand;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class Test {

    private static final String name = "loadertest";
    private static final String description = "Main command of ClassLoader.";
    private static final List<String> aliases = Arrays.asList();
    private static final JShell SHELL = JShell.create();

    //No default constructor
    private Test() {}

    public static PluginCommand register() {
        PluginCommand cmd = CommandRegister.create(name);
        cmd.setDescription(description);
        cmd.setAliases(aliases);
        cmd.setPermission(null);
        cmd.setPermissionMessage(null);
        Arrays.stream(FileManager.getBukkitJars()).map(File::getAbsolutePath).forEach(SHELL::addToClasspath);
        cmd.setExecutor((sender, command, label, args) -> {
            sender.sendMessage("CL Test Executed!");
            StringBuilder sb = new StringBuilder();
            Arrays.stream(args).peek(sb::append).forEach(s -> sb.append(' '));
            sb.trimToSize();
            String str = sb.toString();
            List<SnippetEvent> events = SHELL.eval(str);
            for (SnippetEvent e : events) {
                StringBuilder sbuilder = new StringBuilder();
                if (e.causeSnippet() == null) {
                    //  我们有一个片段创建的事件
                    switch (e.status()) {//根据代码片段的执行进行输出
                        case VALID:
                            sbuilder.append("Successful ");
                            break;
                        case RECOVERABLE_DEFINED:
                            sbuilder.append("With unresolved references ");
                            break;
                        case RECOVERABLE_NOT_DEFINED:
                            sbuilder.append("Possibly reparable, failed  ");
                            break;
                        case REJECTED:
                            sbuilder.append("Failed ");
                            break;
                    }
                    if (e.previousStatus() == Snippet.Status.NONEXISTENT) {
                        sbuilder.append("addition");
                    } else {
                        sbuilder.append("modification");
                    }
                    sbuilder.append(" of ");
                    sbuilder.append(e.snippet().source());
                    System.out.println(sbuilder);
                    if (e.value() != null) {
                        System.out.printf("Value is: %s\n", e.value());
                    }
                    System.out.flush();
                }
            }
            /*try {
                SimplePluginManager mgr = (SimplePluginManager) Bukkit.getPluginManager();
                Plugin p = ClassLoader.getInstance();
                mgr.disablePlugin(p);
                Field f1 = SimplePluginManager.class.getDeclaredField("lookupNames");
                f1.setAccessible(true);
                ((Map<String, Plugin>) f1.get(mgr)).remove(p.getName().replace(' ', '_'));
                HandlerList.unregisterAll(p);
                Field f2 = SimplePluginManager.class.getDeclaredField("fileAssociations");
                f2.setAccessible(true);
                Map<Pattern, PluginLoader> map = (Map<Pattern, PluginLoader>) f2.get(mgr);
                for (Map.Entry<Pattern, PluginLoader> e : map.entrySet()) {
                    if (e.getValue().equals(p.getPluginLoader())) {
                        map.remove(e.getKey());
                        break;
                    }
                }
                System.gc();


            } catch (Exception e) {
                e.printStackTrace();
            }*/
            return true;
        });
        return cmd;
    }
}

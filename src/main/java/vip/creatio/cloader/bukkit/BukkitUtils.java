package vip.creatio.cloader.bukkit;

import vip.creatio.accessor.Func;
import vip.creatio.accessor.Reflection;
import vip.creatio.accessor.Unsafe;
import vip.creatio.basic.util.BukkitUtil;
import vip.creatio.cloader.ccl.FileManager;
import vip.creatio.cloader.util.ClassUtil;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import vip.creatio.common.util.ReflectUtil;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BukkitUtils {

    static JavaPlugin getVirtualInstance(CLoader c) {
        JavaPlugin p;
        try {
            p = (JavaPlugin) Unsafe.getUnsafe().allocateInstance(ExternalPlugin.class);
            Method init = ReflectUtil.method(JavaPlugin.class, "init",
                    PluginLoader.class,
                    Server.class,
                    PluginDescriptionFile.class,
                    File.class,
                    File.class,
                    ClassLoader.class);
            InputStream stream = c.getResource("resources/virtual.ymlx");
            PluginDescriptionFile file;

            if (stream == null) file = c.getDescription();
            else file = new PluginDescriptionFile(stream);

            ReflectUtil.invoke(init, p, c.getPluginLoader(), Bukkit.getServer(), file, FileManager.DATA, c.getFile0(), c.getClassLoader0());
        } catch (Throwable t) {
            CLoader.intern("Failed to init ExternalPlugin for ClassLoader! Using fallback...");
            t.printStackTrace();
            p = c;
        }
        return p;
    }

    //All classes related clazz will be unregistered
    public static void unregister(Class<?> clazz) {
        //Unregister all event listeners
        if (ClassUtil.interfaceContains(clazz, Listener.class)) {
            for (HandlerList l : HandlerList.getHandlerLists()) {
                List<Listener> bklist = new ArrayList<>();
                for (RegisteredListener reg : l.getRegisteredListeners()) {
                    if (ClassUtil.isInnerClass(clazz, reg.getListener().getClass()))
                        bklist.add(reg.getListener());
                }

                for (Listener ll : bklist) {
                    l.unregister(ll);
                }
            }
        }
        //Unregister all commands
        for (Command cmd : BukkitUtil.getCommandMap().getCommands()) {
            try {
                if (cmd instanceof PluginCommand) {
                    if (ClassUtil.isInnerClass(clazz, cmd.getClass()))
                        BukkitUtil.unregisterCommand(cmd);
                    if (ClassUtil.isInnerClass(clazz, ((PluginCommand) cmd).getExecutor().getClass()))
                        BukkitUtil.unregisterCommand(cmd);
                    if (((PluginCommand) cmd).getTabCompleter() != null) {
                        if(ClassUtil.isInnerClass(clazz, ((PluginCommand) cmd).getTabCompleter().getClass()))
                            BukkitUtil.unregisterCommand(cmd);
                    }
                }
            } catch (NullPointerException ignored) {}
        }
        //Cancel all tasks
        Bukkit.getScheduler().cancelTasks(CLoader.getInstance().getVirtual());
    }
}

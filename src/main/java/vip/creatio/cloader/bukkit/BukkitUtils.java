package vip.creatio.cloader.bukkit;

import vip.creatio.accessor.Func;
import vip.creatio.accessor.Reflection;
import vip.creatio.clib.basic.util.BukkitUtil;
import vip.creatio.cloader.ccl.FileManager;
import vip.creatio.cloader.cmd.CommandRegister;
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
import vip.creatio.common.ReflectUtil;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BukkitUtils {

    private static final PriorityQueue<BukkitTask> PENDING =
            ReflectUtil.get(Bukkit.getScheduler().getClass(), "pending");
    private static final ConcurrentHashMap<Integer, BukkitTask> RUNNERS =
            ReflectUtil.get(Bukkit.getScheduler().getClass(), "runners");

    public static PriorityQueue<BukkitTask> getScedulerPending() {
        return PENDING;
    }

    public static ConcurrentHashMap<Integer, BukkitTask> getSchedulerRunners() {
        return RUNNERS;
    }

    public static Set<BukkitTask> getAllTasks() {
        Set<BukkitTask> task = new HashSet<>();
        task.addAll(PENDING);
        task.addAll(RUNNERS.values());
        return task;
    }

    public final static Func<Class<?>> GET_TASK_CLASS = Reflection.method(BukkitUtil.getCbClass("scheduler.CraftTask"), "getTaskClass");

    public static Class<?> getTaskClass(BukkitTask task) {
        return GET_TASK_CLASS.invoke(task);
    }

    static JavaPlugin getVirtualInstance(CLoader c) {
        JavaPlugin p;
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field f = unsafeClass.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Object unsafe = f.get(null);
            Method allocInst = unsafeClass.getMethod("allocateInstance", Class.class);
            p = (JavaPlugin) allocInst.invoke(unsafe, ExternalPlugin.class);
            Method method = JavaPlugin.class.getDeclaredMethod("init",
                    PluginLoader.class,
                    Server.class,
                    PluginDescriptionFile.class,
                    File.class,
                    File.class,
                    java.lang.ClassLoader.class);
            method.setAccessible(true);
            InputStream stream = c.getResource("resources/virtual.ymlx");
            PluginDescriptionFile file;
            if (stream == null) file = c.getDescription();
            else file = new PluginDescriptionFile(stream);
            method.invoke(p, c.getPluginLoader(), Bukkit.getServer(), file, FileManager.DATA, c.getFile0(), c.getClassLoader0());
        } catch (Exception e) {
            CLoader.intern("Failed to init ExternalPlugin for ClassLoader! Using fallback...");
            e.printStackTrace();
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
        for (Command cmd : CommandRegister.getCommandMap().getCommands()) {
            try {
                if (cmd instanceof PluginCommand) {
                    if (ClassUtil.isInnerClass(clazz, cmd.getClass()))
                        CLoader.getInstance().getPluginCommands().unregister(cmd);
                    if (ClassUtil.isInnerClass(clazz, ((PluginCommand) cmd).getExecutor().getClass()))
                        CLoader.getInstance().getPluginCommands().unregister(cmd);
                    if (((PluginCommand) cmd).getTabCompleter() != null) {
                        if(ClassUtil.isInnerClass(clazz, ((PluginCommand) cmd).getTabCompleter().getClass()))
                            CLoader.getInstance().getPluginCommands().unregister(cmd);
                    }
                }
            } catch (NullPointerException ignored) {}
        }
        //Cancel all tasks
        for (BukkitTask task : getAllTasks()) {
            if (ClassUtil.isInnerClass(clazz, getTaskClass(task))) Bukkit.getScheduler().cancelTask(task.getTaskId());
        }
    }
}

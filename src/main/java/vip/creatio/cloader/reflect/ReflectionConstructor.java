package vip.creatio.cloader.reflect;

import vip.creatio.cloader.msg.Message;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


/**
 * An enum of reflection constructor, basically reflected from
 * net.minecraft.server package and craftbukkit package.
 *
 * Get the constructor of an enum: etc. XXX.c
 *
 * @variable c the Constructor represented by enum.
 */

public enum ReflectionConstructor {

    PluginCommand("org.bukkit.command.PluginCommand", String.class, Plugin.class),
    PacketPlayOutChat(ReflectionClass.PacketPlayOutChat, ReflectionClass.IChatBaseComponent.c, ReflectionClass.ChatMessageType.c, UUID.class),

    ;

    public Constructor<?> c;

    ReflectionConstructor(Class<?> clazz, Class<?>... params) {
        try {
            Constructor<?> c = clazz.getDeclaredConstructor(params);
            c.setAccessible(true);
            this.c = c;
        } catch (NoSuchMethodException e) {
            Message.internal("&4Registration of constructor of class &6&l" + clazz.getSimpleName() + "&4 failed!");
            e.printStackTrace();
            this.c = null;
        }
    }

    ReflectionConstructor(ReflectionClass refClass, Class<?>... params) {
        this(refClass.c, params);
    }

    ReflectionConstructor(String rawClass, Class<?>... params) {
        this();
        try {
            Constructor<?> c = Class.forName(rawClass).getDeclaredConstructor(params);
            c.setAccessible(true);
            this.c = c;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            Message.internal("&4Registration of constructor of class from string &6&l" + rawClass + "&4 failed!");
            throw new RuntimeException(e);
        }
    }

    ReflectionConstructor() {this.c = null;}

    /**
     * Run a constructor from a class using specific types of param.
     * The constructor will be automatically selected using types of param.
     * Number, Char, and Boolean will be converted to their primitive.
     *
     * This automatically bypass the access check.
     *
     * Return an instance created by the constructor as an object
     *
     * @param clazz the class to which constructor belongs.
     *
     * @param args params to be used in the constructor.
     *
     * @return Object
     */
    public static Object run(Class<?> clazz, Object... args) {
        try {
            if (args.length == 1) {
                return get(clazz).newInstance(args);
            } else {
                List<Class<?>> list = new ArrayList<>();
                for (Object o : args) {
                    list.add(ReflectionUtils.toPrimitive(o.getClass()));
                }
                return get(clazz, list.toArray(new Class[0])).newInstance(args);
            }
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
            Message.internal("&4Construction of instance from class &6&l" + clazz.getSimpleName() + "&4 failed!");
            throw new RuntimeException(e);
        }
    }

    /**
     * Run a constructor with a known constructor.
     * This can be used for constructors with complicated param types.
     * The preset constructor do not require param analyser, which is
     * faster than the one above.
     *
     * This automatically bypass the access check.
     *
     * Return an instance created by the constructor as an object
     *
     * @param constructor the constructor of a class.
     *
     * @param args params to be used in the constructor.
     *
     * @return java.lang.Object
     */
    public static Object run(Constructor<?> constructor, Object... args) {
        try {
            return constructor.newInstance(args);
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
            Message.internal("&4Construction of instance using constructor &6&l" + constructor.getName() + "&4 failed!");
            throw new RuntimeException(e);
        }
    }
    public static Object run(ReflectionConstructor refConstructor, Object... args) {
        return run(refConstructor.c, args);
    }
    public Object run(Object... args) {
        return run(this.c, args);
    }

    /**
     * Get the constructor with specific param type from a class.
     *
     * This automatically bypass the access check.
     *
     * @param clazz the class to which constructor belongs.
     *
     * @param argsClass the class(es) of param types.
     *
     * @return java.lang.reflect.Constructor
     */
    public static Constructor<?> get(Class<?> clazz, Class<?>... argsClass) {
        try {
            Constructor<?> c = clazz.getDeclaredConstructor(argsClass);
            c.setAccessible(true);
            return c;
        } catch (NoSuchMethodException e) {
            Message.internal("&4Failed to find constructor in class &6&l" + clazz.getSimpleName() + "&4 with args " + Arrays.toString(argsClass));
            throw new RuntimeException(e);
        }
    }
}

package vip.creatio.cloader.reflect;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;

public class ReflectionUtils {

    private static final String VERSION = Bukkit.getServer().getClass().getPackage().getName().substring(23);

    public static Class<?> getNmsClass(String name) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + VERSION + "." + name);
    }

    public static Class<?> getCbClass(String name) throws ClassNotFoundException {
        return Class.forName("org.bukkit.craftbukkit." + VERSION + "." + name);
    }

    //get a annotation by reflection
    public static Annotation getAnnotation(Class<?> clazz, Class<Annotation> annotationClass) {
        return clazz.getAnnotation(annotationClass);
    }

    //Primitive conversion
    static Class<?> toPrimitive(Class<?> wrapClass) {
        switch (wrapClass.getCanonicalName()) {
            case "java.lang.Byte":
                return byte.class;
            case "java.lang.Integer":
                return int.class;
            case "java.lang.Short":
                return short.class;
            case "java.lang.Long":
                return long.class;
            case "java.lang.Float":
                return float.class;
            case "java.lang.Double":
                return double.class;
            case "java.lang.Boolean":
                return boolean.class;
            case "java.lang.Character":
                return char.class;
            case "java.lang.Byte[]":
                return byte[].class;
            case "java.lang.Integer[]":
                return int[].class;
            case "java.lang.Short[]":
                return short[].class;
            case "java.lang.Long[]":
                return long[].class;
            case "java.lang.Float[]":
                return float[].class;
            case "java.lang.Double[]":
                return double[].class;
            case "java.lang.Boolean[]":
                return boolean[].class;
            case "java.lang.Character[]":
                return char[].class;
        }
        return wrapClass;
    }

    //Bukkit Player -> NMS Entity Player
    public static Object getNmsPlayer(@NotNull Player p) {
        return ReflectionMethod.run(ReflectionMethod.CraftPlayer_getHandle.m, p);
    }

    //Bukkit Server -> CraftServer
    public static Object getCraftServer(@NotNull Server server) {
        return ReflectionClass.CraftServer.c.cast(server);
    }

    //String Json -> NMS IChatBaseComponent
    public static Object getNmsChatComponent(@NotNull String json) {
        return ReflectionMethod.run(ReflectionMethod.IChatBaseComponent_deserialize2, null, json);
    }
}


package vip.creatio.cloader.util;

public class ClassUtil {

    //TODO: replace this
    public static void init() {}

    public static boolean interfaceContains(Class<?> c, Class<?> i) {
        for (Class<?> cls : c.getInterfaces()) {
            if (cls.getTypeName().equals(i.getTypeName())) return true;
        }
        return false;
    }

    public static boolean extendsContains(Class<?> c, Class<?> supers) {
        for (Class<?> cls : c.getClasses()) {
            if (cls.getTypeName().equals(supers.getTypeName())) return true;
        }
        return false;
    }

    public static boolean isInnerClass(Class<?> s, Class<?> i) {
        String s1 = s.getTypeName();
        String i1 = i.getTypeName();
        if (s1.equals(i1)) return true;
        else if (i1.contains("$")) {
            return i1.startsWith(s1) && i1.charAt(s1.length()) == '$';
        }
        return false;
    }
}

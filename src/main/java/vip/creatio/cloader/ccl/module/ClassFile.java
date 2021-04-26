package vip.creatio.cloader.ccl.module;

import vip.creatio.cloader.ccl.FileManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.util.Arrays;

public class ClassFile extends ScriptFile {

    private final ClassFile[] relatedClass;
    private final String classTypeName;

    public ClassFile(@NotNull File clazz) throws FileNotFoundException, InvalidClassException {
        super(clazz);
        if (!checkType(".class")) throw new InvalidClassException(toString());
        String name = getName().substring(0, getName().lastIndexOf("."));
        File[] files = clazz.getParentFile().listFiles(f -> !f.getName().equalsIgnoreCase(clazz.getName()) && !f.isDirectory() && f.getName().startsWith(name));
        ClassFile[] cls = new ClassFile[files.length];
        for (int i = 0; i < files.length; i++) {
            try {
                cls[i] = new ClassFile(files[i]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        relatedClass = cls;
        String s = FileManager
                .relativePath(FileManager.COMPILED, this)
                .substring(1)
                .replace('/', '.');
        classTypeName = (s.endsWith(".class")) ? s.substring(0, s.length() - 6) : s;
    }

    public ClassFile(File parent, @NotNull String child) throws FileNotFoundException, InvalidClassException {
        super(parent, child);
        if (!checkType(".class")) throw new InvalidClassException(toString());
        String name = getName().substring(0, getName().lastIndexOf("."));
        File[] files = parent.listFiles((f, n) -> f != this && !f.isDirectory() && n.startsWith(name));
        ClassFile[] cls = new ClassFile[files.length];
        for (int i = 0; i < files.length; i++) {
            try {
                cls[i] = new ClassFile(files[i]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        relatedClass = cls;
        String s = FileManager
                .relativePath(FileManager.COMPILED, this)
                .substring(1)
                .replace('/', '.');
        classTypeName = (s.endsWith(".class")) ? s.substring(0, s.length() - 6) : s;
    }

    /** Get all related class file, contains this class */
    public ClassFile[] getRelatedClass() {
        ClassFile[] cf = Arrays.copyOf(relatedClass, relatedClass.length + 1);
        cf[cf.length - 1] = this;
        return cf;
    }

    /**
     * Get loaded class related to this file
     *
     * @return null if the class hasn't been loaded
     */
    @Nullable
    public Class<?> getClassObject() {
        return Module.getJava().getExternalClass(getClassName());
    }

    public String getClassName() {
        return classTypeName;
    }

    public static ClassFile getClassFile(String binaryName) throws FileNotFoundException, InvalidClassException {
        return new ClassFile(FileManager.COMPILED, binaryName.replace('.', separatorChar).concat(".class"));
    }

    public static ClassFile getClassFile(Class<?> externalClass) throws InvalidClassException {
        try {
            return new ClassFile(FileManager.COMPILED, externalClass.getTypeName().replace('.', separatorChar).concat(".class"));
        } catch (FileNotFoundException e) {
            //impossible
            throw new RuntimeException(e);
        }
    }
}

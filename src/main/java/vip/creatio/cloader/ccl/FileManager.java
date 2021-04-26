package vip.creatio.cloader.ccl;

import vip.creatio.cloader.bukkit.CLoader;
import vip.creatio.cloader.ccl.module.Module;
import com.sun.management.OperatingSystemMXBean;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import vip.creatio.common.ArrayUtil;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

public class FileManager {

    private static final MessageDigest MD5;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");

    public static final File PLUGINS = new File(Bukkit.getWorldContainer().getParentFile(), "plugins");
    public static final File ROOT = new File(PLUGINS.getAbsolutePath()).getParentFile();
    public static final File CLASS_LOADER = new File(PLUGINS, "ClassLoader");
    public static final File CACHE = new File(ROOT, "cache");
    public static final File DISK;
    public static final File MODULES = new File(ROOT, "libs\\modules");

    public static final File CLASSES = new File(CLASS_LOADER, "classes");
    public static final File SOURCE = new File(CLASSES, "src");
    public static final File LIB = new File(CLASSES, "lib");
    public static final File COMPILED = new File(CLASSES, "compiled");               //Stores all compiled class file
    public static final File DATA = new File(CLASSES, "data");
    public static final File SCRIPT = new File(CLASSES, "script");

    public static OperatingSystemMXBean OSINFO = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    static {

        //Init MD5;
        try {
            MD5 = MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        //Date Format
        DATE_FORMAT.setTimeZone(Calendar.getInstance().getTimeZone());

        //Init file system
        String absPath = ROOT.getAbsolutePath();
        DISK = new File(absPath.substring(0, absPath.indexOf(File.separator) + 1));
        if (!MODULES.exists()) {
            MODULES.mkdirs();
        }
        if (!COMPILED.exists()) {
            COMPILED.mkdirs();
        }
        if (!LIB.exists()) {
            LIB.mkdirs();
        }
        if (!DATA.exists()) {
            DATA.mkdirs();
        }
        if (!SCRIPT.exists()) {
            SCRIPT.mkdirs();
        }
        if (!SOURCE.exists()) {
            SOURCE.mkdirs();
            try {
                File example = new File(SOURCE, "Example.java");
                example.createNewFile();

                InputStream is = CLoader.getInstance().getResource("resources/Example.javax");
                if (is == null) throw new IOException("File Example.javax not found!");
                OutputStream os = new BufferedOutputStream(new FileOutputStream(example, false));

                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }

                is.close();
                os.flush();
                os.close();
            } catch (IOException e) {
                CLoader.intern("Failed to initialize example.java!");
                e.printStackTrace();
            }
        }
    }

    public static File[] getServerJars() {
        return ArrayUtil.append(getBukkitJars(), PLUGINS.listFiles(f -> f.getName().toLowerCase().endsWith(".jar")));
    }

    public static File[] getBukkitJars() {
        return ArrayUtil.append(ROOT.listFiles(f -> f.getName().toLowerCase().endsWith(".jar"))
                , CACHE.listFiles(f -> f.getName().toLowerCase().endsWith(".jar")
                && f.getName().toLowerCase().startsWith("patched")));
    }

    public static File[] getDependencies() {
        File[] f1 = ArrayUtil.append(getServerJars(), LIB.listFiles(f -> f.getName().toLowerCase().endsWith(".jar")));
        return ArrayUtil.append(f1, url2File(Module.getJava().getLoadedDependencies()).toArray(new File[0]));
    }

    public static Set<File> url2File(Collection<URL> urls) {
        Set<File> f = new HashSet<>();
        for (URL url : urls) {
            f.add(new File(url.getFile()));
        }
        return f;
    }

    //Get all .java under CreatioLib/sources, ignores if file name/ dir name starts with "-"
    //etc -example.java
    public static File[] getSources() {
        return getFiles(SOURCE, ".java");
    }

    /** Get compiled class name of a source file */
    public static String getCompiledName(File sourceFile) {
        if (sourceFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().startsWith("package ")) {
                        return line.substring(8, line.length() - 1)
                                + '.'
                                + sourceFile.getName().substring(0, sourceFile.getName().lastIndexOf("."));
                    }
                }
                return sourceFile.getName().substring(0, sourceFile.getName().lastIndexOf("."));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else throw new RuntimeException("File " + sourceFile.getName() + " does not exist!");
    }

    private static void getFiles0(File sourceFile, List<File> sourceList, String type) {
        // Not null check
        if (sourceFile.exists() && sourceList != null) {
            // If sourceFile is a directory
            if (sourceFile.isDirectory()) {
                // If File is directory or java file
                for (File childFile : sourceFile.listFiles((file -> (file.isDirectory() || file.getName().endsWith(type)) && !file.getName().startsWith("-")))) {
                    getFiles0(childFile, sourceList, type);
                }
                // If sourceFile is a realFile
            } else if (sourceFile.getName().endsWith(type) && !sourceFile.getName().startsWith("-")) sourceList.add(sourceFile);
        }
    }

    private static void getFolders0(File sourceFile, List<File> sourceList) {
        if (sourceFile.exists() && sourceList != null) {
            if (sourceFile.isDirectory()) {
                sourceList.add(sourceFile);
                for (File childFile : sourceFile.listFiles((file -> file.isDirectory() && !file.getName().startsWith("-")))) {
                    getFolders0(childFile, sourceList);
                }
            }
        }
    }

    private static void getDisabled0(File sourceFile, List<File> sourceList, String type) {
        if (sourceFile.exists() && sourceList != null) {
            if (sourceFile.isDirectory()) {
                if (sourceFile.getName().startsWith("-")) sourceList.add(sourceFile);
                else for (File childFile : sourceFile.listFiles((file -> (file.isDirectory() || file.getName().endsWith(type))))) {
                    getDisabled0(childFile, sourceList, type);
                }
            } else if (sourceFile.getName().endsWith(type) && sourceFile.getName().startsWith("-")) sourceList.add(sourceFile);
        }
    }

    /** Get all compiled .class files */
    public static File[] getClasses() {
        return getFiles(COMPILED, ".class");
    }

    public static File[] getCompiled() {
        return getFileAndFolders(COMPILED, ".class");
    }

    //get class file through it's type name
    public static File getClassFile(String name) throws FileNotFoundException {
        String path;
        if (name.equals("*")) return COMPILED;
        if (name.contains("*")) {
            path = name.replace("[]","").replace(".", File.separator)
                    .trim();
        } else {
            path = name.replace('.', File.separatorChar).trim().concat(".class");
        }

        File f = new File(COMPILED, path);
        if (!f.exists())
            throw new FileNotFoundException("Path " + name + " does not seemed to exist in compiled folder");
        return f;
    }

    public static List<String> getLoadedClasses() {
        List<String> l = new ArrayList<>();
        Module.getJava().getExternalClasses().stream().map(Class::getTypeName).forEach(l::add);
        return l;
    }

    public static List<String> getUnloadClasses() {
        List<String> c = getAllClasses();
        c.removeAll(getLoadedClasses());
        return c;
    }

    public static List<String> getAllClasses() {
        File[] files = getClasses();
        Set<String> str = new HashSet<>();
        for (File f : files) {
            String path = relativePath(COMPILED, f).replace('/', '.').substring(1);
            str.add(path.substring(0, path.lastIndexOf(".")));
        }
        return new ArrayList<>(str);
    }

    public static List<String> getAllCompiled() {
        File[] files = getCompiled();
        return getCompiled0(files);
    }

    public static List<String> getLoadedCompiled() {
        Set<String> str = new HashSet<>();
        getLoadedClasses().stream()
                .map(s -> (s.contains(".")) ? s.substring(0, s.lastIndexOf(".") + 1) + '*' : s).forEach(str::add);
        return new ArrayList<>(str);
    }

    public static List<String> getUnloadCompiled() {
        List<String> c = getAllClasses();
        c.removeAll(getLoadedClasses());
        Set<String> str = new HashSet<>();
        c.stream().map(s -> (s.contains(".")) ? s.substring(0, s.lastIndexOf(".") + 1) + '*' : s).forEach(str::add);
        c.addAll(str);
        c.remove("*");
        return c;
    }

    @NotNull
    private static List<String> getCompiled0(File[] files) {
        Set<String> str = new HashSet<>();
        for (File f : files) {
            String path = relativePath(COMPILED, f).replace('/', '.').substring(1);
            if (f.isDirectory()) {
                str.add(path + ".*");
            } else {
                str.add(path.substring(0, path.lastIndexOf(".")));
            }
        }
        return new ArrayList<>(str);
    }

    /** Get all disabled classes and packages */
    public static List<String> getDisabledCompiled() {
        File[] files = getDisabled(COMPILED, ".class");
        return getCompiled0(files);
    }

    public static File[] getFiles(File folder, String suffix) {
        List<File> f = new ArrayList<>();
        getFiles0(folder, f, suffix);
        return f.toArray(new File[0]);
    }

    public static File[] getFolders(File folder) {
        List<File> f = new ArrayList<>();
        getFolders0(folder, f);
        f.remove(folder);
        return f.toArray(new File[0]);
    }

    public static File[] getFileAndFolders(File folder, String suffix) {
        List<File> f1 = new ArrayList<>();
        List<File> f2 = new ArrayList<>();
        getFiles0(folder, f1, suffix);
        getFolders0(folder, f2);
        f2.addAll(f1);
        f2.remove(folder);
        return f2.toArray(new File[0]);
    }

    public static File[] getDisabled(File folder, String suffix) {
        List<File> f = new ArrayList<>();
        getDisabled0(folder, f, suffix);
        f.remove(folder);
        return f.toArray(new File[0]);
    }

    /** Get the relative path of file from server root folder */
    public static String relativePath(File root, File file) {
        if (root == file) return "/";
        return file.getAbsolutePath()
                .substring(root.getAbsolutePath().length())
                .replace(File.separatorChar, '/');
    }

    /** Get md5 checksum of a file, this could take a long time if file is too large */
    public static String md5Checksum(File file) throws IOException {
        byte[] buffer = new byte[(int) Math.min(file.length(), 8192)];
        int len;
        FileInputStream fis = new FileInputStream(file);
        while ((len = fis.read(buffer)) != -1) {
            MD5.update(buffer, 0, len);
        }
        fis.close();
        byte[] bytes = MD5.digest();
        return new BigInteger(1, bytes).toString(16);
    }

    //TODO: real file type get using it's header.
    /** Get type of a file(suffix, etc .class) */
    public static String type(File file) {
        return file.getName().substring(file.getName().lastIndexOf("."));
    }

    public static String lastModifiedDate(File f) {
        return DATE_FORMAT.format(new Date(f.lastModified()));
    }


    //Deprecated items
    @Deprecated
    public static Class<?> getClass(File cls) throws ClassNotFoundException {
        if (cls.exists() && cls.getName().endsWith(".class")) {
            Class<?> c = Module.getJava().getExternalClass(getClassName(cls));
            if (c != null) return c;
        }
        throw new ClassNotFoundException("File " + cls.getPath() + " is not a class!");
    }

    @Deprecated
    public static String getClassName(File cls) {
        String s = relativePath(COMPILED, cls);
        return s.substring(1, s.length() - 6);
    }

    /** Get all files related to a class */
    @Deprecated
    public static HashSet<File> getRelatedClass(String name) throws FileNotFoundException {
        File f = getClassFile(name);

        return getRelatedClass(f);
    }

    /** Get all files related to a class */
    @Deprecated
    public static HashSet<File> getRelatedClass(File clazz) {
        int i = clazz.getName().lastIndexOf(".");
        if (i >= 0) {
            final String str = clazz.getName().substring(0, i);
            final String suffix = clazz.getName().substring(i);

            File[] files = clazz.getParentFile().listFiles(f -> !f.isDirectory()
                    && f.getName().startsWith(str)
                    && f.getName().endsWith(suffix));
            return new HashSet<>(Arrays.asList(files));
        }
        return new HashSet<>(Collections.singletonList(clazz));
    }
}

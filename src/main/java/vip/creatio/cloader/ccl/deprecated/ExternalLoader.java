package vip.creatio.cloader.ccl.deprecated;

import vip.creatio.cloader.bukkit.BukkitUtils;
import vip.creatio.cloader.bukkit.CLoader;
import vip.creatio.cloader.ccl.FileManager;
import vip.creatio.cloader.msg.Message;
import org.bukkit.plugin.java.JavaPlugin;

import javax.tools.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static vip.creatio.cloader.msg.Message.fromPath;
import static vip.creatio.cloader.msg.Message.internal;

/**
 * The first class that implements dynamic class-loading
 * Unfortunately it carries too much, so I decided to
 * modularize it's function into several small classes.
 * So it's deprecated.
 */
@SuppressWarnings("unused")
@Deprecated
public class ExternalLoader {

    /** Compiler class */
    private static final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    private static final URLClassLoader clsLoader = (URLClassLoader) CLoader.class.getClassLoader();

    /** All the loaded external classes */
    private static final Map<String, Class<?>> CLASSES = new HashMap<>();
    private static final Set<URL> DEPENDENCIES = new HashSet<>();

    private static final Map<String, Class<?>> LOADER_CLASSES;

    /** All the non-arg methods in external classes */
    private static final Map<Class<?>, Method[]> METHODS = new HashMap<>();

    private static final Method addURL;

    static {
        try {

            Field f0 = clsLoader.getClass().getDeclaredField("classes");
            f0.setAccessible(true);
            LOADER_CLASSES = (Map<String, Class<?>>) f0.get(clsLoader);

            addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addURL.setAccessible(true);

        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException e) {
            Message.internal("&4Failed to initialize ExternalLoader!");
            throw new RuntimeException(e);
        }
    }


    /** Encoding of the .java file(s) */
    private Charset encoding = StandardCharsets.UTF_8;       //Charset.availableCharsets()

    /** Compiler error message collector */
    private final DiagnosticCollector<JavaFileObject> diagCollector = new DiagnosticCollector<>();

    /** Compiling manager */
    private final StandardJavaFileManager manager = compiler.getStandardFileManager(diagCollector, null, encoding);

    /** Compiling options
     * etc Arrays.asList("-encoding", encoding)
     */
    private List<String> options;

    /** Is task succeed */
    private boolean succeed = false;



    public ExternalLoader() {
        options = Arrays.asList("-encoding", encoding.displayName());
    }


    public static void init() {
        if (compiler == null) {
            Message.internal("Failed to init ExternalLoader: system compiler not found!");
            Message.internal("Make sure you're running on JDK 8 or Java 11, out-of-dated " +
                    "JRE does not provide built-in compiler class!");
            throw new Error("Java version incompatible");
        }
    }



    /** For Command /creatio class compile
     *
     * Returns a list of information.
     */
    public String[] compileSrc(String path) {
        return compileSrc(new File(FileManager.SOURCE, path));
    }

    public String[] compileAllSrc() {
        return compileSrc(FileManager.SOURCE);
    }

    public String[] compileSrc(File[] files) {
        List<String> msg = new ArrayList<>();
            try {
                //Compile whole folder
                String[] clsName = new String[files.length];
                for (int i = 0; i < files.length; i++) {
                    clsName[i] = FileManager.getCompiledName(files[i]);
                }

                //No valid .java file
                if (files.length == 0) return message0("MAIN.FILE.NO_FILE", ".java");

                msg.addAll(message1("MAIN.SOURCE.COMPILATION.START",
                        "" + files.length, Message.fromPath("MAIN.FILE.FILE")[0]));

                succeed = compile(FileManager.COMPILED, files);
                if (!succeed) {
                    msg.addAll(Arrays.asList(reportDiagnostic()));
                } else {
                    msg.addAll(message1("MAIN.SOURCE.COMPILATION.SUCCESS",
                            files.length + Message.fromPath("MAIN.FILE.FILE")[0]));
                    msg.addAll(message1("MAIN.CLASS.LOADING.START",
                            Message.fromPath("MAIN.CLASS.LOADING.RECENTLY")));

                    List<File> load = new LinkedList<>();
                    for (String s : clsName){
                        load.addAll(FileManager.getRelatedClass(s));
                    }
                    try {
                        File[] file = load.toArray(new File[0]);
                        for (File f : file) {
                            try {
                                unload(FileManager.getClass(f));
                            } catch (ClassNotFoundException ignored) {}
                        }
                        msg.addAll(message1("MAIN.CLASS.LOADING.LOADED",
                                String.valueOf(load(file))));
                    } catch (Exception e) {
                        msg.addAll(message1("MAIN.CLASS.LOADING.FAILED"));
                        e.printStackTrace();
                    }
                }
                return msg.toArray(new String[0]);
            } catch (IOException e) {
                Message.internal("Cannot set locations for ExternalLoader " + hashCode());
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                msg.addAll(message1("MAIN.SOURCE.COMPILATION.BAD_OPTION", options.toArray(new String[0])));
            }
            return new String[]{"/null/"};
    }

    public String[] compileSrc(File src) {
        List<String> msg = new ArrayList<>();
        try {
            //Not a valid .java file
            if (!src.getName().endsWith(".java"))
                return message0("MAIN.FILE.INVALID_FILE",
                        src.getName().substring(src.getName().lastIndexOf(".")),
                        ".java");

            msg.addAll(message1("MAIN.SOURCE.COMPILATION.START", src.getName()));

            succeed = compile(FileManager.COMPILED, src);
            if (!succeed) {
                msg.addAll(Arrays.asList(reportDiagnostic()));
            } else {
                msg.addAll(message1("MAIN.SOURCE.COMPILATION.SUCCESS", src.getName()));
                msg.addAll(message1("MAIN.CLASS.LOADING.START",
                        Message.fromPath("MAIN.CLASS.LOADING.RECENTLY")));

                String str = FileManager.getCompiledName(src);

                try {
                    File[] files = FileManager.getRelatedClass(str).toArray(new File[0]);
                    for (File f : files) {
                        try {
                            unload(FileManager.getClass(f));
                        } catch (ClassNotFoundException ignored) {}
                    }
                    msg.addAll(message1("MAIN.CLASS.LOADING.LOADED",
                            String.valueOf(load())));
                } catch (Exception e) {
                    msg.addAll(message1("MAIN.CLASS.LOADING.FAILED"));
                    e.printStackTrace();
                }
            }
            return msg.toArray(new String[0]);
        } catch (IOException e) {
            Message.internal("Cannot set locations for ExternalLoader " + hashCode());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            msg.addAll(message1("MAIN.SOURCE.COMPILATION.BAD_OPTION", options.toArray(new String[0])));
        }
        return new String[]{"/null/"};
    }

    public boolean compile(File outputDir, File... sources) throws IOException {
        manager.setLocation(StandardLocation.CLASS_PATH, Arrays.asList(FileManager.getDependencies()));
        manager.setLocation(StandardLocation.SOURCE_PATH, Arrays.asList(sources));
        manager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(outputDir));

        Iterable<? extends JavaFileObject> units = manager.getJavaFileObjectsFromFiles(Arrays.asList(sources));
        JavaCompiler.CompilationTask task = compiler.getTask(null, manager, diagCollector, options, null, units);
        return task.call();
    }

    public static int load(File... file) {
        if (file.length < 1) throw new RuntimeException("Empty file array!");
        Class<?>[] clazz = loadClass(file);
        for (Class<?> c : clazz) {
            CLASSES.put(c.getTypeName(), c);
            findMethods0(c);
        }
        return clazz.length;
    }

    public static Class<?>[] loadClass(File... file) throws RuntimeException {
        //New loader & Class array
        java.lang.ClassLoader loader;
        try {
            loader = new URLClassLoader(new URL[]{FileManager.COMPILED.toURI().toURL()}, clsLoader);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        List<Class<?>> clazz = new ArrayList<>();

        //Load all
        for (File f : file) {
            try {
                Class<?> cls = loader.loadClass(FileManager.getClassName(f));
                clazz.add(cls);
                try {
                    Method m = cls.getMethod("load", JavaPlugin.class);
                    if (Modifier.isStatic(m.getModifiers())) m.invoke(null, CLoader.getInstance().getVirtual());
                } catch (NoSuchMethodException ignored) {
                } catch (InvocationTargetException | IllegalAccessException | NullPointerException e) {
                    Message.internal("Failed to run load() method for class " + cls.getTypeName());
                    if (e instanceof InvocationTargetException) e.getCause().printStackTrace();
                    else e.printStackTrace();
                }
            } catch (NoClassDefFoundError e) {
                Message.internal("No class def found for " + f.getAbsolutePath() + "! Go check if you have moved files under compiled folder.");
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                Message.internal("File " + f.getPath() + " is not a class!");
                throw new RuntimeException(e);
            } catch (UnsupportedClassVersionError e) {
                Message.internal("Unspport class version of " + f.getName() + "! ");
                Message.internal(e.getMessage());
            }
        }
        return clazz.toArray(new Class[0]);
    }

    private static void findMethods0(Class<?> clazz) {
        List<Method> mths = new ArrayList<>();
        for (Method mth : clazz.getDeclaredMethods()) {
            if ((mth.getParameterCount() == 0 ||
                    (mth.getParameterCount() == 1 && mth.getParameterTypes()[0] == String[].class))
                    && Modifier.isStatic(mth.getModifiers())) {
                mth.setAccessible(true);
                mths.add(mth);
            }

        }
        METHODS.put(clazz, mths.toArray(new Method[0]));
    }

    public static void loadDependency(File... jars) {
        List<URL> urls = new ArrayList<>();
        for (File f : jars) {
            if (f.exists() && !f.isDirectory() && f.getName().endsWith(".jar")) {
                try {
                    urls.add(f.toURI().toURL());
                } catch (MalformedURLException e) {
                    Message.internal("Failed to load dependency " + f.getAbsolutePath());
                    e.printStackTrace();
                }
            } else Message.internal("Failed to load dependency " + f.getAbsolutePath());
        }
        loadDependency(urls.toArray(new URL[0]));
    }

    public static void loadDependency(URL... urls) {
        List<URL> u = Arrays.asList(clsLoader.getURLs());
        for (URL url : urls) {
            try {
                if (!u.contains(url)) DEPENDENCIES.add(url);
                addURL.invoke(clsLoader, url);
            } catch (InvocationTargetException e) {
                Message.internal("Failed to load dependency " + url + "!");
                e.getCause().printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }



    public String[] reportDiagnostic() {
        List<String> str = message1("MAIN.SOURCE.COMPILATION.FAILED");
        File src = null;
        BufferedReader rd;
        int ctr = 0;
        for (Diagnostic<? extends JavaFileObject> diag : diagCollector.getDiagnostics() ) {
            try {
                if (src == null || src.toURI() != diag.getSource().toUri()) {
                    src = new File(diag.getSource().toUri());
                    str.add("@ §9" + getFilePath0(src.getAbsolutePath()));
                    ctr++;
                }

                rd = new BufferedReader(new FileReader(src));

                str.add("  §6<§e⚡" + ctr++ + "§6> §r" + diag.getMessage(null).replaceAll("\n", "  ->"));
                str.add("    " + contentAt0((int) diag.getLineNumber(), rd));
                str.add(stackPos0((int) diag.getColumnNumber()) + "§7 (Line " + diag.getLineNumber() + ", Column " + diag.getColumnNumber() + ')');
            } catch (IOException e) {
                Message.internal("Cannot read file " + diag.getSource().toUri());
                e.printStackTrace();
            }
        }
        return str.toArray(new String[0]);
    }

    public static void unload(String binaryName) {
        unload(CLASSES.get(binaryName));
    }

    public static void unload(Class<?> clazz) {
        //unload() method execute
        try {
            Method m = clazz.getMethod("unload", JavaPlugin.class);
            if (Modifier.isStatic(m.getModifiers())) m.invoke(null, CLoader.getInstance());
        } catch (NoSuchMethodException ignored) {
        } catch (InvocationTargetException | IllegalAccessException | NullPointerException e) {
            Message.internal("Failed to run unload() method for class " + clazz.getTypeName());
            if (e instanceof InvocationTargetException) e.getCause().printStackTrace();
            else e.printStackTrace();
        }

        String name = clazz.getTypeName();
        BukkitUtils.unregister(clazz);
        METHODS.remove(clazz);
        CLASSES.remove(name);
        LOADER_CLASSES.remove(name);
    }

    public static void unloadAll(Class<?>... classes) {
        for (Class<?> cls : classes) {
            unload(cls);
        }
    }

    public static void unloadAll(Collection<Class<?>> classes) {
        for (Class<?> cls : new ArrayList<>(classes)) {
            unload(cls);
        }
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

    private String contentAt0(int line, BufferedReader reader) {
        int n = 1;
        String str;
        try {
            while ((str = reader.readLine()) != null) {
                if (n++ == line) return str;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "/null/";
    }

    private String stackPos0(int times) {
        char[] chr = new char[times + 4];
        for (int i = 0; i < times + 4; i++) {
            chr[i] = ' ';
        }
        chr[times + 3] = '^';
        return new String(chr);
    }

    public static String[] _autoCompileAll() {
        Arrays.stream(FileManager.COMPILED.listFiles()).parallel().forEach(File::delete);
        ExternalLoader e = new ExternalLoader();
        return e.compileAllSrc();
    }

    private String getFilePath0(String path) {
        return path.substring(path.lastIndexOf("plugins" + File.separatorChar + "ClassLoader"));
    }

    private static List<String> message1(String path) {
        return new ArrayList<>(Arrays.asList(message0(path)));
    }

    private static List<String> message1(String path, String... vars) {
        return new ArrayList<>(Arrays.asList(message0(path, vars)));
    }

    private static String[] message0(String path) {
        return message0(path, (String) null);
    }

    private static String[] message0(String path, String... vars) {
        return Message.fromPath(path, vars);
    }

    public static Map<String, Class<?>> getExternalClasses() {
        return Collections.unmodifiableMap(CLASSES);
    }

    public static Set<URL> getLoadedDependencies() {
        return Collections.unmodifiableSet(DEPENDENCIES);
    }

    public static Map<Class<?>, Method[]> getMethods() {
        return Collections.unmodifiableMap(METHODS);
    }

    public static String[] listMethods(String binaryName) {
        return listMethods(CLASSES.get(binaryName));
    }

    public static String[] listMethods(Class<?> clazz) {
        Method[] mth = METHODS.get(clazz);
        String[] str = mth == null ? new String[0] : new String[mth.length];
        for (int i = 0; i < str.length; i++) {
            str[i] = mth[i].getName();
        }
        return str;
    }

    public Charset getEncoding() {
        return encoding;
    }

    public void setEncoding(Charset encoding) {
        this.encoding = encoding;
    }

    public DiagnosticCollector<JavaFileObject> getDiagCollector() {
        return diagCollector;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> opts) {
        this.options = opts;
    }

    public boolean succeed() {
        return succeed;
    }
}

package vip.creatio.cloader.ccl.module;

import vip.creatio.cloader.bukkit.BukkitUtils;
import vip.creatio.cloader.bukkit.CLoader;
import vip.creatio.cloader.ccl.CompilationException;
import vip.creatio.cloader.ccl.CompilationTask;
import vip.creatio.cloader.ccl.FileManager;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import javax.tools.*;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

class JavaImpl extends ModuleImpl {

    /** Parent ClassLoader */
    private static final URLClassLoader clsLoader = (URLClassLoader) CLoader.class.getClassLoader();

    /** Compiler class */
    private static final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    private static final Map<String, Class<?>> LOADER_CLASSES;

    private static final Method addURL;

    //TODO: temporarily
    private JavaClassLoader temp;

    {
        try {
            temp = new JavaClassLoader(new URL[]{FileManager.COMPILED.toURI().toURL()});
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    static {
        try {

            Field f0 = clsLoader.getClass().getDeclaredField("classes");
            f0.setAccessible(true);
            LOADER_CLASSES = (Map<String, Class<?>>) f0.get(clsLoader);

            addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addURL.setAccessible(true);

        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException e) {
            msg.intern("&4Failed to initialize ExternalLoader JavaImpl!");
            throw new RuntimeException(e);
        }
    }

    /** All the loaded external classes */
    private final Map<String, Class<?>> CLASSES = new HashMap<>();

    /** All the non-arg methods in external classes */
    private final Map<Class<?>, Method[]> METHODS = new HashMap<>();

    /** All the class fields in external classes */
    private final Map<Class<?>, Field[]> FIELDS = new HashMap<>();

    /** All the external Jars loaded as dependencies */
    private final Set<URL> DEPENDENCIES = new HashSet<>();

    class JavaClassLoader extends URLClassLoader {

        public JavaClassLoader(URL[] urls) {
            super(urls, clsLoader);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            Class<?> c = CLASSES.get(name);
            if (c == null) {
                try {
                    c = super.findClass(name);
                    CLASSES.put(name, c);
                } catch (ClassNotFoundException e) {
                    c = getParent().loadClass(name);
                }
            }
            return c;
        }
    }

    public JavaImpl() {
        if (compiler == null) {
            msg.intern("Failed to init JavaImpl: system compiler not found!");
            msg.intern("Make sure you're running on JDK 8 or Java 11, out-of-dated " +
                    "JRE does not provide Java compiler!");
            throw new Error("Java version incompatible");
        }
    }

    @Override
    public Object run(ScriptFile script, Object... invoke) throws Exception {
        ClassFile c = getClassFile0(script);
        if (invoke.length < 1 || c.getClassObject() == null) return 0;
        Method[] m = METHODS.get(c.getClassObject());

        for (Method method : m) {
            if (method.getName().equals(invoke[0].toString())) {
                try {
                    Object result = null;
                    if (method.getParameterCount() == 0) {
                        result = method.invoke(null);
                    }
                    else if (method.getParameterCount() >= 1) {
                        result = method.invoke(null,
                                (Object[]) Arrays.copyOfRange(invoke, 1, invoke.length));
                    }
                    return result;

                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return -1;
    }

    @Override
    public boolean load(ScriptFile script) throws RuntimeException {
        ClassFile cf = getClassFile0(script);
        Class<?>[] cls = loadClass(cf.getRelatedClass());
        if (cls.length == 0) return false;
        Arrays.stream(cls)
                .peek(this::findMethods0)
                .peek(this::findFields0)
                .forEach(c -> CLASSES.put(c.getTypeName(), c));
        return true;
    }

    private void findMethods0(Class<?> clazz) {
        List<Method> mths = new ArrayList<>();
        for (Method mth : clazz.getDeclaredMethods()) {
            if ((mth.getParameterCount() == 0
                    || (mth.getParameterCount() == 1 && mth.getParameterTypes()[0] == String[].class)
                    || (mth.getParameterCount() == 1 && mth.getParameterTypes()[0] == CommandSender.class)
                    || (mth.getParameterCount() == 2 && mth.getParameterTypes()[0] == CommandSender.class
                                                     && mth.getParameterTypes()[1] == String[].class))
                    && Modifier.isStatic(mth.getModifiers())) {
                mth.setAccessible(true);
                mths.add(mth);
            }

        }
        METHODS.put(clazz, mths.toArray(new Method[0]));
    }

    private void findFields0(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Arrays.stream(clazz.getDeclaredFields()).filter(f -> Modifier.isStatic(f.getModifiers()))
                .peek(f -> f.setAccessible(true))
                .forEach(fields::add);
        FIELDS.put(clazz, fields.toArray(new Field[0]));
    }

    public Class<?>[] loadClass(ClassFile... file) throws RuntimeException {
        //New loader & Class array
        JavaClassLoader loader = temp;
//        try {
//            loader = new JavaClassLoader(new URL[]{FileManager.COMPILED.toURI().toURL()});
//        } catch (MalformedURLException e) {
//            throw new RuntimeException(e);
//        }

        List<Class<?>> clazz = new ArrayList<>();

        //Load all
        for (ClassFile f : file) {
            try {
                Class<?> cls = loader.loadClass(f.getClassName());
                clazz.add(cls);
                try {
                    Method m = cls.getMethod("load", JavaPlugin.class);
                    if (Modifier.isStatic(m.getModifiers())) m.invoke(null, CLoader.getInstance().getVirtual());
                } catch (NoSuchMethodException ignored) {
                } catch (Throwable e) {
                    msg.intern("Failed to run load() method for class " + cls.getTypeName());
                    if (e instanceof InvocationTargetException) e.getCause().printStackTrace();
                    else e.printStackTrace();
                }
            } catch (NoClassDefFoundError e) {
                msg.intern("No class def found for " + f.getAbsolutePath() + "! Go check if you have moved files under compiled folder.");
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                msg.intern("File " + f.getPath() + " is not a class!");
                throw new RuntimeException(e);
            } catch (UnsupportedClassVersionError e) {
                msg.intern("Unspport class version of " + f.getName() + "! ");
                msg.intern(e.getMessage());
            }
        }
        return clazz.toArray(new Class[0]);
    }

    @Override
    public boolean unload(ScriptFile script) throws RuntimeException {
        ClassFile cf = getClassFile0(script);
        ClassFile[] files = cf.getRelatedClass();
        Set<Class<?>> classes = new HashSet<>();
        Arrays.stream(files).map(ClassFile::getClassName).map(CLASSES::get).forEach(classes::add);
        if (classes.isEmpty()) return false;
        classes.forEach(this::unloadClass);
        return false;
    }

    /** Unload a external class, throws exception in method execution, but will always unoad the class */
    void unloadClass(Class<?> clazz) throws RuntimeException {
        //unload() method execute
        RuntimeException re = null;
        try {
            Method m = clazz.getMethod("unload", JavaPlugin.class);
            if (Modifier.isStatic(m.getModifiers())) m.invoke(null, CLoader.getInstance());
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable e) {
            msg.intern("Failed to run unload() method for class " + clazz.getTypeName());
            if (e instanceof InvocationTargetException) re = new RuntimeException(e.getCause());
            else re = new RuntimeException(e);
        }

        {
            String name = clazz.getTypeName();
            BukkitUtils.unregister(clazz);
            METHODS.remove(clazz);
            FIELDS.remove(clazz);
            CLASSES.remove(name);
            LOADER_CLASSES.remove(name);
        }

        for (Class<?> sub : clazz.getDeclaredClasses()) {
            String name = sub.getTypeName();
            BukkitUtils.unregister(sub);
            METHODS.remove(sub);
            FIELDS.remove(sub);
            CLASSES.remove(name);
            LOADER_CLASSES.remove(name);
        }

        try {
            temp = new JavaClassLoader(new URL[]{FileManager.COMPILED.toURI().toURL()});
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        //Throw exceptions
        if (re != null) throw re;
    }

    void loadDependency(File... jars) {
        List<URL> urls = new ArrayList<>();
        for (File f : jars) {
            if (f.exists() && !f.isDirectory() && f.getName().endsWith(".jar")) {
                try {
                    urls.add(f.toURI().toURL());
                } catch (MalformedURLException e) {
                    msg.intern("Failed to load dependency " + f.getAbsolutePath());
                    e.printStackTrace();
                }
            } else msg.intern("Failed to load dependency " + f.getAbsolutePath());
        }
        loadDependency(urls.toArray(new URL[0]));
    }

    void loadDependency(URL... urls) {
        List<URL> u = Arrays.asList(clsLoader.getURLs());
        for (URL url : urls) {
            try {
                if (!u.contains(url)) DEPENDENCIES.add(url);
                addURL.invoke(clsLoader, url);
            } catch (InvocationTargetException e) {
                msg.intern("Failed to load dependency " + url + "!");
                e.getCause().printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String suffix() {
        return null;
    }

    @Override
    public ScriptFile[] list() {
        return new ScriptFile[0];
    }

    @Override
    public CompilationTask compile(File dir) throws CompilationException {
        return new JavacTask(dir);
    }

    List<Class<?>> getClasses() {
        return Collections.unmodifiableList(new ArrayList<>(CLASSES.values()));
    }

    Class<?> getCLASS(String binaryName) {
        return CLASSES.get(binaryName);
    }

    List<Method> getMethods(Class<?> cls) {
        return Collections.unmodifiableList(Arrays.asList(METHODS.get(cls)));
    }

    List<Method> getMethods() {
        List<Method> l = new ArrayList<>();
        METHODS.values().stream().map(Arrays::asList).forEach(l::addAll);
        return Collections.unmodifiableList(l);
    }

    List<Field> getFields(Class<?> cls) {
        return Collections.unmodifiableList(Arrays.asList(FIELDS.get(cls)));
    }

    List<Field> getFields() {
        List<Field> l = new ArrayList<>();
        FIELDS.values().stream().map(Arrays::asList).forEach(l::addAll);
        return Collections.unmodifiableList(l);
    }

    private static ClassFile getClassFile0(ScriptFile script) throws RuntimeException {
        if (script.checkType(".class")) {
            try {
                return new ClassFile(script);
            } catch (FileNotFoundException | InvalidClassException e) {
                throw new RuntimeException(e);
            }
        } else throw new RuntimeException("Cannot load a non-class file");
    }

    URLClassLoader getClassLoader() {
        return clsLoader;
    }

    List<URL> getDependencies() {
        return Collections.unmodifiableList(new ArrayList<>(DEPENDENCIES));
    }

    class JavacTask implements CompilationTask {

        /** Compiler error message collector */
        private final DiagnosticCollector<JavaFileObject> diagCollector = new DiagnosticCollector<>();

        /** Compiling manager */
        private final StandardJavaFileManager manager = compiler.getStandardFileManager(diagCollector, null, encoding);

        /** Compiling options
         * etc Arrays.asList("-encoding", encoding)
         */
        private List<String> options = new ArrayList<>();

        private final File output;

        JavacTask(File outputDir) throws CompilationException {
            output = outputDir;
            try {
                manager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(outputDir));
            } catch (IOException e) {
                throw new CompilationException(CompilationException.Type.CANT_SET_LOCATION, e);
            }
        }

        @Override
        public void setOptions(String... options) {
            this.options = new ArrayList<>(Arrays.asList(options));
        }

        @Override
        public File getOutputDir() {
            return output;
        }

        @Override
        public boolean compile(ScriptFile[] sources) throws CompilationException {
            if (!ScriptFile.checkType(".java", sources))
                throw new CompilationException(CompilationException.Type.FILE_TYPE_MISMATCH);
            try {
                manager.setLocation(StandardLocation.CLASS_PATH, Arrays.asList(FileManager.getDependencies()));
                manager.setLocation(StandardLocation.SOURCE_PATH, Arrays.asList(sources));
                options.addAll(Arrays.asList("-encoding", encoding.displayName()));

                Iterable<? extends JavaFileObject> units = manager.getJavaFileObjectsFromFiles(Arrays.asList(sources));
                JavaCompiler.CompilationTask task = compiler.getTask(null, manager, diagCollector, options, null, units);
                return task.call();
            } catch (IOException e) {
                throw new CompilationException(CompilationException.Type.CANT_SET_LOCATION, e);
            } catch (RuntimeException e) {
                throw new CompilationException(CompilationException.Type.INTERNAL_ERROR, e);
            }
        }

        @Override
        public String[] reportDiagnostic() {
            List<String> str = new ArrayList<>(Arrays.asList(msg.getList("MAIN.SOURCE.COMPILATION.FAILED")));
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
                    msg.intern("Cannot read file " + diag.getSource().toUri());
                    e.printStackTrace();
                }
            }
            return str.toArray(new String[0]);
        }

        private String getFilePath0(String path) {
            return path.substring(path.lastIndexOf("plugins" + File.separatorChar + "ClassLoader"));
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
    }
}

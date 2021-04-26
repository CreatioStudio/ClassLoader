package vip.creatio.cloader.ccl.module;

import vip.creatio.cloader.ccl.CompilationException;
import vip.creatio.cloader.ccl.CompilationTask;
import vip.creatio.cloader.ccl.FileManager;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class Java extends Module {

    private final JavaImpl impl = new JavaImpl();

    private static final Pattern classDeclaration = Pattern.compile("^(final )?class ([a-zA-Z0-9$]+)");

    Java() {
        if (Module.getJava() != null)
            throw new RuntimeException("This already instanced!");
    }

    @Override
    public ModuleType getType() {
        return ModuleType.JAVA;
    }

    @Override
    public ModuleImpl getImpl() {
        return impl;
    }


    /** For Command /creatio class compile
     *
     * Returns a list of information.
     */
    public String[] compileSrc(String path, String[] options) {
        return compileSrc(new File[]{new File(FileManager.SOURCE, path)}, options);
    }

    public String[] compileSrc(String path) {
        return compileSrc(path, null);
    }

    public String[] compileAllSrc(String[] options) {
        return compileSrc(FileManager.getSources(), options);
    }

    public String[] compileAllSrc() {
        return compileAllSrc(null);
    }

    public String[] compileSrc(File[] files, String[] options) {
        List<String> msg = new ArrayList<>();
        try {
            //Get ScriptFile and validate
            List<ScriptFile> sf = new ArrayList<>();
            Arrays.stream(files).forEach(f -> {
                try {
                    ScriptFile ff = new ScriptFile(f);
                    if (!ff.checkType(".java")) msg.addAll(message1("MAIN.FILE.INVALID_FILE", ff.getType(), ".java"));
                    else sf.add(ff);
                } catch (FileNotFoundException e) {
                    msg.addAll(message1("MAIN.FILE.DOESNT_EXIST", f.getPath()));
                }
            });

            //No valid .java file
            if (sf.size() == 0) return message0("MAIN.FILE.NO_FILE", ".java");

            //msg start compiling
            msg.addAll(message1("MAIN.SOURCE.COMPILATION.START",
                    listFileCrafter0(3, sf.toArray(new File[0])),
                    Module.msg.fromPath("MAIN.FILE.FILE")[0]));

            //start compiling
            CompilationTask task = impl.compile(FileManager.COMPILED);
            task.setOptions(options);
            boolean succeed = task.compile(sf.toArray(new ScriptFile[0]));

            //success detection and loading
            if (!succeed) {
                msg.addAll(message1("MAIN.SOURCE.COMPILATION.FAILED"));
                msg.addAll(Arrays.asList(task.reportDiagnostic()));
            } else {
                msg.addAll(message1("MAIN.SOURCE.COMPILATION.SUCCESS",
                        listFileCrafter0(3, sf.toArray(new File[0]))));
                msg.addAll(message1("MAIN.CLASS.LOADING.START",
                        Module.msg.fromPath("MAIN.CLASS.LOADING.RECENTLY")));

                List<ClassFile> load = new LinkedList<>();

                for (ScriptFile f : sf){
                    load.addAll(getCompiledClassFile(f));
                }

                try {
                    ClassFile[] file = load.toArray(new ClassFile[0]);
                    for (ClassFile f : file) {
                        Class<?> cls = impl.getCLASS(f.getClassName());
                        if (cls != null) impl.unloadClass(cls);
                    }
                    msg.addAll(Arrays.asList(load(file)));
                } catch (Exception e) {
                    msg.addAll(message1("MAIN.CLASS.LOADING.FAILED"));
                    e.printStackTrace();
                }
            }
            return msg.toArray(new String[0]);
        } catch (IllegalArgumentException e) {
            msg.addAll(message1("MAIN.SOURCE.COMPILATION.BAD_OPTION", ""/*TODO: make this! StringUtil.lineArgs(options)*/));
        } catch (CompilationException e) {
            switch (e.type) {
                case BAD_OPTIONS:
                    msg.addAll(message1("MAIN.SOURCE.COMPILATION.BAD_OPTION", ""/*TODO: make this! StringUtil.lineArgs(options)*/));
                    break;
                case FILE_NOT_FOUND:
                    msg.addAll(message1("MAIN.FILE.DOESNT_EXIST", e.getMessage()));
                    break;
                case INTERNAL_ERROR:
                    msg.addAll(message1("MAIN.SOURCE.COMPILATION.INTERNAL"));
                    break;
                case CANT_SET_LOCATION:
                    msg.addAll(message1("MAIN.SOURCE,COMPILATION.FAILED"));
                    break;
                case FILE_TYPE_MISMATCH:
                    msg.addAll(message1("MAIN.FILE.INVALID_FILE", e.toString(), ".java"));
            }
        }
        return new String[]{"/null/"};
    }

    public String[] compileSrc(File[] files) {
        return compileSrc(files, null);
    }

    public String[] load(File... files) {
        List<String> msg = new ArrayList<>();
        List<ClassFile> success = new ArrayList<>();
        for (File f : files) {
            ScriptFile f1 = null;
            try {
                f1 = new ScriptFile(f);
                ClassFile ff = new ClassFile(f1);
                try {
                    if (impl.load(ff)) success.add(ff);
                } catch (RuntimeException e) {
                    msg.addAll(message1("MAIN.CLASS.LOADING.FAILED", f.getName()));
                    if (e.getCause() != null) e.getCause().printStackTrace();
                    else e.printStackTrace();
                }
            } catch (FileNotFoundException e) {
                msg.addAll(message1("MAIN.FILE.DOESNT_EXIST", f.getPath()));
            } catch (InvalidClassException e) {
                msg.addAll(message1("MAIN.FILE.INVALID_FILE", f1.getType(), ".class"));
            }
        }
        msg.addAll(message1("MAIN.CLASS.LOADING.LOADED", listClassCrafter1(3, success.toArray(new ClassFile[0]))));
        return msg.toArray(new String[0]);
    }

    public String[] unload(Class<?>... classes) {
        List<String> msg = new ArrayList<>();
        List<Class<?>> success = new ArrayList<>();
        for (Class<?> c : classes) {
            if (!impl.getClasses().contains(c))
                throw new RuntimeException("Class " + c.getTypeName() + " is not a external class!");
            try {
                success.add(c);
                impl.unloadClass(c);
            } catch (RuntimeException e) {
                msg.addAll(message1("MAIN.CLASS.UNLOAD.METHOD_EXECUTION_FAILED", c.getTypeName()));
                e.printStackTrace();
            }
        }
        msg.addAll(message1("MAIN.CLASS.UNLOAD.UNLOADED", listClassCrafter0(3, success.toArray(new Class[0]))));
        return msg.toArray(new String[0]);
    }

    public String[] reload(Class<?>... classes) {
        List<String> msg = new ArrayList<>();
        List<ClassFile> reloaded = new ArrayList<>();
        for (Class<?> c : classes) {
            if (!impl.getClasses().contains(c))
                throw new RuntimeException("Class " + c.getTypeName() + " is not a external class!");
            try {
                //unload
                impl.unloadClass(c);

                //load
                try {
                    ClassFile ff = ClassFile.getClassFile(c);
                    ScriptFile sf = new ScriptFile(ff);
                    try {
                        if (impl.load(ff)) reloaded.add(ff);
                    } catch (RuntimeException e) {
                        msg.addAll(message1("MAIN.CLASS.LOADING.FAILED", ff.getName()));
                        if (e.getCause() != null) e.getCause().printStackTrace();
                        else e.printStackTrace();
                    }
                } catch (FileNotFoundException e) {
                    msg.addAll(message1("MAIN.CLASS.NOT_FOUND", e.getMessage()));
                } catch (InvalidClassException e) {
                    msg.addAll(message1("MAIN.FILE.INVALID_FILE", "", ".class"));
                }
            } catch (RuntimeException e) {
                msg.addAll(message1("MAIN.CLASS.UNLOAD.METHOD_EXECUTION_FAILED", c.getTypeName()));
                e.printStackTrace();
            }
        }
        msg.addAll(message1("MAIN.CLASS.RELOAD.COMPLETE", listClassCrafter1(3, reloaded.toArray(new ClassFile[0]))));
        return msg.toArray(new String[0]);
    }

    public String[] run(Class<?> cls, Method mth, Object... args) {
        List<String> msg = new ArrayList<>();
        try {
            Object[] newargs = new Object[args.length + 1];
            System.arraycopy(args, 0, newargs, 1, args.length);
            newargs[0] = mth.getName();
            Object result = impl.run(ClassFile.getClassFile(cls), newargs);
            if (result != null) {
                if (result.getClass().isArray())
                    msg.addAll(message1("MAIN.CLASS.RUN.RETURN", Arrays.toString((Object[]) result)));
                else
                    msg.addAll(message1("MAIN.CLASS.RUN.RETURN", result.toString()));
            }
        } catch (InvalidClassException e) {
            msg.addAll(message1("MAIN.CLASS.NOT_FOUND", cls.getTypeName()));
        } catch (Exception e) {
            if (e instanceof InvocationTargetException) {
                msg.addAll(message1(
                        "MAIN.CLASS.RUN.EXCEPTION",
                        ((InvocationTargetException) e).getTargetException().getClass().getTypeName(),
                        mth.getName())
                );
                ((InvocationTargetException) e).getTargetException().printStackTrace();
            } else {
                msg.addAll(message1(
                        "MAIN.CLASS.RUN.EXCEPTION",
                        e.getClass().getTypeName(),
                        mth.getName())
                );
                e.printStackTrace();
            }
        }
        return msg.toArray(new String[0]);
    }

    public String[] __autoCompileAll() {
        Arrays.stream(FileManager.COMPILED.listFiles()).parallel().forEach(File::delete);
        return compileAllSrc();
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
        return msg.fromPath(path, vars);
    }

    static List<ClassFile> getCompiledClassFile(ScriptFile java) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(java));
            String line;
            String pkg = null;
            String clsName = java.getName();

            List<ClassFile> cf = new ArrayList<>();

            clsName = clsName.substring(0, clsName.length() - 5);
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("package ")) {
                    pkg = line.substring(8, line.length() - 1);
                }
                Matcher mth = classDeclaration.matcher("");
                if (mth.find()) {
                    cf.add(new ClassFile(FileManager.COMPILED,
                            ((pkg == null) ? "" : pkg.replace('.',File.separatorChar)) + mth.group(2) + ".class"));
                }
            }
            line = (pkg == null) ? clsName : pkg + '.' + clsName;
            cf.add(new ClassFile(FileManager.COMPILED, line.replace('.', File.separatorChar).concat(".class")));
            return cf;
        } catch (IOException e) {
            msg.intern("Unable to get class name for source file " + java.getAbsolutePath());
            throw new RuntimeException(e);
        }
    }

    public Class<?> getExternalClass(String binaryName) {
        return impl.getCLASS(binaryName);
    }

    public List<Class<?>> getExternalClasses() {
        return impl.getClasses();
    }

    public List<Method> getMethods(Class<?> clazz) {
        return impl.getMethods(clazz);
    }

    public List<Method> getMethods() {
        return impl.getMethods();
    }

    public String[] listMethods(String binaryName) {
        Class<?> c = impl.getCLASS(binaryName);
        return c == null ? new String[0] : listMethods(c);
    }

    public String[] listMethods(@NotNull Class<?> clazz) {
        List<String> l = new ArrayList<>();
        impl.getMethods(clazz).stream().map(Method::getName).forEach(l::add);
        return l.toArray(new String[0]);
    }

    void loadDependency(File... jars) {
        impl.loadDependency(jars);
    }

    void loadDependency(URL... urls) {
        impl.loadDependency(urls);
    }

    public List<URL> getLoadedDependencies() {
        return impl.getDependencies();
    }



    //TODO: temp

    public static String listFileCrafter0(int max, File... items) {
        return listItemCrafter0(File::getName, max, msg.fromPath("MAIN.FILE.FILE")[0], items);
    }

    public static String listClassCrafter0(int max, Class<?>... classes) {
        return listItemCrafter0(Class::getTypeName, max, msg.fromPath("MAIN.FILE.CLASS")[0], classes);
    }

    public static String listClassCrafter1(int max, ClassFile... classes) {
        return listItemCrafter0(ClassFile::getClassName, max, msg.fromPath("MAIN.FILE.CLASS")[0], classes);
    }

    @SafeVarargs
    public static <T> String listItemCrafter0(Messager<T> msg, int max, String alias, T... items) {
        if (items.length > max) return items.length + alias;
        if (items.length == 0) return 0 + alias;
        boolean flag = false;
        StringBuilder sb = new StringBuilder();
        for (T item : items) {
            if (flag) sb.append(", ");
            flag = true;
            sb.append(msg.getMessage(item));
        }
        return sb.toString();
    }

    public interface Messager<T> {
        String getMessage(T item);
    }
}

package vip.creatio.cloader.ccl.module;

import vip.creatio.cloader.bukkit.CLoader;
import vip.creatio.cloader.ccl.FileManager;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum ModuleType {
    GROOVY(true, Groovy.class, "groovy", "https://dl.bintray.com/groovy/maven/", "http://groovy-lang.org/documentation.html"),
    SCALA(true, Scala.class, "scala", "https://repo1.maven.org/maven2/org/scala-lang/scala-compiler/", "https://docs.scala-lang.org/"),
    KOTLIN(true, Kotlin.class, "kotlin", "https://api.github.com/repos/JetBrains/kotlin/releases", "https://kotlinlang.org/docs/reference/basic-syntax.html"),
    JYTHON(true, Jython.class, "jython", "https://repo1.maven.org/maven2/org/python/jython-standalone/", "https://docs.python.org/2.7/"),
    JAVA(false, Java.class, "java"),
    ;

    private final Class<? extends Module> cls;
    private final boolean isExternal;
    private final File module_folder;
    private final String name;
    private final URL download;
    private final URL docs;

    ModuleType(boolean external, Class<? extends Module> c, String n, String dwl, String doc) {
        isExternal = external;
        this.cls = c;
        name = n;
        module_folder = new File(FileManager.MODULES, n);
        try {
            download = new URL(dwl);
            docs = new URL(doc);
        } catch (MalformedURLException e) {
            CLoader.intern("Unable to init Module!");
            throw new RuntimeException(e);
        }
    }

    ModuleType(boolean external, Class<? extends Module> c, String n) {
        isExternal = external;
        cls = c;
        name = n;
        module_folder = null;
        download = null;
        docs = null;
    }

    /** Blocking! */
    URLConnection connectDownload() throws IOException {
        return download.openConnection();
    }

    /** Blocking! */
    URLConnection connectDocs() throws IOException {
        return docs.openConnection();
    }

    public boolean isExternal() {
        return isExternal;
    }

    URL getDownload() {
        return download;
    }

    URL getDocs() {
        return docs;
    }

    public File getModuleFolder() {
        return module_folder;
    }

    public String getName() {
        return name;
    }

    Class<? extends Module> getModuleClass() {
        return cls;
    }

    public static ModuleType[] externals() {
        List<ModuleType> list = new ArrayList<>();
        Arrays.stream(values()).filter(ModuleType::isExternal).forEach(list::add);
        return list.toArray(new ModuleType[0]);
    }
}

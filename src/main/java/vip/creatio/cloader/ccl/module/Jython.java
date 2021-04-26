package vip.creatio.cloader.ccl.module;

import vip.creatio.cloader.ccl.FileManager;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Jython extends ExternalModule {

    private static final ModuleType type = ModuleType.JYTHON;

    private ModuleImpl impl = null;

    private static SoftReference<Map<String, URL>> versionAndLink = new SoftReference<>(null);

    private static final Pattern link_regex = Pattern.compile("\"([0-9a-zA-Z.\\-]+)/\" ");

    static File DEFAULT_JAR = new File(type.getModuleFolder(), "jython.jar");

    Jython(String version) {
        super(version);
    }

    /** Blocking */
    public static String[] acquireVersionList() throws IOException {
        if (versionAndLink.get() == null) {
            versionAndLink = new SoftReference<>(new TreeMap<>(Collections.reverseOrder()));
            URLConnection con = type.connectDownload();
            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                Matcher mth = link_regex.matcher(line);
                if (mth.find()) {
                    URL url = new URL("https://repo1.maven.org/maven2/org/python/jython-standalone/" + mth.group(1) + "/jython-standalone-" + mth.group(1) + ".jar");
                    Objects.requireNonNull(versionAndLink.get()).put(mth.group(1), url);
                }
            }
        }
        return Objects.requireNonNull(versionAndLink.get()).keySet().toArray(new String[0]);
    }

    @Override
    public ModuleType getType() {
        return type;
    }

    @Override @Nullable
    public ModuleImpl getImpl() {
        return impl;
    }

    /** Blocking */
    @Override @Nullable
    public URL getDownloadLink() throws IOException {
        if (versionAndLink.get() == null) acquireVersionList();
        return versionAndLink.get().get(super.version);
    }

    /** Blocking */
    @Override
    public Downloader download() throws IOException, RuntimeException {
        Downloader d = super.getDownloader(type.getModuleFolder());
        d.setEndLogic(() -> d.getProduct().renameTo(new File(type.getModuleFolder(), "jython.jar")));
        return d;
    }

    @Override
    public void loadJars() {
        File[] files = FileManager.getFiles(type.getModuleFolder(), ".jar");
        getJava().loadDependency(files);
    }
}

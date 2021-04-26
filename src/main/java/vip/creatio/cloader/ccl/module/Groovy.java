package vip.creatio.cloader.ccl.module;

import vip.creatio.cloader.ccl.FileManager;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class Groovy extends ExternalModule {

    private static final ModuleType type = ModuleType.GROOVY;

    private GroovyImpl impl = null;

    private static SoftReference<Map<String, URL>> versionAndLink = new SoftReference<>(null);

    private static final Pattern link_regex = Pattern.compile("\":(apache-groovy-binary-([0-9.]+(-[a-z]+-[0-9]+)?).zip)\"");

    static File DEFAULT_BIN = new File(type.getModuleFolder(), "bin");

    Groovy(String version) {
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
                    URL url = new URL("https://dl.bintray.com/groovy/maven/" + mth.group(1));
                    Objects.requireNonNull(versionAndLink.get()).put(mth.group(2), url);
                }
            }
        }
        return Objects.requireNonNull(versionAndLink.get()).keySet().toArray(new String[0]);
    }

    @Override
    public ModuleType getType() {
        return type;
    }

    /** Blocking */
    @Override @Nullable
    URL getDownloadLink() throws IOException {
        if (versionAndLink.get() == null) acquireVersionList();
        return versionAndLink.get().get(super.version);
    }

    @Override
    public void loadJars() {
        File[] files = FileManager.getFiles(type.getModuleFolder(), ".jar");
        getJava().loadDependency(files);
    }

    @Override @Nullable
    public ModuleImpl getImpl() {
        if (DEFAULT_BIN.exists()) impl = new GroovyImpl();
        return impl;
    }

    /** Blocking */
    @Override
    public Downloader download() throws IOException, RuntimeException {
        Downloader d = super.getDownloader(type.getModuleFolder());
        d.setEndLogic(() -> {
            try {
                ZipInputStream stream = new ZipInputStream(new BufferedInputStream(new FileInputStream(d.getProduct())));
                ZipEntry entry;
                while ((entry = stream.getNextEntry()) != null) {
                    String path = entry.getName();
                    if (!(path.endsWith("/") && path.indexOf("/") == path.lastIndexOf("/"))) {
                        File f = new File(type.getModuleFolder(), path.substring(path.indexOf("/") + 1));
                        if (entry.isDirectory()) {
                            f.mkdirs();
                        } else {

                            f.getParentFile().mkdirs();
                            f.createNewFile();
                            int len;

                            byte[] b = new byte[Downloader.BUFFER_SIZE];
                            FileOutputStream os = new FileOutputStream(f);
                            while ((len = stream.read(b)) != -1) {
                                os.write(b, 0, len);
                            }

                            os.close();
                        }
                    }
                }
                stream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return d;
    }
}

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

class Kotlin extends ExternalModule {

    private static final ModuleType type = ModuleType.KOTLIN;

    private ModuleImpl impl = null;

    private static SoftReference<Map<String, URL>> versionAndLink = new SoftReference<>(null);

    private static final Pattern link_regex = Pattern.compile("\"browser_download_url\":\"(https://github.com/JetBrains/kotlin/releases/download/v([0-9a-zA-Z.\\-]+)/kotlin-compiler-([0-9a-zA-Z.\\-]+).zip)\"");

    static File DEFAULT_BIN = new File(type.getModuleFolder(), "kotlin\\bin");

    public Kotlin(String version) {
        super(version);
    }

    /** Blocking */
    public static String[] acquireVersionList() throws IOException {
        if (versionAndLink.get() == null) {
            versionAndLink = new SoftReference<>(new TreeMap<>(Collections.reverseOrder()));
            URLConnection con = type.connectDownload();
            InputStream stream = con.getInputStream();

            byte[] b = new byte[4096];
            stream.read(b, 0, 2048);
            while (stream.read(b, 2048, 2048) != -1) {
                Matcher mth = link_regex.matcher(new String(b));

                while (mth.find() && mth.start() < 2048) {
                    URL url = new URL(mth.group(1));
                    Objects.requireNonNull(versionAndLink.get()).put(mth.group(2), url);
                }

                System.arraycopy(b, 2048, b, 0, 2048);
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
    public URL getDownloadLink() throws IOException {
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

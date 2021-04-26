package vip.creatio.cloader.ccl.module;

import vip.creatio.cloader.ccl.FileManager;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

public abstract class ExternalModule extends Module {
    protected final String version;

    ExternalModule(String version) {
        this.version = version;
    }

    public static String[] acquireVersionList(ModuleType type) throws IOException {
        try {
            return (String[]) type.getModuleClass().getMethod("acquireVersionList").invoke(null);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (IOException) e.getCause();
        }
    }

    @Nullable
    abstract URL getDownloadLink() throws IOException;

    /** Blocking */
    protected Downloader getDownloader(File destFolder) throws IOException {
        return new Downloader(getDownloadLink(), destFolder,getType().getName());
    }

    /** Blocking */
    public abstract Downloader download() throws IOException;

    public abstract void loadJars();

    static ExternalModule getInstalledInstance(ModuleType type) {
        if (type.isExternal()) {
            try {
                File version = new File(FileManager.MODULES, type.getName() + "\\__version");
                if (version.exists() && !version.isDirectory()) {
                    FileReader reader = new FileReader(version);
                    char[] a = new char[256];
                    reader.read(a);
                    String ver = new String(a);
                    ExternalModule mod = (ExternalModule) type.getModuleClass().getDeclaredConstructor(String.class).newInstance(ver);
                    mod.loadJars();
                    return mod;
                }
                return null;
            } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException | IOException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("Non-downloadable module!");
    }
}

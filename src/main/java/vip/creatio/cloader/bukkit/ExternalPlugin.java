package vip.creatio.cloader.bukkit;

import vip.creatio.cloader.ccl.FileManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/** A dummy plugin for external classes */
public class ExternalPlugin extends JavaPlugin {

    private ExternalPlugin() {
        throw new RuntimeException("Boom!");
    }

    @Nullable
    @Override
    public InputStream getResource(@NotNull String filename) {
        File f = new File(FileManager.DATA, filename);
        try {
            return new FileInputStream(f);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override
    public void reloadConfig() {}

    @Override
    public void saveConfig() {}

    @Override
    public void saveDefaultConfig() {}

    @Override
    public void saveResource(@NotNull String resourcePath, boolean replace) {}
}

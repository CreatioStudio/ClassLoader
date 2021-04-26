package vip.creatio.cloader.bukkit;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.Arrays;
import java.util.List;

public class Config {

    public static FileConfiguration load(JavaPlugin plugin, String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public static void updateConfig(JavaPlugin plugin, String name, int version) {
        File file = new File(plugin.getDataFolder(), name);
        if (file.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            if ((version > config.getInt("version")) || (version == -1)) {
                try {
                    if (version != -1) {
                        CLoader.log("&8&l[&a&lClass&2&lLoader&8&l] &aFile &6&l&n" + name + "&a is out-of-date, auto updating...");
                        config.set("version", version);
                        config.save(file);
                    }
                    //byte[] data = new byte[1024];
                    //new FileInputStream(file).read(data);
                    updateYML(file, YamlConfiguration.loadConfiguration(new InputStreamReader(plugin.getResource(name))));
                } catch (IOException e) {
                    CLoader.intern("&8&l[&a&lClass&2&lLoader&8&l] &4File &6&l&n" + name + "&4 load failed, stack trace:");
                    e.printStackTrace();
                    return;
                }
                if (version != -1) CLoader.log("&8&l[&a&lClass&2&lLoader&8&l] &fFile &6&l&n" + name + "&f successfully updated.");
            }
        } else {
            if (plugin.getResource(name) == null) {
                CLoader.intern("Unable to create default file: " + name + ", file not exist!");
                return;
            }

            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                CLoader.intern("Exception while creating default files!");
            }

            try (BufferedInputStream i = new BufferedInputStream(plugin.getResource(name));
                 BufferedOutputStream o = new BufferedOutputStream(new FileOutputStream(file))) {

                byte[] bytes = new byte[i.available()];
                i.read(bytes);
                o.write(bytes);

                o.flush();

            } catch (IOException e) {
                CLoader.intern("Exception while writing default files!");
                e.printStackTrace();
            }
        }
    }

    public static void updateYML(File old, FileConfiguration sample) throws IOException {
        FileConfiguration config = YamlConfiguration.loadConfiguration(old);
        List<String> keys = Arrays.asList(config.getKeys(true).toArray(new String[0]));
        String[] ori = sample.getKeys(true).toArray(new String[0]);

        for (String value : ori) {
            if (!keys.contains(value)) config.set(value, sample.get(value));
        }
        config.options().copyDefaults(true);
        config.save(old);
    }
}


package vip.creatio.cloader.bukkit;

import org.bukkit.configuration.Configuration;
import vip.creatio.basic.cmd.CommandManager;
import vip.creatio.basic.cmd.CommandRegister;
import vip.creatio.basic.config.Configs;
import vip.creatio.basic.tools.FormatMsgManager;
import vip.creatio.cloader.ccl.FileManager;
import vip.creatio.cloader.ccl.RuntimeExecutor;
import vip.creatio.cloader.ccl.module.Module;
import vip.creatio.cloader.ccl.module.ScriptFile;
import vip.creatio.cloader.cmd.CommandCL;
import vip.creatio.cloader.cmd.CommandEval;
import vip.creatio.cloader.cmd.CommandSystem;
import vip.creatio.cloader.util.ClassUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public final class CLoader extends JavaPlugin {

    private static CLoader          instance;
    private int                     configVersion;
    private boolean                 enable_update_checker;
    private boolean                 enable_metrics;
    private boolean                 auto_compile_java_files;
    private boolean                 ingame_code_privilege;
    private boolean                 allow_command_block_execute;

    private FileConfiguration       config;

    private FormatMsgManager        msg;
    private CommandRegister         cmdRegister;

    private final ExecutorService   threadPool = Executors.newCachedThreadPool();
    private final RuntimeExecutor   executor = new RuntimeExecutor();
    private final JavaPlugin        virtual;

    public CLoader() {
        if (instance != null) throw new RuntimeException("Already instanced");
        instance = this;
        loadAllConfig();

        virtual = BukkitUtils.getVirtualInstance(this);
    }

    @Override
    public void onLoad() {
        msg.sendStatic(Level.INFO, "MAIN.LOADING.CONFIG");

        Module.init();

        ClassUtil.init();
    }

    @Override
    public void onEnable() {

        msg.sendStatic(Level.INFO, "MAIN.LOADING.TITLE");
        msg.sendStatic(Level.INFO, "MAIN.LOADING.COMMAND");

        //Register
        cmdRegister = new CommandManager(this);
        CommandCL.register(cmdRegister);
        CommandEval.register(cmdRegister);
        CommandSystem.register(cmdRegister);

        if (auto_compile_java_files) {
            msg.sendStatic(Level.INFO, "MAIN.SOURCE.COMPILATION.AUTO");
            msg.log(Level.INFO, Module.getJava().__autoCompileAll());
        }

        msg.sendStatic(Level.INFO, "MAIN.LOADING.CLASS");

        //Load all classes
        int count = 0;
        for (File f : FileManager.getClasses()) {
            try {
                ScriptFile sf = new ScriptFile(f);
                if (Module.getJava().getImpl().load(sf)) count++;
                //TODO: Raw Exception handling
            } catch (RuntimeException | FileNotFoundException ignored) {} catch (Exception e) {
                e.printStackTrace();
            }
        }

        msg.sendStatic(Level.INFO, "MAIN.CLASS.LOADING.LOADED", "" + count);

        msg.sendStatic(Level.INFO, "MAIN.LOADING.FINISHED");

    }

    @Override
    public void onDisable() {
        Module.getJava().unload(Module.getJava().getExternalClasses().toArray(new Class[0]));
    }

    public void loadAllConfig() {
        loadConfig();
        loadLanguageConfig();
    }

    //Config setter
    public void loadConfig() {
        Configs.updateConfig(instance, "config.yml", configVersion);
        config =                                Configs.load(instance, "config.yml");

        configVersion =                         config.getInt("version");
        enable_update_checker =                 config.getBoolean("enable_update_checker", true);
        enable_metrics =                        config.getBoolean("enable_metrics", true);
        auto_compile_java_files =               config.getBoolean("auto_compile_java_files", false);
        ingame_code_privilege =             config.getBoolean("ingame_code_privilege", false);
        allow_command_block_execute =           config.getBoolean("allow_command_block_execute", true);
    }

    public void loadLanguageConfig() {
        String lang = config.getString("language", "en_US");
        Configs.updateConfig(this, "lang/" + lang + ".yml", -1);
        Configuration langConfig = Configs.load(this, "lang/" + lang + ".yml");
        this.msg = new FormatMsgManager(langConfig);
        this.msg.addReplacerToPath("%prefix%", "MAIN.FORMAT.PREFIX");
        this.msg.addReplacer("%w%", langConfig.getString("MAIN.FORMAT.WARN", "&e"));
        this.msg.addReplacer("%e%", langConfig.getString("MAIN.FORMAT.ERROR", "&c"));
        this.msg.addReplacer("%n%", langConfig.getString("MAIN.FORMAT.NORMAL", "&7"));
        this.msg.addReplacer("%s%", langConfig.getString("MAIN.FORMAT.SUCCESS", "&a"));
        this.msg.addReplacer("%h%", langConfig.getString("MAIN.FORMAT.HIGHLIGHT", "&6"));
    }

    public static FormatMsgManager getMsgSender() {
        return instance.msg;
    }

    public static void log(String msg) {
        instance.msg.log(msg);
    }

    public static void intern(String msg) {
        instance.msg.intern(msg);
    }

    public static CLoader getInstance() {
        return instance;
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }

    public int configVersion() {
        return configVersion;
    }

    public RuntimeExecutor getRuntimeExecutor() {
        return executor;
    }

    @NotNull @Override
    public FileConfiguration getConfig() {
        return config;
    }

    public boolean enableUpdateChecker() {
        return enable_update_checker;
    }

    public boolean enableMetrics() {
        return enable_metrics;
    }

    public boolean autoCompileJavaFiles() {
        return auto_compile_java_files;
    }

    public boolean allowPlayersExecuteDos() {
        return ingame_code_privilege;
    }

    public boolean allowCommandBlockExecute() {
        return allow_command_block_execute;
    }

    public CommandRegister getCommandRegister() {
        return cmdRegister;
    }

    public JavaPlugin getVirtual() {
        return virtual;
    }

    File getFile0() {
        return super.getFile();
    }

    java.lang.ClassLoader getClassLoader0() {
        return super.getClassLoader();
    }
}

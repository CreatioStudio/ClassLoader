package vip.creatio.cloader.ccl.module;

import vip.creatio.clib.basic.tools.FormatMsgManager;
import vip.creatio.cloader.bukkit.CLoader;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public abstract class Module {

    public static final FormatMsgManager msg = CLoader.getMsgSender();

    public abstract ModuleType getType();

    abstract ModuleImpl getImpl();




    static final Set<ExternalModule> INSTALLED = new HashSet<>();

    static Java JAVA_MODULE;

    public static void init() {
        JAVA_MODULE = new Java();
        for (ModuleType type : ModuleType.externals()) {
            ExternalModule m = ExternalModule.getInstalledInstance(type);
            if (m != null) INSTALLED.add(m);
        }
    }


    public static EnumSet<ModuleType> getInstalled() {
        EnumSet<ModuleType> type = EnumSet.noneOf(ModuleType.class);
        for (ExternalModule mod : INSTALLED) {
            type.add(mod.getType());
        }
        return type;
    }

    public static EnumSet<ModuleType> getNotInstalled() {
        EnumSet<ModuleType> type = EnumSet.copyOf(Arrays.asList(ModuleType.externals()));
        for (ExternalModule mod : INSTALLED) {
            type.remove(mod.getType());
        }
        return type;
    }

    public static Java getJava() {
        return JAVA_MODULE;
    }

    public static void install(CommandSender sender, ModuleType type, String version) {
        if (!type.isExternal()) throw new RuntimeException("Uninstallable module!");
        try {
            ExternalModule mod = (ExternalModule) type.getModuleClass().getDeclaredConstructor(String.class).newInstance(version);
            try {
                URL link = mod.getDownloadLink();
                if (link == null) {
                    msg.sendStatic(sender, "MAIN.DOWNLOAD.INVALID_URL", type.getName());
                    return;
                }

                Downloader d = mod.download();

                d.setExceptionLogic((e) -> {
                    msg.sendStatic(sender, "MAIN.DOWNLOAD.FAILED", type.getName(), link.toString());
                    e.printStackTrace();
                });

                msg.sendStatic(sender, "MAIN.DOWNLOAD.START");

                d.start();

                boolean connected = false;
                int percent = 1;
                while (d.getProcess() < 100) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (!connected && d.getProcess() >= 0) {
                        connected = true;
                        msg.sendStatic(sender, "MAIN.DOWNLOAD.BEGIN", d.getProduct().getName());
                    }

                    if ((d.getProcess() / (percent * 10)) >= 1) {

                        msg.sendStatic(sender, "MAIN.DOWNLOAD.PROGRESS",
                                "" + percent * 10/*,        //TODO: make this!
                                StringUtil.getD1().format(d.getDownloadSpeed() / 1024.0)*/);

                        percent++;
                    }
                }
                msg.sendStatic(sender, "MAIN.DOWNLOAD.DONE", d.getProduct().getName());
                mod.loadJars();
                INSTALLED.add(mod);

                File f = new File(d.getDest(), "__version");
                f.createNewFile();
                FileWriter writer = new FileWriter(f);
                writer.write(mod.version);
                writer.close();

            } catch (IOException e) {
                try {
                    msg.sendStatic(sender, "MAIN.DOWNLOAD.FAILED", type.getName(), mod.getDownloadLink().toString());
                } catch (IOException ee) {
                    throw new RuntimeException(ee);
                }
            }
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}

package vip.creatio.cloader.ccl.module;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;

public class ScriptFile extends File {

    private final String suffix;

    public ScriptFile(@NotNull File d) throws FileNotFoundException {
        super(d.getAbsolutePath());
        if (!exists()) throw new FileNotFoundException(getAbsolutePath());
        suffix = getName().substring(getName().lastIndexOf("."));
    }

    public ScriptFile(File parent, @NotNull String child) throws FileNotFoundException {
        super(parent, child);
        if (!exists()) throw new FileNotFoundException(getAbsolutePath());
        suffix = getName().substring(getName().lastIndexOf("."));
    }

    public boolean checkType(String suffix) {
        return this.suffix.equalsIgnoreCase(suffix);
    }

    public static boolean checkType(String suffix, @NotNull ScriptFile... files) {
        for (ScriptFile sf : files) {
            if (!sf.checkType(suffix)) return false;
        }
        return true;
    }

    public String getType() {
        return suffix;
    }
}

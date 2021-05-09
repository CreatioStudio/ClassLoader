package vip.creatio.cloader.ccl.module;

import vip.creatio.basic.tools.FormatMsgManager;
import vip.creatio.cloader.bukkit.CLoader;
import vip.creatio.cloader.ccl.CompilationException;
import vip.creatio.cloader.ccl.CompilationTask;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public abstract class ModuleImpl {

    protected static final FormatMsgManager msg = CLoader.getMsgSender();

    protected Charset encoding = StandardCharsets.UTF_8;       //Charset.availableCharsets()


    public abstract CompilationTask compile(File outputDir) throws CompilationException;

    //TODO: Raw and unclear exception throwing, a special "ScriptLoadException" need to be added.
    public abstract Object run(ScriptFile script, Object... invoke) throws Exception;

    public abstract boolean load(ScriptFile script) throws Exception;

    public abstract boolean unload(ScriptFile script) throws Exception;

    public abstract String suffix();

    public abstract ScriptFile[] list();

    public Charset getEncoding() {
        return encoding;
    }

    public void setEndocing(Charset encoding) {
        this.encoding = encoding;
    }

}

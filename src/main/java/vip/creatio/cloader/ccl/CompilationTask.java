package vip.creatio.cloader.ccl;

import vip.creatio.cloader.ccl.module.ScriptFile;

import java.io.File;

public interface CompilationTask {

    boolean compile(ScriptFile... files) throws CompilationException;

    String[] reportDiagnostic();

    void setOptions(String... options);

    File getOutputDir();
}

package vip.creatio.cloader.ccl.module;

import vip.creatio.cloader.ccl.CompilationException;
import vip.creatio.cloader.ccl.CompilationTask;
import groovy.lang.GroovyClassLoader;

import java.io.File;

class GroovyImpl extends ModuleImpl {

    private GroovyClassLoader clsLoader = new GroovyClassLoader();
    //private GroovyScriptEngine scriptEngine = new GroovyScriptEngine(clsLoader);

//    GroovyImpl() {
//        GroovyClassLoader loader = new GroovyClassLoader();
//        ScriptEngine
//    }

    @Override
    public CompilationTask compile(File outputDir) throws CompilationException {
        return null;
    }

    @Override
    public Object run(ScriptFile script, Object... invoke) throws Exception {
        return false;
    }

    @Override
    public boolean load(ScriptFile script) throws Exception {
        return false;
    }

    @Override
    public boolean unload(ScriptFile script) throws Exception {
        return false;
    }

    @Override
    public String suffix() {
        return ".groovy";
    }

    @Override
    public ScriptFile[] list() {
        return new ScriptFile[0];
    }


}

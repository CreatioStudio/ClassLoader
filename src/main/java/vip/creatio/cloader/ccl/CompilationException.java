package vip.creatio.cloader.ccl;

public class CompilationException extends Exception {

    public final Type type;

    public CompilationException(Type type, String msg) {
        super(msg);
        this.type = type;
    }

    public CompilationException(Type type) {
        super();
        this.type = type;
    }

    public CompilationException(Type type, Throwable reason) {
        super(reason);
        this.type = type;
    }

    @Override
    public String toString() {
        String message = (this.getCause() != this)
                ? this.getCause().getLocalizedMessage() : getLocalizedMessage();
        String s = getClass().getName();

        return (message != null) ? (s + ": " + message) : (s + ": " + type.name());
    }

    public enum Type {
        FILE_NOT_FOUND,
        FILE_TYPE_MISMATCH,
        CANT_SET_LOCATION,
        BAD_OPTIONS,
        INTERNAL_ERROR;
    }
}

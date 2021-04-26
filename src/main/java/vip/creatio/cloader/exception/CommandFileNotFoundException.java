package vip.creatio.cloader.exception;

public class CommandFileNotFoundException extends RuntimeException {
    public final String input;

    public CommandFileNotFoundException(String input) {
        super("File " + input + " not found!");
        this.input = input;
    }
}

package vip.creatio.cloader.exception;

public class CommandNumberFormatException extends RuntimeException {
    public final String input;
    public final NumberFormat require;

    public CommandNumberFormatException(String input, NumberFormat requires) {
        super();
        this.input = input;
        this.require = requires;
    }

    public enum NumberFormat {
        Int,
        Positive,
        NotNegative,
        NotZero;
    }
}

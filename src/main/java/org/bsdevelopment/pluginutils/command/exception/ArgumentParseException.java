package org.bsdevelopment.pluginutils.command.exception;

public class ArgumentParseException extends Exception {
    public ArgumentParseException(String message) {
        super(message);
    }

    public static ArgumentParseException fromString(String message) {
        return new ArgumentParseException(message);
    }
}

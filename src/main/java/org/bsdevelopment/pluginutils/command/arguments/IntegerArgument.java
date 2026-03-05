package org.bsdevelopment.pluginutils.command.arguments;

import org.bsdevelopment.pluginutils.command.arguments.suggestions.SuggestionInfo;
import org.bsdevelopment.pluginutils.command.exception.ArgumentParseException;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.Collections;

public class IntegerArgument extends Argument<Integer> {
    private final int min;
    private final int max;

    public IntegerArgument(String nodeName) {
        this(nodeName, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public IntegerArgument(String nodeName, int min, int max) {
        super(nodeName);
        this.min = min;
        this.max = max;
    }

    @Override
    public Integer parse(CommandSender sender, String input) throws ArgumentParseException {
        try {
            int value = Integer.parseInt(input);
            if (value < min || value > max) {
                throw ArgumentParseException.fromString("Value must be between " + min + " and " + max + ", got: " + value);
            }
            return value;
        } catch (NumberFormatException e) {
            throw ArgumentParseException.fromString("Expected an integer, got: " + input);
        }
    }

    @Override
    public Collection<String> suggest(SuggestionInfo info) {
        return Collections.emptyList();
    }
}

package org.bsdevelopment.pluginutils.command.arguments;

import org.bsdevelopment.pluginutils.command.arguments.suggestions.SuggestionInfo;
import org.bsdevelopment.pluginutils.command.exception.ArgumentParseException;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.Collections;

public class LongArgument extends Argument<Long> {
    private final long min;
    private final long max;

    public LongArgument(String nodeName) {
        this(nodeName, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    public LongArgument(String nodeName, long min, long max) {
        super(nodeName);
        this.min = min;
        this.max = max;
    }

    @Override
    public Long parse(CommandSender sender, String input) throws ArgumentParseException {
        try {
            long value = Long.parseLong(input);
            if (value < min || value > max) {
                throw ArgumentParseException.fromString("Value must be between " + min + " and " + max + ", got: " + value);
            }
            return value;
        } catch (NumberFormatException e) {
            throw ArgumentParseException.fromString("Expected a whole number, got: " + input);
        }
    }

    @Override
    public Collection<String> suggest(SuggestionInfo info) {
        return Collections.emptyList();
    }
}

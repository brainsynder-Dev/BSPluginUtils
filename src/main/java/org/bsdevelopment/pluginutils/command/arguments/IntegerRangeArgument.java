package org.bsdevelopment.pluginutils.command.arguments;

import org.bsdevelopment.pluginutils.command.arguments.range.IntegerRange;
import org.bsdevelopment.pluginutils.command.arguments.suggestions.SuggestionInfo;
import org.bsdevelopment.pluginutils.command.exception.ArgumentParseException;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.Collections;

public class IntegerRangeArgument extends Argument<IntegerRange> {
    public IntegerRangeArgument(String nodeName) {
        super(nodeName);
    }

    @Override
    public IntegerRange parse(CommandSender sender, String input) throws ArgumentParseException {
        try {
            if (input.contains("..")) {
                String[] parts = input.split("\\.\\.", 2);
                int min = Integer.parseInt(parts[0]);
                int max = Integer.parseInt(parts[1]);
                if (min > max) throw ArgumentParseException.fromString("Range min cannot exceed max: " + input);
                return new IntegerRange(min, max);
            } else {
                int value = Integer.parseInt(input);
                return new IntegerRange(value, value);
            }
        } catch (NumberFormatException e) {
            throw ArgumentParseException.fromString("Invalid integer range: " + input);
        }
    }

    @Override
    public Collection<String> suggest(SuggestionInfo info) {
        return Collections.emptyList();
    }
}

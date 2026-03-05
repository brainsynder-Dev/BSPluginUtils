package org.bsdevelopment.pluginutils.command.arguments;

import org.bsdevelopment.pluginutils.command.arguments.range.DoubleRange;
import org.bsdevelopment.pluginutils.command.arguments.suggestions.SuggestionInfo;
import org.bsdevelopment.pluginutils.command.exception.ArgumentParseException;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.Collections;

public class DoubleRangeArgument extends Argument<DoubleRange> {
    public DoubleRangeArgument(String nodeName) {
        super(nodeName);
    }

    @Override
    public DoubleRange parse(CommandSender sender, String input) throws ArgumentParseException {
        try {
            if (input.contains("..")) {
                String[] parts = input.split("\\.\\.", 2);
                double min = Double.parseDouble(parts[0]);
                double max = Double.parseDouble(parts[1]);
                if (min > max) throw ArgumentParseException.fromString("Range min cannot exceed max: " + input);
                return new DoubleRange(min, max);
            } else {
                double value = Double.parseDouble(input);
                return new DoubleRange(value, value);
            }
        } catch (NumberFormatException e) {
            throw ArgumentParseException.fromString("Invalid double range: " + input);
        }
    }

    @Override
    public Collection<String> suggest(SuggestionInfo info) {
        return Collections.emptyList();
    }
}

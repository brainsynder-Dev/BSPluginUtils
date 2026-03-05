package org.bsdevelopment.pluginutils.command.arguments;

import org.bsdevelopment.pluginutils.command.arguments.suggestions.SuggestionInfo;
import org.bsdevelopment.pluginutils.command.exception.ArgumentParseException;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.Collections;

public class DoubleArgument extends Argument<Double> {
    private final double min;
    private final double max;

    public DoubleArgument(String nodeName) {
        this(nodeName, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    public DoubleArgument(String nodeName, double min, double max) {
        super(nodeName);
        this.min = min;
        this.max = max;
    }

    @Override
    public Double parse(CommandSender sender, String input) throws ArgumentParseException {
        try {
            double value = Double.parseDouble(input);
            if (value < min || value > max) {
                throw ArgumentParseException.fromString("Value must be between " + min + " and " + max + ", got: " + value);
            }
            return value;
        } catch (NumberFormatException e) {
            throw ArgumentParseException.fromString("Expected a number, got: " + input);
        }
    }

    @Override
    public Collection<String> suggest(SuggestionInfo info) {
        return Collections.emptyList();
    }
}

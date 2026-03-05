package org.bsdevelopment.pluginutils.command.arguments;

import org.bsdevelopment.pluginutils.command.arguments.suggestions.SuggestionInfo;
import org.bsdevelopment.pluginutils.command.exception.ArgumentParseException;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.Collections;

public class FloatArgument extends Argument<Float> {
    private final float min;
    private final float max;

    public FloatArgument(String nodeName) {
        this(nodeName, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
    }

    public FloatArgument(String nodeName, float min, float max) {
        super(nodeName);
        this.min = min;
        this.max = max;
    }

    @Override
    public Float parse(CommandSender sender, String input) throws ArgumentParseException {
        try {
            float value = Float.parseFloat(input);
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

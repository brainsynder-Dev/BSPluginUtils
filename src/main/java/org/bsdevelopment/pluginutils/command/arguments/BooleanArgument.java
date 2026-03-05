package org.bsdevelopment.pluginutils.command.arguments;

import org.bsdevelopment.pluginutils.command.arguments.suggestions.SuggestionInfo;
import org.bsdevelopment.pluginutils.command.exception.ArgumentParseException;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class BooleanArgument extends Argument<Boolean> {
    private static final List<String> SUGGESTIONS = Arrays.asList("true", "false");

    public BooleanArgument(String nodeName) {
        super(nodeName);
    }

    @Override
    public Boolean parse(CommandSender sender, String input) throws ArgumentParseException {
        return switch (input.toLowerCase()) {
            case "true", "yes", "on" -> true;
            case "false", "no", "off" -> false;
            default -> throw ArgumentParseException.fromString("Expected true/false, got: " + input);
        };
    }

    @Override
    public Collection<String> suggest(SuggestionInfo info) {
        return SUGGESTIONS;
    }
}

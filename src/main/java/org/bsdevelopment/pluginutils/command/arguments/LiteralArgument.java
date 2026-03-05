package org.bsdevelopment.pluginutils.command.arguments;

import org.bsdevelopment.pluginutils.command.arguments.suggestions.SuggestionInfo;
import org.bsdevelopment.pluginutils.command.exception.ArgumentParseException;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.List;

public class LiteralArgument extends Argument<String> {
    private final String literal;

    public LiteralArgument(String literal) {
        super(literal);
        this.literal = literal;
    }

    public String getLiteral() {
        return literal;
    }

    @Override
    public String parse(CommandSender sender, String input) throws ArgumentParseException {
        if (!input.equalsIgnoreCase(literal)) {
            throw ArgumentParseException.fromString("Expected '" + literal + "', got: " + input);
        }
        return literal;
    }

    @Override
    public Collection<String> suggest(SuggestionInfo info) {
        return List.of(literal);
    }
}

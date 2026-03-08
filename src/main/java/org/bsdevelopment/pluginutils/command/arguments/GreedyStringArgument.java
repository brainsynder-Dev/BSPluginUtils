package org.bsdevelopment.pluginutils.command.arguments;

import org.bsdevelopment.pluginutils.command.arguments.suggestions.SuggestionInfo;
import org.bsdevelopment.pluginutils.command.exception.ArgumentParseException;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.Collections;

public class GreedyStringArgument extends Argument<String> implements GreedyArgument {
    public GreedyStringArgument(String nodeName) {
        super(nodeName);
    }

    @Override
    public String parse(CommandSender sender, String input) throws ArgumentParseException {
        return input;
    }

    @Override
    public Collection<String> suggest(SuggestionInfo info) {
        return Collections.emptyList();
    }

    public boolean isGreedy() {
        return true;
    }
}

package org.bsdevelopment.pluginutils.command.arguments.suggestions;

import org.bsdevelopment.pluginutils.command.CommandArguments;
import org.bukkit.command.CommandSender;

public record SuggestionInfo(
        CommandSender sender,
        CommandArguments previousArgs,
        String currentInput,
        String rawInput
) {
}

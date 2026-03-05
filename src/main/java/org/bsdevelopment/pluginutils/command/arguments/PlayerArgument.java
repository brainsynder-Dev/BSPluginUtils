package org.bsdevelopment.pluginutils.command.arguments;

import org.bsdevelopment.pluginutils.command.arguments.suggestions.SuggestionInfo;
import org.bsdevelopment.pluginutils.command.exception.ArgumentParseException;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.stream.Collectors;

public class PlayerArgument extends Argument<Player> {
    public PlayerArgument(String nodeName) {
        super(nodeName);
    }

    @Override
    public Player parse(CommandSender sender, String input) throws ArgumentParseException {
        Player player = Bukkit.getPlayerExact(input);
        if (player == null) {
            throw ArgumentParseException.fromString("Player not found: " + input);
        }
        return player;
    }

    @Override
    public Collection<String> suggest(SuggestionInfo info) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }
}

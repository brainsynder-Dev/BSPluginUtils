package org.bsdevelopment.pluginutils.command.arguments;

import org.bsdevelopment.pluginutils.command.arguments.suggestions.SuggestionInfo;
import org.bsdevelopment.pluginutils.command.exception.ArgumentParseException;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.stream.Collectors;

public class WorldArgument extends Argument<World> {
    public WorldArgument(String nodeName) {
        super(nodeName);
    }

    @Override
    public World parse(CommandSender sender, String input) throws ArgumentParseException {
        World world = Bukkit.getWorld(input);
        if (world == null) {
            throw ArgumentParseException.fromString("World not found: " + input);
        }
        return world;
    }

    @Override
    public Collection<String> suggest(SuggestionInfo info) {
        return Bukkit.getWorlds().stream()
                .map(World::getName)
                .collect(Collectors.toList());
    }
}

package org.bsdevelopment.pluginutils.command.arguments;

import org.bsdevelopment.pluginutils.command.arguments.suggestions.SuggestionInfo;
import org.bsdevelopment.pluginutils.command.exception.ArgumentParseException;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class EntitySelectorArgument extends Argument<List<Entity>> {

    private final SelectionType selectionType;

    private EntitySelectorArgument(String nodeName, SelectionType selectionType) {
        super(nodeName);
        this.selectionType = selectionType;
    }

    public static EntitySelectorArgument oneEntity(String nodeName) {
        return new EntitySelectorArgument(nodeName, SelectionType.ONE_ENTITY);
    }

    public static EntitySelectorArgument onePlayer(String nodeName) {
        return new EntitySelectorArgument(nodeName, SelectionType.ONE_PLAYER);
    }

    public static EntitySelectorArgument manyPlayers(String nodeName) {
        return new EntitySelectorArgument(nodeName, SelectionType.MANY_PLAYERS);
    }

    public static EntitySelectorArgument manyEntities(String nodeName) {
        return new EntitySelectorArgument(nodeName, SelectionType.MANY_ENTITIES);
    }

    @Override
    public List<Entity> parse(CommandSender sender, String input) throws ArgumentParseException {
        try {
            List<Entity> entities = Bukkit.selectEntities(sender, input);
            if (entities.isEmpty()) {
                throw ArgumentParseException.fromString("No entities found for selector: " + input);
            }
            return switch (selectionType) {
                case ONE_ENTITY -> {
                    if (entities.size() > 1) throw ArgumentParseException.fromString("Selector matched more than one entity");
                    yield entities;
                }
                case ONE_PLAYER -> {
                    List<Entity> players = entities.stream().filter(e -> e instanceof Player).collect(Collectors.toList());
                    if (players.isEmpty()) throw ArgumentParseException.fromString("No player found for selector: " + input);
                    if (players.size() > 1) throw ArgumentParseException.fromString("Selector matched more than one player");
                    yield players;
                }
                case MANY_PLAYERS -> entities.stream().filter(e -> e instanceof Player).collect(Collectors.toList());
                case MANY_ENTITIES -> entities;
            };
        } catch (IllegalArgumentException e) {
            throw ArgumentParseException.fromString("Invalid entity selector: " + input);
        }
    }

    @Override
    public Collection<String> suggest(SuggestionInfo info) {
        return Arrays.asList("@a", "@e", "@p", "@r", "@s");
    }

    public enum SelectionType {ONE_ENTITY, ONE_PLAYER, MANY_PLAYERS, MANY_ENTITIES}
}

package org.bsdevelopment.pluginutils.command.arguments;

import org.bsdevelopment.pluginutils.command.arguments.suggestions.SuggestionInfo;
import org.bsdevelopment.pluginutils.command.exception.ArgumentParseException;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RegistryArgument<T extends Keyed> extends Argument<T> {
    private final Registry<T> registry;

    public RegistryArgument(String nodeName, Registry<T> registry) {
        super(nodeName);
        this.registry = registry;
    }

    @Override
    public T parse(CommandSender sender, String input) throws ArgumentParseException {
        NamespacedKey key = NamespacedKey.fromString(input.toLowerCase());
        if (key == null) {
            throw ArgumentParseException.fromString("Invalid key format: " + input);
        }

        T value = registry.get(key);
        if (value == null) {
            // Retry with minecraft: namespace if no namespace was given
            if (!input.contains(":")) {
                value = registry.get(NamespacedKey.minecraft(input.toLowerCase()));
            }
            if (value == null) {
                throw ArgumentParseException.fromString("Unknown registry entry: " + input);
            }
        }
        return value;
    }

    @Override
    public Collection<String> suggest(SuggestionInfo info) {
        List<String> keys = new ArrayList<>();
        for (T entry : registry) {
            NamespacedKey key = entry.getKey();
            keys.add(key.getNamespace().equals(NamespacedKey.MINECRAFT) ? key.getKey() : key.toString());
        }
        return keys;
    }
}

package org.bsdevelopment.pluginutils.command.arguments;

import org.bsdevelopment.pluginutils.command.arguments.suggestions.SuggestionInfo;
import org.bsdevelopment.pluginutils.command.exception.ArgumentParseException;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class EnumArgument<E extends Enum<E>> extends Argument<E> {
    private final Class<E> enumClass;

    public EnumArgument(String nodeName, Class<E> enumClass) {
        super(nodeName);
        this.enumClass = enumClass;
    }

    @Override
    public E parse(CommandSender sender, String input) throws ArgumentParseException {
        try {
            return Enum.valueOf(enumClass, input.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ArgumentParseException.fromString("Invalid value: " + input);
        }
    }

    @Override
    public Collection<String> suggest(SuggestionInfo info) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(e -> e.name().toLowerCase())
                .collect(Collectors.toList());
    }
}

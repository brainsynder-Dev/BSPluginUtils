package org.bsdevelopment.pluginutils.command.arguments;

import org.bsdevelopment.pluginutils.command.CommandArguments;
import org.bsdevelopment.pluginutils.command.arguments.suggestions.SuggestionInfo;
import org.bsdevelopment.pluginutils.command.exception.ArgumentParseException;
import org.bukkit.command.CommandSender;

import java.util.Collection;

public class CustomArgument<T, B> extends Argument<T> {
    private final Argument<B> base;
    private final CustomArgumentParser<T> parser;

    public CustomArgument(Argument<B> base, CustomArgumentParser<T> parser) {
        super(base.getNodeName());
        this.base = base;
        this.parser = parser;
    }

    @Override
    public T parse(CommandSender sender, String input) throws ArgumentParseException {
        return parser.parse(new CustomArgumentInfo(input, sender, null));
    }

    @Override
    public Collection<String> suggest(SuggestionInfo info) {
        return base.getSuggestions(info);
    }

    public Argument<B> getBase() {
        return base;
    }

    @FunctionalInterface
    public interface CustomArgumentParser<T> {
        T parse(CustomArgumentInfo info) throws ArgumentParseException;
    }

    public record CustomArgumentInfo(String input, CommandSender sender, CommandArguments previousArgs) {
    }
}

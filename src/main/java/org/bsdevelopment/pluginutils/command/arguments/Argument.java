package org.bsdevelopment.pluginutils.command.arguments;

import org.bsdevelopment.pluginutils.command.CommandPermission;
import org.bsdevelopment.pluginutils.command.arguments.suggestions.ArgumentSuggestions;
import org.bsdevelopment.pluginutils.command.arguments.suggestions.SuggestionInfo;
import org.bsdevelopment.pluginutils.command.exception.ArgumentParseException;
import org.bukkit.command.CommandSender;

import java.util.Collection;

public abstract class Argument<T> {
    private final String nodeName;
    private boolean optional = false;
    private CommandPermission permission = CommandPermission.NONE;
    private ArgumentSuggestions overrideSuggestions = null;
    private ArgumentSuggestions includeSuggestions = null;

    protected Argument(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getNodeName() {
        return nodeName;
    }

    public boolean isOptional() {
        return optional;
    }

    public Argument<T> setOptional(boolean optional) {
        this.optional = optional;
        return this;
    }

    public abstract T parse(CommandSender sender, String input) throws ArgumentParseException;

    public abstract Collection<String> suggest(SuggestionInfo info);

    public Argument<T> replaceSuggestions(ArgumentSuggestions suggestions) {
        this.overrideSuggestions = suggestions;
        return this;
    }

    public Argument<T> includeSuggestions(ArgumentSuggestions suggestions) {
        this.includeSuggestions = suggestions;
        return this;
    }

    public Argument<T> withPermission(CommandPermission permission) {
        this.permission = permission;
        return this;
    }

    public CommandPermission getPermission() {
        return permission;
    }

    public Collection<String> getSuggestions(SuggestionInfo info) {
        if (overrideSuggestions != null) {
            return overrideSuggestions.suggest(info);
        }
        Collection<String> base = suggest(info);
        if (includeSuggestions != null) {
            java.util.List<String> combined = new java.util.ArrayList<>(base);
            combined.addAll(includeSuggestions.suggest(info));
            return combined;
        }
        return base;
    }
}

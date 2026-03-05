package org.bsdevelopment.pluginutils.command.arguments.suggestions;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@FunctionalInterface
public interface ArgumentSuggestions {
    static ArgumentSuggestions of(String... values) {
        List<String> list = Arrays.asList(values);
        return info -> list;
    }

    static ArgumentSuggestions of(Collection<String> values) {
        return info -> values;
    }

    static ArgumentSuggestions of(Function<SuggestionInfo, Collection<String>> function) {
        return function::apply;
    }

    static ArgumentSuggestions empty() {
        return info -> Collections.emptyList();
    }

    Collection<String> suggest(SuggestionInfo info);
}

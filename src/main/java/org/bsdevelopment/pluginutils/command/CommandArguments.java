package org.bsdevelopment.pluginutils.command;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CommandArguments {
    public static final CommandArguments EMPTY = new CommandArguments(Collections.emptyMap(), Collections.emptyMap());

    private final Map<String, Object> namedArgs;
    private final Map<Integer, Object> indexedArgs;

    public CommandArguments(Map<String, Object> namedArgs, Map<Integer, Object> indexedArgs) {
        this.namedArgs = Collections.unmodifiableMap(namedArgs);
        this.indexedArgs = Collections.unmodifiableMap(indexedArgs);
    }

    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String name) {
        return (T) namedArgs.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(int index) {
        return (T) indexedArgs.get(index);
    }

    public <T> T getOrDefault(String name, T defaultValue) {
        T value = get(name);
        return value != null ? value : defaultValue;
    }

    public boolean has(String name) {
        return namedArgs.containsKey(name);
    }

    public int size() {
        return namedArgs.size();
    }

    public static class Builder {
        private final Map<String, Object> named = new HashMap<>();
        private final Map<Integer, Object> indexed = new HashMap<>();
        private int nextIndex = 0;

        public Builder put(String name, Object value) {
            named.put(name, value);
            indexed.put(nextIndex++, value);
            return this;
        }

        public CommandArguments build() {
            return new CommandArguments(named, indexed);
        }
    }
}

package org.bsdevelopment.pluginutils.chat.components;

import org.bsdevelopment.pluginutils.chat.Component;
import org.bsdevelopment.pluginutils.chat.Style;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A translatable component (for i18n).
 *
 * @param key
 *         the translation key
 * @param args
 *         format arguments (other components)
 * @param style
 *         styling information
 */
public record TranslatableComponent(String key, List<Component> args, Style style) implements Component {
    public TranslatableComponent {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(args, "args");
        Objects.requireNonNull(style, "style");
    }

    @Override
    public List<Component> children() {
        return Collections.unmodifiableList(args);
    }
}

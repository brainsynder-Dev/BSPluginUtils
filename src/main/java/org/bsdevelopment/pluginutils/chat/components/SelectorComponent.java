package org.bsdevelopment.pluginutils.chat.components;

import org.bsdevelopment.pluginutils.chat.Component;
import org.bsdevelopment.pluginutils.chat.Style;

import java.util.Objects;

/**
 * A component that resolves to a Minecraft selector (e.g. "@p", "@e[type=zombie]").
 *
 * @param selector
 *         the selector string
 * @param style
 *         styling information
 */
public record SelectorComponent(String selector, Style style) implements Component {
    public SelectorComponent {
        Objects.requireNonNull(selector, "selector");
        Objects.requireNonNull(style, "style");
    }

    @Override
    public java.util.List<Component> children() {
        return java.util.List.of();
    }
}

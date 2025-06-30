package org.bsdevelopment.pluginutils.chat.components;

import org.bsdevelopment.pluginutils.chat.Component;
import org.bsdevelopment.pluginutils.chat.Style;

import java.util.Objects;

/**
 * A wrapper that applies a new style to an existing component.
 *
 * @param inner
 *         the original component
 * @param style
 *         the style to apply
 */
public record StyledComponent(Component inner, Style style) implements Component {
    public StyledComponent {
        Objects.requireNonNull(inner, "inner");
        Objects.requireNonNull(style, "style");
    }

    @Override
    public java.util.List<Component> children() {
        return inner.children();
    }
}

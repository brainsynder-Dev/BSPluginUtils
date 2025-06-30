package org.bsdevelopment.pluginutils.chat.components;

import org.bsdevelopment.pluginutils.chat.Component;
import org.bsdevelopment.pluginutils.chat.Style;

import java.util.Objects;

/**
 * A component that shows the client’s keybind name.
 *
 * @param keybind
 *         the keybind identifier (e.g. "key.jump")
 * @param style
 *         styling information
 */
public record KeybindComponent(String keybind, Style style) implements Component {
    public KeybindComponent {
        Objects.requireNonNull(keybind, "keybind");
        Objects.requireNonNull(style, "style");
    }

    @Override
    public java.util.List<Component> children() {
        return java.util.List.of();
    }
}

package org.bsdevelopment.pluginutils.chat.components;

import org.bsdevelopment.pluginutils.chat.Component;
import org.bsdevelopment.pluginutils.chat.Style;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A plain text component.
 *
 * @param content
 *         the raw text
 * @param style
 *         styling information
 * @param children
 *         child components (usually used for extra formatting)
 */
public record TextComponent(String content, Style style, List<Component> children) implements Component {
    public TextComponent {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(style, "style");
        Objects.requireNonNull(children, "children");
    }

    @Override
    public List<Component> children() {
        return Collections.unmodifiableList(children);
    }
}

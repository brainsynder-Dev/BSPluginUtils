package org.bsdevelopment.pluginutils.chat.components;

import org.bsdevelopment.pluginutils.chat.Component;
import org.bsdevelopment.pluginutils.chat.Style;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A composite component that holds a list of children and a single style.
 *
 * @param children
 *         list of components
 * @param style
 *         styling information
 */
public record CompositeComponent(List<Component> children, Style style) implements Component {
    public CompositeComponent {
        Objects.requireNonNull(children, "children");
        Objects.requireNonNull(style, "style");
    }

    @Override
    public List<Component> children() {
        return Collections.unmodifiableList(children);
    }
}

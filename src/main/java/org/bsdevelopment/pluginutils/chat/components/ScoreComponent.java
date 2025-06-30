package org.bsdevelopment.pluginutils.chat.components;

import org.bsdevelopment.pluginutils.chat.Component;
import org.bsdevelopment.pluginutils.chat.Style;

import java.util.Objects;

/**
 * A component that displays a scoreboard score.
 *
 * @param name
 *         the player or entity name
 * @param objective
 *         the scoreboard objective
 * @param style
 *         styling information
 */
public record ScoreComponent(String name, String objective, Style style) implements Component {
    public ScoreComponent {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(objective, "objective");
        Objects.requireNonNull(style, "style");
    }

    @Override
    public java.util.List<Component> children() {
        return java.util.List.of();
    }
}

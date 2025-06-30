package org.bsdevelopment.pluginutils.chat;

import org.bsdevelopment.pluginutils.chat.decoration.NamedTextColor;
import org.bsdevelopment.pluginutils.chat.decoration.TextDecoration;
import org.bsdevelopment.pluginutils.chat.events.ClickEvent;
import org.bsdevelopment.pluginutils.chat.events.HoverEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Styling information for components: color, decorations, click/hover events, insertion text.
 *
 * @param color
 *         the named color (or null for default)
 * @param decorations
 *         a set of text decorations (bold, italic, etc.)
 * @param clickEvent
 *         optional click action
 * @param hoverEvent
 *         optional hover action
 * @param insertion
 *         optional insertion text (for shift‐click)
 */
public record Style(
        NamedTextColor color,
        Set<TextDecoration> decorations,
        ClickEvent clickEvent,
        HoverEvent hoverEvent,
        String insertion
) {
    /**
     * Returns an empty style (no color, no decorations, no events, no insertion).
     */
    public static Style empty() {
        return new Style(null, Collections.emptySet(), null, null, null);
    }

    /**
     * A builder for {@link Style}.
     */
    public static class Builder {
        private NamedTextColor color;
        private final Set<TextDecoration> decos = new HashSet<>();
        private ClickEvent click;
        private HoverEvent hover;
        private String insertion;

        public Builder color(NamedTextColor color) {
            this.color = color;
            return this;
        }

        public Builder decorate(TextDecoration deco) {
            this.decos.add(deco);
            return this;
        }

        public Builder clickEvent(ClickEvent click) {
            this.click = click;
            return this;
        }

        public Builder hoverEvent(HoverEvent hover) {
            this.hover = hover;
            return this;
        }

        public Builder insertion(String ins) {
            this.insertion = ins;
            return this;
        }

        public Style build() {
            return new Style(color,
                    Collections.unmodifiableSet(decos), click, hover, insertion);
        }
    }
}

package org.bsdevelopment.pluginutils.chat.events;

import org.bsdevelopment.pluginutils.chat.Component;

/**
 * A hover event, e.g. show text, show item, show entity.
 *
 * @param action
 *         the hover action
 * @param value
 *         the component or data to display
 */
public record HoverEvent(HoverAction action, Component value) {
    public enum HoverAction {
        SHOW_TEXT, SHOW_ITEM, SHOW_ENTITY
    }
}

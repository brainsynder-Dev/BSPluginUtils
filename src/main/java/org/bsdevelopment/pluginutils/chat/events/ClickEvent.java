package org.bsdevelopment.pluginutils.chat.events;

/**
 * A click event, e.g. run command, open URL, suggest command, copy to clipboard.
 *
 * @param action
 *         the click action
 * @param value
 *         the associated value
 */
public record ClickEvent(ClickAction action, String value) {
    public enum ClickAction {
        RUN_COMMAND, OPEN_URL, SUGGEST_COMMAND, COPY_TO_CLIPBOARD
    }
}

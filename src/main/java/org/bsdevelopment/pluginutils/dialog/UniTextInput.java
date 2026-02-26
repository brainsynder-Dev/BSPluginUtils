package org.bsdevelopment.pluginutils.dialog;

import io.github.projectunified.unidialog.core.input.TextInput;

/**
 * A platform-neutral wrapper around {@link TextInput} for configuring a text-entry
 * input field inside a dialog.
 *
 * <p>Obtain an instance via {@link UniInputBuilder#textInput()}.
 *
 * <p><b>Example:</b>
 * <pre>
 * UniDialog.notice()
 *     .input("playerName", i -> i.textInput()
 *         .label("Your name")
 *         .initial("Steve")
 *         .maxLength(16))
 *     .action(a -> a.label("Submit").dynamicRunCommand("/rename %playerName%"))
 *     .open(player);
 * </pre>
 */
public final class UniTextInput {
    private final TextInput raw;

    UniTextInput(TextInput raw) {
        this.raw = raw;
    }

    /**
     * Sets the label displayed above this text field.
     *
     * @param label the label text
     *
     * @return this builder
     */
    public UniTextInput label(String label) {
        raw.label(label);
        return this;
    }

    /**
     * Sets the pre-filled value of this text field.
     *
     * @param initial the initial text value
     *
     * @return this builder
     */
    public UniTextInput initial(String initial) {
        raw.initial(initial);
        return this;
    }

    /**
     * Sets the pixel width of this text field.
     *
     * @param width the width in pixels
     *
     * @return this builder
     */
    public UniTextInput width(int width) {
        raw.width(width);
        return this;
    }

    /**
     * Sets the maximum number of characters allowed in this text field.
     *
     * @param maxLength the maximum character count
     *
     * @return this builder
     */
    public UniTextInput maxLength(int maxLength) {
        raw.maxLength(maxLength);
        return this;
    }

    /**
     * Sets the maximum number of lines allowed in this text field.
     * Pass {@code null} to use the default.
     *
     * @param maxLines the maximum line count, or {@code null} for default
     *
     * @return this builder
     */
    public UniTextInput maxLines(Integer maxLines) {
        raw.maxLines(maxLines);
        return this;
    }

    /**
     * Sets the pixel height of this text field.
     * Pass {@code null} to use the default.
     *
     * @param height the height in pixels, or {@code null} for default
     *
     * @return this builder
     */
    public UniTextInput height(Integer height) {
        raw.height(height);
        return this;
    }
}

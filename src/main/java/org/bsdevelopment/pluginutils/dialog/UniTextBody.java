package org.bsdevelopment.pluginutils.dialog;

import io.github.projectunified.unidialog.core.body.TextBody;

/**
 * A platform-neutral wrapper around {@link TextBody} for configuring plain-text
 * body content inside a dialog.
 *
 * <p>Obtain an instance via {@link UniBodyBuilder#text()}.
 *
 * <p><b>Example:</b>
 * <pre>
 * UniDialog.notice()
 *     .body(b -> b.text().text("Welcome to the server!").width(400))
 *     .open(player);
 * </pre>
 */
public final class UniTextBody {
    private final TextBody raw;

    UniTextBody(TextBody raw) {
        this.raw = raw;
    }

    /**
     * Sets the text content of this body section.
     *
     * @param text the text to display (supports legacy formatting codes on Spigot,
     *             MiniMessage on Paper if configured)
     *
     * @return this builder
     */
    public UniTextBody text(String text) {
        raw.text(text);
        return this;
    }

    /**
     * Sets the pixel width of this text body section.
     *
     * @param width the width in pixels
     *
     * @return this builder
     */
    public UniTextBody width(int width) {
        raw.width(width);
        return this;
    }
}

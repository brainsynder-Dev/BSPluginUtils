package org.bsdevelopment.pluginutils.dialog;

import io.github.projectunified.unidialog.core.action.DialogActionBuilder;
import io.github.projectunified.unidialog.core.opener.DialogOpener;

/**
 * A platform-neutral wrapper around {@link DialogActionBuilder} that works on both
 * Spigot and Paper servers without requiring platform-specific imports.
 *
 * <p>Instances are created internally by the dialog builders and passed to your
 * {@code Consumer<UniActionBuilder>} lambdas.
 *
 * <p><b>Example:</b>
 * <pre>
 * UniDialog.notice()
 *     .title("Info")
 *     .body(b -> b.text().text("Hello!"))
 *     .action(a -> a.label("OK").runCommand("/spawn"))
 *     .open(player);
 * </pre>
 */
public final class UniActionBuilder {
    private final DialogActionBuilder raw;

    UniActionBuilder(DialogActionBuilder raw) {
        this.raw = raw;
    }

    /**
     * Sets the display label of this action button.
     *
     * @param label the button label text
     *
     * @return this builder
     */
    public UniActionBuilder label(String label) {
        raw.label(label);
        return this;
    }

    /**
     * Sets the tooltip text shown when hovering over this action button.
     *
     * @param tooltip the tooltip text
     *
     * @return this builder
     */
    public UniActionBuilder tooltip(String tooltip) {
        raw.tooltip(tooltip);
        return this;
    }

    /**
     * Sets the pixel width of this action button.
     *
     * @param width the button width in pixels
     *
     * @return this builder
     */
    public UniActionBuilder width(int width) {
        raw.width(width);
        return this;
    }

    /**
     * Clicking this button copies the given text to the player's clipboard.
     *
     * @param text the text to copy
     *
     * @return this builder
     */
    public UniActionBuilder copyToClipboard(String text) {
        raw.copyToClipboard(text);
        return this;
    }

    /**
     * Associates a dynamic custom action by id (uses the default namespace).
     *
     * @param id the action id
     *
     * @return this builder
     */
    public UniActionBuilder dynamicCustom(String id) {
        raw.dynamicCustom(id);
        return this;
    }

    /**
     * Associates a dynamic custom action by namespace and id.
     *
     * @param namespace the action namespace
     * @param id        the action id
     *
     * @return this builder
     */
    public UniActionBuilder dynamicCustom(String namespace, String id) {
        raw.dynamicCustom(namespace, id);
        return this;
    }

    /**
     * Runs a command dynamically based on input values when the button is clicked.
     *
     * @param command the command template
     *
     * @return this builder
     */
    public UniActionBuilder dynamicRunCommand(String command) {
        raw.dynamicRunCommand(command);
        return this;
    }

    /**
     * Opens the given URL in the player's browser when the button is clicked.
     *
     * @param url the URL to open
     *
     * @return this builder
     */
    public UniActionBuilder openUrl(String url) {
        raw.openUrl(url);
        return this;
    }

    /**
     * Runs the given command as the player when the button is clicked.
     *
     * @param command the command to run (include leading slash)
     *
     * @return this builder
     */
    public UniActionBuilder runCommand(String command) {
        raw.runCommand(command);
        return this;
    }

    /**
     * Fills the player's chat bar with the given command without executing it.
     *
     * @param command the command to suggest
     *
     * @return this builder
     */
    public UniActionBuilder suggestCommand(String command) {
        raw.suggestCommand(command);
        return this;
    }

    /**
     * Opens another dialog identified by an existing {@link DialogOpener} when clicked.
     *
     * @param opener the opener for the target dialog
     *
     * @return this builder
     */
    public UniActionBuilder showDialog(DialogOpener opener) {
        raw.showDialog(opener);
        return this;
    }

    /**
     * Opens another dialog identified by its namespace and key when clicked.
     *
     * @param namespace the dialog namespace
     * @param id        the dialog key
     *
     * @return this builder
     */
    public UniActionBuilder showDialog(String namespace, String id) {
        raw.showDialog(namespace, id);
        return this;
    }
}

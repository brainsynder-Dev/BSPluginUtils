package org.bsdevelopment.pluginutils.dialog;

import io.github.projectunified.unidialog.core.action.DialogActionBuilder;
import io.github.projectunified.unidialog.core.dialog.Dialog;
import io.github.projectunified.unidialog.core.dialog.ServerLinksDialog;
import org.bsdevelopment.pluginutils.PluginUtilities;

import java.util.function.Consumer;

/**
 * Builds a server-links dialog — a dialog that displays the server's configured
 * links (set via {@code server-links} in {@code server.properties}).
 *
 * <p>Obtain an instance via {@link UniDialog#serverLinks()}.
 *
 * <p><b>Example:</b>
 * <pre>
 * UniDialog.serverLinks()
 *     .title("Server Links")
 *     .columns(2)
 *     .exitAction(a -> a.label("Close"))
 *     .open(player);
 * </pre>
 */
public final class ServerLinksDialogBuilder extends BaseDialogBuilder<ServerLinksDialogBuilder> {
    private Consumer<UniActionBuilder> exitAction;
    private int columns = 2;
    private int buttonWidth = 150;

    /**
     * Sets the dedicated exit/close button.
     *
     * @param action a consumer that configures the button via {@link UniActionBuilder}
     *
     * @return this builder
     */
    public ServerLinksDialogBuilder exitAction(Consumer<UniActionBuilder> action) {
        this.exitAction = action;
        return this;
    }

    /**
     * Sets the number of link button columns.
     *
     * @param columns the number of columns
     *
     * @return this builder
     */
    public ServerLinksDialogBuilder columns(int columns) {
        this.columns = columns;
        return this;
    }

    /**
     * Sets the pixel width of each link button.
     *
     * @param buttonWidth the button width in pixels
     *
     * @return this builder
     */
    public ServerLinksDialogBuilder buttonWidth(int buttonWidth) {
        this.buttonWidth = buttonWidth;
        return this;
    }

    @Override
    protected Dialog buildDialog() {
        ServerLinksDialog dialog = PluginUtilities.getDialogManager().createServerLinksDialog();
        applyBase(dialog);
        dialog.columns(columns);
        dialog.buttonWidth(buttonWidth);
        if (exitAction != null) dialog.exitAction(action -> exitAction.accept(new UniActionBuilder((DialogActionBuilder) action)));
        return dialog;
    }
}

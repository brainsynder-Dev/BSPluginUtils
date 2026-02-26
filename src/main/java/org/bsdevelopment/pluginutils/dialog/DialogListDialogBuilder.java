package org.bsdevelopment.pluginutils.dialog;

import io.github.projectunified.unidialog.core.action.DialogActionBuilder;
import io.github.projectunified.unidialog.core.dialog.Dialog;
import io.github.projectunified.unidialog.core.dialog.DialogListDialog;
import io.github.projectunified.unidialog.core.opener.DialogOpener;
import org.bsdevelopment.pluginutils.PluginUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Builds a dialog-list dialog — a dialog that presents a grid of buttons, each
 * opening a child dialog when clicked.
 *
 * <p>Obtain an instance via {@link UniDialog#dialogList()}.
 *
 * <p>Child dialogs are added via their {@link DialogOpener}, obtained by calling
 * {@link BaseDialogBuilder#opener()} on any other builder without opening it.
 *
 * <p><b>Example:</b>
 * <pre>
 * DialogOpener helpDialog = UniDialog.notice()
 *     .title("Help")
 *     .body(b -> b.text().text("Type /help for commands."))
 *     .opener();
 *
 * DialogOpener rulesDialog = UniDialog.notice()
 *     .title("Rules")
 *     .body(b -> b.text().text("Be respectful."))
 *     .opener();
 *
 * UniDialog.dialogList()
 *     .title("Menu")
 *     .dialog(helpDialog)
 *     .dialog(rulesDialog)
 *     .columns(2)
 *     .exitAction(a -> a.label("Close"))
 *     .open(player);
 * </pre>
 */
public final class DialogListDialogBuilder extends BaseDialogBuilder<DialogListDialogBuilder> {
    private final List<DialogOpener> openers = new ArrayList<>();
    private Consumer<UniActionBuilder> exitAction;
    private int columns = DialogListDialog.DEFAULT_COLUMNS;
    private int buttonWidth = DialogListDialog.DEFAULT_BUTTON_WIDTH;

    /**
     * Adds a child dialog entry using an existing {@link DialogOpener}.
     * Use {@link BaseDialogBuilder#opener()} on any builder to obtain one.
     *
     * @param opener the opener for the child dialog
     *
     * @return this builder
     */
    public DialogListDialogBuilder dialog(DialogOpener opener) {
        this.openers.add(opener);
        return this;
    }

    /**
     * Adds a child dialog entry by namespace and key (for pre-registered dialogs).
     *
     * @param namespace the dialog namespace
     * @param id        the dialog key
     *
     * @return this builder
     */
    public DialogListDialogBuilder dialog(String namespace, String id) {
        this.openers.add(new NamespaceKeyOpener(namespace, id));
        return this;
    }

    /**
     * Sets the dedicated exit/close button.
     *
     * @param action a consumer that configures the button via {@link UniActionBuilder}
     *
     * @return this builder
     */
    public DialogListDialogBuilder exitAction(Consumer<UniActionBuilder> action) {
        this.exitAction = action;
        return this;
    }

    /**
     * Sets the number of button columns in the dialog grid.
     * Defaults to {@link DialogListDialog#DEFAULT_COLUMNS}.
     *
     * @param columns the number of columns
     *
     * @return this builder
     */
    public DialogListDialogBuilder columns(int columns) {
        this.columns = columns;
        return this;
    }

    /**
     * Sets the pixel width of each dialog button.
     * Defaults to {@link DialogListDialog#DEFAULT_BUTTON_WIDTH}.
     *
     * @param buttonWidth the button width in pixels
     *
     * @return this builder
     */
    public DialogListDialogBuilder buttonWidth(int buttonWidth) {
        this.buttonWidth = buttonWidth;
        return this;
    }

    @Override
    protected Dialog buildDialog() {
        DialogListDialog dialog = PluginUtilities.getDialogManager().createDialogListDialog();
        applyBase(dialog);
        dialog.columns(columns);
        dialog.buttonWidth(buttonWidth);

        for (DialogOpener opener : openers) {
            if (opener instanceof NamespaceKeyOpener(String namespace, String id)) {
                dialog.dialog(namespace, id);
            } else {
                dialog.dialog(opener);
            }
        }

        if (exitAction != null) dialog.exitAction(action -> exitAction.accept(new UniActionBuilder((DialogActionBuilder) action)));
        return dialog;
    }

    private record NamespaceKeyOpener(String namespace, String id) implements DialogOpener {
        @Override
        public boolean open(java.util.UUID uuid) {
            throw new UnsupportedOperationException("NamespaceKeyOpener is a sentinel and cannot be used to open dialogs directly.");
        }
    }
}

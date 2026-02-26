package org.bsdevelopment.pluginutils.dialog;

import io.github.projectunified.unidialog.core.action.DialogActionBuilder;
import io.github.projectunified.unidialog.core.dialog.ConfirmationDialog;
import io.github.projectunified.unidialog.core.dialog.Dialog;
import org.bsdevelopment.pluginutils.PluginUtilities;

import java.util.function.Consumer;

/**
 * Builds a confirmation dialog — a dialog with a "Yes" and a "No" action button.
 *
 * <p>Obtain an instance via {@link UniDialog#confirm()}.
 *
 * <p><b>Example:</b>
 * <pre>
 * UniDialog.confirm()
 *     .title("Delete Home")
 *     .body(b -> b.text().text("Are you sure you want to delete this home?"))
 *     .yes(a -> a.label("Delete").runCommand("/delhome confirm"))
 *     .no(a -> a.label("Cancel"))
 *     .open(player);
 * </pre>
 */
public class ConfirmDialogBuilder extends BaseDialogBuilder<ConfirmDialogBuilder> {
    private Consumer<UniActionBuilder> yesAction;
    private Consumer<UniActionBuilder> noAction;

    /**
     * Configures the "Yes" / confirm action button.
     *
     * @param action a consumer that configures the button via {@link UniActionBuilder}
     *
     * @return this builder
     */
    public ConfirmDialogBuilder yes(Consumer<UniActionBuilder> action) {
        this.yesAction = action;
        return this;
    }

    /**
     * Configures the "No" / cancel action button.
     *
     * @param action a consumer that configures the button via {@link UniActionBuilder}
     *
     * @return this builder
     */
    public ConfirmDialogBuilder no(Consumer<UniActionBuilder> action) {
        this.noAction = action;
        return this;
    }

    @Override
    protected Dialog buildDialog() {
        ConfirmationDialog dialog = PluginUtilities.getDialogManager().createConfirmationDialog();
        applyBase(dialog);
        if (yesAction != null) dialog.yesAction(action -> yesAction.accept(new UniActionBuilder((DialogActionBuilder) action)));
        if (noAction != null) dialog.noAction(action -> noAction.accept(new UniActionBuilder((DialogActionBuilder) action)));
        return dialog;
    }
}

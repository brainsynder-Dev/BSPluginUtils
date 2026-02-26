package org.bsdevelopment.pluginutils.dialog;

import io.github.projectunified.unidialog.core.action.DialogActionBuilder;
import io.github.projectunified.unidialog.core.dialog.Dialog;
import io.github.projectunified.unidialog.core.dialog.MultiActionDialog;
import org.bsdevelopment.pluginutils.PluginUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Builds a multi-action dialog — a dialog with multiple action buttons arranged in
 * a configurable grid layout.
 *
 * <p>Obtain an instance via {@link UniDialog#multiAction()}.
 *
 * <p><b>Example:</b>
 * <pre>
 * UniDialog.multiAction()
 *     .title("Choose Difficulty")
 *     .body(b -> b.text().text("Select the game difficulty:"))
 *     .columns(3)
 *     .action(a -> a.label("Easy").runCommand("/difficulty easy"))
 *     .action(a -> a.label("Normal").runCommand("/difficulty normal"))
 *     .action(a -> a.label("Hard").runCommand("/difficulty hard"))
 *     .exitAction(a -> a.label("Cancel"))
 *     .open(player);
 * </pre>
 */
public final class MultiActionDialogBuilder extends BaseDialogBuilder<MultiActionDialogBuilder> {
    private final List<Consumer<UniActionBuilder>> actions = new ArrayList<>();
    private Consumer<UniActionBuilder> exitAction;
    private int columns = MultiActionDialog.DEFAULT_COLUMNS;

    /**
     * Adds an action button to this dialog. Call multiple times to add more buttons.
     *
     * @param action a consumer that configures the button via {@link UniActionBuilder}
     *
     * @return this builder
     */
    public MultiActionDialogBuilder action(Consumer<UniActionBuilder> action) {
        this.actions.add(action);
        return this;
    }

    /**
     * Sets the dedicated exit/cancel button shown separately from the main actions.
     *
     * @param action a consumer that configures the button via {@link UniActionBuilder}
     *
     * @return this builder
     */
    public MultiActionDialogBuilder exitAction(Consumer<UniActionBuilder> action) {
        this.exitAction = action;
        return this;
    }

    /**
     * Sets the number of button columns in the action grid.
     * Defaults to {@link MultiActionDialog#DEFAULT_COLUMNS}.
     *
     * @param columns the number of columns
     *
     * @return this builder
     */
    public MultiActionDialogBuilder columns(int columns) {
        this.columns = columns;
        return this;
    }

    @Override
    protected Dialog buildDialog() {
        MultiActionDialog dialog = PluginUtilities.getDialogManager().createMultiActionDialog();
        applyBase(dialog);
        dialog.columns(columns);
        actions.forEach(consumer -> dialog.action(action -> consumer.accept(new UniActionBuilder((DialogActionBuilder) action))));
        if (exitAction != null) dialog.exitAction(rawAb -> exitAction.accept(new UniActionBuilder((DialogActionBuilder) rawAb)));
        return dialog;
    }
}

package org.bsdevelopment.pluginutils.dialog;

import io.github.projectunified.unidialog.core.action.DialogActionBuilder;
import io.github.projectunified.unidialog.core.dialog.Dialog;
import io.github.projectunified.unidialog.core.dialog.NoticeDialog;
import org.bsdevelopment.pluginutils.PluginUtilities;

import java.util.function.Consumer;

/**
 * Builds a notice dialog — a simple informational dialog with a single action button.
 *
 * <p>Obtain an instance via {@link UniDialog#notice()}.
 *
 * <p><b>Example:</b>
 * <pre>
 * UniDialog.notice()
 *     .title("Server Notice")
 *     .body(b -> b.text().text("The server will restart in 5 minutes."))
 *     .action(a -> a.label("Got it").runCommand("/dismiss"))
 *     .open(player);
 * </pre>
 */
public final class NoticeDialogBuilder extends BaseDialogBuilder<NoticeDialogBuilder> {
    private Consumer<UniActionBuilder> action;

    /**
     * Sets the action button shown in this notice dialog.
     * If not called, a default "OK" button with no action is used.
     *
     * @param action a consumer that configures the button via {@link UniActionBuilder}
     *
     * @return this builder
     */
    public NoticeDialogBuilder action(Consumer<UniActionBuilder> action) {
        this.action = action;
        return this;
    }

    @Override
    protected Dialog buildDialog() {
        NoticeDialog dialog = PluginUtilities.getDialogManager().createNoticeDialog();
        applyBase(dialog);
        if (action != null) dialog.action(action -> this.action.accept(new UniActionBuilder((DialogActionBuilder) action)));
        return dialog;
    }
}

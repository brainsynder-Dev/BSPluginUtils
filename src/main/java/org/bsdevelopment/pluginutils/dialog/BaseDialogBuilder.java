package org.bsdevelopment.pluginutils.dialog;

import io.github.projectunified.unidialog.core.body.DialogBodyBuilder;
import io.github.projectunified.unidialog.core.dialog.Dialog;
import io.github.projectunified.unidialog.core.input.DialogInputBuilder;
import io.github.projectunified.unidialog.core.opener.DialogOpener;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Abstract base for all dialog builders. Handles the common {@link Dialog} properties
 * (title, pause, escape, after-action, body, input) and delegates platform-specific
 * construction to each subclass via {@link #buildDialog()}.
 *
 * @param <SELF> the concrete builder type, for fluent chaining
 */
abstract class BaseDialogBuilder<SELF extends BaseDialogBuilder<SELF>> {
    private final List<Consumer<UniBodyBuilder>> bodies = new ArrayList<>();
    private final Map<String, Consumer<UniInputBuilder>> inputs = new LinkedHashMap<>();
    private String title = "";
    private boolean canCloseWithEscape = true;
    private Dialog.AfterAction afterAction = Dialog.AfterAction.CLOSE;

    private SELF self() {
        return (SELF) this;
    }

    /**
     * Sets the title of the dialog window.
     *
     * @param title the title text
     *
     * @return this builder
     */
    public SELF title(String title) {
        this.title = title;
        return self();
    }

    /**
     * Controls whether the player can close the dialog by pressing Escape.
     * Defaults to {@code true}.
     *
     * @param value {@code false} to prevent Escape-close
     *
     * @return this builder
     */
    public SELF canCloseWithEscape(boolean value) {
        this.canCloseWithEscape = value;
        return self();
    }

    /**
     * Sets what happens after a player completes an action in the dialog.
     * Defaults to {@link Dialog.AfterAction#CLOSE}.
     *
     * @param action the after-action behavior
     *
     * @return this builder
     */
    public SELF afterAction(Dialog.AfterAction action) {
        this.afterAction = action;
        return self();
    }

    /**
     * Adds a body section to the dialog. Multiple calls append additional sections.
     *
     * @param body a consumer that configures the body via {@link UniBodyBuilder}
     *
     * @return this builder
     */
    public SELF body(Consumer<UniBodyBuilder> body) {
        this.bodies.add(body);
        return self();
    }

    /**
     * Adds an input field to the dialog. The {@code key} is used as the placeholder
     * name in {@code dynamicRunCommand} patterns (e.g. {@code /cmd %key%}).
     *
     * @param key   the input key
     * @param input a consumer that configures the input via {@link UniInputBuilder}
     *
     * @return this builder
     */
    public SELF input(String key, Consumer<UniInputBuilder> input) {
        this.inputs.put(key, input);
        return self();
    }

    /**
     * Applies all common properties to the given raw dialog instance.
     */
    protected void applyBase(Dialog dialog) {
        dialog.title(title);
        dialog.canCloseWithEscape(canCloseWithEscape);
        dialog.afterAction(afterAction);

        bodies.forEach(bodyConsumer -> dialog.body(body -> bodyConsumer.accept(new UniBodyBuilder((DialogBodyBuilder) body))));
        inputs.forEach((key, inputConsumer) -> dialog.input(key, input -> inputConsumer.accept(new UniInputBuilder((DialogInputBuilder) input))));
    }

    /**
     * Builds the platform-specific dialog with all configured properties applied.
     * Called by {@link #opener()} and {@link #open(Player)}.
     *
     * @return the fully-configured raw dialog
     */
    protected abstract Dialog buildDialog();

    /**
     * Builds the dialog and returns its {@link DialogOpener}, which can be stored
     * and used to open the same dialog again, or referenced in dialog-list/action builders.
     *
     * @return the dialog opener
     */
    public DialogOpener opener() {
        return buildDialog().opener();
    }

    /**
     * Builds and immediately opens the dialog for the given player.
     *
     * @param player the player to show the dialog to
     *
     * @return {@code true} if the dialog was opened successfully
     */
    public boolean open(Player player) {
        return opener().open(player.getUniqueId());
    }
}

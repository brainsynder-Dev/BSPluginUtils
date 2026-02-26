package org.bsdevelopment.pluginutils.dialog;

import io.github.projectunified.unidialog.core.input.SingleOptionInput;

/**
 * A platform-neutral wrapper around {@link SingleOptionInput} for configuring a
 * dropdown/select input inside a dialog.
 *
 * <p>Obtain an instance via {@link UniInputBuilder#singleOptionInput()}.
 *
 * <p><b>Example:</b>
 * <pre>
 * UniDialog.notice()
 *     .input("gamemode", i -> i.singleOptionInput()
 *         .label("Game Mode")
 *         .option("survival", "Survival", true)
 *         .option("creative", "Creative")
 *         .option("adventure", "Adventure"))
 *     .action(a -> a.label("Apply").dynamicRunCommand("/gamemode %gamemode%"))
 *     .open(player);
 * </pre>
 */
public final class UniSingleOptionInput {
    private final SingleOptionInput raw;

    UniSingleOptionInput(SingleOptionInput raw) {
        this.raw = raw;
    }

    /**
     * Sets the label displayed above this dropdown.
     *
     * @param label the label text
     *
     * @return this builder
     */
    public UniSingleOptionInput label(String label) {
        raw.label(label);
        return this;
    }

    /**
     * Sets the pixel width of this dropdown.
     *
     * @param width the width in pixels
     *
     * @return this builder
     */
    public UniSingleOptionInput width(int width) {
        raw.width(width);
        return this;
    }

    /**
     * Adds an option to this dropdown.
     *
     * @param id      the internal key used in command substitution
     * @param display the text shown to the player
     * @param initial {@code true} if this option should be selected by default
     *
     * @return this builder
     */
    public UniSingleOptionInput option(String id, String display, boolean initial) {
        raw.option(id, display, initial);
        return this;
    }

    /**
     * Adds a non-default option to this dropdown.
     *
     * @param id      the internal key used in command substitution
     * @param display the text shown to the player
     *
     * @return this builder
     */
    public UniSingleOptionInput option(String id, String display) {
        raw.option(id, display);
        return this;
    }
}

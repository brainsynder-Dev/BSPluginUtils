package org.bsdevelopment.pluginutils.dialog;

import io.github.projectunified.unidialog.core.input.BooleanInput;

/**
 * A platform-neutral wrapper around {@link BooleanInput} for configuring a
 * toggle/checkbox input inside a dialog.
 *
 * <p>Obtain an instance via {@link UniInputBuilder#booleanInput()}.
 *
 * <p><b>Example:</b>
 * <pre>
 * UniDialog.notice()
 *     .input("confirm", i -> i.booleanInput()
 *         .label("I agree to the terms")
 *         .initial(false)
 *         .onTrue("Agreed")
 *         .onFalse("Disagreed"))
 *     .action(a -> a.label("Submit").dynamicRunCommand("/accept %confirm%"))
 *     .open(player);
 * </pre>
 */
public final class UniBooleanInput {
    private final BooleanInput raw;

    UniBooleanInput(BooleanInput raw) {
        this.raw = raw;
    }

    /**
     * Sets the label displayed next to this toggle.
     *
     * @param label the label text
     *
     * @return this builder
     */
    public UniBooleanInput label(String label) {
        raw.label(label);
        return this;
    }

    /**
     * Sets the initial checked state of this toggle.
     *
     * @param initial {@code true} for checked, {@code false} for unchecked
     *
     * @return this builder
     */
    public UniBooleanInput initial(boolean initial) {
        raw.initial(initial);
        return this;
    }

    /**
     * Sets the text displayed when the toggle is in the {@code true} state.
     *
     * @param text the "on" state label
     *
     * @return this builder
     */
    public UniBooleanInput onTrue(String text) {
        raw.onTrue(text);
        return this;
    }

    /**
     * Sets the text displayed when the toggle is in the {@code false} state.
     *
     * @param text the "off" state label
     *
     * @return this builder
     */
    public UniBooleanInput onFalse(String text) {
        raw.onFalse(text);
        return this;
    }
}

package org.bsdevelopment.pluginutils.dialog;

import io.github.projectunified.unidialog.core.input.NumberRangeInput;

/**
 * A platform-neutral wrapper around {@link NumberRangeInput} for configuring a
 * slider/range input inside a dialog.
 *
 * <p>Obtain an instance via {@link UniInputBuilder#numberRangeInput()}.
 *
 * <p><b>Example:</b>
 * <pre>
 * UniDialog.notice()
 *     .input("volume", i -> i.numberRangeInput()
 *         .label("Volume")
 *         .start(0f)
 *         .end(100f)
 *         .initial(50f)
 *         .step(1f))
 *     .action(a -> a.label("Apply").dynamicRunCommand("/volume %volume%"))
 *     .open(player);
 * </pre>
 */
public final class UniNumberRangeInput {
    private final NumberRangeInput raw;

    UniNumberRangeInput(NumberRangeInput raw) {
        this.raw = raw;
    }

    /**
     * Sets the label displayed above this slider.
     *
     * @param label the label text
     *
     * @return this builder
     */
    public UniNumberRangeInput label(String label) {
        raw.label(label);
        return this;
    }

    /**
     * Sets the pixel width of this slider.
     *
     * @param width the width in pixels
     *
     * @return this builder
     */
    public UniNumberRangeInput width(int width) {
        raw.width(width);
        return this;
    }

    /**
     * Sets the format string used to display the current value.
     * Typically a printf-style format such as {@code "%.0f%%"}.
     *
     * @param format the label format string
     *
     * @return this builder
     */
    public UniNumberRangeInput labelFormat(String format) {
        raw.labelFormat(format);
        return this;
    }

    /**
     * Sets the minimum value of this slider.
     *
     * @param start the minimum value
     *
     * @return this builder
     */
    public UniNumberRangeInput start(float start) {
        raw.start(start);
        return this;
    }

    /**
     * Sets the maximum value of this slider.
     *
     * @param end the maximum value
     *
     * @return this builder
     */
    public UniNumberRangeInput end(float end) {
        raw.end(end);
        return this;
    }

    /**
     * Sets the initial position of the slider.
     * Pass {@code null} to use the default (typically the start value).
     *
     * @param initial the initial value, or {@code null} for default
     *
     * @return this builder
     */
    public UniNumberRangeInput initial(Float initial) {
        raw.initial(initial);
        return this;
    }

    /**
     * Sets the step size between slider positions.
     * Pass {@code null} for continuous movement.
     *
     * @param step the step size, or {@code null} for continuous
     *
     * @return this builder
     */
    public UniNumberRangeInput step(Float step) {
        raw.step(step);
        return this;
    }
}

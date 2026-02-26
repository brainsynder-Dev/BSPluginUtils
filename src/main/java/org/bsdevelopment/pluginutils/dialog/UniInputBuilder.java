package org.bsdevelopment.pluginutils.dialog;

import io.github.projectunified.unidialog.core.input.BooleanInput;
import io.github.projectunified.unidialog.core.input.DialogInputBuilder;
import io.github.projectunified.unidialog.core.input.NumberRangeInput;
import io.github.projectunified.unidialog.core.input.SingleOptionInput;
import io.github.projectunified.unidialog.core.input.TextInput;

/**
 * A platform-neutral factory for creating input fields inside a dialog.
 *
 * <p>Instances are passed to your {@code Consumer<UniInputBuilder>} lambdas
 * via {@link BaseDialogBuilder#input(String, java.util.function.Consumer)}.
 *
 * <p>The key you provide to {@code input(key, ...)} maps to the placeholder
 * used in {@code dynamicRunCommand}, e.g. {@code /cmd %key%}.
 *
 * <p><b>Example:</b>
 * <pre>
 * UniDialog.notice()
 *     .input("name", i -> i.textInput().label("Name").maxLength(16))
 *     .input("agree", i -> i.booleanInput().label("Agree").initial(false))
 *     .action(a -> a.label("Submit").dynamicRunCommand("/register %name% %agree%"))
 *     .open(player);
 * </pre>
 */
public final class UniInputBuilder {
    private final DialogInputBuilder raw;

    UniInputBuilder(DialogInputBuilder raw) {
        this.raw = raw;
    }

    /**
     * Configures a single-line or multi-line text entry field.
     *
     * @return a {@link UniTextInput} for further configuration
     */
    public UniTextInput textInput() {
        return new UniTextInput((TextInput) raw.textInput());
    }

    /**
     * Configures a boolean toggle/checkbox field.
     *
     * @return a {@link UniBooleanInput} for further configuration
     */
    public UniBooleanInput booleanInput() {
        return new UniBooleanInput((BooleanInput) raw.booleanInput());
    }

    /**
     * Configures a numeric range slider field.
     *
     * @return a {@link UniNumberRangeInput} for further configuration
     */
    public UniNumberRangeInput numberRangeInput() {
        return new UniNumberRangeInput((NumberRangeInput) raw.numberRangeInput());
    }

    /**
     * Configures a single-select dropdown field.
     *
     * @return a {@link UniSingleOptionInput} for further configuration
     */
    public UniSingleOptionInput singleOptionInput() {
        return new UniSingleOptionInput((SingleOptionInput) raw.singleOptionInput());
    }
}

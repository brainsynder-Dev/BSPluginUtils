package org.bsdevelopment.pluginutils.dialog;

import io.github.projectunified.unidialog.core.body.DialogBodyBuilder;
import io.github.projectunified.unidialog.core.body.ItemBody;
import io.github.projectunified.unidialog.core.body.TextBody;

/**
 * A platform-neutral factory for creating body sections inside a dialog.
 *
 * <p>Instances are passed to your {@code Consumer<UniBodyBuilder>} lambdas
 * via {@link BaseDialogBuilder#body(java.util.function.Consumer)}.
 *
 * <p>Each call to {@link #text()} or {@link #item()} replaces the current body section
 * for that particular consumer invocation.
 *
 * <p><b>Example:</b>
 * <pre>
 * UniDialog.notice()
 *     .body(b -> b.text().text("Line one"))
 *     .body(b -> b.text().text("Line two"))
 *     .open(player);
 * </pre>
 */
public final class UniBodyBuilder {
    private final DialogBodyBuilder raw;

    UniBodyBuilder(DialogBodyBuilder raw) {
        this.raw = raw;
    }

    /**
     * Configures a plain-text body section.
     *
     * @return a {@link UniTextBody} for further configuration
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public UniTextBody text() {
        return new UniTextBody((TextBody) raw.text());
    }

    /**
     * Configures an item-based body section.
     *
     * @return a {@link UniItemBody} for further configuration
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public UniItemBody item() {
        return new UniItemBody((ItemBody) raw.item());
    }
}

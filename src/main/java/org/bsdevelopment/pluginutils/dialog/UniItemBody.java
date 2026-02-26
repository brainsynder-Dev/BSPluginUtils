package org.bsdevelopment.pluginutils.dialog;

import io.github.projectunified.unidialog.core.body.ItemBody;
import io.github.projectunified.unidialog.core.body.TextBody;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

/**
 * A platform-neutral wrapper around {@link ItemBody} for configuring an item-based
 * body section inside a dialog.
 *
 * <p>Obtain an instance via {@link UniBodyBuilder#item()}.
 *
 * <p><b>Example:</b>
 * <pre>
 * ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
 * UniDialog.notice()
 *     .body(b -> b.item()
 *         .item(sword)
 *         .description(d -> d.text("A powerful weapon"))
 *         .showTooltip(true))
 *     .open(player);
 * </pre>
 */
public final class UniItemBody {
    private final ItemBody raw;

    UniItemBody(ItemBody raw) {
        this.raw = raw;
    }

    /**
     * Sets the item to display in this body section.
     *
     * @param item the item stack to display
     *
     * @return this builder
     */
    public UniItemBody item(ItemStack item) {
        raw.item(item);
        return this;
    }

    /**
     * Configures the text description shown alongside the item.
     *
     * @param description a consumer that configures the description text body
     *
     * @return this builder
     */
    public UniItemBody description(Consumer<UniTextBody> description) {
        raw.description(rawText -> description.accept(new UniTextBody((TextBody) rawText)));
        return this;
    }

    /**
     * Controls whether item decorations (e.g. enchantment glint) are rendered.
     *
     * @param show {@code true} to show decorations
     *
     * @return this builder
     */
    public UniItemBody showDecorations(boolean show) {
        raw.showDecorations(show);
        return this;
    }

    /**
     * Controls whether the item tooltip is shown on hover.
     *
     * @param show {@code true} to show the tooltip
     *
     * @return this builder
     */
    public UniItemBody showTooltip(boolean show) {
        raw.showTooltip(show);
        return this;
    }

    /**
     * Sets the pixel width of this item body section.
     *
     * @param width the width in pixels
     *
     * @return this builder
     */
    public UniItemBody width(int width) {
        raw.width(width);
        return this;
    }

    /**
     * Sets the pixel height of this item body section.
     *
     * @param height the height in pixels
     *
     * @return this builder
     */
    public UniItemBody height(int height) {
        raw.height(height);
        return this;
    }
}

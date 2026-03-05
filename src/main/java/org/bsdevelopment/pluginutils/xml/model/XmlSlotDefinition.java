package org.bsdevelopment.pluginutils.xml.model;

import org.bsdevelopment.pluginutils.inventory.ItemBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure-data representation of one or more GUI slots that share the same item and actions.
 *
 * <p>A single {@code XmlSlotDefinition} may cover multiple slot indices when the XML uses
 * range or list notation (e.g. {@code index="0-8,18-26"}).
 *
 * <p>Either {@link #getItemRef()} or {@link #getBuilder()} will be non-null (or both null for
 * an invisible/no-item slot).
 */
public final class XmlSlotDefinition {

    /** Resolved slot indices, e.g. [0, 1, 2, 3, 4, 5, 6, 7, 8]. */
    private final List<Integer> slots;

    /**
     * Reference to a named definition id (from the {@code <definitions>} block),
     * or {@code null} when the item is defined inline.
     */
    private final String itemRef;

    /**
     * Inline {@link ItemBuilder}; {@code null} when {@link #itemRef} is set or when the slot
     * has no item.
     */
    private final ItemBuilder builder;

    /** Ordered list of actions triggered when this slot is clicked. */
    private final List<XmlActionDefinition> actions;

    /**
     * @param slots   resolved slot indices (must not be null or empty)
     * @param itemRef named definition reference (null for inline or no-item slots)
     * @param builder inline item builder (null when using itemRef or no-item slots)
     * @param actions list of click actions (null treated as empty)
     */
    public XmlSlotDefinition(List<Integer> slots, String itemRef, ItemBuilder builder,
                             List<XmlActionDefinition> actions) {
        this.slots = Collections.unmodifiableList(new ArrayList<>(slots));
        this.itemRef = itemRef;
        this.builder = builder;
        this.actions = actions == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(actions));
    }

    /** Returns the list of resolved slot indices covered by this definition. */
    public List<Integer> getSlots() {
        return slots;
    }

    /**
     * Returns the named definition id to use as the item, or {@code null} if using an inline
     * item builder or no item at all.
     */
    public String getItemRef() {
        return itemRef;
    }

    /**
     * Returns the inline {@link ItemBuilder}, or {@code null} if using a named reference or
     * no item.
     */
    public ItemBuilder getBuilder() {
        return builder;
    }

    /** Returns the ordered list of click actions. Never null; may be empty. */
    public List<XmlActionDefinition> getActions() {
        return actions;
    }

    /**
     * Returns {@code true} when this slot uses a named definition reference rather than an
     * inline item builder.
     */
    public boolean hasItemRef() {
        return itemRef != null && !itemRef.isBlank();
    }

    /** Returns {@code true} when this slot has no item (neither ref nor inline builder). */
    public boolean hasNoItem() {
        return itemRef == null && builder == null;
    }
}

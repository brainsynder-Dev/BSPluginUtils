package org.bsdevelopment.pluginutils.xml.model;

import org.bsdevelopment.pluginutils.inventory.ItemBuilder;
import org.bukkit.event.inventory.InventoryType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure-data representation of a complete XML GUI definition.
 *
 * <p>Instances are immutable and can be created either by the fluent builder API (for
 * hardcoded defaults) or by {@link org.bsdevelopment.pluginutils.xml.io.XmlGuiReader}
 * (from an XML file).
 *
 * <p>Example fluent usage:
 * <pre>{@code
 * XmlGuiDefinition def = XmlGuiDefinition.chest("&6My Shop", 3)
 *     .define("filler", ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).withName(" "))
 *     .define("close", ItemBuilder.of(Material.BARRIER).withName("&cClose"))
 *     .slot("0-8,18-26").ref("filler").end()
 *     .slot("13").item(ItemBuilder.of(Material.DIAMOND).withName("&bDiamond"))
 *         .action("message", "&aYou clicked a diamond!")
 *         .end()
 *     .slot("22").ref("close")
 *         .action("close")
 *         .end()
 *     .build();
 * }</pre>
 */
public final class XmlGuiDefinition {

    private final String title;
    private final int rows;                   // -1 when using inventoryType
    private final InventoryType inventoryType; // null when using rows
    private final Map<String, ItemBuilder> definitions;
    private final List<XmlSlotDefinition> slots;

    private XmlGuiDefinition(String title, int rows, InventoryType inventoryType,
                             Map<String, ItemBuilder> definitions, List<XmlSlotDefinition> slots) {
        this.title = title;
        this.rows = rows;
        this.inventoryType = inventoryType;
        this.definitions = Collections.unmodifiableMap(new LinkedHashMap<>(definitions));
        this.slots = Collections.unmodifiableList(new ArrayList<>(slots));
    }

    /**
     * Start building a chest-style (row-based) GUI definition.
     *
     * @param title GUI window title (supports {@code &}-color codes)
     * @param rows  number of rows (1–6)
     *
     * @return a new {@link DefinitionBuilder}
     */
    public static DefinitionBuilder chest(String title, int rows) {
        return new DefinitionBuilder(title, rows, null);
    }

    /**
     * Start building a typed inventory GUI definition (e.g. DROPPER, WORKBENCH).
     *
     * @param title GUI window title (supports {@code &}-color codes)
     * @param type  the {@link InventoryType} to use
     *
     * @return a new {@link DefinitionBuilder}
     */
    public static DefinitionBuilder typed(String title, InventoryType type) {
        return new DefinitionBuilder(title, -1, type);
    }

    /**
     * Parse a slot index expression into a list of resolved integer slot indices.
     *
     * <p>Supported formats:
     * <ul>
     *   <li>Single: {@code "4"} → {@code [4]}</li>
     *   <li>Range: {@code "0-8"} → {@code [0, 1, 2, 3, 4, 5, 6, 7, 8]}</li>
     *   <li>Comma list: {@code "0,4,8"} → {@code [0, 4, 8]}</li>
     *   <li>Mixed: {@code "0-8,18-26"} → {@code [0…8, 18…26]}</li>
     * </ul>
     *
     * @param expression the slot index expression string
     *
     * @return resolved list of slot indices
     *
     * @throws NumberFormatException if the expression contains non-integer tokens
     */
    public static List<Integer> parseSlotExpression(String expression) {
        List<Integer> result = new ArrayList<>();
        for (String segment : expression.split(",")) {
            segment = segment.trim();
            if (segment.isEmpty()) continue;
            if (segment.contains("-")) {
                String[] bounds = segment.split("-", 2);
                int from = Integer.parseInt(bounds[0].trim());
                int to = Integer.parseInt(bounds[1].trim());
                for (int i = from; i <= to; i++) result.add(i);
            } else {
                result.add(Integer.parseInt(segment));
            }
        }
        return result;
    }

    /**
     * Convert a list of slot indices to a compact range expression string.
     *
     * <p>Consecutive runs are collapsed to {@code from-to} notation.
     * Example: {@code [0,1,2,3,4,5,6,7,8,18,19,20,21,22,23,24,25,26]} → {@code "0-8,18-26"}.
     *
     * @param indices the slot indices
     *
     * @return compact slot expression
     */
    public static String toSlotExpression(List<Integer> indices) {
        if (indices.isEmpty()) return "";
        List<Integer> sorted = new ArrayList<>(indices);
        Collections.sort(sorted);

        StringBuilder sb = new StringBuilder();
        int start = sorted.get(0);
        int prev = start;

        for (int i = 1; i < sorted.size(); i++) {
            int curr = sorted.get(i);
            if (curr == prev + 1) {
                prev = curr;
            } else {
                appendRangeSegment(sb, start, prev);
                start = curr;
                prev = curr;
            }
        }
        appendRangeSegment(sb, start, prev);
        return sb.toString();
    }

    private static void appendRangeSegment(StringBuilder sb, int from, int to) {
        if (sb.length() > 0) sb.append(',');
        if (from == to) {
            sb.append(from);
        } else {
            sb.append(from).append('-').append(to);
        }
    }

    /** The GUI title (may contain {@code &}-style color codes). */
    public String getTitle() {
        return title;
    }

    // -------------------------------------------------------------------------
    // Static entry points
    // -------------------------------------------------------------------------

    /** Number of chest rows (1–6), or {@code -1} when using a typed inventory. */
    public int getRows() {
        return rows;
    }

    /** The typed {@link InventoryType}, or {@code null} for a chest-style inventory. */
    public InventoryType getInventoryType() {
        return inventoryType;
    }

    // -------------------------------------------------------------------------
    // Fluent builder
    // -------------------------------------------------------------------------

    /** Returns {@code true} when this is a chest-style (row-based) inventory. */
    public boolean isRowBased() {
        return inventoryType == null;
    }

    /**
     * Named item templates declared in {@code <definitions>}.
     * Keys are definition ids; values are {@link ItemBuilder} instances.
     */
    public Map<String, ItemBuilder> getDefinitions() {
        return definitions;
    }

    // -------------------------------------------------------------------------
    // Static helpers
    // -------------------------------------------------------------------------

    /** All slot definitions, in declaration order. */
    public List<XmlSlotDefinition> getSlots() {
        return slots;
    }

    /**
     * Builds an {@link XmlGuiDefinition} using a fluent API.
     *
     * <p>Typical flow:
     * <ol>
     *   <li>Call {@link XmlGuiDefinition#chest(String, int)} or {@link XmlGuiDefinition#typed}</li>
     *   <li>Optionally call {@link #define(String, ItemBuilder)} for reusable templates</li>
     *   <li>Call {@link #slot(String)} / {@link #slot(int...)} to add slot entries</li>
     *   <li>Call {@link #build()} to finish</li>
     * </ol>
     */
    public static final class DefinitionBuilder {

        private final String title;
        private final int rows;
        private final InventoryType inventoryType;
        private final Map<String, ItemBuilder> definitions = new LinkedHashMap<>();
        private final List<XmlSlotDefinition> slots = new ArrayList<>();

        private DefinitionBuilder(String title, int rows, InventoryType inventoryType) {
            this.title = title;
            this.rows = rows;
            this.inventoryType = inventoryType;
        }

        /**
         * Register a reusable item template that can be referenced from slots.
         *
         * <p>The {@code id} is used in the XML {@code item="id"} attribute and from the
         * builder via {@link SlotBuilder#ref(String)}.
         *
         * @param id      unique definition identifier
         * @param builder the item to associate with that id
         *
         * @return this builder
         */
        public DefinitionBuilder define(String id, ItemBuilder builder) {
            definitions.put(id, builder);
            return this;
        }

        /**
         * Start a slot definition using an index expression.
         *
         * <p>The expression supports:
         * <ul>
         *   <li>Single index: {@code "4"}</li>
         *   <li>Range: {@code "0-8"}</li>
         *   <li>Comma-separated: {@code "0,4,8"}</li>
         *   <li>Mixed: {@code "0-8,18-26"}</li>
         * </ul>
         *
         * @param indexExpression slot index expression
         *
         * @return a {@link SlotBuilder} for configuring this slot entry
         */
        public SlotBuilder slot(String indexExpression) {
            return new SlotBuilder(this, parseSlotExpression(indexExpression));
        }

        /**
         * Start a slot definition with explicit integer indices.
         *
         * @param indices one or more slot indices
         *
         * @return a {@link SlotBuilder} for configuring this slot entry
         */
        public SlotBuilder slot(int... indices) {
            List<Integer> list = new ArrayList<>(indices.length);
            for (int i : indices) list.add(i);
            return new SlotBuilder(this, list);
        }

        /**
         * Directly add a pre-built {@link XmlSlotDefinition}.
         *
         * <p>This is used by {@link org.bsdevelopment.pluginutils.xml.io.XmlGuiReader}
         * and advanced programmatic builders that bypass the {@link SlotBuilder} API.
         *
         * @param slot the pre-built slot definition
         *
         * @return this builder
         */
        public DefinitionBuilder addSlot(XmlSlotDefinition slot) {
            slots.add(slot);
            return this;
        }

        /**
         * Build and return the immutable {@link XmlGuiDefinition}.
         *
         * @return the completed definition
         */
        public XmlGuiDefinition build() {
            return new XmlGuiDefinition(title, rows, inventoryType, definitions, slots);
        }
    }

    /**
     * Fluent builder for a single slot entry (potentially spanning multiple indices).
     *
     * <p>Obtain via {@link DefinitionBuilder#slot(String)} or {@link DefinitionBuilder#slot(int...)}.
     * Configure the item and actions, then call {@link #end()} to return to the parent builder.
     */
    public static final class SlotBuilder {

        private final DefinitionBuilder parent;
        private final List<Integer> slotIndices;
        private final List<XmlActionDefinition> actionDefs = new ArrayList<>();
        private String itemRef;
        private ItemBuilder itemBuilder;

        private SlotBuilder(DefinitionBuilder parent, List<Integer> slotIndices) {
            this.parent = parent;
            this.slotIndices = slotIndices;
        }

        /**
         * Use a named item template (defined via {@link DefinitionBuilder#define}) or a
         * namespaced {@code ItemRegistry} key ({@code "ns:id"}).
         *
         * @param definitionId the template id or namespaced key
         *
         * @return this builder
         */
        public SlotBuilder ref(String definitionId) {
            this.itemRef = definitionId;
            this.itemBuilder = null;
            return this;
        }

        /**
         * Use an inline item builder for this slot.
         *
         * @param builder item builder
         *
         * @return this builder
         */
        public SlotBuilder item(ItemBuilder builder) {
            this.itemBuilder = builder;
            this.itemRef = null;
            return this;
        }

        /**
         * Add an action with just a type and no extra data.
         *
         * <p>Example: {@code .action("close")}
         *
         * @param type action type key
         *
         * @return this builder
         */
        public SlotBuilder action(String type) {
            actionDefs.add(new XmlActionDefinition(type, null, null));
            return this;
        }

        /**
         * Add an action with a type and text content.
         *
         * <p>Examples:
         * <pre>{@code
         * .action("message", "&aHello!")
         * .action("command", "/spawn")
         * .action("give", "DIAMOND 5")
         * }</pre>
         *
         * @param type action type key
         * @param text text content (e.g. message, command, or material+amount)
         *
         * @return this builder
         */
        public SlotBuilder action(String type, String text) {
            actionDefs.add(new XmlActionDefinition(type, null, text));
            return this;
        }

        /**
         * Add an action with a type, attribute map, and optional text content.
         *
         * <p>Example:
         * <pre>{@code
         * .action("give", Map.of("item", "myplugin:sword", "amount", "1"), "")
         * }</pre>
         *
         * @param type       action type key
         * @param attributes extra key→value attributes
         * @param text       text content or empty string
         *
         * @return this builder
         */
        public SlotBuilder action(String type, Map<String, String> attributes, String text) {
            actionDefs.add(new XmlActionDefinition(type, attributes, text));
            return this;
        }

        /**
         * Add a pre-built {@link XmlActionDefinition}.
         *
         * @param definition the action definition
         *
         * @return this builder
         */
        public SlotBuilder action(XmlActionDefinition definition) {
            actionDefs.add(definition);
            return this;
        }

        /**
         * Finish configuring this slot and return to the parent {@link DefinitionBuilder}.
         *
         * @return the parent definition builder
         */
        public DefinitionBuilder end() {
            parent.addSlot(new XmlSlotDefinition(slotIndices, itemRef, itemBuilder, actionDefs));
            return parent;
        }
    }
}

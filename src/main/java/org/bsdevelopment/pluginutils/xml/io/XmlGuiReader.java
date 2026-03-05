package org.bsdevelopment.pluginutils.xml.io;

import org.bsdevelopment.pluginutils.gui.parser.XmlValidationException;
import org.bsdevelopment.pluginutils.inventory.ItemBuilder;
import org.bsdevelopment.pluginutils.item.ItemXmlIO;
import org.bsdevelopment.pluginutils.xml.model.XmlActionDefinition;
import org.bsdevelopment.pluginutils.xml.model.XmlGuiDefinition;
import org.bsdevelopment.pluginutils.xml.model.XmlSlotDefinition;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.plugin.java.JavaPlugin;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads an XML GUI file into an {@link XmlGuiDefinition}.
 *
 * <p>Supported XML schema:
 * <pre>{@code
 * <gui title="&6My GUI" rows="3">            <!-- or inventory-type="DROPPER" -->
 *
 *   <definitions>                            <!-- optional reusable items -->
 *     <item id="filler" material="GRAY_STAINED_GLASS_PANE" name=" "/>
 *     <item id="close-btn" material="BARRIER" name="&cClose">
 *       <lore><line>&7Click to close.</line></lore>
 *     </item>
 *   </definitions>
 *
 *   <slots>
 *     <!-- Single slot with item reference, no actions -->
 *     <slot index="4" item="filler"/>
 *
 *     <!-- Range with item reference -->
 *     <slot index="0-8,18-26" item="filler"/>
 *
 *     <!-- Inline item with actions -->
 *     <slot index="13" material="DIAMOND" name="&bDiamond">
 *       <lore><line>&7A shiny gem.</line></lore>
 *       <actions>
 *         <action type="message">&aYou clicked!</action>
 *         <action type="give">DIAMOND 1</action>
 *       </actions>
 *     </slot>
 *
 *     <!-- Reference with actions -->
 *     <slot index="22" item="close-btn">
 *       <actions>
 *         <action type="close"/>
 *       </actions>
 *     </slot>
 *   </slots>
 * </gui>
 * }</pre>
 *
 * <p>Item attributes inside {@code <item>} and inline {@code <slot>} elements follow the same
 * schema as {@link ItemXmlIO} (material, name, lore, enchants, flags, attributes,
 * persistent-data, nbt-json, skull-texture).
 */
public final class XmlGuiReader {

    private XmlGuiReader() {
    }

    /**
     * Read an {@link XmlGuiDefinition} from a file.
     *
     * @param file   the XML GUI file
     * @param plugin the owning plugin (used for namespaced keys in item parsing)
     *
     * @return the parsed definition
     *
     * @throws XmlValidationException if the file cannot be read or the XML is invalid
     */
    public static XmlGuiDefinition read(File file, JavaPlugin plugin) {
        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(file);
            doc.getDocumentElement().normalize();
            return parse(doc.getDocumentElement(), plugin);
        } catch (XmlValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new XmlValidationException(null,
                    "Failed to read GUI XML '" + file.getName() + "': " + e.getMessage(),
                    "Ensure the file is well-formed XML and the schema is correct");
        }
    }

    /**
     * Read an {@link XmlGuiDefinition} from an {@link InputStream}.
     *
     * <p>The stream is not closed by this method.
     *
     * @param stream the XML source stream
     * @param plugin the owning plugin
     *
     * @return the parsed definition
     *
     * @throws XmlValidationException if the stream cannot be parsed
     */
    public static XmlGuiDefinition read(InputStream stream, JavaPlugin plugin) {
        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(stream);
            doc.getDocumentElement().normalize();
            return parse(doc.getDocumentElement(), plugin);
        } catch (XmlValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new XmlValidationException(null,
                    "Failed to parse GUI XML stream: " + e.getMessage(),
                    "Ensure the stream contains well-formed XML");
        }
    }

    // -------------------------------------------------------------------------
    // Core parsing
    // -------------------------------------------------------------------------

    private static XmlGuiDefinition parse(Element root, JavaPlugin plugin) {
        requireTag(root, "gui");

        String title = requireAttr(root, "title",
                "Add a title attribute to <gui>, e.g. title=\"&6My Menu\"");

        // ── Inventory spec ────────────────────────────────────────────────────
        int rows = -1;
        InventoryType inventoryType = null;

        if (root.hasAttribute("inventory-type")) {
            String raw = root.getAttribute("inventory-type").trim().toUpperCase(Locale.ROOT);
            try {
                inventoryType = InventoryType.valueOf(raw);
            } catch (IllegalArgumentException e) {
                throw new XmlValidationException(root,
                        "Invalid inventory-type: '" + raw + "'",
                        "Use a valid InventoryType such as CHEST, DROPPER, HOPPER, WORKBENCH");
            }
        } else {
            rows = parseIntAttr(root, "rows", 1, 6,
                    "rows must be between 1 and 6, e.g. rows=\"3\"");
        }

        // ── Definitions ───────────────────────────────────────────────────────
        XmlGuiDefinition.DefinitionBuilder builder = inventoryType != null
                ? XmlGuiDefinition.typed(title, inventoryType)
                : XmlGuiDefinition.chest(title, rows);

        Element defsBlock = firstChildElement(root, "definitions");
        if (defsBlock != null) {
            NodeList itemNodes = defsBlock.getElementsByTagName("item");
            Map<String, Boolean> seenIds = new LinkedHashMap<>();
            for (int i = 0; i < itemNodes.getLength(); i++) {
                // Only process direct children of <definitions>, not nested items
                Node parent = itemNodes.item(i).getParentNode();
                if (parent != defsBlock) continue;

                Element itemEl = (Element) itemNodes.item(i);
                String id = requireAttr(itemEl, "id",
                        "Each <item> inside <definitions> needs an id attribute");

                if (seenIds.containsKey(id)) {
                    throw new XmlValidationException(itemEl,
                            "Duplicate definition id='" + id + "'",
                            "Use a unique id for each <item> inside <definitions>");
                }
                seenIds.put(id, true);

                ItemBuilder itemBuilder = ItemXmlIO.read(itemEl, plugin);
                builder.define(id, itemBuilder);
            }
        }

        // ── Slots ─────────────────────────────────────────────────────────────
        Element slotsBlock = firstChildElement(root, "slots");
        if (slotsBlock != null) {
            NodeList children = slotsBlock.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                if (!(node instanceof Element child)) continue;

                switch (child.getTagName()) {
                    case "slot" -> builder.addSlot(parseSlot(child, plugin));
                    // Future extension point: <fill>, <border>, <pattern>, etc.
                    default -> { /* ignore unknown elements */ }
                }
            }
        }

        return builder.build();
    }

    // -------------------------------------------------------------------------
    // Slot parsing
    // -------------------------------------------------------------------------

    private static XmlSlotDefinition parseSlot(Element slotEl, JavaPlugin plugin) {
        // ── Slot indices ──────────────────────────────────────────────────────
        String indexExpr = requireAttr(slotEl, "index",
                "Each <slot> needs an index attribute (e.g. index=\"4\" or index=\"0-8,18-26\")");

        List<Integer> slotIndices;
        try {
            slotIndices = XmlGuiDefinition.parseSlotExpression(indexExpr);
        } catch (NumberFormatException e) {
            throw new XmlValidationException(slotEl,
                    "Invalid slot index expression: '" + indexExpr + "'",
                    "Use integers, ranges (0-8), or comma-separated combinations (0-8,18-26)");
        }

        if (slotIndices.isEmpty()) {
            throw new XmlValidationException(slotEl,
                    "Slot index expression resolved to no slots: '" + indexExpr + "'",
                    "Provide at least one valid slot index");
        }

        // ── Item ──────────────────────────────────────────────────────────────
        String itemRef = null;
        ItemBuilder itemBuilder = null;

        if (slotEl.hasAttribute("item")) {
            // Reference to a <definitions> entry or namespaced ItemRegistry key
            itemRef = slotEl.getAttribute("item").trim();
        } else if (hasItemAttributes(slotEl)) {
            // Inline item definition (same schema as <item> elements)
            itemBuilder = ItemXmlIO.read(slotEl, plugin);
        }
        // else: slot intentionally has no item (e.g. invisible click zone)

        // ── Actions ───────────────────────────────────────────────────────────
        List<XmlActionDefinition> actions = new ArrayList<>();
        Element actionsEl = firstChildElement(slotEl, "actions");
        if (actionsEl != null) {
            NodeList actionNodes = actionsEl.getChildNodes();
            for (int i = 0; i < actionNodes.getLength(); i++) {
                Node node = actionNodes.item(i);
                if (!(node instanceof Element actionEl)) continue;
                if (!"action".equals(actionEl.getTagName())) continue;

                actions.add(parseAction(actionEl));
            }
        }

        return new XmlSlotDefinition(slotIndices, itemRef, itemBuilder, actions);
    }

    private static XmlActionDefinition parseAction(Element actionEl) {
        String type = requireAttr(actionEl, "type",
                "Each <action> needs a type attribute, e.g. type=\"message\"");

        // Collect all attributes except "type"
        Map<String, String> attrs = new LinkedHashMap<>();
        NamedNodeMap nodeMap = actionEl.getAttributes();
        for (int i = 0; i < nodeMap.getLength(); i++) {
            Node attr = nodeMap.item(i);
            if (!"type".equals(attr.getNodeName())) {
                attrs.put(attr.getNodeName(), attr.getNodeValue());
            }
        }

        String text = actionEl.getTextContent().trim();
        return new XmlActionDefinition(type, attrs, text);
    }

    // -------------------------------------------------------------------------
    // XML utilities
    // -------------------------------------------------------------------------

    /** Returns the first direct child {@link Element} with the given tag name, or null. */
    private static Element firstChildElement(Element parent, String tag) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element el && el.getTagName().equals(tag)) {
                return el;
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if the element has attributes or children that signal an inline
     * item definition (material, skull-texture, or nbt-json child).
     */
    private static boolean hasItemAttributes(Element el) {
        return el.hasAttribute("material")
                || el.hasAttribute("skull-texture")
                || el.getElementsByTagName("nbt-json").getLength() > 0;
    }

    private static void requireTag(Element el, String expected) {
        if (!expected.equals(el.getTagName())) {
            throw new XmlValidationException(el,
                    "Expected <" + expected + ">, found <" + el.getTagName() + ">",
                    "Rename the root element to <" + expected + ">");
        }
    }

    private static String requireAttr(Element el, String name, String hint) {
        String value = el.getAttribute(name);
        if (value == null || value.isBlank()) {
            throw new XmlValidationException(el,
                    "Missing required attribute '" + name + "' on <" + el.getTagName() + ">",
                    hint);
        }
        return value;
    }

    private static int parseIntAttr(Element el, String name, int min, int max, String hint) {
        String raw = requireAttr(el, name, hint);
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < min || value > max) {
                throw new XmlValidationException(el,
                        "Value " + value + " for '" + name + "' is out of range [" + min + ", " + max + "]",
                        hint);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new XmlValidationException(el,
                    "Cannot parse '" + raw + "' as integer for attribute '" + name + "'",
                    hint);
        }
    }
}

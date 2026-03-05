package org.bsdevelopment.pluginutils.xml.io;

import org.bsdevelopment.pluginutils.inventory.ItemBuilder;
import org.bsdevelopment.pluginutils.item.ItemXmlIO;
import org.bsdevelopment.pluginutils.xml.XmlUtils;
import org.bsdevelopment.pluginutils.xml.model.XmlActionDefinition;
import org.bsdevelopment.pluginutils.xml.model.XmlGuiDefinition;
import org.bsdevelopment.pluginutils.xml.model.XmlSlotDefinition;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * Serializes an {@link XmlGuiDefinition} to an XML file or output stream.
 *
 * <p>The produced XML matches the schema expected by {@link XmlGuiReader}, allowing
 * round-trip save/load operations. This is primarily used by
 * {@link org.bsdevelopment.pluginutils.xml.XmlGuiManager} to write hardcoded default GUIs
 * to disk so server owners can customise them.
 *
 * <p>Item elements are serialized using {@link ItemXmlIO#writeInto} so all item properties
 * (material, name, lore, enchants, flags, attributes, skull texture, NBT JSON, etc.) are
 * preserved.
 *
 * <p>Slot index lists are compacted to range notation (e.g. {@code "0-8,18-26"}) via
 * {@link XmlGuiDefinition#toSlotExpression}.
 */
public final class XmlGuiWriter {

    private XmlGuiWriter() {
    }

    /**
     * Write an {@link XmlGuiDefinition} to a file.
     *
     * <p>Creates or overwrites the file. Parent directories must already exist (or be created
     * by the caller — {@link org.bsdevelopment.pluginutils.xml.XmlGuiManager} handles this).
     *
     * @param file       destination file
     * @param definition the definition to serialize
     *
     * @throws RuntimeException wrapping any I/O or XML transformer exception
     */
    public static void write(File file, XmlGuiDefinition definition) {
        try (OutputStream out = new FileOutputStream(file)) {
            write(out, definition);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to write GUI XML to '" + file + "': " + e.getMessage(), e);
        }
    }

    /**
     * Write an {@link XmlGuiDefinition} to an {@link OutputStream}.
     *
     * <p>The stream is not closed by this method.
     *
     * @param out        destination stream
     * @param definition the definition to serialize
     *
     * @throws RuntimeException wrapping any XML serialization exception
     */
    public static void write(OutputStream out, XmlGuiDefinition definition) {
        Document doc = XmlUtils.newDocument();
        doc.appendChild(buildGuiElement(doc, definition));
        XmlUtils.writeDocument(doc, out);
    }

    // -------------------------------------------------------------------------
    // Element builders
    // -------------------------------------------------------------------------

    private static Element buildGuiElement(Document doc, XmlGuiDefinition definition) {
        Element gui = doc.createElement("gui");
        gui.setAttribute("title", definition.getTitle());

        if (definition.isRowBased()) {
            gui.setAttribute("rows", Integer.toString(definition.getRows()));
        } else {
            gui.setAttribute("inventory-type", definition.getInventoryType().name());
        }

        // <definitions>
        if (!definition.getDefinitions().isEmpty()) {
            gui.appendChild(buildDefinitionsElement(doc, definition.getDefinitions()));
        }

        // <slots>
        if (!definition.getSlots().isEmpty()) {
            gui.appendChild(buildSlotsElement(doc, definition));
        }

        return gui;
    }

    private static Element buildDefinitionsElement(Document doc, Map<String, ItemBuilder> definitions) {
        Element defsEl = doc.createElement("definitions");

        for (Map.Entry<String, ItemBuilder> entry : definitions.entrySet()) {
            Element itemEl = doc.createElement("item");
            itemEl.setAttribute("id", entry.getKey());
            ItemXmlIO.writeInto(doc, itemEl, entry.getValue());
            defsEl.appendChild(itemEl);
        }

        return defsEl;
    }

    private static Element buildSlotsElement(Document doc, XmlGuiDefinition definition) {
        Element slotsEl = doc.createElement("slots");

        for (XmlSlotDefinition slotDef : definition.getSlots()) {
            slotsEl.appendChild(buildSlotElement(doc, slotDef));
        }

        return slotsEl;
    }

    private static Element buildSlotElement(Document doc, XmlSlotDefinition slotDef) {
        Element slotEl = doc.createElement("slot");

        // ── index expression ──────────────────────────────────────────────────
        slotEl.setAttribute("index", XmlGuiDefinition.toSlotExpression(slotDef.getSlots()));

        // ── item ──────────────────────────────────────────────────────────────
        if (slotDef.hasItemRef()) {
            slotEl.setAttribute("item", slotDef.getItemRef());
        } else if (slotDef.getBuilder() != null) {
            // Inline item — write item properties directly into the <slot> element
            ItemXmlIO.writeInto(doc, slotEl, slotDef.getBuilder());
        }

        // ── actions ───────────────────────────────────────────────────────────
        List<XmlActionDefinition> actions = slotDef.getActions();
        if (!actions.isEmpty()) {
            Element actionsEl = doc.createElement("actions");
            for (XmlActionDefinition actionDef : actions) {
                actionsEl.appendChild(buildActionElement(doc, actionDef));
            }
            slotEl.appendChild(actionsEl);
        }

        return slotEl;
    }

    private static Element buildActionElement(Document doc, XmlActionDefinition actionDef) {
        Element actionEl = doc.createElement("action");
        actionEl.setAttribute("type", actionDef.getType());

        // Write extra attributes (all except "type" which is already set)
        for (Map.Entry<String, String> entry : actionDef.getAttributes().entrySet()) {
            actionEl.setAttribute(entry.getKey(), entry.getValue());
        }

        // Write text content if present
        String text = actionDef.getText();
        if (!text.isBlank()) {
            actionEl.setTextContent(text);
        }

        return actionEl;
    }
}

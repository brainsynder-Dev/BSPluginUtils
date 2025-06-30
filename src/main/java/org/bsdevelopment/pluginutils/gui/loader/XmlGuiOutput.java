package org.bsdevelopment.pluginutils.gui.loader;

import org.bsdevelopment.pluginutils.inventory.ItemBuilder;
import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Build a <gui></gui> XML from Java data and write it to disk.
 *
 * <p>Usage:
 * <pre>{@code
 * XmlGuiOutput out = new XmlGuiOutput("&eMy GUI", 3);
 * // Alternate:
 * XmlGuiOutput out2 = new XmlGuiOutput("&eMy GUI", InventoryType.DROPPER);
 *
 * // 1) add definitions
 * out.addDefinition("poweredSword", ItemBuilder.of(Material.DIAMOND_SWORD)
 *     .withName("&bPowered Sword")
 *     .withLore("Electrified","Handle with care")
 *     .setUnbreakable(true)
 *     .withEnchant(Enchantment.SHARPNESS,5)
 * );
 * // 2) add a component that uses that definition
 * XmlGuiOutput.Component comp = new XmlGuiOutput.Component(4,1,"poweredSword");
 * comp.addAction(new XmlGuiOutput.Action("message", Map.of(), "&aYou clicked!"));
 * comp.addAction(new XmlGuiOutput.Action("give", Map.of("item-id","poweredSword","amount","1"), ""));
 * out.addComponent(comp);
 * // 3) write it out
 * out.write(new File(plugin.getDataFolder(), "output-gui.xml"));
 * }</pre>
 */
public class XmlGuiOutput {
    private final String title;
    private final InventorySpecification specification;

    private final Map<String, ItemBuilder> definitions = new LinkedHashMap<>();
    private final List<GuiComponent> components = new ArrayList<>();

    /**
     * Creates an output using a named inventory type.
     *
     * @param title
     *         the GUI title (supports '&' color codes)
     * @param inventoryType
     *         the Bukkit InventoryType, e.g. "CHEST", "ENDER_CHEST"
     */
    public XmlGuiOutput(String title, InventoryType inventoryType) {
        this.title = title;
        this.specification = new InventorySpecification(inventoryType, -1);
    }

    /**
     * Creates an output using a chest-style inventory with the given rows.
     *
     * @param title
     *         the GUI title (supports '&' color codes)
     * @param rows
     *         number of rows (1–6) in a chest inventory (9 columns each)
     */
    public XmlGuiOutput(String title, int rows) {
        this.title = title;
        this.specification = new InventorySpecification(null, rows);
    }

    /**
     * Register a reusable item‐template.
     *
     * @param id
     *         the definition id used in XML
     * @param builder
     *         the ItemBuilder that holds all name/lore/enchant/etc.
     */
    public void addDefinition(String id, ItemBuilder builder) {
        definitions.put(id, builder);
    }

    /**
     * Register a GUI slot.
     *
     * @param component
     *         the component with x,y and either itemId or inline builder
     */
    public void addComponent(GuiComponent component) {
        components.add(component);
    }

    /**
     * Write the built XML to the given file (overwrites if exists).
     *
     * @param file
     *         the output file
     *
     * @throws Exception
     *         on I/O or XML errors
     */
    public void write(File file) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .newDocument();

        // root <gui>
        Element guiElement = doc.createElement("gui");
        guiElement.setAttribute("title", title);
        if (specification.inventoryType != null) {
            guiElement.setAttribute("inventory-type", specification.inventoryType.name());
        } else {
            guiElement.setAttribute("rows", Integer.toString(specification.rows));
        }
        doc.appendChild(guiElement);

        // <definitions>
        if (!definitions.isEmpty()) {
            Element definitions = doc.createElement("definitions");
            for (var entry : this.definitions.entrySet()) {
                definitions.appendChild(buildDefinitionElement(doc, entry.getKey(), entry.getValue()));
            }
            guiElement.appendChild(definitions);
        }

        // <component> entries
        for (GuiComponent component : components) {
            guiElement.appendChild(buildComponentElement(doc, component));
        }

        // write to file
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(new DOMSource(doc), new StreamResult(file));
    }

    private Element buildDefinitionElement(Document doc, String id, ItemBuilder builder) {
        Element itemDefinition = doc.createElement("item-definition");
        itemDefinition.setAttribute("id", id);
        // use ItemBuilder → ItemStack → ItemMeta to extract all properties:
        var item = builder.build();
        var meta = item.getItemMeta();

        itemDefinition.setAttribute("material", item.getType().name());
        itemDefinition.setAttribute("amount", String.valueOf(item.getAmount()));

        // name
        if (meta.hasDisplayName()) itemDefinition.setAttribute("name", meta.getDisplayName());

        // unbreakable
        if (meta.isUnbreakable()) itemDefinition.setAttribute("unbreakable", "true");

        if (meta instanceof SkullMeta skull && skull.getOwnerProfile() != null) {
            // assumes PlayerProfileHelper can extract the texture URL
            PlayerProfile profile = skull.getOwnerProfile();
            itemDefinition.setAttribute("skull-texture", profile.getTextures().getSkin().toString());
        }

        // lore
        if (meta.hasLore()) {
            Element loreElement = doc.createElement("lore");

            for (String line : meta.getLore()) {
                Element lineElement = doc.createElement("line");
                lineElement.setTextContent(line);
                loreElement.appendChild(lineElement);
            }

            itemDefinition.appendChild(loreElement);
        }

        // flags
        if (!meta.getItemFlags().isEmpty()) {
            Element flagsElement = doc.createElement("flags");

            for (var flag : meta.getItemFlags()) {
                Element element = doc.createElement("flag");
                element.setTextContent(flag.name());
                flagsElement.appendChild(element);
            }

            itemDefinition.appendChild(flagsElement);
        }

        // enchants
        if (!meta.getEnchants().isEmpty()) {
            Element enchantsElement = doc.createElement("enchants");

            for (var entry : meta.getEnchants().entrySet()) {
                Element element = doc.createElement("enchant");
                element.setAttribute("type", entry.getKey().getKey().getKey());
                element.setAttribute("level", String.valueOf(entry.getValue()));
                enchantsElement.appendChild(element);
            }

            itemDefinition.appendChild(enchantsElement);
        }

        // attributes
        if (meta.hasAttributeModifiers()) {
            Element attributesElement = doc.createElement("attributes");

            meta.getAttributeModifiers().forEach((attribute, modifiers) -> {
                Element element = doc.createElement("attribute");
                element.setAttribute("attribute", attribute.getKey().toString());
                element.setAttribute("amount", String.valueOf(modifiers.getAmount()));
                element.setAttribute("operation", modifiers.getOperation().name());
                element.setAttribute("slot", modifiers.getSlot().toString());
                attributesElement.appendChild(element);
            });

            itemDefinition.appendChild(attributesElement);
        }

        // persistent-data (string & int only)
        Element pdcList = buildPersistentDataList(doc, meta);
        if (pdcList != null) itemDefinition.appendChild(pdcList);

        // raw NBT JSON?
        try {
            var tag = builder.toTag();
            Element nbtElement = doc.createElement("nbt-json");
            nbtElement.setTextContent(tag.toString());
            itemDefinition.appendChild(nbtElement);
        } catch (Exception ignored) {
        }

        return itemDefinition;
    }

    private Element buildComponentElement(Document doc, GuiComponent comp) {
        Element componentElement = doc.createElement("component");
        componentElement.setAttribute("type", "item");
        componentElement.setAttribute("slot", Integer.toString(comp.slot));

        if (comp.itemId != null) {
            componentElement.setAttribute("item-id", comp.itemId);
        } else {
            var inlineDef = buildDefinitionElement(doc, "_inline", comp.builder);

            NamedNodeMap attributes = inlineDef.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                var a = attributes.item(i);
                componentElement.setAttribute(a.getNodeName(), a.getNodeValue());
            }

            for (var child = inlineDef.getFirstChild(); child != null; child = child.getNextSibling()) {
                componentElement.appendChild(child.cloneNode(true));
            }
        }

        // actions
        for (Action action : comp.actions) {
            Element actionElement = doc.createElement("action");
            actionElement.setAttribute("type", action.type);

            for (var entry : action.attributes.entrySet()) {
                actionElement.setAttribute(entry.getKey(), entry.getValue());
            }

            if (!action.text.isBlank()) actionElement.setTextContent(action.text);

            componentElement.appendChild(actionElement);
        }
        return componentElement;
    }

    /**
     * Builds a <persistent-data-list> element from the given ItemMeta’s
     * PersistentDataContainer, covering STRING, BOOLEAN, INT, BYTE,
     * DOUBLE, FLOAT, LONG, and SHORT. Returns null if the container is empty.
     *
     * @param doc
     *         the XML Document
     * @param meta
     *         the ItemMeta holding the data
     *
     * @return an Element ready to append, or null if no data
     */
    private Element buildPersistentDataList(Document doc, ItemMeta meta) {
        var pdc = meta.getPersistentDataContainer();
        if (pdc.getKeys().isEmpty()) return null;

        Element listElement = doc.createElement("persistent-data-list");
        for (NamespacedKey key : pdc.getKeys()) {
            String type;
            String value;

            if (pdc.has(key, PersistentDataType.STRING)) {
                type = "STRING";
                value = pdc.get(key, PersistentDataType.STRING);
            } else if (pdc.has(key, PersistentDataType.BOOLEAN)) {
                type = "BOOLEAN";
                value = Boolean.toString(pdc.get(key, PersistentDataType.BOOLEAN));
            } else if (pdc.has(key, PersistentDataType.INTEGER)) {
                type = "INT";
                value = Integer.toString(pdc.get(key, PersistentDataType.INTEGER));
            } else if (pdc.has(key, PersistentDataType.BYTE)) {
                type = "BYTE";
                value = Byte.toString(pdc.get(key, PersistentDataType.BYTE));
            } else if (pdc.has(key, PersistentDataType.DOUBLE)) {
                type = "DOUBLE";
                value = Double.toString(pdc.get(key, PersistentDataType.DOUBLE));
            } else if (pdc.has(key, PersistentDataType.FLOAT)) {
                type = "FLOAT";
                value = Float.toString(pdc.get(key, PersistentDataType.FLOAT));
            } else if (pdc.has(key, PersistentDataType.LONG)) {
                type = "LONG";
                value = Long.toString(pdc.get(key, PersistentDataType.LONG));
            } else if (pdc.has(key, PersistentDataType.SHORT)) {
                type = "SHORT";
                value = Short.toString(pdc.get(key, PersistentDataType.SHORT));
            } else continue;

            Element pdElement = doc.createElement("persistent-data");
            pdElement.setAttribute("key", key.getKey());
            pdElement.setAttribute("type", type);
            pdElement.setTextContent(value);
            listElement.appendChild(pdElement);
        }

        return listElement;
    }


    //───────────────────────────────────────────────────────────────────────────
    // Public model classes
    //───────────────────────────────────────────────────────────────────────────

    private record InventorySpecification(InventoryType inventoryType, int rows) {
    }

    /**
     * A slot in the GUI.
     */
    public static class GuiComponent {
        public final int slot;
        public final String itemId;       // null if inline
        public final ItemBuilder builder; // only if itemId==null
        public final List<Action> actions = new ArrayList<>();

        /**
         * Use a defined template.
         */
        public GuiComponent(int slot, String itemId) {
            this.slot = slot;
            this.itemId = itemId;
            this.builder = null;
        }

        /**
         * Inline‐build the item here.
         */
        public GuiComponent(int slot, ItemBuilder builder) {
            this.slot = slot;
            this.builder = builder;
            this.itemId = null;
        }

        /**
         * Add an action for this slot.
         */
        public void addAction(Action action) {
            actions.add(action);
        }
    }

    /**
     * A click‐action to appear in XML.
     */
    public static class Action {
        public final String type;
        public final Map<String, String> attributes;
        public final String text;

        /**
         * @param type
         *         the action type, e.g. "message", "give", etc.
         * @param attributes
         *         key→value attributes on the <action> tag
         * @param text
         *         optional text‐content (empty for self-closing)
         */
        public Action(String type, Map<String, String> attributes, String text) {
            this.type = type;
            this.attributes = attributes == null ? Map.of() : attributes;
            this.text = text == null ? "" : text;
        }
    }
}

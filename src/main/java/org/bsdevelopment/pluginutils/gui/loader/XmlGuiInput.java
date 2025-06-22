package org.bsdevelopment.pluginutils.gui.loader;

import org.bsdevelopment.pluginutils.gui.ActionRegistry;
import org.bsdevelopment.pluginutils.gui.CustomGui;
import org.bsdevelopment.pluginutils.gui.GuiAction;
import org.bsdevelopment.pluginutils.gui.parser.XmlUtils;
import org.bsdevelopment.pluginutils.gui.parser.XmlValidationException;
import org.bsdevelopment.pluginutils.inventory.ItemBuilder;
import org.bsdevelopment.pluginutils.nbt.serialization.NBTJSON;
import org.bsdevelopment.pluginutils.nbt.types.CompoundData;
import org.bsdevelopment.pluginutils.text.Colorize;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads a <gui> XML into a CustomGui, building each ItemStack via ItemBuilder
 */
public class XmlGuiInput {

    private static final Map<String, ItemBuilder> definitions = new HashMap<>();

    public static CustomGui load(JavaPlugin plugin, File xmlFile) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
            doc.getDocumentElement().normalize();
            
            Element guiElement = doc.getDocumentElement();
            XmlUtils.requireTag(guiElement, "gui");

            String title = Colorize.translateBungeeHex(XmlUtils.requireAttribute(guiElement, "title"));
            int width = XmlUtils.parseIntAttribute(guiElement, "width", 1, 54, "Width must be 1–54");
            int height = XmlUtils.parseIntAttribute(guiElement, "height", 1, 6, "Height must be 1–6");
            
            Inventory inv = Bukkit.createInventory(null, width * height, title);

            // item definitions
            definitions.clear();
            NodeList itemDefinitions = guiElement.getElementsByTagName("item-definition");
            for (int i = 0; i < itemDefinitions.getLength(); i++) {
                Element def = (Element) itemDefinitions.item(i);
                String id = XmlUtils.requireAttribute(def, "id");
                if (definitions.containsKey(id)) 
                    throw new XmlValidationException(def, "Duplicate definition id='" + id + "'", "Use a unique id per <item-definition>");
                
                definitions.put(id, parseItemBuilder(def, plugin));
            }

            // gui components
            Map<Integer, List<GuiAction>> actionMap = new HashMap<>();
            NodeList components = guiElement.getElementsByTagName("component");
            for (int i = 0; i < components.getLength(); i++) {
                Element component = (Element) components.item(i);
                XmlUtils.requireTag(component, "component");

                int x = XmlUtils.parseIntAttribute(component, "x", 0, width - 1, "x must be 0–" + (width - 1));
                int y = XmlUtils.parseIntAttribute(component, "y", 0, height - 1, "y must be 0–" + (height - 1));
                int slot = y * width + x;

                ItemBuilder builder;
                if (component.hasAttribute("item-id")) {
                    String ref = component.getAttribute("item-id");
                    builder = definitions.get(ref);
                    if (builder == null) throw new XmlValidationException(component, 
                            "Unknown item-id='" + ref + "'", 
                            "Declare <item-definition id=\"" + ref + "\"/> first"
                    );
                } else {
                    builder = parseItemBuilder(component, plugin);
                }
                inv.setItem(slot, builder.build());

                // parse actions (unchanged)
                NodeList actions = component.getElementsByTagName("action");
                List<GuiAction> list = new ArrayList<>();
                for (int j = 0; j < actions.getLength(); j++) {
                    Element element = (Element) actions.item(j);
                    var action = ActionRegistry.parse(element)
                            .orElseThrow(() -> new XmlValidationException(
                                    element,
                                    "Unsupported action type='" + element.getAttribute("type") + "'",
                                    "Register it via ActionRegistry.register(...)"
                            ));
                    list.add(action);
                }
                if (!list.isEmpty()) actionMap.put(slot, list);
            }

            CustomGui gui = new CustomGui(inv, actionMap);
            gui.register(plugin);
            return gui;
        } catch (XmlValidationException xmlValidationException) {
            throw xmlValidationException;
        } catch (Exception exception) {
            throw new XmlValidationException(
                    null,
                    "Failed to parse GUI XML: " + exception.getMessage(),
                    "Ensure the file is well‐formed and matches the schema"
            );
        }
    }

    /** Expose definitions for give‐actions. */
    public static ItemBuilder getDefinition(String id) {
        return definitions.get(id);
    }

    /** Build an ItemBuilder from <item-definition> or inline <component>. */
    private static ItemBuilder parseItemBuilder(Element element, JavaPlugin plugin) {
        ItemBuilder builder;

        // 1) raw NBT JSON
        NodeList nbt = element.getElementsByTagName("nbt-json");
        if (nbt.getLength() > 0) {
            String json = nbt.item(0).getTextContent().trim();
            CompoundData tag = (CompoundData) NBTJSON.readFromJsonString(json);
            builder = ItemBuilder.of(tag);
        }
        // 2) skull
        else if (element.hasAttribute("skull-owner")) {
            builder = ItemBuilder.playerSkull(element.getAttribute("skull-owner"));
        }
        // 3) material & amount
        else {
            String rawMaterial = XmlUtils.requireAttribute(element, "material");
            var material = XmlUtils.parseEnum(Material.class, rawMaterial, element, "Use a valid Material name");
            int amount = element.hasAttribute("amount") ? XmlUtils.parseIntAttribute(element, "amount", 1, 64, "1–64") : 1;
            builder = ItemBuilder.of(material, amount);
        }

        // name
        if (element.hasAttribute("name")) builder.withName(Colorize.translateBungeeHex(element.getAttribute("name")));

        // lore
        NodeList loreElement = element.getElementsByTagName("lore");
        if (loreElement.getLength() > 0) {
            var lines = new ArrayList<String>();
            Element lore = (Element) loreElement.item(0);
            NodeList lineElement = lore.getElementsByTagName("line");

            for (int i = 0; i < lineElement.getLength(); i++) {
                lines.add(Colorize.translateBungeeHex(lineElement.item(i).getTextContent().trim()));
            }
            builder.withLore(lines);
        }

        // unbreakable
        if ("true".equalsIgnoreCase(element.getAttribute("unbreakable"))) builder.setUnbreakable(true);
        

        // flags
        NodeList flagGroups = element.getElementsByTagName("flags");
        if (flagGroups.getLength() > 0) {
            Element flagElement = (Element) flagGroups.item(0);
            NodeList flag = flagElement.getElementsByTagName("flag");
            for (int i = 0; i < flag.getLength(); i++) {
                var itemFlag = XmlUtils.parseEnum(
                        ItemFlag.class,
                        flag.item(i).getTextContent().trim(),
                        (Element) flag.item(i),
                        "Use a valid ItemFlag"
                );
                builder.withFlag(itemFlag);
            }
        }

        // enchants
        NodeList enchantments = element.getElementsByTagName("enchant");
        for (int i = 0; i < enchantments.getLength(); i++) {
            Element enchantElement = (Element) enchantments.item(i);
            String raw = enchantElement.getTextContent().trim();               // e.g. "SHARPNESS:5"
            String[] parts = raw.split(":", 2);
            Enchantment enchantment = XmlUtils.parseEnchantment(enchantElement, parts[0], "Use a valid Enchantment key, e.g. SHARPNESS");
            int level = Integer.parseInt(parts[1]);

            builder.withEnchant(enchantment, level);
        }

        // attributes
        NodeList attributeElement = element.getElementsByTagName("attribute");
        for (int i = 0; i < attributeElement.getLength(); i++) {
            Element attrElement = (Element) attributeElement.item(i);

            String rawAttr = XmlUtils.requireAttribute(attrElement, "attribute");
            Attribute attribute = XmlUtils.parseAttribute(attrElement, rawAttr, "Use a valid Attribute key, e.g. GENERIC_ATTACK_DAMAGE");

            double amountVal = Double.parseDouble(XmlUtils.requireAttribute(attrElement, "amount"));
            Operation op = XmlUtils.parseEnum(
                    Operation.class,
                    XmlUtils.requireAttribute(attrElement, "operation"),
                    attrElement,
                    "Use ADD_NUMBER, MULTIPLY_SCALAR, etc."
            );

            String rawSlot = XmlUtils.requireAttribute(attrElement, "slot");
            EquipmentSlotGroup slotGroup = XmlUtils.parseSlotGroup(
                    attrElement,
                    rawSlot,
                    "Use: any, mainhand, offhand, hand, feet, legs, chest, head, armor, saddle"
            );

            NamespacedKey modKey = new NamespacedKey(
                    plugin,
                    rawAttr.toLowerCase() + "_" + rawSlot.toLowerCase()
            );
            AttributeModifier modifier = new AttributeModifier(
                    modKey,
                    amountVal,
                    op,
                    slotGroup
            );

            builder.handleMeta(ItemMeta.class, meta -> {
                meta.addAttributeModifier(attribute, modifier);
                return meta;
            });
        }

        // persistent-data
        NodeList pdcs = element.getElementsByTagName("persistent-data");
        for (int i = 0; i < pdcs.getLength(); i++) {
            Element pd = (Element) pdcs.item(i);
            String key = XmlUtils.requireAttribute(pd, "key");
            String type = XmlUtils.requireAttribute(pd, "type");
            String val = pd.getTextContent().trim();
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);

            builder.handleMeta(ItemMeta.class, meta -> {
                var c = meta.getPersistentDataContainer();
                switch (type.toUpperCase()) {
                    // TODO: Add the other PDC types (at least the common ones)
                    case "STRING" -> c.set(namespacedKey, PersistentDataType.STRING, val);
                    case "INT" -> c.set(namespacedKey, PersistentDataType.INTEGER, Integer.parseInt(val));

                    default -> throw new XmlValidationException(pd, "Unsupported PDC type='" + type + "'", "Use STRING or INT");
                }
                return meta;
            });
        }

        return builder;
    }
}

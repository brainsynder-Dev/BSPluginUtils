package org.bsdevelopment.pluginutils.gui.loader;

import org.bsdevelopment.nbt.JsonToNBT;
import org.bsdevelopment.nbt.StorageTagCompound;
import org.bsdevelopment.nbt.other.NBTException;
import org.bsdevelopment.pluginutils.gui.ActionRegistry;
import org.bsdevelopment.pluginutils.gui.CustomGui;
import org.bsdevelopment.pluginutils.gui.GuiAction;
import org.bsdevelopment.pluginutils.inventory.ItemBuilder;
import org.bsdevelopment.pluginutils.item.ItemRegistry;
import org.bsdevelopment.pluginutils.text.Colorize;
import org.bsdevelopment.pluginutils.xml.XmlUtils;
import org.bsdevelopment.pluginutils.xml.XmlValidationException;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
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
            Document doc = XmlUtils.parseDocument(xmlFile);
            Element guiElement = doc.getDocumentElement();
            XmlUtils.requireTag(guiElement, "gui");

            String title = Colorize.translateBungeeHex(XmlUtils.requireAttr(guiElement, "title", "Add title=\"...\" to <gui>"));

            Inventory inv;
            if (guiElement.hasAttribute("inventory-type")) {
                String typeRaw = XmlUtils.requireAttr(guiElement, "inventory-type", "Use a valid InventoryType, e.g. CHEST, DROPPER, WORKBENCH");
                InventoryType invType = XmlUtils.parseEnum(InventoryType.class, typeRaw, guiElement,
                        "Use a valid InventoryType, e.g. CHEST, DROPPER, WORKBENCH");
                inv = Bukkit.createInventory(null, invType, title);
            } else {
                int rows = XmlUtils.parseIntAttr(guiElement, "rows", 1, 6, "Rows must be between 1 and 6");
                inv = Bukkit.createInventory(null, rows * 9, title);
            }

            // item definitions
            definitions.clear();
            NodeList itemDefinitions = guiElement.getElementsByTagName("item-definition");
            for (int i = 0; i < itemDefinitions.getLength(); i++) {
                Element def = (Element) itemDefinitions.item(i);
                String id = XmlUtils.requireAttr(def, "id", "Add id=\"...\" to <item-definition>");
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

                int slot = XmlUtils.parseIntAttr(component, "slot", 0, inv.getSize() - 1,
                        "slot must be between 0 and " + (inv.getSize() - 1));

                ItemBuilder builder;
                if (component.hasAttribute("item-id")) {
                    String ref = component.getAttribute("item-id").trim();

                    // 1) Namespaced lookup first (external registry)
                    if (ref.contains(":")) {
                        org.bukkit.NamespacedKey k = org.bukkit.NamespacedKey.fromString(ref);
                        builder = ItemRegistry.get(k).orElse(null);
                        if (builder == null) {
                            throw new XmlValidationException(component,
                                    "Unknown external item-id='" + ref + "'",
                                    "Ensure you registered the item via ItemRegistry.registerFromFile(...)");
                        }
                    } else {
                        // 2) Fall back to local <definitions>
                        builder = definitions.get(ref);
                        if (builder == null) {
                            throw new XmlValidationException(component,
                                    "Unknown item-id='" + ref + "'",
                                    "Declare <item-definition id=\"" + ref + "\"/> or reference a namespaced key 'ns:id'");
                        }
                    }
                } else {
                    builder = parseItemBuilder(component, plugin); // inline
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
        } catch (IOException ioException) {
            throw new XmlValidationException(null, "Failed to read GUI XML file: " + ioException.getMessage(), "Ensure the file exists and is readable");
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
    private static ItemBuilder parseItemBuilder(Element element, JavaPlugin plugin) throws NBTException {
        ItemBuilder builder;

        // 1) raw NBT JSON
        NodeList nbt = element.getElementsByTagName("nbt-json");
        if (nbt.getLength() > 0) {
            String json = nbt.item(0).getTextContent().trim();
            StorageTagCompound tag = JsonToNBT.getTagFromJson(json);
            builder = ItemBuilder.of(tag);
        }
        // 2) skull
        else if (element.hasAttribute("skull-texture")) {
            builder = ItemBuilder.playerSkull(XmlUtils.parseTextureUrl(element, "skull-texture",
                    "Use texture URL like, e.g. http://textures.minecraft.net/texture/5eedfaf69ff40304faad7ea4dbf10ed8a7a49245108199f222ee9d77b45a2f6d").toString());
        }
        // 3) material & amount
        else {
            String rawMaterial = XmlUtils.requireAttr(element, "material", "Add material=\"MATERIAL_NAME\" to this element");
            var material = XmlUtils.parseEnum(Material.class, rawMaterial, element, "Use a valid Material name");
            int amount = element.hasAttribute("amount") ? XmlUtils.parseIntAttr(element, "amount", 1, 64, "1–64") : 1;
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
            Enchantment enchantment = XmlUtils.lookupEnchantment(enchantElement, parts[0], "Use a valid Enchantment key, e.g. SHARPNESS");
            int level = Integer.parseInt(parts[1]);

            builder.withEnchant(enchantment, level);
        }

        // attributes
        NodeList attributeElement = element.getElementsByTagName("attribute");
        for (int i = 0; i < attributeElement.getLength(); i++) {
            Element attrElement = (Element) attributeElement.item(i);

            String rawAttr = XmlUtils.requireAttr(attrElement, "attribute", "Add attribute=\"minecraft:key\" to <attribute>");
            Attribute attribute = XmlUtils.lookupAttribute(attrElement, rawAttr, "Use a valid Attribute key, e.g. GENERIC_ATTACK_DAMAGE");

            double amountVal = Double.parseDouble(XmlUtils.requireAttr(attrElement, "amount", "Add amount=\"...\" to <attribute>"));
            Operation op = XmlUtils.parseEnum(
                    Operation.class,
                    XmlUtils.requireAttr(attrElement, "operation", "Use ADD_NUMBER, MULTIPLY_SCALAR, etc."),
                    attrElement,
                    "Use ADD_NUMBER, MULTIPLY_SCALAR, etc."
            );

            String rawSlot = XmlUtils.requireAttr(attrElement, "slot", "Add slot=\"hand|armor|...\" to <attribute>");
            EquipmentSlotGroup slotGroup = XmlUtils.lookupSlotGroup(
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
            String key = XmlUtils.requireAttr(pd, "key", "Add key=\"namespace:id\" to <persistent-data>");
            String type = XmlUtils.requireAttr(pd, "type", "Add type=\"STRING|INT|...\" to <persistent-data>");
            String val = pd.getTextContent().trim();
            NamespacedKey namespacedKey = NamespacedKey.fromString(key);

            builder.handleMeta(ItemMeta.class, meta -> {
                var container = meta.getPersistentDataContainer();
                switch (type.toUpperCase()) {
                    // TODO: Add the other PDC types (at least the common ones)
                    case "STRING" -> container.set(namespacedKey, PersistentDataType.STRING, val);
                    case "BOOLEAN" -> container.set(namespacedKey, PersistentDataType.BOOLEAN, Boolean.parseBoolean(val));
                    case "INT" -> container.set(namespacedKey, PersistentDataType.INTEGER, Integer.parseInt(val));
                    case "BYTE" -> container.set(namespacedKey, PersistentDataType.BYTE, Byte.parseByte(val));
                    case "DOUBLE" -> container.set(namespacedKey, PersistentDataType.DOUBLE, Double.parseDouble(val));
                    case "FLOAT" -> container.set(namespacedKey, PersistentDataType.FLOAT, Float.parseFloat(val));
                    case "LONG" -> container.set(namespacedKey, PersistentDataType.LONG, Long.parseLong(val));
                    case "SHORT" -> container.set(namespacedKey, PersistentDataType.SHORT, Short.parseShort(val));

                    default -> throw new XmlValidationException(pd, "Unsupported PDC type='" + type + "'", "Use STRING, INT, BOOLEAN, DOUBLE, FLOAT, LONG, SHORT, or BYTE");
                }
                return meta;
            });
        }

        return builder;
    }
}

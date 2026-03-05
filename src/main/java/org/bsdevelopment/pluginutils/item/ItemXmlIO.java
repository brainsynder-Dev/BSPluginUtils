package org.bsdevelopment.pluginutils.item;

import org.bsdevelopment.nbt.JsonToNBT;
import org.bsdevelopment.nbt.StorageTagCompound;
import org.bsdevelopment.pluginutils.inventory.ItemBuilder;
import org.bsdevelopment.pluginutils.text.Colorize;
import org.bsdevelopment.pluginutils.xml.XmlUtils;
import org.bsdevelopment.pluginutils.xml.XmlValidationException;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerProfile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * XML ↔ {@link ItemBuilder} codec.
 *
 * <p>Supported XML features: {@code material}, {@code amount}, {@code name}, {@code unbreakable},
 * {@code skull-texture}, {@code <lore>}, {@code <flags>}, {@code <enchants>}, {@code <attributes>},
 * {@code <persistent-data-list>}, and raw {@code <nbt-json>} (takes priority over all others).
 *
 * <p>The root element may be {@code <item>}, {@code <item-definition>}, or any element carrying
 * the item attributes and children.
 */
public final class ItemXmlIO {

    public static ItemBuilder read(File file, JavaPlugin plugin) {
        try (InputStream inputStream = new FileInputStream(file)) {
            return read(inputStream, plugin);
        } catch (Exception e) {
            throw new XmlValidationException(null, "Failed to read '" + file + "': " + e.getMessage(), "Ensure the XML file exists and is readable");
        }
    }

    public static ItemBuilder read(InputStream inputStream, JavaPlugin plugin) {
        try {
            Document document = XmlUtils.parseDocument(inputStream);
            return read(document.getDocumentElement(), plugin);
        } catch (XmlValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new XmlValidationException(null, "Failed to parse XML: " + e.getMessage(), "Check for well-formed XML");
        }
    }

    public static ItemIdEntry readIdEntry(File file, JavaPlugin plugin) {
        try (InputStream inputStream = new FileInputStream(file)) {
            Document document = XmlUtils.parseDocument(inputStream);
            Element root = document.getDocumentElement();
            return new ItemIdEntry(readItemId(root, plugin), read(root, plugin));
        } catch (XmlValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new XmlValidationException(null, "Failed to read item entry: " + e.getMessage(), "Ensure the file has <item ... item-id=\"namespace:id\">");
        }
    }

    public static ItemBuilder read(Element element, JavaPlugin plugin) {
        ItemBuilder builder;

        NodeList nbtNodes = element.getElementsByTagName("nbt-json");
        if (nbtNodes.getLength() > 0) {
            String json = nbtNodes.item(0).getTextContent().trim();
            builder = ItemBuilder.of(safeCompound((Element) nbtNodes.item(0), json));
        } else if (element.hasAttribute("skull-texture")) {
            String texture = element.getAttribute("skull-texture").trim();
            require(!texture.isBlank(), element, "Empty skull-texture", "Provide a valid texture URL");
            builder = ItemBuilder.playerSkull(texture);
        } else {
            String rawMaterial = XmlUtils.requireAttr(element, "material", "Provide a Material, e.g. DIAMOND_SWORD");
            Material material;
            try {
                material = Material.valueOf(rawMaterial.toUpperCase(Locale.ROOT));
            } catch (Exception e) {
                throw new XmlValidationException(element, "Invalid Material: " + rawMaterial, "Use a valid Bukkit Material");
            }
            int amount = element.hasAttribute("amount") ? XmlUtils.parseIntAttr(element, "amount", 1, 64, "Amount must be 1–64") : 1;
            builder = ItemBuilder.of(material, amount);
        }

        if (element.hasAttribute("name")) builder.withName(colorize(element.getAttribute("name")));

        if ("true".equalsIgnoreCase(element.getAttribute("unbreakable"))) builder.setUnbreakable(true);

        NodeList loreElements = element.getElementsByTagName("lore");
        if (loreElements.getLength() > 0) {
            NodeList lineNodes = ((Element) loreElements.item(0)).getElementsByTagName("line");
            List<String> lines = new ArrayList<>(lineNodes.getLength());
            for (int i = 0; i < lineNodes.getLength(); i++) {
                lines.add(colorize(lineNodes.item(i).getTextContent().trim()));
            }
            builder.withLore(lines);
        }

        NodeList flagsGroups = element.getElementsByTagName("flags");
        if (flagsGroups.getLength() > 0) {
            NodeList flagNodes = ((Element) flagsGroups.item(0)).getElementsByTagName("flag");
            for (int i = 0; i < flagNodes.getLength(); i++) {
                String rawFlag = flagNodes.item(i).getTextContent().trim();
                try {
                    builder.withFlag(ItemFlag.valueOf(rawFlag.toUpperCase(Locale.ROOT)));
                } catch (Exception e) {
                    throw new XmlValidationException((Element) flagNodes.item(i), "Invalid ItemFlag: " + rawFlag, "Use a valid ItemFlag");
                }
            }
        }

        NodeList enchantElements = element.getElementsByTagName("enchant");
        for (int i = 0; i < enchantElements.getLength(); i++) {
            Element enchantElement = (Element) enchantElements.item(i);
            String raw = enchantElement.getTextContent().trim();
            int colon = raw.lastIndexOf(':');
            require(colon > 0, enchantElement, "Invalid enchant format: '" + raw + "'", "Use NAME:LEVEL (e.g. SHARPNESS:5)");
            Enchantment enchantment = XmlUtils.lookupEnchantment(enchantElement, raw.substring(0, colon), "Use a valid enchantment key or name");
            int level;
            try {
                level = Integer.parseInt(raw.substring(colon + 1));
            } catch (NumberFormatException e) {
                throw new XmlValidationException(enchantElement, "Invalid enchant level: " + raw.substring(colon + 1), "Use a positive integer");
            }
            builder.withEnchant(enchantment, level);
        }

        NodeList attributeElements = element.getElementsByTagName("attribute");
        for (int i = 0; i < attributeElements.getLength(); i++) {
            Element attrElement = (Element) attributeElements.item(i);
            Attribute attribute = XmlUtils.lookupAttribute(attrElement, XmlUtils.requireAttr(attrElement, "attribute", "Provide an attribute key, e.g. minecraft:generic_attack_speed"), "Use a valid Attribute key");
            double amount = Double.parseDouble(XmlUtils.requireAttr(attrElement, "amount", "Provide a numeric amount"));
            Operation operation = XmlUtils.parseEnum(Operation.class, XmlUtils.requireAttr(attrElement, "operation", "Provide operation"), attrElement, "Use ADD_NUMBER, MULTIPLY_SCALAR, ADD_SCALAR");
            EquipmentSlotGroup group = XmlUtils.lookupSlotGroup(attrElement, XmlUtils.requireAttr(attrElement, "slot", "Provide slot group: hand, head, armor, etc."), "Use: any, mainhand, offhand, hand, feet, legs, chest, head, armor, saddle");
            NamespacedKey key = new NamespacedKey(plugin, (attribute.getKey().getKey() + "_" + group).toLowerCase(Locale.ROOT));
            AttributeModifier modifier = new AttributeModifier(key, amount, operation, group);
            builder.handleMeta(ItemMeta.class, meta -> {
                meta.addAttributeModifier(attribute, modifier);
                return meta;
            });
        }

        NodeList pdcElements = element.getElementsByTagName("persistent-data");
        for (int i = 0; i < pdcElements.getLength(); i++) {
            Element pdcElement = (Element) pdcElements.item(i);
            NamespacedKey key = NamespacedKey.fromString(XmlUtils.requireAttr(pdcElement, "key", "Provide a namespaced key"));
            String type = XmlUtils.requireAttr(pdcElement, "type", "Provide a supported PDC type");
            String value = pdcElement.getTextContent().trim();
            builder.handleMeta(ItemMeta.class, meta -> {
                var pdc = meta.getPersistentDataContainer();
                switch (type.toUpperCase(Locale.ROOT)) {
                    case "STRING"  -> pdc.set(key, PersistentDataType.STRING,  value);
                    case "BOOLEAN" -> pdc.set(key, PersistentDataType.BOOLEAN, Boolean.parseBoolean(value));
                    case "INT"     -> pdc.set(key, PersistentDataType.INTEGER,  Integer.parseInt(value));
                    case "BYTE"    -> pdc.set(key, PersistentDataType.BYTE,    Byte.parseByte(value));
                    case "DOUBLE"  -> pdc.set(key, PersistentDataType.DOUBLE,  Double.parseDouble(value));
                    case "FLOAT"   -> pdc.set(key, PersistentDataType.FLOAT,   Float.parseFloat(value));
                    case "LONG"    -> pdc.set(key, PersistentDataType.LONG,    Long.parseLong(value));
                    case "SHORT"   -> pdc.set(key, PersistentDataType.SHORT,   Short.parseShort(value));
                    default -> throw new XmlValidationException(pdcElement, "Unsupported PDC type='" + type + "'", "Use STRING, BOOLEAN, INT, BYTE, DOUBLE, FLOAT, LONG, SHORT");
                }
                return meta;
            });
        }

        return builder;
    }

    public static void write(File file, ItemBuilder builder) {
        try (OutputStream out = new FileOutputStream(file)) {
            write(out, builder);
        } catch (Exception e) {
            throw new XmlValidationException(null, "Failed to write '" + file + "': " + e.getMessage(), "Ensure the path is writable");
        }
    }

    public static void write(OutputStream outputStream, ItemBuilder builder) {
        try {
            Document document = XmlUtils.newDocument();
            Element root = document.createElement("item");
            writeInto(document, root, builder);
            document.appendChild(root);
            XmlUtils.writeDocument(document, outputStream);
        } catch (XmlValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new XmlValidationException(null, "Failed to serialize item: " + e.getMessage(), "Verify the ItemBuilder/ItemMeta is valid");
        }
    }

    /**
     * Populate an existing element with the item's attributes and children.
     * Used by GUI writers that embed item definitions inside a larger document.
     */
    public static void writeInto(Document document, Element target, ItemBuilder builder) {
        ItemStack item = builder.build();
        ItemMeta meta = item.getItemMeta();

        target.setAttribute("material", item.getType().name());
        target.setAttribute("amount", Integer.toString(item.getAmount()));
        if (meta.hasDisplayName()) target.setAttribute("name", meta.getDisplayName());
        if (meta.isUnbreakable()) target.setAttribute("unbreakable", "true");

        if (meta instanceof SkullMeta skull && skull.getOwnerProfile() != null) {
            PlayerProfile profile = skull.getOwnerProfile();
            var skin = profile.getTextures() != null ? profile.getTextures().getSkin() : null;
            if (skin != null) target.setAttribute("skull-texture", skin.toString());
        }

        if (meta.hasLore()) {
            Element loreElement = document.createElement("lore");
            for (String line : meta.getLore()) {
                Element lineElement = document.createElement("line");
                lineElement.setTextContent(line);
                loreElement.appendChild(lineElement);
            }
            target.appendChild(loreElement);
        }

        if (!meta.getItemFlags().isEmpty()) {
            Element flagsElement = document.createElement("flags");
            for (ItemFlag flag : meta.getItemFlags()) {
                Element flagElement = document.createElement("flag");
                flagElement.setTextContent(flag.name());
                flagsElement.appendChild(flagElement);
            }
            target.appendChild(flagsElement);
        }

        if (!meta.getEnchants().isEmpty()) {
            Element enchantsElement = document.createElement("enchants");
            meta.getEnchants().forEach((enchantment, level) -> {
                Element enchantElement = document.createElement("enchant");
                enchantElement.setTextContent(enchantment.getKey().getKey().toUpperCase(Locale.ROOT) + ":" + level);
                enchantsElement.appendChild(enchantElement);
            });
            target.appendChild(enchantsElement);
        }

        var attributeMap = meta.getAttributeModifiers();
        if (attributeMap != null && !attributeMap.isEmpty()) {
            Element attributesElement = document.createElement("attributes");
            attributeMap.asMap().forEach((attribute, modifiers) -> {
                for (AttributeModifier modifier : modifiers) {
                    Element attributeElement = document.createElement("attribute");
                    attributeElement.setAttribute("attribute", attribute.getKey().toString());
                    attributeElement.setAttribute("amount", Double.toString(modifier.getAmount()));
                    attributeElement.setAttribute("operation", modifier.getOperation().name());
                    attributeElement.setAttribute("slot", modifier.getSlot().toString());
                    attributesElement.appendChild(attributeElement);
                }
            });
            target.appendChild(attributesElement);
        }

        Element pdcList = buildPdcList(document, meta);
        if (pdcList != null) target.appendChild(pdcList);

        try {
            StorageTagCompound tag = builder.toTag();
            Element nbtElement = document.createElement("nbt-json");
            nbtElement.setTextContent(tag.toString());
            target.appendChild(nbtElement);
        } catch (Exception ignored) {}
    }

    private static String readItemId(Element root, JavaPlugin plugin) {
        if (root.hasAttribute("item-id")) {
            String value = root.getAttribute("item-id").trim();
            if (!value.contains(":")) throw new XmlValidationException(root, "Invalid item-id='" + value + "'", "Use namespace:id (e.g., myplugin:test_item)");
            return value.toLowerCase(Locale.ROOT);
        }

        String id = root.getAttribute("id").trim();
        String namespace = root.getAttribute("namespace").trim();

        if (!id.isEmpty() && !namespace.isEmpty()) return (namespace + ":" + id).toLowerCase(Locale.ROOT);
        if (!id.isEmpty()) return (plugin.getName().toLowerCase(Locale.ROOT) + ":" + id.toLowerCase(Locale.ROOT));

        throw new XmlValidationException(root, "Missing item-id / id+namespace", "Add item-id=\"ns:id\" (recommended)");
    }

    private static Element buildPdcList(Document document, ItemMeta meta) {
        var pdc = meta.getPersistentDataContainer();
        if (pdc.getKeys().isEmpty()) return null;

        Element listElement = document.createElement("persistent-data-list");
        for (NamespacedKey key : pdc.getKeys()) {
            String type;
            String value;

            if      (pdc.has(key, PersistentDataType.STRING))  { type = "STRING";  value = pdc.get(key, PersistentDataType.STRING); }
            else if (pdc.has(key, PersistentDataType.BOOLEAN)) { type = "BOOLEAN"; value = Boolean.toString(pdc.get(key, PersistentDataType.BOOLEAN)); }
            else if (pdc.has(key, PersistentDataType.INTEGER)) { type = "INT";     value = Integer.toString(pdc.get(key, PersistentDataType.INTEGER)); }
            else if (pdc.has(key, PersistentDataType.BYTE))    { type = "BYTE";    value = Byte.toString(pdc.get(key, PersistentDataType.BYTE)); }
            else if (pdc.has(key, PersistentDataType.DOUBLE))  { type = "DOUBLE";  value = Double.toString(pdc.get(key, PersistentDataType.DOUBLE)); }
            else if (pdc.has(key, PersistentDataType.FLOAT))   { type = "FLOAT";   value = Float.toString(pdc.get(key, PersistentDataType.FLOAT)); }
            else if (pdc.has(key, PersistentDataType.LONG))    { type = "LONG";    value = Long.toString(pdc.get(key, PersistentDataType.LONG)); }
            else if (pdc.has(key, PersistentDataType.SHORT))   { type = "SHORT";   value = Short.toString(pdc.get(key, PersistentDataType.SHORT)); }
            else continue;

            Element entry = document.createElement("persistent-data");
            entry.setAttribute("key", key.getKey());
            entry.setAttribute("type", type);
            entry.setTextContent(value);
            listElement.appendChild(entry);
        }
        return listElement;
    }

    private static StorageTagCompound safeCompound(Element element, String json) {
        try {
            return JsonToNBT.getTagFromJson(json);
        } catch (Exception e) {
            throw new XmlValidationException(element, "Invalid NBT JSON", "Ensure valid CompoundData JSON");
        }
    }

    private static void require(boolean condition, Element element, String message, String hint) {
        if (!condition) throw new XmlValidationException(element, message, hint);
    }

    private static String colorize(String string) {
        try {
            return Colorize.translateBungeeHex(string);
        } catch (Throwable t) {
            return ChatColor.translateAlternateColorCodes('&', string);
        }
    }

    public record ItemIdEntry(String itemId, ItemBuilder builder) {}
}

package org.bsdevelopment.pluginutils.item;

import org.bsdevelopment.nbt.JsonToNBT;
import org.bsdevelopment.nbt.StorageTagCompound;
import org.bsdevelopment.pluginutils.gui.parser.XmlValidationException;
import org.bsdevelopment.pluginutils.inventory.ItemBuilder;
import org.bsdevelopment.pluginutils.text.Colorize;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
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

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Stand-alone XML &lt;-&gt; Item codec.
 *
 * <h3>Read</h3>
 * <pre>{@code
 * ItemBuilder builder = ItemXmlIO.read(new File("item.xml"), plugin);
 * ItemStack stack = builder.build();
 * }</pre>
 *
 * <h3>Write</h3>
 * <pre>{@code
 * ItemXmlIO.write(new File("item.xml"), ItemBuilder.of(Material.DIAMOND).withName("&bShiny"));
 * }</pre>
 *
 * <p>Supported features:
 * <ul>
 *   <li>&lt;nbt-json&gt; raw NBT (wins over everything)</li>
 *   <li>skull-texture="http://textures.minecraft.net/texture/..."</li>
 *   <li>material, amount, name (&amp; color codes), unbreakable</li>
 *   <li>&lt;lore&gt;&lt;line&gt;...&lt;/line&gt;&lt;/lore&gt;</li>
 *   <li>&lt;flags&gt;&lt;flag&gt;...&lt;/flag&gt;&lt;/flags&gt;</li>
 *   <li>&lt;enchants&gt;&lt;enchant&gt;SHARPNESS:5&lt;/enchant&gt; …</li>
 *   <li>&lt;attributes&gt;&lt;attribute attribute="minecraft:generic_attack_damage" amount="8" operation="ADD_NUMBER" slot="hand"/&gt;</li>
 *   <li>&lt;persistent-data-list&gt;&lt;persistent-data key="..." type="STRING|BOOLEAN|INT|BYTE|DOUBLE|FLOAT|LONG|SHORT"&gt;value&lt;/persistent-data&gt;…</li>
 * </ul>
 *
 * <p>Root element can be &lt;item&gt;, &lt;item-definition&gt; or any element containing those attributes/children.</p>
 */
public final class ItemXmlIO {

    /**
     * Read an {@link ItemBuilder} from an XML file.
     * <p>
     * Root element may be {@code <item>} or {@code <item-definition>}.
     *
     * @param file   XML file to read from
     * @param plugin owning plugin used for default namespaces and attribute keys
     *
     * @return builder created from the XML contents
     *
     * @throws XmlValidationException if the file cannot be read or parsed
     */
    public static ItemBuilder read(File file, JavaPlugin plugin) {
        try (InputStream inputStream = new FileInputStream(file)) {
            return read(inputStream, plugin);
        } catch (Exception exception) {
            throw new XmlValidationException(null, "Failed to read '" + file + "': " + exception.getMessage(), "Ensure the XML file exists and is readable");
        }
    }

    /**
     * Read an {@link ItemBuilder} from an input stream.
     * <p>
     * The stream is not closed by this method.
     *
     * @param inputStream XML source stream
     * @param plugin      owning plugin used for default namespaces and attribute keys
     *
     * @return builder created from the XML contents
     *
     * @throws XmlValidationException if the XML cannot be parsed
     */
    public static ItemBuilder read(InputStream inputStream, JavaPlugin plugin) {
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
            document.getDocumentElement().normalize();
            return read(document.getDocumentElement(), plugin);
        } catch (Exception exception) {
            throw new XmlValidationException(null, "Failed to parse XML: " + exception.getMessage(), "Check for well-formed XML");
        }
    }

    /**
     * Read an item entry with an {@code item-id} plus its {@link ItemBuilder}.
     *
     * @param file   XML file to read from
     * @param plugin owning plugin used for default namespaces and attribute keys
     *
     * @return parsed {@link ItemIdEntry} containing item id and builder
     *
     * @throws XmlValidationException if the file cannot be read or parsed
     */
    public static ItemIdEntry readIdEntry(File file, JavaPlugin plugin) {
        try (InputStream inputStream = new FileInputStream(file)) {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
            document.getDocumentElement().normalize();
            Element rootElement = document.getDocumentElement();
            String itemId = readItemId(rootElement, plugin);
            ItemBuilder builder = read(rootElement, plugin);
            return new ItemIdEntry(itemId, builder);
        } catch (Exception exception) {
            throw new XmlValidationException(null, "Failed to read item entry: " + exception.getMessage(), "Ensure the file has <item ... item-id=\"namespace:id\">");
        }
    }

    /**
     * Accepts {@code item-id="ns:id"} OR {@code id="id"} + {@code namespace="ns"}.
     * If only {@code id} is present, defaults to the plugin namespace.
     */
    private static String readItemId(Element rootElement, JavaPlugin plugin) {
        if (rootElement.hasAttribute("item-id")) {
            String string = rootElement.getAttribute("item-id").trim();
            if (!string.contains(":")) throw new XmlValidationException(rootElement, "Invalid item-id='" + string + "'", "Use namespace:id (e.g., myplugin:test_item)");

            return string.toLowerCase(Locale.ROOT);
        }

        String id = rootElement.getAttribute("id").trim();
        String namespace = rootElement.getAttribute("namespace").trim();

        if (!id.isEmpty() && !namespace.isEmpty()) return (namespace + ":" + id).toLowerCase(Locale.ROOT);
        if (!id.isEmpty()) return (plugin.getName().toLowerCase(Locale.ROOT) + ":" + id.toLowerCase(Locale.ROOT));

        throw new XmlValidationException(rootElement, "Missing item-id / id+namespace", "Add item-id=\"ns:id\" (recommended)");
    }

    /**
     * Read an {@link ItemBuilder} from a DOM element that matches the item schema.
     *
     * @param element item root element
     * @param plugin  owning plugin used for default namespaces and attribute keys
     *
     * @return builder created from the element
     */
    public static ItemBuilder read(Element element, JavaPlugin plugin) {
        ItemBuilder builder;

        // 1) nbt-json wins
        NodeList nbtNodes = element.getElementsByTagName("nbt-json");
        if (nbtNodes.getLength() > 0) {
            String json = nbtNodes.item(0).getTextContent().trim();
            builder = ItemBuilder.of(safeCompound((Element) nbtNodes.item(0), json));
        }
        // 2) skull-texture
        else if (element.hasAttribute("skull-texture")) {
            String texture = element.getAttribute("skull-texture").trim();
            require(!texture.isBlank(), element, "Empty skull-texture", "Provide a valid texture URL");
            builder = ItemBuilder.playerSkull(texture);
        }
        // 3) material/amount
        else {
            String rawMaterialName = requireAttr(element, "material", "Provide a Material, e.g. DIAMOND_SWORD");
            Material material;
            try {
                material = Material.valueOf(rawMaterialName.toUpperCase(Locale.ROOT));
            } catch (Exception exception) {
                throw new XmlValidationException(element, "Invalid Material: " + rawMaterialName, "Use a valid Bukkit Material");
            }
            int amount = element.hasAttribute("amount") ? parseIntAttr(element, "amount", 1, 64, "Amount must be 1–64") : 1;
            builder = ItemBuilder.of(material, amount);
        }

        // name (& colors; fallback to simple ChatColor if Colorize is absent)
        if (element.hasAttribute("name")) builder.withName(colorize(element.getAttribute("name")));

        // lore
        NodeList loreElements = element.getElementsByTagName("lore");
        if (loreElements.getLength() > 0) {
            Element loreElement = (Element) loreElements.item(0);
            NodeList lineNodes = loreElement.getElementsByTagName("line");
            List<String> loreLines = new ArrayList<>(lineNodes.getLength());

            for (int index = 0; index < lineNodes.getLength(); index++) {
                loreLines.add(colorize(lineNodes.item(index).getTextContent().trim()));
            }

            builder.withLore(loreLines);
        }

        // unbreakable
        if ("true".equalsIgnoreCase(element.getAttribute("unbreakable"))) builder.setUnbreakable(true);

        // flags
        NodeList flagsGroups = element.getElementsByTagName("flags");
        if (flagsGroups.getLength() > 0) {
            Element flagsElement = (Element) flagsGroups.item(0);
            NodeList flagNodes = flagsElement.getElementsByTagName("flag");
            for (int index = 0; index < flagNodes.getLength(); index++) {
                String rawFlag = flagNodes.item(index).getTextContent().trim();
                try {
                    builder.withFlag(ItemFlag.valueOf(rawFlag.toUpperCase(Locale.ROOT)));
                } catch (Exception exception) {
                    throw new XmlValidationException((Element) flagNodes.item(index), "Invalid ItemFlag: " + rawFlag, "Use a valid ItemFlag");
                }
            }
        }

        // enchants (supports <enchant>SHARPNESS:5</enchant> and also minecraft:sharpness:5 → first part up to last ':')
        NodeList enchantElements = element.getElementsByTagName("enchant");
        for (int index = 0; index < enchantElements.getLength(); index++) {
            Element enchantElement = (Element) enchantElements.item(index);
            String rawEnchant = enchantElement.getTextContent().trim();
            int colonIndex = rawEnchant.lastIndexOf(':');
            require(colonIndex > 0, enchantElement, "Invalid enchant format: '" + rawEnchant + "'", "Use NAME:LEVEL (e.g. SHARPNESS:5)");
            String enchantType = rawEnchant.substring(0, colonIndex);
            String levelString = rawEnchant.substring(colonIndex + 1);
            Enchantment enchantment = lookupEnchantment(enchantElement, enchantType, "Use a valid enchantment key or name");
            int level;
            try {
                level = Integer.parseInt(levelString);
            } catch (NumberFormatException exception) {
                throw new XmlValidationException(enchantElement, "Invalid enchant level: " + levelString, "Use a positive integer");
            }
            builder.withEnchant(enchantment, level);
        }

        // attributes
        NodeList attributeElements = element.getElementsByTagName("attribute");
        for (int index = 0; index < attributeElements.getLength(); index++) {
            Element attributeElement = (Element) attributeElements.item(index);
            String rawAttributeKey = requireAttr(attributeElement, "attribute", "Provide an attribute key, e.g. minecraft:generic_attack_speed");
            Attribute attribute = lookupAttribute(attributeElement, rawAttributeKey, "Use a valid Attribute key");
            double amount = Double.parseDouble(requireAttr(attributeElement, "amount", "Provide a numeric amount"));
            Operation operation = parseEnum(Operation.class, requireAttr(attributeElement, "operation", "Provide operation"), attributeElement, "Use ADD_NUMBER, MULTIPLY_SCALAR, ADD_SCALAR");
            String rawGroup = requireAttr(attributeElement, "slot", "Provide slot group: hand, head, armor, etc.");
            EquipmentSlotGroup group = lookupSlotGroup(attributeElement, rawGroup, "Use: any, mainhand, offhand, hand, feet, legs, chest, head, armor, saddle");

            NamespacedKey key = new NamespacedKey(plugin, (attribute.getKey().getKey() + "_" + group.toString()).toLowerCase(Locale.ROOT));
            AttributeModifier modifier = new AttributeModifier(key, amount, operation, group);

            builder.handleMeta(ItemMeta.class, meta -> {
                meta.addAttributeModifier(attribute, modifier);
                return meta;
            });
        }

        // persistent-data
        NodeList persistentDataElements = element.getElementsByTagName("persistent-data");
        for (int index = 0; index < persistentDataElements.getLength(); index++) {
            Element persistentDataElement = (Element) persistentDataElements.item(index);
            String key = requireAttr(persistentDataElement, "key", "Provide a namespaced key (plugin:key or key)");
            String type = requireAttr(persistentDataElement, "type", "Provide a supported PDC type");
            String value = persistentDataElement.getTextContent().trim();
            NamespacedKey namespacedKey = NamespacedKey.fromString(key);
            builder.handleMeta(ItemMeta.class, meta -> {
                var dataContainer = meta.getPersistentDataContainer();
                switch (type.toUpperCase(Locale.ROOT)) {
                    case "STRING" -> dataContainer.set(namespacedKey, PersistentDataType.STRING, value);
                    case "BOOLEAN" -> dataContainer.set(namespacedKey, PersistentDataType.BOOLEAN, Boolean.parseBoolean(value));
                    case "INT" -> dataContainer.set(namespacedKey, PersistentDataType.INTEGER, Integer.parseInt(value));
                    case "BYTE" -> dataContainer.set(namespacedKey, PersistentDataType.BYTE, Byte.parseByte(value));
                    case "DOUBLE" -> dataContainer.set(namespacedKey, PersistentDataType.DOUBLE, Double.parseDouble(value));
                    case "FLOAT" -> dataContainer.set(namespacedKey, PersistentDataType.FLOAT, Float.parseFloat(value));
                    case "LONG" -> dataContainer.set(namespacedKey, PersistentDataType.LONG, Long.parseLong(value));
                    case "SHORT" -> dataContainer.set(namespacedKey, PersistentDataType.SHORT, Short.parseShort(value));
                    default -> throw new XmlValidationException(persistentDataElement, "Unsupported PDC type='" + type + "'", "Use STRING, BOOLEAN, INT, BYTE, DOUBLE, FLOAT, LONG, SHORT");
                }
                return meta;
            });
        }

        return builder;
    }

    /**
     * Write a single item to a file; root tag is {@code <item>}.
     *
     * @param file    output XML file
     * @param builder item to serialize
     *
     * @throws XmlValidationException if the item cannot be written
     */
    public static void write(File file, ItemBuilder builder) {
        try (OutputStream outputStream = new FileOutputStream(file)) {
            write(outputStream, builder);
        } catch (Exception exception) {
            throw new XmlValidationException(null, "Failed to write '" + file + "': " + exception.getMessage(), "Ensure the XML file exists and is readable");
        }
    }

    /**
     * Write a single item to an {@link OutputStream}; root tag is {@code <item>}.
     *
     * @param outputStream XML destination stream
     * @param builder      item to serialize
     *
     * @throws XmlValidationException if the item cannot be serialized
     */
    public static void write(OutputStream outputStream, ItemBuilder builder) {
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element rootElement = document.createElement("item");
            writeInto(document, rootElement, builder);
            document.appendChild(rootElement);

            var transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(new DOMSource(document), new StreamResult(outputStream));
        } catch (Exception exception) {
            throw new XmlValidationException(null, "Failed to serialize item: " + exception.getMessage(), "Verify the ItemBuilder/ItemMeta is valid");
        }
    }

    /**
     * Populate an existing element (e.g. {@code <item>} or {@code <item-definition>})
     * with the item's attributes and children.
     *
     * @param document owning document
     * @param target   element to populate
     * @param builder  source item
     */
    public static void writeInto(Document document, Element target, ItemBuilder builder) {
        ItemStack item = builder.build();
        ItemMeta meta = item.getItemMeta();

        target.setAttribute("material", item.getType().name());
        target.setAttribute("amount", Integer.toString(item.getAmount()));

        if (meta.hasDisplayName()) target.setAttribute("name", meta.getDisplayName());
        if (meta.isUnbreakable()) target.setAttribute("unbreakable", "true");


        // skull-texture (if present)
        if (meta instanceof SkullMeta skull && skull.getOwnerProfile() != null) {
            PlayerProfile profile = skull.getOwnerProfile();
            var skin = profile.getTextures() != null ? profile.getTextures().getSkin() : null;
            if (skin != null) {
                target.setAttribute("skull-texture", skin.toString());
            }
        }

        // lore
        if (meta.hasLore()) {
            Element loreElement = document.createElement("lore");
            for (String line : meta.getLore()) {
                Element lineElement = document.createElement("line");
                lineElement.setTextContent(line);
                loreElement.appendChild(lineElement);
            }
            target.appendChild(loreElement);
        }

        // flags
        if (!meta.getItemFlags().isEmpty()) {
            Element flagsElement = document.createElement("flags");
            for (ItemFlag flag : meta.getItemFlags()) {
                Element flagElement = document.createElement("flag");
                flagElement.setTextContent(flag.name());
                flagsElement.appendChild(flagElement);
            }
            target.appendChild(flagsElement);
        }

        // enchants -> write as text "SHARPNESS:5" to be broadly compatible
        if (!meta.getEnchants().isEmpty()) {
            Element enchantsElement = document.createElement("enchants");
            meta.getEnchants().forEach((enchantment, level) -> {
                Element enchantElement = document.createElement("enchant");
                String vanillaName = enchantment.getKey().getKey().toUpperCase(Locale.ROOT);
                enchantElement.setTextContent(vanillaName + ":" + level);
                enchantsElement.appendChild(enchantElement);
            });
            target.appendChild(enchantsElement);
        }

        // attributes
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

        // persistent-data
        Element persistentDataListElement = buildPdcList(document, meta);
        if (persistentDataListElement != null) {
            target.appendChild(persistentDataListElement);
        }

        // raw NBT JSON (best-effort)
        try {
            StorageTagCompound tag = builder.toTag();
            Element nbtElement = document.createElement("nbt-json");
            nbtElement.setTextContent(tag.toString());
            target.appendChild(nbtElement);
        } catch (Exception ignored) {
        }
    }

    private static StorageTagCompound safeCompound(Element element, String json) {
        try {
            return JsonToNBT.getTagFromJson(json);
        } catch (Exception exception) {
            throw new XmlValidationException(element, "Invalid NBT JSON", "Ensure valid CompoundData JSON");
        }
    }

    private static String requireAttr(Element element, String name, String hint) {
        String value = element.getAttribute(name);
        if (value == null || value.isBlank()) {
            throw new XmlValidationException(element, "Missing required attribute '" + name + "'", hint);
        }
        return value;
    }

    private static void require(boolean condition, Element element, String message, String hint) {
        if (!condition) {
            throw new XmlValidationException(element, message, hint);
        }
    }

    private static int parseIntAttr(Element element, String name, int min, int max, String hint) {
        String rawValue = requireAttr(element, name, hint);
        try {
            int value = Integer.parseInt(rawValue);
            if (value < min || value > max) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new XmlValidationException(element, "Invalid integer for '" + name + "': " + rawValue, hint);
        }
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> type, String raw, Element element, String hint) {
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            throw new XmlValidationException(element, "Invalid " + type.getSimpleName() + ": " + raw, hint);
        }
    }

    private static Enchantment lookupEnchantment(Element element, String raw, String hint) {
        NamespacedKey key = toKey(raw);
        Enchantment enchantment = Registry.ENCHANTMENT.get(key);
        if (enchantment == null) {
            throw new XmlValidationException(element, "Unknown Enchantment key: '" + raw + "'", hint);
        }
        return enchantment;
    }

    private static Attribute lookupAttribute(Element element, String raw, String hint) {
        NamespacedKey key = toKey(raw);
        Attribute attribute = Registry.ATTRIBUTE.get(key);
        if (attribute == null) {
            throw new XmlValidationException(element, "Unknown Attribute key: '" + raw + "'", hint);
        }
        return attribute;
    }

    private static EquipmentSlotGroup lookupSlotGroup(Element element, String raw, String hint) {
        EquipmentSlotGroup group = EquipmentSlotGroup.getByName(raw.toLowerCase(Locale.ROOT));
        if (group == null) {
            throw new XmlValidationException(element, "Unknown EquipmentSlotGroup: '" + raw + "'", hint);
        }
        return group;
    }

    private static NamespacedKey toKey(String raw) {
        return raw.contains(":") ? NamespacedKey.fromString(raw.toLowerCase(Locale.ROOT)) : NamespacedKey.minecraft(raw.toLowerCase(Locale.ROOT));
    }

    private static String colorize(String string) {
        try {
            return Colorize.translateBungeeHex(string);
        } catch (Throwable ignore) {
            return ChatColor.translateAlternateColorCodes('&', string);
        }
    }

    /**
     * Build {@code <persistent-data-list>} from {@link ItemMeta}'s PDC (all common types).
     */
    private static Element buildPdcList(Document document, ItemMeta meta) {
        var dataContainer = meta.getPersistentDataContainer();
        if (dataContainer.getKeys().isEmpty()) {
            return null;
        }

        Element listElement = document.createElement("persistent-data-list");
        for (NamespacedKey key : dataContainer.getKeys()) {
            String type;
            String value;

            if (dataContainer.has(key, PersistentDataType.STRING)) {
                type = "STRING";
                value = dataContainer.get(key, PersistentDataType.STRING);
            } else if (dataContainer.has(key, PersistentDataType.BOOLEAN)) {
                type = "BOOLEAN";
                value = Boolean.toString(dataContainer.get(key, PersistentDataType.BOOLEAN));
            } else if (dataContainer.has(key, PersistentDataType.INTEGER)) {
                type = "INT";
                value = Integer.toString(dataContainer.get(key, PersistentDataType.INTEGER));
            } else if (dataContainer.has(key, PersistentDataType.BYTE)) {
                type = "BYTE";
                value = Byte.toString(dataContainer.get(key, PersistentDataType.BYTE));
            } else if (dataContainer.has(key, PersistentDataType.DOUBLE)) {
                type = "DOUBLE";
                value = Double.toString(dataContainer.get(key, PersistentDataType.DOUBLE));
            } else if (dataContainer.has(key, PersistentDataType.FLOAT)) {
                type = "FLOAT";
                value = Float.toString(dataContainer.get(key, PersistentDataType.FLOAT));
            } else if (dataContainer.has(key, PersistentDataType.LONG)) {
                type = "LONG";
                value = Long.toString(dataContainer.get(key, PersistentDataType.LONG));
            } else if (dataContainer.has(key, PersistentDataType.SHORT)) {
                type = "SHORT";
                value = Short.toString(dataContainer.get(key, PersistentDataType.SHORT));
            } else {
                continue;
            }

            Element persistentDataElement = document.createElement("persistent-data");
            persistentDataElement.setAttribute("key", key.getKey());
            persistentDataElement.setAttribute("type", type);
            persistentDataElement.setTextContent(value);
            listElement.appendChild(persistentDataElement);
        }
        return listElement;
    }

    /**
     * Simple holder for an item id and its {@link ItemBuilder}.
     *
     * @param itemId  fully-qualified item identifier (e.g. {@code myplugin:test_item})
     * @param builder item builder parsed from XML
     */
    public record ItemIdEntry(String itemId, ItemBuilder builder) {
    }
}

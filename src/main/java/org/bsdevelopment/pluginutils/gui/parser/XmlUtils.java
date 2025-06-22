package org.bsdevelopment.pluginutils.gui.parser;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.w3c.dom.Element;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Helpers for validating and parsing XML attributes and enums.
 */
public final class XmlUtils {
    public static void requireTag(Element element, String expectedTag) {
        if (!element.getTagName().equals(expectedTag)) {
            throw new XmlValidationException(
                    element,
                    "Expected <" + expectedTag + ">, found <" + element.getTagName() + ">",
                    "Rename your tag to <" + expectedTag + ">"
            );
        }
    }

    public static String requireAttribute(Element element, String name) {
        String val = element.getAttribute(name);
        if (val.isBlank()) {
            throw new XmlValidationException(
                    element,
                    "Missing required attribute '" + name + "'",
                    "Add " + name + "=\"...\" to <" + element.getTagName() + ">"
            );
        }
        return val;
    }

    public static int parseIntAttribute(Element element, String name, int min, int max, String hint) {
        String raw = requireAttribute(element, name);

        try {
            int value = Integer.parseInt(raw);

            if (value < min || value > max) throw new XmlValidationException(element, "Invalid " + name + ": " + raw, hint);

            return value;
        } catch (NumberFormatException ex) {
            throw new XmlValidationException(element, "Cannot parse integer '" + raw + "' for " + name, hint);
        }
    }

    public static <T extends Enum<T>> T parseEnum(Class<T> clazz, String raw, Element element, String hint) {
        try {
            return Enum.valueOf(clazz, raw.toUpperCase());
        } catch (Exception ex) {
            throw new XmlValidationException(element, "Invalid " + clazz.getSimpleName() + ": " + raw, hint);
        }
    }

    public static URL parseTextureUrl (String name, Element element, String hint) {
        String raw = requireAttribute(element, name);

        if (!raw.startsWith("http://textures.minecraft.net/texture/"))
            throw new XmlValidationException(element, "Invalid Texture URL: " + raw, hint);

        try {
            // Use URI.create() to parse the string and then convert to URL.
            return URI.create(raw).toURL();
        } catch (MalformedURLException e) {
            throw new XmlValidationException(element, "Invalid Texture URL: " + raw, hint);
        }
    }

    /**
     * Lookup an Enchantment via the registry.
     *
     * @param element
     *         the XML element for context in error messages
     * @param raw
     *         the raw enchantment key, e.g. "sharpness" or "minecraft:sharpness"
     * @param hint
     *         what to tell the user if lookup fails
     *
     * @return the matching Enchantment
     * @throws XmlValidationException
     *         if no such Enchantment exists
     */
    public static Enchantment parseEnchantment(Element element, String raw, String hint) {
        NamespacedKey key = raw.contains(":") ? NamespacedKey.fromString(raw) : NamespacedKey.minecraft(raw.toLowerCase());
        Enchantment enchantment = Registry.ENCHANTMENT.get(key);

        if (enchantment == null)
            throw new XmlValidationException(element, "Unknown Enchantment key: \"" + raw + "\"", hint);
        return enchantment;
    }

    /**
     * Lookup an Attribute via the registry.
     *
     * @param element
     *         the XML element for context in error messages
     * @param raw
     *         the raw attribute key, e.g. "generic_attack_damage"
     * @param hint
     *         what to tell the user if lookup fails
     *
     * @return the matching Attribute
     * @throws XmlValidationException
     *         if no such Attribute exists
     */
    public static Attribute parseAttribute(Element element, String raw, String hint) {
        NamespacedKey key = raw.contains(":") ? NamespacedKey.fromString(raw) : NamespacedKey.minecraft(raw.toLowerCase());
        Attribute attribute = Registry.ATTRIBUTE.get(key);

        if (attribute == null) throw new XmlValidationException(element, "Unknown Attribute key: \"" + raw + "\"", hint);
        return attribute;
    }

    /**
     * Parse a slot‐group string into the API’s EquipmentSlotGroup via getByName().
     *
     * @param element
     *         the XML element (for error context)
     * @param raw
     *         the raw slot text, e.g. "hand", "armor", "feet", etc.
     * @param hint
     *         hint for the user if lookup fails
     *
     * @return the matching EquipmentSlotGroup
     * @throws XmlValidationException
     *         if null
     */
    public static EquipmentSlotGroup parseSlotGroup(Element element, String raw, String hint) {
        EquipmentSlotGroup group = EquipmentSlotGroup.getByName(raw);

        if (group == null) throw new XmlValidationException(element, "Unknown EquipmentSlotGroup: \"" + raw + "\"", hint);
        return group;
    }
}

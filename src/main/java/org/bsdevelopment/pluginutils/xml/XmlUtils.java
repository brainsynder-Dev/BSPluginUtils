package org.bsdevelopment.pluginutils.xml;

import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Central XML utility for BSPluginUtils.
 *
 * <p>All XML-related code (document I/O, attribute parsing, element navigation,
 * Bukkit registry lookups, and color parsing) lives here so that every subsystem
 * (particle, GUI, item) can share it without duplicating boilerplate.
 *
 * <p>All methods throw {@link XmlValidationException} (unchecked) for XML-level
 * errors. Methods that open files also declare checked {@link IOException} so
 * callers can handle file-system failures explicitly.
 */
public final class XmlUtils {
    /**
     * Parse an XML {@link Document} from a {@link File}.
     *
     * @param file the XML file to read
     * @return the parsed document with its root element normalized
     * @throws IOException            if the file cannot be opened
     * @throws XmlValidationException if the content is not well-formed XML
     */
    public static @NotNull Document parseDocument(@NotNull File file) throws IOException {
        try (InputStream stream = new FileInputStream(file)) {
            return parseDocument(stream);
        }
    }

    /**
     * Parse an XML {@link Document} from an {@link InputStream}.
     *
     * <p>The stream is not closed by this method.
     *
     * @param stream the XML source stream
     * @return the parsed document with its root element normalized
     * @throws XmlValidationException if the content is not well-formed XML
     */
    public static @NotNull Document parseDocument(@NotNull InputStream stream) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);
            Document doc = factory.newDocumentBuilder().parse(stream);
            doc.getDocumentElement().normalize();
            return doc;
        } catch (SAXException | ParserConfigurationException e) {
            throw new XmlValidationException(null, "Failed to parse XML: " + e.getMessage(), "Ensure the content is well-formed XML.");
        } catch (IOException e) {
            throw new XmlValidationException(null, "I/O error while reading XML: " + e.getMessage(), "Ensure the stream is readable.");
        }
    }

    /**
     * Create a new, empty {@link Document}.
     */
    public static @NotNull Document newDocument() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Cannot create new XML Document", e);
        }
    }

    /**
     * Write a {@link Document} to a {@link File}.
     *
     * @throws IOException if the file cannot be written
     */
    public static void writeDocument(@NotNull Document doc, @NotNull File file) throws IOException {
        try (OutputStream out = new FileOutputStream(file)) {
            writeDocument(doc, out);
        }
    }

    /**
     * Write a {@link Document} to a {@link Path}.
     *
     * @throws IOException if the path cannot be written
     */
    public static void writeDocument(@NotNull Document doc, @NotNull Path path) throws IOException {
        try (OutputStream out = Files.newOutputStream(path)) {
            writeDocument(doc, out);
        }
    }

    /**
     * Write a {@link Document} to an {@link OutputStream}.
     *
     * <p>The stream is not closed. Output is pretty-printed with 2-space indentation and UTF-8 encoding.
     *
     * @throws XmlValidationException if serialization fails
     */
    public static void writeDocument(@NotNull Document doc, @NotNull OutputStream stream) {
        try {
            newTransformer().transform(new DOMSource(doc), new StreamResult(stream));
        } catch (TransformerException e) {
            throw new XmlValidationException(null, "Failed to write XML: " + e.getMessage(), "Verify the document structure is valid.");
        }
    }

    /**
     * Serialize a {@link Document} to a self-contained XML string (pretty-printed).
     */
    public static @NotNull String documentToString(@NotNull Document doc) {
        try {
            StringWriter writer = new StringWriter();
            newTransformer().transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();
        } catch (TransformerException e) {
            throw new IllegalStateException("Failed to serialize XML document: " + e.getMessage(), e);
        }
    }

    /**
     * Assert that an element has the expected tag name.
     *
     * @throws XmlValidationException if the tag does not match
     */
    public static void requireTag(@NotNull Element element, @NotNull String expectedTag) {
        if (!element.getTagName().equals(expectedTag)) {
            throw new XmlValidationException(element, "Expected <" + expectedTag + ">, found <" + element.getTagName() + ">", "Rename your element to <" + expectedTag + ">");
        }
    }

    /**
     * Require an attribute and return its trimmed value.
     *
     * @throws XmlValidationException if the attribute is missing or blank
     */
    public static @NotNull String requireAttr(@NotNull Element element, @NotNull String name, @NotNull String hint) {
        String value = element.getAttribute(name);
        if (value == null || value.isBlank()) {
            throw new XmlValidationException(element, "Missing required attribute '" + name + "' on <" + element.getTagName() + ">", hint);
        }
        return value.trim();
    }

    /**
     * Return a trimmed attribute value, or {@code defaultValue} if absent or blank.
     */
    public static @Nullable String optionalAttr(@NotNull Element element, @NotNull String name, @Nullable String defaultValue) {
        String value = element.getAttribute(name);
        if (value == null || value.isBlank()) return defaultValue;
        return value.trim();
    }

    /**
     * Require an integer attribute whose value falls within {@code [min, max]}.
     *
     * @throws XmlValidationException if missing, not numeric, or out of range
     */
    public static int parseIntAttr(@NotNull Element element, @NotNull String name, int min, int max, @NotNull String hint) {
        String raw = requireAttr(element, name, hint);
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < min || value > max) {
                throw new XmlValidationException(element, "Value " + value + " for '" + name + "' is out of range [" + min + ", " + max + "]", hint);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new XmlValidationException(element, "Cannot parse '" + raw + "' as integer for attribute '" + name + "'", hint);
        }
    }

    /**
     * Read an optional integer attribute, returning {@code defaultValue} if absent.
     *
     * @throws XmlValidationException if present but not numeric
     */
    public static int parseIntAttr(@NotNull Element element, @NotNull String name, int defaultValue, @NotNull String hint) {
        String raw = optionalAttr(element, name, null);
        if (raw == null) return defaultValue;
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new XmlValidationException(element, "Invalid integer for '" + name + "': " + raw, hint);
        }
    }

    /**
     * Read an optional double attribute, returning {@code defaultValue} if absent.
     *
     * @throws XmlValidationException if present but not numeric
     */
    public static double parseDoubleAttr(@NotNull Element element, @NotNull String name, double defaultValue, @NotNull String hint) {
        String raw = optionalAttr(element, name, null);
        if (raw == null) return defaultValue;
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            throw new XmlValidationException(element, "Invalid double for '" + name + "': " + raw, hint);
        }
    }

    /**
     * Read an optional boolean attribute ({@code "true"} / {@code "false"}, case-insensitive),
     * returning {@code defaultValue} if absent.
     */
    public static boolean parseBoolAttr(@NotNull Element element, @NotNull String name, boolean defaultValue) {
        String raw = optionalAttr(element, name, null);
        if (raw == null) return defaultValue;
        return Boolean.parseBoolean(raw);
    }

    /**
     * Parse a string into an enum constant (case-insensitive).
     *
     * @throws XmlValidationException if the value is not a valid constant
     */
    public static <T extends Enum<T>> T parseEnum(@NotNull Class<T> clazz, @NotNull String raw,
                                                   @NotNull Element element, @NotNull String hint) {
        try {
            return Enum.valueOf(clazz, raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new XmlValidationException(element, "Invalid " + clazz.getSimpleName() + ": " + raw, hint);
        }
    }

    /**
     * Return the first direct child {@link Element} with the given tag name, or {@code null}.
     */
    public static @Nullable Element firstChildElement(@NotNull Element parent, @NotNull String tag) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element element && element.getTagName().equals(tag)) return element;
        }
        return null;
    }

    /**
     * Return the first direct child {@link Element} of any tag, or {@code null}.
     */
    public static @Nullable Element firstChildElement(@NotNull Element parent) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element element) return element;
        }
        return null;
    }

    /**
     * Validate and return a Minecraft texture URL from an element attribute.
     * The URL must start with {@code http://textures.minecraft.net/texture/}.
     *
     * @throws XmlValidationException if missing or not a valid texture URL
     */
    public static @NotNull URL parseTextureUrl(@NotNull Element element, @NotNull String attrName, @NotNull String hint) {
        String raw = requireAttr(element, attrName, hint);
        if (!raw.startsWith("http://textures.minecraft.net/texture/")) {
            throw new XmlValidationException(element, "Invalid texture URL: " + raw, hint);
        }

        try {
            return URI.create(raw).toURL();
        } catch (MalformedURLException e) {
            throw new XmlValidationException(element, "Malformed texture URL: " + raw, hint);
        }
    }

    /**
     * Look up an {@link Enchantment} by key (e.g. {@code "sharpness"} or {@code "minecraft:sharpness"}).
     *
     * @throws XmlValidationException if no such enchantment exists
     */
    public static @NotNull Enchantment lookupEnchantment(@NotNull Element element, @NotNull String raw, @NotNull String hint) {
        Enchantment enchantment = Registry.ENCHANTMENT.get(toKey(raw));
        if (enchantment == null) throw new XmlValidationException(element, "Unknown enchantment key: '" + raw + "'", hint);
        return enchantment;
    }

    /**
     * Look up an {@link Attribute} by key (e.g. {@code "generic_attack_damage"} or
     * {@code "minecraft:generic_attack_damage"}).
     *
     * @throws XmlValidationException if no such attribute exists
     */
    public static @NotNull Attribute lookupAttribute(@NotNull Element element, @NotNull String raw, @NotNull String hint) {
        Attribute attribute = Registry.ATTRIBUTE.get(toKey(raw));
        if (attribute == null) throw new XmlValidationException(element, "Unknown attribute key: '" + raw + "'", hint);
        return attribute;
    }

    /**
     * Look up an {@link EquipmentSlotGroup} by name (e.g. {@code "hand"}, {@code "armor"}).
     *
     * @throws XmlValidationException if no such slot group exists
     */
    public static @NotNull EquipmentSlotGroup lookupSlotGroup(@NotNull Element element, @NotNull String raw, @NotNull String hint) {
        EquipmentSlotGroup group = EquipmentSlotGroup.getByName(raw.toLowerCase(Locale.ROOT));
        if (group == null) throw new XmlValidationException(element, "Unknown EquipmentSlotGroup: '" + raw + "'", hint);
        return group;
    }

    /**
     * Normalize a raw string into a {@link NamespacedKey}, adding the {@code minecraft}
     * namespace when no {@code :} separator is present.
     */
    public static @NotNull NamespacedKey toKey(@NotNull String raw) {
        return raw.contains(":") ? NamespacedKey.fromString(raw.toLowerCase(Locale.ROOT)) : NamespacedKey.minecraft(raw.toLowerCase(Locale.ROOT));
    }

    /**
     * Parse a Bukkit {@link Color} from a {@code #RRGGBB} hex string or {@code R,G,B} triplet.
     *
     * @param input   the raw color string
     * @param element the surrounding XML element (for error context)
     * @param hint    user hint shown on failure
     * @throws XmlValidationException if the format is unrecognized or values are out of range
     */
    public static @NotNull Color parseColor(@NotNull String input, @NotNull Element element, @NotNull String hint) {
        String raw = input.trim();

        if (raw.startsWith("#")) {
            String hex = raw.substring(1);
            if (hex.length() != 6)
                throw new XmlValidationException(element, "Invalid hex color '" + raw + "', expected #RRGGBB", hint);
            try {
                int rgb = Integer.parseInt(hex, 16);
                return Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
            } catch (NumberFormatException e) {
                throw new XmlValidationException(element, "Invalid hex color '" + raw + "'", hint);
            }
        }

        String[] parts = raw.split(",");
        if (parts.length == 3) {
            try {
                return Color.fromRGB(
                        Integer.parseInt(parts[0].trim()),
                        Integer.parseInt(parts[1].trim()),
                        Integer.parseInt(parts[2].trim()));
            } catch (NumberFormatException e) {
                throw new XmlValidationException(element, "Invalid RGB color '" + raw + "'", hint);
            }
        }

        throw new XmlValidationException(element, "Unsupported color format '" + input + "'", hint);
    }

    /**
     * Format a Bukkit {@link Color} as a {@code #RRGGBB} hex string.
     */
    public static @NotNull String formatColor(@NotNull Color color) {
        return String.format(Locale.ROOT, "#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static @NotNull Transformer newTransformer() {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            return transformer;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create XML Transformer", e);
        }
    }
}

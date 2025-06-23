package org.bsdevelopment.pluginutils.text.component;

import com.eclipsesource.json.JsonObject;
import com.google.gson.stream.JsonWriter;
import org.bukkit.ChatColor;
import org.bukkit.Color;

/**
 * Represents a text component part used for constructing formatted messages.
 *
 * <p>This class contains properties such as default MC color, custom RGB color,
 * text styles, text content, and a custom font. It provides methods to convert
 * the part to a JSON representation, suitable for sending as a formatted message.
 *
 * <p><b>Examples:</b>
 * <pre>
 * // Create a part with text and a default MC color:
 * Part part = new Part("Hello World");
 * part.color = ChatColor.AQUA;
 *
 * // Convert the part to JSON:
 * JsonObject json = part.toJson();
 *
 * // Write JSON using a JsonWriter:
 * JsonWriter writer = ...;
 * part.writeJson(writer);
 * </pre>
 */
public class Part {

    public ChatColor color = null;
    public Color customColor = null;
    public ChatColor[] styles = null;
    public String text = "";
    public String font = null;

    public Part() {
    }

    public Part(String text) {
        this.text = text;
    }

    /**
     * Converts this part to a JsonObject.
     *
     * <p><b>Example:</b>
     * <pre>
     * JsonObject json = part.toJson();
     * </pre>
     *
     * @return a JsonObject representing this part
     */
    public JsonObject toJson() {
        var json = new JsonObject();
        json.add("text", text);

        if (this.color != null) {
            // Uses the ChatColor variable (Default MC colors)
            json.add("color", this.color.name().toLowerCase());
        } else if (customColor != null) {
            // Uses the Color (Allows RGB/HEX colors) [1.16+]
            json.add("color", toHex(customColor.getRed(), customColor.getGreen(), customColor.getBlue()));
        }

        if (this.font != null)
            json.add("font", font.toLowerCase());

        if (this.styles != null) {
            for (ChatColor style : this.styles)
                json.add(style.name().toLowerCase(), true);
        }

        return json;
    }

    /**
     * Writes this part as JSON using the provided JsonWriter.
     *
     * <p><b>Example:</b>
     * <pre>
     * JsonWriter writer = ...;
     * part.writeJson(writer);
     * </pre>
     *
     * @param json
     *         the JsonWriter to write to
     *
     * @return the JsonWriter after writing this part
     */
    public JsonWriter writeJson(JsonWriter json) {
        try {
            json.beginObject().name("text").value(this.text);

            if (this.color != null) {
                // Uses the ChatColor variable (Default MC colors)
                json.name("color").value(this.color.name().toLowerCase());
            } else if (customColor != null) {
                // Uses the Color (Allows RGB/HEX colors) [1.16+]
                json.name("color").value(toHex(customColor.getRed(), customColor.getGreen(), customColor.getBlue()));
            }

            if (this.font != null)
                json.name("font").value(font.toLowerCase());

            if (this.styles != null) {
                for (ChatColor style : this.styles) {
                    json.name(style.name().toLowerCase()).value(true);
                }
            }

            return json.endObject();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return json;
    }

    /**
     * Converts the given RGB values to a hex color string.
     *
     * <p><b>Example:</b>
     * <pre>
     * String hex = Part.toHex(255, 255, 255);
     * // hex: "#FFFFFF"
     * </pre>
     *
     * @param r
     *         the red component
     * @param g
     *         the green component
     * @param b
     *         the blue component
     *
     * @return the hex color string
     */
    public static String toHex(int r, int g, int b) {
        return "#" + toBrowserHexValue(r) + toBrowserHexValue(g) + toBrowserHexValue(b);
    }

    // Private helper method; no JavaDocs as requested.
    private static String toBrowserHexValue(int number) {
        var builder = new StringBuilder(Integer.toHexString(number & 0xff));

        while (builder.length() < 2)
            builder.append("0");

        return builder.toString().toUpperCase();
    }
}

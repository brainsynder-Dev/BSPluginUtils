package org.bsdevelopment.pluginutils.text;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import net.md_5.bungee.api.ChatColor;
import org.bsdevelopment.pluginutils.reflection.Reflection;
import org.bsdevelopment.pluginutils.text.component.Part;
import org.bukkit.Color;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides methods for colorizing text.
 *
 * <p>This class offers various utilities to translate color codes,
 * handle hex colors, and convert text into JSON formatted components.
 *
 * <p><b>Examples:</b>
 * <pre>
 * // Translate color codes using Bungee ChatColor:
 * String colored = Colorize.translateBungee("&aHello World");
 *
 * // Check if a string contains hex colors:
 * boolean hasHex = Colorize.containsHexColors("This is a &#FFFFFF test");
 *
 * // Convert a list of Parts to JSON:
 * JsonObject json = Colorize.convertParts2Json(parts);
 *
 * // Split a message into parts:
 * List&lt;Part&gt; parts = Colorize.splitMessageToParts("&aHello &bWorld");
 * </pre>
 */
public class Colorize {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#(\\w{5}[0-9a-fA-F])");

    private static Method of;

    static {
        try {
            of = Reflection.resolveMethod(ChatColor.class, "of", String.class);
        } catch (Exception e) {
            of = null;
        }
    }

    /**
     * Fetches a ChatColor based on the provided hex string.
     *
     * <p><b>Example:</b>
     * <pre>
     * ChatColor color = Colorize.fetchColor("&#FFFFFF");
     * </pre>
     *
     * @param hex
     *         the hex color code
     *
     * @return the corresponding ChatColor, or ChatColor.WHITE if not found
     */
    public static ChatColor fetchColor(String hex) {
        return fetchColor(hex, ChatColor.WHITE);
    }

    /**
     * Fetches a ChatColor based on the provided hex string, with a fallback.
     *
     * <p><b>Example:</b>
     * <pre>
     * ChatColor color = Colorize.fetchColor("&#ABCDEF", ChatColor.GRAY);
     * </pre>
     *
     * @param hex
     *         the hex color code
     * @param fallback
     *         the fallback ChatColor if conversion fails
     *
     * @return the corresponding ChatColor, or the fallback if conversion fails
     */
    public static ChatColor fetchColor(String hex, ChatColor fallback) {
        if (of == null) return fallback;
        if ((hex == null) || hex.isEmpty()) return fallback;

        if (hex.startsWith("&#")) hex = hex.replace("&", "");

        if (!hex.startsWith("#")) hex = "#" + hex;

        return Reflection.executeMethod(of, null, hex);
    }

    /**
     * Fetches a ChatColor based on a Bukkit Color.
     *
     * <p><b>Example:</b>
     * <pre>
     * ChatColor color = Colorize.fetchColor(Color.fromRGB(255, 255, 255));
     * </pre>
     *
     * @param color
     *         the Bukkit Color
     *
     * @return the corresponding ChatColor, or ChatColor.WHITE if color is null
     */
    public static ChatColor fetchColor(Color color) {
        if (color == null) return ChatColor.WHITE;
        return fetchColor(toHex(color.getRed(), color.getGreen(), color.getBlue()));
    }

    /**
     * Translates the text using the Bungee ChatColor translation.
     *
     * <p><b>Example:</b>
     * <pre>
     * String result = Colorize.translateBungee("&aHello World");
     * </pre>
     *
     * @param text
     *         the text to translate
     *
     * @return the translated text
     */
    public static String translateBungee(String text) {
        if ((text == null) || text.isEmpty()) return text;

        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Translates the text using the Bukkit ChatColor translation.
     *
     * <p><b>Example:</b>
     * <pre>
     * String result = Colorize.translateBukkit("&aHello World");
     * </pre>
     *
     * @param text
     *         the text to translate
     *
     * @return the translated text
     */
    public static String translateBukkit(String text) {
        if ((text == null) || text.isEmpty()) return text;

        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Checks if the provided text contains valid HEX colors (e.g., "&#FFFFFF").
     *
     * <p><b>Example:</b>
     * <pre>
     * boolean hasHex = Colorize.containsHexColors("Test &#FFFFFF");
     * </pre>
     *
     * @param text
     *         the text to check
     *
     * @return true if valid hex is found; false otherwise
     */
    public static boolean containsHexColors(String text) {
        if ((text == null) || text.isEmpty()) return false;

        text = text.replace(ChatColor.COLOR_CHAR, '&');

        Matcher matcher = HEX_PATTERN.matcher(text);
        return matcher.find();
    }

    /**
     * Translates text that uses the '&' symbol and supports hex colors.
     *
     * <p><b>Example:</b>
     * <pre>
     * String result = Colorize.translateBungeeHex("&aHello &#FFFFFFWorld");
     * </pre>
     *
     * @param text
     *         the text to translate
     *
     * @return the colorized text
     */
    public static String translateBungeeHex(String text) {
        if ((text == null) || text.isEmpty()) return text;

        text = text.replace(ChatColor.COLOR_CHAR, '&');

        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String replacement = "";

            if (of != null) {
                // This is mostly in case someone is using a legacy version (e.g., below 1.16)
                try {
                    replacement = String.valueOf(Reflection.executeMethod(of, null, "#" + matcher.group(1)));
                } catch (Exception ignored) {
                }
            }

            matcher.appendReplacement(buffer, replacement).toString();
        }

        return ChatColor.translateAlternateColorCodes('&', matcher.appendTail(buffer).toString());
    }

    /**
     * Removes hex color formatting from the text and converts it to a readable format.
     *
     * <p><b>Example:</b>
     * <pre>
     * String result = Colorize.removeHexColor("&x&e&3&a&a&4&fExample");
     * </pre>
     *
     * @param text
     *         the text to process
     *
     * @return the text with hex color formatting removed
     */
    public static String removeHexColor(String text) {
        if ((text == null) || text.isEmpty()) return text;

        text = text.replace(ChatColor.COLOR_CHAR, '&');

        Pattern word = Pattern.compile("&x");
        Matcher matcher = word.matcher(text);

        char[] chars = text.toCharArray();
        while (matcher.find()) {
            StringBuilder builder = new StringBuilder();
            int start = matcher.start();
            int end = start + 13;

            if (end > text.length()) continue;

            for (int i = start; i < end; i++) builder.append(chars[i]);

            String hex = builder.toString();
            hex = hex.replace("&x", "").replace("&", "");

            text = text.replace(builder.toString(), "&#" + hex);
        }

        return text;
    }

    /**
     * Converts a list of Parts to a JSON object.
     *
     * <p>The JSON format is: {"text":"","extra":[...]}
     *
     * <p><b>Example:</b>
     * <pre>
     * JsonObject json = Colorize.convertParts2Json(parts);
     * </pre>
     *
     * @param parts
     *         the list of Parts
     *
     * @return a JsonObject representing the parts
     */
    public static JsonObject convertParts2Json(List<Part> parts) {
        JsonObject json = new JsonObject();
        json.add("text", "");

        JsonArray extra = new JsonArray();

        for (Part part : parts) {
            extra.add(part.toJson());
        }

        json.add("extra", extra);
        return json;
    }

    /**
     * Splits a message into Parts based on color codes.
     *
     * <p><b>Example:</b>
     * <pre>
     * List&lt;Part&gt; parts = Colorize.splitMessageToParts("&aHello &bWorld");
     * </pre>
     *
     * @param value
     *         the message to split
     *
     * @return a list of Parts extracted from the message
     */
    public static List<Part> splitMessageToParts(String value) {
        List<Part> parts = new ArrayList<>();

        if ((value == null) || value.isEmpty()) return parts;

        value = value.replace(org.bukkit.ChatColor.COLOR_CHAR, '&');

        if (value.contains("&")) {
            String[] args = value.split("&");

            for (String string : args) {
                if (string == null) continue;
                if (string.isEmpty()) continue;

                Part part = new Part();

                if (string.startsWith("#")) {
                    StringBuilder HEX = new StringBuilder();
                    int end = 6;

                    for (int i = -1; i < end; i++) HEX.append(string.charAt(i + 1));

                    part.text = string.replace(HEX.toString(), "");
                    part.customColor = hex2Color(HEX.toString());
                } else {
                    org.bukkit.ChatColor color = org.bukkit.ChatColor.getByChar(string.charAt(0));

                    if (color == null) {
                        part.text = "&" + string;
                    } else {
                        part.text = string.replaceFirst(String.valueOf(string.charAt(0)), "");
                        part.color = color;
                    }
                }

                parts.add(part);
            }
        } else {
            parts.add(new Part(value));
        }

        return parts;
    }

    /**
     * Converts a hex string to a Bukkit Color.
     *
     * <p><b>Example:</b>
     * <pre>
     * Color color = Colorize.hex2Color("&#FFFFFF");
     * </pre>
     *
     * @param hex
     *         the hex string (must start with '#' and have 7 characters)
     *
     * @return the corresponding Color
     */
    public static Color hex2Color(String hex) {
        return Color.fromRGB(
                Integer.valueOf(hex.substring(1, 3), 16),

                Integer.valueOf(hex.substring(3, 5), 16),

                Integer.valueOf(hex.substring(5, 7), 16)
        );
    }

    /**
     * Converts RGB values to a hex string.
     *
     * <p><b>Example:</b>
     * <pre>
     * String hex = Colorize.toHex(255, 255, 255);
     * // hex: "#FFFFFF"
     * </pre>
     *
     * @param r
     *         the red value
     * @param g
     *         the green value
     * @param b
     *         the blue value
     *
     * @return the hex string representing the color
     */
    public static String toHex(int r, int g, int b) {
        return "#" + toBrowserHexValue(r) + toBrowserHexValue(g) + toBrowserHexValue(b);
    }

    /**
     * Converts an integer to a two-character hex string.
     *
     * @param number
     *         the integer value
     *
     * @return the two-character hex string in uppercase
     */
    private static String toBrowserHexValue(int number) {
        StringBuilder builder = new StringBuilder(Integer.toHexString(number & 0xff));

        while (builder.length() < 2) {
            builder.append("0");
        }

        return builder.toString().toUpperCase();
    }
}

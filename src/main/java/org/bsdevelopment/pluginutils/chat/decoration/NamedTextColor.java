package org.bsdevelopment.pluginutils.chat.decoration;

import org.bukkit.Color;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Represents a text color by name and RGB value.
 * <p>
 * Provides a number of standard Minecraft colors as constants, and
 * factory methods for creating custom colors from hex strings, RGB
 * triples, or Bukkit {@link Color} instances.
 */
public class NamedTextColor {
    // Standard named Minecraft colors:
    public static final NamedTextColor BLACK = new NamedTextColor("black", 0x000000);
    public static final NamedTextColor DARK_BLUE = new NamedTextColor("dark_blue", 0x0000AA);
    public static final NamedTextColor DARK_GREEN = new NamedTextColor("dark_green", 0x00AA00);
    public static final NamedTextColor DARK_AQUA = new NamedTextColor("dark_aqua", 0x00AAAA);
    public static final NamedTextColor DARK_RED = new NamedTextColor("dark_red", 0xAA0000);
    public static final NamedTextColor DARK_PURPLE = new NamedTextColor("dark_purple", 0xAA00AA);
    public static final NamedTextColor GOLD = new NamedTextColor("gold", 0xFFAA00);
    public static final NamedTextColor GRAY = new NamedTextColor("gray", 0xAAAAAA);
    public static final NamedTextColor DARK_GRAY = new NamedTextColor("dark_gray", 0x555555);
    public static final NamedTextColor BLUE = new NamedTextColor("blue", 0x5555FF);
    public static final NamedTextColor GREEN = new NamedTextColor("green", 0x55FF55);
    public static final NamedTextColor AQUA = new NamedTextColor("aqua", 0x55FFFF);
    public static final NamedTextColor RED = new NamedTextColor("red", 0xFF5555);
    public static final NamedTextColor LIGHT_PURPLE = new NamedTextColor("light_purple", 0xFF55FF);
    public static final NamedTextColor YELLOW = new NamedTextColor("yellow", 0xFFFF55);
    public static final NamedTextColor WHITE = new NamedTextColor("white", 0xFFFFFF);


    private static final Pattern HEX_PATTERN = Pattern.compile("^#?[0-9A-Fa-f]{6}$");

    /** The name of this color (e.g. "dark_red" or custom identifier). */
    private final String name;
    /** RGB value packed as 0xRRGGBB. */
    private final int rgb;


    /**
     * Constructs a new NamedTextColor.
     *
     * @param name
     *         a unique name or identifier for this color
     * @param rgb
     *         the packed RGB value (0xRRGGBB)
     */
    public NamedTextColor(String name, int rgb) {
        this.name = Objects.requireNonNull(name, "name");
        this.rgb = rgb & 0xFFFFFF; // mask to 24 bits
    }

    /**
     * @return this color’s identifier
     */
    public String name() {
        return name;
    }

    /**
     * @return the packed RGB value (0xRRGGBB)
     */
    public int rgb() {
        return rgb;
    }

    /**
     * @return the hex string for this color, in "#RRGGBB" format
     */
    public String asHexString() {
        return String.format("#%06X", rgb);
    }

    // ——— Factory methods ——————————————————————————

    /**
     * Creates a custom color from a hex string.
     *
     * @param hexCode
     *         hex code in the form "#RRGGBB" or "RRGGBB"
     *
     * @return a new NamedTextColor with name equal to the sanitized hex string
     * @throws IllegalArgumentException
     *         if the string isn’t a valid 6-digit hex code
     */
    public static NamedTextColor ofHex(String hexCode) {
        Objects.requireNonNull(hexCode, "hexCode");
        String normalized = hexCode.startsWith("#") ? hexCode.substring(1) : hexCode;
        String fullHex = "#" + normalized.toUpperCase();
        if (!HEX_PATTERN.matcher(fullHex).matches()) {
            throw new IllegalArgumentException("Invalid hex code: " + hexCode);
        }
        int rgb = Integer.parseInt(normalized, 16);
        return new NamedTextColor(fullHex, rgb);
    }

    /**
     * Creates a custom color from individual red, green, and blue components.
     *
     * @param red
     *         red channel (0–255)
     * @param green
     *         green channel (0–255)
     * @param blue
     *         blue channel (0–255)
     *
     * @return a new NamedTextColor with name "rgb_R_G_B"
     * @throws IllegalArgumentException
     *         if any channel is outside 0–255
     */
    public static NamedTextColor ofRgb(int red, int green, int blue) {
        if ((red | green | blue) < 0 || red > 255 || green > 255 || blue > 255) {
            throw new IllegalArgumentException(
                    "RGB values must be between 0 and 255: "
                            + red + "," + green + "," + blue
            );
        }
        int rgb = (red << 16) | (green << 8) | blue;
        String name = "rgb_" + red + "_" + green + "_" + blue;
        return new NamedTextColor(name, rgb);
    }

    /**
     * Creates a custom color from a Bukkit {@link Color}, converting its RGB channels.
     *
     * @param bukkitColor
     *         the Bukkit color
     *
     * @return a new NamedTextColor with name "bukkit_R_G_B"
     */
    public static NamedTextColor ofColor(Color bukkitColor) {
        Objects.requireNonNull(bukkitColor, "bukkitColor");
        int red = bukkitColor.getRed();
        int green = bukkitColor.getGreen();
        int blue = bukkitColor.getBlue();
        int rgb = (red << 16) | (green << 8) | blue;
        String name = "bukkit_" + red + "_" + green + "_" + blue;
        return new NamedTextColor(name, rgb);
    }

    @Override
    public String toString() {
        return name + "(" + asHexString() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NamedTextColor that)) return false;
        return rgb == that.rgb && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, rgb);
    }
}
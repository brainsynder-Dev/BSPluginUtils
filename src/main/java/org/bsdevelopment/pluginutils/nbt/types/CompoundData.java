package org.bsdevelopment.pluginutils.nbt.types;

import org.bsdevelopment.pluginutils.nbt.BasicData;
import org.bsdevelopment.pluginutils.nbt.TagType;
import org.bsdevelopment.pluginutils.nbt.types.array.ByteArrayData;
import org.bsdevelopment.pluginutils.nbt.types.array.IntArrayData;
import org.bsdevelopment.pluginutils.text.Colorize;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * An NBT tag representing a collection of name-to-tag mappings, behaving like a map where
 * keys are strings and values are other NBT tags.
 *
 * <p><b>Example Usage:</b>
 * <pre>
 * // Create and store multiple data tags
 * CompoundData compound = new CompoundData();
 * compound.setInteger("Level", 42);
 * compound.setString("Message", "Hello!");
 *
 * // Retrieve stored values
 * int level = compound.getInteger("Level");
 * String message = compound.getString("Message");
 * </pre>
 */
public class CompoundData implements BasicData {
    private static final Pattern PATTERN = Pattern.compile("[A-Za-z0-9._+-]+");
    private final Map<String, BasicData> dataMap = new HashMap<>();

    public CompoundData() {
    }

    /**
     * Constructs a {@code CompoundData} with an existing map of tags.
     *
     * <p>A defensive copy is created to avoid external mutation of internal data.
     *
     * @param dataMap
     *         a map of name-to-data entries
     */
    public CompoundData(Map<String, BasicData> dataMap) {
        this.dataMap.putAll(dataMap);
    }

    /**
     * Checks if this compound has a specific named key.
     *
     * @param key
     *         the data name
     *
     * @return true if the name is present, otherwise false
     */
    public boolean hasData(String key) {
        return dataMap.containsKey(key);
    }

    /**
     * Adds or replaces a named data in this compound.
     *
     * @param name
     *         the data name
     * @param data
     *         the data to insert
     * @param <T>
     *         the type of the data
     *
     * @return the previous data associated with the name, or {@code null} if none
     */
    public <T extends BasicData> T setData(String name, BasicData data) {
        return (T) dataMap.put(name, data);
    }

    /**
     * Retrieves a data from this compound by name.
     *
     * @param name
     *         the data name
     * @param <T>
     *         the type of the data
     *
     * @return the requested data, or {@code null} if none
     */
    public <T extends BasicData> T getData(String name) {
        return (T) dataMap.get(name);
    }

    /**
     * Returns a set of all keys in this compound.
     *
     * @return the set of data names
     */
    public Set<String> getKeys() {
        return dataMap.keySet();
    }

    /**
     * Removes a data entry by name.
     *
     * @param name
     *         the data name
     *
     * @return the removed data, or null if not found
     */
    public BasicData remove(String name) {
        return dataMap.remove(name);
    }

    /**
     * Returns the number of tag entries in this compound.
     *
     * @return the size of this compound tag
     */
    public int size() {
        return dataMap.size();
    }

    /**
     * Gets the type of this tag, always {@link TagType#COMPOUND}.
     *
     * @return the compound tag type
     */
    @Override
    public TagType getType() {
        return TagType.COMPOUND;
    }

    /**
     * Creates a deep copy of this compound, including copies of all nested tags.
     *
     * @return a new {@code CompoundTag} with cloned data
     */
    @Override
    public CompoundData copy() {
        var copyMap = new HashMap<String, BasicData>();
        for (var entry : dataMap.entrySet()) copyMap.put(entry.getKey(), entry.getValue().copy());
        return new CompoundData(copyMap);
    }

    /**
     * Retrieves a shallow copy of the internal map for direct use.
     *
     * @return a copy of the internal map data
     */
    public Map<String, BasicData> copyMap() {
        return new HashMap<>(dataMap);
    }

    /**
     * Returns a string representation of the compound in NBT-style format.
     *
     * @return a string including the keys and values
     */
    @Override
    public String toString() {
        var stringbuilder = new StringBuilder("{");
        var collection = dataMap.keySet();

        for (var s : collection) {
            if (stringbuilder.length() != 1) stringbuilder.append(',');
            stringbuilder.append(match(s)).append(':').append(dataMap.get(s));
        }
        return stringbuilder.append('}').toString();
    }

    private String match(String key) {
        if (PATTERN.matcher(key).matches()) return key;

        var stringbuilder = new StringBuilder("\"");
        for (var i = 0; i < key.length(); i++) {
            char c0 = key.charAt(i);
            if (c0 == '\\' || c0 == '"') stringbuilder.append('\\');
            stringbuilder.append(c0);
        }
        return stringbuilder.append('"').toString();
    }


    // --- BULK QOL METHODS --- //

    /**
     * Inserts a {@link ByteData} with the specified value.
     *
     * @param key
     *         the key
     * @param value
     *         the byte value
     *
     * @return this compound for chaining
     */
    public CompoundData setByte(String key, byte value) {
        dataMap.put(key, new ByteData(value));
        return this;
    }

    /**
     * Inserts a {@link ShortData} with the specified value.
     *
     * @param key
     *         the key
     * @param value
     *         the short value
     *
     * @return this compound for chaining
     */
    public CompoundData setShort(String key, short value) {
        dataMap.put(key, new ShortData(value));
        return this;
    }

    /**
     * Inserts an {@link IntData} with the specified integer value.
     *
     * @param key
     *         the key
     * @param value
     *         the int value
     *
     * @return this compound for chaining
     */
    public CompoundData setInteger(String key, int value) {
        dataMap.put(key, new IntData(value));
        return this;
    }

    /**
     * Inserts a {@link LongData} with the specified long value.
     *
     * @param key
     *         the key
     * @param value
     *         the long value
     *
     * @return this compound for chaining
     */
    public CompoundData setLong(String key, long value) {
        dataMap.put(key, new LongData(value));
        return this;
    }

    /**
     * Stores a {@link UUID} value as a string within this compound.
     *
     * @param key
     *         the key
     * @param value
     *         the UUID to store
     *
     * @return this compound for chaining
     */
    public CompoundData setUniqueId(String key, UUID value) {
        setString(key, value.toString());
        return this;
    }

    /**
     * Retrieves a stored {@link UUID} from this compound, or generates a new one if not present or invalid.
     *
     * @param key
     *         the key
     *
     * @return the retrieved or newly generated UUID
     */
    public UUID getUniqueId(String key) {
        if (hasData(key)) {
            var raw = getString(key);
            try {
                return UUID.fromString(raw);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return UUID.randomUUID();
    }

    /**
     * Inserts a {@link FloatData} with the specified float value.
     *
     * @param key
     *         the key
     * @param value
     *         the float value
     *
     * @return this compound for chaining
     */
    public CompoundData setFloat(String key, float value) {
        dataMap.put(key, new FloatData(value));
        return this;
    }

    /**
     * Inserts a {@link DoubleData} with the specified double value.
     *
     * @param key
     *         the key
     * @param value
     *         the double value
     *
     * @return this compound for chaining
     */
    public CompoundData setDouble(String key, double value) {
        dataMap.put(key, new DoubleData(value));
        return this;
    }

    /**
     * Inserts a {@link StringData} with the specified string value.
     *
     * @param key
     *         the key
     * @param value
     *         the string value
     *
     * @return this compound for chaining
     */
    public CompoundData setString(String key, String value) {
        dataMap.put(key, new StringData(value));
        return this;
    }

    /**
     * Inserts a {@link ByteArrayData} with the specified byte array value.
     *
     * @param key
     *         the key
     * @param value
     *         the byte array
     *
     * @return this compound for chaining
     */
    public CompoundData setByteArray(String key, byte[] value) {
        dataMap.put(key, new ByteArrayData(value));
        return this;
    }

    /**
     * Inserts an {@link IntArrayData} with the specified int array value.
     *
     * @param key
     *         the key
     * @param value
     *         the int array
     *
     * @return this compound for chaining
     */
    public CompoundData setIntArray(String key, int[] value) {
        dataMap.put(key, new IntArrayData(value));
        return this;
    }

    /**
     * Inserts a boolean value as a {@link ByteData}, storing 1 for {@code true} and 0 for {@code false}.
     *
     * @param key
     *         the key
     * @param value
     *         the boolean value
     *
     * @return this compound for chaining
     */
    public CompoundData setBoolean(String key, boolean value) {
        dataMap.put(key, new ByteData((byte) (value ? 1 : 0)));
        return this;
    }

    public boolean getBoolean(String key) {
        ByteData data = (ByteData) dataMap.getOrDefault(key, null);
        if (data == null) return false;
        return data.value() == 1;
    }

    /**
     * Inserts a {@link Location} value under the given key, storing its coordinates and world name.
     *
     * @param key
     *         the key
     * @param location
     *         the {@link Location} to store
     *
     * @return this compound for chaining
     */
    public CompoundData setLocation(String key, Location location) {
        var compound = new CompoundData();
        compound.setString("world", location.getWorld().getName());
        compound.setDouble("x", location.getX());
        compound.setDouble("y", location.getY());
        compound.setDouble("z", location.getZ());
        compound.setFloat("yaw", location.getYaw());
        compound.setFloat("pitch", location.getPitch());
        setData(key, compound);
        return this;
    }

    /**
     * Retrieves a {@link Location} from the compound using the given key, or a fallback world name "world" if not found.
     *
     * @param key
     *         the key
     *
     * @return the constructed {@link Location} object
     */
    public Location getLocation(String key) {
        CompoundData compound = getData(key);
        var world = Bukkit.getWorld(compound.getString("world", "world"));
        var x = compound.getDouble("x", 0);
        var y = compound.getDouble("y", 0);
        var z = compound.getDouble("z", 0);
        var yaw = compound.getFloat("yaw", 0f);
        var pitch = compound.getFloat("pitch", 0f);
        return new Location(world, x, y, z, yaw, pitch);
    }

    /**
     * Retrieves a {@link Location} from the compound using the given key, or returns a fallback if not present.
     *
     * @param key
     *         the key
     * @param fallback
     *         the fallback {@link Location}
     *
     * @return the found {@link Location} or {@code fallback}
     */
    public Location getLocation(String key, Location fallback) {
        return hasData(key) ? getLocation(key) : fallback;
    }

    /**
     * Inserts a {@link Color} value under the given key using {@link ColorStorageType#COMPOUND} by default.
     *
     * @param key
     *         the key
     * @param color
     *         the {@link Color}
     *
     * @return this compound for chaining
     */
    public CompoundData setColor(String key, Color color) {
        return setColor(key, color, ColorStorageType.COMPOUND);
    }

    /**
     * Inserts a {@link Color} under the given key in the specified {@link ColorStorageType} format.
     *
     * @param key
     *         the key
     * @param color
     *         the {@link Color}
     * @param type
     *         the storage type format
     *
     * @return this compound for chaining
     */
    public CompoundData setColor(String key, Color color, ColorStorageType type) {
        switch (type) {
            case HEX -> setString(key, Colorize.toHex(color.getRed(), color.getGreen(), color.getBlue()));
            case INT -> setInteger(key, color.asRGB());
            case STRING -> {
                var joined = color.getRed() + "," + color.getGreen() + "," + color.getBlue();
                setString(key, joined);
            }
            case COMPOUND -> {
                var compound = new CompoundData();
                compound.setInteger("r", color.getRed());
                compound.setInteger("g", color.getGreen());
                compound.setInteger("b", color.getBlue());
                setData(key, compound);
            }
        }
        return this;
    }

    /**
     * Retrieves a {@link Color} from the compound using the given key, or {@link Color#RED} if not present.
     *
     * @param key
     *         the key
     *
     * @return the retrieved {@link Color}, or red if none
     */
    public Color getColor(String key) {
        return getColor(key, Color.RED);
    }

    /**
     * Retrieves a {@link Color} from the compound using the given key, or a fallback color if not present.
     *
     * @param key
     *         the key
     * @param fallback
     *         the fallback {@link Color}
     *
     * @return the retrieved color or the fallback
     */
    public Color getColor(String key, Color fallback) {
        if (!hasData(key)) return fallback;

        var base = getData(key);

        // Stored as a compound with r,g,b
        if (base instanceof CompoundData compound) {
            var r = compound.getInteger("r", 0);
            if (r > 255) r = 255;
            if (r < 0) r = 0;

            var g = compound.getInteger("g", 0);
            if (g > 255) g = 255;
            if (g < 0) g = 0;

            var b = compound.getInteger("b", 0);
            if (b > 255) b = 255;
            if (b < 0) b = 0;
            return Color.fromRGB(r, g, b);
        }

        // Stored as an int
        if (base instanceof IntData(int value)) return Color.fromRGB(value);

        // Stored as a string
        if (base instanceof StringData(String value)) {
            // If it's hex
            if (value.startsWith("#")) {
                return Color.fromRGB(
                        Integer.valueOf(value.substring(1, 3), 16),
                        Integer.valueOf(value.substring(3, 5), 16),
                        Integer.valueOf(value.substring(5, 7), 16)
                );
            }
            // If it's "r,g,b"
            if (value.contains(",")) {
                var args = value.split(",");
                var r = 255;
                var g = 255;
                var b = 255;
                if (args.length >= 3) {
                    r = Integer.parseInt(args[0].trim());
                    g = Integer.parseInt(args[1].trim());
                    b = Integer.parseInt(args[2].trim());
                }
                return Color.fromRGB(r, g, b);
            }
            // Possibly an int in string form
            try {
                return Color.fromRGB(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
        return fallback;
    }

    /**
     * Stores an enum value by name using a {@link StringData}.
     *
     * @param key
     *         the key
     * @param anEnum
     *         the enum value
     *
     * @return this compound for chaining
     */
    public CompoundData setEnum(String key, Enum<?> anEnum) {
        setString(key, anEnum.name());
        return this;
    }

    /**
     * Retrieves an enum value by name.
     *
     * @param key
     *         the key
     * @param type
     *         the enum class
     * @param <E>
     *         the type of the enum
     *
     * @return the retrieved enum, or null if not found
     */
    public <E extends Enum<E>> E getEnum(String key, Class<E> type) {
        return getEnum(key, type, null);
    }

    /**
     * Retrieves an enum value by name, returning a fallback if the key doesn't exist.
     *
     * @param key
     *         the key
     * @param type
     *         the enum class
     * @param fallback
     *         a fallback enum value
     * @param <E>
     *         the type of the enum
     *
     * @return the retrieved enum or the fallback
     */
    public <E extends Enum<E>> E getEnum(String key, Class<E> type, E fallback) {
        if (!hasData(key)) return fallback;
        return E.valueOf(type, getString(key));
    }

    /**
     * Retrieves a byte from this compound, or 0 if not found or mismatched.
     *
     * @param key
     *         the key
     *
     * @return the byte value, defaulting to 0
     */
    public byte getByte(String key) {
        var storage = dataMap.get(key);
        if (storage != null && storage.getType() == TagType.BYTE) return ((ByteData) storage).value();
        return 0;
    }

    /**
     * Retrieves a byte from this compound, returning a fallback if not found.
     *
     * @param key
     *         the key
     * @param fallback
     *         the fallback byte
     *
     * @return the found value or fallback
     */
    public byte getByte(String key, byte fallback) {
        return hasData(key) ? getByte(key) : fallback;
    }

    /**
     * Retrieves a short from this compound, or 0 if not found.
     *
     * <p><b>Note:</b> This uses {@link #getValue(String)} internally and may cause exceptions
     * if the stored string is not a valid short.
     *
     * @param key
     *         the key
     *
     * @return the short value or 0
     */
    public short getShort(String key) {
        return Short.parseShort(getValue(key));
    }

    /**
     * Retrieves a short from this compound, returning a fallback if not found.
     *
     * @param key
     *         the key
     * @param fallback
     *         fallback short
     *
     * @return the found value or fallback
     */
    public short getShort(String key, short fallback) {
        return hasData(key) ? getShort(key) : fallback;
    }

    /**
     * Retrieves an integer from this compound, or 0 if not found.
     *
     * <p><b>Note:</b> This uses {@link #getValue(String)} which may throw
     * an exception if the data is incompatible with an int.
     *
     * @param key
     *         the key
     *
     * @return the integer value or 0
     */
    public int getInteger(String key) {
        return Integer.parseInt(getValue(key));
    }

    /**
     * Retrieves an integer, returning a fallback if not found.
     *
     * @param key
     *         the key
     * @param fallback
     *         fallback integer
     *
     * @return the integer or fallback
     */
    public int getInteger(String key, int fallback) {
        return hasData(key) ? getInteger(key) : fallback;
    }

    /**
     * Retrieves a long from this compound, or 0 if not found.
     *
     * @param key
     *         the key
     *
     * @return the long value or 0
     */
    public long getLong(String key) {
        return Long.parseLong(getValue(key));
    }

    /**
     * Retrieves a long, returning a fallback if not found.
     *
     * @param key
     *         the key
     * @param fallback
     *         fallback long
     *
     * @return the long or fallback
     */
    public long getLong(String key, long fallback) {
        return hasData(key) ? getLong(key) : fallback;
    }

    /**
     * Retrieves a float from this compound, or 0 if not found.
     *
     * @param key
     *         the key
     *
     * @return the float value or 0
     */
    public float getFloat(String key) {
        return Float.parseFloat(getValue(key));
    }

    /**
     * Retrieves a float, returning a fallback if not found.
     *
     * @param key
     *         the key
     * @param fallback
     *         fallback float
     *
     * @return the float or fallback
     */
    public float getFloat(String key, float fallback) {
        return hasData(key) ? getFloat(key) : fallback;
    }

    /**
     * Retrieves a double from this compound, or 0 if not found.
     *
     * @param key
     *         the key
     *
     * @return the double or 0
     */
    public double getDouble(String key) {
        return Double.parseDouble(getValue(key));
    }

    /**
     * Retrieves a double, returning a fallback if not found.
     *
     * @param key
     *         the key
     * @param fallback
     *         fallback double
     *
     * @return the double or fallback
     */
    public double getDouble(String key, double fallback) {
        return hasData(key) ? getDouble(key) : fallback;
    }

    /**
     * Retrieves a string from this compound, or an empty string if not found.
     *
     * @param key
     *         the key
     *
     * @return the string value or an empty string
     */
    public String getString(String key) {
        return getValue(key);
    }

    /**
     * Retrieves a string, returning a fallback if not found.
     *
     * @param key
     *         the key
     * @param fallback
     *         fallback string
     *
     * @return the string or fallback
     */
    public String getString(String key, String fallback) {
        return hasData(key) ? getValue(key) : fallback;
    }

    private String getValue(String key) {
        try {
            if (hasData(key)) return fetchValue(dataMap.get(key));
        } catch (ClassCastException ignored) {
        }
        return "";
    }

    private String fetchValue(BasicData base) {
        if (base instanceof ByteData(byte value)) {
            if (value == 0 || value == 1) return String.valueOf(value == 1);
            return String.valueOf(value);
        }
        if (base instanceof ByteArrayData(byte[] value)) return Arrays.toString(value);
        if (base instanceof DoubleData(double value)) return String.valueOf(value);
        if (base instanceof FloatData(float value)) return String.valueOf(value);
        if (base instanceof IntData(int value)) return String.valueOf(value);
        if (base instanceof IntArrayData(int[] value)) return Arrays.toString(value);
        if (base instanceof LongData(long value)) return String.valueOf(value);
        if (base instanceof ShortData(short value)) return String.valueOf(value);
        if (base instanceof StringData(String value)) return value;
        if (base instanceof ListData listData) {
            var builder = new StringBuilder();
            for (var i = 0; i < listData.size(); i++) {
                builder.append(fetchValue(listData.get(i)));
            }
            return builder.toString();
        }
        return "";
    }

    /**
     * Different ways to store {@link Color} data in a CompoundTag.
     */
    public enum ColorStorageType {
        /**
         * Stores color in a separate compound as r, g, b integer tags.
         */
        COMPOUND,
        /**
         * Stores color as a single integer using {@link Color#asRGB()}.
         */
        INT,
        /**
         * Stores color as a hex string (#RRGGBB).
         */
        HEX,
        /**
         * Stores color as a comma-delimited string: "R,G,B".
         */
        STRING
    }
}

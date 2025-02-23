package org.bsdevelopment.pluginutils.nbt.types;

import org.bsdevelopment.pluginutils.nbt.Tag;
import org.bsdevelopment.pluginutils.nbt.TagType;
import org.bsdevelopment.pluginutils.nbt.types.array.ByteArrayTag;
import org.bsdevelopment.pluginutils.nbt.types.array.IntArrayTag;
import org.bsdevelopment.pluginutils.text.Colorize;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * An NBT tag representing a collection of name-to-tag mappings.
 * 
 * <p>This is similar to a map where keys are strings and
 * values are NBT tags.</p>
 */
public final class CompoundTag implements Tag {
    private static final Pattern PATTERN = Pattern.compile("[A-Za-z0-9._+-]+");

    private final LinkedHashMap<String, Tag> tagMap;

    /**
     * Constructs an empty CompoundTag.
     */
    public CompoundTag() {
        this.tagMap = new LinkedHashMap<>();
    }

    /**
     * Constructs a CompoundTag with an existing map of tags.
     *
     * @param tagMap The map of name-to-Tag entries to store.
     */
    public CompoundTag(Map<String, Tag> tagMap) {
        // Defensive copy to ensure external changes can’t mutate this internally
        this.tagMap = new LinkedHashMap<>(tagMap);
    }

    public boolean hasKey(String key) {
        return tagMap.containsKey(key);
    }

    /**
     * Puts a named tag into this compound tag.
     *
     * @param name The name of the tag.
     * @param tag  The tag instance to store.
     * @return The previous tag associated with this name, or null if none existed.
     */
    public <T extends Tag> T put(String name, Tag tag) {
        return (T) tagMap.put(name, tag);
    }

    /**
     * Retrieves a named tag from this compound tag.
     *
     * @param name The name of the tag.
     * @return The tag instance if found, or null otherwise.
     */
    public <T extends Tag> T get(String name) {
        return (T) tagMap.get(name);
    }

    /**
     * Returns a Set of all tag names stored in this compound.
     *
     * @return A Set of names for the stored tags.
     */
    public Set<String> getKeys() {
        return tagMap.keySet();
    }

    /**
     * Removes a named tag from this compound tag.
     *
     * @param name The name of the tag to remove.
     * @return The removed tag instance, or null if it did not exist.
     */
    public Tag remove(String name) {
        return tagMap.remove(name);
    }

    /**
     * Returns the size (the number of tags) in this CompoundTag.
     *
     * @return The number of entries in this compound.
     */
    public int size() {
        return tagMap.size();
    }

    @Override
    public TagType getType() {
        return TagType.COMPOUND;
    }

    @Override
    public CompoundTag copy() {
        // Deep copy each value
        Map<String, Tag> copyMap = new HashMap<>();
        for (Map.Entry<String, Tag> entry : tagMap.entrySet()) {
            copyMap.put(entry.getKey(), entry.getValue().copy());
        }
        return new CompoundTag(copyMap);
    }

    public Map<String, Tag> copyMap() {
        return new HashMap<>(tagMap);
    }


    @Override
    public String toString() {
        StringBuilder stringbuilder = new StringBuilder("{");
        Collection<String> collection = this.tagMap.keySet();

        for (String s : collection) {
            if (stringbuilder.length() != 1) {
                stringbuilder.append(',');
            }

            stringbuilder.append(match(s)).append(':').append(this.tagMap.get(s));
        }

        return stringbuilder.append('}').toString();
    }

    private String match(String key) {
        if (PATTERN.matcher(key).matches()) return key;

        StringBuilder stringbuilder = new StringBuilder("\"");

        for (int i = 0; i < key.length(); ++i) {
            char c0 = key.charAt(i);

            if (c0 == '\\' || c0 == '"') {
                stringbuilder.append('\\');
            }

            stringbuilder.append(c0);
        }

        return stringbuilder.append('"').toString();
    }


    // --- BULK QOL METHODS --- //


    /**
     * Stores a new NBTTagByte with the given byte value into the map with the given string key.
     */
    public CompoundTag setByte(String key, byte value) {
        this.tagMap.put(key, new ByteTag(value));
        return this;
    }

    /**
     * Stores a new NBTTagShort with the given short value into the map with the given string key.
     */
    public CompoundTag setShort(String key, short value) {
        this.tagMap.put(key, new ShortTag(value));
        return this;
    }

    /**
     * Stores a new NBTTagInt with the given integer value into the map with the given string key.
     */
    public CompoundTag setInteger(String key, int value) {
        this.tagMap.put(key, new IntTag(value));
        return this;
    }

    /**
     * Stores a new NBTTagLong with the given long value into the map with the given string key.
     */
    public CompoundTag setLong(String key, long value) {
        this.tagMap.put(key, new LongTag(value));
        return this;
    }

    public CompoundTag setUniqueId(String key, UUID value) {
        setString(key, value.toString());
        return this;
    }

    public UUID getUniqueId(String key) {
        if (hasKey(key)) {
            String raw = getString(key);
            try {
                return UUID.fromString(raw);
            }catch (IllegalArgumentException ignored) {}
        }

        return UUID.randomUUID();
    }

    /**
     * Stores a new NBTTagFloat with the given float value into the map with the given string key.
     */
    public CompoundTag setFloat(String key, float value) {
        this.tagMap.put(key, new FloatTag(value));
        return this;
    }

    /**
     * Stores a new NBTTagDouble with the given double value into the map with the given string key.
     */
    public CompoundTag setDouble(String key, double value) {
        this.tagMap.put(key, new DoubleTag(value));
        return this;
    }

    /**
     * Stores a new NBTTagString with the given string value into the map with the given string key.
     */
    public CompoundTag setString(String key, String value) {
        this.tagMap.put(key, new StringTag(value));
        return this;
    }

    /**
     * Stores a new NBTTagByteArray with the given array as data into the map with the given string key.
     */
    public CompoundTag setByteArray(String key, byte[] value) {
        this.tagMap.put(key, new ByteArrayTag(value));
        return this;
    }

    /**
     * Stores a new NBTTagIntArray with the given array as data into the map with the given string key.
     */
    public CompoundTag setIntArray(String key, int[] value) {
        this.tagMap.put(key, new IntArrayTag(value));
        return this;
    }

    /**
     * Stores the given boolean value as a NBTTagByte, storing 1 for true and 0 for false, using the given string key.
     */
    public CompoundTag setBoolean(String key, boolean value) {
        tagMap.put(key, new ByteTag((byte) ((value) ? 1 : 0)));
        booleans.add(key);
        return this;
    }

    public CompoundTag setLocation(String key, Location location) {
        CompoundTag compound = new CompoundTag();
        compound.setString("world", location.getWorld().getName());
        compound.setDouble("x", location.getX());
        compound.setDouble("y", location.getY());
        compound.setDouble("z", location.getZ());
        compound.setFloat("yaw", location.getYaw());
        compound.setFloat("pitch", location.getPitch());
        put(key, compound);
        return this;
    }

    public Location getLocation(String key) {
        CompoundTag compound = get(key);
        World world = Bukkit.getWorld(compound.getString("world", "world"));
        double x = compound.getDouble("x", 0);
        double y = compound.getDouble("y", 0);
        double z = compound.getDouble("z", 0);
        float yaw = compound.getFloat("yaw", 0f);
        float pitch = compound.getFloat("pitch", 0f);
        return new Location(world, x, y, z, yaw, pitch);
    }

    public Location getLocation(String key, Location fallback) {
        return (hasKey(key) ? getLocation(key) : fallback);
    }

    public CompoundTag setColor(String key, Color color) {
        return setColor(key, color, StorageColorType.COMPOUND);
    }

    public CompoundTag setColor(String key, Color color, StorageColorType type) {
        switch (type) {
            case HEX:
                setString(key, Colorize.toHex(color.getRed(), color.getGreen(), color.getBlue()));
                break;
            case INT:
                setInteger(key, color.asRGB());
                break;
            case STRING:
                setString(key, color.getRed() + "," + color.getGreen() + "," + color.getBlue());
                break;
            case COMPOUND:
                CompoundTag compound = new CompoundTag();
                compound.setInteger("r", color.getRed());
                compound.setInteger("g", color.getGreen());
                compound.setInteger("b", color.getBlue());
                put(key, compound);
                break;
        }
        return this;
    }

    public Color getColor(String key) {
        return getColor(key, Color.RED);
    }

    public Color getColor(String key, Color fallback) {
        if (!hasKey(key)) return fallback;

        Tag base = get(key);
        // Was saved as a Compound {R,G,B}
        if (base instanceof CompoundTag compound) {
            int r = compound.getInteger("r", 0);
            if (r > 255) r = 255;
            if (r < 0) r = 0;

            int g = compound.getInteger("g", 0);
            if (g > 255) r = 255;
            if (g < 0) r = 0;

            int b = compound.getInteger("b", 0);
            if (b > 255) r = 255;
            if (b < 0) r = 0;

            return Color.fromRGB(r, g, b);
        }

        // Was saved as an int
        if (base instanceof IntTag(int value)) return Color.fromRGB(value);

        if (base instanceof StringTag(String value)) {

            // String is a HEX code
            if (value.startsWith("#")) {
                return Color.fromRGB(
                        Integer.valueOf(value.substring(1, 3), 16),
                        Integer.valueOf(value.substring(3, 5), 16),
                        Integer.valueOf(value.substring(5, 7), 16)
                );
            }

            // String is a split R,G,B
            if (value.contains(",")) {
                String[] args = value.split(",");
                int r = 255, g = 255, b = 255;
                if (args.length >= 3) {
                    r = Integer.parseInt(args[0].trim());
                    g = Integer.parseInt(args[1].trim());
                    b = Integer.parseInt(args[2].trim());
                }
                return Color.fromRGB(r, g, b);
            }

            try {
                // String is a rgb integer, someone added some quotes
                int rgb = Integer.parseInt(value);
                return Color.fromRGB(rgb);
            } catch (NumberFormatException e) {
                return fallback;
            }
        }

        return fallback;
    }

    public CompoundTag setEnum(String key, Enum anEnum) {
        setString(key, anEnum.name());
        return this;
    }

    public <E extends Enum> E getEnum(String key, Class<E> type) {
        return getEnum(key, type, null);
    }

    public <E extends Enum> E getEnum(String key, Class<E> type, E fallback) {
        if (!hasKey(key)) return fallback;
        return (E) E.valueOf(type, getString(key));
    }

    /**
     * Retrieves a byte value using the specified key, or 0 if no such key was stored.
     */
    public byte getByte(String key) {
        Tag storage = this.tagMap.get(key);
        if (storage.getType() == TagType.BYTE) {
            return ((ByteTag) storage).value();
        }
        return 0;
    }

    public byte getByte(String key, byte fallback) {
        return (hasKey(key) ? getByte(key) : fallback);
    }

    /**
     * Retrieves a short value using the specified key, or 0 if no such key was stored.
     */
    public short getShort(String key) {
        return Short.parseShort(getValue(key));
    }

    public short getShort(String key, short fallback) {
        return (hasKey(key) ? getShort(key) : fallback);
    }

    /**
     * Retrieves an integer value using the specified key, or 0 if no such key was stored.
     */
    public int getInteger(String key) {
        return Integer.parseInt(getValue(key));
    }

    public int getInteger(String key, int fallback) {
        return (hasKey(key) ? getInteger(key) : fallback);
    }

    /**
     * Retrieves a long value using the specified key, or 0 if no such key was stored.
     */
    public long getLong(String key) {
        return Long.parseLong(getValue(key));
    }

    public long getLong(String key, long fallback) {
        return (hasKey(key) ? getLong(key) : fallback);
    }

    /**
     * Retrieves a float value using the specified key, or 0 if no such key was stored.
     */
    public float getFloat(String key) {
        return Float.parseFloat(getValue(key));
    }

    public float getFloat(String key, float fallback) {
        return (hasKey(key) ? getFloat(key) : fallback);
    }

    /**
     * Retrieves a double value using the specified key, or 0 if no such key was stored.
     */
    public double getDouble(String key) {
        return Double.parseDouble(getValue(key));
    }

    public double getDouble(String key, double fallback) {
        return (hasKey(key) ? getDouble(key) : fallback);
    }

    /**
     * Retrieves a string value using the specified key, or an empty string if no such key was stored.
     */
    public String getString(String key) {
        return getValue(key);
    }

    public String getString(String key, String fallback) {
        return (hasKey(key) ? getValue(key) : fallback);
    }



    private String getValue(String key) {
        try {
            if (this.hasKey(key)) {
                return fetchValue(tagMap.get(key));
            }
        } catch (ClassCastException ignored) {
        }
        return "";
    }


    private String fetchValue(Tag base) {
        if (base instanceof ByteTag(byte value)) {
            if ((value == 0) || (value == 1))
                return String.valueOf(value == 1);
            return String.valueOf(value);
        }
        if (base instanceof ByteArrayTag(byte[] value))
            return Arrays.toString(value);
        if (base instanceof DoubleTag(double value))
            return String.valueOf(value);
        if (base instanceof FloatTag(float value))
            return String.valueOf(value);
        if (base instanceof IntTag(int value))
            return String.valueOf(value);
        if (base instanceof IntArrayTag(int[] value))
            return Arrays.toString(value);
        if (base instanceof LongTag(long value))
            return String.valueOf(value);

        if (base instanceof ShortTag(short value))
            return String.valueOf(value);


        if (base instanceof StringTag(String value))
            return String.valueOf(value);
        if (base instanceof ListTag list) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                builder.append(fetchValue(list.get(i)));
            }
            return builder.toString();
        }

        return "";
    }
}

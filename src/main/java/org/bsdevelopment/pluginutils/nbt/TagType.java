package org.bsdevelopment.pluginutils.nbt;

/**
 * Enum representing possible NBT tag types.
 * 
 * <p>This can be used for (de)serialization, validation, or switching
 * on the particular subclass of {@link BasicData} you’re dealing with.</p>
 */
public enum TagType {
    END(0),
    BYTE(1),
    SHORT(2),
    INT(3),
    LONG(4),
    FLOAT(5),
    DOUBLE(6),
    BYTE_ARRAY(7),
    STRING(8),
    LIST(9),
    COMPOUND(10),
    INT_ARRAY(11),
    LONG_ARRAY(12);

    private final int id;

    TagType(int id) {
        this.id = id;
    }

    /**
     * Returns the numeric ID used internally by Minecraft for this tag type.
     *
     * @return The numeric ID.
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the corresponding {@link TagType} for a given ID.
     *
     * @param id The numeric tag type ID.
     * @return The matching TagType if found, otherwise null.
     */
    public static TagType fromId(int id) {
        for (TagType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return null; // Or throw an IllegalArgumentException
    }
}

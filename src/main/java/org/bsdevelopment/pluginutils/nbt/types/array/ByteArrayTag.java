package org.bsdevelopment.pluginutils.nbt.types.array;

import org.bsdevelopment.pluginutils.nbt.Tag;
import org.bsdevelopment.pluginutils.nbt.TagType;

import java.util.Arrays;

/**
 * An NBT tag holding a byte array.
 * 
 * <p>Implemented as a record. Note that we create a defensive copy
 * of the array for both construction and copying to ensure immutability.</p>
 *
 * @param value The byte array stored by this tag.
 */
public record ByteArrayTag(byte[] value) implements Tag {

    /**
     * Primary constructor to store a <strong>copy</strong> of the provided array.
     */
    public ByteArrayTag {
        // If null is passed, treat it as an empty array.
        if (value == null) {
            value = new byte[0];
        } else {
            value = Arrays.copyOf(value, value.length);
        }
    }

    @Override
    public TagType getType() {
        return TagType.BYTE_ARRAY;
    }

    @Override
    public ByteArrayTag copy() {
        return new ByteArrayTag(this.value);
    }

    @Override
    public String toString() {
        return "ByteArrayTag(length=" + value.length + ")";
    }
}

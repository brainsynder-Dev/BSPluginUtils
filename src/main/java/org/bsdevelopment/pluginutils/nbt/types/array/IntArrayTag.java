package org.bsdevelopment.pluginutils.nbt.types.array;

import org.bsdevelopment.pluginutils.nbt.Tag;
import org.bsdevelopment.pluginutils.nbt.TagType;

import java.util.Arrays;

/**
 * An NBT tag holding an int array.
 * 
 * <p>Implemented as a record with defensive copying.</p>
 *
 * @param value The int array stored by this tag.
 */
public record IntArrayTag(int[] value) implements Tag {

    /**
     * Primary constructor to store a <strong>copy</strong> of the provided array.
     */
    public IntArrayTag {
        if (value == null) {
            value = new int[0];
        } else {
            value = Arrays.copyOf(value, value.length);
        }
    }

    @Override
    public TagType getType() {
        return TagType.INT_ARRAY;
    }

    @Override
    public IntArrayTag copy() {
        return new IntArrayTag(this.value);
    }

    @Override
    public String toString() {
        return "IntArrayTag(length=" + value.length + ")";
    }
}

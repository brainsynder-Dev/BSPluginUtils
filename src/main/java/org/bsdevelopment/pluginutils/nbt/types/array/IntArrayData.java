package org.bsdevelopment.pluginutils.nbt.types.array;

import org.bsdevelopment.pluginutils.nbt.BasicData;
import org.bsdevelopment.pluginutils.nbt.TagType;

import java.util.Arrays;

/**
 * An NBT tag holding an int array.
 * 
 * <p>Implemented as a record with defensive copying.</p>
 *
 * @param value The int array stored by this tag.
 */
public record IntArrayData(int[] value) implements BasicData {

    /**
     * Primary constructor to store a <strong>copy</strong> of the provided array.
     */
    public IntArrayData {
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
    public IntArrayData copy() {
        return new IntArrayData(this.value);
    }

    @Override
    public String toString() {
        return "IntArrayTag(length=" + value.length + ")";
    }
}

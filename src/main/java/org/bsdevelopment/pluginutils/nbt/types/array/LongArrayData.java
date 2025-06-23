package org.bsdevelopment.pluginutils.nbt.types.array;

import org.bsdevelopment.pluginutils.nbt.BasicData;
import org.bsdevelopment.pluginutils.nbt.TagType;

import java.util.Arrays;

/**
 * An NBT tag holding a long array.
 *
 * <p>Implemented as a record with defensive copying.</p>
 *
 * @param value
 *         The long array stored by this tag.
 */
public record LongArrayData(long[] value) implements BasicData {

    /**
     * Primary constructor to store a <strong>copy</strong> of the provided array.
     */
    public LongArrayData {
        if (value == null) {
            value = new long[0];
        } else {
            value = Arrays.copyOf(value, value.length);
        }
    }

    @Override
    public TagType getType() {
        return TagType.LONG_ARRAY;
    }

    @Override
    public LongArrayData copy() {
        return new LongArrayData(this.value);
    }

    @Override
    public String toString() {
        return "LongArrayTag(length=" + value.length + ")";
    }
}

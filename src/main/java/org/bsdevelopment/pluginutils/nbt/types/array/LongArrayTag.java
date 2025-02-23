package org.bsdevelopment.pluginutils.nbt.types.array;

import org.bsdevelopment.pluginutils.nbt.Tag;
import org.bsdevelopment.pluginutils.nbt.TagType;

import java.util.Arrays;

/**
 * An NBT tag holding a long array.
 * 
 * <p>Implemented as a record with defensive copying.</p>
 *
 * @param value The long array stored by this tag.
 */
public record LongArrayTag(long[] value) implements Tag {

    /**
     * Primary constructor to store a <strong>copy</strong> of the provided array.
     */
    public LongArrayTag {
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
    public LongArrayTag copy() {
        return new LongArrayTag(this.value);
    }

    @Override
    public String toString() {
        return "LongArrayTag(length=" + value.length + ")";
    }
}

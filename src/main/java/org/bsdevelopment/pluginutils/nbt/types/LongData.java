package org.bsdevelopment.pluginutils.nbt.types;

import org.bsdevelopment.pluginutils.nbt.BasicData;
import org.bsdevelopment.pluginutils.nbt.TagType;

/**
 * An NBT tag holding a single long value.
 *
 * <p>Implemented as a record for conciseness.</p>
 *
 * @param value
 *         The long value stored by this tag.
 */
public record LongData(long value) implements BasicData {

    @Override
    public TagType getType() {
        return TagType.LONG;
    }

    @Override
    public LongData copy() {
        return new LongData(this.value);
    }

    @Override
    public String toString() {
        return value + "L";
    }
}

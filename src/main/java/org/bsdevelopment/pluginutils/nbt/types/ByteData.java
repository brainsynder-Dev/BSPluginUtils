package org.bsdevelopment.pluginutils.nbt.types;

import org.bsdevelopment.pluginutils.nbt.BasicData;
import org.bsdevelopment.pluginutils.nbt.TagType;

/**
 * An NBT tag holding a single byte value.
 *
 * <p>Implemented as a record for conciseness.</p>
 *
 * @param value
 *         The byte value stored by this tag.
 */
public record ByteData(byte value) implements BasicData {

    @Override
    public TagType getType() {
        return TagType.BYTE;
    }

    @Override
    public ByteData copy() {
        return new ByteData(this.value);
    }

    @Override
    public String toString() {
        return value + "b";
    }
}

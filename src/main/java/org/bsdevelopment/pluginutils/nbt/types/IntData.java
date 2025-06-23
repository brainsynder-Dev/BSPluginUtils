package org.bsdevelopment.pluginutils.nbt.types;

import org.bsdevelopment.pluginutils.nbt.BasicData;
import org.bsdevelopment.pluginutils.nbt.TagType;

/**
 * An NBT tag holding a single int value.
 *
 * <p>Implemented as a record for conciseness.</p>
 *
 * @param value
 *         The int value stored by this tag.
 */
public record IntData(int value) implements BasicData {

    @Override
    public TagType getType() {
        return TagType.INT;
    }

    @Override
    public IntData copy() {
        return new IntData(this.value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}

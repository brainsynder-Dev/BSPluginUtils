package org.bsdevelopment.pluginutils.nbt.types;

import org.bsdevelopment.pluginutils.nbt.BasicData;
import org.bsdevelopment.pluginutils.nbt.TagType;

/**
 * An NBT tag holding a single short value.
 *
 * <p>Implemented as a record for conciseness.</p>
 *
 * @param value
 *         The short value stored by this tag.
 */
public record ShortData(short value) implements BasicData {

    @Override
    public TagType getType() {
        return TagType.SHORT;
    }

    @Override
    public ShortData copy() {
        return new ShortData(this.value);
    }

    @Override
    public String toString() {
        return value + "s";
    }
}

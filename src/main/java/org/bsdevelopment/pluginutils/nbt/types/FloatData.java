package org.bsdevelopment.pluginutils.nbt.types;

import org.bsdevelopment.pluginutils.nbt.BasicData;
import org.bsdevelopment.pluginutils.nbt.TagType;

/**
 * An NBT tag holding a single float value.
 * 
 * <p>Implemented as a record for conciseness.</p>
 *
 * @param value The float value stored by this tag.
 */
public record FloatData(float value) implements BasicData {

    @Override
    public TagType getType() {
        return TagType.FLOAT;
    }

    @Override
    public FloatData copy() {
        return new FloatData(this.value);
    }

    @Override
    public String toString() {
        return value + "f";
    }
}

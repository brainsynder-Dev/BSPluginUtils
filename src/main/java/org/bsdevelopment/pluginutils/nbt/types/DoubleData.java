package org.bsdevelopment.pluginutils.nbt.types;

import org.bsdevelopment.pluginutils.nbt.BasicData;
import org.bsdevelopment.pluginutils.nbt.TagType;

/**
 * An NBT tag holding a single double value.
 * 
 * <p>Implemented as a record for conciseness.</p>
 *
 * @param value The double value stored by this tag.
 */
public record DoubleData(double value) implements BasicData {

    @Override
    public TagType getType() {
        return TagType.DOUBLE;
    }

    @Override
    public DoubleData copy() {
        return new DoubleData(this.value);
    }

    @Override
    public String toString() {
        return value + "d";
    }
}

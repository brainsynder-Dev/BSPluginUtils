package org.bsdevelopment.pluginutils.nbt.types;

import org.bsdevelopment.pluginutils.nbt.Tag;
import org.bsdevelopment.pluginutils.nbt.TagType;

/**
 * An NBT tag holding a single double value.
 * 
 * <p>Implemented as a record for conciseness.</p>
 *
 * @param value The double value stored by this tag.
 */
public record DoubleTag(double value) implements Tag {

    @Override
    public TagType getType() {
        return TagType.DOUBLE;
    }

    @Override
    public DoubleTag copy() {
        return new DoubleTag(this.value);
    }

    @Override
    public String toString() {
        return value + "d";
    }
}

package org.bsdevelopment.pluginutils.nbt.types;

import org.bsdevelopment.pluginutils.nbt.Tag;
import org.bsdevelopment.pluginutils.nbt.TagType;

/**
 * An NBT tag holding a single float value.
 * 
 * <p>Implemented as a record for conciseness.</p>
 *
 * @param value The float value stored by this tag.
 */
public record FloatTag(float value) implements Tag {

    @Override
    public TagType getType() {
        return TagType.FLOAT;
    }

    @Override
    public FloatTag copy() {
        return new FloatTag(this.value);
    }

    @Override
    public String toString() {
        return value + "f";
    }
}

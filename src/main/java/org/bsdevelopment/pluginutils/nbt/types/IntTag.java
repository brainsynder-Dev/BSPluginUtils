package org.bsdevelopment.pluginutils.nbt.types;

import org.bsdevelopment.pluginutils.nbt.Tag;
import org.bsdevelopment.pluginutils.nbt.TagType;

/**
 * An NBT tag holding a single int value.
 * 
 * <p>Implemented as a record for conciseness.</p>
 *
 * @param value The int value stored by this tag.
 */
public record IntTag(int value) implements Tag {

    @Override
    public TagType getType() {
        return TagType.INT;
    }

    @Override
    public IntTag copy() {
        return new IntTag(this.value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}

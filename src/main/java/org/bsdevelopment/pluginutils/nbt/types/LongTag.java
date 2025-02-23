package org.bsdevelopment.pluginutils.nbt.types;

import org.bsdevelopment.pluginutils.nbt.Tag;
import org.bsdevelopment.pluginutils.nbt.TagType;

/**
 * An NBT tag holding a single long value.
 * 
 * <p>Implemented as a record for conciseness.</p>
 *
 * @param value The long value stored by this tag.
 */
public record LongTag(long value) implements Tag {

    @Override
    public TagType getType() {
        return TagType.LONG;
    }

    @Override
    public LongTag copy() {
        return new LongTag(this.value);
    }

    @Override
    public String toString() {
        return value + "L";
    }
}

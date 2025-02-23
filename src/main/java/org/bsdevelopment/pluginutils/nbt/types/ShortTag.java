package org.bsdevelopment.pluginutils.nbt.types;

import org.bsdevelopment.pluginutils.nbt.Tag;
import org.bsdevelopment.pluginutils.nbt.TagType;

/**
 * An NBT tag holding a single short value.
 * 
 * <p>Implemented as a record for conciseness.</p>
 *
 * @param value The short value stored by this tag.
 */
public record ShortTag(short value) implements Tag {

    @Override
    public TagType getType() {
        return TagType.SHORT;
    }

    @Override
    public ShortTag copy() {
        return new ShortTag(this.value);
    }

    @Override
    public String toString() {
        return value + "s";
    }
}

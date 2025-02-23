package org.bsdevelopment.pluginutils.nbt.types;

import org.bsdevelopment.pluginutils.nbt.Tag;
import org.bsdevelopment.pluginutils.nbt.TagType;

/**
 * An NBT tag holding a single byte value.
 * 
 * <p>Implemented as a record for conciseness.</p>
 *
 * @param value The byte value stored by this tag.
 */
public record ByteTag(byte value) implements Tag {

    @Override
    public TagType getType() {
        return TagType.BYTE;
    }

    @Override
    public ByteTag copy() {
        // Byte is primitive, so a copy is just a new record with the same value
        return new ByteTag(this.value);
    }

    @Override
    public String toString() {
        return value + "b";
    }
}

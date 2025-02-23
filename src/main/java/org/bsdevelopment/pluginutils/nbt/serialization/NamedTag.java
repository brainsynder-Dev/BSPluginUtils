package org.bsdevelopment.pluginutils.nbt.serialization;

import org.bsdevelopment.pluginutils.nbt.Tag;

/**
 * Represents a named NBT tag (like how they're stored on disk: type + name + payload).
 *
 * @param name The name of this tag.
 * @param tag The actual NBT tag data.
 */
public record NamedTag(String name, Tag tag) {
}

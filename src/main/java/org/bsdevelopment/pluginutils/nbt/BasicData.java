package org.bsdevelopment.pluginutils.nbt;

/**
 * Represents a generic NBT tag interface.
 *
 * <p>This interface defines the common methods all NBT tag types
 * should support.</p>
 */
public interface BasicData {

    /**
     * Returns the type of this tag, corresponding to an entry in {@link TagType}.
     *
     * @return The {@link TagType} for this tag.
     */
    TagType getType();

    /**
     * Creates a deep copy of this tag. Each tag should implement
     * clone logic that returns a structurally identical but separate object.
     *
     * @return A deep copy of this tag.
     */
    BasicData copy();
}

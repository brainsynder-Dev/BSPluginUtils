package org.bsdevelopment.pluginutils.nbt.types;

import org.bsdevelopment.pluginutils.nbt.Tag;
import org.bsdevelopment.pluginutils.nbt.TagType;

/**
 * A special tag type that signifies the end of a compound (in some contexts).
 * 
 * <p>In Minecraft, EndTag often serves as a sentinel value. It typically doesn't
 * hold data and is not stored in normal user data structures.</p>
 */
public final class EndTag implements Tag {
    // Typically, there's only one instance of EndTag in real usage
    public static final EndTag INSTANCE = new EndTag();

    private EndTag() {}

    @Override
    public TagType getType() {
        return TagType.END;
    }

    @Override
    public Tag copy() {
        // Return the same instance; it holds no data
        return INSTANCE;
    }

    @Override
    public String toString() {
        return "EndTag";
    }
}

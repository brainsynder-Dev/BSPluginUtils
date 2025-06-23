package org.bsdevelopment.pluginutils.nbt.types;

import org.bsdevelopment.pluginutils.nbt.BasicData;
import org.bsdevelopment.pluginutils.nbt.TagType;

/**
 * A special tag type that signifies the end of a compound (in some contexts).
 *
 * <p>In Minecraft, EndTag often serves as a sentinel value. It typically doesn't
 * hold data and is not stored in normal user data structures.</p>
 */
public final class EndData implements BasicData {
    // Typically, there's only one instance of EndTag in real usage
    public static final EndData INSTANCE = new EndData();

    private EndData() {
    }

    @Override
    public TagType getType() {
        return TagType.END;
    }

    @Override
    public BasicData copy() {
        // Return the same instance; it holds no data
        return INSTANCE;
    }

    @Override
    public String toString() {
        return "EndTag";
    }
}

package org.bsdevelopment.pluginutils.nbt.types;

import org.bsdevelopment.pluginutils.nbt.BasicData;
import org.bsdevelopment.pluginutils.nbt.TagType;

/**
 * An NBT tag holding a single string value.
 *
 * <p>Implemented as a record for conciseness. In Minecraft, an empty
 * string is often used in place of null.</p>
 *
 * @param value
 *         The string value stored by this tag (never null).
 */
public record StringData(String value) implements BasicData {

    /**
     * Constructs a new StringTag with the specified string value.
     * An empty string is used if null is passed.
     */
    public StringData {
        if (value == null) {
            value = "";
        }
    }

    @Override
    public TagType getType() {
        return TagType.STRING;
    }

    @Override
    public StringData copy() {
        return new StringData(this.value);
    }

    @Override
    public String toString() {
        StringBuilder stringbuilder = new StringBuilder("\"");

        for (int i = 0; i < value.length(); ++i) {
            char c0 = value.charAt(i);

            if (c0 == '\\' || c0 == '"') {
                stringbuilder.append('\\');
            }

            stringbuilder.append(c0);
        }

        return stringbuilder.append('"').toString();
    }
}

package org.bsdevelopment.pluginutils.nbt.types;

import org.bsdevelopment.pluginutils.nbt.BasicData;
import org.bsdevelopment.pluginutils.nbt.TagType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * An NBT tag that holds a list of tags, all of the same type.
 *
 * <p>Minecraft's ListTag requires that all elements be of the same type,
 * though some implementations may relax this requirement.</p>
 */
public final class ListData implements BasicData {

    private final LinkedList<BasicData> value;
    private final TagType elementType;

    /**
     * Creates an empty ListTag that is <em>not</em> restricted to a specific element type yet.
     * It will become restricted once the first element is added.
     */
    public ListData() {
        this.value = new LinkedList<>();
        this.elementType = TagType.END; // sentinel that means "no elements yet"
    }

    /**
     * Creates a new ListTag with a predefined element type and an initial list of tags.
     *
     * @param elementType
     *         The element type for all tags in this list.
     * @param initialTags
     *         The tags to store in this list. Must all match elementType.
     */
    public ListData(TagType elementType, List<BasicData> initialTags) {
        if (elementType == null) {
            throw new IllegalArgumentException("elementType cannot be null");
        }
        this.elementType = elementType;
        // Validate that all tags match the given elementType
        for (BasicData tag : initialTags) {
            if (tag.getType() != elementType && elementType != TagType.END) {
                throw new IllegalArgumentException("All tags must match the element type: " + elementType);
            }
        }
        // Defensive copy
        this.value = new LinkedList<>(initialTags);
    }

    /**
     * Adds a tag to the end of this list. If this list was previously empty,
     * we set the element type based on the new tag.
     *
     * @param tag
     *         The tag to add.
     */
    public ListData add(BasicData tag) {
        if (tag == null) throw new IllegalArgumentException("Cannot add null tag to ListTag");

//        if (this.elementType == TagType.END) {
//            // If we had no element type, adopt the new tag's type
//            // This is how Minecraft does it once you add the first element
//        } else if (tag.getType() != elementType) {
//            throw new IllegalArgumentException("Tag type " + tag.getType() +
//                    " does not match existing element type " + elementType);
//        }
        this.value.add(tag);
        return this;
    }

    /**
     * Returns the tag at the specified index in this list.
     *
     * @param index
     *         The index of the desired tag.
     *
     * @return The tag at the given index.
     */
    public BasicData get(int index) {
        return value.get(index);
    }

    /**
     * Removes the tag at the specified index.
     *
     * @param index
     *         The index of the tag to remove.
     *
     * @return The removed tag.
     */
    public BasicData remove(int index) {
        return value.remove(index);
    }

    /**
     * Returns the number of tags in this list.
     *
     * @return The size of the underlying list.
     */
    public int size() {
        return value.size();
    }

    /**
     * Returns an unmodifiable view of the underlying list of tags.
     *
     * @return An unmodifiable list of tags.
     */
    public List<BasicData> getValue() {
        return Collections.unmodifiableList(value);
    }

    /**
     * Returns the type of the tags stored in this ListTag.
     *
     * @return A {@link TagType} representing the element type.
     */
    public TagType getElementType() {
        return elementType;
    }

    @Override
    public TagType getType() {
        return TagType.LIST;
    }

    @Override
    public ListData copy() {
        List<BasicData> copiedList = new ArrayList<>(value.size());
        for (BasicData tag : value) {
            copiedList.add(tag.copy());
        }
        // Return a new ListTag with the same element type
        return new ListData(elementType, copiedList);
    }

    @Override
    public String toString() {
        StringBuilder stringbuilder = new StringBuilder("[");

        for (int i = 0; i < this.value.size(); ++i) {
            if (i != 0) {
                stringbuilder.append(',');
            }

            stringbuilder.append(this.value.get(i));
        }

        return stringbuilder.append(']').toString();
    }
}

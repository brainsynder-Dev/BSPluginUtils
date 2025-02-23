package org.bsdevelopment.pluginutils.nbt.serialization;

import org.bsdevelopment.pluginutils.nbt.Tag;
import org.bsdevelopment.pluginutils.nbt.TagType;
import org.bsdevelopment.pluginutils.nbt.types.*;
import org.bsdevelopment.pluginutils.nbt.types.array.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A stream for reading NBT tags from a binary input source.
 *
 * <p>Follows the typical NBT format:
 * <ul>
 *   <li>1 byte: TagType ID</li>
 *   <li>For named tags (TagType != END): 2-byte name length (unsigned short), followed by bytes for the name</li>
 *   <li>Payload depends on tag type</li>
 * </ul>
 */
public class NBTInputStream implements AutoCloseable {

    private final DataInputStream dataIn;

    /**
     * Constructs a new NBTInputStream that reads from the given InputStream.
     * 
     * @param inputStream The InputStream to read from.
     */
    public NBTInputStream(InputStream inputStream) {
        this.dataIn = new DataInputStream(inputStream);
    }

    /**
     * Reads an entire named tag from the stream (type ID + name + payload).
     * This is usually how you'd read the "root" tag in an NBT file.
     * 
     * @return A Tag read from the stream, or null if TagType is END (which can occur in certain contexts).
     * @throws IOException If an I/O error occurs.
     */
    public NamedTag readNamedTag() throws IOException {
        byte typeId = dataIn.readByte();  
        TagType type = TagType.fromId(typeId);

        if (type == null) {
            throw new IOException("Unknown tag type ID: " + typeId);
        }
        if (type == TagType.END) {
            // No name or payload for END tag
            return new NamedTag("", EndTag.INSTANCE);
        }

        // Read the name (2-byte length + UTF-8 bytes)
        String name = readString();
        
        // Read the payload
        Tag tag = readTagPayload(type);
        return new NamedTag(name, tag);
    }

    /**
     * Reads a tag payload of the given type (excluding name).
     * 
     * @param type The type of the tag to read.
     * @return A Tag instance (matching the type).
     * @throws IOException If an I/O error occurs or if the format is invalid.
     */
    private Tag readTagPayload(TagType type) throws IOException {
        return switch (type) {
            case END -> EndTag.INSTANCE; 
            case BYTE -> new ByteTag(dataIn.readByte());
            case SHORT -> new ShortTag(dataIn.readShort());
            case INT -> new IntTag(dataIn.readInt());
            case LONG -> new LongTag(dataIn.readLong());
            case FLOAT -> new FloatTag(dataIn.readFloat());
            case DOUBLE -> new DoubleTag(dataIn.readDouble());
            case BYTE_ARRAY -> readByteArrayTag();
            case STRING -> new StringTag(readString());
            case LIST -> readListTag();
            case COMPOUND -> readCompoundTag();
            case INT_ARRAY -> readIntArrayTag();
            case LONG_ARRAY -> readLongArrayTag();
        };
    }

    /**
     * Reads a string (2-byte length, then UTF-8 data).
     * 
     * @return The decoded string.
     * @throws IOException If an I/O error occurs.
     */
    private String readString() throws IOException {
        int length = dataIn.readUnsignedShort();
        byte[] bytes = new byte[length];
        dataIn.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Reads a ByteArrayTag.
     */
    private ByteArrayTag readByteArrayTag() throws IOException {
        int length = dataIn.readInt();  // The array length
        byte[] bytes = new byte[length];
        dataIn.readFully(bytes);
        return new ByteArrayTag(bytes);
    }

    /**
     * Reads an IntArrayTag.
     */
    private IntArrayTag readIntArrayTag() throws IOException {
        int length = dataIn.readInt(); 
        int[] ints = new int[length];
        for (int i = 0; i < length; i++) {
            ints[i] = dataIn.readInt();
        }
        return new IntArrayTag(ints);
    }

    /**
     * Reads a LongArrayTag.
     */
    private LongArrayTag readLongArrayTag() throws IOException {
        int length = dataIn.readInt();
        long[] longs = new long[length];
        for (int i = 0; i < length; i++) {
            longs[i] = dataIn.readLong();
        }
        return new LongArrayTag(longs);
    }

    /**
     * Reads a ListTag, which includes:
     * <ul>
     *   <li>1 byte: element type ID</li>
     *   <li>4 bytes: length</li>
     *   <li>That many elements, each read with {@code readTagPayload(elementType)}</li>
     * </ul>
     */
    private ListTag readListTag() throws IOException {
        byte elementTypeId = dataIn.readByte();
        TagType elementType = TagType.fromId(elementTypeId);
        int length = dataIn.readInt();

        if (elementType == null) {
            throw new IOException("Unknown element type in ListTag: " + elementTypeId);
        }

        List<Tag> listData = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            Tag element = readTagPayload(elementType);
            listData.add(element);
        }
        // Use the specialized constructor from the example that requires element type
        return new ListTag(elementType, listData);
    }

    /**
     * Reads a CompoundTag, which is a series of named tags until an END tag is encountered.
     */
    private CompoundTag readCompoundTag() throws IOException {
        Map<String, Tag> tagMap = new HashMap<>();
        
        while (true) {
            byte typeId = dataIn.readByte();
            TagType innerType = TagType.fromId(typeId);
            if (innerType == null) {
                throw new IOException("Unknown tag type in Compound: " + typeId);
            }
            if (innerType == TagType.END) {
                // End of compound
                break;
            }
            String name = readString();
            Tag payload = readTagPayload(innerType);
            tagMap.put(name, payload);
        }
        
        return new CompoundTag(tagMap);
    }

    @Override
    public void close() throws IOException {
        dataIn.close();
    }
}

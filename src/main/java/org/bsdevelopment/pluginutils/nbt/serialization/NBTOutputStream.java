package org.bsdevelopment.pluginutils.nbt.serialization;

import org.bsdevelopment.pluginutils.nbt.Tag;
import org.bsdevelopment.pluginutils.nbt.TagType;
import org.bsdevelopment.pluginutils.nbt.types.*;
import org.bsdevelopment.pluginutils.nbt.types.array.*;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * A stream for writing NBT tags to a binary output source.
 *
 * <p>Follows the typical NBT format:
 * <ul>
 *   <li>1 byte: TagType ID</li>
 *   <li>For named tags (TagType != END): 2-byte name length (unsigned short), followed by bytes for the name</li>
 *   <li>Payload depends on tag type</li>
 * </ul>
 */
public class NBTOutputStream implements AutoCloseable {

    private final DataOutputStream dataOut;

    /**
     * Constructs a new NBTOutputStream that writes to the given OutputStream.
     * 
     * @param outputStream The OutputStream to write to.
     */
    public NBTOutputStream(OutputStream outputStream) {
        this.dataOut = new DataOutputStream(outputStream);
    }

    /**
     * Writes a named tag (type, name, payload) to the stream.
     * 
     * @param namedTag The NamedTag object containing a name and a Tag.
     * @throws IOException If an I/O error occurs.
     */
    public void writeNamedTag(NamedTag namedTag) throws IOException {
        TagType type = namedTag.tag().getType();
        dataOut.writeByte(type.getId());
        if (type != TagType.END) {
            writeString(namedTag.name());
            writeTagPayload(namedTag.tag());
        }
    }

    /**
     * Writes only the payload (no type ID, no name).
     * 
     * @param tag The tag to write.
     * @throws IOException If an I/O error occurs.
     */
    private void writeTagPayload(Tag tag) throws IOException {
        switch (tag.getType()) {
            case END -> {
                // EndTag has no payload
            }
            case BYTE -> {
                ByteTag byteTag = (ByteTag) tag;
                dataOut.writeByte(byteTag.value());
            }
            case SHORT -> {
                ShortTag shortTag = (ShortTag) tag;
                dataOut.writeShort(shortTag.value());
            }
            case INT -> {
                IntTag intTag = (IntTag) tag;
                dataOut.writeInt(intTag.value());
            }
            case LONG -> {
                LongTag longTag = (LongTag) tag;
                dataOut.writeLong(longTag.value());
            }
            case FLOAT -> {
                FloatTag floatTag = (FloatTag) tag;
                dataOut.writeFloat(floatTag.value());
            }
            case DOUBLE -> {
                DoubleTag doubleTag = (DoubleTag) tag;
                dataOut.writeDouble(doubleTag.value());
            }
            case BYTE_ARRAY -> {
                ByteArrayTag baTag = (ByteArrayTag) tag;
                dataOut.writeInt(baTag.value().length);
                dataOut.write(baTag.value());
            }
            case STRING -> {
                StringTag strTag = (StringTag) tag;
                writeString(strTag.value());
            }
            case LIST -> {
                ListTag listTag = (ListTag) tag;
                TagType elemType = listTag.getElementType();
                dataOut.writeByte(elemType.getId());
                dataOut.writeInt(listTag.size());
                for (Tag elem : listTag.getValue()) {
                    // Write just the payload of each element (List doesn't store names)
                    writeTagPayload(elem);
                }
            }
            case COMPOUND -> {
                CompoundTag compoundTag = (CompoundTag) tag;
                // Write each named sub-tag until we finish, then write END
                for (Map.Entry<String, Tag> entry : compoundTag.copyMap().entrySet()) {
                    Tag subTag = entry.getValue();
                    TagType subType = subTag.getType();
                    dataOut.writeByte(subType.getId());
                    if (subType != TagType.END) {
                        writeString(entry.getKey());
                        writeTagPayload(subTag);
                    }
                }
                // Finally, write an END tag to close the compound
                dataOut.writeByte(TagType.END.getId());
            }
            case INT_ARRAY -> {
                IntArrayTag intArrayTag = (IntArrayTag) tag;
                dataOut.writeInt(intArrayTag.value().length);
                for (int i : intArrayTag.value()) {
                    dataOut.writeInt(i);
                }
            }
            case LONG_ARRAY -> {
                LongArrayTag longArrayTag = (LongArrayTag) tag;
                dataOut.writeInt(longArrayTag.value().length);
                for (long l : longArrayTag.value()) {
                    dataOut.writeLong(l);
                }
            }
        }
    }

    /**
     * Writes a string to the stream: unsigned short length + UTF-8 bytes.
     *
     * @param str The string to write.
     * @throws IOException If an I/O error occurs.
     */
    private void writeString(String str) throws IOException {
        if (str == null) {
            str = "";
        }
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        dataOut.writeShort(bytes.length);
        dataOut.write(bytes);
    }

    @Override
    public void close() throws IOException {
        dataOut.close();
    }
}

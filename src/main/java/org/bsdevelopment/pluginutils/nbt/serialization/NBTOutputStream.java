package org.bsdevelopment.pluginutils.nbt.serialization;

import org.bsdevelopment.pluginutils.nbt.BasicData;
import org.bsdevelopment.pluginutils.nbt.TagType;
import org.bsdevelopment.pluginutils.nbt.types.ByteData;
import org.bsdevelopment.pluginutils.nbt.types.CompoundData;
import org.bsdevelopment.pluginutils.nbt.types.DoubleData;
import org.bsdevelopment.pluginutils.nbt.types.FloatData;
import org.bsdevelopment.pluginutils.nbt.types.IntData;
import org.bsdevelopment.pluginutils.nbt.types.ListData;
import org.bsdevelopment.pluginutils.nbt.types.LongData;
import org.bsdevelopment.pluginutils.nbt.types.ShortData;
import org.bsdevelopment.pluginutils.nbt.types.StringData;
import org.bsdevelopment.pluginutils.nbt.types.array.ByteArrayData;
import org.bsdevelopment.pluginutils.nbt.types.array.IntArrayData;
import org.bsdevelopment.pluginutils.nbt.types.array.LongArrayData;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * A stream for writing a single root NBT tag (type, empty name, and payload)
 * to an OutputStream in standard NBT format.
 *
 * <p>We write an empty string for the root name to comply with the spec,
 * but then ignore it when reading.</p>
 */
public class NBTOutputStream implements AutoCloseable {

    private final DataOutputStream dataOut;

    /**
     * Constructs a new NBTOutputStream that writes to the given OutputStream.
     *
     * @param outputStream the underlying output stream.
     */
    public NBTOutputStream(OutputStream outputStream) {
        this.dataOut = new DataOutputStream(outputStream);
    }

    /**
     * Writes the root data to the stream.
     * The root data is written with an empty name.
     *
     * @param data the root Data.
     * @throws IOException if an I/O error occurs.
     */
    public void writeRootTag(BasicData data) throws IOException {
        TagType type = data.getType();
        dataOut.writeByte(type.getId());
        if (type != TagType.END) {
            // Write an empty name (length 0)
            dataOut.writeShort(0);
            writeTagPayload(data);
        }
    }

    // Writes the payload for a given data (no name).
    private void writeTagPayload(BasicData data) throws IOException {
        switch (data.getType()) {
            case END -> { /* no payload */ }
            case BYTE -> dataOut.writeByte(((ByteData) data).value());
            case SHORT -> dataOut.writeShort(((ShortData) data).value());
            case INT -> dataOut.writeInt(((IntData) data).value());
            case LONG -> dataOut.writeLong(((LongData) data).value());
            case FLOAT -> dataOut.writeFloat(((FloatData) data).value());
            case DOUBLE -> dataOut.writeDouble(((DoubleData) data).value());
            case STRING -> writeString(((StringData) data).value());
            case BYTE_ARRAY -> {
                ByteArrayData arrayData = (ByteArrayData) data;
                dataOut.writeInt(arrayData.value().length);
                dataOut.write(arrayData.value());
            }
            case INT_ARRAY -> {
                IntArrayData arrayData = (IntArrayData) data;
                dataOut.writeInt(arrayData.value().length);
                for (int v : arrayData.value()) {
                    dataOut.writeInt(v);
                }
            }
            case LONG_ARRAY -> {
                LongArrayData arrayData = (LongArrayData) data;
                dataOut.writeInt(arrayData.value().length);
                for (long v : arrayData.value()) {
                    dataOut.writeLong(v);
                }
            }
            case LIST -> {
                ListData listData = (ListData) data;
                dataOut.writeByte(listData.getElementType().getId());
                dataOut.writeInt(listData.size());
                for (BasicData elem : listData.getValue()) {
                    writeTagPayload(elem);
                }
            }
            case COMPOUND -> {
                CompoundData compound = (CompoundData) data;
                for (Map.Entry<String, BasicData> entry : compound.copyMap().entrySet()) {
                    BasicData subData = entry.getValue();
                    dataOut.writeByte(subData.getType().getId());
                    if (subData.getType() != TagType.END) {
                        writeString(entry.getKey());
                        writeTagPayload(subData);
                    }
                }
                // Write END to close the compound.
                dataOut.writeByte(TagType.END.getId());
            }
        }
    }

    // Writes a string as a 2-byte length followed by UTF-8 bytes.
    private void writeString(String string) throws IOException {
        if (string == null) string = "";

        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        dataOut.writeShort(bytes.length);
        dataOut.write(bytes);
    }

    @Override
    public void close() throws IOException {
        dataOut.close();
    }
}

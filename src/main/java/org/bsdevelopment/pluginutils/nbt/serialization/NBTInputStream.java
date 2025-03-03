package org.bsdevelopment.pluginutils.nbt.serialization;

import org.bsdevelopment.pluginutils.nbt.BasicData;
import org.bsdevelopment.pluginutils.nbt.TagType;
import org.bsdevelopment.pluginutils.nbt.types.ByteData;
import org.bsdevelopment.pluginutils.nbt.types.CompoundData;
import org.bsdevelopment.pluginutils.nbt.types.DoubleData;
import org.bsdevelopment.pluginutils.nbt.types.EndData;
import org.bsdevelopment.pluginutils.nbt.types.FloatData;
import org.bsdevelopment.pluginutils.nbt.types.IntData;
import org.bsdevelopment.pluginutils.nbt.types.ListData;
import org.bsdevelopment.pluginutils.nbt.types.LongData;
import org.bsdevelopment.pluginutils.nbt.types.ShortData;
import org.bsdevelopment.pluginutils.nbt.types.StringData;
import org.bsdevelopment.pluginutils.nbt.types.array.ByteArrayData;
import org.bsdevelopment.pluginutils.nbt.types.array.IntArrayData;
import org.bsdevelopment.pluginutils.nbt.types.array.LongArrayData;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A stream for reading a single NBT tag (the root or any nested tag)
 * from a binary input source in standard NBT format.
 *
 * <p>The root Data is stored with:
 * <ul>
 *   <li>1 byte: TagType ID</li>
 *   <li>2 bytes: name length (unsigned short) + name bytes (UTF-8) – which we discard</li>
 *   <li>Payload: depends on data type</li>
 * </ul>
 * If the type is END (0), there is no name or payload.
 */
public class NBTInputStream implements AutoCloseable {
    private final DataInputStream dataIn;

    /**
     * Constructs a new NBTInputStream that reads from the given InputStream.
     *
     * @param inputStream the underlying input stream.
     */
    public NBTInputStream(InputStream inputStream) {
        this.dataIn = new DataInputStream(inputStream);
    }

    /**
     * Reads the "root" data from the stream.
     * The root name is read and discarded.
     *
     * @return the root Data.
     * @throws IOException if an I/O error occurs or if the data is invalid.
     */
    public BasicData readRootData() throws IOException {
        // Read the type ID
        byte typeId = dataIn.readByte();
        TagType type = TagType.fromId(typeId);
        if (type == null) {
            throw new IOException("Unknown tag type ID at root: " + typeId);
        }
        if (type == TagType.END) {
            return EndData.INSTANCE;
        }

        // Read and discard the name
        int nameLength = dataIn.readUnsignedShort();
        if (nameLength > 0) {
            dataIn.skipBytes(nameLength);
        }

        // Read the payload according to its type
        return readDataPayload(type);
    }

    // Reads the payload for a given tag type (no name).
    private BasicData readDataPayload(TagType type) throws IOException {
        return switch (type) {
            case END -> EndData.INSTANCE;
            case BYTE -> new ByteData(dataIn.readByte());
            case SHORT -> new ShortData(dataIn.readShort());
            case INT -> new IntData(dataIn.readInt());
            case LONG -> new LongData(dataIn.readLong());
            case FLOAT -> new FloatData(dataIn.readFloat());
            case DOUBLE -> new DoubleData(dataIn.readDouble());
            case BYTE_ARRAY -> readByteArrayData();
            case STRING -> new StringData(readString());
            case LIST -> readListData();
            case COMPOUND -> readCompoundData();
            case INT_ARRAY -> readIntArrayData();
            case LONG_ARRAY -> readLongArrayData();
        };
    }

    // Reads a UTF-8 string (2-byte length then bytes).
    private String readString() throws IOException {
        int length = dataIn.readUnsignedShort();
        byte[] bytes = new byte[length];
        dataIn.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    // Reads a ByteArrayData.
    private ByteArrayData readByteArrayData() throws IOException {
        int length = dataIn.readInt();
        byte[] bytes = new byte[length];
        dataIn.readFully(bytes);
        return new ByteArrayData(bytes);
    }

    // Reads an IntArrayData.
    private IntArrayData readIntArrayData() throws IOException {
        int length = dataIn.readInt();
        int[] ints = new int[length];
        for (int i = 0; i < length; i++) {
            ints[i] = dataIn.readInt();
        }
        return new IntArrayData(ints);
    }

    // Reads a LongArrayData.
    private LongArrayData readLongArrayData() throws IOException {
        int length = dataIn.readInt();
        long[] longs = new long[length];
        for (int i = 0; i < length; i++) {
            longs[i] = dataIn.readLong();
        }
        return new LongArrayData(longs);
    }

    // Reads a ListData.
    private ListData readListData() throws IOException {
        byte elementTypeId = dataIn.readByte();
        TagType elementType = TagType.fromId(elementTypeId);
        if (elementType == null) {
            throw new IOException("Unknown element type in ListData: " + elementTypeId);
        }
        int length = dataIn.readInt();
        List<BasicData> listData = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            listData.add(readDataPayload(elementType));
        }
        return new ListData(elementType, listData);
    }

    // Reads a CompoundData.
    private CompoundData readCompoundData() throws IOException {
        Map<String, BasicData> dataMap = new HashMap<>();
        while (true) {
            byte typeId = dataIn.readByte();
            TagType innerType = TagType.fromId(typeId);
            if (innerType == null) {
                throw new IOException("Unknown tag type in Compound: " + typeId);
            }
            if (innerType == TagType.END) {
                break; // End of compound
            }
            String key = readString();
            BasicData payload = readDataPayload(innerType);
            dataMap.put(key, payload);
        }
        return new CompoundData(dataMap);
    }

    @Override
    public void close() throws IOException {
        dataIn.close();
    }
}

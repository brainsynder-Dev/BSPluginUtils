package org.bsdevelopment.pluginutils.nbt;

import org.bsdevelopment.pluginutils.nbt.serialization.NBTInputStream;
import org.bsdevelopment.pluginutils.nbt.serialization.NBTOutputStream;
import org.bsdevelopment.pluginutils.nbt.serialization.NamedTag;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Utility class for reading and writing NBT data from/to files.
 *
 * <p>In Minecraft, NBT files are often (but not always) compressed (GZIP).
 * If you need GZIP, wrap your streams accordingly.</p>
 */
public final class NBTIO {

    private NBTIO() {
        // Utility class; no instantiation
    }

    /**
     * Reads a NamedTag from the given file. Typically this will be a CompoundTag as the root,
     * but it can be any tag type.
     * 
     * @param file The file to read from.
     * @return The NamedTag read from the file, or null if no valid data.
     * @throws IOException If an I/O error or format error occurs.
     */
    public static NamedTag readFromFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             NBTInputStream nbtIn = new NBTInputStream(fis)) {

            return nbtIn.readNamedTag();
        }
    }

    /**
     * Writes a NamedTag to the given file in NBT binary format. Overwrites existing file data.
     *
     * @param namedTag The NamedTag to write.
     * @param file     The file to write to.
     * @throws IOException If an I/O error occurs.
     */
    public static void writeToFile(NamedTag namedTag, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             NBTOutputStream nbtOut = new NBTOutputStream(fos)) {

            nbtOut.writeNamedTag(namedTag);
        }
    }
}

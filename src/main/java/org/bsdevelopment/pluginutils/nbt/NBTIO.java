package org.bsdevelopment.pluginutils.nbt;

import org.bsdevelopment.pluginutils.nbt.serialization.NBTInputStream;
import org.bsdevelopment.pluginutils.nbt.serialization.NBTOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utility class for reading and writing a single root NBT tag to/from files.
 * Supports both uncompressed and GZIP-compressed formats.
 *
 * <p>Note: The root name is written as an empty string.</p>
 */
public final class NBTIO {
    // ----- Uncompressed File I/O -----

    /**
     * Reads the root Data from an uncompressed NBT file.
     *
     * @param file
     *         the file to read from.
     *
     * @return the root Data.
     * @throws IOException
     *         if an I/O error occurs.
     */
    public static BasicData readFromFile(File file) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file);
             NBTInputStream nbtIn = new NBTInputStream(inputStream)) {
            return nbtIn.readRootData();
        }
    }

    /**
     * Writes the root Data to an uncompressed NBT file.
     *
     * @param data
     *         the Data to write.
     * @param file
     *         the file to write to.
     *
     * @throws IOException
     *         if an I/O error occurs.
     */
    public static void writeToFile(BasicData data, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             NBTOutputStream nbtOut = new NBTOutputStream(fos)) {
            nbtOut.writeRootTag(data);
        }
    }

    // ----- GZIP-Compressed File I/O -----

    /**
     * Reads the root Tag from a GZIP-compressed NBT file.
     *
     * @param file
     *         the compressed file to read from.
     *
     * @return the root Data.
     * @throws IOException
     *         if an I/O error occurs.
     */
    public static BasicData readFromCompressedFile(File file) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file);
             GZIPInputStream gzipIn = new GZIPInputStream(inputStream);
             NBTInputStream nbtIn = new NBTInputStream(gzipIn)) {
            return nbtIn.readRootData();
        }
    }

    /**
     * Writes the root Tag to a GZIP-compressed NBT file.
     *
     * @param data
     *         the Data to write.
     * @param file
     *         the file to write to.
     *
     * @throws IOException
     *         if an I/O error occurs.
     */
    public static void writeToCompressedFile(BasicData data, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             GZIPOutputStream gzipOut = new GZIPOutputStream(fos);
             NBTOutputStream nbtOut = new NBTOutputStream(gzipOut)) {
            nbtOut.writeRootTag(data);
        }
    }
}

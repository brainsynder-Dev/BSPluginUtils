package org.bsdevelopment.pluginutils.files;

import org.bsdevelopment.pluginutils.nbt.NBTIO;
import org.bsdevelopment.pluginutils.nbt.types.CompoundData;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * A data file that encapsulates NBT data stored in a file.
 * The file is loaded into a CompoundData instance, and modifications can be saved back to the file.
 *
 * <p><b>Example Usage:</b>
 * <pre>
 * File file = new File("data.nbt");
 * DataFile dataFile = new DataFile(file);
 * dataFile.save();
 * </pre>
 */
public class DataFile extends CompoundData {
    private final File file;

    /**
     * Constructs a new DataFile from the specified file.
     *
     * <p>This constructor ensures that the parent directory and the file exist.
     * It then reads NBT data from the file and copies the data into this CompoundData.
     *
     * <p><b>Example Usage:</b>
     * <pre>
     * File file = new File("data.nbt");
     * DataFile dataFile = new DataFile(file);
     * </pre>
     *
     * @param file the file to load data from (must not be null)
     */
    public DataFile(@NotNull File file) {
        this.file = file;

        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ignored) {}
        }

        try {
            var compound = (CompoundData) NBTIO.readFromCompressedFile(file);
            compound.copyMap().forEach(this::setData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Saves this DataFile to its underlying file by writing its NBT data in a compressed format.
     *
     * <p><b>Example Usage:</b>
     * <pre>
     * dataFile.save();
     * </pre>
     */
    public void save() {
        try {
            if (!file.exists()) file.createNewFile();
            NBTIO.writeToCompressedFile(this, file);
        } catch (IOException ignored) {}
    }
}

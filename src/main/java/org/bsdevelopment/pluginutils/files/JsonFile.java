package org.bsdevelopment.pluginutils.files;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.WriterConfig;
import com.google.common.base.Charsets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;

/**
 * A base class for handling JSON file operations, including loading defaults,
 * reloading from file, and saving modifications.
 *
 * <p><b>Example usage:</b>
 * <pre>
 * public class MyJsonFile extends JsonFile {
 *     public MyJsonFile(File file) {
 *         super(file);
 *     }
 *
 *     &#64;Override
 *     public void loadDefaults() {
 *         setDefault("settings.option", true);
 *         setDefault("settings.value", 42);
 *     }
 * }
 * </pre>
 */
public abstract class JsonFile {
    private final Charset ENCODE = Charsets.UTF_8;

    private JsonObject json;
    protected JsonObject defaults = new JsonObject();

    private final File file;
    private boolean update = false;

    /**
     * Constructs a JsonFile using the given file, automatically loading defaults.
     *
     * <p><b>Example:</b>
     * <pre>
     * JsonFile myFile = new MyJsonFile(new File("config.json"));
     * </pre>
     *
     * @param file
     *         the file to use
     */
    public JsonFile(File file) {
        this(file, true);
    }

    /**
     * Constructs a JsonFile using the given file, with optional default loading.
     *
     * @param file
     *         the file to use
     * @param loadDefaults
     *         if true, calls {@link #loadDefaults()} immediately
     */
    public JsonFile(File file, boolean loadDefaults) {
        this.file = file;
        if (loadDefaults) reload();
    }

    /**
     * Loads the default JSON values. Must be implemented by subclasses.
     *
     * <p><b>Example:</b>
     * <pre>
     * &#64;Override
     * public void loadDefaults() {
     *     setDefault("setting.option", true);
     * }
     * </pre>
     */
    public abstract void loadDefaults();

    /**
     * Reloads the JSON data from the file, creating it with defaults if it does not exist.
     *
     * <p><b>Example:</b>
     * <pre>
     * myJsonFile.reload();
     * </pre>
     */
    public void reload() {
        try {
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();

            boolean defaultsLoaded = false;
            if (!file.exists()) {
                loadDefaults();
                defaultsLoaded = true;

                var writer = new OutputStreamWriter(new FileOutputStream(file), ENCODE);
                writer.write(defaults.toString(WriterConfig.PRETTY_PRINT).replace("\u0026", "&"));
                writer.flush();
                writer.close();
            }

            json = (JsonObject) Json.parse(new InputStreamReader(new FileInputStream(file), ENCODE));
            if (!defaultsLoaded) loadDefaults();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves the JSON data to the file.
     *
     * <p>This is equivalent to calling {@code save(false)}.
     *
     * <p><b>Example:</b>
     * <pre>
     * boolean success = myJsonFile.save();
     * </pre>
     *
     * @return true if the save was successful, false otherwise
     */
    public boolean save() {
        return save(false);
    }

    /**
     * Saves the JSON data to the file, optionally clearing existing data.
     *
     * <p>If {@code clearFile} is true, the file's contents are deleted before writing new data
     * to avoid duplication issues.
     *
     * <p><b>Example:</b>
     * <pre>
     * boolean success = myJsonFile.save(true);
     * </pre>
     *
     * @param clearFile
     *         whether to clear the file's contents before saving
     *
     * @return true if the save was successful, false otherwise
     */
    public boolean save(boolean clearFile) {
        var text = json.toString(WriterConfig.PRETTY_PRINT).replace("\u0026", "&");

        if (file.exists() && clearFile) {
            file.delete();
            try {
                file.createNewFile();
            } catch (Exception ignored) {
            }
        }

        try (var fw = new OutputStreamWriter(new FileOutputStream(file), ENCODE)) {
            fw.write(text);
            fw.flush();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Retrieves the list of top-level keys from the JSON data.
     *
     * <p><b>Example:</b>
     * <pre>
     * List&lt;String&gt; keys = myJsonFile.getKeys();
     * </pre>
     *
     * @return the list of keys, or an empty list if none exist
     */
    public List<String> getKeys() {
        return json.names();
    }

    /**
     * Retrieves the base name of the file, without the .json extension.
     *
     * <p><b>Example:</b>
     * <pre>
     * String name = myJsonFile.getName();
     * </pre>
     *
     * @return the file name without its .json extension
     */
    public String getName() {
        return file.getName().replace(".json", "");
    }

    /**
     * Checks if the specified key is found in either the main JSON or the defaults.
     *
     * @param key
     *         the key to check
     *
     * @return true if the key is found, false otherwise
     */
    public boolean containsKey(String key) {
        return hasKey(key) || hasDefaultKey(key);
    }

    /**
     * Checks if the specified key is found in the main JSON object.
     *
     * @param key
     *         the key to check
     *
     * @return true if the key exists in the JSON, false otherwise
     */
    public boolean hasKey(String key) {
        return json.names().contains(key);
    }

    /**
     * Checks if the specified key is found in the defaults object.
     *
     * @param key
     *         the key to check
     *
     * @return true if the key exists in the defaults, false otherwise
     */
    private boolean hasDefaultKey(String key) {
        return defaults.names().contains(key);
    }

    /**
     * Retrieves the value for the specified key, checking the main JSON first and then defaults.
     *
     * <p><b>Example:</b>
     * <pre>
     * JsonValue value = myJsonFile.getValue("settings.option");
     * </pre>
     *
     * @param key
     *         the key to check
     *
     * @return the {@link JsonValue}, or null if none is found
     */
    public JsonValue getValue(String key) {
        JsonValue value = null;
        if (hasKey(key)) value = json.get(key);

        if (value == null && hasDefaultKey(key)) value = defaults.get(key);

        return value;
    }

    /**
     * Retrieves the default value for the specified key, ignoring the main JSON.
     *
     * <p><b>Example:</b>
     * <pre>
     * JsonValue defValue = myJsonFile.getDefaultValue("settings.option");
     * </pre>
     *
     * @param key
     *         the key to check
     *
     * @return the {@link JsonValue} from defaults, or null if not found
     */
    public JsonValue getDefaultValue(String key) {
        if (!hasDefaultKey(key)) return null;
        return defaults.get(key);
    }

    /**
     * Retrieves a string from the JSON data.
     *
     * @param key
     *         the key to check
     *
     * @return the string value, or an empty string if missing
     */
    public String getString(String key) {
        return getString(key, "");
    }

    /**
     * Retrieves a string from the JSON data, returning a fallback if absent.
     *
     * @param key
     *         the key to check
     * @param fallback
     *         the fallback string if missing
     *
     * @return the string value or fallback
     */
    public String getString(String key, String fallback) {
        var value = getValue(key);
        if (value == null) return fallback;
        return value.asString();
    }

    /**
     * Retrieves a double from the JSON data.
     *
     * @param key
     *         the key to check
     *
     * @return the double value, or 0 if missing
     */
    public double getDouble(String key) {
        return getDouble(key, 0);
    }

    /**
     * Retrieves a double from the JSON data, returning a fallback if absent.
     *
     * @param key
     *         the key to check
     * @param fallback
     *         the fallback double
     *
     * @return the double value or fallback
     */
    public double getDouble(String key, double fallback) {
        var value = getValue(key);
        if (value == null) return fallback;
        if (value.isString()) return Double.parseDouble(value.asString());
        return value.asDouble();
    }

    /**
     * Retrieves a byte from the JSON data.
     *
     * @param key
     *         the key to check
     *
     * @return the byte value, or 0 if missing
     */
    public byte getByte(String key) {
        return getByte(key, (byte) 0);
    }

    /**
     * Retrieves a byte from the JSON data, returning a fallback if absent.
     *
     * @param key
     *         the key to check
     * @param fallback
     *         the fallback byte
     *
     * @return the byte value or fallback
     */
    public byte getByte(String key, byte fallback) {
        var value = getValue(key);
        if (value == null) return fallback;
        if (value.isString()) return Byte.parseByte(value.asString());
        return (byte) value.asInt();
    }

    /**
     * Retrieves a boolean from the JSON data.
     *
     * @param key
     *         the key to check
     *
     * @return the boolean value, or false if missing
     */
    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    /**
     * Retrieves a boolean from the JSON data, returning a fallback if absent.
     *
     * @param key
     *         the key to check
     * @param fallback
     *         the fallback boolean
     *
     * @return the boolean value or fallback
     */
    public boolean getBoolean(String key, boolean fallback) {
        var value = getValue(key);
        if (value == null) return fallback;

        try {
            if (value.isString()) return Boolean.parseBoolean(value.asString());
        } catch (IllegalArgumentException | NullPointerException ex) {
            return fallback;
        }
        return value.asBoolean();
    }

    /**
     * Retrieves a short from the JSON data.
     *
     * @param key
     *         the key to check
     *
     * @return the short value, or 0 if missing
     */
    public short getShort(String key) {
        return getShort(key, (short) 0);
    }

    /**
     * Retrieves a short from the JSON data, returning a fallback if absent.
     *
     * @param key
     *         the key to check
     * @param fallback
     *         the fallback short
     *
     * @return the short value or fallback
     */
    public short getShort(String key, short fallback) {
        var value = getValue(key);
        if (value == null) return fallback;
        if (value.isString()) return Short.parseShort(value.asString());
        return (short) value.asLong();
    }

    /**
     * Retrieves a float from the JSON data.
     *
     * @param key
     *         the key to check
     *
     * @return the float value, or 0 if missing
     */
    public float getFloat(String key) {
        return getFloat(key, 0);
    }

    /**
     * Retrieves a float from the JSON data, returning a fallback if absent.
     *
     * @param key
     *         the key to check
     * @param fallback
     *         the fallback float
     *
     * @return the float value or fallback
     */
    public float getFloat(String key, float fallback) {
        var value = getValue(key);
        if (value == null) return fallback;
        if (value.isString()) return Float.parseFloat(value.asString());
        return value.asFloat();
    }

    /**
     * Retrieves a long from the JSON data.
     *
     * @param key
     *         the key to check
     *
     * @return the long value, or 0 if missing
     */
    public long getLong(String key) {
        return getLong(key, 0);
    }

    /**
     * Retrieves a long from the JSON data, returning a fallback if absent.
     *
     * @param key
     *         the key to check
     * @param fallback
     *         the fallback long
     *
     * @return the long value or fallback
     */
    public long getLong(String key, long fallback) {
        var value = getValue(key);
        if (value == null) return fallback;
        if (value.isString()) return Long.parseLong(value.asString());
        return value.asLong();
    }

    /**
     * Retrieves an integer from the JSON data.
     *
     * @param key
     *         the key to check
     *
     * @return the integer value, or 0 if missing
     */
    public int getInteger(String key) {
        return getInteger(key, 0);
    }

    /**
     * Retrieves an integer from the JSON data, returning a fallback if absent.
     *
     * @param key
     *         the key to check
     * @param fallback
     *         the fallback integer
     *
     * @return the integer value or fallback
     */
    public int getInteger(String key, int fallback) {
        var value = getValue(key);
        if (value == null) return fallback;
        if (value.isString()) return Integer.parseInt(value.asString());
        return value.asInt();
    }

    /**
     * Sets the integer value for the given key in the JSON data.
     *
     * @param key
     *         the key to set
     * @param value
     *         the integer value
     */
    public void set(String key, int value) {
        update = true;
        json.set(key, value);
    }

    /**
     * Sets the long value for the given key in the JSON data.
     *
     * @param key
     *         the key to set
     * @param value
     *         the long value
     */
    public void set(String key, long value) {
        update = true;
        json.set(key, value);
    }

    /**
     * Sets the float value for the given key in the JSON data.
     *
     * @param key
     *         the key to set
     * @param value
     *         the float value
     */
    public void set(String key, float value) {
        update = true;
        json.set(key, value);
    }

    /**
     * Sets the short value for the given key in the JSON data.
     *
     * @param key
     *         the key to set
     * @param value
     *         the short value
     */
    public void set(String key, short value) {
        update = true;
        json.set(key, value);
    }

    /**
     * Sets the byte value for the given key in the JSON data.
     *
     * @param key
     *         the key to set
     * @param value
     *         the byte value
     */
    public void set(String key, byte value) {
        update = true;
        json.set(key, value);
    }

    /**
     * Sets the double value for the given key in the JSON data.
     *
     * @param key
     *         the key to set
     * @param value
     *         the double value
     */
    public void set(String key, double value) {
        update = true;
        json.set(key, value);
    }

    /**
     * Sets the boolean value for the given key in the JSON data.
     *
     * @param key
     *         the key to set
     * @param value
     *         the boolean value
     */
    public void set(String key, boolean value) {
        update = true;
        json.set(key, value);
    }

    /**
     * Sets the string value for the given key in the JSON data.
     *
     * @param key
     *         the key to set
     * @param value
     *         the string value
     */
    public void set(String key, String value) {
        update = true;
        json.set(key, value);
    }

    /**
     * Sets the {@link JsonValue} for the given key in the JSON data.
     *
     * @param key
     *         the key to set
     * @param value
     *         the JsonValue
     */
    public void set(String key, JsonValue value) {
        update = true;
        json.set(key, value);
    }

    /**
     * Removes the specified key from both the main JSON and defaults if it exists.
     *
     * <p><b>Example:</b>
     * <pre>
     * myJsonFile.remove("settings.option");
     * </pre>
     *
     * @param key
     *         the key to remove
     */
    public void remove(String key) {
        boolean changed = false;

        if (defaults.names().contains(key)) {
            changed = true;
            defaults.remove(key);
        }
        if (json.names().contains(key)) {
            changed = true;
            json.remove(key);
        }
        if (changed) save();
    }

    /**
     * Sets a default integer value for the given key.
     *
     * @param key
     *         the key to set
     * @param value
     *         the default integer value
     */
    public void setDefault(String key, int value) {
        defaults.add(key, value);
    }

    /**
     * Sets a default long value for the given key.
     *
     * @param key
     *         the key to set
     * @param value
     *         the default long value
     */
    public void setDefault(String key, long value) {
        defaults.add(key, value);
    }

    /**
     * Sets a default float value for the given key.
     *
     * @param key
     *         the key to set
     * @param value
     *         the default float value
     */
    public void setDefault(String key, float value) {
        defaults.add(key, value);
    }

    /**
     * Sets a default short value for the given key.
     *
     * @param key
     *         the key to set
     * @param value
     *         the default short value
     */
    public void setDefault(String key, short value) {
        defaults.add(key, value);
    }

    /**
     * Sets a default byte value for the given key.
     *
     * @param key
     *         the key to set
     * @param value
     *         the default byte value
     */
    public void setDefault(String key, byte value) {
        defaults.add(key, value);
    }

    /**
     * Sets a default double value for the given key.
     *
     * @param key
     *         the key to set
     * @param value
     *         the default double value
     */
    public void setDefault(String key, double value) {
        defaults.add(key, value);
    }

    /**
     * Sets a default boolean value for the given key.
     *
     * @param key
     *         the key to set
     * @param value
     *         the default boolean value
     */
    public void setDefault(String key, boolean value) {
        defaults.add(key, value);
    }

    /**
     * Sets a default string value for the given key.
     *
     * @param key
     *         the key to set
     * @param value
     *         the default string value
     */
    public void setDefault(String key, String value) {
        defaults.add(key, value);
    }

    /**
     * Sets a default {@link JsonValue} for the given key.
     *
     * @param key
     *         the key to set
     * @param value
     *         the JsonValue
     */
    public void setDefault(String key, JsonValue value) {
        defaults.add(key, value);
    }

    /**
     * Moves the specified key from its old name to a new name in the JSON data.
     *
     * <p>If found, sets the new key to the old key's value, removes the old key, and saves the file.
     *
     * <p><b>Example:</b>
     * <pre>
     * boolean moved = myJsonFile.move("oldKey", "newKey");
     * </pre>
     *
     * @param oldKey
     *         the original key
     * @param newKey
     *         the new key
     *
     * @return true if the key was successfully moved, false otherwise
     */
    public boolean move(String oldKey, String newKey) {
        if (hasKey(oldKey)) {
            json.set(newKey, getValue(oldKey));
            json.remove(oldKey);
            save();
            return true;
        }
        return false;
    }

    /**
     * Retrieves the underlying file used by this JsonFile.
     *
     * <p><b>Example:</b>
     * <pre>
     * File configFile = myJsonFile.getFile();
     * </pre>
     *
     * @return the {@link File} instance
     */
    public File getFile() {
        return file;
    }
}

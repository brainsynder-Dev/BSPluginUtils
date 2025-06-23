package org.bsdevelopment.pluginutils.files;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.bsdevelopment.pluginutils.text.AdvString;
import org.bsdevelopment.pluginutils.text.Colorize;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides YAML file handling with support for comments, sections, and moved keys.
 *
 * <p>This abstract class is intended to be extended to create YAML file managers
 * that automatically handle saving, reloading, comments, and section headers.
 *
 * <p><b>Examples:</b>
 * <pre>
 * public class MyConfig extends YamlFile {
 *     public MyConfig(Plugin plugin) {
 *         super(plugin, "configs", "myconfig.yml");
 *     }
 *
 *     // Implement defaults loading
 *     &#64;Override
 *     public void loadDefaults() {
 *         addDefault("settings.option", true, "Whether the option is enabled");
 *         addDefault("settings.value", 42, "The value to use");
 *     }
 * }
 * </pre>
 */
public abstract class YamlFile implements ConfigurationSection {

    private File file;
    private FileConfiguration configuration;
    private FileConfiguration tempConfig;
    private final HashMap<String, String> movedKeys;
    private final HashMap<String, String> comments;
    private final HashMap<String, String> sections;
    private final HashMap<String, AdvString.AlignText> sectionAlign;
    private List<String> currentLines;

    /**
     * Creates parent directories for the given file.
     *
     * @param file
     *         the file whose parent directories are to be created
     *
     * @throws IOException
     *         if the parent directories cannot be created
     */
    private void createParentDirs(File file) throws IOException {
        Preconditions.checkNotNull(file);

        File parent = file.getCanonicalFile().getParentFile();

        if (parent != null) {
            parent.mkdirs();
            if (!parent.isDirectory()) {
                throw new IOException("Unable to create parent directories of " + file);
            }
        }
    }

    /**
     * Constructs a new YamlFile using the specified plugin, directory, and file name.
     *
     * @param plugin
     *         the plugin instance
     * @param directory
     *         the directory relative to the plugin's data folder
     * @param fileName
     *         the file name
     */
    public YamlFile(Plugin plugin, String directory, String fileName) {
        this(new File(plugin.getDataFolder() + File.separator + directory), fileName);
    }

    /**
     * Constructs a new YamlFile using the specified folder and file name.
     *
     * @param folder
     *         the folder
     * @param fileName
     *         the file name
     */
    public YamlFile(File folder, String fileName) {
        this(new File(folder, fileName));
    }

    /**
     * Constructs a new YamlFile using the specified file.
     *
     * @param file
     *         the file
     */
    public YamlFile(File file) {
        movedKeys = new HashMap<>();
        comments = new HashMap<>();
        sections = new HashMap<>();
        sectionAlign = new HashMap<>();
        currentLines = new ArrayList<>();

        try {
            createParentDirs(file);

            if (!file.exists())
                file.createNewFile();

            if (!file.canWrite())
                file.setWritable(true, false);

            if (!file.canRead())
                file.setReadable(true, false);

            if (!file.canExecute())
                file.setExecutable(true, false);

            this.file = file;
        } catch (Throwable ignored) {
        }

        reload();
    }

    /**
     * Reloads the YAML configuration from file.
     *
     * <p><b>Example:</b>
     * <pre>
     * yamlFile.reload();
     * </pre>
     */
    public void reload() {
        if (file == null)
            return;

        if (!file.canWrite())
            file.setWritable(true, false);

        if (!file.canRead())
            file.setReadable(true, false);

        if (!file.canExecute())
            file.setExecutable(true, false);

        currentLines = new ArrayList<>();

        configuration = YamlConfiguration.loadConfiguration(file);
        tempConfig = new YamlConfiguration();

        loadDefaults();

        configuration.options().copyDefaults(true);

        save(true);

        writeSections();
        writeComments();

        save(false);

        try {
            configuration.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the default configuration values.
     *
     * <p>This method should be implemented by subclasses to set default configuration values.
     *
     * <p><b>Example:</b>
     * <pre>
     * &#64;Override
     * public void loadDefaults() {
     *     addDefault("settings.option", true, "Enable option");
     * }
     * </pre>
     */
    public abstract void loadDefaults();

    /**
     * Removes the configuration entry for the given key.
     *
     * <p><b>Example:</b>
     * <pre>
     * yamlFile.remove("old.key");
     * </pre>
     *
     * @param key
     *         the configuration key to remove
     */
    public void remove(String key) {
        if (configuration.contains(key)) {
            set(key, null);
        }
    }

    /**
     * Adds a comment to the given configuration path.
     *
     * <p><b>Example:</b>
     * <pre>
     * yamlFile.addComment("settings.option", "This option enables X");
     * </pre>
     *
     * @param path
     *         the configuration path
     * @param comment
     *         the comment text
     */
    public void addComment(String path, String comment) {
        String key = fetchKey(path);
        if (comments.containsKey(key))
            return;

        comments.put(key, comment);
    }

    /**
     * Adds a section header for the specified configuration path.
     *
     * <p><b>Example:</b>
     * <pre>
     * yamlFile.addSectionHeader("settings", "Settings Section");
     * </pre>
     *
     * @param path
     *         the configuration path
     * @param text
     *         the header text
     */
    public void addSectionHeader(String path, String text) {
        sections.put(fetchKey(path), text);
    }

    /**
     * Adds a section header with alignment for the specified configuration path.
     *
     * <p><b>Example:</b>
     * <pre>
     * yamlFile.addSectionHeader("settings", AdvString.AlignText.CENTER, "Centered Settings");
     * </pre>
     *
     * @param path
     *         the configuration path
     * @param alignText
     *         the alignment for the header
     * @param text
     *         the header text
     */
    public void addSectionHeader(String path, AdvString.AlignText alignText, String text) {
        sections.put(fetchKey(path), text);
        sectionAlign.put(fetchKey(path), alignText);
    }

    @Override
    public void addDefault(String key, Object value) {
        configuration.addDefault(fetchKey(key), value);
        tempConfig.set(fetchKey(key), configuration.get(key));
    }

    /**
     * Returns inline comments for the given key.
     *
     * @param s
     *         the configuration key
     *
     * @return null (not implemented)
     */
    public List<String> getComments(String s) {
        return null;
    }

    /**
     * Returns inline comments for the given key.
     *
     * @param s
     *         the configuration key
     *
     * @return null (not implemented)
     */
    public List<String> getInlineComments(String s) {
        return null;
    }

    /**
     * Sets the comments for the given key.
     *
     * @param s
     *         the configuration key
     * @param list
     *         the list of comments
     */
    public void setComments(String s, List<String> list) {
    }

    /**
     * Sets the inline comments for the given key.
     *
     * @param s
     *         the configuration key
     * @param list
     *         the list of inline comments
     */
    public void setInlineComments(String s, List<String> list) {
    }

    /**
     * Adds a default value with a comment for the specified configuration key.
     *
     * <p><b>Example:</b>
     * <pre>
     * yamlFile.addDefault("settings.option", true, "Enable option");
     * </pre>
     *
     * @param key
     *         the configuration key
     * @param value
     *         the default value
     * @param comment
     *         the comment to add
     */
    public void addDefault(String key, Object value, String comment) {
        configuration.addDefault(fetchKey(key), value);
        tempConfig.set(fetchKey(key), configuration.get(key));
        addComment(key, comment);
    }

    /**
     * Writes all stored comments to the current lines.
     */
    private void writeComments() {
        for (String path : comments.keySet()) {
            String[] divisions = path.split("\\.");
            writeComment(path, divisions, 0, 0);
        }
    }

    /**
     * Writes a comment at the correct indentation based on the configuration divisions.
     *
     * @param path
     *         the configuration path
     * @param divisions
     *         the divisions of the path
     * @param iteration
     *         current division index
     * @param startingLine
     *         starting line number in currentLines
     */
    private void writeComment(String path, String[] divisions, int iteration, int startingLine) {
        var indent = new StringBuilder();
        for (int j = 0; j < iteration; j++) {
            indent.append("  ");
        }

        for (int i = startingLine; i < currentLines.size(); i++) {
            String line = currentLines.get(i);
            if (!line.startsWith(indent.toString()))
                return;

            if (line.startsWith("#"))
                continue;

            if (line.startsWith(indent + divisions[iteration]) ||
                    line.startsWith(indent + "'" + divisions[iteration] + "'")) {
                iteration++;
                if (iteration == divisions.length) {
                    int currentLine = i;
                    if (iteration == 1) {
                        currentLines.add(currentLine, "");
                        currentLine++;
                    }
                    String[] rawComment = comments.get(fetchKey(path)).split("\n");
                    for (String commentPart : rawComment) {
                        currentLines.add(currentLine, indent + "# " + commentPart);
                        currentLine++;
                    }
                    break;
                } else {
                    writeComment(fetchKey(path), divisions, iteration, i + 1);
                }
            }
        }
    }

    /**
     * Writes all stored section headers to the current lines.
     */
    private void writeSections() {
        for (String path : sections.keySet()) {
            String[] divisions = path.split("\\.");
            writeSection(path, divisions, 0);
        }
    }

    /**
     * Writes a section header at the correct indentation based on the configuration divisions.
     *
     * @param path
     *         the configuration path
     * @param divisions
     *         the divisions of the path
     * @param iteration
     *         current division index
     */
    private void writeSection(String path, String[] divisions, int iteration) {
        var indent = new StringBuilder();
        for (int j = 0; j < iteration; j++) {
            indent.append("  ");
        }

        for (int i = 0; i < currentLines.size(); i++) {
            String line = currentLines.get(i);
            if (line.startsWith(indent + divisions[iteration]) ||
                    line.startsWith(indent + "'" + divisions[iteration] + "'")) {
                iteration++;
                if (iteration == divisions.length) {
                    String section = sections.get(fetchKey(path));
                    var length = new StringBuilder();
                    length.append("###");

                    List<String> sectionList = new ArrayList<>(Arrays.asList(section.split("\n")));
                    int largestString = sectionList.get(0).length();

                    for (String s : sectionList) {
                        if (s.length() > largestString) {
                            largestString = s.length();
                        }
                    }

                    for (int j = 0; j < largestString; j++) {
                        length.append("#");
                    }
                    length.append("###");
                    currentLines.add(i, indent + length.toString());

                    for (int l = sectionList.size() - 1; l >= 0; l--) {
                        String s = sectionList.get(l);
                        currentLines.add(i, indent + "#  " + AdvString.getPaddedString(s, ' ', largestString, sectionAlign.getOrDefault(path, AdvString.AlignText.CENTER)) + "  #");
                    }
                    currentLines.add(i, indent + length.toString());
                    currentLines.add(i, "");
                    break;
                } else {
                    writeSection(fetchKey(path), divisions, iteration);
                }
            }
        }
    }

    /**
     * Saves the YAML file.
     *
     * @param isConfig
     *         if true, saves the temp configuration; if false, writes currentLines to file.
     */
    private void save(boolean isConfig) {
        try {
            if (isConfig) {
                tempConfig.save(file);

                var stream = new FileInputStream(file);
                var reader = new BufferedReader(new InputStreamReader(stream, Charsets.UTF_8));
                String currentLine;
                while ((currentLine = reader.readLine()) != null) {
                    if (currentLine.startsWith("#"))
                        continue;
                    currentLines.add(currentLine);
                }
                reader.close();
            } else {
                var stream = new FileOutputStream(file);
                var writer = new BufferedWriter(new OutputStreamWriter(stream, Charsets.UTF_8));
                for (String line : currentLines) {
                    writer.write(line);
                    writer.write("\n");
                }
                writer.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Retrieves a string from the configuration with optional color translation.
     *
     * <p><b>Example:</b>
     * <pre>
     * String text = yamlFile.getString("path.to.key", true);
     * </pre>
     *
     * @param tag
     *         the configuration key
     * @param color
     *         if true, translates color codes
     *
     * @return the string value
     */
    public String getString(String tag, boolean color) {
        return (color ? translate(getString(tag)) : getString(tag));
    }

    @Override
    public String getString(String tag) {
        return getString(tag, "");
    }

    @Override
    public String getString(String tag, String fallback) {
        if (!contains(tag))
            return fallback;

        return configuration.getString(fetchKey(tag), fallback);
    }

    @Override
    public boolean isString(String tag) {
        return configuration.isString(fetchKey(tag));
    }

    @Override
    public ItemStack getItemStack(String tag) {
        return getItemStack(tag, new ItemStack(Material.AIR));
    }

    @Override
    public ItemStack getItemStack(String tag, ItemStack fallback) {
        if (!contains(tag))
            return fallback;

        return configuration.getItemStack(fetchKey(tag), fallback);
    }

    @Override
    public boolean isItemStack(String tag) {
        return configuration.isItemStack(fetchKey(tag));
    }

    @Override
    public Color getColor(String tag) {
        return getColor(tag, Color.WHITE);
    }

    @Override
    public Color getColor(String tag, Color fallback) {
        if (!contains(tag))
            return fallback;

        return configuration.getColor(fetchKey(tag), fallback);
    }

    @Override
    public boolean isColor(String tag) {
        return configuration.isColor(fetchKey(tag));
    }

    @Override
    public ConfigurationSection getConfigurationSection(String tag) {
        return configuration.getConfigurationSection(fetchKey(tag));
    }

    @Override
    public boolean isConfigurationSection(String tag) {
        return configuration.isConfigurationSection(fetchKey(tag));
    }

    @Override
    public ConfigurationSection getDefaultSection() {
        return configuration.getDefaultSection();
    }

    @Override
    public boolean getBoolean(String tag) {
        return getBoolean(tag, false);
    }

    @Override
    public boolean getBoolean(String tag, boolean fallback) {
        if (!contains(tag))
            return fallback;

        return configuration.getBoolean(fetchKey(tag), fallback);
    }

    @Override
    public boolean isBoolean(String tag) {
        return configuration.isBoolean(fetchKey(tag));
    }

    @Override
    public int getInt(String tag) {
        return getInt(tag, 0);
    }

    @Override
    public int getInt(String tag, int fallback) {
        if (!contains(tag))
            return fallback;

        return configuration.getInt(fetchKey(tag), fallback);
    }

    @Override
    public boolean isInt(String tag) {
        return configuration.isInt(fetchKey(tag));
    }

    @Override
    public double getDouble(String tag) {
        return getDouble(tag, 0);
    }

    @Override
    public double getDouble(String tag, double fallback) {
        if (!contains(tag))
            return fallback;

        return configuration.getDouble(fetchKey(tag), fallback);
    }

    @Override
    public boolean isDouble(String tag) {
        return configuration.isDouble(fetchKey(tag));
    }

    @Override
    public long getLong(String tag) {
        return getLong(tag, 0);
    }

    @Override
    public long getLong(String tag, long fallback) {
        if (!contains(tag))
            return fallback;

        return configuration.getLong(fetchKey(tag), fallback);
    }

    @Override
    public boolean isLong(String tag) {
        return configuration.isLong(fetchKey(tag));
    }

    @Override
    public List<?> getList(String tag) {
        return configuration.getList(fetchKey(tag));
    }

    @Override
    public List<?> getList(String tag, List<?> fallback) {
        if (!contains(tag))
            return fallback;

        return configuration.getList(fetchKey(tag), fallback);
    }

    @Override
    public boolean isList(String tag) {
        return configuration.isList(fetchKey(tag));
    }

    @Override
    public Set<String> getKeys(boolean deep) {
        return configuration.getKeys(deep);
    }

    @Override
    public Map<String, Object> getValues(boolean b) {
        return configuration.getValues(b);
    }

    @Override
    public boolean contains(String tag) {
        return configuration.get(fetchKey(tag)) != null;
    }

    @Override
    public boolean contains(String tag, boolean ignoreDefault) {
        return configuration.contains(fetchKey(tag), ignoreDefault);
    }

    @Override
    public boolean isSet(String tag) {
        return configuration.isSet(fetchKey(tag));
    }

    @Override
    public String getCurrentPath() {
        return configuration.getCurrentPath();
    }

    @Override
    public String getName() {
        return configuration.getName();
    }

    @Override
    public Configuration getRoot() {
        return configuration.getRoot();
    }

    @Override
    public ConfigurationSection getParent() {
        return configuration.getParent();
    }

    @Override
    public List<String> getStringList(String tag) {
        return getStringList(tag, new ArrayList<>());
    }

    /**
     * Retrieves a list of strings from the configuration.
     *
     * <p><b>Example:</b>
     * <pre>
     * List&lt;String&gt; list = yamlFile.getStringList("path.to.list");
     * </pre>
     *
     * @param tag
     *         the configuration key
     * @param fallback
     *         the fallback list if the key doesn't exist
     *
     * @return the list of strings
     */
    public List<String> getStringList(String tag, List<String> fallback) {
        if (!contains(tag))
            return fallback;

        return configuration.getStringList(fetchKey(tag));
    }

    @Override
    public List<Integer> getIntegerList(String tag) {
        return configuration.getIntegerList(fetchKey(tag));
    }

    @Override
    public List<Boolean> getBooleanList(String tag) {
        return configuration.getBooleanList(fetchKey(tag));
    }

    @Override
    public List<Double> getDoubleList(String tag) {
        return configuration.getDoubleList(fetchKey(tag));
    }

    @Override
    public List<Float> getFloatList(String tag) {
        return configuration.getFloatList(fetchKey(tag));
    }

    @Override
    public List<Long> getLongList(String tag) {
        return configuration.getLongList(fetchKey(tag));
    }

    @Override
    public List<Byte> getByteList(String tag) {
        return configuration.getByteList(fetchKey(tag));
    }

    @Override
    public List<Character> getCharacterList(String tag) {
        return configuration.getCharacterList(fetchKey(tag));
    }

    @Override
    public List<Short> getShortList(String tag) {
        return configuration.getShortList(fetchKey(tag));
    }

    @Override
    public List<Map<?, ?>> getMapList(String tag) {
        return configuration.getMapList(fetchKey(tag));
    }

    @Override
    public <T> T getObject(String s, Class<T> aClass) {
        return configuration.getObject(fetchKey(s), aClass);
    }

    @Override
    public <T> T getObject(String s, Class<T> aClass, T t) {
        return configuration.getObject(fetchKey(s), aClass, t);
    }

    @Override
    public <T extends ConfigurationSerializable> T getSerializable(String s, Class<T> aClass) {
        return configuration.getSerializable(fetchKey(s), aClass);
    }

    @Override
    public <T extends ConfigurationSerializable> T getSerializable(String s, Class<T> aClass, T t) {
        return configuration.getSerializable(fetchKey(s), aClass, t);
    }

    @Override
    public Vector getVector(String tag) {
        return configuration.getVector(fetchKey(tag));
    }

    @Override
    public Vector getVector(String tag, Vector fallback) {
        if (!contains(tag))
            return fallback;

        return configuration.getVector(fetchKey(tag), fallback);
    }

    @Override
    public boolean isVector(String tag) {
        return configuration.isVector(fetchKey(tag));
    }

    @Override
    public OfflinePlayer getOfflinePlayer(String tag) {
        return configuration.getOfflinePlayer(fetchKey(tag));
    }

    @Override
    public OfflinePlayer getOfflinePlayer(String tag, OfflinePlayer fallback) {
        if (!contains(tag))
            return fallback;

        return configuration.getOfflinePlayer(fetchKey(tag), fallback);
    }

    @Override
    public boolean isOfflinePlayer(String tag) {
        return configuration.isOfflinePlayer(fetchKey(tag));
    }

    /**
     * Retrieves the configuration section for the given tag.
     *
     * <p><b>Example:</b>
     * <pre>
     * ConfigurationSection section = yamlFile.getSection("settings");
     * </pre>
     *
     * @param tag
     *         the configuration key
     *
     * @return the configuration section
     */
    public ConfigurationSection getSection(String tag) {
        return configuration.getConfigurationSection(fetchKey(tag));
    }

    @Override
    public Object get(String tag) {
        return configuration.get(fetchKey(tag));
    }

    @Override
    public Object get(String tag, Object fallback) {
        if (!contains(tag))
            return fallback;

        return configuration.get(fetchKey(tag));
    }

    private String translate(String msg) {
        return Colorize.translateBungeeHex(msg);
    }

    @Override
    public void set(String tag, Object data) {
        set(tag, data, true);
    }

    /**
     * Sets the configuration value for the given tag.
     *
     * <p><b>Example:</b>
     * <pre>
     * yamlFile.set("settings.option", true);
     * </pre>
     *
     * @param tag
     *         the configuration key
     * @param data
     *         the data to set (null to remove)
     * @param save
     *         if true, immediately saves the configuration
     */
    public void set(String tag, Object data, boolean save) {
        currentLines = new ArrayList<>();

        configuration.set(fetchKey(tag), data);
        tempConfig.set(fetchKey(tag), data);

        if (save)
            save();
    }

    /**
     * Saves the configuration to file.
     *
     * <p><b>Example:</b>
     * <pre>
     * yamlFile.save();
     * </pre>
     */
    public void save() {
        save(true);

        writeSections();
        writeComments();

        save(false);
    }

    @Override
    public ConfigurationSection createSection(String tag) {
        return configuration.createSection(fetchKey(tag));
    }

    @Override
    public ConfigurationSection createSection(String tag, Map<?, ?> fallback) {
        return configuration.createSection(fetchKey(tag), fallback);
    }

    /**
     * Sets the header of the configuration file.
     *
     * <p><b>Example:</b>
     * <pre>
     * yamlFile.setHeader("My Config Header");
     * </pre>
     *
     * @param header
     *         the header lines
     */
    public void setHeader(String... header) {
        configuration.options().header(Arrays.toString(header));
    }

    /**
     * Retrieves the configuration section values as a Map.
     *
     * <p><b>Example:</b>
     * <pre>
     * Map&lt;String, Object&gt; values = yamlFile.getConfigSectionValue(someSection);
     * </pre>
     *
     * @param o
     *         the configuration section or map
     *
     * @return a Map of values
     */
    public Map<String, Object> getConfigSectionValue(Object o) {
        return getConfigSectionValue(o, false);
    }

    /**
     * Retrieves the configuration section values as a Map.
     *
     * <p><b>Example:</b>
     * <pre>
     * Map&lt;String, Object&gt; values = yamlFile.getConfigSectionValue(someSection, true);
     * </pre>
     *
     * @param o
     *         the configuration section or map
     * @param deep
     *         if true, retrieves values recursively
     *
     * @return a Map of values
     */
    public Map<String, Object> getConfigSectionValue(Object o, boolean deep) {
        configuration = YamlConfiguration.loadConfiguration(file);
        Map<String, Object> map = new HashMap<>();
        if (o == null) {
            return map;
        } else {
            if (o instanceof ConfigurationSection) {
                map = ((ConfigurationSection) o).getValues(deep);
            } else if (o instanceof Map) {
                map = (Map) o;
            }
            return map;
        }
    }

    /**
     * Moves a configuration key from oldKey to newKey.
     *
     * <p><b>Example:</b>
     * <pre>
     * boolean moved = yamlFile.move("old.key", "new.key");
     * </pre>
     *
     * @param oldKey
     *         the original key
     * @param newKey
     *         the new key to map to
     *
     * @return true if the key was moved; false otherwise
     */
    public boolean move(String oldKey, String newKey) {
        if (contains(oldKey)) {
            movedKeys.putIfAbsent(oldKey, newKey);

            if ((!comments.containsKey(newKey)) && comments.containsKey(oldKey))
                comments.put(newKey, comments.get(oldKey));

            if ((!sections.containsKey(newKey)) && sections.containsKey(oldKey))
                sections.put(newKey, sections.get(oldKey));

            set(newKey, get(oldKey));
            set(oldKey, null);
            return true;
        }
        return false;
    }

    /**
     * Registers moved keys mapping.
     *
     * <p><b>Example:</b>
     * <pre>
     * yamlFile.registerMovedKeys("new.key", "old.key1", "old.key2");
     * </pre>
     *
     * @param newKey
     *         the new key
     * @param oldKeys
     *         one or more old keys to map to the new key
     */
    public void registerMovedKeys(String newKey, String... oldKeys) {
        Lists.newArrayList(oldKeys).forEach(oldKey -> movedKeys.putIfAbsent(oldKey, newKey));
    }

    /**
     * Checks if the key was moved; if it was, returns the updated key.
     *
     * <p><b>Example:</b>
     * <pre>
     * String key = yamlFile.fetchKey("oldKey");
     * </pre>
     *
     * @param key
     *         the original key
     *
     * @return the new key if moved, or the original key
     */
    public String fetchKey(String key) {
        if (key == null)
            return null;

        if (key.isEmpty())
            return key;

        return movedKeys.getOrDefault(key, key);
    }

    /**
     * Retrieves a Location from the configuration.
     *
     * <p><b>Example:</b>
     * <pre>
     * Location loc = yamlFile.getLocation("spawn");
     * </pre>
     *
     * @param path
     *         the configuration key
     *
     * @return the Location
     */
    public Location getLocation(String path) {
        return getSerializable(fetchKey(path), Location.class);
    }

    /**
     * Retrieves a Location from the configuration with a default value.
     *
     * <p><b>Example:</b>
     * <pre>
     * Location loc = yamlFile.getLocation("spawn", defaultLocation);
     * </pre>
     *
     * @param path
     *         the configuration key
     * @param def
     *         the default Location
     *
     * @return the Location, or the default if not found
     */
    public Location getLocation(String path, Location def) {
        return getSerializable(fetchKey(path), Location.class, def);
    }

    /**
     * Checks if the configuration value at the given path is a Location.
     *
     * <p><b>Example:</b>
     * <pre>
     * boolean isLoc = yamlFile.isLocation("spawn");
     * </pre>
     *
     * @param path
     *         the configuration key
     *
     * @return true if it is a Location, false otherwise
     */
    public boolean isLocation(String path) {
        return getSerializable(fetchKey(path), Location.class) != null;
    }

    /**
     * Retrieves the file associated with this YAML configuration.
     *
     * <p><b>Example:</b>
     * <pre>
     * File configFile = yamlFile.getFile();
     * </pre>
     *
     * @return the File object
     */
    public File getFile() {
        return file;
    }
}

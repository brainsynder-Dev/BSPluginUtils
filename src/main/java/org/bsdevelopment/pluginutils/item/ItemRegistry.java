package org.bsdevelopment.pluginutils.item;

import org.bsdevelopment.pluginutils.gui.parser.XmlValidationException;
import org.bsdevelopment.pluginutils.inventory.ItemBuilder;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central store for {@link ItemBuilder} entries keyed by {@code item-id} strings (namespace:id).
 *
 * <p>Designed to work with {@link ItemXmlIO#readIdEntry(File, JavaPlugin)}, which returns
 * {@code (itemId, builder)} where {@code itemId} is the {@code namespace:id} you place on
 * the root &lt;item item-id="..."/&gt; element of standalone item XML files.
 *
 * <h2>Typical usage</h2>
 * <pre>{@code
 * // OnEnable:
 * ItemRegistry.clear();
 * File itemsDir = new File(getDataFolder(), "items");
 * itemsDir.mkdirs();
 * int count = ItemRegistry.registerDirectory(this, itemsDir, true, true); // recursive, forbid duplicates
 * getLogger().info("Loaded " + count + " item xml entries");
 *
 * // Later: resolve by id
 * ItemBuilder b = ItemRegistry.get("myplugin:test_item").orElseThrow(() -> new IllegalStateException("Missing item-id"));
 *
 * // Or resolve by NamespacedKey:
 * ItemBuilder c = ItemRegistry.get(NamespacedKey.fromString("myplugin:test_item")).orElseThrow();
 * }</pre>
 *
 * <p>All keys are normalized to lowercase {@code namespace:id} to avoid case-related duplication.</p>
 */
public final class ItemRegistry {

    /** Internal storage keyed by normalized (lowercase) {@code item-id} like {@code myplugin:foo}. */
    private static final ConcurrentHashMap<String, ItemBuilder> REGISTRY = new ConcurrentHashMap<>();

    /**
     * Register (or replace) an item definition under a specific {@code item-id}.
     *
     * @param itemId  required, {@code namespace:id}
     * @param builder required
     *
     * @return previous builder if one existed
     */
    public static ItemBuilder register(String itemId, ItemBuilder builder) {
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(builder, "builder");
        String key = normalizeId(itemId);
        validateItemIdFormat(key, null);
        return REGISTRY.put(key, builder);
    }

    /**
     * Register (or replace) using a {@link NamespacedKey}.
     *
     * @param key     namespaced key
     * @param builder required
     *
     * @return previous builder if one existed
     */
    public static ItemBuilder register(NamespacedKey key, ItemBuilder builder) {
        Objects.requireNonNull(key, "namespaced key is null");
        return register(toId(key), builder);
    }

    /**
     * Load one standalone item XML file and register its {@code item-id}.
     *
     * @param plugin           plugin instance (used by {@link ItemXmlIO})
     * @param xmlFile          the XML file to read
     * @param forbidDuplicates if true, throws on duplicate {@code item-id}
     */
    public static void registerFromFile(JavaPlugin plugin, File xmlFile, boolean forbidDuplicates) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(xmlFile, "xmlFile");

        var entry = ItemXmlIO.readIdEntry(xmlFile, plugin);
        String key = normalizeId(entry.itemId());
        validateItemIdFormat(key, xmlFile.getName());

        if (forbidDuplicates && REGISTRY.containsKey(key))
            throw new XmlValidationException(null, "Duplicate item-id '" + key + "' in file: " + xmlFile.getName(), "Ensure each standalone item XML uses a unique item-id");

        REGISTRY.put(key, entry.builder());
    }

    /**
     * Scan a directory for {@code .xml} files and register each item file.
     *
     * @param plugin           plugin instance
     * @param directory        directory to scan
     * @param recursive        whether to descend into subdirectories
     * @param forbidDuplicates if true, throws on the first duplicate key found
     *
     * @return number of items registered
     */
    public static int registerDirectory(JavaPlugin plugin, File directory, boolean recursive, boolean forbidDuplicates) {
        Objects.requireNonNull(plugin, "plugin");
        if (directory == null || !directory.isDirectory()) return 0;

        int count = 0;
        File[] children = directory.listFiles();
        if (children == null) return 0;

        for (File file : children) {
            if (file.isDirectory() && recursive) {
                count += registerDirectory(plugin, file, true, forbidDuplicates);
            } else if (isXmlFile(file)) {
                registerFromFile(plugin, file, forbidDuplicates);
                count++;
            }
        }
        return count;
    }

    /** Get an entry by {@code item-id} (case-insensitive). */
    public static Optional<ItemBuilder> get(String itemId) {
        if (itemId == null) return Optional.empty();
        return Optional.ofNullable(REGISTRY.get(normalizeId(itemId)));
    }

    /** Get an entry by {@link NamespacedKey}. */
    public static Optional<ItemBuilder> get(NamespacedKey key) {
        if (key == null) return Optional.empty();
        return get(toId(key));
    }

    /** @return true if the {@code item-id} exists. */
    public static boolean contains(String itemId) {
        return itemId != null && REGISTRY.containsKey(normalizeId(itemId));
    }

    /** @return true if the {@link NamespacedKey} exists. */
    public static boolean contains(NamespacedKey key) {
        return key != null && contains(toId(key));
    }

    /** Remove an entry by {@code item-id}. */
    public static Optional<ItemBuilder> unregister(String itemId) {
        if (itemId == null) return Optional.empty();
        return Optional.ofNullable(REGISTRY.remove(normalizeId(itemId)));
    }

    /** Remove an entry by {@link NamespacedKey}. */
    public static Optional<ItemBuilder> unregister(NamespacedKey key) {
        if (key == null) return Optional.empty();
        return unregister(toId(key));
    }

    /** Remove everything. */
    public static void clear() {
        REGISTRY.clear();
    }

    /** @return number of registered items. */
    public static int size() {
        return REGISTRY.size();
    }

    /** @return immutable view of all {@code item-id} keys. */
    public static Set<String> ids() {
        return Set.copyOf(REGISTRY.keySet());
    }

    /** @return immutable view of all values. */
    public static Collection<ItemBuilder> values() {
        return Set.copyOf(REGISTRY.values());
    }

    /**
     * Get a copy of the backing map (useful to pass into XML GUI loader overloads).
     * Keys are normalized {@code item-id} strings.
     */
    public static Map<String, ItemBuilder> asMap() {
        return Map.copyOf(REGISTRY);
    }

    private static boolean isXmlFile(File file) {
        return file != null && file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(".xml");
    }

    private static String normalizeId(String id) {
        return id.toLowerCase(Locale.ROOT);
    }

    private static String toId(NamespacedKey key) {
        return (key.getNamespace() + ":" + key.getKey()).toLowerCase(Locale.ROOT);
    }

    private static void validateItemIdFormat(String itemId, String filenameHint) {
        if (itemId == null || !itemId.contains(":")) {
            throw new XmlValidationException(null, "Invalid item-id '" + itemId + "'" + (filenameHint != null ? (" in file: " + filenameHint) : ""), "Use namespace:id (e.g., myplugin:powered_sword)");
        }
    }
}

package org.bsdevelopment.pluginutils.item;

import org.bsdevelopment.pluginutils.inventory.ItemBuilder;
import org.bsdevelopment.pluginutils.xml.XmlValidationException;
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
 * Central store for {@link ItemBuilder} entries keyed by {@code namespace:id} strings.
 *
 * <p>Keys are always normalized to lowercase. Use {@link #registerDirectory} to bulk-load
 * standalone item XML files at startup, then resolve them later with {@link #get(String)}.
 */
public final class ItemRegistry {

    private static final ConcurrentHashMap<String, ItemBuilder> REGISTRY = new ConcurrentHashMap<>();

    public static ItemBuilder register(String itemId, ItemBuilder builder) {
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(builder, "builder");
        String key = normalizeId(itemId);
        validateFormat(key, null);
        return REGISTRY.put(key, builder);
    }

    public static ItemBuilder register(NamespacedKey key, ItemBuilder builder) {
        Objects.requireNonNull(key, "key");
        return register(toId(key), builder);
    }

    /**
     * Load one standalone item XML file and register its {@code item-id}.
     *
     * @param forbidDuplicates throw on duplicate {@code item-id} if {@code true}
     */
    public static void registerFromFile(JavaPlugin plugin, File xmlFile, boolean forbidDuplicates) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(xmlFile, "xmlFile");

        var entry = ItemXmlIO.readIdEntry(xmlFile, plugin);
        String key = normalizeId(entry.itemId());
        validateFormat(key, xmlFile.getName());

        if (forbidDuplicates && REGISTRY.containsKey(key))
            throw new XmlValidationException(null, "Duplicate item-id '" + key + "' in file: " + xmlFile.getName(), "Ensure each standalone item XML uses a unique item-id");

        REGISTRY.put(key, entry.builder());
    }

    /**
     * Scan a directory for {@code .xml} files and register each one.
     *
     * @param recursive        descend into subdirectories if {@code true}
     * @param forbidDuplicates throw on the first duplicate key found if {@code true}
     * @return number of items registered
     */
    public static int registerDirectory(JavaPlugin plugin, File directory, boolean recursive, boolean forbidDuplicates) {
        Objects.requireNonNull(plugin, "plugin");
        if (directory == null || !directory.isDirectory()) return 0;

        File[] children = directory.listFiles();
        if (children == null) return 0;

        int count = 0;
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

    public static Optional<ItemBuilder> get(String itemId) {
        if (itemId == null) return Optional.empty();
        return Optional.ofNullable(REGISTRY.get(normalizeId(itemId)));
    }

    public static Optional<ItemBuilder> get(NamespacedKey key) {
        if (key == null) return Optional.empty();
        return get(toId(key));
    }

    public static boolean contains(String itemId) {
        return itemId != null && REGISTRY.containsKey(normalizeId(itemId));
    }

    public static boolean contains(NamespacedKey key) {
        return key != null && contains(toId(key));
    }

    public static Optional<ItemBuilder> unregister(String itemId) {
        if (itemId == null) return Optional.empty();
        return Optional.ofNullable(REGISTRY.remove(normalizeId(itemId)));
    }

    public static Optional<ItemBuilder> unregister(NamespacedKey key) {
        if (key == null) return Optional.empty();
        return unregister(toId(key));
    }

    public static void clear() {
        REGISTRY.clear();
    }

    public static int size() {
        return REGISTRY.size();
    }

    public static Set<String> ids() {
        return Set.copyOf(REGISTRY.keySet());
    }

    public static Collection<ItemBuilder> values() {
        return Set.copyOf(REGISTRY.values());
    }

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

    private static void validateFormat(String itemId, String filenameHint) {
        if (itemId == null || !itemId.contains(":")) {
            String detail = filenameHint != null ? " in file: " + filenameHint : "";
            throw new XmlValidationException(null, "Invalid item-id '" + itemId + "'" + detail, "Use namespace:id (e.g., myplugin:powered_sword)");
        }
    }
}

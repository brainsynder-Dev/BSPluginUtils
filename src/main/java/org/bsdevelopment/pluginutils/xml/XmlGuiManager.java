package org.bsdevelopment.pluginutils.xml;

import org.bsdevelopment.pluginutils.inventory.ItemBuilder;
import org.bsdevelopment.pluginutils.item.ItemRegistry;
import org.bsdevelopment.pluginutils.text.Colorize;
import org.bsdevelopment.pluginutils.xml.io.XmlGuiReader;
import org.bsdevelopment.pluginutils.xml.io.XmlGuiWriter;
import org.bsdevelopment.pluginutils.xml.model.XmlActionDefinition;
import org.bsdevelopment.pluginutils.xml.model.XmlGuiDefinition;
import org.bsdevelopment.pluginutils.xml.model.XmlSlotDefinition;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central manager for the XML GUI system.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li><b>Default registration</b> — store hardcoded {@link XmlGuiDefinition} defaults that
 *       describe each GUI as it should look out-of-the-box.</li>
 *   <li><b>Load-or-save</b> — if the GUI file already exists, load it; otherwise write the
 *       default to disk so server owners can customise it, then use the default for this
 *       session.</li>
 *   <li><b>Compilation</b> — resolve item references and action definitions to produce a
 *       live {@link XmlGui} backed by a real Bukkit {@link Inventory}.</li>
 * </ol>
 *
 * <p>Typical usage in {@code onEnable}:
 * <pre>{@code
 * XmlGuiManager guiManager = new XmlGuiManager(this);
 *
 * guiManager.registerDefault("main_menu",
 *     XmlGuiDefinition.chest("&6Main Menu", 3)
 *         .define("filler", ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).withName(" "))
 *         .define("close", ItemBuilder.of(Material.BARRIER).withName("&cClose"))
 *         .slot("0-8,18-26").ref("filler").end()
 *         .slot("13").item(ItemBuilder.of(Material.DIAMOND).withName("&bDiamond"))
 *             .action("message", "&aYou clicked!")
 *             .end()
 *         .slot("22").ref("close")
 *             .action("close")
 *             .end()
 *         .build());
 *
 * File menuFile = new File(getDataFolder(), "guis/main_menu.xml");
 * XmlGui mainMenu = guiManager.loadOrSaveDefault("main_menu", menuFile);
 *
 * // Later, to open for a player:
 * mainMenu.open(player);
 * }</pre>
 */
public class XmlGuiManager {

    private final JavaPlugin plugin;
    private final Map<String, XmlGuiDefinition> defaults = new LinkedHashMap<>();

    /**
     * @param plugin the owning plugin; used for listener registration, namespaced keys,
     *               and logging
     */
    public XmlGuiManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Default registration
    // -------------------------------------------------------------------------

    /**
     * Register a hardcoded default definition under the given id.
     *
     * <p>The id is normalized to lower-case. Registering the same id again replaces the
     * previous default.
     *
     * @param id         unique GUI identifier (e.g. {@code "main_menu"})
     * @param definition the default definition (built via the fluent builder API)
     */
    public void registerDefault(String id, XmlGuiDefinition definition) {
        defaults.put(id.toLowerCase(), definition);
    }

    /**
     * Returns the registered default for the given id, or {@code null} if none.
     *
     * @param id GUI identifier (case-insensitive)
     *
     * @return the registered default, or {@code null}
     */
    public XmlGuiDefinition getDefault(String id) {
        return defaults.get(id.toLowerCase());
    }

    /**
     * Returns {@code true} if a default is registered for the given id.
     *
     * @param id GUI identifier (case-insensitive)
     *
     * @return whether a default exists
     */
    public boolean hasDefault(String id) {
        return defaults.containsKey(id.toLowerCase());
    }

    // -------------------------------------------------------------------------
    // Load / save
    // -------------------------------------------------------------------------

    /**
     * Load an {@link XmlGui} from {@code file} if it exists; otherwise write the registered
     * default to disk and compile the default for this session.
     *
     * <p>This is the primary method used in {@code onEnable} to give server owners a
     * customisable file while ensuring a sensible out-of-the-box experience.
     *
     * @param id   GUI identifier matching a registered default
     * @param file the XML file to load from / save to
     *
     * @return a compiled, registered {@link XmlGui} ready for {@link XmlGui#open(org.bukkit.entity.Player)}
     *
     * @throws IllegalArgumentException if {@code id} has no registered default and the file
     *                                  does not exist
     */
    public XmlGui loadOrSaveDefault(String id, File file) {
        if (file.exists()) {
            return load(file);
        }

        XmlGuiDefinition definition = getDefault(id);
        if (definition == null) {
            throw new IllegalArgumentException(
                    "No default registered for GUI id '" + id + "' and file does not exist: " + file);
        }

        // Write the default so the server owner can customise it
        saveDefault(id, file);

        return compile(definition);
    }

    /**
     * Load and compile an {@link XmlGui} directly from an XML file.
     *
     * <p>This does not require a registered default.
     *
     * @param file the XML GUI file
     *
     * @return a compiled, registered {@link XmlGui}
     */
    public XmlGui load(File file) {
        return compile(XmlGuiReader.read(file, plugin));
    }

    /**
     * Write the registered default for {@code id} to {@code file}.
     *
     * <p>Creates parent directories automatically. Logs a warning (instead of throwing) if
     * the write fails so that a missing data folder does not crash the plugin.
     *
     * @param id   GUI identifier matching a registered default
     * @param file destination file
     *
     * @throws IllegalArgumentException if {@code id} has no registered default
     */
    public void saveDefault(String id, File file) {
        XmlGuiDefinition definition = getDefault(id);
        if (definition == null) {
            throw new IllegalArgumentException("No default registered for GUI id '" + id + "'");
        }

        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        try {
            XmlGuiWriter.write(file, definition);
        } catch (Exception exception) {
            plugin.getLogger().warning(
                    "[XmlGuiManager] Failed to write default GUI '" + id + "' to " + file + ": "
                            + exception.getMessage());
        }
    }

    /**
     * Reload a GUI from its file, replacing the previously compiled instance.
     *
     * <p>Useful for {@code /reload}-style commands. Viewers of the old GUI will not be
     * automatically updated; close and re-open the GUI for them as needed.
     *
     * @param file the XML GUI file
     *
     * @return a freshly compiled {@link XmlGui}
     */
    public XmlGui reload(File file) {
        return load(file);
    }

    // -------------------------------------------------------------------------
    // Compilation
    // -------------------------------------------------------------------------

    /**
     * Compile an {@link XmlGuiDefinition} into a live {@link XmlGui}.
     *
     * <p>Steps:
     * <ol>
     *   <li>Creates the Bukkit {@link Inventory}</li>
     *   <li>For each slot definition: resolves the item from local definitions or
     *       {@link ItemRegistry}, then sets it in the inventory</li>
     *   <li>Builds the slot→actions map via {@link XmlActionRegistry}</li>
     *   <li>Wraps everything in an {@link XmlGui} and registers it</li>
     * </ol>
     *
     * @param definition the definition to compile
     *
     * @return a compiled, registered {@link XmlGui}
     *
     * @throws IllegalStateException  if an item reference or action type cannot be resolved
     * @throws XmlValidationException if item parsing fails
     */
    public XmlGui compile(XmlGuiDefinition definition) {
        // 1. Create the Bukkit inventory
        String title = Colorize.translateBungeeHex(definition.getTitle());
        Inventory inventory;
        if (definition.isRowBased()) {
            int size = definition.getRows() * 9;
            inventory = Bukkit.createInventory(null, size, title);
        } else {
            inventory = Bukkit.createInventory(null, definition.getInventoryType(), title);
        }

        // 2. Resolve items + build actions map
        Map<Integer, List<XmlGuiAction>> actionsMap = new HashMap<>();

        for (XmlSlotDefinition slotDef : definition.getSlots()) {
            // Resolve item builder
            ItemBuilder builder = null;

            if (slotDef.hasItemRef()) {
                builder = resolveItemRef(slotDef.getItemRef(), definition);
            } else if (slotDef.getBuilder() != null) {
                builder = slotDef.getBuilder();
            }

            // Place item in each covered slot
            for (int slot : slotDef.getSlots()) {
                if (slot < 0 || slot >= inventory.getSize()) {
                    plugin.getLogger().warning(
                            "[XmlGuiManager] Slot index " + slot + " is out of range (inventory size "
                                    + inventory.getSize() + ") — skipping.");
                    continue;
                }

                if (builder != null) {
                    inventory.setItem(slot, builder.build());
                }

                // Compile actions
                if (!slotDef.getActions().isEmpty()) {
                    List<XmlGuiAction> compiledActions = new ArrayList<>();
                    for (XmlActionDefinition actionDef : slotDef.getActions()) {
                        XmlGuiAction action = XmlActionRegistry.parse(actionDef).orElseThrow(() ->
                                new IllegalStateException(
                                        "Unknown action type '" + actionDef.getType()
                                                + "'. Register it via XmlActionRegistry.register(...)"));
                        compiledActions.add(action);
                    }
                    actionsMap.put(slot, compiledActions);
                }
            }
        }

        // 3. Build XmlGui and register
        XmlGui gui = new XmlGui(inventory, actionsMap);
        gui.register(plugin);
        return gui;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Resolve an item reference string to an {@link ItemBuilder}.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>If the ref contains {@code ':'} → look up in {@link ItemRegistry}</li>
     *   <li>Otherwise → look up in the definition's local definitions map</li>
     * </ol>
     */
    private ItemBuilder resolveItemRef(String ref, XmlGuiDefinition definition) {
        if (ref.contains(":")) {
            // Namespaced key — look up in external ItemRegistry
            return ItemRegistry.get(ref).orElseThrow(() ->
                    new IllegalStateException(
                            "Unknown ItemRegistry key '" + ref
                                    + "'. Register it via ItemRegistry.registerFromFile(...) before compiling."));
        }

        // Local definition
        ItemBuilder local = definition.getDefinitions().get(ref);
        if (local == null) {
            throw new IllegalStateException(
                    "Unknown item definition id '" + ref
                            + "'. Add <item id=\"" + ref + "\" .../> to <definitions> in the GUI XML.");
        }
        return local;
    }
}

package org.bsdevelopment.pluginutils.xml;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A runtime XML-driven GUI inventory.
 *
 * <p>Holds a Bukkit {@link Inventory} and the slot→actions map produced by
 * {@link XmlGuiManager}. Implements {@link Listener} to intercept click and drag events.
 *
 * <p>Usage:
 * <pre>{@code
 * // Obtain via XmlGuiManager:
 * XmlGui gui = manager.loadOrSaveDefault("main_menu", file);
 *
 * // Open for a player (auto-registers if the manager was used):
 * gui.open(player);
 * }</pre>
 *
 * <p>Register once with {@link #register(Plugin)} before opening. Safe to call multiple
 * times — duplicate registrations are ignored.
 */
public class XmlGui implements Listener {

    private final Inventory inventory;
    private final Map<Integer, List<XmlGuiAction>> actions;

    /** Tracks which players currently have this GUI open (by UUID). */
    private final Set<UUID> viewers = new HashSet<>();

    private boolean registered = false;

    /**
     * @param inventory the Bukkit inventory to display
     * @param actions   slot index → ordered list of actions; copied defensively
     */
    public XmlGui(Inventory inventory, Map<Integer, List<XmlGuiAction>> actions) {
        this.inventory = inventory;
        this.actions = Map.copyOf(actions);
    }

    // -------------------------------------------------------------------------
    // Registration & opening
    // -------------------------------------------------------------------------

    /**
     * Register this GUI's event listener with the server.
     *
     * <p>Safe to call multiple times — only registers once. Normally called automatically by
     * {@link XmlGuiManager}.
     *
     * @param plugin owning plugin
     */
    public void register(Plugin plugin) {
        if (!registered) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
    }

    /**
     * Open the GUI for a player, registering the listener if not already registered.
     *
     * @param player the player to show the GUI to
     * @param plugin owning plugin (used for registration if needed)
     */
    public void open(Player player, Plugin plugin) {
        register(plugin);
        open(player);
    }

    /**
     * Open the GUI for a player.
     *
     * <p>{@link #register(Plugin)} must be called before using this overload.
     *
     * @param player the player to show the GUI to
     */
    public void open(Player player) {
        viewers.add(player.getUniqueId());
        player.openInventory(inventory);
    }

    /**
     * Close the GUI for a specific player if they have it open.
     *
     * @param player the player whose inventory should be closed
     */
    public void close(Player player) {
        if (viewers.contains(player.getUniqueId())) {
            player.closeInventory();
        }
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** Returns the underlying Bukkit inventory. */
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Returns an unmodifiable view of the UUIDs of players currently viewing this GUI.
     *
     * @return current viewers
     */
    public Set<UUID> getViewers() {
        return Collections.unmodifiableSet(viewers);
    }

    /** Returns {@code true} if the given player currently has this GUI open. */
    public boolean isViewing(Player player) {
        return viewers.contains(player.getUniqueId());
    }

    /** Returns {@code true} if this GUI has been registered as a Bukkit listener. */
    public boolean isRegistered() {
        return registered;
    }

    // -------------------------------------------------------------------------
    // Event handling
    // -------------------------------------------------------------------------

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;

        // Always cancel clicks in the top (GUI) inventory
        if (event.getClickedInventory() != null && event.getClickedInventory().equals(inventory)) {
            event.setCancelled(true);
            int slot = event.getSlot();
            List<XmlGuiAction> slotActions = actions.get(slot);
            if (slotActions != null) {
                // Allow actions to un-cancel if needed
                event.setCancelled(false);
                slotActions.forEach(action -> action.execute(event));
            }
        } else {
            // Shift-click from player inventory into a GUI — cancel to prevent item movement
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
        }
    }

    /** Prevent item dragging into the GUI inventory. */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        // If any dragged slot is in the GUI inventory, cancel
        for (int slot : event.getRawSlots()) {
            if (slot < inventory.getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory)) {
            viewers.remove(event.getPlayer().getUniqueId());
        }
    }
}

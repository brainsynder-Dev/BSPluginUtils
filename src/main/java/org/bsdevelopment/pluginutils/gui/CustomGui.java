package org.bsdevelopment.pluginutils.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;

/**
 * Wraps a Bukkit Inventory plus its slot→actions map.
 */
public class CustomGui implements Listener {
    private final Inventory inventory;
    private final Map<Integer, List<GuiAction>> actions;

    /**
     * @param inventory
     *         the Inventory to display
     * @param actions
     *         map slot-index → list of actions
     */
    public CustomGui(Inventory inventory, Map<Integer, List<GuiAction>> actions) {
        this.inventory = inventory;
        this.actions = actions;
    }

    /**
     * Register this GUI’s click-listener with the server.
     *
     * @param plugin
     *         your JavaPlugin instance
     */
    public void register(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Opens the GUI for a player.
     *
     * @param player
     *         who to show it to
     */
    public void open(Player player) {
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        int slot = event.getSlot();
        var list = actions.get(slot);
        if (list != null) {
            list.forEach(action -> action.execute(event));
        } else {
            event.setCancelled(true);
        }
    }
}

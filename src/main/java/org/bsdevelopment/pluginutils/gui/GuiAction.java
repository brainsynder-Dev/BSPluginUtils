package org.bsdevelopment.pluginutils.gui;

import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * A behavior to run when a GUI component is clicked.
 */
public interface GuiAction {
    /**
     * Execute this action in response to a click event.
     *
     * @param event
     *         the InventoryClickEvent
     */
    void execute(InventoryClickEvent event);
}

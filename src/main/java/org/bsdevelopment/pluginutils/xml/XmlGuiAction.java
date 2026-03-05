package org.bsdevelopment.pluginutils.xml;

import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * A behavior to execute when a GUI slot is clicked.
 *
 * <p>Registered via {@link XmlActionRegistry} under a type key, and instantiated from an
 * {@link org.bsdevelopment.pluginutils.xml.model.XmlActionDefinition} by an
 * {@link XmlActionFactory}.
 *
 * <p>Example registration:
 * <pre>{@code
 * XmlActionRegistry.register("warp", def -> event -> {
 *     event.setCancelled(true);
 *     if (event.getWhoClicked() instanceof Player player)
 *         player.performCommand("warp " + def.getText());
 * });
 * }</pre>
 */
@FunctionalInterface
public interface XmlGuiAction {

    /**
     * Execute this action in response to a slot click.
     *
     * @param event the inventory click event
     */
    void execute(InventoryClickEvent event);
}

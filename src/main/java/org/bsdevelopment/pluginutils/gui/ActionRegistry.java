package org.bsdevelopment.pluginutils.gui;

import org.bsdevelopment.pluginutils.gui.loader.XmlGuiInput;
import org.bsdevelopment.pluginutils.inventory.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of named action‐factories, so you can handle <action> tags.
 */
public final class ActionRegistry {
    private static final Map<String, ActionFactory> FACTORIES = new HashMap<>();

    private ActionRegistry() {
    }

    /**
     * Register a new action type.
     *
     * @param name
     *         the XML “type” attribute (case‐insensitive)
     * @param factory
     *         how to build the action
     */
    public static void register(String name, ActionFactory factory) {
        FACTORIES.put(name.toLowerCase(), factory);
    }

    /**
     * Parse an <action> element.
     *
     * @param element
     *         the XML element
     *
     * @return empty if no factory found
     */
    public static Optional<GuiAction> parse(Element element) {
        String type = element.getAttribute("type").toLowerCase();
        ActionFactory factory = FACTORIES.get(type);
        return factory == null ? Optional.empty() : Optional.of(factory.create(element));
    }

    static {
        // 1) Send a chat message
        register("message", elm -> event -> {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player)
                player.sendMessage(elm.getTextContent().trim());
        });

        // 2) Close the inventory
        register("close", elm -> event -> {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player)
                player.closeInventory();
        });

        // 3) Run a command as the player
        register("command", elm -> event -> {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player)
                player.chat(elm.getTextContent().trim());
        });

        // 4) Give an item (by template id or fallback to material)
        register("give", elm -> {
            String itemId = elm.getAttribute("item-id");
            String raw = elm.getTextContent().trim();
            return event -> {
                event.setCancelled(true);
                if (!(event.getWhoClicked() instanceof Player player)) return;

                // Attempt to give a defined template
                if (!itemId.isBlank()) {
                    ItemBuilder tmpl = XmlGuiInput.getDefinition(itemId);
                    if (tmpl != null) {
                        var toGive = tmpl.build();
                        if (elm.hasAttribute("amount")) {
                            toGive.setAmount(Integer.parseInt(elm.getAttribute("amount")));
                        }
                        player.getInventory().addItem(toGive);
                        return;
                    }
                }

                // Fallback to raw material [amount]
                String[] parts = raw.split("\\s+", 2);
                var mat = Material.valueOf(parts[0].toUpperCase());
                int amt = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                player.getInventory().addItem(new ItemStack(mat, amt));
            };
        });
    }
}

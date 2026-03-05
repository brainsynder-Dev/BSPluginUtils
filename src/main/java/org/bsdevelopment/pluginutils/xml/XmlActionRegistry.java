package org.bsdevelopment.pluginutils.xml;

import org.bsdevelopment.pluginutils.item.ItemRegistry;
import org.bsdevelopment.pluginutils.text.Colorize;
import org.bsdevelopment.pluginutils.xml.model.XmlActionDefinition;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Central registry of named {@link XmlActionFactory} instances.
 *
 * <p>Actions are identified by a lowercase type key matching the {@code type} attribute on
 * {@code <action>} elements in XML.
 *
 * <p>Built-in actions registered automatically:
 * <ul>
 *   <li>{@code message} — sends a colored chat message to the player</li>
 *   <li>{@code close} — closes the player's inventory</li>
 *   <li>{@code command} — runs a command as the player</li>
 *   <li>{@code console-command} — runs a command from the server console</li>
 *   <li>{@code give} — gives the player an item by ItemRegistry key or material+amount</li>
 * </ul>
 *
 * <p>Custom actions can be registered at any time:
 * <pre>{@code
 * XmlActionRegistry.register("warp", def -> event -> {
 *     event.setCancelled(true);
 *     if (event.getWhoClicked() instanceof Player player)
 *         player.performCommand("warp " + def.getText());
 * });
 * }</pre>
 */
public final class XmlActionRegistry {

    private static final Map<String, XmlActionFactory> FACTORIES = new HashMap<>();

    static {
        // ── message ──────────────────────────────────────────────────────────
        // <action type="message">&aHello, {player}!</action>
        register("message", def -> event -> {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                String msg = def.getText()
                        .replace("{player}", player.getName())
                        .replace("{displayname}", player.getDisplayName());
                player.sendMessage(Colorize.translateBungeeHex(msg));
            }
        });

        // ── close ─────────────────────────────────────────────────────────────
        // <action type="close"/>
        register("close", def -> event -> {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player)
                player.closeInventory();
        });

        // ── command ───────────────────────────────────────────────────────────
        // Runs as the player (with leading slash or without).
        // <action type="command">/spawn</action>
        register("command", def -> event -> {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                String cmd = def.getText()
                        .replace("{player}", player.getName())
                        .replace("{displayname}", player.getDisplayName());
                // Strip leading slash — player.performCommand expects no slash
                if (cmd.startsWith("/")) cmd = cmd.substring(1);
                player.performCommand(cmd);
            }
        });

        // ── console-command ───────────────────────────────────────────────────
        // Runs as the server console.
        // <action type="console-command">give {player} diamond 1</action>
        register("console-command", def -> event -> {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                String cmd = def.getText()
                        .replace("{player}", player.getName())
                        .replace("{displayname}", player.getDisplayName());
                if (cmd.startsWith("/")) cmd = cmd.substring(1);
                player.getServer().dispatchCommand(player.getServer().getConsoleSender(), cmd);
            }
        });

        // ── give ──────────────────────────────────────────────────────────────
        // Option A (ItemRegistry reference):
        //   <action type="give" item="myplugin:sword" amount="1"/>
        // Option B (inline material):
        //   <action type="give">DIAMOND 5</action>
        register("give", def -> {
            // Capture once at build time for efficiency
            final String itemRef = def.getAttribute("item");
            final String amountStr = def.getAttribute("amount");
            final String rawText = def.getText();

            return event -> {
                event.setCancelled(true);
                if (!(event.getWhoClicked() instanceof Player player)) return;

                // Option A: ItemRegistry lookup
                if (!itemRef.isBlank()) {
                    Optional<org.bsdevelopment.pluginutils.inventory.ItemBuilder> opt =
                            ItemRegistry.get(itemRef);
                    if (opt.isPresent()) {
                        ItemStack stack = opt.get().build();
                        if (!amountStr.isBlank()) {
                            try {
                                stack.setAmount(Integer.parseInt(amountStr));
                            } catch (NumberFormatException ignored) {
                            }
                        }
                        player.getInventory().addItem(stack);
                        return;
                    }
                }

                // Option B: "MATERIAL [amount]" in text
                String[] parts = rawText.split("\\s+", 2);
                if (parts.length == 0 || parts[0].isBlank()) return;
                try {
                    Material mat = Material.valueOf(parts[0].toUpperCase(Locale.ROOT));
                    int amt = 1;
                    if (parts.length > 1) {
                        try {
                            amt = Integer.parseInt(parts[1]);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    player.getInventory().addItem(new ItemStack(mat, amt));
                } catch (IllegalArgumentException ignored) {
                    // Invalid material — silently skip
                }
            };
        });
    }

    private XmlActionRegistry() {
    }

    /**
     * Register a custom action type.
     *
     * <p>The {@code name} is case-insensitive and stored in lower case. Registering an
     * existing name replaces the previous factory.
     *
     * @param name    the XML {@code type} attribute value (case-insensitive)
     * @param factory factory that builds an {@link XmlGuiAction} from a definition
     */
    public static void register(String name, XmlActionFactory factory) {
        FACTORIES.put(name.toLowerCase(Locale.ROOT), factory);
    }

    /**
     * Parse an {@link XmlActionDefinition} into an {@link XmlGuiAction}.
     *
     * @param definition the parsed action definition
     *
     * @return the constructed action, or {@link Optional#empty()} if the type is unregistered
     */
    public static Optional<XmlGuiAction> parse(XmlActionDefinition definition) {
        XmlActionFactory factory = FACTORIES.get(definition.getType());
        return factory == null ? Optional.empty() : Optional.of(factory.create(definition));
    }

    // -------------------------------------------------------------------------
    // Built-in actions
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if a factory for the given type name is registered.
     *
     * @param type action type key (case-insensitive)
     *
     * @return whether a factory is registered
     */
    public static boolean isRegistered(String type) {
        return FACTORIES.containsKey(type.toLowerCase(Locale.ROOT));
    }
}

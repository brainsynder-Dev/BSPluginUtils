package org.bsdevelopment.pluginutils.xml;

import org.bsdevelopment.pluginutils.inventory.ItemBuilder;
import org.bsdevelopment.pluginutils.xml.model.XmlGuiDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;

public class ExamplePlugin extends JavaPlugin {
    private XmlGuiManager guiManager;

    // Hold compiled GUIs as fields so you can open them at any time
    private XmlGui mainMenu;
    private XmlGui shopGui;
    private XmlGui confirmGui;

    private void registerCustomActions() {
        // Register a "sound" action:
        //   <action type="sound">ENTITY_EXPERIENCE_ORB_PICKUP</action>
        XmlActionRegistry.register("sound", def -> event -> {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                try {
                    Sound sound = Sound.valueOf(def.getText().toUpperCase());
                    player.playSound(player.getLocation(), sound, 1f, 1f);
                } catch (IllegalArgumentException ignored) {
                }
            }
        });

        // Register an "open-gui" action to chain GUIs together:
        //   <action type="open-gui">shop</action>
        XmlActionRegistry.register("open-gui", def -> event -> {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                String guiId = def.getText().trim();
                switch (guiId) {
                    case "main" -> mainMenu.open(player);
                    case "shop" -> shopGui.open(player);
                    case "confirm" -> confirmGui.open(player);
                    default -> player.sendMessage("Unknown GUI: " + guiId);
                }
            }
        });

        // Register an action with custom attributes:
        //   <action type="teleport" world="world" x="0" y="64" z="0"/>
        XmlActionRegistry.register("teleport", def -> event -> {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                String world = def.getAttribute("world");
                double x = Double.parseDouble(def.getAttribute("x"));
                double y = Double.parseDouble(def.getAttribute("y"));
                double z = Double.parseDouble(def.getAttribute("z"));
                var loc = Bukkit.getWorld(world);
                if (loc != null) {
                    player.teleport(new org.bukkit.Location(loc, x, y, z));
                }
            }
        });
    }

    private void registerDefaults() {
        // ── Main menu (3-row chest) ───────────────────────────────────────────
        guiManager.registerDefault("main_menu",
                XmlGuiDefinition.chest("&6&lMain Menu", 3)
                        .define("filler",
                                ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).withName(" "))
                        .define("shop-btn",
                                ItemBuilder.of(Material.EMERALD).withName("&a&lShop")
                                        .addLore("&7Click to open the shop"))
                        .define("exit-btn",
                                ItemBuilder.of(Material.BARRIER).withName("&c&lClose"))

                        // Border fill — ranges and comma lists both work
                        .slot("0-8,18-26").ref("filler").end()

                        // Single slot using a reference, two actions
                        .slot("11").ref("shop-btn")
                        .action("sound", "UI_BUTTON_CLICK")
                        .action("open-gui", "shop")
                        .end()

                        // Inline item with no reference
                        .slot("13")
                        .item(ItemBuilder.of(Material.PLAYER_HEAD).withName("&eYour Profile")
                                .addLore("&7Name: &f{player}"))
                        .action("message", "&eYour name is: &f{player}")
                        .end()

                        // Close button
                        .slot("15").ref("exit-btn")
                        .action("close")
                        .end()
                        .build());

        // ── Shop GUI (4-row chest) ────────────────────────────────────────────
        guiManager.registerDefault("shop",
                XmlGuiDefinition.chest("&2&lShop", 4)
                        .define("filler",
                                ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE).withName(" "))
                        .define("back",
                                ItemBuilder.of(Material.ARROW).withName("&7Back"))

                        .slot("0-8,27-35").ref("filler").end()

                        .slot("10")
                        .item(ItemBuilder.of(Material.DIAMOND_SWORD).withName("&bDiamond Sword")
                                .addLore("&7Price: &a$500", "&eClick to purchase"))
                        .action("sound", "ENTITY_PLAYER_LEVELUP")
                        .action("give", Map.of("item", "minecraft:diamond_sword"), "")
                        .action("message", "&aYou purchased a &bDiamond Sword&a!")
                        .end()

                        .slot("12")
                        .item(ItemBuilder.of(Material.GOLDEN_APPLE).withName("&6Golden Apple")
                                .addLore("&7Price: &a$100", "&eClick to purchase"))
                        .action("give", "GOLDEN_APPLE 1")
                        .action("message", "&aYou purchased a &6Golden Apple&a!")
                        .end()

                        .slot("31").ref("back")
                        .action("open-gui", "main")
                        .end()
                        .build());

        // ── Confirm dialog (DROPPER — typed inventory) ────────────────────────
        guiManager.registerDefault("confirm",
                XmlGuiDefinition.typed("&eAre you sure?", org.bukkit.event.inventory.InventoryType.DROPPER)
                        .slot("0")
                        .item(ItemBuilder.of(Material.LIME_DYE).withName("&aConfirm"))
                        .action("console-command", "give {player} diamond 64")
                        .action("message", "&aDone! Enjoy your diamonds.")
                        .action("close")
                        .end()
                        .slot("2")
                        .item(ItemBuilder.of(Material.RED_DYE).withName("&cCancel"))
                        .action("message", "&cCancelled.")
                        .action("close")
                        .end()
                        .build());
    }

    private void loadGuis() {
        File guiDir = new File(getDataFolder(), "guis");

        // loadOrSaveDefault: reads the file if it exists; otherwise writes the
        // hardcoded default to disk, then compiles it.
        mainMenu = guiManager.loadOrSaveDefault("main_menu", new File(guiDir, "main_menu.xml"));
        shopGui = guiManager.loadOrSaveDefault("shop", new File(guiDir, "shop.xml"));
        confirmGui = guiManager.loadOrSaveDefault("confirm", new File(guiDir, "confirm.xml"));
    }

    private XmlGui loadRawFromFile() {
        // Load directly from file — no default required
        return guiManager.load(new File(getDataFolder(), "guis/custom.xml"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (!cmd.getName().equalsIgnoreCase("menu")) return false;

        if (args.length == 0) {
            // Open the pre-compiled main menu
            mainMenu.open(player);
            return true;
        }

        switch (args[0].toLowerCase()) {

            // Open a specific GUI by name
            case "shop" -> shopGui.open(player);
            case "confirm" -> confirmGui.open(player);

            // Reload all GUIs from their files at runtime (no restart needed)
            case "reload" -> {
                if (!player.hasPermission("myplugin.admin")) {
                    player.sendMessage("No permission.");
                    return true;
                }
                mainMenu = guiManager.reload(new File(getDataFolder(), "guis/main_menu.xml"));
                shopGui = guiManager.reload(new File(getDataFolder(), "guis/shop.xml"));
                confirmGui = guiManager.reload(new File(getDataFolder(), "guis/confirm.xml"));
                player.sendMessage("&aGUIs reloaded.");
            }

            // Re-save a default back to disk (resets customizations)
            case "reset" -> {
                if (!player.hasPermission("myplugin.admin")) return true;
                guiManager.saveDefault("main_menu", new File(getDataFolder(), "guis/main_menu.xml"));
                player.sendMessage("&amain_menu.xml reset to defaults.");
            }

            // Open a completely custom GUI from a file (no registered default)
            case "custom" -> {
                File file = new File(getDataFolder(), "guis/custom.xml");
                if (!file.exists()) {
                    player.sendMessage("&ccustom.xml not found.");
                    return true;
                }
                XmlGui custom = guiManager.load(file);
                custom.open(player);
            }

            default -> player.sendMessage("Usage: /menu [shop|confirm|reload|reset|custom]");
        }

        return true;
    }

    @Override
    public void onEnable() {
        guiManager = new XmlGuiManager(this);

        // Step 1 — register custom actions BEFORE compiling any GUI
        registerCustomActions();

        // Step 2 — register all hardcoded defaults
        registerDefaults();

        // Step 3 — load (or write defaults and load)
        loadGuis();
    }
}

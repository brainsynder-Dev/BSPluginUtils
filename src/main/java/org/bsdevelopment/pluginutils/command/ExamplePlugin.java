package org.bsdevelopment.pluginutils.command;

import org.bsdevelopment.pluginutils.command.arguments.Argument;
import org.bsdevelopment.pluginutils.command.arguments.BooleanArgument;
import org.bsdevelopment.pluginutils.command.arguments.CustomArgument;
import org.bsdevelopment.pluginutils.command.arguments.EnumArgument;
import org.bsdevelopment.pluginutils.command.arguments.GreedyStringArgument;
import org.bsdevelopment.pluginutils.command.arguments.PlayerArgument;
import org.bsdevelopment.pluginutils.command.arguments.StringArgument;
import org.bsdevelopment.pluginutils.command.arguments.suggestions.ArgumentSuggestions;
import org.bsdevelopment.pluginutils.command.exception.ArgumentParseException;
import org.bsdevelopment.pluginutils.command.exception.CommandException;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ExamplePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        registerHealCommand();
        registerGmCommand();
        registerTpCommand();
        registerBroadcastCommand();
        registerAdminCommand();
    }

    // ── /heal [player] ────────────────────────────────────────────────────────
    private void registerHealCommand() {
        CommandBuilder.create("heal")
                .withDescription("Heal yourself or another player")
                .withPermission("example.heal")
                .withArguments(
                        new PlayerArgument("target").setOptional(true)
                )
                .executesPlayer((player, args) -> {
                    Player target = args.getOrDefault("target", player);
                    target.setHealth(target.getMaxHealth());
                    player.sendMessage("§aHealed " + target.getName() + ".");
                    if (!target.equals(player)) target.sendMessage("§aYou were healed by " + player.getName() + ".");
                })
                .register(this);
    }

    // ── /gm <mode> [player] ───────────────────────────────────────────────────
    private void registerGmCommand() {
        CommandBuilder.create("gm")
                .withDescription("Change game mode")
                .withAliases("gamemode")
                .withPermission("example.gamemode")
                .withArguments(
                        new EnumArgument<>("mode", GameMode.class),
                        new PlayerArgument("target").setOptional(true)
                )
                .executesPlayer((player, args) -> {
                    GameMode mode = args.get("mode");
                    Player target = args.getOrDefault("target", player);
                    target.setGameMode(mode);
                    player.sendMessage("§aSet " + target.getName() + " to " + mode.name().toLowerCase() + ".");
                })
                .register(this);
    }

    // ── /tp <player> ──────────────────────────────────────────────────────────
    private void registerTpCommand() {
        CommandBuilder.create("tp")
                .withDescription("Teleport to a player")
                .withPermission("example.tp")
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(new PlayerArgument("target"))
                .executesPlayer((player, args) -> {
                    Player target = args.get("target");
                    player.teleport(target.getLocation());
                    player.sendMessage("§aTeleported to §e" + target.getName() + "§a.");
                })
                .register(this);
    }

    // ── /broadcast <message…> ─────────────────────────────────────────────────
    private void registerBroadcastCommand() {
        CommandBuilder.create("broadcast")
                .withDescription("Broadcast a message to all players")
                .withAliases("bc")
                .withPermission(CommandPermission.OP)
                .withArguments(new GreedyStringArgument("message"))
                .executes((sender, args) -> {
                    String message = args.get("message");
                    getServer().broadcastMessage("§6[Broadcast] §f" + message);
                })
                .register(this);
    }

    // ── /admin <subcommand> ───────────────────────────────────────────────────
    private void registerAdminCommand() {
        // Custom argument: parses an online player's name into their display name.
        Argument<String> displayNameArg = new CustomArgument<>(
                new StringArgument("player"),
                info -> {
                    Player target = getServer().getPlayerExact(info.input());
                    if (target == null) throw ArgumentParseException.fromString("Player not online: " + info.input());
                    return target.getDisplayName();
                }
        ).replaceSuggestions(ArgumentSuggestions.of(info ->
                getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .toList()
        ));

        CommandBuilder.create("admin")
                .withDescription("Admin utility commands")
                .withPermission("example.admin")

                // /admin info <player> — prints player info to sender
                .withSubcommand(CommandBuilder.create("info")
                        .withArguments(new PlayerArgument("target"))
                        .executes((sender, args) -> {
                            Player target = args.get("target");
                            sender.sendMessage("§eName: §f" + target.getName());
                            sender.sendMessage("§eGamemode: §f" + target.getGameMode().name().toLowerCase());
                            sender.sendMessage("§eWorld: §f" + target.getWorld().getName());
                            sender.sendMessage("§eHealth: §f" + String.format("%.1f", target.getHealth()));
                        })
                )

                // /admin kick <player> [reason…] — kicks a player with an optional reason
                .withSubcommand(CommandBuilder.create("kick")
                        .withPermission("example.admin.kick")
                        .withArguments(
                                new PlayerArgument("target"),
                                new GreedyStringArgument("reason").setOptional(true)
                        )
                        .executes((sender, args) -> {
                            Player target = args.get("target");
                            String reason = args.getOrDefault("reason", "You have been kicked.");
                            target.kickPlayer("§c" + reason);
                            sender.sendMessage("§aKicked §e" + target.getName() + "§a: " + reason);
                        })
                )

                // /admin fly <set|toggle> <player> — demonstrates LiteralArgument branching
                .withSubcommand(CommandBuilder.create("fly")
                        .withSubcommand(CommandBuilder.create("set")
                                .withArguments(
                                        new PlayerArgument("target"),
                                        new BooleanArgument("value")
                                )
                                .executes((sender, args) -> {
                                    Player target = args.get("target");
                                    boolean value = args.get("value");
                                    target.setAllowFlight(value);
                                    target.setFlying(value);
                                    sender.sendMessage("§aFlight §f" + (value ? "enabled" : "disabled") + "§a for §e" + target.getName() + "§a.");
                                })
                        )
                        .withSubcommand(CommandBuilder.create("toggle")
                                .withArguments(new PlayerArgument("target"))
                                .executes((sender, args) -> {
                                    Player target = args.get("target");
                                    boolean next = !target.getAllowFlight();
                                    target.setAllowFlight(next);
                                    target.setFlying(next);
                                    sender.sendMessage("§aToggled flight for §e" + target.getName() + "§a.");
                                })
                        )
                )

                // /admin displayname <player> — uses the CustomArgument defined above
                .withSubcommand(CommandBuilder.create("displayname")
                        .withArguments(displayNameArg)
                        .executes((sender, args) -> {
                            String displayName = args.get("player");
                            sender.sendMessage("§eDisplay name: §f" + displayName);
                        })
                )

                // /admin warn <world> set spawn — chained LiteralArguments example
                .withSubcommand(CommandBuilder.create("setspawn")
                        .withRequirement(sender -> sender instanceof Player)
                        .executesPlayer((player, args) -> {
                            // In a real plugin you'd persist this location
                            player.sendMessage("§aSpawn set to your current location in §e" + player.getWorld().getName() + "§a.");
                        })
                )

                // /admin error — demonstrates throwing CommandException from an executor
                .withSubcommand(CommandBuilder.create("error")
                        .executes((sender, args) -> {
                            throw new CommandException("This is a demonstration of CommandException — shown in red to the sender.");
                        })
                ).register(this);
    }
}

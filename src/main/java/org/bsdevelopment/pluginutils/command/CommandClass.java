package org.bsdevelopment.pluginutils.command;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Marker interface for commands that live in their own class.
 * <p>
 * Implement {@link #build()} to return your fully-configured
 * {@link CommandBuilder}, then call {@link #register(JavaPlugin)} from your
 * plugin's {@code onEnable}. No constructor injection or boilerplate needed.
 *
 * <pre>{@code
 * public class HealCommand implements CommandClass {
 *     @Override
 *     public CommandBuilder build() {
 *         return CommandBuilder.create("heal")
 *                 .withPermission("example.heal")
 *                 .executesPlayer((player, args) -> player.setHealth(player.getMaxHealth()));
 *     }
 * }
 *
 * // In onEnable:
 * new HealCommand().register(this);
 * }</pre>
 */
public interface CommandClass {

    /**
     * Builds and returns the fully-configured {@link CommandBuilder} for this command.
     *
     * @return the command builder
     */
    CommandBuilder build();

    /**
     * Registers this command with the server.
     * <p>
     * The default implementation simply calls {@code build().register(plugin)}.
     *
     * @param plugin the owning plugin
     */
    default void register(JavaPlugin plugin) {
        build().register(plugin);
    }
}

package org.bsdevelopment.pluginutils;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * A utility class for initializing and retrieving a {@link TaskScheduler} instance.
 *
 * <p>Use {@link #initialize(Plugin)} once in your plugin's onEnable or an early setup stage,
 * then call {@link PluginUtilities#getScheduler()} whenever you need to schedule tasks.
 *
 * <p><b>Example Usage:</b>
 * <pre>
 * public final class MyPlugin extends JavaPlugin {
 *     &#64;Override
 *     public void onEnable() {
 *         PluginUtilities.initialize(this);
 *         TaskScheduler scheduler = PluginUtilities.getTaskScheduler();
 *         // Schedule tasks using the returned scheduler...
 *     }
 * }
 * </pre>
 */
public final class PluginUtilities extends JavaPlugin {
    private static Plugin plugin;
    private static TaskScheduler scheduler;

    public static void initialize(Plugin plugin) {
        scheduler = UniversalScheduler.getScheduler(plugin);
    }

    public static Plugin getPlugin() {
        return plugin;
    }

    /**
     * Retrieves the {@link TaskScheduler} instance for scheduling tasks.
     *
     * <p><b>Example:</b>
     * <pre>
     * TaskScheduler scheduler = PluginUtilities.getScheduler();
     * scheduler.runTask(() -> {
     *     // Task logic here
     * });
     * </pre>
     *
     * @return the initialized {@link TaskScheduler}
     *
     * @throws UnsupportedOperationException if {@link #initialize(Plugin)} was never called
     */
    public static TaskScheduler getScheduler() {
        if (scheduler == null)
            throw new UnsupportedOperationException("PluginUtilities.initialize() has not been initialized before this method was called.");
        return scheduler;
    }
}

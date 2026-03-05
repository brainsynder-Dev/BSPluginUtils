package org.bsdevelopment.pluginutils.command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.logging.Level;

public class CommandManager {
    private static CommandMap commandMap;

    public static CommandMap getCommandMap() {
        if (commandMap != null) return commandMap;
        try {
            Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            commandMap = (CommandMap) field.get(Bukkit.getServer());
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "[BSPluginUtils] Failed to obtain CommandMap", e);
        }
        return commandMap;
    }

    public static void register(Plugin plugin, CommandBuilder command) {
        CommandMap map = getCommandMap();
        if (map == null) return;
        map.register(plugin.getName().toLowerCase(), command.toBukkitCommand(plugin));
    }
}

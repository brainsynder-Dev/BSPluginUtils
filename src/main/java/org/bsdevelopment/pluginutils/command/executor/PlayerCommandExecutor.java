package org.bsdevelopment.pluginutils.command.executor;

import org.bsdevelopment.pluginutils.command.CommandArguments;
import org.bsdevelopment.pluginutils.command.exception.CommandException;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface PlayerCommandExecutor {
    void execute(Player player, CommandArguments args) throws CommandException;
}

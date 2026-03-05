package org.bsdevelopment.pluginutils.command.executor;

import org.bsdevelopment.pluginutils.command.CommandArguments;
import org.bsdevelopment.pluginutils.command.exception.CommandException;
import org.bukkit.command.CommandSender;

@FunctionalInterface
public interface CommandExecutor {
    void execute(CommandSender sender, CommandArguments args) throws CommandException;
}

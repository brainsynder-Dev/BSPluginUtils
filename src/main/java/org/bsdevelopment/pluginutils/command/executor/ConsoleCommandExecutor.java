package org.bsdevelopment.pluginutils.command.executor;

import org.bsdevelopment.pluginutils.command.CommandArguments;
import org.bsdevelopment.pluginutils.command.exception.CommandException;
import org.bukkit.command.ConsoleCommandSender;

@FunctionalInterface
public interface ConsoleCommandExecutor {
    void execute(ConsoleCommandSender console, CommandArguments args) throws CommandException;
}

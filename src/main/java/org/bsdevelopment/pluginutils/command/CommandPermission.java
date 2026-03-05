package org.bsdevelopment.pluginutils.command;

import org.bukkit.command.CommandSender;

public class CommandPermission {
    public static final CommandPermission NONE = new CommandPermission(null, Type.NONE);
    public static final CommandPermission OP = new CommandPermission(null, Type.OP);

    private final String node;
    private final Type type;

    private CommandPermission(String node, Type type) {
        this.node = node;
        this.type = type;
    }

    public static CommandPermission of(String node) {
        return new CommandPermission(node, Type.NODE);
    }

    public boolean test(CommandSender sender) {
        return switch (type) {
            case NONE -> true;
            case OP -> sender.isOp();
            case NODE -> sender.hasPermission(node);
        };
    }

    public String getNode() {
        return node;
    }

    private enum Type {NONE, OP, NODE}
}

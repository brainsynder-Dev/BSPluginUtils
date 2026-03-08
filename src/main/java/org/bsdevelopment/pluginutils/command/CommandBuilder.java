package org.bsdevelopment.pluginutils.command;

import org.bsdevelopment.pluginutils.command.arguments.Argument;
import org.bsdevelopment.pluginutils.command.arguments.GreedyArgument;
import org.bsdevelopment.pluginutils.command.arguments.LiteralArgument;
import org.bsdevelopment.pluginutils.command.arguments.suggestions.SuggestionInfo;
import org.bsdevelopment.pluginutils.command.exception.ArgumentParseException;
import org.bsdevelopment.pluginutils.command.exception.CommandException;
import org.bsdevelopment.pluginutils.command.executor.CommandExecutor;
import org.bsdevelopment.pluginutils.command.executor.ConsoleCommandExecutor;
import org.bsdevelopment.pluginutils.command.executor.PlayerCommandExecutor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CommandBuilder {
    private final String name;
    private final List<Argument<?>> arguments = new ArrayList<>();
    private final Map<String, CommandBuilder> subcommands = new LinkedHashMap<>();
    private String description = "";
    private List<String> aliases = new ArrayList<>();
    private CommandPermission permission = CommandPermission.NONE;
    private Predicate<CommandSender> requirement = sender -> true;
    private CommandExecutor anyExecutor;
    private PlayerCommandExecutor playerExecutor;
    private ConsoleCommandExecutor consoleExecutor;

    private CommandBuilder(String name) {
        this.name = name;
    }

    public static CommandBuilder create(String name) {
        return new CommandBuilder(name);
    }

    private static void collectPermissionsInto(CommandBuilder cmd, Set<String> result) {
        String node = cmd.permission.getNode();
        if (node != null) result.add(node);

        for (CommandBuilder sub : cmd.subcommands.values().stream().distinct().toList()) {
            collectPermissionsInto(sub, result);
        }
    }

    public CommandBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public CommandBuilder withAliases(String... aliases) {
        this.aliases = Arrays.asList(aliases);
        return this;
    }

    public CommandBuilder withPermission(String node) {
        this.permission = CommandPermission.of(node);
        return this;
    }

    public CommandBuilder withPermission(CommandPermission permission) {
        this.permission = permission;
        return this;
    }

    public CommandBuilder withRequirement(Predicate<CommandSender> requirement) {
        this.requirement = requirement;
        return this;
    }

    public CommandBuilder withArguments(Argument<?>... args) {
        this.arguments.addAll(Arrays.asList(args));
        return this;
    }

    public CommandBuilder withSubcommand(CommandBuilder subcommand) {
        subcommands.put(subcommand.name.toLowerCase(), subcommand);
        for (String alias : subcommand.aliases) {
            subcommands.put(alias.toLowerCase(), subcommand);
        }
        return this;
    }

    public CommandBuilder executes(CommandExecutor executor) {
        this.anyExecutor = executor;
        return this;
    }

    public CommandBuilder executesPlayer(PlayerCommandExecutor executor) {
        this.playerExecutor = executor;
        return this;
    }

    public CommandBuilder executesConsole(ConsoleCommandExecutor executor) {
        this.consoleExecutor = executor;
        return this;
    }

    public void register(Plugin plugin) {
        CommandManager.register(plugin, this);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public CommandPermission getPermission() {
        return permission;
    }

    public Predicate<CommandSender> getRequirement() {
        return requirement;
    }

    public List<String> getAliases() {
        return Collections.unmodifiableList(aliases);
    }

    public List<Argument<?>> getArguments() {
        return Collections.unmodifiableList(arguments);
    }

    public Collection<CommandBuilder> getSubcommands() {
        return subcommands.values().stream().distinct().toList();
    }

    public Set<String> collectPermissions() {
        Set<String> result = new LinkedHashSet<>();
        collectPermissionsInto(this, result);
        return result;
    }

    Command toBukkitCommand(Plugin plugin) {
        CommandBuilder self = this;
        Command cmd = new Command(name, description, "/" + name, aliases) {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                return self.handleExecution(sender, label, args);
            }

            @Override
            public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
                return self.handleTabComplete(sender, alias, args);
            }
        };
        if (permission != null && permission != CommandPermission.NONE) {
            String node = permission.getNode();
            if (node != null) cmd.setPermission(node);
        }
        return cmd;
    }

    boolean handleExecution(CommandSender sender, String label, String[] args) {
        if (!permission.test(sender)) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }
        if (!requirement.test(sender)) {
            sender.sendMessage("§cYou do not meet the requirements to use this command.");
            return true;
        }

        if (args.length > 0) {
            CommandBuilder sub = subcommands.get(args[0].toLowerCase());
            if (sub != null) {
                return sub.handleExecution(sender, args[0], Arrays.copyOfRange(args, 1, args.length));
            }
        }

        CommandArguments parsedArgs;
        try {
            parsedArgs = parseArguments(sender, args);
        } catch (ArgumentParseException e) {
            sender.sendMessage("§c" + e.getMessage());
            return true;
        }

        try {
            if (sender instanceof Player player && playerExecutor != null) {
                playerExecutor.execute(player, parsedArgs);
            } else if (sender instanceof ConsoleCommandSender console && consoleExecutor != null) {
                consoleExecutor.execute(console, parsedArgs);
            } else if (anyExecutor != null) {
                anyExecutor.execute(sender, parsedArgs);
            } else if (sender instanceof Player && playerExecutor == null) {
                sender.sendMessage("§cThis command cannot be run by a player.");
            } else if (sender instanceof ConsoleCommandSender && consoleExecutor == null) {
                sender.sendMessage("§cThis command cannot be run from the console.");
            } else {
                sender.sendMessage("§cThis command has no executor.");
            }
        } catch (CommandException e) {
            sender.sendMessage("§c" + e.getMessage());
        }
        return true;
    }

    List<String> handleTabComplete(CommandSender sender, String alias, String[] args) {
        if (!permission.test(sender)) return Collections.emptyList();
        if (!requirement.test(sender)) return Collections.emptyList();

        String currentToken = args.length > 0 ? args[args.length - 1] : "";

        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            for (String subName : subcommands.keySet()) {
                CommandBuilder sub = subcommands.get(subName);
                if (sub.permission.test(sender) && sub.requirement.test(sender)) {
                    suggestions.add(subName);
                }
            }
            if (!arguments.isEmpty()) {
                Argument<?> first = arguments.get(0);
                SuggestionInfo info = new SuggestionInfo(sender, CommandArguments.EMPTY, currentToken, currentToken);
                suggestions.addAll(first.getSuggestions(info));
            }
            return filterAndSort(suggestions, currentToken);
        }

        if (args.length > 1) {
            CommandBuilder sub = subcommands.get(args[0].toLowerCase());
            if (sub != null) {
                return sub.handleTabComplete(sender, args[0], Arrays.copyOfRange(args, 1, args.length));
            }
        }

        int argIndex = computeArgIndex(args);
        if (argIndex < arguments.size()) {
            Argument<?> arg = arguments.get(argIndex);
            CommandArguments previousArgs = buildPartialArgs(sender, args, argIndex);
            SuggestionInfo info = new SuggestionInfo(sender, previousArgs, currentToken, currentToken);
            return filterAndSort(new ArrayList<>(arg.getSuggestions(info)), currentToken);
        }

        return Collections.emptyList();
    }

    private CommandArguments parseArguments(CommandSender sender, String[] rawArgs) throws ArgumentParseException {
        CommandArguments.Builder builder = CommandArguments.builder();
        int rawIndex = 0;

        for (Argument<?> arg : arguments) {
            if (rawIndex >= rawArgs.length) {
                if (arg.isOptional()) continue;
                throw ArgumentParseException.fromString("Missing required argument: " + arg.getNodeName());
            }

            if (arg instanceof LiteralArgument literal) {
                literal.parse(sender, rawArgs[rawIndex]);
                rawIndex++;
                continue;
            }

            if (arg instanceof GreedyArgument) {
                String joined = String.join(" ", Arrays.copyOfRange(rawArgs, rawIndex, rawArgs.length));
                builder.put(arg.getNodeName(), arg.parse(sender, joined));
                break;
            }

            builder.put(arg.getNodeName(), arg.parse(sender, rawArgs[rawIndex]));
            rawIndex++;
        }

        return builder.build();
    }

    private int computeArgIndex(String[] args) {
        int filled = args.length - 1;
        int literalCount = 0;
        for (int i = 0; i < Math.min(filled, arguments.size()); i++) {
            if (arguments.get(i) instanceof LiteralArgument) literalCount++;
        }
        return filled - literalCount;
    }

    private CommandArguments buildPartialArgs(CommandSender sender, String[] args, int upToIndex) {
        CommandArguments.Builder builder = CommandArguments.builder();
        int rawIndex = 0;
        int logicalIndex = 0;
        for (Argument<?> arg : arguments) {
            if (logicalIndex >= upToIndex) break;
            if (rawIndex >= args.length - 1) break;
            try {
                if (arg instanceof LiteralArgument) {
                    rawIndex++;
                    continue;
                }
                builder.put(arg.getNodeName(), arg.parse(sender, args[rawIndex]));
                rawIndex++;
                logicalIndex++;
            } catch (ArgumentParseException ignored) {
                rawIndex++;
                logicalIndex++;
            }
        }
        return builder.build();
    }

    private List<String> filterAndSort(List<String> suggestions, String prefix) {
        return suggestions.stream()
                .distinct()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
    }
}

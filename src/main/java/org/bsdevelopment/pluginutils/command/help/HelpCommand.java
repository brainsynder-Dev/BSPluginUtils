package org.bsdevelopment.pluginutils.command.help;

import org.bsdevelopment.pluginutils.command.CommandBuilder;
import org.bsdevelopment.pluginutils.command.CommandPermission;
import org.bsdevelopment.pluginutils.command.arguments.Argument;
import org.bsdevelopment.pluginutils.command.arguments.IntegerArgument;
import org.bsdevelopment.pluginutils.command.arguments.LiteralArgument;
import org.bsdevelopment.pluginutils.storage.ListPager;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class HelpCommand {
    private final CommandBuilder parent;
    private String name = "help";
    private int pageSize = 6;
    private HelpFormatter formatter = new PlainTextHelpFormatter();
    private CommandPermission permission = CommandPermission.NONE;

    private HelpCommand(CommandBuilder parent) {
        this.parent = parent;
    }

    public static HelpCommand of(CommandBuilder parent) {
        return new HelpCommand(parent);
    }

    public static PlainTextHelpFormatter plainFormatter() {
        return new PlainTextHelpFormatter();
    }

    public static TellrawHelpFormatter tellrawFormatter() {
        return new TellrawHelpFormatter();
    }

    private static List<HelpEntry> collectEntries(CommandBuilder parent, CommandSender sender, String helpName) {
        List<HelpEntry> entries = new ArrayList<>();
        collectEntriesRecursive(parent.getName(), parent.getSubcommands(), sender, helpName, entries);
        return entries;
    }

    private static void collectEntriesRecursive(String path, Collection<CommandBuilder> subs, CommandSender sender, String helpName, List<HelpEntry> entries) {
        for (CommandBuilder sub : subs) {
            if (sub.getName().equalsIgnoreCase(helpName)) continue;
            if (!sub.getPermission().test(sender)) continue;
            if (!sub.getRequirement().test(sender)) continue;

            entries.add(new HelpEntry(buildUsage(path, sub), sub.getDescription()));

            Collection<CommandBuilder> children = sub.getSubcommands();
            if (!children.isEmpty()) {
                collectEntriesRecursive(path + " " + sub.getName(), children, sender, helpName, entries);
            }
        }
    }

    private static String buildUsage(String path, CommandBuilder sub) {
        StringBuilder sb = new StringBuilder(path).append(' ').append(sub.getName());
        for (Argument<?> arg : sub.getArguments()) {
            if (arg instanceof LiteralArgument) {
                sb.append(' ').append(arg.getNodeName());
            } else if (arg.isOptional()) {
                sb.append(" [").append(arg.getNodeName()).append(']');
            } else {
                sb.append(" <").append(arg.getNodeName()).append('>');
            }
        }
        return sb.toString();
    }

    /**
     * Overrides the name of the generated help subcommand (default: {@code "help"}).
     *
     * @param name the subcommand name to use
     *
     * @return this builder, for chaining
     */
    public HelpCommand named(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the number of entries shown per page (default: {@code 6}).
     *
     * @param size the number of entries per page; must be at least 1
     *
     * @return this builder, for chaining
     */
    public HelpCommand withPageSize(int size) {
        this.pageSize = size;
        return this;
    }

    /**
     * Replaces the output formatter used to render help messages (default: {@link PlainTextHelpFormatter}).
     *
     * <p>Use {@link HelpCommand#plainFormatter()} or {@link HelpCommand#tellrawFormatter()} to obtain
     * a pre-configured instance, or supply a custom {@link HelpFormatter} implementation.
     *
     * @param formatter the formatter to use
     *
     * @return this builder, for chaining
     */
    public HelpCommand withFormatter(HelpFormatter formatter) {
        this.formatter = formatter;
        return this;
    }

    /**
     * Requires the given permission node to run the help subcommand.
     *
     * <p>Console senders always pass this check regardless of the node specified.
     *
     * @param node the permission node to require (e.g. {@code "myplugin.admin.help"})
     *
     * @return this builder, for chaining
     */
    public HelpCommand withPermission(String node) {
        this.permission = CommandPermission.of(node);
        return this;
    }

    /**
     * Requires the given {@link CommandPermission} to run the help subcommand.
     *
     * <p>Console senders always pass this check regardless of the permission specified.
     *
     * @param permission the permission to require
     *
     * @return this builder, for chaining
     *
     * @see #withPermission(String)
     */
    public HelpCommand withPermission(CommandPermission permission) {
        this.permission = permission;
        return this;
    }

    public CommandBuilder build() {
        String helpName = this.name;
        int localSize = this.pageSize;
        HelpFormatter localFormatter = this.formatter;

        IntegerArgument pageArgument = (IntegerArgument) new IntegerArgument("page", 1, Integer.MAX_VALUE).setOptional(true);

        return CommandBuilder.create(helpName)
                .withPermission(permission)
                .withArguments(pageArgument)
                .executes((sender, args) -> {
                    List<HelpEntry> entries = collectEntries(parent, sender, helpName);

                    if (entries.isEmpty()) {
                        localFormatter.sendEmpty(sender);
                        return;
                    }

                    ListPager<HelpEntry> pager = new ListPager<>(localSize, entries);
                    int totalPages = pager.totalPages();
                    int page = args.get("page") != null ? (int) args.get("page") : 1;

                    if (!pager.exists(page)) {
                        localFormatter.sendInvalidPage(sender, totalPages);
                        return;
                    }

                    localFormatter.sendHeader(sender, parent.getName(), page, totalPages);
                    for (HelpEntry entry : pager.getPage(page)) {
                        localFormatter.sendEntry(sender, entry);
                    }
                    localFormatter.sendFooter(sender, parent.getName(), helpName, page, totalPages);
                });
    }
}

package org.bsdevelopment.pluginutils.command.help;

import org.bukkit.command.CommandSender;

/**
 * Controls how the help subcommand renders its output.
 *
 * <p>Two built-in implementations are available via {@link HelpCommand}:
 * <ul>
 *   <li>{@link HelpCommand#plainFormatter()} — colorized plain-text with configurable templates</li>
 *   <li>{@link HelpCommand#tellrawFormatter()} — clickable/hoverable tellraw components</li>
 * </ul>
 *
 * <p>Custom implementations can be supplied via {@link HelpCommand#withFormatter(HelpFormatter)}.
 */
public interface HelpFormatter {

    /**
     * Sends the page header (called once before any entries).
     *
     * @param sender     the command sender
     * @param command    the parent command name
     * @param page       the current page number (1-based)
     * @param totalPages the total number of pages
     */
    void sendHeader(CommandSender sender, String command, int page, int totalPages);

    /**
     * Sends a single help entry.
     *
     * @param sender the command sender
     * @param entry  the entry containing usage path and description
     */
    void sendEntry(CommandSender sender, HelpEntry entry);

    /**
     * Sends the message shown when no accessible commands were found.
     *
     * @param sender the command sender
     */
    void sendEmpty(CommandSender sender);

    /**
     * Sends the message shown when the requested page does not exist.
     *
     * @param sender     the command sender
     * @param totalPages the total number of available pages
     */
    void sendInvalidPage(CommandSender sender, int totalPages);

    /**
     * Sends the navigation footer shown after all entries on the current page.
     *
     * <p>The default implementation is a no-op, so existing custom {@link HelpFormatter}
     * implementations are not required to override this method.
     *
     * @param sender     the command sender
     * @param command    the parent command name (e.g. {@code "admin"})
     * @param helpName   the help subcommand name (e.g. {@code "help"})
     * @param page       the current page number (1-based)
     * @param totalPages the total number of pages
     */
    default void sendFooter(CommandSender sender, String command, String helpName, int page, int totalPages) {
    }
}

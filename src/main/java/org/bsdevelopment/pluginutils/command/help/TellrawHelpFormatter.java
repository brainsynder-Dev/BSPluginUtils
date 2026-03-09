package org.bsdevelopment.pluginutils.command.help;

import org.bsdevelopment.pluginutils.chat.TellrawMessage;
import org.bsdevelopment.pluginutils.chat.decoration.NamedTextColor;
import org.bukkit.command.CommandSender;

/**
 * A {@link HelpFormatter} that sends interactive chat components for players using
 * {@link TellrawMessage}.
 *
 * <p>Messages are delivered via {@code player.spigot().sendMessage()} — compatible with
 * both Spigot and Paper. Console senders automatically receive a plain-text fallback.
 *
 * <p>Each entry is rendered as a clickable line:
 * <ul>
 *   <li><b>Click</b> — suggests the base command in the player's chat bar</li>
 *   <li><b>Hover</b> — shows the description, or a fallback hint if none is set</li>
 * </ul>
 *
 * <p>After the last entry a navigation footer is shown with clickable
 * {@code [◀ Prev]} and {@code [Next ▶]} buttons (only the buttons relevant to the
 * current page are included). The footer is omitted entirely when there is only one page.
 *
 * <p>Example:
 * <pre>{@code
 * HelpCommand.of(admin)
 *     .withFormatter(HelpCommand.tellrawFormatter()
 *         .usageColor(NamedTextColor.AQUA)
 *         .descriptionColor(NamedTextColor.WHITE))
 *     .build();
 * }</pre>
 */
public class TellrawHelpFormatter implements HelpFormatter {

    private String headerTemplate = "&6=== Help: /{command} (Page {page}/{pages}) ===";
    private String emptyTemplate = "&cNo commands available.";
    private String invalidPageTemplate = "&cInvalid page. There are {pages} page(s) available.";

    private NamedTextColor usageColor = NamedTextColor.YELLOW;
    private NamedTextColor descriptionColor = NamedTextColor.GRAY;
    private NamedTextColor separatorColor = NamedTextColor.DARK_GRAY;
    private NamedTextColor navColor = NamedTextColor.GOLD;

    private String prevLabel = "[◀ Prev]";
    private String nextLabel = "[Next ▶]";

    private boolean displayDescription = false;

    /**
     * Strips argument placeholders from a usage path so the player's chat bar is
     * pre-filled with just the base command and a trailing space.
     *
     * <p>For example, {@code "admin kick <target> [reason]"} becomes {@code "/admin kick "}.
     *
     * @param usage the full usage string
     *
     * @return the suggest-command string prefixed with {@code /} and suffixed with a space
     */
    private static String buildSuggestCommand(String usage) {
        StringBuilder sb = new StringBuilder("/");
        for (String token : usage.split(" ")) {
            if (token.startsWith("<") || token.startsWith("[")) break;
            if (sb.length() > 1) sb.append(' ');
            sb.append(token);
        }
        sb.append(' ');
        return sb.toString();
    }


    /**
     * Sets the header template shown once before any entries.
     *
     * <p>Supports the placeholders {@code {command}}, {@code {page}}, and {@code {pages}}.
     *
     * @param template the new header template string
     *
     * @return this formatter, for chaining
     */
    public TellrawHelpFormatter header(String template) {
        this.headerTemplate = template;
        return this;
    }

    /**
     * Sets the message shown when no accessible commands are found.
     *
     * @param template the new empty-list message template
     *
     * @return this formatter, for chaining
     */
    public TellrawHelpFormatter empty(String template) {
        this.emptyTemplate = template;
        return this;
    }

    /**
     * Sets the message shown when the requested page does not exist.
     *
     * <p>Supports the placeholder {@code {pages}}.
     *
     * @param template the new invalid-page message template
     *
     * @return this formatter, for chaining
     */
    public TellrawHelpFormatter invalidPage(String template) {
        this.invalidPageTemplate = template;
        return this;
    }

    /**
     * Controls whether the description is displayed inline after the usage text.
     *
     * <p>When {@code true}, each entry shows {@code /usage - description}.
     * When {@code false} (the default), only the usage is shown; the description
     * is still available via the hover tooltip.
     *
     * @param displayDescription {@code true} to show descriptions inline
     *
     * @return this formatter, for chaining
     */
    public TellrawHelpFormatter displayDescription(boolean displayDescription) {
        this.displayDescription = displayDescription;
        return this;
    }

    /**
     * Sets the color of the usage text (default: yellow).
     *
     * @param color the new usage text color
     *
     * @return this formatter, for chaining
     */
    public TellrawHelpFormatter usageColor(NamedTextColor color) {
        this.usageColor = color;
        return this;
    }

    /**
     * Sets the color of the description text and the hover tooltip (default: gray).
     *
     * @param color the new description color
     *
     * @return this formatter, for chaining
     */
    public TellrawHelpFormatter descriptionColor(NamedTextColor color) {
        this.descriptionColor = color;
        return this;
    }

    /**
     * Sets the color of the {@code " - "} separator between usage and description (default: dark gray).
     *
     * @param color the new separator color
     *
     * @return this formatter, for chaining
     */
    public TellrawHelpFormatter separatorColor(NamedTextColor color) {
        this.separatorColor = color;
        return this;
    }

    /**
     * Sets the color of the {@code [◀ Prev]} and {@code [Next ▶]} navigation buttons
     * in the footer (default: gold).
     *
     * @param color the new navigation button color
     *
     * @return this formatter, for chaining
     */
    public TellrawHelpFormatter navColor(NamedTextColor color) {
        this.navColor = color;
        return this;
    }

    /**
     * Sets the label shown for the previous-page button (default: {@code "[◀ Prev]"}).
     *
     * @param label the new previous-page button label
     *
     * @return this formatter, for chaining
     */
    public TellrawHelpFormatter prevLabel(String label) {
        this.prevLabel = label;
        return this;
    }

    /**
     * Sets the label shown for the next-page button (default: {@code "[Next ▶]"}).
     *
     * @param label the new next-page button label
     *
     * @return this formatter, for chaining
     */
    public TellrawHelpFormatter nextLabel(String label) {
        this.nextLabel = label;
        return this;
    }

    @Override
    public void sendHeader(CommandSender sender, String command, int page, int totalPages) {
        TellrawMessage.of(headerTemplate.replace("{command}", command).replace("{page}", String.valueOf(page))
                .replace("{pages}", String.valueOf(totalPages))).send(sender);
    }

    @Override
    public void sendEntry(CommandSender sender, HelpEntry entry) {
        boolean hasDesc = entry.description() != null && !entry.description().isBlank();
        String suggestCmd = buildSuggestCommand(entry.usage());
        String hoverText = hasDesc ? entry.description() : "Click to suggest command";
        TellrawMessage msg = TellrawMessage.empty().then("/" + entry.usage()).color(usageColor).suggest(suggestCmd).tooltip(hoverText);

        if (hasDesc && displayDescription) {
            msg.then(" - ").color(separatorColor).then(entry.description()).color(descriptionColor);
        }

        msg.send(sender);
    }

    @Override
    public void sendEmpty(CommandSender sender) {
        TellrawMessage.of(emptyTemplate).send(sender);
    }

    @Override
    public void sendInvalidPage(CommandSender sender, int totalPages) {
        TellrawMessage.of(invalidPageTemplate.replace("{pages}", String.valueOf(totalPages))).send(sender);
    }

    @Override
    public void sendFooter(CommandSender sender, String command, String helpName, int page, int totalPages) {
        if (totalPages <= 1) return;

        boolean hasPrev = page > 1;
        boolean hasNext = page < totalPages;
        TellrawMessage footer = TellrawMessage.empty();

        if (hasPrev) {
            footer.then(prevLabel).color(navColor).command("/" + command + " " + helpName + " " + (page - 1)).tooltip("&7Go to page " + (page - 1));
        }

        if (hasPrev && hasNext) {
            footer.then("  ");
        }

        if (hasNext) {
            footer.then(nextLabel).color(navColor).command("/" + command + " " + helpName + " " + (page + 1))
                    .tooltip("&7Go to page " + (page + 1));
        }

        footer.send(sender);
    }
}

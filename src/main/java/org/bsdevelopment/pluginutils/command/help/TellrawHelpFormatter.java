package org.bsdevelopment.pluginutils.command.help;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bsdevelopment.pluginutils.chat.decoration.NamedTextColor;
import org.bsdevelopment.pluginutils.text.Colorize;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * A {@link HelpFormatter} that sends interactive chat components for players.
 *
 * <p>Uses the BungeeCord chat API ({@code net.md_5.bungee.api.chat}) via
 * {@code player.spigot().sendMessage()} — compatible with both Spigot and Paper.
 *
 * <p>Each entry is rendered as a clickable line:
 * <ul>
 *   <li><b>Click</b> — suggests the base command in the player's chat bar</li>
 *   <li><b>Hover</b> — shows the description (or a fallback hint if none is set)</li>
 * </ul>
 *
 * <p>For console senders, a plain-text fallback is used automatically.
 * Header, empty, and invalid-page messages are sent as plain colorized text.
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

    private boolean displayDescription = false;

    /**
     * Builds the suggest-command string from a usage path.
     *
     * <p>Strips argument placeholders so the player's chat bar is pre-filled with
     * the base command and a trailing space ready for input.
     *
     * <p>For example, {@code "admin kick <target> [reason]"} becomes {@code "/admin kick "}.
     *
     * @param usage the full usage string (e.g. {@code "admin kick <target> [reason]"})
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
     * Converts a {@link NamedTextColor} to a BungeeCord {@link ChatColor}.
     *
     * @param color the color to convert
     *
     * @return the equivalent BungeeCord {@link ChatColor}
     */
    private static ChatColor toChat(NamedTextColor color) {
        return ChatColor.of(color.asHexString());
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

    @Override
    public void sendHeader(CommandSender sender, String command, int page, int totalPages) {
        sender.sendMessage(Colorize.translateBungeeHex(headerTemplate
                .replace("{command}", command)
                .replace("{page}", String.valueOf(page))
                .replace("{pages}", String.valueOf(totalPages))));
    }

    @Override
    public void sendEntry(CommandSender sender, HelpEntry entry) {
        if (!(sender instanceof Player player)) {
            // Plain-text fallback for console
            boolean hasDesc = entry.description() != null && !entry.description().isBlank();
            sender.sendMessage("/" + entry.usage() + (hasDesc ? " - " + entry.description() : ""));
            return;
        }

        boolean hasDesc = entry.description() != null && !entry.description().isBlank();
        String suggestCmd = buildSuggestCommand(entry.usage());

        // Hover tooltip
        BaseComponent[] hoverText = new ComponentBuilder(
                hasDesc ? entry.description() : "Click to suggest command")
                .color(toChat(descriptionColor))
                .create();

        // Clickable usage part
        TextComponent usagePart = new TextComponent("/" + entry.usage());
        usagePart.setColor(toChat(usageColor));
        usagePart.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggestCmd));
        usagePart.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText));

        if (hasDesc) {
            TextComponent separator = new TextComponent(" - ");
            separator.setColor(toChat(separatorColor));

            TextComponent description = new TextComponent(entry.description());
            description.setColor(toChat(descriptionColor));

            if (displayDescription) {
                player.spigot().sendMessage(usagePart, separator, description);
            } else {
                player.spigot().sendMessage(usagePart);
            }
        } else {
            player.spigot().sendMessage(usagePart);
        }
    }

    @Override
    public void sendEmpty(CommandSender sender) {
        sender.sendMessage(Colorize.translateBungeeHex(emptyTemplate));
    }

    @Override
    public void sendInvalidPage(CommandSender sender, int totalPages) {
        sender.sendMessage(Colorize.translateBungeeHex(
                invalidPageTemplate.replace("{pages}", String.valueOf(totalPages))));
    }
}

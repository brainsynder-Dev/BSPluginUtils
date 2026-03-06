package org.bsdevelopment.pluginutils.command.help;

import org.bsdevelopment.pluginutils.text.Colorize;
import org.bukkit.command.CommandSender;

/**
 * A {@link HelpFormatter} that sends colorized plain-text messages.
 *
 * <p>All five templates support {@code &} color codes and hex colors
 * (processed by {@link Colorize#translateBungeeHex}).
 *
 * <p>Placeholder tokens:
 * <ul>
 *   <li>{@code {command}} — parent command name (header only)</li>
 *   <li>{@code {page}} — current page number (header only)</li>
 *   <li>{@code {pages}} — total pages (header and invalid-page)</li>
 *   <li>{@code {usage}} — full usage path (entry only)</li>
 *   <li>{@code {description}} — command description (entry only)</li>
 * </ul>
 *
 * <p>Example:
 * <pre>{@code
 * HelpCommand.of(admin)
 *     .withFormatter(HelpCommand.plainFormatter()
 *         .header("&b--- {command} Help ({page}/{pages}) ---")
 *         .entry("  &f/{usage} &8» &7{description}"))
 *     .build();
 * }</pre>
 */
public class PlainTextHelpFormatter implements HelpFormatter {
    private String headerTemplate = "&6=== Help: /{command} (Page {page}/{pages}) ===";
    private String entryTemplate = "&e/{usage} &7- {description}";
    private String entryNoDescTemplate = "&e/{usage}";
    private String emptyTemplate = "&cNo commands available.";
    private String invalidPageTemplate = "&cInvalid page. There are {pages} page(s) available.";

    /**
     * Sets the header template shown once before any entries.
     *
     * <p>Supports the placeholders {@code {command}}, {@code {page}}, and {@code {pages}}.
     *
     * @param template the new header template string
     * @return this formatter, for chaining
     */
    public PlainTextHelpFormatter header(String template) {
        this.headerTemplate = template;
        return this;
    }

    /**
     * Sets the entry template used when a description is present.
     *
     * <p>Supports the placeholders {@code {usage}} and {@code {description}}.
     *
     * @param template the new entry template string
     * @return this formatter, for chaining
     */
    public PlainTextHelpFormatter entry(String template) {
        this.entryTemplate = template;
        return this;
    }

    /**
     * Sets the entry template used when no description is set on the subcommand.
     *
     * <p>Supports the placeholder {@code {usage}}.
     *
     * @param template the new no-description entry template string
     * @return this formatter, for chaining
     */
    public PlainTextHelpFormatter entryNoDesc(String template) {
        this.entryNoDescTemplate = template;
        return this;
    }

    /**
     * Sets the message shown when no accessible commands are found.
     *
     * @param template the new empty-list message template
     * @return this formatter, for chaining
     */
    public PlainTextHelpFormatter empty(String template) {
        this.emptyTemplate = template;
        return this;
    }

    /**
     * Sets the message shown when the requested page does not exist.
     *
     * <p>Supports the placeholder {@code {pages}}.
     *
     * @param template the new invalid-page message template
     * @return this formatter, for chaining
     */
    public PlainTextHelpFormatter invalidPage(String template) {
        this.invalidPageTemplate = template;
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
        boolean hasDesc = entry.description() != null && !entry.description().isBlank();
        String template = hasDesc ? entryTemplate : entryNoDescTemplate;
        String msg = template
                .replace("{usage}", entry.usage())
                .replace("{description}", hasDesc ? entry.description() : "");
        sender.sendMessage(Colorize.translateBungeeHex(msg));
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

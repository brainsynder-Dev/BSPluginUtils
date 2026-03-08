package org.bsdevelopment.pluginutils.chat;

import net.md_5.bungee.chat.ComponentSerializer;
import org.bsdevelopment.pluginutils.chat.components.MutableComponent;
import org.bsdevelopment.pluginutils.chat.components.TextComponent;
import org.bsdevelopment.pluginutils.chat.decoration.NamedTextColor;
import org.bsdevelopment.pluginutils.chat.decoration.TextDecoration;
import org.bsdevelopment.pluginutils.chat.events.ClickEvent;
import org.bsdevelopment.pluginutils.chat.events.HoverEvent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A fluent builder for interactive Minecraft chat messages backed by the project's
 * {@link Component} API.
 *
 * <p>Each call to {@link #then(String)} starts a new styled segment. Style methods
 * ({@link #color}, {@link #bold}, {@link #link}, {@link #tooltip}, …) always apply to
 * the most recently added segment.
 *
 * <p>Messages are serialized to Minecraft-compatible JSON via
 * {@link ComponentJsonSerializer} and delivered to players via
 * {@code player.spigot().sendMessage()} — compatible with both Spigot and Paper.
 * Console senders receive a plain-text fallback.
 *
 * <p>Example:
 * <pre>{@code
 * TellrawMessage.of("&6Welcome, ")
 *     .then(player.getName()).color(NamedTextColor.YELLOW).bold()
 *     .then("! Click here").color(NamedTextColor.AQUA)
 *         .command("/help")
 *         .tooltip("&7Opens the help menu")
 *     .send(player);
 * }</pre>
 */
public class TellrawMessage {
    private final List<Part> parts = new ArrayList<>();
    private boolean dirty = true;
    private String cachedJson = null;

    /**
     * Creates an empty {@code TellrawMessage}. Add content with {@link #then(String)}.
     *
     * @return a new, empty message
     */
    public static TellrawMessage empty() {
        return new TellrawMessage();
    }

    /**
     * Creates a {@code TellrawMessage} by parsing a legacy color-code string.
     *
     * <p>Supports:
     * <ul>
     *   <li>{@code &0}–{@code &f} — color codes</li>
     *   <li>{@code &l}, {@code &o}, {@code &n}, {@code &m}, {@code &k} — formatting</li>
     *   <li>{@code &r} — reset</li>
     *   <li>{@code &#RRGGBB} — hex colors</li>
     *   <li>{@code §} as an alternate color prefix</li>
     * </ul>
     *
     * @param text the legacy-formatted string to parse
     * @return a new message whose parts reflect the color-coded input
     */
    public static TellrawMessage of(String text) {
        TellrawMessage msg = new TellrawMessage();
        msg.parts.addAll(parseLegacy(text));
        return msg;
    }

    /**
     * Creates a {@code TellrawMessage} from a pre-built {@link Component}.
     *
     * @param component the component to wrap
     * @return a new message containing the given component
     */
    public static TellrawMessage of(Component component) {
        TellrawMessage msg = new TellrawMessage();
        msg.parts.add(new Part(component));
        return msg;
    }

    /**
     * Appends a new text segment. All subsequent style calls apply to this segment.
     *
     * @param text the text to append
     * @return this message, for chaining
     */
    public TellrawMessage then(String text) {
        parts.add(new Part(text));
        return markDirty();
    }

    /**
     * Appends a pre-built {@link Component} as the next segment.
     *
     * @param component the component to append
     * @return this message, for chaining
     */
    public TellrawMessage then(Component component) {
        parts.add(new Part(component));
        return markDirty();
    }

    /**
     * Removes the last added segment.
     *
     * @return this message, for chaining
     */
    public TellrawMessage removeLastPart() {
        if (!parts.isEmpty()) {
            parts.remove(parts.size() - 1);
            dirty = true;
        }
        return this;
    }

    /**
     * Sets the color of the latest segment.
     *
     * @param color the color to apply
     * @return this message, for chaining
     */
    public TellrawMessage color(NamedTextColor color) {
        latest().style.color(color);
        return markDirty();
    }

    /**
     * Sets the color of the latest segment using a hex string.
     *
     * @param hex the hex color string (e.g. {@code "#FF0000"})
     * @return this message, for chaining
     */
    public TellrawMessage color(String hex) {
        return color(NamedTextColor.ofHex(hex));
    }

    /**
     * Applies bold formatting to the latest segment.
     *
     * @return this message, for chaining
     */
    public TellrawMessage bold() { return decorate(TextDecoration.BOLD); }

    /**
     * Applies italic formatting to the latest segment.
     *
     * @return this message, for chaining
     */
    public TellrawMessage italic() { return decorate(TextDecoration.ITALIC); }

    /**
     * Applies underline formatting to the latest segment.
     *
     * @return this message, for chaining
     */
    public TellrawMessage underline() { return decorate(TextDecoration.UNDERLINED); }

    /**
     * Applies strikethrough formatting to the latest segment.
     *
     * @return this message, for chaining
     */
    public TellrawMessage strikethrough() { return decorate(TextDecoration.STRIKETHROUGH); }

    /**
     * Applies obfuscated (random characters) formatting to the latest segment.
     *
     * @return this message, for chaining
     */
    public TellrawMessage obfuscate() { return decorate(TextDecoration.OBFUSCATED); }

    private TellrawMessage decorate(TextDecoration decoration) {
        latest().style.decorate(decoration);
        return markDirty();
    }

    /**
     * Sets the shift-click insertion text for the latest segment.
     *
     * @param text the text to insert into the chat bar on shift-click
     * @return this message, for chaining
     */
    public TellrawMessage insertion(String text) {
        latest().style.insertion(text);
        return markDirty();
    }

    /**
     * Opens a URL in the player's browser when the latest segment is clicked.
     *
     * @param url the URL to open
     * @return this message, for chaining
     */
    public TellrawMessage link(String url) {
        latest().style.clickEvent(new ClickEvent(ClickEvent.ClickAction.OPEN_URL, url));
        return markDirty();
    }

    /**
     * Suggests a command in the player's chat bar when the latest segment is clicked.
     *
     * @param command the command to suggest (including the leading {@code /})
     * @return this message, for chaining
     */
    public TellrawMessage suggest(String command) {
        latest().style.clickEvent(new ClickEvent(ClickEvent.ClickAction.SUGGEST_COMMAND, command));
        return markDirty();
    }

    /**
     * Runs a command when the latest segment is clicked.
     *
     * @param command the command to run (including the leading {@code /})
     * @return this message, for chaining
     */
    public TellrawMessage command(String command) {
        latest().style.clickEvent(new ClickEvent(ClickEvent.ClickAction.RUN_COMMAND, command));
        return markDirty();
    }

    /**
     * Copies text to the clipboard when the latest segment is clicked.
     *
     * @param text the text to copy
     * @return this message, for chaining
     */
    public TellrawMessage copyToClipboard(String text) {
        latest().style.clickEvent(new ClickEvent(ClickEvent.ClickAction.COPY_TO_CLIPBOARD, text));
        return markDirty();
    }

    /**
     * Shows a pre-built {@link Component} as a hover tooltip for the latest segment.
     *
     * @param hoverComponent the component to display on hover
     * @return this message, for chaining
     */
    public TellrawMessage tooltip(Component hoverComponent) {
        latest().style.hoverEvent(new HoverEvent(HoverEvent.HoverAction.SHOW_TEXT, hoverComponent));
        return markDirty();
    }

    /**
     * Shows one or more lines as a hover tooltip for the latest segment.
     *
     * <p>Each line supports legacy color codes ({@code &c}, {@code &#RRGGBB}, etc.).
     * Multiple lines are separated by newlines in the tooltip.
     *
     * @param lines the tooltip lines
     * @return this message, for chaining
     */
    public TellrawMessage tooltip(String... lines) {
        Component hoverComponent;
        if (lines.length == 1) {
            hoverComponent = fromLegacy(lines[0]);
        } else {
            MutableComponent root = new MutableComponent();
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) root.addChild(new TextComponent("\n", Style.empty(), List.of()));
                root.addChild(fromLegacy(lines[i]));
            }
            hoverComponent = root;
        }
        latest().style.hoverEvent(new HoverEvent(HoverEvent.HoverAction.SHOW_TEXT, hoverComponent));
        return markDirty();
    }

    /**
     * Shows one or more lines as a hover tooltip for the latest segment.
     *
     * @param lines the tooltip lines (each supports legacy color codes)
     * @return this message, for chaining
     * @see #tooltip(String...)
     */
    public TellrawMessage tooltip(List<String> lines) {
        return tooltip(lines.toArray(String[]::new));
    }

    /**
     * Shows an item as a hover tooltip for the latest segment.
     *
     * @param itemJson the item's NBT JSON (as produced by Minecraft's item serializer)
     * @return this message, for chaining
     */
    public TellrawMessage itemTooltip(String itemJson) {
        latest().style.hoverEvent(new HoverEvent(
                HoverEvent.HoverAction.SHOW_ITEM,
                ComponentJsonSerializer.fromJson(itemJson)
        ));
        return markDirty();
    }

    /**
     * Serializes this message to a Minecraft-compatible JSON string.
     *
     * <p>The result is cached; subsequent calls return the cached value unless
     * the message has been modified since the last call.
     *
     * @return the JSON string representation of this message
     */
    public String toJSONString() {
        if (!dirty && cachedJson != null) return cachedJson;

        Component root;
        if (parts.size() == 1) {
            root = parts.get(0).toComponent();
        } else {
            MutableComponent composite = new MutableComponent();
            for (Part part : parts) composite.addChild(part.toComponent());
            root = composite;
        }

        cachedJson = ComponentJsonSerializer.toJson(root);
        dirty = false;
        return cachedJson;
    }

    /**
     * Sends this message to a {@link CommandSender}.
     *
     * <p>Players receive the full interactive component message. Console senders
     * receive a plain-text fallback (color information is not included).
     *
     * @param sender the recipient
     */
    public void send(CommandSender sender) {
        if (sender instanceof Player player) {
            send(player);
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (Part part : parts) sb.append(part.plainText());
        sender.sendMessage(sb.toString());
    }

    /**
     * Sends this message to a {@link Player} as interactive chat components.
     *
     * @param player the recipient
     */
    public void send(Player player) {
        player.spigot().sendMessage(ComponentSerializer.parse(toJSONString()));
    }

    private Part latest() {
        if (parts.isEmpty()) throw new IllegalStateException("No parts added yet; call then() first.");
        return parts.get(parts.size() - 1);
    }

    private TellrawMessage markDirty() {
        this.dirty = true;
        return this;
    }

    private static Component fromLegacy(String text) {
        List<Part> parsed = parseLegacy(text);
        if (parsed.isEmpty()) return new TextComponent("", Style.empty(), List.of());
        if (parsed.size() == 1) return parsed.get(0).toComponent();
        MutableComponent root = new MutableComponent();
        for (Part p : parsed) root.addChild(p.toComponent());
        return root;
    }

    private static List<Part> parseLegacy(String text) {
        List<Part> result = new ArrayList<>();
        if (text == null || text.isEmpty()) return result;

        text = text.replace(ChatColor.COLOR_CHAR, '&'); // normalize § to &

        NamedTextColor currentColor = null;
        Set<TextDecoration> currentDecorations = new LinkedHashSet<>();
        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c != '&' || i + 1 >= text.length()) {
                buffer.append(c);
                continue;
            }

            char next = text.charAt(i + 1);

            // Hex color: &#RRGGBB — requires 7 additional chars (# + 6 hex digits)
            if (next == '#' && i + 8 <= text.length()) {
                String maybeHex = text.substring(i + 1, i + 8);
                if (maybeHex.matches("#[0-9a-fA-F]{6}")) {
                    flushBuffer(buffer, currentColor, currentDecorations, result);
                    currentColor = NamedTextColor.ofHex(maybeHex);
                    currentDecorations = new LinkedHashSet<>();
                    i += 7; // skip &#RRGGBB (7 chars after the &)
                    continue;
                }
            }

            char code = Character.toLowerCase(next);
            NamedTextColor color = colorFromCode(code);
            TextDecoration decoration = decorationFromCode(code);

            if (color != null) {
                flushBuffer(buffer, currentColor, currentDecorations, result);
                currentColor = color;
                currentDecorations = new LinkedHashSet<>();
                i++;
            } else if (decoration != null) {
                flushBuffer(buffer, currentColor, currentDecorations, result);
                currentDecorations.add(decoration);
                i++;
            } else if (code == 'r') {
                flushBuffer(buffer, currentColor, currentDecorations, result);
                currentColor = null;
                currentDecorations = new LinkedHashSet<>();
                i++;
            } else {
                buffer.append('&'); // not a recognized code — treat as literal
            }
        }

        flushBuffer(buffer, currentColor, currentDecorations, result);
        return result;
    }

    private static void flushBuffer(StringBuilder buffer, NamedTextColor color,
                                    Set<TextDecoration> decorations, List<Part> result) {
        if (buffer.isEmpty()) return;
        Part part = new Part(buffer.toString());
        if (color != null) part.style.color(color);
        for (TextDecoration dec : decorations) part.style.decorate(dec);
        result.add(part);
        buffer.setLength(0);
    }

    private static NamedTextColor colorFromCode(char code) {
        return switch (code) {
            case '0' -> NamedTextColor.BLACK;
            case '1' -> NamedTextColor.DARK_BLUE;
            case '2' -> NamedTextColor.DARK_GREEN;
            case '3' -> NamedTextColor.DARK_AQUA;
            case '4' -> NamedTextColor.DARK_RED;
            case '5' -> NamedTextColor.DARK_PURPLE;
            case '6' -> NamedTextColor.GOLD;
            case '7' -> NamedTextColor.GRAY;
            case '8' -> NamedTextColor.DARK_GRAY;
            case '9' -> NamedTextColor.BLUE;
            case 'a' -> NamedTextColor.GREEN;
            case 'b' -> NamedTextColor.AQUA;
            case 'c' -> NamedTextColor.RED;
            case 'd' -> NamedTextColor.LIGHT_PURPLE;
            case 'e' -> NamedTextColor.YELLOW;
            case 'f' -> NamedTextColor.WHITE;
            default  -> null;
        };
    }

    private static TextDecoration decorationFromCode(char code) {
        return switch (code) {
            case 'l' -> TextDecoration.BOLD;
            case 'o' -> TextDecoration.ITALIC;
            case 'n' -> TextDecoration.UNDERLINED;
            case 'm' -> TextDecoration.STRIKETHROUGH;
            case 'k' -> TextDecoration.OBFUSCATED;
            default  -> null;
        };
    }

    private static final class Part {
        private final String text;
        private final Component prebuilt;
        final Style.Builder style = new Style.Builder();

        Part(String text) {
            this.text = text;
            this.prebuilt = null;
        }

        Part(Component component) {
            this.text = null;
            this.prebuilt = component;
        }

        Component toComponent() {
            if (prebuilt != null) return prebuilt;
            return new TextComponent(text, style.build(), List.of());
        }

        String plainText() {
            return text != null ? text : "";
        }
    }
}

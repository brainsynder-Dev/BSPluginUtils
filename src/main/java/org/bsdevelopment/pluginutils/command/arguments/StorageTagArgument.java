package org.bsdevelopment.pluginutils.command.arguments;

import org.bsdevelopment.nbt.JsonToNBT;
import org.bsdevelopment.nbt.StorageTagCompound;
import org.bsdevelopment.pluginutils.command.arguments.suggestions.SuggestionInfo;
import org.bsdevelopment.pluginutils.command.exception.ArgumentParseException;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * An {@link Argument} that parses an SNBT (Stringified NBT) compound string into a
 * {@link StorageTagCompound}.
 *
 * <p>Because NBT strings frequently contain spaces (e.g. inside display names or lore entries),
 * this argument implements {@link GreedyArgument} — it consumes all remaining command tokens
 * as a single input and must therefore be placed <b>last</b> in the argument list.
 *
 * <p>An optional template {@link StorageTagCompound} can be supplied to drive tab-completion.
 * When present, the argument suggests:
 * <ul>
 *   <li>The full template compound as a starting point</li>
 *   <li>Individual key completions as the player types inside {@code {}}</li>
 *   <li>Value completions once a key name has been typed followed by {@code :}</li>
 * </ul>
 *
 * <p>Parsing delegates to {@code JsonToNBT.getTagFromJson(String)}, which accepts standard
 * Minecraft SNBT syntax. For example:
 * <pre>{@code
 * {display:{Name:'{"text":"My Pet"}'},custom:1b}
 * }</pre>
 *
 * <p>Example usage with a template:
 * <pre>{@code
 * StorageTagCompound template = new StorageTagCompound()
 *     .setString("type", "cat")
 *     .setBoolean("flying", false);
 *
 * CommandBuilder.create("spawnpet")
 *     .withArguments(new StorageTagArgument("nbt", template))
 *     .executesPlayer((player, args) -> {
 *         StorageTagCompound tag = args.get("nbt");
 *         String type = tag.getString("type");
 *     });
 * }</pre>
 */
public class StorageTagArgument extends Argument<StorageTagCompound> implements GreedyArgument {

    private StorageTagCompound template;

    /**
     * Creates a new {@code StorageTagArgument} with no tab-completion template.
     * Suggestions will only offer {@code {}}.
     *
     * @param nodeName the argument name used to retrieve the parsed value from
     *                 {@link org.bsdevelopment.pluginutils.command.CommandArguments}
     */
    public StorageTagArgument(String nodeName) {
        super(nodeName);
    }

    /**
     * Creates a new {@code StorageTagArgument} with a tab-completion template.
     *
     * <p>The template's keys and values are used to drive intelligent suggestions as the
     * player types their NBT compound.
     *
     * @param nodeName the argument name used to retrieve the parsed value from
     *                 {@link org.bsdevelopment.pluginutils.command.CommandArguments}
     * @param template a {@link StorageTagCompound} whose keys and values are offered as suggestions
     */
    public StorageTagArgument(String nodeName, StorageTagCompound template) {
        super(nodeName);
        this.template = template;
    }

    /**
     * Returns the index of the last top-level separator ({@code \{} or {@code ,}) in the
     * partial SNBT input, correctly skipping over quoted strings and nested structures.
     *
     * <p>"Top-level" means depth 1 — directly inside the outermost compound, not nested
     * inside a sub-compound, list, or quoted string.
     *
     * @param input the partial SNBT string being typed
     *
     * @return the index of the last top-level separator, or {@code 0} if none is found
     */
    private static int findLastTopLevelSep(String input) {
        int depth = 0;
        boolean inString = false;
        char stringChar = 0;
        int lastSep = 0;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (inString) {
                if (c == '\\') {
                    i++;
                    continue;
                } // skip escaped character
                if (c == stringChar) inString = false;
                continue;
            }

            if (c == '"' || c == '\'') {
                inString = true;
                stringChar = c;
                continue;
            }

            if (c == '{' || c == '[') {
                if (c == '{' && depth == 0) lastSep = i; // opening brace of root compound
                depth++;
            } else if (c == '}' || c == ']') {
                depth--;
            } else if (c == ',' && depth == 1) {
                lastSep = i; // top-level separator between key-value pairs
            }
        }

        return lastSep;
    }

    /**
     * Extracts the set of top-level keys that are already present in the partial SNBT compound.
     *
     * <p>Only keys at depth 1 (directly inside the outermost compound) are collected.
     * Nested keys inside sub-compounds are ignored.
     *
     * @param input the partial SNBT string being typed
     *
     * @return an ordered set of key names already in the compound
     */
    private static Set<String> extractUsedKeys(String input) {
        Set<String> keys = new LinkedHashSet<>();
        int depth = 0;
        boolean inString = false;
        char stringChar = 0;
        int keyStart = -1;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (inString) {
                if (c == '\\') {
                    i++;
                    continue;
                }
                if (c == stringChar) inString = false;
                continue;
            }

            if (c == '"' || c == '\'') {
                inString = true;
                stringChar = c;
                continue;
            }

            if (c == '{' || c == '[') {
                if (c == '{' && depth == 0) keyStart = i + 1; // start reading the first key
                depth++;
            } else if (c == '}' || c == ']') {
                depth--;
            } else if (c == ':' && depth == 1) {
                if (keyStart >= 0) {
                    String key = input.substring(keyStart, i).trim();
                    if (!key.isEmpty()) keys.add(key);
                    keyStart = -1;
                }
            } else if (c == ',' && depth == 1) {
                keyStart = i + 1; // start reading the next key after the comma
            }
        }

        return keys;
    }

    /**
     * Sets or replaces the tab-completion template.
     *
     * @param template a {@link StorageTagCompound} whose keys and values are offered as suggestions
     *
     * @return this argument, for chaining
     */
    public StorageTagArgument withTemplate(StorageTagCompound template) {
        this.template = template;
        return this;
    }

    /**
     * Parses the given SNBT string into a {@link StorageTagCompound}.
     *
     * @param sender the command sender (unused, but required by the argument contract)
     * @param input  the raw SNBT string, e.g. {@code {key:value,foo:1b}}
     *
     * @return the parsed {@link StorageTagCompound}
     *
     * @throws ArgumentParseException if the input is not valid SNBT
     */
    @Override
    public StorageTagCompound parse(CommandSender sender, String input) throws ArgumentParseException {
        try {
            return JsonToNBT.getTagFromJson(input);
        } catch (Exception e) {
            throw ArgumentParseException.fromString("Invalid NBT: " + e.getMessage());
        }
    }

    /**
     * Returns tab-completion suggestions based on the current partial input.
     *
     * <p>Without a template only {@code {}} is suggested. With a template the suggestions
     * are driven by the template's keys and values:
     * <ul>
     *   <li>The full template compound is always suggested.</li>
     *   <li>While typing a key name, every matching template key is suggested with its value.</li>
     *   <li>While typing a value (after {@code :}), the template's value for that key is suggested.</li>
     *   <li>Keys already present in the partial input are excluded from further suggestions.</li>
     * </ul>
     *
     * @param info the suggestion context; {@code currentInput} is the partial token being typed
     *
     * @return a list of candidate completions
     */
    @Override
    public Collection<String> suggest(SuggestionInfo info) {
        if (template == null) return List.of("{}");

        String current = info.currentInput();
        String fullTemplate = template.toString();
        List<String> result = new ArrayList<>();

        result.add("{}");
        if (!fullTemplate.equals("{}")) result.add(fullTemplate);

        if (!current.startsWith("{")) return result;

        int lastSep = findLastTopLevelSep(current);
        String existingPart = current.substring(0, lastSep + 1);
        String typingPart = current.substring(lastSep + 1);
        Set<String> usedKeys = extractUsedKeys(current);

        if (typingPart.contains(":")) {
            // The player is mid-value — suggest completing the value from the template
            int colon = typingPart.indexOf(':');
            String typedKey = typingPart.substring(0, colon);
            if (template.hasKey(typedKey)) {
                result.add(existingPart + typedKey + ":" + template.getValue(typedKey) + "}");
            }
        } else {
            // The player is mid-key — suggest every matching template key with its value
            for (String key : template.getKeySet()) {
                if (usedKeys.contains(key)) continue;
                if (key.toLowerCase().startsWith(typingPart.toLowerCase())) {
                    result.add(existingPart + key + ":" + template.getValue(key) + "}");
                }
            }
        }

        return result;
    }
}

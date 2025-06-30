package org.bsdevelopment.pluginutils.chat;

import org.bsdevelopment.pluginutils.chat.components.CompositeComponent;
import org.bsdevelopment.pluginutils.chat.components.KeybindComponent;
import org.bsdevelopment.pluginutils.chat.components.ScoreComponent;
import org.bsdevelopment.pluginutils.chat.components.SelectorComponent;
import org.bsdevelopment.pluginutils.chat.components.StyledComponent;
import org.bsdevelopment.pluginutils.chat.components.TextComponent;
import org.bsdevelopment.pluginutils.chat.components.TranslatableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Base interface for a rich text component.
 */
public interface Component {
    /**
     * The child components of this component.
     *
     * @return an unmodifiable list of children
     */
    List<Component> children();

    /**
     * The styling (color, decorations, events, insertion) of this component.
     *
     * @return the component’s style
     */
    Style style();

    /**
     * Returns a new component whose children are this component’s children plus the given one.
     *
     * @param component
     *         to append
     *
     * @return a composite component
     */
    default Component append(Component component) {
        Objects.requireNonNull(component, "component");
        List<Component> merged = new ArrayList<>(children());
        merged.add(component);
        return new CompositeComponent(merged, style());
    }

    /**
     * Returns a copy of this component with the given style applied.
     *
     * @param newStyle
     *         the style to apply
     *
     * @return a styled component
     */
    default Component style(Style newStyle) {
        Objects.requireNonNull(newStyle, "newStyle");
        return new StyledComponent(this, newStyle);
    }

    // ----- Static factories (no utility class needed) -----

    /**
     * Creates a plain text component.
     *
     * @param content
     *         the text
     *
     * @return a TextComponent
     */
    static TextComponent text(String content) {
        return new TextComponent(Objects.requireNonNull(content, "content"), Style.empty(), List.of());
    }

    /**
     * Creates a translatable component.
     *
     * @param key
     *         the translation key
     *
     * @return a TranslatableComponent
     */
    static TranslatableComponent translatable(String key) {
        return new TranslatableComponent(Objects.requireNonNull(key, "key"), List.of(), Style.empty());
    }

    /**
     * Creates a keybind component.
     *
     * @param keybind
     *         the client‐side keybind identifier
     *
     * @return a KeybindComponent
     */
    static KeybindComponent keybind(String keybind) {
        return new KeybindComponent(Objects.requireNonNull(keybind, "keybind"), Style.empty());
    }

    /**
     * Creates a score component.
     *
     * @param name
     *         the entity or name
     * @param objective
     *         the scoreboard objective
     *
     * @return a ScoreComponent
     */
    static ScoreComponent score(String name, String objective) {
        return new ScoreComponent(
                Objects.requireNonNull(name, "name"),
                Objects.requireNonNull(objective, "objective"),
                Style.empty()
        );
    }

    /**
     * Creates a selector component.
     *
     * @param selector
     *         a Minecraft‐style selector string
     *
     * @return a SelectorComponent
     */
    static SelectorComponent selector(String selector) {
        return new SelectorComponent(Objects.requireNonNull(selector, "selector"), Style.empty());
    }


    /**
     * Serialize this component to a JSON string in the same format
     * that Minecraft’s built-in `Component.Serializer.fromJson` expects.
     *
     * @return JSON representation
     */
    default String toJson() {
        return ComponentJsonSerializer.toJson(this);
    }

    /**
     * Deserialize a JSON string (in Minecraft component format) back
     * into your own Component model.
     *
     * @param json
     *         a valid component JSON
     *
     * @return the parsed Component
     */
    static Component fromJson(String json) {
        Objects.requireNonNull(json, "json");
        return ComponentJsonSerializer.fromJson(json);
    }
}

package org.bsdevelopment.pluginutils.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.bsdevelopment.pluginutils.chat.components.CompositeComponent;
import org.bsdevelopment.pluginutils.chat.components.KeybindComponent;
import org.bsdevelopment.pluginutils.chat.components.ScoreComponent;
import org.bsdevelopment.pluginutils.chat.components.SelectorComponent;
import org.bsdevelopment.pluginutils.chat.components.TextComponent;
import org.bsdevelopment.pluginutils.chat.components.TranslatableComponent;
import org.bsdevelopment.pluginutils.chat.decoration.NamedTextColor;
import org.bsdevelopment.pluginutils.chat.decoration.TextDecoration;
import org.bsdevelopment.pluginutils.chat.events.ClickEvent;
import org.bsdevelopment.pluginutils.chat.events.HoverEvent;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * (De)serializes {@link Component} instances to/from JSON compatible with
 * Minecraft's `net.minecraft.network.chat.Component` JSON format.
 */
public final class ComponentJsonSerializer {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Component.class, new ComponentAdapter())
            .registerTypeAdapter(Style.class, new StyleAdapter())
            .create();

    private ComponentJsonSerializer() { /* no instantiation */ }

    /** Serialize a Component to its Minecraft-compatible JSON. */
    public static String toJson(Component component) {
        return GSON.toJson(component, Component.class);
    }

    /** Deserialize a Minecraft-compatible JSON string into a Component. */
    public static Component fromJson(String json) {
        return GSON.fromJson(json, Component.class);
    }

    // ─── Adapter for Component ─────────────────────────────────────────────────

    private static class ComponentAdapter
            implements JsonSerializer<Component>, JsonDeserializer<Component> {

        @Override
        public JsonElement serialize(
                Component component,
                Type typeOfComponent,
                JsonSerializationContext context
        ) {
            JsonObject componentJson = new JsonObject();

            // 1️⃣ Type-specific content
            if (component instanceof TextComponent textComponent) {
                componentJson.addProperty("text", textComponent.content());
            } else if (component instanceof TranslatableComponent translatableComponent) {
                componentJson.addProperty("translate", translatableComponent.key());
                if (!translatableComponent.args().isEmpty()) {
                    JsonArray withArray = new JsonArray();
                    for (Component argument : translatableComponent.args()) {
                        withArray.add(context.serialize(argument, Component.class));
                    }
                    componentJson.add("with", withArray);
                }
            } else if (component instanceof KeybindComponent keybindComponent) {
                componentJson.addProperty("keybind", keybindComponent.keybind());
            } else if (component instanceof ScoreComponent scoreComponent) {
                JsonObject scoreJson = new JsonObject();
                scoreJson.addProperty("name", scoreComponent.name());
                scoreJson.addProperty("objective", scoreComponent.objective());
                componentJson.add("score", scoreJson);
            } else if (component instanceof SelectorComponent selectorComponent) {
                componentJson.addProperty("selector", selectorComponent.selector());
            }

            // 2️⃣ Style
            Style componentStyle = component.style();
            if (!componentStyle.equals(Style.empty())) {
                componentJson.add("style", context.serialize(componentStyle, Style.class));
            }

            // 3️⃣ Children (extra)
            if (!component.children().isEmpty()) {
                JsonArray childrenArray = new JsonArray();
                for (Component childComponent : component.children()) {
                    childrenArray.add(context.serialize(childComponent, Component.class));
                }
                componentJson.add("extra", childrenArray);
            }

            return componentJson;
        }

        @Override
        public Component deserialize(
                JsonElement jsonElement,
                Type typeOfComponent,
                JsonDeserializationContext context
        ) throws JsonParseException {
            JsonObject rootJsonObject = jsonElement.getAsJsonObject();

            // Style
            Style deserializedStyle = rootJsonObject.has("style")
                    ? context.deserialize(rootJsonObject.get("style"), Style.class)
                    : Style.empty();

            // Children
            List<Component> childComponents = new ArrayList<>();
            if (rootJsonObject.has("extra")) {
                for (JsonElement childElement : rootJsonObject.getAsJsonArray("extra")) {
                    childComponents.add(context.deserialize(childElement, Component.class));
                }
            }

            // Reconstruct by known keys
            if (rootJsonObject.has("text")) {
                return new TextComponent(
                        rootJsonObject.get("text").getAsString(),
                        deserializedStyle,
                        childComponents
                );
            } else if (rootJsonObject.has("translate")) {
                List<Component> translationArgs = new ArrayList<>();
                if (rootJsonObject.has("with")) {
                    for (JsonElement withElement : rootJsonObject.getAsJsonArray("with")) {
                        translationArgs.add(context.deserialize(withElement, Component.class));
                    }
                }
                return new TranslatableComponent(
                        rootJsonObject.get("translate").getAsString(),
                        translationArgs,
                        deserializedStyle
                );
            } else if (rootJsonObject.has("keybind")) {
                return new KeybindComponent(
                        rootJsonObject.get("keybind").getAsString(),
                        deserializedStyle
                );
            } else if (rootJsonObject.has("score")) {
                JsonObject scoreJson = rootJsonObject.getAsJsonObject("score");
                return new ScoreComponent(
                        scoreJson.get("name").getAsString(),
                        scoreJson.get("objective").getAsString(),
                        deserializedStyle
                );
            } else if (rootJsonObject.has("selector")) {
                return new SelectorComponent(
                        rootJsonObject.get("selector").getAsString(),
                        deserializedStyle
                );
            }

            // Fallback to a generic composite
            return new CompositeComponent(childComponents, deserializedStyle);
        }
    }

    // ─── Adapter for Style ──────────────────────────────────────────────────────

    private static class StyleAdapter
            implements JsonSerializer<Style>, JsonDeserializer<Style> {

        @Override
        public JsonElement serialize(
                Style style,
                Type typeOfStyle,
                JsonSerializationContext context
        ) {
            JsonObject styleJsonObject = new JsonObject();

            if (style.color() != null) {
                styleJsonObject.addProperty("color", style.color().asHexString());
            }
            for (TextDecoration decoration : style.decorations()) {
                styleJsonObject.addProperty(decoration.name().toLowerCase(), true);
            }
            if (style.clickEvent() != null) {
                JsonObject clickEventJson = new JsonObject();
                clickEventJson.addProperty(
                        "action",
                        style.clickEvent().action().name().toLowerCase()
                );
                clickEventJson.addProperty("value", style.clickEvent().value());
                styleJsonObject.add("clickEvent", clickEventJson);
            }
            if (style.hoverEvent() != null) {
                JsonObject hoverEventJson = new JsonObject();
                hoverEventJson.addProperty(
                        "action",
                        style.hoverEvent().action().name().toLowerCase()
                );
                hoverEventJson.add(
                        "contents",
                        context.serialize(style.hoverEvent().value(), Component.class)
                );
                styleJsonObject.add("hoverEvent", hoverEventJson);
            }
            if (style.insertion() != null) {
                styleJsonObject.addProperty("insertion", style.insertion());
            }

            return styleJsonObject;
        }

        @Override
        public Style deserialize(
                JsonElement jsonElement,
                Type typeOfStyle,
                JsonDeserializationContext context
        ) throws JsonParseException {
            JsonObject styleJsonObject = jsonElement.getAsJsonObject();
            Style.Builder styleBuilder = new Style.Builder();

            if (styleJsonObject.has("color")) {
                styleBuilder.color(
                        NamedTextColor.ofHex(styleJsonObject.get("color").getAsString())
                );
            }
            for (TextDecoration decoration : TextDecoration.values()) {
                String key = decoration.name().toLowerCase();
                if (styleJsonObject.has(key) && styleJsonObject.get(key).getAsBoolean()) {
                    styleBuilder.decorate(decoration);
                }
            }
            if (styleJsonObject.has("clickEvent")) {
                JsonObject clickEventJson = styleJsonObject.getAsJsonObject("clickEvent");
                styleBuilder.clickEvent(new ClickEvent(
                        ClickEvent.ClickAction.valueOf(
                                clickEventJson.get("action").getAsString().toUpperCase()
                        ),
                        clickEventJson.get("value").getAsString()
                ));
            }
            if (styleJsonObject.has("hoverEvent")) {
                JsonObject hoverEventJson = styleJsonObject.getAsJsonObject("hoverEvent");
                Component hoverComponent = context.deserialize(
                        hoverEventJson.get("contents"), Component.class
                );
                styleBuilder.hoverEvent(new HoverEvent(
                        HoverEvent.HoverAction.valueOf(
                                hoverEventJson.get("action").getAsString().toUpperCase()
                        ),
                        hoverComponent
                ));
            }
            if (styleJsonObject.has("insertion")) {
                styleBuilder.insertion(styleJsonObject.get("insertion").getAsString());
            }

            return styleBuilder.build();
        }
    }
}

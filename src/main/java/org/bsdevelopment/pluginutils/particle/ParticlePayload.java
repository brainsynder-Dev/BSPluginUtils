/*
 * Copyright © 2025
 * BSDevelopment <https://bsdevelopment.org>
 */

package org.bsdevelopment.pluginutils.particle;

import org.bsdevelopment.pluginutils.xml.XmlUtils;
import org.bsdevelopment.pluginutils.xml.XmlValidationException;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.lang.reflect.Constructor;

/**
 * Logical particle "data" (DustOptions, BlockData, Vibration, Trail, Color, etc.)
 * in a version-safe way.
 *
 * <p>This is both:
 * <ul>
 *     <li>The runtime description of particle data</li>
 *     <li>The XML-serializable representation (via {@link #toXml(Document)} and {@link #fromXml(Element)})</li>
 * </ul>
 * </p>
 */
public sealed interface ParticlePayload permits
        ParticlePayload.None,
        ParticlePayload.Dust,
        ParticlePayload.DustTransition,
        ParticlePayload.Block,
        ParticlePayload.ColorOnly,
        ParticlePayload.Spell,
        ParticlePayload.VibrationRelative,
        ParticlePayload.TrailRelative {
    /**
     * Reads a payload from a payload child element (e.g. &lt;dust&gt;, &lt;trail&gt;).
     * Returns {@link None} if element is {@code null}.
     *
     * @throws XmlValidationException on invalid XML
     */
    static @NotNull ParticlePayload fromXml(@Nullable Element element) throws XmlValidationException {
        if (element == null) return None.INSTANCE;

        String tag = element.getTagName();

        return switch (tag) {
            case "none" -> None.INSTANCE;

            case "dust" -> {
                Color color = XmlUtils.parseColor(XmlUtils.requireAttr(element, "color", "Color must be #RRGGBB or R,G,B (e.g. #FF8800 or 255,136,0)."), element,
                        "Use #RRGGBB or R,G,B for dust color (e.g. #FF8800 or 255,136,0).");
                float size = (float) XmlUtils.parseDoubleAttr(element, "size", 1.0, "Dust size must be a number like 1.0.");

                yield new Dust(color, size);
            }

            case "dust-transition", "dustTransition" -> {
                Color from = XmlUtils.parseColor(XmlUtils.requireAttr(element, "from", "Use #RRGGBB or R,G,B for 'from' color."), element,
                        "Use #RRGGBB or R,G,B for 'from' color.");
                Color to = XmlUtils.parseColor(XmlUtils.requireAttr(element, "to", "Use #RRGGBB or R,G,B for 'to' color."), element,
                        "Use #RRGGBB or R,G,B for 'to' color.");
                float size = (float) XmlUtils.parseDoubleAttr(element, "size", 1.0, "Dust transition size must be a number like 1.0.");

                yield new DustTransition(from, to, size);
            }

            case "block", "block-data", "blockData" -> {
                String dataString = XmlUtils.requireAttr(element, "data", "Provide a valid block data string, e.g. 'minecraft:stone' or 'minecraft:oak_log[axis=y]'.");
                BlockData blockData;

                try {
                    blockData = Bukkit.createBlockData(dataString);
                } catch (IllegalArgumentException ex) {
                    throw new XmlValidationException(element, "Invalid block data '" + dataString + "': " + ex.getMessage(),
                            "Check that the block name and properties are valid for this Minecraft version.");
                }

                yield new Block(blockData);
            }

            case "color" -> {
                String value = XmlUtils.optionalAttr(element, "value", null);
                if (value == null || value.isEmpty()) value = XmlUtils.optionalAttr(element, "color", null);

                if (value == null || value.isEmpty()) {
                    throw new XmlValidationException(element, "Missing 'color' attribute for <color> payload.",
                            "Add color=\"#RRGGBB\" or color=\"R,G,B\" (e.g. color=\"#33CCFF\").");
                }

                Color color = XmlUtils.parseColor(value, element, "Use #RRGGBB or R,G,B for color (e.g. #33CCFF or 51,204,255).");

                yield new ColorOnly(color);
            }

            case "spell" -> {
                Color color = XmlUtils.parseColor(XmlUtils.requireAttr(element, "color", "Spell color must be #RRGGBB or R,G,B (e.g. #55FFAA or 85,255,170)."), element,
                        "Use #RRGGBB or R,G,B for spell color (e.g. #55FFAA or 85,255,170).");
                float power = (float) XmlUtils.parseDoubleAttr(element, "power", 1.0, "Spell power must be a number like 0.25, 1.0, etc.");

                yield new Spell(color, power);
            }

            case "vibration" -> {
                double dx = XmlUtils.parseDoubleAttr(element, "offsetX", 0.0, "offsetX must be a number, e.g. offsetX=\"0.0\".");
                double dy = XmlUtils.parseDoubleAttr(element, "offsetY", 0.0, "offsetY must be a number, e.g. offsetY=\"1.0\".");
                double dz = XmlUtils.parseDoubleAttr(element, "offsetZ", 0.0, "offsetZ must be a number, e.g. offsetZ=\"0.0\".");
                int arrival = XmlUtils.parseIntAttr(element, "arrival", 20, "arrival must be an integer tick count, e.g. arrival=\"20\".");

                yield new VibrationRelative(dx, dy, dz, arrival);
            }

            case "trail" -> {
                double dx = XmlUtils.parseDoubleAttr(element, "offsetX", 0.0, "offsetX must be a number, e.g. offsetX=\"0.0\".");
                double dy = XmlUtils.parseDoubleAttr(element, "offsetY", 0.0, "offsetY must be a number, e.g. offsetY=\"0.5\".");
                double dz = XmlUtils.parseDoubleAttr(element, "offsetZ", 0.0, "offsetZ must be a number, e.g. offsetZ=\"0.0\".");
                int duration = XmlUtils.parseIntAttr(element, "duration", 40, "duration must be an integer tick count, e.g. duration=\"40\".");
                Color color = XmlUtils.parseColor(XmlUtils.requireAttr(element, "color", "Trail color required (#RRGGBB or R,G,B)."), element,
                        "Trail color must be #RRGGBB or R,G,B (e.g. #33CCFF or 51,204,255).");

                yield new TrailRelative(dx, dy, dz, color, duration);
            }

            default -> throw new XmlValidationException(element, "Unknown particle payload element <" + tag + ">.",
                    "Supported payload tags: <none>, <dust>, <dust-transition>, <block>, <color>, <spell>, <vibration>, <trail>.");
        };
    }

    /**
     * Convert this payload into the underlying Bukkit data object for a particular
     * origin location, or {@code null} if not supported on this server.
     *
     * @param origin origin location (used by relative payloads)
     * @param caps   reflection-based capability info
     */
    @Nullable
    Object toBukkitData(@NotNull Location origin, @NotNull ParticleDataCapabilities caps);

    /**
     * Writes this payload as a child element (e.g. &lt;dust&gt;, &lt;trail&gt;) in the given Document.
     */
    @NotNull
    Element toXml(@NotNull Document doc);

    /**
     * No payload (null).
     */
    final class None implements ParticlePayload {
        static final None INSTANCE = new None();

        @Override
        public @Nullable Object toBukkitData(@NotNull Location origin, @NotNull ParticleDataCapabilities caps) {
            return null;
        }

        @Override
        public @NotNull Element toXml(@NotNull Document doc) {
            return doc.createElement("none");
        }
    }

    /**
     * DustOptions-like payload (color + size).
     * <p>
     * Uses reflection to build Particle.DustOptions when available.
     */
    record Dust(@NotNull Color color, float size) implements ParticlePayload {
        @Override
        public @Nullable Object toBukkitData(@NotNull Location origin, @NotNull ParticleDataCapabilities caps) {
            Class<?> dustClass = caps.dustOptionsClass();
            if (dustClass == null) return null;

            try {
                return dustClass.getConstructor(Color.class, float.class).newInstance(color, size);
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }

        @Override
        public @NotNull Element toXml(@NotNull Document doc) {
            Element element = doc.createElement("dust");
            element.setAttribute("color", XmlUtils.formatColor(color));
            element.setAttribute("size", Float.toString(size));
            return element;
        }
    }

    /**
     * DustTransition payload (fromColor -> toColor).
     * <p>
     * Uses Particle.DustTransition when available; otherwise falls back to DustOptions with the toColor.
     */
    record DustTransition(@NotNull Color fromColor, @NotNull Color toColor, float size) implements ParticlePayload {
        @Override
        public @Nullable Object toBukkitData(@NotNull Location origin, @NotNull ParticleDataCapabilities caps) {
            Class<?> transitionClass = caps.dustTransitionClass();
            if (transitionClass != null) {
                try {
                    return transitionClass.getConstructor(Color.class, Color.class, float.class).newInstance(fromColor, toColor, size);
                } catch (ReflectiveOperationException ignored) {
                }
            }

            if (caps.hasDustOptions()) return new Dust(toColor, size).toBukkitData(origin, caps);
            return null;
        }

        @Override
        public @NotNull Element toXml(@NotNull Document doc) {
            Element element = doc.createElement("dust-transition");
            element.setAttribute("from", XmlUtils.formatColor(fromColor));
            element.setAttribute("to", XmlUtils.formatColor(toColor));
            element.setAttribute("size", Float.toString(size));
            return element;
        }
    }

    /**
     * BlockData-based payload.
     */
    record Block(@NotNull BlockData blockData) implements ParticlePayload {
        @Override
        public @Nullable Object toBukkitData(@NotNull Location origin, @NotNull ParticleDataCapabilities caps) {
            return blockData;
        }

        @Override
        public @NotNull Element toXml(@NotNull Document doc) {
            Element element = doc.createElement("block");
            element.setAttribute("data", blockData.getAsString());
            return element;
        }
    }

    /**
     * Simple Color-only payload (ENTITY_EFFECT, FLASH, etc.).
     */
    record ColorOnly(@NotNull Color color) implements ParticlePayload {
        @Override
        public @Nullable Object toBukkitData(@NotNull Location origin, @NotNull ParticleDataCapabilities caps) {
            return color;
        }

        @Override
        public @NotNull Element toXml(@NotNull Document doc) {
            Element element = doc.createElement("color");
            element.setAttribute("value", XmlUtils.formatColor(color));
            return element;
        }
    }

    /**
     * Spell payload (color + power) for EFFECT / INSTANT_EFFECT particles.
     * <p>
     * Uses Particle.Spell when available.
     */
    record Spell(@NotNull Color color, float power) implements ParticlePayload {
        @Override
        public @Nullable Object toBukkitData(@NotNull Location origin, @NotNull ParticleDataCapabilities caps) {
            Class<?> spellClass = caps.spellClass();
            if (spellClass == null) return null;

            try {
                return spellClass.getConstructor(Color.class, float.class).newInstance(color, power);
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }

        @Override
        public @NotNull Element toXml(@NotNull Document doc) {
            Element element = doc.createElement("spell");
            element.setAttribute("color", XmlUtils.formatColor(color));
            element.setAttribute("power", Float.toString(power));
            return element;
        }
    }

    /**
     * Vibration from origin to origin + (dx,dy,dz).
     */
    record VibrationRelative(double offsetX, double offsetY, double offsetZ, int arrivalTicks) implements ParticlePayload {
        @Override
        public @Nullable Object toBukkitData(@NotNull Location origin, @NotNull ParticleDataCapabilities caps) {
            if (!caps.hasVibration()) return null;

            Class<?> vibrationClass = caps.vibrationClass();
            Class<?> destinationInterface = caps.vibrationDestinationInterface();
            Class<?> blockDestClass = caps.vibrationBlockDestinationClass();
            if (vibrationClass == null || destinationInterface == null || blockDestClass == null) return null;

            try {
                Location target = origin.clone().add(offsetX, offsetY, offsetZ);

                Constructor<?> blockDestCtor = blockDestClass.getConstructor(Location.class);
                Object dest = blockDestCtor.newInstance(target);

                Constructor<?> vibrationCtor = vibrationClass.getConstructor(Location.class, destinationInterface, int.class);
                return vibrationCtor.newInstance(origin, dest, arrivalTicks);
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }

        @Override
        public @NotNull Element toXml(@NotNull Document doc) {
            Element element = doc.createElement("vibration");
            element.setAttribute("offsetX", Double.toString(offsetX));
            element.setAttribute("offsetY", Double.toString(offsetY));
            element.setAttribute("offsetZ", Double.toString(offsetZ));
            element.setAttribute("arrival", Integer.toString(arrivalTicks));
            return element;
        }
    }

    /**
     * Trail from origin to origin + (dx,dy,dz).
     */
    record TrailRelative(double offsetX, double offsetY, double offsetZ, @NotNull Color color, int durationTicks) implements ParticlePayload {
        @Override
        public @Nullable Object toBukkitData(@NotNull Location origin, @NotNull ParticleDataCapabilities caps) {
            Class<?> trailClass = caps.trailClass();
            if (trailClass == null) return null;

            try {
                Location target = origin.clone().add(offsetX, offsetY, offsetZ);
                return trailClass.getConstructor(Location.class, Color.class, int.class).newInstance(target, color, durationTicks);
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }

        @Override
        public @NotNull Element toXml(@NotNull Document doc) {
            Element element = doc.createElement("trail");
            element.setAttribute("offsetX", Double.toString(offsetX));
            element.setAttribute("offsetY", Double.toString(offsetY));
            element.setAttribute("offsetZ", Double.toString(offsetZ));
            element.setAttribute("duration", Integer.toString(durationTicks));
            element.setAttribute("color", XmlUtils.formatColor(color));
            return element;
        }
    }
}

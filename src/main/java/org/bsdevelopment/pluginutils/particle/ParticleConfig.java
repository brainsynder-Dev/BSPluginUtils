/*
 * Copyright © 2025
 * BSDevelopment <https://bsdevelopment.org>
 */

package org.bsdevelopment.pluginutils.particle;

import org.bsdevelopment.pluginutils.gui.parser.XmlValidationException;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * XML-serializable definition of a particle effect.
 * <p>
 * Contains:
 * - Particle type + optional fallback
 * - count, offsets, extra, force
 * - Optional {@link ParticlePayload}
 */
public record ParticleConfig(@NotNull ParticleTypeWrapper handle, int count, double offsetX, double offsetY, double offsetZ, double extra, boolean force, @Nullable ParticlePayload payload) {
    /**
     * Reads a ParticleConfig from a &lt;particle&gt; element.
     */
    public static @NotNull ParticleConfig fromXml(@NotNull Element element) throws XmlValidationException {
        String typeName = requireAttr(element, "type", "Set type=\"PARTICLE_NAME\" (e.g. type=\"FLAME\").");
        String fallbackName = optionalAttr(element, "fallback", null);

        ParticleTypeWrapper handle;
        if (fallbackName != null && !fallbackName.isEmpty()) {
            handle = ParticleTypeWrapper.named(typeName, ParticleTypeWrapper.named(fallbackName));
        } else {
            handle = ParticleTypeWrapper.named(typeName);
        }

        int count = parseIntAttr(element, "count", 1, "count must be an integer, e.g. count=\"10\".");
        double offsetX = parseDoubleAttr(element, "offsetX", 0.0, "offsetX must be a number, e.g. offsetX=\"0.25\".");
        double offsetY = parseDoubleAttr(element, "offsetY", 0.0, "offsetY must be a number, e.g. offsetY=\"0.25\".");
        double offsetZ = parseDoubleAttr(element, "offsetZ", 0.0, "offsetZ must be a number, e.g. offsetZ=\"0.25\".");
        double extra = parseDoubleAttr(element, "extra", 0.0, "extra must be a number (speed/extra parameter).");
        boolean force = parseBooleanAttr(element, "force", false);

        Element payloadElement = firstChildElement(element);
        ParticlePayload payload = ParticlePayload.fromXml(payloadElement);

        return new ParticleConfig(handle, count, offsetX, offsetY, offsetZ, extra, force, payload);
    }

    private static @NotNull String requireAttr(@NotNull Element element, @NotNull String name, @NotNull String hint) throws XmlValidationException {
        String value = element.getAttribute(name);
        if (value.isEmpty()) throw new XmlValidationException(element, "Missing required attribute '" + name + "' on <" + element.getTagName() + ">.", hint);
        return value.trim();
    }

    private static @Nullable String optionalAttr(@NotNull Element element, @NotNull String name, @Nullable String def) {
        String value = element.getAttribute(name);
        if (value.isEmpty()) return def;
        return value.trim();
    }

    private static int parseIntAttr(@NotNull Element element, @NotNull String name, int def, @NotNull String hint) throws XmlValidationException {
        String raw = optionalAttr(element, name, null);
        if (raw == null) return def;

        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            throw new XmlValidationException(element, "Invalid integer for '" + name + "': " + raw, hint);
        }
    }

    private static double parseDoubleAttr(@NotNull Element element, @NotNull String name, double def, @NotNull String hint) throws XmlValidationException {
        String raw = optionalAttr(element, name, null);
        if (raw == null) return def;

        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            throw new XmlValidationException(element, "Invalid double for '" + name + "': " + raw, hint);
        }
    }

    private static boolean parseBooleanAttr(@NotNull Element element, @NotNull String name, boolean def) {
        String raw = optionalAttr(element, name, null);
        if (raw == null) return def;

        return Boolean.parseBoolean(raw);
    }

    private static @Nullable Element firstChildElement(@NotNull Element parent) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);

            if (node instanceof Element element) return element;
        }
        return null;
    }

    public @NotNull ParticleRequest toRequest(@NotNull World world, @NotNull Location origin) {
        return new ParticleRequest(world, origin, handle, count, offsetX, offsetY, offsetZ, extra, payload, force);
    }

    /**
     * Writes this config to a &lt;particle&gt; element.
     */
    public @NotNull Element toXml(@NotNull Document doc, @NotNull String elementName) {
        Element root = doc.createElement(elementName);

        root.setAttribute("type", handle.bukkitName());
        if (handle.fallback() != null) root.setAttribute("fallback", handle.fallback().bukkitName());

        root.setAttribute("count", Integer.toString(count));
        root.setAttribute("offsetX", Double.toString(offsetX));
        root.setAttribute("offsetY", Double.toString(offsetY));
        root.setAttribute("offsetZ", Double.toString(offsetZ));
        root.setAttribute("extra", Double.toString(extra));
        root.setAttribute("force", Boolean.toString(force));

        if (payload != null && !(payload instanceof ParticlePayload.None)) {
            Element payloadElement = payload.toXml(doc);
            root.appendChild(payloadElement);
        }

        return root;
    }
}

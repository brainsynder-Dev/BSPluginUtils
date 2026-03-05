/*
 * Copyright © 2025
 * BSDevelopment <https://bsdevelopment.org>
 */

package org.bsdevelopment.pluginutils.particle;

import org.bsdevelopment.pluginutils.xml.XmlUtils;
import org.bsdevelopment.pluginutils.xml.XmlValidationException;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * XML-serializable definition of a particle effect.
 * <p>
 * Contains:
 * <ul>
 *   <li>Particle type + optional fallback</li>
 *   <li>count, offsets, extra, force</li>
 *   <li>Optional {@link ParticlePayload}</li>
 * </ul>
 */
public record ParticleConfig(
        @NotNull ParticleTypeWrapper handle,
        int count,
        double offsetX,
        double offsetY,
        double offsetZ,
        double extra,
        boolean force,
        @Nullable ParticlePayload payload) {

    /**
     * Reads a ParticleConfig from a &lt;particle&gt; element.
     */
    public static @NotNull ParticleConfig fromXml(@NotNull Element element) throws XmlValidationException {
        String typeName = XmlUtils.requireAttr(element, "type",
                "Set type=\"PARTICLE_NAME\" (e.g. type=\"FLAME\").");
        String fallbackName = XmlUtils.optionalAttr(element, "fallback", null);

        ParticleTypeWrapper handle = (fallbackName != null && !fallbackName.isEmpty())
                ? ParticleTypeWrapper.named(typeName, ParticleTypeWrapper.named(fallbackName))
                : ParticleTypeWrapper.named(typeName);

        int count       = XmlUtils.parseIntAttr(element, "count",   1,   "count must be an integer, e.g. count=\"10\".");
        double offsetX  = XmlUtils.parseDoubleAttr(element, "offsetX", 0.0, "offsetX must be a number, e.g. offsetX=\"0.25\".");
        double offsetY  = XmlUtils.parseDoubleAttr(element, "offsetY", 0.0, "offsetY must be a number, e.g. offsetY=\"0.25\".");
        double offsetZ  = XmlUtils.parseDoubleAttr(element, "offsetZ", 0.0, "offsetZ must be a number, e.g. offsetZ=\"0.25\".");
        double extra    = XmlUtils.parseDoubleAttr(element, "extra",   0.0, "extra must be a number (speed/extra parameter).");
        boolean force   = XmlUtils.parseBoolAttr(element, "force", false);

        Element payloadElement = XmlUtils.firstChildElement(element);
        ParticlePayload payload = ParticlePayload.fromXml(payloadElement);

        return new ParticleConfig(handle, count, offsetX, offsetY, offsetZ, extra, force, payload);
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

        root.setAttribute("count",   Integer.toString(count));
        root.setAttribute("offsetX", Double.toString(offsetX));
        root.setAttribute("offsetY", Double.toString(offsetY));
        root.setAttribute("offsetZ", Double.toString(offsetZ));
        root.setAttribute("extra",   Double.toString(extra));
        root.setAttribute("force",   Boolean.toString(force));

        if (payload != null && !(payload instanceof ParticlePayload.None)) {
            root.appendChild(payload.toXml(doc));
        }

        return root;
    }
}

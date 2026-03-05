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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for working with {@link ParticleConfig} and XML.
 * <p>
 * Responsibilities:
 * <ul>
 *     <li>Load a single {@link ParticleConfig} from XML</li>
 *     <li>Load multiple configs from an XML list (&lt;particles&gt;)</li>
 *     <li>Save single / multiple configs to XML</li>
 *     <li>Small runtime conveniences for spawning</li>
 * </ul>
 * <p>
 * XML shapes supported:
 *
 * <pre>{@code
 * <!-- Single -->
 * <particle type="DUST" ...>
 *     <dust color="#FF8800" size="1.0"/>
 * </particle>
 *
 * <!-- List -->
 * <particles>
 *     <particle .../>
 *     <particle .../>
 * </particles>
 * }</pre>
 */
public final class ParticleUtils {

    /**
     * Load a single {@link ParticleConfig} from a {@link Path} whose root element
     * is expected to be &lt;particle&gt;.
     */
    public static @NotNull ParticleConfig loadConfig(@NotNull Path path) throws IOException, XmlValidationException {
        try (InputStream in = Files.newInputStream(path)) {
            return loadConfig(in);
        }
    }

    /**
     * Load a single {@link ParticleConfig} from an {@link InputStream} whose root
     * element is expected to be &lt;particle&gt;.
     */
    public static @NotNull ParticleConfig loadConfig(@NotNull InputStream in) throws IOException, XmlValidationException {
        Document document = XmlUtils.parseDocument(in);
        Element element = document.getDocumentElement();

        if (element == null)
            throw new XmlValidationException(null, "XML document has no root element.",
                    "Ensure the file starts with <particle>...</particle>.");

        if (!"particle".equals(element.getTagName()))
            throw new XmlValidationException(element,
                    "Unexpected root element <" + element.getTagName() + "> when loading ParticleConfig.",
                    "Use <particle> as the root element for a single particle config.");

        return ParticleConfig.fromXml(element);
    }

    /**
     * Load a list of {@link ParticleConfig} from a {@link Path} whose root element
     * is expected to be &lt;particles&gt; containing children &lt;particle&gt;.
     */
    public static @NotNull List<ParticleConfig> loadConfigList(@NotNull Path path) throws IOException, XmlValidationException {
        try (InputStream in = Files.newInputStream(path)) {
            return loadConfigList(in);
        }
    }

    /**
     * Load a list of {@link ParticleConfig} from an {@link InputStream} whose root
     * is &lt;particles&gt; with child &lt;particle&gt; elements.
     */
    public static @NotNull List<ParticleConfig> loadConfigList(@NotNull InputStream in) throws IOException, XmlValidationException {
        Document document = XmlUtils.parseDocument(in);
        Element rootElement = requireParticlesRoot(document);

        List<ParticleConfig> result = new ArrayList<>();
        NodeList children = rootElement.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element element && "particle".equals(element.getTagName()))
                result.add(ParticleConfig.fromXml(element));
        }

        return result;
    }

    /**
     * Save a single {@link ParticleConfig} to a {@link Path} with a root &lt;particle&gt; element.
     */
    public static void saveConfig(@NotNull ParticleConfig config, @NotNull Path path) throws IOException {
        Document document = XmlUtils.newDocument();
        document.appendChild(config.toXml(document, "particle"));
        XmlUtils.writeDocument(document, path);
    }

    /**
     * Save a list of {@link ParticleConfig} to a {@link Path} as:
     *
     * <pre>{@code
     * <particles>
     *     <particle .../>
     *     <particle .../>
     * </particles>
     * }</pre>
     */
    public static void saveConfigList(@NotNull List<ParticleConfig> configs, @NotNull Path path) throws IOException {
        Document document = XmlUtils.newDocument();
        Element root = document.createElement("particles");
        document.appendChild(root);

        for (ParticleConfig config : configs) {
            root.appendChild(config.toXml(document, "particle"));
        }

        XmlUtils.writeDocument(document, path);
    }

    /**
     * Convert a single {@link ParticleConfig} into a standalone XML string with &lt;particle&gt; root.
     */
    public static @NotNull String toXmlString(@NotNull ParticleConfig config) {
        Document document = XmlUtils.newDocument();
        document.appendChild(config.toXml(document, "particle"));
        return XmlUtils.documentToString(document);
    }

    /**
     * Convert a list of {@link ParticleConfig} into a standalone XML string with &lt;particles&gt; root.
     */
    public static @NotNull String toXmlString(@NotNull List<ParticleConfig> configs) {
        Document document = XmlUtils.newDocument();
        Element root = document.createElement("particles");
        document.appendChild(root);

        for (ParticleConfig config : configs) {
            root.appendChild(config.toXml(document, "particle"));
        }

        return XmlUtils.documentToString(document);
    }

    /**
     * Convenience: build a request from config and immediately spawn it.
     */
    public static void spawn(@NotNull ParticleConfig config, @NotNull World world, @NotNull Location origin) {
        config.toRequest(world, origin).spawn();
    }

    private static @NotNull Element requireParticlesRoot(@NotNull Document document) {
        Element root = document.getDocumentElement();

        if (root == null)
            throw new XmlValidationException(null, "XML document has no root element.",
                    "Ensure the file has <particles> as the root and <particle> entries inside.");

        if (!"particles".equals(root.getTagName()))
            throw new XmlValidationException(root,
                    "Unexpected root element <" + root.getTagName() + "> when loading list of ParticleConfig.",
                    "Use <particles> as the root element for a list of particle configs.");

        return root;
    }
}

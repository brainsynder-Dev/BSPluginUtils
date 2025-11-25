/*
 * Copyright © 2025
 * BSDevelopment <https://bsdevelopment.org>
 */

package org.bsdevelopment.pluginutils.particle;

import org.bsdevelopment.pluginutils.gui.parser.XmlValidationException;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
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
        Document document = parseDocument(in);
        Element element = document.getDocumentElement();

        if (element == null) throw new XmlValidationException(null, "XML document has no root element.", "Ensure the file starts with <particle>...</particle>.");

        if (!"particle".equals(element.getTagName()))
            throw new XmlValidationException(element, "Unexpected root element <" + element.getTagName() + "> when loading ParticleConfig.",
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
        Document document = parseDocument(in);
        Element rootElement = getElement(document);

        List<ParticleConfig> result = new ArrayList<>();
        NodeList children = rootElement.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);

            if (node instanceof Element element && "particle".equals(element.getTagName())) result.add(ParticleConfig.fromXml(element));
        }

        return result;
    }

    @NotNull
    private static Element getElement(Document document) {
        Element rootElement = document.getDocumentElement();

        if (rootElement == null) throw new XmlValidationException(null, "XML document has no root element.", "Ensure the file has <particles> as the root and <particle> entries inside.");

        if (!"particles".equals(rootElement.getTagName()))
            throw new XmlValidationException(rootElement, "Unexpected root element <" + rootElement.getTagName() + "> when loading list of ParticleConfig.",
                    "Use <particles> as the root element for a list of particle configs.");

        return rootElement;
    }

    /**
     * Save a single {@link ParticleConfig} to a {@link Path} with a root
     * &lt;particle&gt; element.
     */
    public static void saveConfig(@NotNull ParticleConfig config, @NotNull Path path) throws IOException {
        Document document = newEmptyDocument();
        Element element = config.toXml(document, "particle");
        document.appendChild(element);
        writeDocument(document, path);
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
        Document document = newEmptyDocument();
        Element element = document.createElement("particles");
        document.appendChild(element);

        for (ParticleConfig particleConfig : configs) {
            Element child = particleConfig.toXml(document, "particle");
            element.appendChild(child);
        }

        writeDocument(document, path);
    }

    /**
     * Convert a single {@link ParticleConfig} into a standalone XML string
     * with &lt;particle&gt; root.
     */
    public static @NotNull String toXmlString(@NotNull ParticleConfig config) {
        Document document = newEmptyDocument();
        Element element = config.toXml(document, "particle");
        document.appendChild(element);
        return documentToString(document);
    }

    /**
     * Convert a list of {@link ParticleConfig} into a standalone XML string
     * with &lt;particles&gt; root.
     */
    public static @NotNull String toXmlString(@NotNull List<ParticleConfig> configs) {
        Document document = newEmptyDocument();
        Element element = document.createElement("particles");
        document.appendChild(element);

        for (ParticleConfig particleConfig : configs) {
            Element child = particleConfig.toXml(document, "particle");
            element.appendChild(child);
        }

        return documentToString(document);
    }

    /**
     * Convenience: build a request from config and immediately spawn it.
     */
    public static void spawn(@NotNull ParticleConfig config, @NotNull World world, @NotNull Location origin) {
        config.toRequest(world, origin).spawn();
    }

    private static @NotNull Document parseDocument(@NotNull InputStream inputStream) throws IOException, XmlValidationException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(inputStream);
        } catch (ParserConfigurationException | SAXException e) {
            throw new XmlValidationException(null, "Failed to parse particle XML: " + e.getMessage(),
                    "Ensure the file is well-formed XML and follows the expected <particle> or <particles> structure.");
        }
    }

    private static @NotNull Document newEmptyDocument() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.newDocument();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Unable to create new XML Document", e);
        }
    }

    private static void writeDocument(@NotNull Document doc, @NotNull Path path) throws IOException {
        TransformerFactory factory = TransformerFactory.newInstance();
        try (OutputStream outputStream = Files.newOutputStream(path)) {
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            transformer.transform(new DOMSource(doc), new StreamResult(outputStream));
        } catch (Exception e) {
            throw new IOException("Failed to write particle XML to " + path, e);
        }
    }

    private static @NotNull String documentToString(@NotNull Document doc) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to convert Document to String", e);
        }
    }
}

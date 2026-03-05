package org.bsdevelopment.pluginutils.xml.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Pure-data representation of a single {@code <action>} element.
 *
 * <p>Decoupled from XML DOM — created both by {@link org.bsdevelopment.pluginutils.xml.io.XmlGuiReader}
 * and the fluent {@link XmlGuiDefinition.DefinitionBuilder} API.
 *
 * <p>Example XML equivalents:
 * <pre>{@code
 * <action type="message">&aHello!</action>
 * <action type="give" item="myplugin:sword" amount="1"/>
 * <action type="close"/>
 * }</pre>
 */
public final class XmlActionDefinition {

    private final String type;
    private final Map<String, String> attributes;
    private final String text;

    /**
     * @param type       action type key (will be lowercased)
     * @param attributes extra XML attributes excluding {@code type} (may be null)
     * @param text       text content of the {@code <action>} element (may be null)
     */
    public XmlActionDefinition(String type, Map<String, String> attributes, String text) {
        this.type = type == null ? "" : type.toLowerCase(Locale.ROOT);
        this.attributes = attributes == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
        this.text = text == null ? "" : text.trim();
    }

    /** The action type key (lower-case), e.g. {@code "message"}, {@code "close"}. */
    public String getType() {
        return type;
    }

    /** All extra attributes on the {@code <action>} tag (excluding {@code type}). */
    public Map<String, String> getAttributes() {
        return attributes;
    }

    /** Text content of the {@code <action>} element, or empty string if absent. */
    public String getText() {
        return text;
    }

    /**
     * Returns the value of a named attribute, or {@code ""} if not present.
     *
     * @param key attribute name
     *
     * @return attribute value or empty string
     */
    public String getAttribute(String key) {
        return attributes.getOrDefault(key, "");
    }

    /**
     * Returns {@code true} if the named attribute exists and is non-blank.
     *
     * @param key attribute name
     *
     * @return whether the attribute is present and non-blank
     */
    public boolean hasAttribute(String key) {
        String value = attributes.get(key);
        return value != null && !value.isBlank();
    }

    @Override
    public String toString() {
        return "XmlActionDefinition{type='" + type + "', attributes=" + attributes + ", text='" + text + "'}";
    }
}

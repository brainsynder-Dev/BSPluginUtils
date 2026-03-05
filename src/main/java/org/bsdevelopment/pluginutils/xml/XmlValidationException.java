package org.bsdevelopment.pluginutils.xml;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Thrown when XML validation or parsing fails anywhere in BSPluginUtils.
 *
 * <p>Carries the offending {@link Element}'s tag name and attributes in the message,
 * plus a user-facing hint describing how to fix the problem.
 */
public class XmlValidationException extends RuntimeException {

    public XmlValidationException(Element element, String error, String hint) {
        super(String.format("%s%s%nHint: %s", element != null ? "[" + element.getTagName() + "@" + describe(element) + "] " : "", error, hint));
    }

    private static String describe(Element element) {
        NamedNodeMap attrs = element.getAttributes();
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node node = attrs.item(i);
            parts.add(node.getNodeName() + "=" + node.getNodeValue());
        }
        return String.join(",", parts);
    }
}

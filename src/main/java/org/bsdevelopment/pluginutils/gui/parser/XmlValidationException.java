package org.bsdevelopment.pluginutils.gui.parser;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Thrown when XML validation or parsing fails
 */
public class XmlValidationException extends RuntimeException {
    public XmlValidationException(Element e, String error, String hint) {
        super(String.format(
            "%s%s%nHint: %s",
            e != null ? "[" + e.getTagName() + "@" + describe(e) + "] " : "",
            error, hint
        ));
    }


    private static String describe(Element e) {
        NamedNodeMap attrs = e.getAttributes();
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node node = attrs.item(i);
            parts.add(node.getNodeName() + "=" + node.getNodeValue());
        }
        return String.join(",", parts);
    }
}

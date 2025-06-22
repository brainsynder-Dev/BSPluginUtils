package org.bsdevelopment.pluginutils.gui;

import org.w3c.dom.Element;

/**
 * Builds a {@link GuiAction} from its XML element.
 */
@FunctionalInterface
public interface ActionFactory {
    /**
     * Create a GuiAction from the given <action> element.
     *
     * @param element the XML element (type, attributes, text-content)
     * @return a new GuiAction instance
     */
    GuiAction create(Element element);
}

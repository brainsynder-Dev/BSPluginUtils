package org.bsdevelopment.pluginutils.xml;

import org.bsdevelopment.pluginutils.xml.model.XmlActionDefinition;

/**
 * Creates an {@link XmlGuiAction} from a parsed {@link XmlActionDefinition}.
 *
 * <p>Register implementations via {@link XmlActionRegistry#register(String, XmlActionFactory)}.
 *
 * <p>Example:
 * <pre>{@code
 * XmlActionRegistry.register("sound", def -> event -> {
 *     Sound sound = Sound.valueOf(def.getText().toUpperCase());
 *     if (event.getWhoClicked() instanceof Player player)
 *         player.playSound(player.getLocation(), sound, 1f, 1f);
 * });
 * }</pre>
 */
@FunctionalInterface
public interface XmlActionFactory {

    /**
     * Build an {@link XmlGuiAction} from the given action definition.
     *
     * <p>This method is called once per {@code <action>} element during GUI compilation.
     * The returned lambda will be called each time a player clicks the associated slot.
     *
     * @param definition the parsed action data
     *
     * @return a ready-to-execute action
     */
    XmlGuiAction create(XmlActionDefinition definition);
}

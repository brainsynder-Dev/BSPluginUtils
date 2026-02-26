package org.bsdevelopment.pluginutils.dialog;

import io.github.projectunified.unidialog.core.payload.DialogPayload;
import org.bsdevelopment.pluginutils.PluginUtilities;

import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Entry point for the unified dialog API. Works transparently on both Spigot and
 * Paper servers by delegating to whichever {@code DialogManager} was selected
 * during {@link PluginUtilities#initialize(org.bukkit.plugin.Plugin)}.
 *
 * <p>All builder instances are obtained from the static factory methods here.
 * No platform-specific imports are needed in your own code.
 *
 * <h2>Dialog types</h2>
 * <ul>
 *   <li>{@link #notice()} — informational dialog with one action button</li>
 *   <li>{@link #confirm()} — yes/no confirmation dialog</li>
 *   <li>{@link #multiAction()} — grid of multiple action buttons</li>
 *   <li>{@link #serverLinks()} — displays the server's configured links</li>
 *   <li>{@link #dialogList()} — grid of buttons, each opening a child dialog</li>
 * </ul>
 *
 * <h2>Custom action registration</h2>
 * <p>Register server-side handlers that respond to {@code dynamicCustom} button clicks:
 * <pre>
 * // Register once during onEnable:
 * UniDialog.registerAction("my_action", payload -> {
 *     Player p = Bukkit.getPlayer(payload.owner());
 *     String value = payload.textValue("inputKey");
 *     // ... handle response
 * });
 *
 * // Reference in a button:
 * UniDialog.notice()
 *     .input("inputKey", i -> i.textInput().label("Enter value"))
 *     .action(a -> a.label("Submit").dynamicCustom("my_action"))
 *     .open(player);
 * </pre>
 *
 * <h2>Full example</h2>
 * <pre>
 * // Simple notice:
 * UniDialog.notice()
 *     .title("Welcome!")
 *     .body(b -> b.text().text("Thanks for joining the server."))
 *     .action(a -> a.label("Play!").runCommand("/spawn"))
 *     .open(player);
 *
 * // Confirmation:
 * UniDialog.confirm()
 *     .title("Delete Home")
 *     .body(b -> b.text().text("This cannot be undone."))
 *     .yes(a -> a.label("Delete").runCommand("/delhome confirm"))
 *     .no(a -> a.label("Cancel"))
 *     .open(player);
 *
 * // Dialog list with child dialogs built separately:
 * DialogOpener helpPage = UniDialog.notice()
 *     .title("Help")
 *     .body(b -> b.text().text("Use /help to see commands."))
 *     .opener(); // builds but does not open
 *
 * UniDialog.dialogList()
 *     .title("Main Menu")
 *     .dialog(helpPage)
 *     .columns(1)
 *     .open(player);
 * </pre>
 */
public final class UniDialog {
    /**
     * Creates a builder for a notice dialog (single action button).
     *
     * @return a new {@link NoticeDialogBuilder}
     */
    public static NoticeDialogBuilder notice() {
        return new NoticeDialogBuilder();
    }

    /**
     * Creates a builder for a confirmation dialog (yes / no buttons).
     *
     * @return a new {@link ConfirmDialogBuilder}
     */
    public static ConfirmDialogBuilder confirm() {
        return new ConfirmDialogBuilder();
    }

    /**
     * Creates a builder for a multi-action dialog (grid of buttons).
     *
     * @return a new {@link MultiActionDialogBuilder}
     */
    public static MultiActionDialogBuilder multiAction() {
        return new MultiActionDialogBuilder();
    }

    /**
     * Creates a builder for a server-links dialog.
     *
     * @return a new {@link ServerLinksDialogBuilder}
     */
    public static ServerLinksDialogBuilder serverLinks() {
        return new ServerLinksDialogBuilder();
    }

    /**
     * Creates a builder for a dialog-list dialog (grid of child-dialog buttons).
     *
     * @return a new {@link DialogListDialogBuilder}
     */
    public static DialogListDialogBuilder dialogList() {
        return new DialogListDialogBuilder();
    }

    /**
     * Registers a server-side handler for the given action id (uses the default namespace).
     * Triggered when a player clicks a button configured with
     * {@link UniActionBuilder#dynamicCustom(String)}.
     *
     * @param id     the action identifier
     * @param action the handler receiving the full {@link DialogPayload}
     */
    public static void registerAction(String id, Consumer<DialogPayload> action) {
        PluginUtilities.getDialogManager().registerCustomAction(id, action);
    }

    /**
     * Registers a server-side handler for the given namespace and action id.
     * Triggered when a player clicks a button configured with
     * {@link UniActionBuilder#dynamicCustom(String, String)}.
     *
     * @param namespace the action namespace
     * @param id        the action identifier
     * @param action    the handler receiving the full {@link DialogPayload}
     */
    public static void registerAction(String namespace, String id, Consumer<DialogPayload> action) {
        PluginUtilities.getDialogManager().registerCustomAction(namespace, id, action);
    }

    /**
     * Registers a server-side handler using a simplified {@link BiConsumer} signature
     * that receives the player UUID and a flat key→value map of all input values.
     *
     * @param id     the action identifier
     * @param action the handler
     */
    public static void registerAction(String id, BiConsumer<UUID, Map<String, String>> action) {
        PluginUtilities.getDialogManager().registerCustomAction(id, action);
    }

    /**
     * Registers a server-side handler using a simplified {@link BiConsumer} signature
     * with an explicit namespace.
     *
     * @param namespace the action namespace
     * @param id        the action identifier
     * @param action    the handler
     */
    public static void registerAction(String namespace, String id, BiConsumer<UUID, Map<String, String>> action) {
        PluginUtilities.getDialogManager().registerCustomAction(namespace, id, action);
    }

    /**
     * Unregisters a previously registered custom action (uses the default namespace).
     *
     * @param id the action identifier to remove
     */
    public static void unregisterAction(String id) {
        PluginUtilities.getDialogManager().unregisterCustomAction(id);
    }

    /**
     * Unregisters a previously registered custom action by namespace and id.
     *
     * @param namespace the action namespace
     * @param id        the action identifier to remove
     */
    public static void unregisterAction(String namespace, String id) {
        PluginUtilities.getDialogManager().unregisterCustomAction(namespace, id);
    }

    /**
     * Unregisters all custom actions registered through this manager.
     */
    public static void unregisterAllActions() {
        PluginUtilities.getDialogManager().unregisterAllCustomActions();
    }

    /**
     * Clears the currently open dialog for the given player UUID, if any.
     *
     * @param uuid the player's UUID
     *
     * @return {@code true} if a dialog was cleared
     */
    public static boolean clearDialog(UUID uuid) {
        return PluginUtilities.getDialogManager().clearDialog(uuid);
    }
}

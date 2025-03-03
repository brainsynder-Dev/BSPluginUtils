package org.bsdevelopment.pluginutils.inventory;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Represents an Anvil-based GUI that supports slot interactions and custom items,
 * This class is adapted from Sytm's AnvilGUI implementation but uses plain Spigot instead of Paper.
 *
 * <p><b>Example Usage:</b>
 * <pre>
 * AnvilGUI gui = AnvilGUI.builder()
 *     .plugin(myPlugin)
 *     .text("Type here...")
 *     .title("My Anvil GUI")
 *     .onClick((slot, state) -> {
 *         // Handle slot click
 *         return Collections.singletonList(ResponseAction.close());
 *     })
 *     .open(player);
 * </pre>
 */
public final class AnvilGUI {

    /**
     * Copies the given {@link ItemStack}, returning an air {@link ItemStack} if null.
     *
     * @param stack the ItemStack to check
     * @return a safe copy of the ItemStack, or a new air ItemStack if null
     */
    private static ItemStack copyItemNotNull(ItemStack stack) {
        return stack == null ? new ItemStack(Material.AIR) : stack.clone();
    }

    /**
     * The {@link Plugin} that this anvil GUI is associated with.
     */
    private final Plugin plugin;

    /**
     * The player for whom this GUI is opened.
     */
    private final Player player;

    /**
     * The title for the anvil inventory.
     */
    private final String title;

    /**
     * The initial contents of the anvil inventory.
     */
    private final ItemStack[] initialContents;

    /**
     * If true, prevents the user from closing the GUI manually.
     */
    private final boolean preventClose;

    /**
     * The set of slot indices that the user can interact with.
     */
    private final Set<Integer> interactableSlots;

    /**
     * Called when the anvil GUI is closed.
     */
    private final Consumer<StateSnapshot> closeListener;

    /**
     * If true, allows concurrent execution of async click handlers.
     */
    private final boolean concurrentClickHandlerExecution;

    /**
     * Handles slot click events and returns a list of {@link ResponseAction} in a future.
     */
    private final ClickHandler clickHandler;

    private AnvilView view;
    private AnvilInventory inventory;
    private final ListenUp listener = new ListenUp();
    private boolean open;

    /**
     * Constructs the AnvilGUI with all necessary configuration.
     *
     * @param plugin                          the plugin instance
     * @param player                          the player for whom the GUI is opened
     * @param title                           the display title of the anvil
     * @param initialContents                 the initial items to place in the anvil slots
     * @param preventClose                    if true, disallows manual GUI closing
     * @param interactableSlots               the slots in which the user may interact
     * @param closeListener                   a callback when the GUI is closed
     * @param concurrentClickHandlerExecution if true, allows concurrent async click handlers
     * @param clickHandler                    handles slot click events
     */
    private AnvilGUI(
            Plugin plugin,
            Player player,
            String title,
            ItemStack[] initialContents,
            boolean preventClose,
            Set<Integer> interactableSlots,
            Consumer<StateSnapshot> closeListener,
            boolean concurrentClickHandlerExecution,
            ClickHandler clickHandler
    ) {
        this.plugin = plugin;
        this.player = player;
        this.title = title;
        this.initialContents = initialContents;
        this.preventClose = preventClose;
        this.interactableSlots = interactableSlots;
        this.closeListener = closeListener;
        this.concurrentClickHandlerExecution = concurrentClickHandlerExecution;
        this.clickHandler = clickHandler;
    }

    /**
     * Opens the anvil GUI for the player, registering the necessary event listeners.
     */
    private void openInventory() {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);

        view = MenuType.ANVIL.create(player, title);
        inventory = view.getTopInventory();
        player.openInventory(view);

        // Must set each slot individually due to multiple sub-inventories in an anvil
        for (var i = 0; i < initialContents.length; i++) {
            inventory.setItem(i, initialContents[i]);
        }
        open = true;
    }

    /**
     * Closes the inventory for the player if it is currently open.
     */
    public void closeInventory() {
        closeInventoryInternal();
        player.closeInventory();
    }

    /**
     * Internal method to handle inventory closure logic and cleanup.
     */
    private void closeInventoryInternal() {
        if (!open) return;

        open = false;

        var state = StateSnapshot.fromAnvilGUI(this);
        inventory.clear(); // Prevent item drops

        HandlerList.unregisterAll(listener);

        if (closeListener != null) closeListener.accept(state);
    }

    /**
     * Retrieves the underlying {@link AnvilInventory}.
     *
     * @return the AnvilInventory in use
     */
    public AnvilInventory getInventory() {
        return inventory;
    }

    /**
     * Retrieves the text from the rename field, returning an empty string if null.
     *
     * @return the rename text, or an empty string if not present
     */
    public String getRenameText() {
        return Objects.requireNonNullElse(view.getRenameText(), "");
    }

    /**
     * Internal listener for various inventory events associated with this anvil GUI.
     */
    private final class ListenUp implements Listener {

        @EventHandler
        public void onPluginDisable(PluginDisableEvent event) {
            if (event.getPlugin().equals(plugin)) closeInventory();
        }

        @EventHandler(priority = EventPriority.HIGH)
        public void onPrepareAnvil(PrepareAnvilEvent event) {
            if (!event.getInventory().equals(inventory)) return;

            view.setRepairCost(0);

            var result = initialContents[Slot.OUTPUT];
            if (result != null) event.setResult(result);

            player.updateInventory(); // Awaiting potential future fixes
        }

        /**
         * Prevents double execution of the click handler if concurrency is disallowed.
         */
        private boolean clickHandlerRunning = false;

        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            if (!event.getInventory().equals(inventory)) return;

            var clicker = (Player) event.getWhoClicked();

            // Prevent double-click merges from anvil to player inventory
            var clickedInventory = event.getClickedInventory();
            if (clickedInventory != null
                    && clickedInventory.equals(clicker.getInventory())
                    && event.getClick().equals(ClickType.DOUBLE_CLICK)) {
                event.setCancelled(true);
                return;
            }

            // Disallow SHIFT+CLICK if the source is not the anvil
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                    && !event.getClickedInventory().equals(inventory)) {
                event.setCancelled(true);
                return;
            }

            var rawSlot = event.getRawSlot();
            if (rawSlot >= Slot.INPUT_LEFT && rawSlot <= Slot.OUTPUT) {
                event.setCancelled(!interactableSlots.contains(rawSlot));

                if (clickHandlerRunning && !concurrentClickHandlerExecution) {
                    // Another click handler is already running
                    return;
                }

                var actionsFuture = clickHandler.apply(rawSlot, StateSnapshot.fromAnvilGUI(AnvilGUI.this));
                clickHandlerRunning = true;

                actionsFuture
                        .thenAccept(actions -> {
                            for (var action : actions) {
                                action.accept(AnvilGUI.this, clicker);
                            }
                        })
                        .handle((results, exception) -> {
                            if (exception != null) {
                                plugin.getLogger().severe("An exception occurred in the AnvilGUI clickHandler");
                                exception.printStackTrace();
                            }
                            clickHandlerRunning = false;
                            return null;
                        });
            }
        }

        @EventHandler
        public void onInventoryDrag(InventoryDragEvent event) {
            if (!event.getInventory().equals(inventory)) return;

            for (var slot : Slot.VALUES) {
                if (event.getRawSlots().contains(slot)) {
                    if (!interactableSlots.contains(slot)) event.setCancelled(true);
                }
            }
        }

        @EventHandler
        public void onInventoryClose(InventoryCloseEvent event) {
            if (open && event.getInventory().equals(inventory)) closeInventoryInternal();
        }
    }

    /**
     * Creates a new {@link Builder} to configure an {@link AnvilGUI}.
     *
     * @return a new Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing and opening an {@link AnvilGUI}.
     */
    public static final class Builder {
        private Consumer<StateSnapshot> closeListener;
        private boolean concurrentClickHandlerExecution;
        private ClickHandler clickHandler;
        private boolean preventClose;
        private Set<Integer> interactableSlots = Collections.emptySet();
        private Plugin plugin;
        private String title = "";
        private String itemText;
        private ItemStack itemLeft;
        private ItemStack itemRight;
        private ItemStack itemOutput;

        /**
         * Constructs a new Builder. Must set plugin and click handler before calling open(...).
         */
        public Builder() {}

        /**
         * Prevents the user from closing the anvil GUI manually.
         *
         * @return this Builder
         */
        public Builder preventClose() {
            preventClose = true;
            return this;
        }

        /**
         * Sets which slots can be interacted with by the user.
         *
         * @param slots the slot indices
         * @return this Builder
         */
        public Builder interactableSlots(int... slots) {
            var newValue = new HashSet<Integer>();
            for (var slot : slots) newValue.add(slot);
            interactableSlots = newValue;
            return this;
        }

        /**
         * Called when the anvil GUI is closed.
         *
         * @param closeListener the consumer that runs upon GUI close
         * @return this Builder
         * @throws NullPointerException if closeListener is null
         */
        public Builder onClose(Consumer<StateSnapshot> closeListener) {
            this.closeListener = Objects.requireNonNull(closeListener, "closeListener");
            return this;
        }

        /**
         * Sets the asynchronous click handler.
         *
         * @param clickHandler the asynchronous click handler
         * @return this Builder
         * @throws NullPointerException if clickHandler is null
         */
        public Builder onClickAsync(ClickHandler clickHandler) {
            this.clickHandler = Objects.requireNonNull(clickHandler, "clickHandler");
            return this;
        }

        /**
         * Allows concurrent execution of asynchronous click handlers.
         *
         * @return this Builder
         */
        public Builder allowConcurrentClickHandlerExecution() {
            concurrentClickHandlerExecution = true;
            return this;
        }

        /**
         * Sets a synchronous click handler that returns actions immediately.
         *
         * @param clickHandler a function returning a list of {@link ResponseAction}
         * @return this Builder
         * @throws NullPointerException if clickHandler is null
         */
        public Builder onClick(BiFunction<Integer, StateSnapshot, List<ResponseAction>> clickHandler) {
            Objects.requireNonNull(clickHandler, "clickHandler");
            this.clickHandler = (slot, stateSnapshot) ->
                    CompletableFuture.completedFuture(clickHandler.apply(slot, stateSnapshot));
            return this;
        }

        /**
         * Sets the plugin that owns this anvil GUI.
         *
         * @param plugin the plugin
         * @return this Builder
         * @throws NullPointerException if plugin is null
         */
        public Builder plugin(Plugin plugin) {
            this.plugin = Objects.requireNonNull(plugin, "plugin");
            return this;
        }

        /**
         * Sets the name of the item placed into the left slot initially.
         *
         * @param text the display name
         * @return this Builder
         * @throws NullPointerException if text is null
         */
        public Builder text(String text) {
            itemText = Objects.requireNonNull(text, "text");
            return this;
        }

        /**
         * Sets the title of the anvil GUI.
         *
         * @param title the anvil window title
         * @return this Builder
         * @throws NullPointerException if title is null
         */
        public Builder title(String title) {
            this.title = Objects.requireNonNull(title, "title");
            return this;
        }

        /**
         * Sets the {@link ItemStack} in the left anvil slot.
         *
         * @param item the item
         * @return this Builder
         * @throws NullPointerException if item is null
         */
        public Builder itemLeft(ItemStack item) {
            itemLeft = Objects.requireNonNull(item, "item").clone();
            return this;
        }

        /**
         * Sets the {@link ItemStack} in the right anvil slot.
         *
         * @param item the item
         * @return this Builder
         * @throws NullPointerException if item is null
         */
        public Builder itemRight(ItemStack item) {
            itemRight = Objects.requireNonNull(item, "item").clone();
            return this;
        }

        /**
         * Sets the {@link ItemStack} in the output slot.
         *
         * @param item the item
         * @return this Builder
         * @throws NullPointerException if item is null
         */
        public Builder itemOutput(ItemStack item) {
            itemOutput = Objects.requireNonNull(item, "item").clone();
            return this;
        }

        /**
         * Builds and opens the anvil GUI for the specified player.
         *
         * @param player the player
         * @return the constructed AnvilGUI
         * @throws NullPointerException if the plugin or clickHandler is missing, or if player is null
         */
        public AnvilGUI open(Player player) {
            Objects.requireNonNull(plugin, "Plugin must be set");
            Objects.requireNonNull(clickHandler, "clickHandler must be set");
            Objects.requireNonNull(player, "player");

            if (itemText != null) {
                if (itemLeft == null) {
                    itemLeft = new ItemStack(Material.PAPER);
                }
                var meta = itemLeft.getItemMeta();
                meta.setDisplayName(itemText);
                itemLeft.setItemMeta(meta);
            }

            var anvilGUI = new AnvilGUI(
                    plugin,
                    player,
                    title,
                    new ItemStack[]{itemLeft, itemRight, itemOutput},
                    preventClose,
                    interactableSlots,
                    closeListener,
                    concurrentClickHandlerExecution,
                    clickHandler
            );
            anvilGUI.openInventory();
            return anvilGUI;
        }
    }

    /**
     * A functional interface defining the async click handler. It receives a slot index and a
     * {@link StateSnapshot}, returning a {@link CompletableFuture} of ResponseActions.
     */
    @FunctionalInterface
    public interface ClickHandler
            extends BiFunction<Integer, StateSnapshot, CompletableFuture<List<ResponseAction>>> {
    }

    /**
     * Represents an action triggered by a slot click. Multiple static methods are provided
     * to return common behaviors like closing the GUI, running a task, or updating the text.
     */
    @FunctionalInterface
    public interface ResponseAction extends BiConsumer<AnvilGUI, Player> {

        /**
         * Replaces the text in the left slot (or output slot if left is empty) with the specified text.
         *
         * @param text the text to insert
         * @return the response action
         * @throws NullPointerException  if text is null
         * @throws IllegalStateException if both {@link Slot#INPUT_LEFT} and {@link Slot#OUTPUT} are empty
         */
        static ResponseAction replaceInputText(String text) {
            Objects.requireNonNull(text, "text");
            return (anvilGUI, player) -> {
                var item = anvilGUI.getInventory().getItem(Slot.OUTPUT);
                if (item == null) {
                    item = anvilGUI.getInventory().getItem(Slot.INPUT_LEFT);
                }
                if (item == null) {
                    throw new IllegalStateException(
                            "replaceInputText can only be used if slots OUTPUT or INPUT_LEFT have an item."
                    );
                }
                var cloned = item.clone();
                var meta = cloned.getItemMeta();
                meta.setDisplayName(text);
                cloned.setItemMeta(meta);
                anvilGUI.getInventory().setItem(Slot.INPUT_LEFT, cloned);
            };
        }

        /**
         * Updates the anvil GUI's title.
         *
         * @param title              the new title
         * @param preserveRenameText if true, preserves the user's rename text
         * @return the response action
         * @throws NullPointerException if title is null
         */
        static ResponseAction updateTitle(String title, boolean preserveRenameText) {
            Objects.requireNonNull(title, "title");
            return (anvilGUI, player) -> {
                var oldState = StateSnapshot.fromAnvilGUI(anvilGUI);
                anvilGUI.inventory.clear();

                anvilGUI.view = MenuType.ANVIL.create(player, title);
                var newInventory = anvilGUI.view.getTopInventory();
                anvilGUI.inventory = newInventory;

                if (preserveRenameText) {
                    var firstItem = oldState.leftItem();
                    var builder = ItemBuilder.of(firstItem);
                    if (firstItem != null && firstItem.getType() != Material.AIR) {
                        builder.withName(oldState.text);
                        newInventory.setItem(Slot.INPUT_LEFT, builder.build());
                    }
                } else {
                    newInventory.setItem(Slot.INPUT_LEFT, oldState.leftItem());
                }
                newInventory.setItem(Slot.INPUT_RIGHT, oldState.rightItem());
                newInventory.setItem(Slot.OUTPUT, oldState.outputItem());

                player.openInventory(anvilGUI.view);
            };
        }

        /**
         * Opens another {@link Inventory} for the player.
         *
         * @param otherInventory the inventory to open
         * @return the response action
         * @throws NullPointerException if otherInventory is null
         */
        static ResponseAction openInventory(Inventory otherInventory) {
            Objects.requireNonNull(otherInventory, "otherInventory");
            return (anvilGUI, player) -> player.openInventory(otherInventory);
        }

        /**
         * Closes the anvil GUI.
         *
         * @return the response action
         */
        static ResponseAction close() {
            return (anvilGUI, player) -> anvilGUI.closeInventory();
        }

        /**
         * Runs the provided {@link Runnable}.
         *
         * @param runnable the runnable to execute
         * @return the response action
         * @throws NullPointerException if runnable is null
         */
        static ResponseAction run(Runnable runnable) {
            Objects.requireNonNull(runnable, "runnable");
            return (anvilGUI, player) -> runnable.run();
        }
    }

    /**
     * Contains the numeric slot values for the anvil input and output.
     */
    public static final class Slot {
        private Slot() {}

        /**
         * The left input slot where an item is placed for renaming.
         */
        public static final int INPUT_LEFT = 0;

        /**
         * The right input slot for combining items (unused in this simplified GUI).
         */
        public static final int INPUT_RIGHT = 1;

        /**
         * The output slot where the resulting item is placed.
         */
        public static final int OUTPUT = 2;

        private static final int[] VALUES = new int[] { INPUT_LEFT, INPUT_RIGHT, OUTPUT };

        /**
         * Returns all slot indices for the anvil: {@link #INPUT_LEFT}, {@link #INPUT_RIGHT}, and {@link #OUTPUT}.
         *
         * @return an array of slot indices
         */
        public static int[] values() {
            return VALUES.clone();
        }
    }

    /**
     * A snapshot of the anvil's state at a given moment, containing the text,
     * left item, right item, output item, and the player who interacted.
     *
     * @param text       the rename text
     * @param leftItem   the left item
     * @param rightItem  the right item
     * @param outputItem the output item
     * @param player     the player interacting
     */
    public record StateSnapshot(
            String text,
            ItemStack leftItem,
            ItemStack rightItem,
            ItemStack outputItem,
            Player player
    ) {
        /**
         * Builds a snapshot from the current state of the given {@link AnvilGUI}.
         *
         * @param anvilGUI the AnvilGUI instance
         * @return the created snapshot
         */
        private static StateSnapshot fromAnvilGUI(AnvilGUI anvilGUI) {
            var inv = anvilGUI.getInventory();
            return new StateSnapshot(
                    anvilGUI.getRenameText(),
                    copyItemNotNull(inv.getItem(Slot.INPUT_LEFT)),
                    copyItemNotNull(inv.getItem(Slot.INPUT_RIGHT)),
                    copyItemNotNull(inv.getItem(Slot.OUTPUT)),
                    anvilGUI.player
            );
        }
    }
}

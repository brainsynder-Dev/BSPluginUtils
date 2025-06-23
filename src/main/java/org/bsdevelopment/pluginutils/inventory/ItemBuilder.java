package org.bsdevelopment.pluginutils.inventory;

import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bsdevelopment.pluginutils.nbt.serialization.NBTJSON;
import org.bsdevelopment.pluginutils.nbt.types.CompoundData;
import org.bsdevelopment.pluginutils.text.Colorize;
import org.bsdevelopment.pluginutils.text.WordUtils;
import org.bsdevelopment.pluginutils.utilities.PlayerProfileHelper;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A builder class to create and customize {@link ItemStack} objects.
 *
 * <p>This builder provides a fluent API to set properties such as display name,
 * enchantments, lore, item flags, and more.
 *
 * <p><b>Examples:</b>
 * <pre>
 * // Create an ItemStack of DIAMOND_SWORD with a custom name, lore, enchantments, and flags.
 * ItemStack sword = ItemBuilder.of(Material.DIAMOND_SWORD)
 *     .withName("&aEpic Sword")
 *     .withLore(Arrays.asList("&7This sword is", "&7truly epic!"))
 *     .withEnchant(Enchantment.DAMAGE_ALL, 5)
 *     .withFlag(ItemFlag.HIDE_ENCHANTS)
 *     .setUnbreakable(true)
 *     .build();
 * </pre>
 */
public class ItemBuilder {
    private ItemStack item;
    private ItemMeta meta;

    private ItemBuilder(Material material, int amount) {
        item = new ItemStack(material, amount);
        meta = item.getItemMeta();
    }

    /**
     * Creates a new ItemBuilder with the specified material and a default amount of 1.
     *
     * @param material
     *         the material for the item
     *
     * @return a new ItemBuilder instance
     */
    public static ItemBuilder of(Material material) {
        return of(material, 1);
    }

    /**
     * Creates a new ItemBuilder with the specified material and amount.
     *
     * @param material
     *         the material for the item
     * @param amount
     *         the amount for the item
     *
     * @return a new ItemBuilder instance
     */
    public static ItemBuilder of(Material material, int amount) {
        return new ItemBuilder(material, amount);
    }

    /**
     * Creates a new ItemBuilder from an existing ItemStack.
     *
     * @param item
     *         the ItemStack to copy
     *
     * @return a new ItemBuilder with the same material, amount, and meta as the provided item
     */
    public static ItemBuilder of(ItemStack item) {
        var builder = new ItemBuilder(item.getType(), item.getAmount());
        builder.item = item;
        builder.meta = item.getItemMeta();
        return builder;
    }

    /**
     * Creates a new ItemBuilder from a given CompoundTag.
     *
     * @param tag
     *         the CompoundTag to convert
     *
     * @return a new ItemBuilder created from the NBT data in the tag
     */
    public static ItemBuilder of(CompoundData tag) {
        var item = NBTItem.convertNBTtoItem(new NBTContainer(tag.toString()));
        var builder = new ItemBuilder(item.getType(), item.getAmount());
        builder.item = item;
        builder.meta = item.getItemMeta();
        return builder;
    }

    /**
     * Creates a new ItemBuilder for a player skull with the specified texture.
     *
     * @param texture
     *         the texture url to set
     *
     * @return a new ItemBuilder for a PLAYER_HEAD with the custom texture applied
     */
    public static ItemBuilder playerSkull(String texture) {
        var builder = new ItemBuilder(Material.PLAYER_HEAD, 1);
        builder.handleMeta(SkullMeta.class, meta -> {
            meta.setOwnerProfile(PlayerProfileHelper.setSkin(meta.getOwnerProfile(), texture));
            return meta;
        });
        return builder;
    }

    /**
     * Builds and returns a clone of the customized ItemStack.
     *
     * <p><b>Example:</b>
     * <pre>
     * ItemStack customItem = builder.build();
     * </pre>
     *
     * @return a clone of the final ItemStack with applied modifications
     */
    public ItemStack build() {
        item.setItemMeta(meta);
        return item.clone();
    }

    /**
     * Converts the current ItemStack into a CompoundTag representation.
     *
     * @return the CompoundTag representing the ItemStack's NBT data
     * @throws RuntimeException
     *         if an error occurs during conversion
     */
    public CompoundData toTag() {
        String json = NBTItem.convertItemtoNBT(item).toString();
        CompoundData compound = new CompoundData();
        try {
            compound = (CompoundData) NBTJSON.readFromJsonString(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return compound;
    }

    // --- DISPLAY NAME METHODS --- //

    /**
     * Sets the display name of the item.
     *
     * <p><b>Example:</b>
     * <pre>
     * builder.withName("&aEpic Sword");
     * </pre>
     *
     * @param name
     *         the display name to set (supports color codes)
     *
     * @return this ItemBuilder for chaining
     */
    public ItemBuilder withName(String name) {
        meta.setDisplayName(translate(name, false));
        return this;
    }

    /**
     * Retrieves the display name of the item. If no display name is set, returns a capitalized
     * version of the item's material name.
     *
     * <p><b>Example:</b>
     * <pre>
     * String name = builder.getName();
     * </pre>
     *
     * @return the display name if set; otherwise, a capitalized material name
     */
    public String getName() {
        if (meta.hasDisplayName()) return meta.getDisplayName();
        return WordUtils.capitalizeFully(item.getType().name().toLowerCase().replace("_", " "));
    }

    // --- ENCHANT METHODS --- //

    /**
     * Adds an unsafe enchantment to the item.
     *
     * <p><b>Example:</b>
     * <pre>
     * builder.withEnchant(Enchantment.DAMAGE_ALL, 5);
     * </pre>
     *
     * @param enchant
     *         the enchantment to add
     * @param level
     *         the level of the enchantment
     *
     * @return this ItemBuilder for chaining
     */
    public ItemBuilder withEnchant(Enchantment enchant, int level) {
        item.addUnsafeEnchantment(enchant, level);
        return this;
    }

    /**
     * Removes the specified enchantment from the item.
     *
     * <p><b>Example:</b>
     * <pre>
     * builder.removeEnchant(Enchantment.DAMAGE_ALL);
     * </pre>
     *
     * @param enchant
     *         the enchantment to remove
     *
     * @return this ItemBuilder for chaining
     */
    public ItemBuilder removeEnchant(Enchantment enchant) {
        item.removeEnchantment(enchant);
        return this;
    }

    /**
     * Retrieves a map of all enchantments on the item.
     *
     * <p><b>Example:</b>
     * <pre>
     * Map&lt;Enchantment, Integer&gt; enchants = builder.getEnchantments();
     * </pre>
     *
     * @return a map of enchantments to their levels
     */
    public Map<Enchantment, Integer> getEnchantments() {
        return meta.getEnchants();
    }

    // --- ITEMFLAG METHODS --- //

    /**
     * Adds an item flag to the item.
     *
     * <p><b>Example:</b>
     * <pre>
     * builder.withFlag(ItemFlag.HIDE_ENCHANTS);
     * </pre>
     *
     * @param flag
     *         the item flag to add
     *
     * @return this ItemBuilder for chaining
     */
    public ItemBuilder withFlag(ItemFlag flag) {
        meta.addItemFlags(flag);
        return this;
    }

    /**
     * Removes an item flag from the item.
     *
     * <p><b>Example:</b>
     * <pre>
     * builder.removeFlag(ItemFlag.HIDE_ENCHANTS);
     * </pre>
     *
     * @param flag
     *         the item flag to remove
     *
     * @return this ItemBuilder for chaining
     */
    public ItemBuilder removeFlag(ItemFlag flag) {
        meta.removeItemFlags(flag);
        return this;
    }

    /**
     * Retrieves all item flags present on the item.
     *
     * <p><b>Example:</b>
     * <pre>
     * Set&lt;ItemFlag&gt; flags = builder.getFlags();
     * </pre>
     *
     * @return a set of item flags
     */
    public Set<ItemFlag> getFlags() {
        return meta.getItemFlags();
    }

    // --- UNBREAKABLE METHODS --- //

    /**
     * Sets whether the item is unbreakable.
     *
     * <p><b>Example:</b>
     * <pre>
     * builder.setUnbreakable(true);
     * </pre>
     *
     * @param unbreakable
     *         true to mark the item as unbreakable; false otherwise
     *
     * @return this ItemBuilder for chaining
     */
    public ItemBuilder setUnbreakable(boolean unbreakable) {
        meta.setUnbreakable(unbreakable);
        return this;
    }

    /**
     * Checks if the item is unbreakable.
     *
     * <p><b>Example:</b>
     * <pre>
     * boolean unbreakable = builder.isUnbreakable();
     * </pre>
     *
     * @return true if the item is unbreakable; false otherwise
     */
    public boolean isUnbreakable() {
        return meta.isUnbreakable();
    }

    // --- LORE METHODS --- //

    /**
     * Sets the lore (list of text lines) of the item.
     *
     * <p><b>Example:</b>
     * <pre>
     * builder.withLore(Arrays.asList("&7This is a cool item", "&7Use it wisely"));
     * </pre>
     *
     * @param lore
     *         a list of lore lines (supports color codes)
     *
     * @return this ItemBuilder for chaining
     */
    public ItemBuilder withLore(List<String> lore) {
        meta.setLore(translate(lore, false));
        return this;
    }

    /**
     * Appends additional lore lines to the current lore.
     *
     * <p><b>Example:</b>
     * <pre>
     * builder.addLore("&7Extra lore line 1", "&7Extra lore line 2");
     * </pre>
     *
     * @param lore
     *         one or more lore lines (supports color codes)
     *
     * @return this ItemBuilder for chaining
     */
    public ItemBuilder addLore(String... lore) {
        List<String> itemLore = new ArrayList<>();

        if (meta.hasLore()) itemLore = meta.getLore();

        List<String> finalItemLore = itemLore;
        Arrays.asList(lore).forEach(s -> finalItemLore.add(translate(s, false)));
        meta.setLore(finalItemLore);
        return this;
    }

    /**
     * Clears all lore from the item.
     *
     * <p><b>Example:</b>
     * <pre>
     * builder.clearLore();
     * </pre>
     *
     * @return this ItemBuilder for chaining
     */
    public ItemBuilder clearLore() {
        if (meta.hasLore()) meta.getLore().clear();
        return this;
    }

    /**
     * Removes a specific lore line from the item.
     *
     * <p><b>Example:</b>
     * <pre>
     * builder.removeLore("&7Extra lore line");
     * </pre>
     *
     * @param lore
     *         the lore line to remove (supports color codes)
     *
     * @return this ItemBuilder for chaining
     */
    public ItemBuilder removeLore(String lore) {
        List<String> itemLore = new ArrayList<>();

        if (meta.hasLore()) itemLore = meta.getLore();

        itemLore.remove(translate(lore, false));
        meta.setLore(itemLore);
        return this;
    }

    // --- STATIC UTILITY METHODS --- //

    /**
     * Checks if the given material is considered "air".
     *
     * <p><b>Example:</b>
     * <pre>
     * boolean isAir = ItemBuilder.isAir(Material.AIR);
     * </pre>
     *
     * @param mat
     *         the material to check
     *
     * @return true if the material's name ends with "AIR" but not "AIRS"
     */
    public static boolean isAir(Material mat) {
        return mat.name().endsWith("AIR") && !mat.name().endsWith("AIRS");
    }

    /**
     * Checks if the given ItemStack is null or represents air.
     *
     * <p><b>Example:</b>
     * <pre>
     * boolean isAir = ItemBuilder.isAir(itemStack);
     * </pre>
     *
     * @param item
     *         the ItemStack to check
     *
     * @return true if the item is null or its material is considered air
     */
    public static boolean isAir(ItemStack item) {
        return item == null || isAir(item.getType());
    }

    /**
     * Applies a transformation to the {@link ItemMeta} if it matches the specified class.
     *
     * <p><b>Example:</b>
     * <pre>
     * builder.handleMeta(SkullMeta.class, meta -&gt; {
     *     // Modify meta as needed
     *     return meta;
     * });
     * </pre>
     *
     * @param clazz
     *         the meta class to transform
     * @param meta
     *         a function that transforms the meta
     * @param <T>
     *         the type of the meta
     *
     * @return this ItemBuilder for chaining
     */
    public <T extends ItemMeta> ItemBuilder handleMeta(Class<T> clazz, ItemMetaValue<T> meta) {
        if (!clazz.isAssignableFrom(this.meta.getClass())) return this;

        this.meta = meta.accept(clazz.cast(this.meta));
        item.setItemMeta(this.meta);
        return this;
    }

    // --- PRIVATE METHODS --- //

    private List<String> translate(List<String> message, boolean strip) {
        var newLore = new ArrayList<String>();

        message.forEach(msg -> {
            if (strip) {
                msg = msg.replace(ChatColor.COLOR_CHAR, '&');
                msg = Colorize.removeHexColor(msg);
            } else {
                msg = Colorize.translateBungeeHex(msg);
            }
            newLore.add(msg);
        });
        return newLore;
    }

    private String translate(String message, boolean strip) {
        if (strip) {
            message = message.replace(ChatColor.COLOR_CHAR, '&');
            message = Colorize.removeHexColor(message);
        } else {
            message = Colorize.translateBungeeHex(message);
        }
        return message;
    }

    /**
     * Functional interface for transforming an {@link ItemMeta} object.
     *
     * @param <T>
     *         the type of item meta
     */
    public interface ItemMetaValue<T> {
        T accept(T value);
    }
}

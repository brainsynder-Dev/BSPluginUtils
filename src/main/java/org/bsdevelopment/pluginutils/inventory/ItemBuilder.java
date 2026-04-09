package org.bsdevelopment.pluginutils.inventory;

import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bsdevelopment.nbt.StorageTagCompound;
import org.bsdevelopment.nbt.io.StorageStringParser;
import org.bsdevelopment.pluginutils.text.Colorize;
import org.bsdevelopment.pluginutils.text.WordUtils;
import org.bsdevelopment.pluginutils.utilities.PlayerProfileHelper;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.components.BlocksAttacksComponent;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.bukkit.inventory.meta.components.JukeboxPlayableComponent;
import org.bukkit.inventory.meta.components.ToolComponent;
import org.bukkit.inventory.meta.components.UseCooldownComponent;
import org.bukkit.inventory.meta.components.WeaponComponent;
import org.bukkit.inventory.meta.components.consumable.ConsumableComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Fluent builder for creating and customising {@link ItemStack} objects.
 *
 * <p>Supports all standard item properties plus the 1.21.6+ data component API:
 * food, tool, equippable, consumable, jukebox-playable, weapon, blocks-attacks,
 * use-cooldown, custom model data, enchantable value, tooltip style, and custom
 * max stack size.
 */
public class ItemBuilder {
    private ItemStack item;
    private ItemMeta meta;

    private ItemBuilder(Material material, int amount) {
        item = new ItemStack(material, amount);
        meta = item.getItemMeta();
    }

    public static ItemBuilder of(Material material) {
        return of(material, 1);
    }

    public static ItemBuilder of(Material material, int amount) {
        return new ItemBuilder(material, amount);
    }

    public static ItemBuilder of(ItemStack item) {
        var builder = new ItemBuilder(item.getType(), item.getAmount());
        builder.item = item;
        builder.meta = item.getItemMeta();
        return builder;
    }

    public static ItemBuilder of(StorageTagCompound tag) {
        var item = NBTItem.convertNBTtoItem(new NBTContainer(tag.toString()));
        var builder = new ItemBuilder(item.getType(), item.getAmount());
        builder.item = item;
        builder.meta = item.getItemMeta();
        return builder;
    }

    public static ItemBuilder playerSkull(String texture) {
        var builder = new ItemBuilder(Material.PLAYER_HEAD, 1);
        builder.handleMeta(SkullMeta.class, meta -> {
            meta.setOwnerProfile(PlayerProfileHelper.setSkin(meta.getOwnerProfile(), texture));
            return meta;
        });
        return builder;
    }

    public static boolean isAir(Material mat) {
        return mat.name().endsWith("AIR") && !mat.name().endsWith("AIRS");
    }

    public static boolean isAir(ItemStack item) {
        return item == null || isAir(item.getType());
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item.clone();
    }

    public StorageTagCompound toTag() {
        String json = NBTItem.convertItemtoNBT(item).toString();
        StorageTagCompound compound = new StorageTagCompound();
        try {
            compound = StorageStringParser.getTagFromJson(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return compound;
    }

    public ItemBuilder withName(String name) {
        meta.setDisplayName(translate(name, false));
        return this;
    }

    public String getName() {
        if (meta.hasDisplayName()) return meta.getDisplayName();
        return WordUtils.capitalizeFully(item.getType().name().toLowerCase().replace("_", " "));
    }

    public ItemBuilder withEnchant(Enchantment enchant, int level) {
        item.addUnsafeEnchantment(enchant, level);
        return this;
    }

    public ItemBuilder removeEnchant(Enchantment enchant) {
        item.removeEnchantment(enchant);
        return this;
    }

    public Map<Enchantment, Integer> getEnchantments() {
        return meta.getEnchants();
    }

    public ItemBuilder withFlag(ItemFlag flag) {
        meta.addItemFlags(flag);
        return this;
    }

    public ItemBuilder removeFlag(ItemFlag flag) {
        meta.removeItemFlags(flag);
        return this;
    }

    public Set<ItemFlag> getFlags() {
        return meta.getItemFlags();
    }

    public ItemBuilder setUnbreakable(boolean unbreakable) {
        meta.setUnbreakable(unbreakable);
        return this;
    }

    public boolean isUnbreakable() {
        return meta.isUnbreakable();
    }

    public ItemBuilder withLore(List<String> lore) {
        meta.setLore(translate(lore, false));
        return this;
    }

    public ItemBuilder addLore(String... lore) {
        List<String> itemLore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        Arrays.asList(lore).forEach(s -> itemLore.add(translate(s, false)));
        meta.setLore(itemLore);
        return this;
    }

    public ItemBuilder clearLore() {
        meta.setLore(new ArrayList<>());
        return this;
    }

    public ItemBuilder removeLore(String lore) {
        List<String> itemLore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        itemLore.remove(translate(lore, false));
        meta.setLore(itemLore);
        return this;
    }

    /**
     * Sets a simple integer custom model data value.
     *
     * @see #withCustomModelDataComponent(Consumer) for the full component API
     */
    public ItemBuilder withCustomModelData(int modelData) {
        meta.setCustomModelData(modelData);
        return this;
    }

    /**
     * Configures the {@link CustomModelDataComponent}, which supports floats, flags,
     * colors, and strings in addition to the legacy integer value.
     *
     * <pre>{@code
     * builder.withCustomModelDataComponent(cmd -> {
     *     cmd.setFloats(List.of(1.0f, 2.5f));
     *     cmd.setFlags(List.of(true));
     * });
     * }</pre>
     */
    public ItemBuilder withCustomModelDataComponent(Consumer<CustomModelDataComponent> consumer) {
        CustomModelDataComponent component = meta.getCustomModelDataComponent();
        consumer.accept(component);
        meta.setCustomModelDataComponent(component);
        return this;
    }

    /**
     * Controls whether any tooltip is shown for this item.
     */
    public ItemBuilder setHideTooltip(boolean hideTooltip) {
        meta.setHideTooltip(hideTooltip);
        return this;
    }

    /**
     * Sets a custom tooltip style applied when the item is hovered.
     * The key references a tooltip style defined in a resource pack.
     */
    public ItemBuilder withTooltipStyle(NamespacedKey tooltipStyle) {
        meta.setTooltipStyle(tooltipStyle);
        return this;
    }

    /**
     * Sets the custom max stack size for this item (1–99).
     * Overrides the material's default stack size.
     */
    public ItemBuilder withMaxStackSize(int maxStackSize) {
        meta.setMaxStackSize(maxStackSize);
        return this;
    }

    /**
     * Sets the enchantability value for this item, which influences the quality of
     * enchantments offered at an enchanting table. Higher values yield better results.
     */
    public ItemBuilder withEnchantable(int value) {
        meta.setEnchantable(value);
        return this;
    }

    /**
     * Removes the enchantable override, restoring the material's default enchantability.
     */
    public ItemBuilder clearEnchantable() {
        meta.setEnchantable(null);
        return this;
    }

    /**
     * Configures the {@link FoodComponent}.
     *
     * <pre>{@code
     * builder.withFood(food -> {
     *     food.setNutrition(4);
     *     food.setSaturation(0.3f);
     *     food.setCanAlwaysEat(true);
     * });
     * }</pre>
     */
    public ItemBuilder withFood(Consumer<FoodComponent> consumer) {
        FoodComponent food = meta.getFood();
        consumer.accept(food);
        meta.setFood(food);
        return this;
    }

    public ItemBuilder clearFood() {
        meta.setFood(null);
        return this;
    }

    /**
     * Configures the {@link ConsumableComponent}, controlling how the item is consumed.
     *
     * <pre>{@code
     * builder.withConsumable(c -> {
     *     c.setConsumeSeconds(2.0f);
     *     c.setAnimation(ConsumableComponent.Animation.DRINK);
     * });
     * }</pre>
     */
    public ItemBuilder withConsumable(Consumer<ConsumableComponent> consumer) {
        ConsumableComponent consumable = meta.getConsumable();
        consumer.accept(consumable);
        meta.setConsumable(consumable);
        return this;
    }

    public ItemBuilder clearConsumable() {
        meta.setConsumable(null);
        return this;
    }

    /**
     * Configures the {@link ToolComponent}, defining mining speed and damage per block.
     *
     * <pre>{@code
     * builder.withTool(tool -> {
     *     tool.setDefaultMiningSpeed(1.5f);
     *     tool.setDamagePerBlock(1);
     * });
     * }</pre>
     */
    public ItemBuilder withTool(Consumer<ToolComponent> consumer) {
        ToolComponent tool = meta.getTool();
        consumer.accept(tool);
        meta.setTool(tool);
        return this;
    }

    public ItemBuilder clearTool() {
        meta.setTool(null);
        return this;
    }

    /**
     * Configures the {@link WeaponComponent}, adjusting melee attack behavior.
     *
     * <pre>{@code
     * builder.withWeapon(weapon -> {
     *     weapon.setItemDamagePerAttack(1);
     *     weapon.setDisableBlockingForSeconds(0.4f);
     * });
     * }</pre>
     */
    public ItemBuilder withWeapon(Consumer<WeaponComponent> consumer) {
        WeaponComponent weapon = meta.getWeapon();
        consumer.accept(weapon);
        meta.setWeapon(weapon);
        return this;
    }

    public ItemBuilder clearWeapon() {
        meta.setWeapon(null);
        return this;
    }

    /**
     * Configures the {@link BlocksAttacksComponent}, allowing the item to function as a shield.
     *
     * <pre>{@code
     * builder.withBlocksAttacks(shield -> {
     *     shield.setBlockDelaySeconds(0.0f);
     *     shield.setDisableCooldownSeconds(1.0f);
     * });
     * }</pre>
     */
    public ItemBuilder withBlocksAttacks(Consumer<BlocksAttacksComponent> consumer) {
        BlocksAttacksComponent component = meta.getBlocksAttacks();
        consumer.accept(component);
        meta.setBlocksAttacks(component);
        return this;
    }

    public ItemBuilder clearBlocksAttacks() {
        meta.setBlocksAttacks(null);
        return this;
    }

    /**
     * Configures the {@link EquippableComponent}, allowing any item to be worn in an armor slot.
     *
     * <pre>{@code
     * builder.withEquippable(equip -> {
     *     equip.setSlot(EquipmentSlot.HEAD);
     * });
     * }</pre>
     */
    public ItemBuilder withEquippable(Consumer<EquippableComponent> consumer) {
        EquippableComponent equippable = meta.getEquippable();
        consumer.accept(equippable);
        meta.setEquippable(equippable);
        return this;
    }

    public ItemBuilder clearEquippable() {
        meta.setEquippable(null);
        return this;
    }

    /**
     * Configures the {@link UseCooldownComponent}, adding a use cooldown to the item.
     *
     * <pre>{@code
     * builder.withUseCooldown(cd -> {
     *     cd.setCooldownSeconds(1.5f);
     *     cd.setCooldownGroup(new NamespacedKey("myplugin", "special_item"));
     * });
     * }</pre>
     */
    public ItemBuilder withUseCooldown(Consumer<UseCooldownComponent> consumer) {
        UseCooldownComponent cooldown = meta.getUseCooldown();
        consumer.accept(cooldown);
        meta.setUseCooldown(cooldown);
        return this;
    }

    public ItemBuilder clearUseCooldown() {
        meta.setUseCooldown(null);
        return this;
    }

    /**
     * Configures the {@link JukeboxPlayableComponent}, allowing the item to be inserted into a jukebox.
     *
     * <pre>{@code
     * builder.withJukeboxPlayable(jukebox -> {
     *     jukebox.setSongKey(new NamespacedKey("minecraft", "music_disc.13"));
     * });
     * }</pre>
     */
    public ItemBuilder withJukeboxPlayable(Consumer<JukeboxPlayableComponent> consumer) {
        JukeboxPlayableComponent jukebox = meta.getJukeboxPlayable();
        consumer.accept(jukebox);
        meta.setJukeboxPlayable(jukebox);
        return this;
    }

    public ItemBuilder clearJukeboxPlayable() {
        meta.setJukeboxPlayable(null);
        return this;
    }

    /**
     * Applies a transformation to the {@link ItemMeta} if it matches the specified class.
     *
     * <pre>{@code
     * builder.handleMeta(SkullMeta.class, meta -> {
     *     meta.setOwnerProfile(profile);
     *     return meta;
     * });
     * }</pre>
     */
    public <T extends ItemMeta> ItemBuilder handleMeta(Class<T> clazz, ItemMetaValue<T> meta) {
        if (!clazz.isAssignableFrom(this.meta.getClass())) return this;
        this.meta = meta.accept(clazz.cast(this.meta));
        item.setItemMeta(this.meta);
        return this;
    }

    private List<String> translate(List<String> message, boolean strip) {
        var result = new ArrayList<String>();
        message.forEach(msg -> result.add(translate(msg, strip)));
        return result;
    }

    private String translate(String message, boolean strip) {
        if (strip) {
            message = message.replace(ChatColor.COLOR_CHAR, '&');
            return Colorize.removeHexColor(message);
        }
        return Colorize.translateBungeeHex(message);
    }

    public interface ItemMetaValue<T> {
        T accept(T value);
    }
}

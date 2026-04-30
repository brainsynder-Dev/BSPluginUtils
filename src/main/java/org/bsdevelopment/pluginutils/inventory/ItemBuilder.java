package org.bsdevelopment.pluginutils.inventory;

import com.eclipsesource.json.Json;
import org.bsdevelopment.nbt.StorageBase;
import org.bsdevelopment.nbt.StorageTagCompound;
import org.bsdevelopment.nbt.StorageTagList;
import org.bsdevelopment.nbt.StorageTagString;
import org.bsdevelopment.nbt.io.StorageStringParser;
import org.bsdevelopment.pluginutils.text.Colorize;
import org.bsdevelopment.pluginutils.text.WordUtils;
import org.bsdevelopment.pluginutils.utilities.NBTCodec;
import org.bsdevelopment.pluginutils.utilities.PlayerProfileHelper;
import org.bukkit.*;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.TropicalFish;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.inventory.meta.components.*;
import org.bukkit.inventory.meta.components.consumable.ConsumableComponent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.nio.charset.StandardCharsets;
import java.util.*;
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
    private final Map<String, String> replacements = new LinkedHashMap<>();

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
        if (tag.hasKey("material") && !tag.hasKey("id")) return fromLegacyCompound(tag);
        try {
            StorageTagCompound working = tag.copy();
            String extractedName = null;
            List<String> extractedLore = null;

            if (working.hasKey("components")) {
                StorageTagCompound components = working.getCompoundTag("components");

                if (components.hasKey("minecraft:custom_name")) {
                    StorageBase base = components.getTag("minecraft:custom_name");
                    if (base instanceof StorageTagString str && isPlainAmpersandString(str.getString())) {
                        extractedName = str.getString();
                        components.remove("minecraft:custom_name");
                    }
                }

                if (components.hasKey("minecraft:lore")) {
                    StorageBase base = components.getTag("minecraft:lore");
                    if (base instanceof StorageTagList list) {
                        List<StorageBase> entries = list.getTagList();
                        boolean allPlain = !entries.isEmpty();
                        for (StorageBase entry : entries) {
                            if (!(entry instanceof StorageTagString str) || !isPlainAmpersandString(str.getString())) {
                                allPlain = false;
                                break;
                            }
                        }
                        if (allPlain) {
                            extractedLore = new ArrayList<>(entries.size());
                            for (StorageBase entry : entries) extractedLore.add(((StorageTagString) entry).getString());
                            components.remove("minecraft:lore");
                        }
                    }
                }

                if (components.hasKey("minecraft:profile")) {
                    StorageBase base = components.getTag("minecraft:profile");
                    if (base instanceof StorageTagCompound profile) profile.remove("name");
                }
            }

            ItemStack parsed = NBTCodec.nbtStringToBukkit(working.toString());
            ItemMeta parsedMeta = parsed.getItemMeta();
            if (parsedMeta != null && (extractedName != null || extractedLore != null)) {
                if (extractedName != null) parsedMeta.setDisplayName(Colorize.translateBungeeHex(extractedName));
                if (extractedLore != null) {
                    List<String> lore = new ArrayList<>(extractedLore.size());
                    for (String line : extractedLore) lore.add(Colorize.translateBungeeHex(line));
                    parsedMeta.setLore(lore);
                }
                parsed.setItemMeta(parsedMeta);
            }

            var builder = new ItemBuilder(parsed.getType(), parsed.getAmount());
            builder.item = parsed;
            builder.meta = parsed.getItemMeta();
            return builder;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ItemBuilder fromLegacyCompound(StorageTagCompound compound) {
        Material material = Material.getMaterial(compound.getString("material"));
        if (material == null) material = Material.STONE;

        var builder = new ItemBuilder(material, compound.getInteger("amount", 1));

        if (compound.hasKey("name")) builder.withName(compound.getString("name"));

        if (compound.hasKey("lore")) {
            StorageTagList list = (StorageTagList) compound.getTag("lore");
            List<String> lore = new ArrayList<>(list.tagCount());
            list.getTagList().forEach(base -> lore.add(((StorageTagString) base).getString()));
            builder.withLore(lore);
        }

        if (compound.hasKey("meta")) applyLegacyMeta(builder, compound.getCompoundTag("meta"));

        if (compound.hasKey("enchants")) {
            StorageTagList enchants = (StorageTagList) compound.getTag("enchants");
            enchants.getTagList().forEach(base -> {
                StorageTagCompound enchant = (StorageTagCompound) base;
                Enchantment enchantment = Enchantment.getByName(enchant.getString("name"));
                if (enchantment != null) builder.withEnchant(enchantment, enchant.getInteger("level", 1));
            });
        }

        if (compound.hasKey("flags")) {
            StorageTagList flags = (StorageTagList) compound.getTag("flags");
            flags.getTagList().forEach(base -> {
                try {
                    builder.withFlag(ItemFlag.valueOf(((StorageTagString) base).getString()));
                } catch (IllegalArgumentException ignored) {}
            });
        }

        if (compound.hasKey("custom-model-data")) builder.withCustomModelData(compound.getInteger("custom-model-data"));
        else if (compound.hasKey("CustomModelData")) builder.withCustomModelData(compound.getInteger("CustomModelData"));

        if (compound.hasKey("unbreakable")) builder.setUnbreakable(compound.getBoolean("unbreakable"));

        return builder;
    }

    private static void applyLegacyMeta(ItemBuilder builder, StorageTagCompound metaCompound) {
        builder.handleMeta(SkullMeta.class, skullMeta -> {
            if (metaCompound.hasKey("owner")) skullMeta.setOwner(metaCompound.getString("owner"));
            if (metaCompound.hasKey("texture")) {
                String texture = decodeLegacyTexture(metaCompound.getString("texture"));
                if (texture != null) {
                    var profile = skullMeta.getOwnerProfile();
                    if (profile == null) profile = Bukkit.createPlayerProfile(UUID.randomUUID());
                    skullMeta.setOwnerProfile(PlayerProfileHelper.setSkin(profile, texture));
                }
            }
            return skullMeta;
        });

        builder.handleMeta(LeatherArmorMeta.class, armorMeta -> {
            if (metaCompound.hasKey("color")) armorMeta.setColor(metaCompound.getColor("color", Color.fromRGB(10511680)));
            return armorMeta;
        });

        builder.handleMeta(PotionMeta.class, potionMeta -> {
            if (metaCompound.hasKey("color")) potionMeta.setColor(metaCompound.getColor("color"));
            if (metaCompound.hasKey("effects")) {
                StorageTagList list = (StorageTagList) metaCompound.getTag("effects");
                list.getTagList().forEach(base -> {
                    StorageTagCompound effectCompound = (StorageTagCompound) base;
                    PotionEffectType type = PotionEffectType.getByName(effectCompound.getString("type", "SPEED"));
                    if (type == null) return;
                    potionMeta.addCustomEffect(new PotionEffect(
                        type,
                        effectCompound.getInteger("duration", 60),
                        effectCompound.getInteger("amplifier", 1),
                        effectCompound.getBoolean("ambient", true),
                        effectCompound.getBoolean("particles", true),
                        effectCompound.getBoolean("icon", true)
                    ), false);
                });
            }
            return potionMeta;
        });

        builder.handleMeta(BannerMeta.class, bannerMeta -> {
            if (metaCompound.hasKey("patterns")) {
                List<Pattern> patterns = new ArrayList<>();
                StorageTagList list = (StorageTagList) metaCompound.getTag("patterns");
                list.getTagList().forEach(base -> {
                    StorageTagCompound patternCompound = (StorageTagCompound) base;
                    try {
                        PatternType type = PatternType.valueOf(patternCompound.getString("type", "BASE"));
                        DyeColor color = DyeColor.valueOf(patternCompound.getString("color", "WHITE"));
                        patterns.add(new Pattern(color, type));
                    } catch (IllegalArgumentException ignored) {}
                });
                bannerMeta.setPatterns(patterns);
            }
            return bannerMeta;
        });

        builder.handleMeta(BookMeta.class, bookMeta -> {
            if (metaCompound.hasKey("title")) bookMeta.setTitle(Colorize.translateBungeeHex(metaCompound.getString("title")));
            if (metaCompound.hasKey("author")) bookMeta.setAuthor(metaCompound.getString("author"));
            if (metaCompound.hasKey("generation")) {
                try {
                    bookMeta.setGeneration(BookMeta.Generation.valueOf(metaCompound.getString("generation")));
                } catch (IllegalArgumentException ignored) {}
            }
            if (metaCompound.hasKey("pages")) {
                List<String> pages = new ArrayList<>();
                StorageTagList list = (StorageTagList) metaCompound.getTag("pages");
                list.getTagList().forEach(base -> pages.add(Colorize.translateBungeeHex(((StorageTagString) base).getString())));
                bookMeta.setPages(pages);
            }
            return bookMeta;
        });

        builder.handleMeta(FireworkMeta.class, fireworkMeta -> {
            if (metaCompound.hasKey("power")) fireworkMeta.setPower(metaCompound.getInteger("power"));
            if (metaCompound.hasKey("effects")) {
                StorageTagList list = (StorageTagList) metaCompound.getTag("effects");
                list.getTagList().forEach(base -> {
                    FireworkEffect effect = fromLegacyFireworkEffect((StorageTagCompound) base);
                    if (effect != null) fireworkMeta.addEffect(effect);
                });
            }
            return fireworkMeta;
        });

        builder.handleMeta(MapMeta.class, mapMeta -> {
            if (metaCompound.hasKey("color")) mapMeta.setColor(metaCompound.getColor("color"));
            if (metaCompound.hasKey("scaling")) mapMeta.setScaling(metaCompound.getBoolean("scaling"));
            if (metaCompound.hasKey("location-name")) mapMeta.setLocationName(metaCompound.getString("location-name"));
            return mapMeta;
        });

        builder.handleMeta(KnowledgeBookMeta.class, bookMeta -> {
            if (metaCompound.hasKey("recipes")) {
                List<NamespacedKey> recipes = new ArrayList<>();
                StorageTagList list = (StorageTagList) metaCompound.getTag("recipes");
                list.getTagList().forEach(base -> {
                    NamespacedKey key = NamespacedKey.fromString(((StorageTagString) base).getString());
                    if (key != null) recipes.add(key);
                });
                bookMeta.setRecipes(recipes);
            }
            return bookMeta;
        });

        builder.handleMeta(TropicalFishBucketMeta.class, fishMeta -> {
            if (metaCompound.hasKey("pattern-color")) {
                try { fishMeta.setPatternColor(DyeColor.valueOf(metaCompound.getString("pattern-color"))); } catch (IllegalArgumentException ignored) {}
            }
            if (metaCompound.hasKey("body-color")) {
                try { fishMeta.setBodyColor(DyeColor.valueOf(metaCompound.getString("body-color"))); } catch (IllegalArgumentException ignored) {}
            }
            if (metaCompound.hasKey("pattern")) {
                try { fishMeta.setPattern(TropicalFish.Pattern.valueOf(metaCompound.getString("pattern"))); } catch (IllegalArgumentException ignored) {}
            }
            return fishMeta;
        });
    }

    private static String decodeLegacyTexture(String texture) {
        if (texture == null || texture.isEmpty()) return null;
        if (texture.startsWith("http")) return texture;
        try {
            String decoded = new String(Base64.getDecoder().decode(texture), StandardCharsets.UTF_8);
            return Json.parse(decoded).asObject()
                .get("textures").asObject()
                .get("SKIN").asObject()
                .get("url").asString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static FireworkEffect fromLegacyFireworkEffect(StorageTagCompound compound) {
        try {
            FireworkEffect.Builder fxBuilder = FireworkEffect.builder();
            fxBuilder.with(FireworkEffect.Type.valueOf(compound.getString("type", "BALL")));
            if (compound.hasKey("trail")) fxBuilder.trail(compound.getBoolean("trail"));
            if (compound.hasKey("flicker")) fxBuilder.flicker(compound.getBoolean("flicker"));
            if (compound.hasKey("colors")) {
                List<Color> colors = new ArrayList<>();
                StorageTagList list = (StorageTagList) compound.getTag("colors");
                list.getTagList().forEach(base -> {
                    Color color = ((StorageTagString) base).getAsColor();
                    if (color != null) colors.add(color);
                });
                if (!colors.isEmpty()) fxBuilder.withColor(colors);
            }
            if (compound.hasKey("fade-colors")) {
                List<Color> colors = new ArrayList<>();
                StorageTagList list = (StorageTagList) compound.getTag("fade-colors");
                list.getTagList().forEach(base -> {
                    Color color = ((StorageTagString) base).getAsColor();
                    if (color != null) colors.add(color);
                });
                if (!colors.isEmpty()) fxBuilder.withFade(colors);
            }
            return fxBuilder.build();
        } catch (Exception ignored) {
            return null;
        }
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

    /**
     * Registers a placeholder and its replacement value.
     * All registered placeholders are applied to the item name and every lore line when {@link #build()} is called.
     *
     * <pre>{@code
     * builder.replaceString("{player}", player.getName())
     *        .replaceString("{kills}", killCount);
     * }</pre>
     *
     * @param placeholder the token to replace (e.g. {@code "{player}"})
     * @param replacement the value to substitute in; {@link Object#toString()} is called on it
     */
    public ItemBuilder replaceString(String placeholder, Object replacement) {
        replacements.put(placeholder, String.valueOf(replacement));
        return this;
    }

    public ItemStack build() {
        if (!replacements.isEmpty()) {
            if (meta.hasDisplayName()) {
                String name = meta.getDisplayName();
                for (var entry : replacements.entrySet()) name = name.replace(entry.getKey(), entry.getValue());
                meta.setDisplayName(name);
            }

            if (meta.hasLore()) {
                List<String> lore = new ArrayList<>(meta.getLore());
                for (int i = 0; i < lore.size(); i++) {
                    String line = lore.get(i);
                    for (var entry : replacements.entrySet()) line = line.replace(entry.getKey(), entry.getValue());
                    lore.set(i, line);
                }
                meta.setLore(lore);
            }
        }

        item.setItemMeta(meta);
        return item.clone();
    }

    public StorageTagCompound toTag() {
        item.setItemMeta(meta);
        try {
            StorageTagCompound tag = StorageStringParser.getTagFromJson(NBTCodec.bukkitToNbtString(item));
            StorageTagCompound components = tag.hasKey("components") ? tag.getCompoundTag("components") : null;
            if (components != null) {
                if (meta.hasDisplayName()) {
                    components.remove("minecraft:custom_name");
                    components.setString("minecraft:custom_name", toAmpersand(meta.getDisplayName()));
                }
                if (meta.hasLore()) {
                    components.remove("minecraft:lore");
                    StorageTagList loreList = new StorageTagList();
                    for (String line : meta.getLore()) loreList.appendTag(new StorageTagString(toAmpersand(line)));
                    components.setTag("minecraft:lore", loreList);
                }
                if (components.hasKey("minecraft:profile")) {
                    StorageBase base = components.getTag("minecraft:profile");
                    if (base instanceof StorageTagCompound profile) profile.remove("name").remove("id");
                }
            }
            return tag;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ItemBuilder setAmount(int amount) {
        item.setAmount(amount);
        return this;
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

    public ItemBuilder withCustomModelData(int modelData) {
        meta.setCustomModelData(modelData);
        return this;
    }

    public ItemBuilder withCustomModelDataComponent(Consumer<CustomModelDataComponent> consumer) {
        CustomModelDataComponent component = meta.getCustomModelDataComponent();
        consumer.accept(component);
        meta.setCustomModelDataComponent(component);
        return this;
    }

    public ItemBuilder setHideTooltip(boolean hideTooltip) {
        meta.setHideTooltip(hideTooltip);
        return this;
    }

    public ItemBuilder withTooltipStyle(NamespacedKey tooltipStyle) {
        meta.setTooltipStyle(tooltipStyle);
        return this;
    }

    public ItemBuilder withMaxStackSize(int maxStackSize) {
        meta.setMaxStackSize(maxStackSize);
        return this;
    }

    public ItemBuilder withEnchantable(int value) {
        meta.setEnchantable(value);
        return this;
    }

    public ItemBuilder clearEnchantable() {
        meta.setEnchantable(null);
        return this;
    }

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

    public <T extends ItemMeta> ItemBuilder handleMeta(Class<T> clazz, ItemMetaValue<T> meta) {
        if (!clazz.isAssignableFrom(this.meta.getClass())) return this;
        this.meta = meta.accept(clazz.cast(this.meta));
        item.setItemMeta(this.meta);
        return this;
    }

    private static boolean isPlainAmpersandString(String s) {
        if (s == null || s.isEmpty()) return true;
        char c = s.charAt(0);
        return c != '{' && c != '[';
    }

    private static String toAmpersand(String s) {
        if (s == null) return "";
        return Colorize.removeHexColor(s);
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

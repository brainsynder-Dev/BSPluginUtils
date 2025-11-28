/*
 * Copyright © 2025
 * BSDevelopment <https://bsdevelopment.org>
 */

package org.bsdevelopment.pluginutils.sound;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wraps a Bukkit {@link Sound} name with an optional fallback.
 * <p>
 * Resolution is lazy and cached:
 * <ul>
 *     <li>Try primary name via {@link Sound#valueOf(String)}</li>
 *     <li>If not found, try fallback name</li>
 *     <li>If neither exists for this server version, returns {@code null}</li>
 * </ul>
 */
public final class SafeSound {
    private static final Map<String, Sound> CACHE = new ConcurrentHashMap<>();

    private final String primaryName;
    private final @Nullable String fallbackName;

    private SafeSound(@NotNull String primaryName, @Nullable String fallbackName) {
        this.primaryName = primaryName;
        this.fallbackName = (fallbackName == null || fallbackName.isEmpty()) ? null : fallbackName;
    }

    /**
     * Creates a wrapper for a sound name with no fallback.
     */
    public static @NotNull SafeSound of(@NotNull String primaryName) {
        return new SafeSound(primaryName, null);
    }

    /**
     * Creates a wrapper for a primary sound name with a fallback sound name.
     */
    public static @NotNull SafeSound of(@NotNull String primaryName, @Nullable String fallbackName) {
        return new SafeSound(primaryName, fallbackName);
    }

    /**
     * Creates a wrapper that always resolves to the given {@link Sound}.
     */
    public static @NotNull SafeSound of(@NotNull Sound sound) {
        return new SafeSound(sound.getKey().toString(), null);
    }

    /**
     * Resolves the effective {@link Sound} for this wrapper.
     *
     * @return resolved Sound, or {@code null} if neither primary nor fallback exists
     */
    public @Nullable Sound resolve() {
        String cacheKey = primaryName + "|" + (fallbackName == null ? "" : fallbackName);
        return CACHE.computeIfAbsent(cacheKey, key -> compute());
    }

    private @Nullable Sound compute() {
        Sound primary = resolveName(primaryName);
        if (primary != null) return primary;

        if (fallbackName != null) {
            Sound fallback = resolveName(fallbackName);
            if (fallback != null) return fallback;
        }

        Bukkit.getLogger().fine("[BSPluginUtils] SafeSound could not resolve primary='" + primaryName + "', fallback='" + fallbackName + "'");
        return null;
    }

    private @Nullable Sound resolveName(@NotNull String name) {
        try {
            if (name.contains(":")) {
                return Registry.SOUNDS.get(NamespacedKey.fromString(name));
            }else {
                return Sound.valueOf(name);
            }
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * @return {@code true} if this resolves to a valid {@link Sound} on this server
     */
    public boolean isAvailable() {
        return resolve() != null;
    }

    /**
     * Plays this sound to a player if it can be resolved.
     */
    public void playTo(@NotNull Player player, float volume, float pitch) {
        Sound sound = resolve();
        if (sound != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    /**
     * Plays this sound at a location if it can be resolved.
     */
    public void playAt(@NotNull Location location, float volume, float pitch) {
        Sound sound = resolve();
        if (sound != null) {
            location.getWorld().playSound(location, sound, volume, pitch);
        }
    }

    public @NotNull String primaryName() {
        return primaryName;
    }

    public @Nullable String fallbackName() {
        return fallbackName;
    }
}

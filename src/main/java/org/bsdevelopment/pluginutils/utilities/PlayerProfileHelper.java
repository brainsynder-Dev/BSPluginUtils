package org.bsdevelopment.pluginutils.utilities;

import org.bukkit.Bukkit;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlayerProfileHelper {
    private static final Logger LOGGER = Logger.getLogger(PlayerProfileHelper.class.getName());
    private static final String TEXTURE_BASE_URL = "http://textures.minecraft.net/texture/";

    private static @Nullable URL createUrl(@Nullable String urlString) {
        if (urlString == null || !urlString.startsWith(TEXTURE_BASE_URL)) return null;

        try {
            // Use URI.create() to parse the string and then convert to URL.
            return URI.create(urlString).toURL();
        } catch (MalformedURLException e) {
            LOGGER.log(Level.WARNING, "Malformed URL: " + urlString, e);
            return null;
        }
    }

    /**
     * Generate a 16-character, hex-only name by hashing the given seed string
     * (or a random UUID, if seed is null).
     */
    private static String generateProfileName(@Nullable String seed) {
        String base = seed != null ? seed : UUID.randomUUID().toString();
        String hex = UUID.nameUUIDFromBytes(base.getBytes(StandardCharsets.UTF_8))
                .toString()
                .replace("-", "");
        // Take first 16 hex chars for a valid Minecraft name length
        return hex.substring(0, 16);
    }

    /**
     * If the passed profile is null, create a new one using a 16-char name
     * derived from the seed; otherwise return the existing profile.
     */
    private static PlayerProfile getOrCreateProfile(@Nullable PlayerProfile profile, @Nullable String seed) {
        if (profile != null) return profile;

        String name = generateProfileName(seed);
        return Bukkit.createPlayerProfile(null, name);
    }

    /**
     * Sets the skin on the provided PlayerProfile using the given URL string.
     * Clears any previous texture data before applying the new skin.
     *
     * @param profile
     *         The PlayerProfile to update (must not be null).
     * @param skinUrl
     *         The URL string of the new skin.
     *
     * @return The updated PlayerProfile.
     */
    public static @NotNull PlayerProfile setSkin(@NotNull PlayerProfile profile, @Nullable String skinUrl) {
        PlayerProfile effective = getOrCreateProfile(profile, skinUrl);
        var url = createUrl(skinUrl);
        if (url == null) return effective;

        var textures = effective.getTextures();
        textures.clear();
        textures.setSkin(url);
        effective.setTextures(textures);
        return effective;
    }

    /**
     * Sets the skin on the provided PlayerProfile using the given URL string and SkinModel.
     * Clears any previous texture data before applying the new skin.
     *
     * @param profile
     *         The PlayerProfile to update (must not be null).
     * @param skinUrl
     *         The URL string of the new skin.
     * @param skinModel
     *         The SkinModel to use (e.g. CLASSIC or SLIM).
     *
     * @return The updated PlayerProfile.
     */
    public static @NotNull PlayerProfile setSkin(@NotNull PlayerProfile profile, @Nullable String skinUrl, @Nullable PlayerTextures.SkinModel skinModel) {
        PlayerProfile effective = getOrCreateProfile(profile, skinUrl);
        var url = createUrl(skinUrl);
        if (url == null) return effective;

        var textures = effective.getTextures();
        textures.clear();
        textures.setSkin(url, skinModel);
        effective.setTextures(textures);
        return effective;
    }

    /**
     * Sets the cape on the provided PlayerProfile using the given URL string.
     *
     * @param profile
     *         The PlayerProfile to update (must not be null).
     * @param capeUrl
     *         The URL string of the new cape.
     *
     * @return The updated PlayerProfile.
     */
    public static @NotNull PlayerProfile setCape(@NotNull PlayerProfile profile, @Nullable String capeUrl) {
        PlayerProfile effective = getOrCreateProfile(profile, capeUrl);
        var url = createUrl(capeUrl);
        if (url == null) return effective;

        var textures = effective.getTextures();
        textures.setCape(url);
        effective.setTextures(textures);
        return effective;
    }

    /**
     * Clears all texture data (skin and cape) from the provided PlayerProfile.
     *
     * @param profile
     *         The PlayerProfile whose textures will be cleared (must not be null).
     *
     * @return The updated PlayerProfile.
     */
    public static @NotNull PlayerProfile clearTextures(@NotNull PlayerProfile profile) {
        PlayerProfile effective = getOrCreateProfile(profile, null);
        var textures = effective.getTextures();
        textures.clear();
        effective.setTextures(textures);
        return effective;
    }
}

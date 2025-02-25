package org.bsdevelopment.pluginutils.utilities;

import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
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
     * Sets the skin on the provided PlayerProfile using the given URL string.
     * Clears any previous texture data before applying the new skin.
     *
     * @param profile The PlayerProfile to update (must not be null).
     * @param skinUrl The URL string of the new skin.
     * @return The updated PlayerProfile.
     */
    public static @NotNull PlayerProfile setSkin(@NotNull PlayerProfile profile, @Nullable String skinUrl) {
        var url = createUrl(skinUrl);
        if (url == null) return profile;

        var textures = profile.getTextures();
        textures.clear();
        textures.setSkin(url);
        profile.setTextures(textures);
        return profile;
    }

    /**
     * Sets the skin on the provided PlayerProfile using the given URL string and SkinModel.
     * Clears any previous texture data before applying the new skin.
     *
     * @param profile   The PlayerProfile to update (must not be null).
     * @param skinUrl   The URL string of the new skin.
     * @param skinModel The SkinModel to use (e.g. CLASSIC or SLIM).
     * @return The updated PlayerProfile.
     */
    public static @NotNull PlayerProfile setSkin(@NotNull PlayerProfile profile, @Nullable String skinUrl, @Nullable PlayerTextures.SkinModel skinModel) {
        var url = createUrl(skinUrl);
        if (url == null) return profile;

        var textures = profile.getTextures();
        textures.clear();
        textures.setSkin(url, skinModel);
        profile.setTextures(textures);
        return profile;
    }

    /**
     * Sets the cape on the provided PlayerProfile using the given URL string.
     *
     * @param profile The PlayerProfile to update (must not be null).
     * @param capeUrl The URL string of the new cape.
     * @return The updated PlayerProfile.
     */
    public static @NotNull PlayerProfile setCape(@NotNull PlayerProfile profile, @Nullable String capeUrl) {
        var url = createUrl(capeUrl);
        if (url == null) return profile;

        var textures = profile.getTextures();
        textures.setCape(url);
        profile.setTextures(textures);
        return profile;
    }

    /**
     * Clears all texture data (skin and cape) from the provided PlayerProfile.
     *
     * @param profile The PlayerProfile whose textures will be cleared (must not be null).
     * @return The updated PlayerProfile.
     */
    public static @NotNull PlayerProfile clearTextures(@NotNull PlayerProfile profile) {
        var textures = profile.getTextures();
        textures.clear();
        profile.setTextures(textures);
        return profile;
    }
}

package org.bsdevelopment.pluginutils;

import io.papermc.lib.PaperLib;
import org.bsdevelopment.pluginutils.reflection.Reflection;
import org.bsdevelopment.pluginutils.text.AdvString;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.regex.Pattern;

/**
 * Gathers and exposes server-related information such as Java version,
 * Bukkit version, Minecraft version, and more.
 *
 * <p><b>Example Usage:</b>
 * <pre>
 * public class MyPlugin extends JavaPlugin {
 *     &#64;Override
 *     public void onEnable() {
 *         ServerInformation info = new ServerInformation(this, getClassLoader());
 *         getLogger().info("Java Version: " + info.getJavaVersion());
 *         getLogger().info("Server Type: " + info.getServerType());
 *         getLogger().info("Is Mojang Mapped: " + info.isMojangMapped());
 *     }
 * }
 * </pre>
 */
public class ServerInformation {

    /**
     * The Java version in use (e.g., "17").
     */
    private final String javaVersion;

    /**
     * The raw version string from {@link Bukkit#getVersion()}.
     */
    private final String rawVersion;

    /**
     * The Bukkit version string (e.g., "1.20.1-R0.1-SNAPSHOT").
     */
    private final String bukkitVersion;

    /**
     * The Minecraft version extracted from the raw version string (e.g., "1.20.1").
     */
    private final String minecraftVersion;

    /**
     * True if the server is running Paper, false if it's likely Spigot.
     */
    private final boolean paperServer;

    /**
     * A string representation of the server type (e.g., "Spigot", "Paper", etc.).
     */
    private String serverType = "Unknown";

    /**
     * A string representing the build version (often parsed from the raw version).
     */
    private String buildVersion = "Unknown";

    /**
     * Whether this server is using Mojang Mappings (true) or Obfuscated Mappings (false).
     */
    private boolean mojangMapped = false;

    /**
     * Constructs a new {@code ServerInformation} object, immediately collecting
     * details such as Java version, server type, and version strings.
     *
     * <p><b>Example:</b>
     * <pre>
     * public class MyPlugin extends JavaPlugin {
     *     &#64;Override
     *     public void onEnable() {
     *         ServerInformation info = new ServerInformation(this, getClassLoader());
     *         getLogger().info("Java Version: " + info.getJavaVersion());
     *         getLogger().info("Server Type: " + info.getServerType());
     *         getLogger().info("Is Mojang Mapped: " + info.isMojangMapped());
     *     }
     * }
     * </pre>
     *
     * @param plugin      the plugin instance
     * @param classLoader the class loader
     */
    public ServerInformation(Plugin plugin, ClassLoader classLoader) {
        paperServer = PaperLib.isPaper();

        // Fetch Java version
        var fullJavaVersion = System.getProperty("java.version");
        var pos = fullJavaVersion.indexOf('.');
        pos = fullJavaVersion.indexOf('.', pos + 1);

        if (pos != -1) javaVersion = fullJavaVersion.substring(0, pos).replace(".0", "");
        else javaVersion = fullJavaVersion;

        rawVersion = Bukkit.getVersion();
        bukkitVersion = Bukkit.getBukkitVersion();

        if (paperServer) {
            try {
                var buildInfoClass = Class.forName("io.papermc.paper.ServerBuildInfo");
                var buildInfoMethod = Reflection.resolveMethod(buildInfoClass, "buildInfo");
                var instance = Reflection.executeMethod(buildInfoMethod, null);

                serverType = (String) Reflection.executeMethod(
                        Reflection.resolveMethod(buildInfoClass, "brandName"),
                        instance
                );
                buildVersion = AdvString.between("-", "-", rawVersion);
            } catch (Exception ex) {
                var pattern = Pattern.compile("git-(\\w+)-(\\w+) \\(MC: (\\w.+)\\)");
                var matcher = pattern.matcher(rawVersion);
                if (matcher.find()) {
                    serverType = matcher.group(1);
                    buildVersion = matcher.group(2);
                } else {
                    serverType = rawVersion;
                    buildVersion = "Unknown";
                }
            }
        } else {
            serverType = "Spigot";
            buildVersion = AdvString.before("-", rawVersion);
        }

        minecraftVersion = AdvString.between("(MC: ", ")", rawVersion);

        try {
            var livingClass = Class.forName(
                    "net,minecraft,core,registries,BuiltInRegistries".replace(",", "."),
                    false,
                    classLoader
            );
            var field = livingClass.getDeclaredField("ENTITY_TYPE");
            if (field != null) {
                mojangMapped = true;
                plugin.getLogger().info("Plugin is on a server that is using Mojang Mappings");
            }
        } catch (Exception ex) {
            plugin.getLogger().info("Plugin is on a server that is using Obfuscated Mappings");
        }
    }

    /**
     * Retrieves the Java version in use (e.g., "17" or "1.8").
     *
     * @return the Java version
     */
    public String getJavaVersion() {
        return javaVersion;
    }

    /**
     * Retrieves the build version string extracted from the raw version.
     * For Paper servers, this may be the build number. May be "Unknown" if parsing fails.
     *
     * @return the build version string
     */
    public String getBuildVersion() {
        return buildVersion;
    }

    /**
     * Retrieves the Bukkit version (e.g., "1.20.1-R0.1-SNAPSHOT").
     *
     * @return the Bukkit version string
     */
    public String getBukkitVersion() {
        return bukkitVersion;
    }

    /**
     * Retrieves the Minecraft version (e.g., "1.20.1") found in the raw version string.
     *
     * @return the Minecraft version string
     */
    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    /**
     * Retrieves the raw version string from {@link Bukkit#getVersion()}.
     *
     * @return the raw version string
     */
    public String getRawVersion() {
        return rawVersion;
    }

    /**
     * Retrieves the server type (e.g., "Paper", "Spigot", or "Unknown").
     *
     * @return the server type
     */
    public String getServerType() {
        return serverType;
    }

    /**
     * Checks if this server is using Mojang Mappings. Returns true if found, false otherwise.
     *
     * @return true if mojang mapped, false if obfuscated or unknown
     */
    public boolean isMojangMapped() {
        return mojangMapped;
    }

    /**
     * Checks if this server is Paper. If false, it's likely Spigot or another fork.
     *
     * @return true if it's Paper, false otherwise
     */
    public boolean isPaper() {
        return paperServer;
    }
}

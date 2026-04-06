package org.bsdevelopment.pluginutils.version;

import io.papermc.lib.PaperLib;
import org.bsdevelopment.pluginutils.text.AdvString;
import org.bsdevelopment.pluginutils.utilities.Triple;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A class representing the server version.
 *
 * <p>This class provides methods to retrieve and compare server versions based on
 * major, minor, and patch numbers. It also registers known versions for easy access.
 *
 * <p><b>Examples:</b>
 * <pre>
 * // Retrieve the current server version
 * ServerVersion current = ServerVersion.getVersion();
 *
 * // Check if the current version is equal to or newer than a specific version
 * boolean isNewer = current.isEqualOrNewer(ServerVersion.v1_21);
 *
 * // Compare two versions
 * boolean isStrictlyOlder = ServerVersion.v1_21.isStrictlyOlder(ServerVersion.v1_21_2);
 * </pre>
 */
public class ServerVersion {

    private static final Map<Triple<Integer, Integer, Integer>, ServerVersion> VERSION_MAP = new HashMap<>();
    private static final Set<ServerVersion> VERSIONS = new HashSet<>();

    private static ServerVersion CURRENT_VERSION = null;

    public static ServerVersion v1_21 = register(Triple.of(1, 21, 0), "v1_21_R1");
    public static ServerVersion v1_21_1 = register(Triple.of(1, 21, 1), v1_21);
    public static ServerVersion v1_21_2 = register(Triple.of(1, 21, 2), "v1_21_R2");
    public static ServerVersion v1_21_3 = register(Triple.of(1, 21, 3), v1_21_2);
    public static ServerVersion v1_21_4 = register(Triple.of(1, 21, 4), "v1_21_R3");
    public static ServerVersion v1_21_5 = register(Triple.of(1, 21, 5), "v1_21_R4");
    public static ServerVersion v1_21_6 = register(Triple.of(1, 21, 6), "v1_21_R5");
    public static ServerVersion v1_21_7 = register(Triple.of(1, 21, 7), v1_21_6);
    public static ServerVersion v1_21_8 = register(Triple.of(1, 21, 8), v1_21_6);
    public static ServerVersion v1_21_9 = register(Triple.of(1, 21, 9), "v1_21_R6");
    public static ServerVersion v1_21_10 = register(Triple.of(1, 21, 10), v1_21_9);
    public static ServerVersion v1_21_11 = register(Triple.of(1, 21, 11), "v1_21_R7");
    public static ServerVersion v26_1 = register(Triple.of(26, 1, 0));
    public static ServerVersion v26_1_1 = register(Triple.of(26, 1, 1));
    // ---- AUTOMATION: END ---- //

    /**
     * Returns the current server version, resolving and caching it on first call.
     *
     * <p><b>Example:</b>
     * <pre>
     * ServerVersion current = ServerVersion.getVersion();
     * </pre>
     *
     * @return the current ServerVersion
     */
    public static ServerVersion getVersion() {
        if (CURRENT_VERSION != null) return CURRENT_VERSION;

        String mc = AdvString.between("MC: ", ")", Bukkit.getVersion());
        String[] versionParts = mc.split("\\.");

        // (e.g. "MC: 26.1"), so fall back to getBukkitVersion() to get the patch number
        // (e.g. "26.1.1-R0.1-SNAPSHOT" or "26.1.1.build.28-alpha").
        if (versionParts.length < 3) {
            String[] bukkitParts = Bukkit.getBukkitVersion().split("-")[0].split("\\.");
            if (bukkitParts.length >= 3) versionParts = bukkitParts;
        }

        int major = 0, minor = 0, patch = 0;
        try {
            if (versionParts.length >= 1) major = Integer.parseInt(versionParts[0]);
            if (versionParts.length >= 2) minor = Integer.parseInt(versionParts[1]);
            if (versionParts.length >= 3) patch = Integer.parseInt(versionParts[2]);
        } catch (NumberFormatException ignored) {}

        String mcVersion = String.format("v%d_%d_%d", major, minor, patch);

        Triple<Integer, Integer, Integer> triple = Triple.of(major, minor, patch);

        if (VERSION_MAP.containsKey(triple)) return VERSION_MAP.get(triple);

        for (ServerVersion version : VERSIONS) {
            if (version.versionName.equalsIgnoreCase(mcVersion)) {
                CURRENT_VERSION = version;
                return version;
            }
        }

        // No version was found, register the current version to have access to it.
        return CURRENT_VERSION = register(triple, PaperLib.isPaper() ? ""
                : Bukkit.getServer().getClass().getPackage().getName().substring(23));
    }

    /**
     * Returns all registered server versions.
     *
     * @return an unmodifiable view of all {@link ServerVersion} instances
     */
    public static Set<ServerVersion> getVersions() {
        return VERSIONS;
    }

    /**
     * Looks up or creates a {@link ServerVersion} for the given version triple without
     * affecting the cached current version.
     *
     * @param triple the (major, minor, patch) triple
     * @return the matching or newly registered {@link ServerVersion}
     */
    public static ServerVersion of(Triple<Integer, Integer, Integer> triple) {
        if (VERSION_MAP.containsKey(triple)) return VERSION_MAP.get(triple);

        String versionName = (triple.getRight() == 0)
                ? String.format("v%d_%d", triple.getLeft(), triple.getMiddle())
                : String.format("v%d_%d_%d", triple.getLeft(), triple.getMiddle(), triple.getRight());

        for (ServerVersion version : VERSIONS) {
            if (version.versionName.equalsIgnoreCase(versionName)) return version;
        }

        if (getVersion().version.equals(triple)) return getVersion();

        return register(triple, "");
    }

    private final Triple<Integer, Integer, Integer> version;
    private final String spigotNMS;
    private final String versionName;

    private ServerVersion(Triple<Integer, Integer, Integer> version, ServerVersion parentVersion) {
        this(version, parentVersion.spigotNMS);
    }

    private ServerVersion(Triple<Integer, Integer, Integer> version, String spigotNMS) {
        this.version = version;
        this.spigotNMS = spigotNMS;
        this.versionName = (version.right == 0)
                ? String.format("v%d_%d", version.left, version.middle)
                : String.format("v%d_%d_%d", version.left, version.middle, version.right);
    }

    /**
     * @return the (major, minor, patch) triple for this version
     */
    public Triple<Integer, Integer, Integer> getVersionNumbers() {
        return version;
    }

    /**
     * @return the version name string, e.g. {@code v1_21} or {@code v1_21_4}
     */
    public String getVersionName() {
        return versionName;
    }

    /**
     * @return the Spigot NMS package suffix, e.g. {@code v1_21_R1}, or empty on Paper
     */
    public String getSpigotNMS() {
        return spigotNMS;
    }

    /**
     * @param version version to compare against
     * @return {@code true} if this version &gt;= the given version
     */
    public boolean isEqualOrNewer(ServerVersion version) {
        return compareVersion(this.version, version.getVersionNumbers()) >= 0;
    }

    /**
     * @param version version to compare against
     * @return {@code true} if this version &gt; the given version
     */
    public boolean isStrictlyNewer(ServerVersion version) {
        return compareVersion(this.version, version.getVersionNumbers()) > 0;
    }

    /**
     * @param version version to compare against
     * @return {@code true} if this version &lt;= the given version
     */
    public boolean isEqualOrOlder(ServerVersion version) {
        return compareVersion(this.version, version.getVersionNumbers()) <= 0;
    }

    /**
     * @param version version to compare against
     * @return {@code true} if this version &lt; the given version
     */
    public boolean isStrictlyOlder(ServerVersion version) {
        return compareVersion(this.version, version.getVersionNumbers()) < 0;
    }

    /**
     * @param version version to compare against
     * @return {@code true} if both versions share the same major, minor, and patch numbers
     */
    public boolean isSameVersion(ServerVersion version) {
        Triple<Integer, Integer, Integer> compare = version.getVersionNumbers();
        return this.version.left.equals(compare.left)
                && this.version.middle.equals(compare.middle)
                && this.version.right.equals(compare.right);
    }

    private static int compareVersion(Triple<Integer, Integer, Integer> a, Triple<Integer, Integer, Integer> b) {
        int majorCmp = Integer.compare(a.getLeft(), b.getLeft());
        if (majorCmp != 0) return majorCmp;

        int minorCmp = Integer.compare(a.getMiddle(), b.getMiddle());
        if (minorCmp != 0) return minorCmp;

        return Integer.compare(a.getRight(), b.getRight());
    }

    private static ServerVersion register(Triple<Integer, Integer, Integer> version, ServerVersion parentVersion) {
        ServerVersion serverVersion = new ServerVersion(version, parentVersion);
        VERSIONS.add(serverVersion);
        VERSION_MAP.put(version, serverVersion);
        return serverVersion;
    }

    private static ServerVersion register(Triple<Integer, Integer, Integer> version, String spigotNMS) {
        ServerVersion serverVersion = new ServerVersion(version, spigotNMS);
        VERSIONS.add(serverVersion);
        VERSION_MAP.put(version, serverVersion);
        return serverVersion;
    }

    private static ServerVersion register(Triple<Integer, Integer, Integer> version) {
        ServerVersion serverVersion = new ServerVersion(version, "");
        VERSIONS.add(serverVersion);
        VERSION_MAP.put(version, serverVersion);
        return serverVersion;
    }
}

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
    // ---- AUTOMATION: END ---- //

    /**
     * Retrieves the current server version.
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
        String mcVersion = "v" + mc.replace(".", "_");

        String[] args = mc.split("\\.");
        int[] ints = new int[] {0, 0, 0};

        if (args.length >= 1) ints[0] = Integer.parseInt(args[0]);
        if (args.length >= 2) ints[1] = Integer.parseInt(args[1]);
        if (args.length >= 3) ints[2] = Integer.parseInt(args[2]);

        Triple<Integer, Integer, Integer> triple = Triple.of(ints[0], ints[1], ints[2]);

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
     * Retrieves a ServerVersion based on the provided version triple.
     *
     * <p><b>Example:</b>
     * <pre>
     * ServerVersion version = ServerVersion.of(Triple.of(1, 21, 0));
     * </pre>
     *
     * @param triple the triple representing the version (major, minor, patch)
     * @return the corresponding ServerVersion
     */
    public static ServerVersion of(Triple<Integer, Integer, Integer> triple) {
        if (VERSION_MAP.containsKey(triple)) return VERSION_MAP.get(triple);

        String versionName = (triple.right == 0)
                ? String.format("v%d_%d", triple.left, triple.middle)
                : String.format("v%d_%d_%d", triple.left, triple.middle, triple.right);

        for (ServerVersion version : VERSIONS) {
            if (version.versionName.equalsIgnoreCase(versionName)) {
                CURRENT_VERSION = version;
                return version;
            }
        }

        if (getVersion().version == triple) return getVersion();

        // No version was found, register the current version to have access to it.
        return CURRENT_VERSION = register(triple, "");
    }

    private Triple<Integer, Integer, Integer> version;
    private String spigotNMS;
    private String versionName;
    private ServerVersion parentVersion = this;

    private ServerVersion(Triple<Integer, Integer, Integer> version, ServerVersion parentVersion) {
        this(version, parentVersion.spigotNMS);

        this.parentVersion = parentVersion;
    }

    private ServerVersion(Triple<Integer, Integer, Integer> version, String spigotNMS) {
        this.version = version;
        this.spigotNMS = spigotNMS;

        this.versionName = (version.right == 0)
                ? String.format("v%d_%d", version.left, version.middle)
                : String.format("v%d_%d_%d", version.left, version.middle, version.right);
    }

    /**
     * Retrieves the version numbers as a Triple (major, minor, patch).
     *
     * <p><b>Example:</b>
     * <pre>
     * Triple&lt;Integer, Integer, Integer&gt; nums = current.getVersionNumbers();
     * </pre>
     *
     * @return the Triple representing the version numbers
     */
    public Triple<Integer, Integer, Integer> getVersionNumbers() {
        return version;
    }

    /**
     * Retrieves the version name.
     *
     * <p><b>Example:</b>
     * <pre>
     * String name = current.getVersionName();
     * </pre>
     *
     * @return the version name as a string
     */
    public String getVersionName() {
        return versionName;
    }

    /**
     * Retrieves the Spigot NMS string.
     *
     * <p><b>Example:</b>
     * <pre>
     * String nms = current.getSpigotNMS();
     * </pre>
     *
     * @return the Spigot NMS string
     */
    public String getSpigotNMS() {
        return spigotNMS;
    }

    /**
     * Determines if this version is equal to or newer than the specified version.
     *
     * <p>Comparison is performed on the major, minor, and patch numbers in order.
     * For example, v1_17_1 is considered equal to or newer than v1_17, and v1_18 is considered newer than v1_17_1.
     *
     * <p><b>Example:</b>
     * <pre>
     * boolean result = current.isEqualOrNewer(ServerVersion.v1_21);
     * </pre>
     *
     * @param version the version to compare against
     * @return true if this version is equal to or newer than the specified version; false otherwise
     */
    public boolean isEqualOrNewer(ServerVersion version) {
        Triple<Integer, Integer, Integer> compare = version.getVersionNumbers();

        if ((this.version.left.equals(compare.left) || this.version.left > compare.left)
                && (this.version.middle.equals(compare.middle) || this.version.middle > compare.middle)) {

            if (this.version.middle.equals(compare.middle))
                return (this.version.right.equals(compare.right) || this.version.right >= compare.right);

            return true;
        }

        return false;
    }

    /**
     * Determines if this version is strictly newer than the specified version.
     *
     * <p>Comparison is performed on the major, minor, and patch numbers.
     * For example, v1_17_1 is strictly newer than v1_17.
     *
     * <p><b>Example:</b>
     * <pre>
     * boolean result = current.isStrictlyNewer(ServerVersion.v1_21);
     * </pre>
     *
     * @param version the version to compare against
     * @return true if this version is strictly newer than the specified version; false otherwise
     */
    public boolean isStrictlyNewer(ServerVersion version) {
        Triple<Integer, Integer, Integer> compare = version.getVersionNumbers();

        if ((this.version.left > compare.left)
                || (this.version.left.equals(compare.left) && this.version.middle > compare.middle)
                || (this.version.left.equals(compare.left) && this.version.middle.equals(compare.middle)
                && this.version.right > compare.right)) {
            return true;
        }

        return false;
    }

    /**
     * Determines if this version is exactly the same as the specified version.
     *
     * <p>Equality is determined by comparing the major, minor, and patch numbers.
     *
     * <p><b>Example:</b>
     * <pre>
     * boolean equal = current.isSameVersion(ServerVersion.v1_21);
     * </pre>
     *
     * @param version the version to compare against
     * @return true if both versions are exactly equal; false otherwise
     */
    public boolean isSameVersion(ServerVersion version) {
        Triple<Integer, Integer, Integer> compare = version.getVersionNumbers();
        return this.version.left.equals(compare.left)
                && this.version.middle.equals(compare.middle)
                && this.version.right.equals(compare.right);
    }

    /**
     * Determines if this version is equal to or older than the specified version.
     *
     * <p>Comparison is performed on the major, minor, and patch numbers.
     * For example, v1_17 is considered equal to or older than v1_17_1.
     *
     * <p><b>Example:</b>
     * <pre>
     * boolean result = current.isEqualOrOlder(ServerVersion.v1_21);
     * </pre>
     *
     * @param version the version to compare against
     * @return true if this version is equal to or older than the specified version; false otherwise
     */
    public boolean isEqualOrOlder(ServerVersion version) {
        Triple<Integer, Integer, Integer> compare = version.getVersionNumbers();

        if ((this.version.left.equals(compare.left) || this.version.left < compare.left)
                && (this.version.middle.equals(compare.middle) || this.version.middle < compare.middle)) {

            if (this.version.middle.equals(compare.middle))
                return (this.version.right.equals(compare.right) || this.version.right <= compare.right);

            return true;
        }

        return false;
    }

    /**
     * Determines if this version is strictly older than the specified version.
     *
     * <p>Comparison is performed on the major, minor, and patch numbers.
     * For example, v1_17 is strictly older than v1_17_1.
     *
     * <p><b>Example:</b>
     * <pre>
     * boolean result = current.isStrictlyOlder(ServerVersion.v1_21);
     * </pre>
     *
     * @param version the version to compare against
     * @return true if this version is strictly older than the specified version; false otherwise
     */
    public boolean isStrictlyOlder(ServerVersion version) {
        Triple<Integer, Integer, Integer> compare = version.getVersionNumbers();

        if ((this.version.left < compare.left)
                || (this.version.left.equals(compare.left) && this.version.middle < compare.middle)
                || (this.version.left.equals(compare.left) && this.version.middle.equals(compare.middle)
                && this.version.right < compare.right)) {
            return true;
        }

        return false;
    }

    // --- PRIVATE METHODS (No JavaDocs) --- //

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
}

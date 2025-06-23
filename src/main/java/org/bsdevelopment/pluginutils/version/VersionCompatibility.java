package org.bsdevelopment.pluginutils.version;

import org.bsdevelopment.pluginutils.utilities.Triple;

import java.lang.reflect.AnnotatedElement;

/**
 * A utility class providing checks for {@link VersionLimit} annotations
 * on classes, methods, and fields.
 * <p>
 * This class reads the {@code min} and {@code max} server version ranges
 * from the {@link VersionLimit} annotation and compares them with
 * the current {@link ServerVersion} to decide if the annotated element is valid.
 * <p>
 * <b>Example usage:</b>
 * <pre>
 * // Check compatibility of a class:
 * boolean compatible = VersionCompatibility.isCompatible(MyAnnotatedClass.class);
 *
 * // Or, check a specific method reflection:
 * Method method = MyAnnotatedClass.class.getDeclaredMethod(\"someMethod\");
 * boolean methodCompatible = VersionCompatibility.isCompatible(method);
 * </pre>
 */
public final class VersionCompatibility {
    /**
     * Checks whether the given {@link AnnotatedElement} (class, method, or field)
     * is compatible with the current server version based on the
     * {@link VersionLimit} annotation.
     *
     * <p>
     * If the annotation is not present, it is assumed compatible.
     * <p>
     * <b>Example usage:</b>
     * <pre>
     * // Check compatibility of a class:
     * boolean compatible = VersionCompatibility.isCompatible(MyAnnotatedClass.class);
     *
     * // Or, check a specific method reflection:
     * Method method = MyAnnotatedClass.class.getDeclaredMethod(\"someMethod\");
     * boolean methodCompatible = VersionCompatibility.isCompatible(method);
     * </pre>
     *
     * @param element
     *         the annotated element to check
     *
     * @return {@code true} if compatible, {@code false} otherwise
     */
    public static boolean isCompatible(AnnotatedElement element) {
        VersionLimit limit = element.getAnnotation(VersionLimit.class);
        // No annotation => no restrictions => considered compatible
        if (limit == null) return true;

        // Parse the min and max arrays from the annotation
        int[] minArr = limit.min();
        int[] maxArr = limit.max();

        // Fallback handling if the arrays are shorter/longer than expected
        // but by convention they should always be length = 3
        int minMajor = (minArr.length > 0) ? minArr[0] : 1;
        int minMinor = (minArr.length > 1) ? minArr[1] : 0;
        int minPatch = (minArr.length > 2) ? minArr[2] : 0;

        int maxMajor = (maxArr.length > 0) ? maxArr[0] : 999;
        int maxMinor = (maxArr.length > 1) ? maxArr[1] : 999;
        int maxPatch = (maxArr.length > 2) ? maxArr[2] : 999;

        // Convert these into ServerVersion objects
        ServerVersion current = ServerVersion.getVersion();
        ServerVersion minVer = ServerVersion.of(Triple.of(minMajor, minMinor, minPatch));
        ServerVersion maxVer = ServerVersion.of(Triple.of(maxMajor, maxMinor, maxPatch));

        // The annotated element is valid only if current >= min and current <= max
        boolean meetsMin = current.isEqualOrNewer(minVer);
        boolean meetsMax = current.isEqualOrOlder(maxVer);

        return meetsMin && meetsMax;
    }
}

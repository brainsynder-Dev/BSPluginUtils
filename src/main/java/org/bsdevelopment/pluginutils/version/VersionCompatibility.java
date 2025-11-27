package org.bsdevelopment.pluginutils.version;

import org.bsdevelopment.pluginutils.reflection.Reflection;
import org.bsdevelopment.pluginutils.utilities.Triple;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
     * Checks whether the given {@link Enum} constant is compatible with the current server version based on the {@link VersionLimit} annotation.
     *
     * <p>If the annotation is not present, it is assumed compatible.</p>
     *
     * @param constant the enum constant to check
     *
     * @return {@code true} if compatible, {@code false} otherwise
     */
    public static <E extends Enum<E>> boolean isCompatible(E constant) {
        Field field = Reflection.retrieveField(constant.getDeclaringClass(), constant.name());
        if (field == null) return false;

        return isCompatible(field);
    }

    /**
     * Checks whether the given {@link AnnotatedElement} (class, method, or field)
     * is compatible with the current server version based on the
     * {@link VersionLimit} annotation.
     *
     * <p>If the annotation is not present, it is assumed compatible.</p>
     *
     * @param element the annotated element to check
     *
     * @return {@code true} if compatible, {@code false} otherwise
     */
    public static boolean isCompatible(AnnotatedElement element) {
        VersionLimit limit = element.getAnnotation(VersionLimit.class);
        if (limit == null) return true; // No restrictions

        int[] minArr = limit.min();
        int[] maxArr = limit.max();

        int minMajor = (minArr.length > 0) ? minArr[0] : 1;
        int minMinor = (minArr.length > 1) ? minArr[1] : 0;
        int minPatch = (minArr.length > 2) ? minArr[2] : 0;

        int maxMajor = (maxArr.length > 0) ? maxArr[0] : 999;
        int maxMinor = (maxArr.length > 1) ? maxArr[1] : 999;
        int maxPatch = (maxArr.length > 2) ? maxArr[2] : 999;

        ServerVersion current = ServerVersion.getVersion();
        ServerVersion minVer = ServerVersion.of(Triple.of(minMajor, minMinor, minPatch));
        ServerVersion maxVer = ServerVersion.of(Triple.of(maxMajor, maxMinor, maxPatch));

        boolean meetsMin = current.isEqualOrNewer(minVer);
        boolean meetsMax = current.isEqualOrOlder(maxVer);

        return meetsMin && meetsMax;
    }

    /**
     * Returns an array of enum constants that are compatible with the current server version,
     * based on {@link VersionLimit} annotations on the enum constants (fields).
     *
     * <p>Enum constants without the annotation are treated as always compatible.</p>
     *
     * @param enumClass the enum type
     * @param <E>       the enum type parameter
     *
     * @return an array of compatible enum constants (possibly empty, never {@code null})
     */
    public static <E extends Enum<E>> E[] getCompatibleValues(Class<E> enumClass) {
        E[] allConstants = enumClass.getEnumConstants();
        if (allConstants == null) return (E[]) Array.newInstance(enumClass, 0);

        List<E> compatible = new ArrayList<>();
        for (E constant : allConstants) {
            try {
                Field field = enumClass.getField(constant.name());
                if (isCompatible(field)) compatible.add(constant);
            } catch (NoSuchFieldException ignored) {
            }
        }

        E[] result = (E[]) Array.newInstance(enumClass, compatible.size());
        return compatible.toArray(result);
    }

    /**
     * Returns the enum constant if it is compatible with the current server version,
     * otherwise returns an empty {@link Optional}.
     *
     * @param constant the enum constant to check
     * @param <E>      the enum type
     *
     * @return optional containing the constant if compatible, otherwise empty
     */
    public static <E extends Enum<E>> Optional<E> getIfCompatible(E constant) {
        Class<?> enumClass = constant.getDeclaringClass();
        try {
            Field field = enumClass.getField(constant.name());
            return isCompatible(field) ? Optional.of(constant) : Optional.empty();
        } catch (NoSuchFieldException e) {
            return Optional.empty();
        }
    }
}

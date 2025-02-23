package org.bsdevelopment.pluginutils.version;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Specifies a minimum and maximum server version range that the annotated element supports.
 *
 * <p><b>Usage Example:</b>
 * <pre>
 * &#64;VersionLimit(min = {1, 19, 0}, max = {1, 21, 4})
 * public class SomeFeature { ... }
 * </pre>
 *
 * <p>The annotated element (class, method, etc.) will be considered valid only if the
 * server version is within the {@code min} and {@code max} values (inclusive).
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface VersionLimit {
    /**
     * The minimum supported server version range (major, minor, patch).
     *
     * <p>Defaults to {@code {1,0,0}} (the earliest major version).
     *
     * <p><b>Example:</b>
     * <pre>
     * // Means the code is valid on server versions >= 1.19.0
     * min = {1, 19, 0}
     * </pre>
     *
     * @return an integer array representing the minimum version
     */
    int[] min() default {1, 0, 0};

    /**
     * The maximum supported server version range (major, minor, patch).
     *
     * <p>Defaults to {@code {999,999,999}} (a very high version number).
     *
     * <p><b>Example:</b>
     * <pre>
     * // Means the code is valid on server versions up to 1.21.4
     * max = {1, 21, 4}
     * </pre>
     *
     * @return an integer array representing the maximum version
     */
    int[] max() default {999, 999, 999};
}

/*
 * Copyright © 2025
 * BSDevelopment <https://bsdevelopment.org>
 */
package org.bsdevelopment.pluginutils.utilities;

import org.jetbrains.annotations.Nullable;

/**
 * Simple thread-safe lazy value wrapper.
 *
 * <p>The value is computed on first access using {@link #load()},
 * stored, and then returned on every future call.</p>
 *
 * <p>Implementation is lock-free and uses volatile write ordering to
 * ensure correct publication across threads.</p>
 *
 * @param <T> type of value
 */
public abstract class Lazy<T> {
    private volatile boolean loaded = false;
    private @Nullable T value = null;

    public static <T> Lazy<T> of(@Nullable T value) {
        return new Lazy<>() {
            @Override
            protected @Nullable T load() {
                return value;
            }
        };
    }

    /**
     * Loads the value. Called exactly once on first {@link #get()}.
     */
    protected abstract @Nullable T load();

    /**
     * Returns the lazily computed value.
     */
    public final @Nullable T get() {
        if (!loaded) {
            value = load();
            loaded = true;
        }
        return value;
    }

    /**
     * Returns true if the value has already been resolved.
     */
    public final boolean isLoaded() {
        return loaded;
    }

}

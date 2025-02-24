package org.bsdevelopment.pluginutils.storage.optional;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A container object that can hold up to two values of potentially different types, each of which
 * may be absent (null) or present (non-null). Provides utility methods for handling the presence
 * or absence of each value with fluent, Optional-like semantics.
 *
 * <p><b>Example Usage:</b>
 * <pre>
 * BiOptional&lt;String, Integer&gt; both = BiOptional.of("Hello", 42);
 * BiOptional&lt;String, Integer&gt; firstOnly = BiOptional.of("Hello");
 * BiOptional&lt;String, Integer&gt; empty = BiOptional.empty();
 *
 * both.ifBothPresent((s, i) -&gt; {
 *     System.out.println("String: " + s + ", Integer: " + i);
 * });
 *
 * firstOnly.ifFirstOnlyPresent(s -&gt; System.out.println("Only first present: " + s));
 * empty.ifNonePresent(() -&gt; System.out.println("Nothing present!"));
 * </pre>
 *
 * @param <T> the type of the first value
 * @param <U> the type of the second value
 */
public class BiOptional<T, U> {

    @Nullable
    private final T first;
    @Nullable
    private final U second;

    /**
     * Creates a new BiOptional holding the given first and second values (which may be null).
     *
     * @param first  the first value (or null)
     * @param second the second value (or null)
     */
    public BiOptional(T first, U second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Creates a BiOptional containing only a first value, leaving the second empty.
     *
     * @param first the first value (nullable)
     * @param <T>   the first value's type
     * @param <U>   the second value's type
     * @return a new BiOptional with the provided first value
     */
    public static <T, U> BiOptional<T, U> of(T first) {
        var firstOption = Optional.<T>empty();
        if (first != null) firstOption = Optional.of(first);
        return from(firstOption, Optional.empty());
    }

    /**
     * Creates a BiOptional containing both a first and second value.
     *
     * @param first  the first value (nullable)
     * @param second the second value (nullable)
     * @param <T>    the first value's type
     * @param <U>    the second value's type
     * @return a new BiOptional with the provided values
     */
    public static <T, U> BiOptional<T, U> of(T first, U second) {
        var firstOption = Optional.<T>empty();
        var secondOption = Optional.<U>empty();
        if (first != null) firstOption = Optional.of(first);
        if (second != null) secondOption = Optional.of(second);
        return from(firstOption, secondOption);
    }

    /**
     * Creates an empty BiOptional with neither value present.
     *
     * @param <T> the first type
     * @param <U> the second type
     * @return an empty BiOptional
     */
    public static <T, U> BiOptional<T, U> empty() {
        return from(Optional.empty(), Optional.empty());
    }

    /**
     * Creates a BiOptional from two {@link Optional} values, one for the first value and one for the second.
     *
     * @param first  an Optional for the first value
     * @param second an Optional for the second value
     * @param <T>    the first type
     * @param <U>    the second type
     * @return a new BiOptional reflecting the presence or absence of each Optional
     */
    public static <T, U> BiOptional<T, U> from(Optional<T> first, Optional<U> second) {
        return new BiOptional<>(first.orElse(null), second.orElse(null));
    }

    /**
     * Returns an {@link Optional} describing the first value, if present.
     *
     * @return an Optional with the first value or empty if null
     */
    public Optional<T> first() {
        return Optional.ofNullable(first);
    }

    /**
     * Returns an {@link Optional} describing the second value, if present.
     *
     * @return an Optional with the second value or empty if null
     */
    public Optional<U> second() {
        return Optional.ofNullable(second);
    }

    /**
     * Checks if the first value is non-null.
     *
     * @return true if the first value is present, false otherwise
     */
    public boolean isFirstPresent() {
        return first != null;
    }

    /**
     * Checks if the second value is non-null.
     *
     * @return true if the second value is present, false otherwise
     */
    public boolean isSecondPresent() {
        return second != null;
    }

    /**
     * Checks if only the first value is present and the second is absent.
     *
     * @return true if the first is non-null and the second is null
     */
    public boolean isFirstOnlyPresent() {
        return isFirstPresent() && !isSecondPresent();
    }

    /**
     * Checks if only the second value is present and the first is absent.
     *
     * @return true if the second is non-null and the first is null
     */
    public boolean isSecondOnlyPresent() {
        return !isFirstPresent() && isSecondPresent();
    }

    /**
     * Checks if both the first and second values are present.
     *
     * @return true if both are non-null
     */
    public boolean areBothPresent() {
        return isFirstPresent() && isSecondPresent();
    }

    /**
     * Checks if neither the first nor the second value is present.
     *
     * @return true if both are null
     */
    public boolean areNonePresent() {
        return !isFirstPresent() && !isSecondPresent();
    }

    /**
     * If only the first value is present, performs the given action with the first value.
     *
     * @param ifFirstOnlyPresent consumer to execute if only the first is present
     * @return this BiOptional for chaining
     */
    public BiOptional<T, U> ifFirstOnlyPresent(Consumer<? super T> ifFirstOnlyPresent) {
        if (isFirstOnlyPresent()) ifFirstOnlyPresent.accept(first);
        return this;
    }

    /**
     * If only the second value is present, performs the given action with the second value.
     *
     * @param ifSecondOnlyPresent consumer to execute if only the second is present
     * @return this BiOptional for chaining
     */
    public BiOptional<T, U> ifSecondOnlyPresent(Consumer<? super U> ifSecondOnlyPresent) {
        if (isSecondOnlyPresent()) ifSecondOnlyPresent.accept(second);
        return this;
    }

    /**
     * If both values are present, performs the given action with both.
     *
     * @param ifBothPresent consumer to execute if both are present
     * @return this BiOptional for chaining
     */
    public BiOptional<T, U> ifBothPresent(BiConsumer<? super T, ? super U> ifBothPresent) {
        if (areBothPresent()) ifBothPresent.accept(first, second);
        return this;
    }

    /**
     * If neither value is present, performs the given runnable.
     *
     * @param ifNonePresent runnable to execute if none are present
     * @return this BiOptional for chaining
     */
    public BiOptional<T, U> ifNonePresent(Runnable ifNonePresent) {
        if (areNonePresent()) ifNonePresent.run();
        return this;
    }

    /**
     * If neither value is present, throws an exception provided by the given supplier.
     *
     * @param throwableProvider the supplier of the exception to throw
     * @param <X>               the exception type
     * @throws X if none are present
     */
    public <X extends Throwable> void ifNonePresentThrow(Supplier<? extends X> throwableProvider) throws X {
        if (areNonePresent()) throw throwableProvider.get();
    }

    /**
     * Returns a new {@link BiOptionalMapper} instance for transforming or mapping the two values
     * stored in this BiOptional.
     *
     * @param <R> the type of the mapped result
     * @return a BiOptionalMapper bound to this BiOptional
     */
    public <R> BiOptionalMapper<T, U, R> mapper() {
        return new BiOptionalMapper<>(this);
    }
}

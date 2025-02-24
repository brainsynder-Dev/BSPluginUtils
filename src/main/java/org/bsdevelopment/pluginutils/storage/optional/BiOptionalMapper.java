package org.bsdevelopment.pluginutils.storage.optional;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Provides a mechanism to map the state of a {@link BiOptional} into a single result object,
 * depending on which values (first, second, or both) are present—or if none are present.
 *
 * <p><b>Example Usage:</b>
 * <pre>
 * BiOptional&lt;String, Integer&gt; biOpt = BiOptional.of("Hello", 42);
 *
 * // Map the states to a string describing the state:
 * String result = biOpt.mapper()
 *     .onFirstOnlyPresent(s -&gt; "First only: " + s)
 *     .onSecondOnlyPresent(i -&gt; "Second only: " + i)
 *     .onBothPresent((s, i) -&gt; "Both present: " + s + " and " + i)
 *     .onNonePresent("No values present")
 *     .result();  // returns "Both present: Hello and 42"
 * </pre>
 *
 * <p>The mapper must produce exactly one result, or an exception will be thrown if
 * no state matched or multiple states matched and attempted to overwrite a result.
 *
 * @param <T> the type of the first value in the associated {@link BiOptional}
 * @param <U> the type of the second value in the associated {@link BiOptional}
 * @param <R> the type of the result produced by this mapper
 */
public class BiOptionalMapper<T, U, R> {

    private final BiOptional<T, U> biOptional;
    private R result = null;

    /**
     * Constructs a mapper that operates on the given {@link BiOptional}.
     *
     * <p>This constructor is package-private; use {@link BiOptional#mapper()} to obtain a mapper instance.
     *
     * @param biOptional the BiOptional to be mapped
     */
    BiOptionalMapper(BiOptional<T, U> biOptional) {
        this.biOptional = biOptional;
    }

    /**
     * If only the first value is present, applies the given function to produce a mapping result.
     *
     * @param firstMapper a function mapping the first value to a result
     * @return this mapper, for chaining
     */
    public BiOptionalMapper<T, U, R> onFirstOnlyPresent(Function<? super T, ? extends R> firstMapper) {
        if (biOptional.isFirstOnlyPresent()) setResult(firstMapper.apply(biOptional.first().get()));
        return this;
    }

    /**
     * If only the second value is present, applies the given function to produce a mapping result.
     *
     * @param secondMapper a function mapping the second value to a result
     * @return this mapper, for chaining
     */
    public BiOptionalMapper<T, U, R> onSecondOnlyPresent(Function<? super U, ? extends R> secondMapper) {
        if (biOptional.isSecondOnlyPresent()) setResult(secondMapper.apply(biOptional.second().get()));
        return this;
    }

    /**
     * If both values are present, applies the given function (BiFunction) to produce a mapping result.
     *
     * @param bothMapper a function that takes both values and produces a result
     * @return this mapper, for chaining
     */
    public BiOptionalMapper<T, U, R> onBothPresent(BiFunction<? super T, ? super U, ? extends R> bothMapper) {
        if (biOptional.areBothPresent()) setResult(bothMapper.apply(biOptional.first().get(), biOptional.second().get()));
        return this;
    }

    /**
     * If neither value is present, produces a result via the given supplier.
     *
     * @param supplier a supplier that provides the result if none are present
     * @return this mapper, for chaining
     */
    public BiOptionalMapper<T, U, R> onNonePresent(Supplier<? extends R> supplier) {
        if (biOptional.areNonePresent()) setResult(supplier.get());
        return this;
    }

    /**
     * If neither value is present, uses the given object as the result.
     *
     * @param other the result object when none are present
     * @return this mapper, for chaining
     */
    public BiOptionalMapper<T, U, R> onNonePresent(R other) {
        if (biOptional.areNonePresent()) setResult(other);
        return this;
    }

    /**
     * If neither value is present, throws an exception provided by the given supplier.
     *
     * @param throwableProvider a supplier of the exception to be thrown
     * @param <X>              the type of exception
     * @return this mapper, for chaining
     * @throws X if none of the values are present
     */
    public <X extends Throwable> BiOptionalMapper<T, U, R> onNonePresentThrow(Supplier<? extends X> throwableProvider) throws X {
        biOptional.ifNonePresentThrow(throwableProvider);
        return this;
    }

    /**
     * Retrieves the mapping result. Throws {@link IllegalStateException} if no result was set.
     *
     * @return the mapping result
     */
    public R result() {
        if (result == null) throw new IllegalStateException("Result absent");
        return result;
    }

    /**
     * Retrieves an {@link Optional} of the mapping result.
     *
     * @return an Optional describing the result, or empty if none
     */
    public Optional<R> optionalResult() {
        return Optional.ofNullable(result);
    }

    /**
     * Sets the result, ensuring it is non-null and not already set.
     *
     * @param result the result value
     */
    private void setResult(R result) {
        if (result == null) throw new IllegalArgumentException("Null obtained from a mapper");
        if (this.result != null) throw new IllegalStateException("Result already present: " + this.result);
        this.result = result;
    }
}

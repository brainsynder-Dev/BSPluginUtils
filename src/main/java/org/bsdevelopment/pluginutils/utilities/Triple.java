package org.bsdevelopment.pluginutils.utilities;

import java.util.Objects;

/**
 * A generic container class that holds three values.
 *
 * <p>This class represents a triple of values, commonly used to store three related objects.
 *
 * <p><b>Examples:</b>
 * <pre>
 * // Creating a Triple with three values
 * Triple&lt;Integer, String, Boolean&gt; triple = Triple.of(1, "Hello", true);
 *
 * // Accessing the values
 * Integer left = triple.getLeft();
 * String middle = triple.getMiddle();
 * Boolean right = triple.getRight();
 *
 * // Modifying the values
 * triple.setLeft(2)
 *       .setMiddle("World")
 *       .setRight(false);
 * </pre>
 *
 * @param <L>
 *         the type of the left element
 * @param <M>
 *         the type of the middle element
 * @param <R>
 *         the type of the right element
 */
public final class Triple<L, M, R> {

    public L left;
    public M middle;
    public R right;

    /**
     * Creates a new Triple with the specified values.
     *
     * <p><b>Example:</b>
     * <pre>
     * Triple&lt;Integer, String, Boolean&gt; triple = Triple.of(1, "Hello", true);
     * </pre>
     *
     * @param left
     *         the left value
     * @param middle
     *         the middle value
     * @param right
     *         the right value
     * @param <L>
     *         the type of the left value
     * @param <M>
     *         the type of the middle value
     * @param <R>
     *         the type of the right value
     *
     * @return a new Triple containing the specified values
     */
    public static <L, M, R> Triple<L, M, R> of(L left, M middle, R right) {
        return new Triple(left, middle, right);
    }

    /**
     * Constructs a Triple with the given values.
     *
     * @param left
     *         the left value
     * @param middle
     *         the middle value
     * @param right
     *         the right value
     */
    public Triple(L left, M middle, R right) {
        this.left = left;
        this.middle = middle;
        this.right = right;
    }

    /**
     * Sets the left value.
     *
     * <p><b>Example:</b>
     * <pre>
     * triple.setLeft(10);
     * </pre>
     *
     * @param left
     *         the new left value
     *
     * @return this Triple instance for chaining
     */
    public Triple setLeft(L left) {
        this.left = left;
        return this;
    }

    /**
     * Sets the middle value.
     *
     * <p><b>Example:</b>
     * <pre>
     * triple.setMiddle("Updated");
     * </pre>
     *
     * @param middle
     *         the new middle value
     *
     * @return this Triple instance for chaining
     */
    public Triple setMiddle(M middle) {
        this.middle = middle;
        return this;
    }

    /**
     * Sets the right value.
     *
     * <p><b>Example:</b>
     * <pre>
     * triple.setRight(false);
     * </pre>
     *
     * @param right
     *         the new right value
     *
     * @return this Triple instance for chaining
     */
    public Triple setRight(R right) {
        this.right = right;
        return this;
    }

    /**
     * Retrieves the left value.
     *
     * <p><b>Example:</b>
     * <pre>
     * Integer left = triple.getLeft();
     * </pre>
     *
     * @return the left value
     */
    public L getLeft() {
        return this.left;
    }

    /**
     * Retrieves the middle value.
     *
     * <p><b>Example:</b>
     * <pre>
     * String middle = triple.getMiddle();
     * </pre>
     *
     * @return the middle value
     */
    public M getMiddle() {
        return this.middle;
    }

    /**
     * Retrieves the right value.
     *
     * <p><b>Example:</b>
     * <pre>
     * Boolean right = triple.getRight();
     * </pre>
     *
     * @return the right value
     */
    public R getRight() {
        return this.right;
    }

    /**
     * Returns a string representation of this Triple.
     *
     * <p><b>Example:</b>
     * <pre>
     * System.out.println(triple);
     * // Output: Triple{left=1, middle=Hello, right=true}
     * </pre>
     *
     * @return a string representation of the Triple
     */
    @Override
    public String toString() {
        return "Triple{" +
                "left=" + left +
                ", middle=" + middle +
                ", right=" + right +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Triple<?, ?, ?> triple = (Triple<?, ?, ?>) o;
        return Objects.equals(left, triple.left) && Objects.equals(middle, triple.middle) && Objects.equals(right, triple.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, middle, right);
    }
}

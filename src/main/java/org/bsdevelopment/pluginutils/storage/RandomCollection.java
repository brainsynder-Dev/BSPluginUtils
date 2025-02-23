package org.bsdevelopment.pluginutils.storage;

import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

/**
 * Manages a weighted collection of items from which random selections can be made.
 * Each item can be assigned a weight (default 50), influencing its probability.
 *
 * <p>This class is backed by a {@link NavigableMap} that maps cumulative weights to values.
 *
 * <p><b>Example Usage:</b>
 * <pre>
 * // Create a random collection from a set of strings
 * RandomCollection&lt;String&gt; rc = new RandomCollection&lt;&gt;();
 * rc.add(10, "Hello");
 * rc.add(40, "World");
 * String chosen = rc.next();  // returns "Hello" ~20% of the time, "World" ~80% of the time
 * </pre>
 *
 * @param <E> the type of elements this collection holds
 */
public class RandomCollection<E> {

    /**
     * The map of cumulative weights to elements.
     */
    private final NavigableMap<Double, E> map;

    /**
     * A pseudo-random number generator used for element selection.
     */
    private final Random random;

    /**
     * Tracks the total weight of all entries in this collection.
     */
    private double total;

    /**
     * Creates a random collection from a {@link Collection} with default weight (50) assigned to each element.
     *
     * <p><b>Example:</b>
     * <pre>
     * String result = RandomCollection.randomize(Arrays.asList("A", "B", "C"));
     * </pre>
     *
     * @param list the collection of elements
     * @param <E> the element type
     * @return a random element from the collection
     */
    public static <E> E randomize(Collection<E> list) {
        return randomize(list, 50);
    }

    /**
     * Creates a random collection from a {@link Collection} with the given weight assigned to each element,
     * and immediately returns a random element.
     *
     * <p><b>Example:</b>
     * <pre>
     * String result = RandomCollection.randomize(Arrays.asList("A", "B", "C"), 75);
     * </pre>
     *
     * @param list the collection of elements
     * @param percent the weight to assign to each element
     * @param <E> the element type
     * @return a random element from the collection
     */
    public static <E> E randomize(Collection<E> list, int percent) {
        RandomCollection<E> collection = new RandomCollection<>();
        list.forEach(e -> collection.add(percent, e));
        return collection.next();
    }

    /**
     * Builds a {@link RandomCollection} from a {@link Collection} with a default weight of 50.
     *
     * <p><b>Example:</b>
     * <pre>
     * RandomCollection&lt;String&gt; rc = RandomCollection.fromCollection(Arrays.asList("A", "B", "C"));
     * </pre>
     *
     * @param list the collection of elements
     * @param <E> the element type
     * @return a new {@link RandomCollection} with default weight for each element
     */
    public static <E> RandomCollection<E> fromCollection(Collection<E> list) {
        return fromCollection(list, 50);
    }

    /**
     * Builds a {@link RandomCollection} from a {@link Collection} using the specified weight for each element.
     *
     * <p><b>Example:</b>
     * <pre>
     * RandomCollection&lt;String&gt; rc = RandomCollection.fromCollection(Arrays.asList("A", "B", "C"), 25);
     * </pre>
     *
     * @param list the collection of elements
     * @param percent the weight to assign to each element
     * @param <E> the element type
     * @return a new {@link RandomCollection} with the specified weight for each element
     */
    public static <E> RandomCollection<E> fromCollection(Collection<E> list, int percent) {
        RandomCollection<E> collection = new RandomCollection<>();
        list.forEach(e -> collection.add(percent, e));
        return collection;
    }

    /**
     * Constructs an empty {@link RandomCollection} using a default {@link Random} instance.
     */
    public RandomCollection() {
        this(new Random());
    }

    /**
     * Constructs an empty {@link RandomCollection} using the provided {@link Random} instance.
     *
     * @param var1 the random generator to use
     */
    public RandomCollection(Random var1) {
        this.map = new TreeMap<>();
        this.total = 0.0D;
        this.random = var1;
    }

    /**
     * Adds an element with a default weight of 50.
     *
     * @param value the element to add
     */
    public void add(E value) {
        add(50, value);
    }

    /**
     * Adds an element with the specified weight to this collection.
     * If weight is &gt; 0, the element is considered for random selection.
     *
     * <p><b>Example:</b>
     * <pre>
     * randomCollection.add(75, "Hello");
     * </pre>
     *
     * @param percent the weight for this element
     * @param value the element to add
     */
    public void add(double percent, E value) {
        if (percent > 0.0D) {
            total += percent;
            map.put(total, value);
        }
    }

    /**
     * Retrieves a view of all elements in this collection (ignoring weights).
     *
     * @return a collection of elements
     */
    public Collection<E> values() {
        return map.values();
    }

    /**
     * Selects a random element from this collection based on the assigned weights.
     *
     * <p><b>Example:</b>
     * <pre>
     * E item = randomCollection.next();
     * </pre>
     *
     * @return a randomly selected element
     */
    public E next() {
        double rnd = random.nextDouble() * total;
        return map.ceilingEntry(rnd).getValue();
    }

    /**
     * Checks if this random collection is empty.
     *
     * @return true if no items have been added, otherwise false
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Returns the number of distinct elements in this random collection.
     *
     * @return the size of this collection
     */
    public int getSize() {
        return map.size();
    }

    /**
     * Randomly selects an element from this collection and removes it from future consideration.
     *
     * <p>Useful when you want to pick without replacement.
     *
     * <p><b>Example:</b>
     * <pre>
     * E removed = randomCollection.nextRemove();
     * </pre>
     *
     * @return the selected element, or null if the collection is empty
     */
    public E nextRemove() {
        if (map.isEmpty()) return null;

        double rnd = random.nextDouble() * total;
        Map.Entry<Double, E> entry = map.ceilingEntry(rnd);
        var value = entry.getValue();

        total -= entry.getKey();
        map.remove(entry.getKey());
        return value;
    }
}

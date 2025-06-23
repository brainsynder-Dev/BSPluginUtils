package org.bsdevelopment.pluginutils.storage;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Ticker;
import com.google.common.collect.Maps;
import com.google.common.primitives.Longs;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A hash map that stores key-value pairs with individual expiration times. Once an entry
 * expires, it is automatically removed from the map upon the next retrieval or update.
 *
 * <p>This class provides operations similar to a standard {@link Map}, but any method call
 * triggers an eviction check to remove expired entries before proceeding.
 *
 * <p><b>Example Usage:</b>
 * <pre>
 * ExpireHashMap&lt;String, String&gt; cache = new ExpireHashMap&lt;&gt;();
 * cache.put("greeting", "Hello", 30, TimeUnit.SECONDS);
 * // ...
 * String value = cache.get("greeting"); // null if expired
 * </pre>
 *
 * @param <K>
 *         the type of keys maintained by this map
 * @param <V>
 *         the type of mapped values
 */
public class ExpireHashMap<K, V> {

    /**
     * A lookup from key to its corresponding {@link ExpireEntry}.
     */
    private final Map<K, ExpireEntry> keyLookup;

    /**
     * A min-heap (priority queue) that orders entries by their expiration time.
     */
    private final PriorityQueue<ExpireEntry> expireQueue;

    /**
     * A transformed view of the {@code keyLookup} map, exposing only the values.
     */
    private final Map<K, V> valueView;

    /**
     * A {@link Ticker} used to measure elapsed time for expiration checks.
     */
    private final Ticker ticker;

    /**
     * Constructs an {@code ExpireHashMap} using the system ticker for time measurement.
     */
    public ExpireHashMap() {
        this(Ticker.systemTicker());
    }

    /**
     * Constructs an {@code ExpireHashMap} using a specified {@link Ticker} for time measurement.
     *
     * @param ticker
     *         the ticker to use for measuring elapsed time
     */
    public ExpireHashMap(Ticker ticker) {
        keyLookup = new HashMap<>();
        expireQueue = new PriorityQueue<>();
        valueView = Maps.transformValues(keyLookup, entry -> entry.value);
        this.ticker = ticker;
    }

    /**
     * Retrieves the value associated with the given key, evicting any expired entries first.
     *
     * @param key
     *         the key whose associated value is to be returned
     *
     * @return the value for the specified key, or {@code null} if none exists (or if expired)
     */
    public V get(K key) {
        evict();
        var entry = keyLookup.get(key);
        return entry != null ? entry.value : null;
    }

    /**
     * Associates the specified value with the specified key in this map, assigning an expiration time.
     *
     * <p>If the map previously contained a mapping for the key, the old value is replaced, and the
     * old mapping is returned. Otherwise, {@code null} is returned.
     *
     * @param key
     *         the key
     * @param value
     *         the value
     * @param expireDelay
     *         the expiration delay
     * @param expireUnit
     *         the time unit of the expiration delay
     *
     * @return the previous value associated with key, or {@code null} if there was none
     * @throws IllegalStateException
     *         if {@code expireDelay} is non-positive
     * @throws NullPointerException
     *         if {@code expireUnit} is null
     */
    public V put(K key, V value, long expireDelay, TimeUnit expireUnit) {
        Preconditions.checkNotNull(expireUnit, "expireUnit cannot be NULL");
        Preconditions.checkState(expireDelay > 0L, "expireDelay cannot be equal or less than zero.");

        evict();

        var entry = new ExpireEntry(ticker.read() + TimeUnit.NANOSECONDS.convert(expireDelay, expireUnit), key, value);
        var previous = keyLookup.put(key, entry);

        expireQueue.add(entry);
        return previous != null ? previous.value : null;
    }

    /**
     * Checks if this map contains a mapping for the specified key.
     *
     * @param key
     *         the key to check
     *
     * @return {@code true} if the map contains the key, otherwise {@code false}
     */
    public boolean containsKey(K key) {
        evict();
        return keyLookup.containsKey(key);
    }

    /**
     * Checks if this map contains one or more mappings to the specified value.
     *
     * @param value
     *         the value to check
     *
     * @return {@code true} if one or more mappings to the value exist, otherwise {@code false}
     */
    public boolean containsValue(V value) {
        evict();
        for (var entry : keyLookup.values()) {
            if (Objects.equal(value, entry.value)) return true;
        }
        return false;
    }

    /**
     * Removes the mapping for a key from this map if it is present.
     *
     * @param key
     *         the key to remove
     *
     * @return the value that was removed, or {@code null} if no mapping existed
     */
    public V removeKey(K key) {
        evict();
        var entry = keyLookup.remove(key);
        return entry != null ? entry.value : null;
    }

    /**
     * Returns the number of key-value mappings in this map, excluding expired entries.
     *
     * @return the current size of the map
     */
    public int size() {
        evict();
        return keyLookup.size();
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map, excluding expired entries.
     *
     * @return a set of the keys
     */
    public Set<K> keySet() {
        evict();
        return keyLookup.keySet();
    }

    /**
     * Returns a collection view of the values contained in this map, excluding expired entries.
     *
     * @return a collection of the values
     */
    public Collection<V> values() {
        evict();
        return valueView.values();
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map, excluding expired entries.
     *
     * @return a set of the key-value entries
     */
    public Set<Entry<K, V>> entrySet() {
        evict();
        return valueView.entrySet();
    }

    /**
     * Returns a read-only {@link Map} view of this expired-hash map.
     *
     * @return a view of the current mappings
     */
    public Map<K, V> asMap() {
        evict();
        return valueView;
    }

    /**
     * Reconstructs the {@link PriorityQueue} from current entries if needed. This method evicts
     * expired entries first, then rebuilds the queue from the remaining map entries.
     */
    public void collect() {
        evict();
        expireQueue.clear();
        expireQueue.addAll(keyLookup.values());
    }

    /**
     * Removes all mappings from this map, including any that have not yet expired.
     */
    public void clear() {
        keyLookup.clear();
        expireQueue.clear();
    }

    /**
     * Evicts any entries whose expiration time has passed relative to the current ticker value.
     */
    protected void evict() {
        var current = ticker.read();

        while (expireQueue.size() > 0 && expireQueue.peek().time <= current) {
            var entry = expireQueue.poll();
            if (entry == keyLookup.get(entry.key)) keyLookup.remove(entry.key);
        }
    }

    /**
     * Returns a string representation of this {@code ExpireHashMap}.
     *
     * @return a string containing the mappings
     */
    @Override
    public String toString() {
        return keyLookup.toString();
    }

    /**
     * An internal entry representing a key, value, and expiration time.
     */
    private class ExpireEntry implements Comparable<ExpireEntry> {
        public final long time;
        public final K key;
        public final V value;

        public ExpireEntry(long time, K key, V value) {
            this.time = time;
            this.key = key;
            this.value = value;
        }

        /**
         * Compares entries based on their expiration times, in ascending order.
         *
         * @param other
         *         another {@link ExpireEntry}
         *
         * @return a negative integer, zero, or a positive integer
         */
        @Override
        public int compareTo(ExpireEntry other) {
            return Longs.compare(this.time, other.time);
        }

        @Override
        public String toString() {
            return "ExpireEntry [time=" + time + ", key=" + key + ", value=" + value + "]";
        }
    }
}

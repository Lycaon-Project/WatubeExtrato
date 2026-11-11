package org.schabi.newpipe.extractor.utils

import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToLong

/**
 * A [Serializable] cache class used by the extractor to cache manifests
 * generated with extractor's manifests generators.
 *
 * It relies internally on a [ConcurrentHashMap] to allow concurrent access to the cache.
 *
 * @param K the type of cache keys, which must be [Serializable]
 * @param V the type of the second element of [Pair]s used as values of the cache,
 * which must be [Serializable]
 */
public class ManifestCreatorCache<K : Serializable, V : Serializable> : Serializable {

    /**
     * The [ConcurrentHashMap] used internally as the cache of manifests.
     */
    private val concurrentHashMap = ConcurrentHashMap<K, Pair<Int, V>>()

    /**
     * The maximum size of the cache.
     *
     * The default value is [DEFAULT_MAXIMUM_SIZE].
     */
    @Volatile
    private var maxSize = DEFAULT_MAXIMUM_SIZE

    /**
     * The clear factor of the cache, which is a double between `0` and `1` excluded, used when
     * the cache limit size is reached.
     *
     * The default value is [DEFAULT_CLEAR_FACTOR]. Note that a new value will be only used the
     * next time the cache size limit is reached.
     *
     * @throws IllegalArgumentException if the clear factor set is invalid
     */
    @Volatile
    public var clearFactor: Double = DEFAULT_CLEAR_FACTOR
        set(value) {
            require(value > 0 && value < 1) { "Invalid clear factor" }
            field = value
        }

    /**
     * Tests if the specified key is in the cache.
     *
     * @param key the key to test its presence in the cache
     * @return `true` if the key is in the cache, `false` otherwise.
     */
    public fun containsKey(key: K): Boolean = concurrentHashMap.containsKey(key)

    /**
     * Returns the value to which the specified key is mapped, or `null` if the cache
     * contains no mapping for the key.
     *
     * @param key the key to which getting its value
     * @return the value to which the specified key is mapped, or `null`
     */
    public operator fun get(key: K): Pair<Int, V>? = concurrentHashMap[key]

    /**
     * Adds a new element to the cache.
     *
     * If the cache limit is reached, oldest elements will be cleared first using the load factor
     * and the maximum size.
     *
     * @param key   the key to put
     * @param value the value to associate to the key
     *
     * @return the previous value associated with the key, or `null` if there was no mapping
     * for the key (note that a null return can also indicate that the cache previously associated
     * `null` with the key).
     */
    public fun put(key: K, value: V): V? {
        if (!concurrentHashMap.containsKey(key) && concurrentHashMap.size >= maxSize) {
            keepNewestEntries(newCacheSize(maxSize))
        }

        return concurrentHashMap.put(key, Pair(concurrentHashMap.size, value))?.second
    }

    /**
     * Clears the cached manifests.
     *
     * The cache will be empty after this method is called.
     */
    public fun clear() {
        concurrentHashMap.clear()
    }

    /**
     * Resets the cache.
     *
     * The cache will be empty and the clear factor and the maximum size will be reset to their
     * default values.
     *
     * @see clear
     * @see resetClearFactor
     * @see resetMaximumSize
     */
    public fun reset() {
        clear()
        resetClearFactor()
        resetMaximumSize()
    }

    /**
     * @return the number of cached manifests in the cache
     */
    public fun size(): Int = concurrentHashMap.size

    /**
     * The maximum size of the cache; set it with [setMaximumSize].
     */
    public val maximumSize: Long
        get() = maxSize.toLong()

    /**
     * Sets the maximum size of the cache.
     *
     * If the current cache size is more than the new maximum size, the percentage of one less the
     * clear factor of the maximum new size of manifests in the cache will be removed.
     *
     * @param maximumSize the new maximum size of the cache
     * @throws IllegalArgumentException if `maximumSize` is less than or equal to 0
     */
    public fun setMaximumSize(maximumSize: Int) {
        require(maximumSize > 0) { "Invalid maximum size" }

        if (maximumSize < maxSize && !concurrentHashMap.isEmpty()) {
            keepNewestEntries(newCacheSize(maximumSize))
        }

        maxSize = maximumSize
    }

    /**
     * Resets the maximum size of the cache to its [default value][DEFAULT_MAXIMUM_SIZE].
     */
    public fun resetMaximumSize() {
        maxSize = DEFAULT_MAXIMUM_SIZE
    }

    /**
     * Resets the clear factor to its [default value][DEFAULT_CLEAR_FACTOR].
     */
    public fun resetClearFactor() {
        clearFactor = DEFAULT_CLEAR_FACTOR
    }

    override fun toString(): String =
        // the size only: the keys are streaming URLs, which hold the user's IP address
        "ManifestCreatorCache[clearFactor=$clearFactor, maximumSize=$maxSize, size=${concurrentHashMap.size}]"

    /** The size to keep for [limit] and the clear factor; never 0. */
    private fun newCacheSize(limit: Int): Int {
        // Math.round(double) then the (int) cast of the Java original
        val size = (limit * clearFactor).roundToLong().toInt()
        return if (size != 0) size else 1
    }

    /**
     * Keeps only the newest entries in a cache.
     *
     * This method will first collect the entries to remove by looping through the concurrent hash
     * map
     *
     * @param newLimit the new limit of the cache
     */
    private fun keepNewestEntries(newLimit: Int) {
        val difference = concurrentHashMap.size - newLimit
        val entriesToRemove = ArrayList<Map.Entry<K, Pair<Int, V>>>()

        for (entry in concurrentHashMap.entries) {
            val value = entry.value
            if (value.first < difference) {
                entriesToRemove.add(entry)
            } else {
                value.first = value.first - difference
            }
        }

        for (entry in entriesToRemove) {
            concurrentHashMap.remove(entry.key, entry.value)
        }
    }

    public companion object {
        private const val serialVersionUID: Long = 1L

        /**
         * The default maximum size of a manifest cache.
         */
        /**
         * Enough for the streams of the few last videos: the keys are short-lived streaming URLs
         * and the values whole manifests, so an unbounded cache only grows during a session.
         */
        public const val DEFAULT_MAXIMUM_SIZE: Int = 64

        /**
         * The default clear factor of a manifest cache.
         */
        public const val DEFAULT_CLEAR_FACTOR: Double = 0.75
    }
}

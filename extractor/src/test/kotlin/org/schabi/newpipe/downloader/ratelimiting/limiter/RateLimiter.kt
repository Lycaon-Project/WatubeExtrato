/*
 * Copyright (C) 2012 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.schabi.newpipe.downloader.ratelimiting.limiter

import org.schabi.newpipe.downloader.ratelimiting.limiter.SmoothRateLimiter.SmoothWarmingUp
import java.time.Duration
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MICROSECONDS
import java.util.concurrent.TimeUnit.NANOSECONDS
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.math.max

/**
 * A rate limiter. Conceptually, a rate limiter distributes permits at a configurable rate. Each
 * [acquire] blocks if necessary until a permit is available, and then takes it. Once
 * acquired, permits need not be released.
 *
 * `RateLimiter` is safe for concurrent use: It will restrict the total rate of calls from
 * all threads. Note, however, that it does not guarantee fairness.
 *
 * Rate limiters are often used to restrict the rate at which some physical or logical resource
 * is accessed. This is in contrast to [java.util.concurrent.Semaphore] which restricts the
 * number of concurrent accesses instead of the rate (note though that concurrency and rate are
 * closely related, e.g. see [Little's Law](http://en.wikipedia.org/wiki/Little%27s_law)).
 *
 * A `RateLimiter` is defined primarily by the rate at which permits are issued. Absent
 * additional configuration, permits will be distributed at a fixed rate, defined in terms of
 * permits per second. Permits will be distributed smoothly, with the delay between individual
 * permits being adjusted to ensure that the configured rate is maintained.
 *
 * It is possible to configure a `RateLimiter` to have a warmup period during which time
 * the permits issued each second steadily increases until it hits the stable rate.
 *
 * As an example, imagine that we have a list of tasks to execute, but we don't want to submit
 * more than 2 per second:
 *
 * ```
 * val rateLimiter = RateLimiter.create(2.0) // rate is "2 permits per second"
 * fun submitTasks(tasks: List<Runnable>, executor: Executor) {
 *     for (task in tasks) {
 *         rateLimiter.acquire() // may wait
 *         executor.execute(task)
 *     }
 * }
 * ```
 *
 * As another example, imagine that we produce a stream of data, and we want to cap it at 5kb per
 * second. This could be accomplished by requiring a permit per byte, and specifying a rate of 5000
 * permits per second:
 *
 * ```
 * val rateLimiter = RateLimiter.create(5000.0) // rate = 5000 permits per second
 * fun submitPacket(packet: ByteArray) {
 *     rateLimiter.acquire(packet.size)
 *     networkService.send(packet)
 * }
 * ```
 *
 * It is important to note that the number of permits requested *never* affects the
 * throttling of the request itself (an invocation to `acquire(1)` and an invocation to
 * `acquire(1000)` will result in exactly the same throttling, if any), but it affects the
 * throttling of the *next* request. I.e., if an expensive task arrives at an idle RateLimiter, it
 * will be granted immediately, but it is the *next* request that will experience extra throttling,
 * thus paying for the cost of the expensive task.
 *
 * This package (rate limiting) is based on [google/guava](https://github.com/google/guava/)
 * (Apache 2.0 license) but was modified/refactored for our use (by litetex).
 *
 * @author Dimitris Andreou
 * @since 13.0
 */
abstract class RateLimiter internal constructor(
    /**
     * The underlying timer; used both to measure elapsed time and sleep as necessary. A separate
     * object to facilitate testing.
     */
    private val stopwatch: SleepingStopwatch,
) {

    // Can't be initialized in the constructor because mocks don't call the constructor.
    @Volatile
    private var mutexDoNotUseDirectly: Any? = null

    private fun mutex(): Any {
        mutexDoNotUseDirectly?.let { return it }
        synchronized(this) {
            val existing = mutexDoNotUseDirectly
            if (existing != null) {
                return existing
            }
            val created = Any()
            mutexDoNotUseDirectly = created
            return created
        }
    }

    /**
     * Updates the stable rate of this `RateLimiter`, that is, the `permitsPerSecond`
     * argument provided in the factory method that constructed the `RateLimiter`. Currently
     * throttled threads will **not** be awakened as a result of this invocation, thus they do not
     * observe the new rate; only subsequent requests will.
     *
     * Note though that, since each request repays (by waiting, if necessary) the cost of the
     * *previous* request, this means that the very next request after an invocation to
     * `setRate` will not be affected by the new rate; it will pay the cost of the previous
     * request, which is in terms of the previous rate.
     *
     * The behavior of the `RateLimiter` is not modified in any other way, e.g. if the
     * `RateLimiter` was configured with a warmup period of 20 seconds, it still has a warmup
     * period of 20 seconds after this method invocation.
     *
     * @param permitsPerSecond the new stable rate of this `RateLimiter`
     * @throws IllegalArgumentException if `permitsPerSecond` is negative or zero
     */
    fun setRate(permitsPerSecond: Double) {
        require(permitsPerSecond > 0.0) { "rate must be positive" }
        synchronized(mutex()) {
            doSetRate(permitsPerSecond, stopwatch.readMicros())
        }
    }

    internal abstract fun doSetRate(permitsPerSecond: Double, nowMicros: Long)

    /**
     * Returns the stable rate (as `permits per seconds`) with which this `RateLimiter` is
     * configured with. The initial value of this is the same as the `permitsPerSecond` argument
     * passed in the factory method that produced this `RateLimiter`, and it is only updated
     * after invocations to [setRate].
     */
    fun getRate(): Double = synchronized(mutex()) { doGetRate() }

    internal abstract fun doGetRate(): Double

    /**
     * Acquires a single permit from this `RateLimiter`, blocking until the request can be
     * granted. Tells the amount of time slept, if any.
     *
     * This method is equivalent to `acquire(1)`.
     *
     * @return time spent sleeping to enforce rate, in seconds; 0.0 if not rate-limited
     * @since 16.0 (present in 13.0 with `void` return type})
     */
    open fun acquire(): Double = acquire(1)

    /**
     * Acquires the given number of permits from this `RateLimiter`, blocking until the request
     * can be granted. Tells the amount of time slept, if any.
     *
     * @param permits the number of permits to acquire
     * @return time spent sleeping to enforce rate, in seconds; 0.0 if not rate-limited
     * @throws IllegalArgumentException if the requested number of permits is negative or zero
     * @since 16.0 (present in 13.0 with `void` return type})
     */
    open fun acquire(permits: Int): Double {
        val microsToWait = reserve(permits)
        stopwatch.sleepMicrosUninterruptibly(microsToWait)
        return 1.0 * microsToWait / SECONDS.toMicros(1L)
    }

    /**
     * Reserves the given number of permits from this `RateLimiter` for future use, returning
     * the number of microseconds until the reservation can be consumed.
     *
     * @return time in microseconds to wait until the resource can be acquired, never negative
     */
    internal fun reserve(permits: Int): Long {
        checkPermits(permits)
        return synchronized(mutex()) {
            reserveAndGetWaitLength(permits, stopwatch.readMicros())
        }
    }

    /**
     * Acquires a permit from this `RateLimiter` if it can be obtained without exceeding the
     * specified `timeout`, or returns `false` immediately (without waiting) if the permit
     * would not have been granted before the timeout expired.
     *
     * This method is equivalent to `tryAcquire(1, timeout)`.
     *
     * @param timeout the maximum time to wait for the permit. Negative values are treated as zero.
     * @return `true` if the permit was acquired, `false` otherwise
     * @throws IllegalArgumentException if the requested number of permits is negative or zero
     * @since 28.0 (but only since 33.4.0 in the Android flavor)
     */
    open fun tryAcquire(timeout: Duration): Boolean = tryAcquire(1, timeout.toNanos(), NANOSECONDS)

    /**
     * Acquires a permit from this `RateLimiter` if it can be obtained without exceeding the
     * specified `timeout`, or returns `false` immediately (without waiting) if the permit
     * would not have been granted before the timeout expired.
     *
     * This method is equivalent to `tryAcquire(1, timeout, unit)`.
     *
     * @param timeout the maximum time to wait for the permit. Negative values are treated as zero.
     * @param unit the time unit of the timeout argument
     * @return `true` if the permit was acquired, `false` otherwise
     * @throws IllegalArgumentException if the requested number of permits is negative or zero
     */
    open fun tryAcquire(timeout: Long, unit: TimeUnit): Boolean = tryAcquire(1, timeout, unit)

    /**
     * Acquires permits from this [RateLimiter] if it can be acquired immediately without delay.
     *
     * This method is equivalent to `tryAcquire(permits, 0, anyUnit)`.
     *
     * @param permits the number of permits to acquire
     * @return `true` if the permits were acquired, `false` otherwise
     * @throws IllegalArgumentException if the requested number of permits is negative or zero
     * @since 14.0
     */
    open fun tryAcquire(permits: Int): Boolean = tryAcquire(permits, 0, MICROSECONDS)

    /**
     * Acquires a permit from this [RateLimiter] if it can be acquired immediately without
     * delay.
     *
     * This method is equivalent to `tryAcquire(1)`.
     *
     * @return `true` if the permit was acquired, `false` otherwise
     * @since 14.0
     */
    open fun tryAcquire(): Boolean = tryAcquire(1, 0, MICROSECONDS)

    /**
     * Acquires the given number of permits from this `RateLimiter` if it can be obtained
     * without exceeding the specified `timeout`, or returns `false` immediately (without
     * waiting) if the permits would not have been granted before the timeout expired.
     *
     * @param permits the number of permits to acquire
     * @param timeout the maximum time to wait for the permits. Negative values are treated as zero.
     * @return `true` if the permits were acquired, `false` otherwise
     * @throws IllegalArgumentException if the requested number of permits is negative or zero
     * @since 28.0 (but only since 33.4.0 in the Android flavor)
     */
    open fun tryAcquire(permits: Int, timeout: Duration): Boolean =
        tryAcquire(permits, timeout.toNanos(), NANOSECONDS)

    /**
     * Acquires the given number of permits from this `RateLimiter` if it can be obtained
     * without exceeding the specified `timeout`, or returns `false` immediately (without
     * waiting) if the permits would not have been granted before the timeout expired.
     *
     * @param permits the number of permits to acquire
     * @param timeout the maximum time to wait for the permits. Negative values are treated as zero.
     * @param unit the time unit of the timeout argument
     * @return `true` if the permits were acquired, `false` otherwise
     * @throws IllegalArgumentException if the requested number of permits is negative or zero
     */
    open fun tryAcquire(permits: Int, timeout: Long, unit: TimeUnit): Boolean {
        val timeoutMicros = max(unit.toMicros(timeout), 0)
        checkPermits(permits)
        val microsToWait: Long
        synchronized(mutex()) {
            val nowMicros = stopwatch.readMicros()
            if (!canAcquire(nowMicros, timeoutMicros)) {
                return false
            }
            microsToWait = reserveAndGetWaitLength(permits, nowMicros)
        }
        stopwatch.sleepMicrosUninterruptibly(microsToWait)
        return true
    }

    private fun canAcquire(nowMicros: Long, timeoutMicros: Long): Boolean =
        queryEarliestAvailable(nowMicros) - timeoutMicros <= nowMicros

    /**
     * Reserves next ticket and returns the wait time that the caller must wait for.
     *
     * @return the required wait time, never negative
     */
    internal fun reserveAndGetWaitLength(permits: Int, nowMicros: Long): Long {
        val momentAvailable = reserveEarliestAvailable(permits, nowMicros)
        return max(momentAvailable - nowMicros, 0)
    }

    /**
     * Returns the earliest time that permits are available (with one caveat).
     *
     * @return the time that permits are available, or, if permits are available immediately, an
     *     arbitrary past or present time
     */
    internal abstract fun queryEarliestAvailable(nowMicros: Long): Long

    /**
     * Reserves the requested number of permits and returns the time that those permits can be used
     * (with one caveat).
     *
     * @return the time that the permits may be used, or, if the permits may be used immediately, an
     *     arbitrary past or present time
     */
    internal abstract fun reserveEarliestAvailable(permits: Int, nowMicros: Long): Long

    override fun toString(): String =
        String.format(Locale.ROOT, "RateLimiter[stableRate=%3.1fqps]", getRate())

    internal abstract class SleepingStopwatch {
        internal abstract fun readMicros(): Long

        internal abstract fun sleepMicrosUninterruptibly(micros: Long)

        companion object {
            fun createFromSystemTimer(): SleepingStopwatch = object : SleepingStopwatch() {
                val startTimeNano = System.nanoTime()

                override fun readMicros(): Long =
                    MICROSECONDS.convert(System.nanoTime() - startTimeNano, NANOSECONDS)

                override fun sleepMicrosUninterruptibly(micros: Long) {
                    if (micros > 0) {
                        val sleepMs = MICROSECONDS.toMillis(micros)
                        if (sleepMs > 0) {
                            try {
                                Thread.sleep(sleepMs)
                            } catch (iex: InterruptedException) {
                                Thread.currentThread().interrupt()
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val DEFAULT_COLD_FACTOR = 3.0

        /**
         * Creates a `RateLimiter` with the specified stable throughput, given as "permits per
         * second" (commonly referred to as *QPS*, queries per second), and a *warmup period*,
         * during which the `RateLimiter` smoothly ramps up its rate, until it reaches its maximum
         * rate at the end of the period (as long as there are enough requests to saturate it).
         * Similarly, if the `RateLimiter` is left *unused* for a duration of `warmupPeriod`, it
         * will gradually return to its "cold" state, i.e. it will go through the same warming up
         * process as when it was first created.
         *
         * The returned `RateLimiter` is intended for cases where the resource that actually
         * fulfills the requests (e.g., a remote server) needs "warmup" time, rather than being
         * immediately accessed at the stable (maximum) rate.
         *
         * The returned `RateLimiter` starts in a "cold" state (i.e. the warmup period will
         * follow), and if it is left unused for long enough, it will return to that state.
         *
         * @param permitsPerSecond the rate of the returned `RateLimiter`, measured in how many
         *     permits become available per second
         * @param warmupPeriod the duration of the period where the `RateLimiter` ramps up its
         *     rate, before reaching its stable (maximum) rate
         * @throws IllegalArgumentException if `permitsPerSecond` is negative or zero or
         *     `warmupPeriod` is negative
         * @since 28.0 (but only since 33.4.0 in the Android flavor)
         */
        @JvmStatic
        fun create(permitsPerSecond: Double, warmupPeriod: Duration): RateLimiter =
            create(permitsPerSecond, warmupPeriod, DEFAULT_COLD_FACTOR)

        /**
         * Same as [create] with a [Duration] warmup period, with a custom cold factor.
         *
         * @param permitsPerSecond the rate of the returned `RateLimiter`, measured in how many
         *     permits become available per second
         * @param warmupPeriod the duration of the period where the `RateLimiter` ramps up its
         *     rate, before reaching its stable (maximum) rate
         * @param coldFactor see [SmoothWarmingUp]
         * @throws IllegalArgumentException if `permitsPerSecond` is negative or zero or
         *     `warmupPeriod` is negative
         * @since 28.0 (but only since 33.4.0 in the Android flavor)
         */
        @JvmStatic
        fun create(permitsPerSecond: Double, warmupPeriod: Duration, coldFactor: Double): RateLimiter =
            create(permitsPerSecond, warmupPeriod.toNanos(), NANOSECONDS, coldFactor)

        /**
         * Same as [create] with a [Duration] warmup period, with the warmup period given as a
         * number of [unit].
         *
         * @param permitsPerSecond the rate of the returned `RateLimiter`, measured in how many
         *     permits become available per second
         * @param warmupPeriod the duration of the period where the `RateLimiter` ramps up its
         *     rate, before reaching its stable (maximum) rate
         * @param unit the time unit of the warmupPeriod argument
         * @param coldFactor see [SmoothWarmingUp]
         * @throws IllegalArgumentException if `permitsPerSecond` is negative or zero or
         *     `warmupPeriod` is negative
         */
        @JvmStatic
        fun create(
            permitsPerSecond: Double,
            warmupPeriod: Long,
            unit: TimeUnit,
            coldFactor: Double,
        ): RateLimiter {
            require(warmupPeriod >= 0) { "warmupPeriod must not be negative: $warmupPeriod" }
            return create(
                permitsPerSecond,
                warmupPeriod,
                unit,
                coldFactor,
                SleepingStopwatch.createFromSystemTimer(),
            )
        }

        internal fun create(
            permitsPerSecond: Double,
            warmupPeriod: Long,
            unit: TimeUnit,
            coldFactor: Double,
            stopwatch: SleepingStopwatch,
        ): RateLimiter {
            val rateLimiter: RateLimiter = SmoothWarmingUp(stopwatch, warmupPeriod, unit, coldFactor)
            rateLimiter.setRate(permitsPerSecond)
            return rateLimiter
        }

        private fun checkPermits(permits: Int) {
            require(permits > 0) { "Requested permits ($permits) must be positive" }
        }
    }
}

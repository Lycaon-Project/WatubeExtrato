package org.schabi.newpipe.extractor.utils

import java.time.Duration

/**
 * A view of a string which can only be read until a deadline.
 *
 * Java's regex engine has no time limit of its own and reads its input through
 * [get] (`charAt`): matching a pattern against this view instead of the string itself
 * stops a pattern which backtracks without end on an unexpected input, by throwing a
 * [TimeoutException], instead of keeping a thread busy for minutes.
 *
 * A view is meant to be read by one thread at a time.
 */
public class TimeLimitedCharSequence private constructor(
    private val source: String,
    private val start: Int,
    private val end: Int,
    private val deadlineNanos: Long,
) : CharSequence {

    private var readsBeforeCheck = READS_BETWEEN_CHECKS

    /**
     * @param source the string to read
     * @param budget how long it can be read from now on
     */
    public constructor(source: String, budget: Duration) :
        this(source, 0, source.length, System.nanoTime() + budget.toNanos())

    override val length: Int
        get() = end - start

    override fun get(index: Int): Char {
        if (--readsBeforeCheck <= 0) {
            readsBeforeCheck = READS_BETWEEN_CHECKS
            if (System.nanoTime() - deadlineNanos > 0) {
                throw TimeoutException()
            }
        }
        if (index < 0 || index >= length) {
            // not IndexOutOfBoundsException(int): Java 16 API, only on Android 14 (API 34)+
            throw IndexOutOfBoundsException("Index out of range: $index")
        }
        return source[start + index]
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        if (startIndex < 0 || endIndex > length || startIndex > endIndex) {
            throw IndexOutOfBoundsException("from $startIndex to $endIndex")
        }
        return TimeLimitedCharSequence(source, start + startIndex, start + endIndex, deadlineNanos)
    }

    override fun toString(): String = source.substring(start, end)

    /** Thrown when the view is read after its deadline. */
    public class TimeoutException internal constructor() :
        RuntimeException("Reading took longer than allowed") {
        private companion object {
            private const val serialVersionUID = 1L
        }
    }

    private companion object {
        /** The clock is only read once every so many characters, to keep reads cheap. */
        const val READS_BETWEEN_CHECKS = 4096
    }
}

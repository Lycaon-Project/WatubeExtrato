package org.schabi.newpipe.extractor.utils;

import java.time.Duration;

import javax.annotation.Nonnull;

/**
 * A view of a string which can only be read until a deadline.
 *
 * <p>
 * Java's regex engine has no time limit of its own and reads its input through
 * {@link #charAt(int)}: matching a pattern against this view instead of the string itself
 * stops a pattern which backtracks without end on an unexpected input, by throwing a
 * {@link TimeoutException}, instead of keeping a thread busy for minutes.
 * </p>
 *
 * <p>
 * A view is meant to be read by one thread at a time.
 * </p>
 */
public final class TimeLimitedCharSequence implements CharSequence {

    /** The clock is only read once every so many characters, to keep reads cheap. */
    private static final int READS_BETWEEN_CHECKS = 4096;

    @Nonnull
    private final String source;
    private final int start;
    private final int end;
    private final long deadlineNanos;
    private int readsBeforeCheck = READS_BETWEEN_CHECKS;

    /**
     * @param source the string to read
     * @param budget how long it can be read from now on
     */
    public TimeLimitedCharSequence(@Nonnull final String source, @Nonnull final Duration budget) {
        this(source, 0, source.length(), System.nanoTime() + budget.toNanos());
    }

    private TimeLimitedCharSequence(@Nonnull final String source,
                                    final int start,
                                    final int end,
                                    final long deadlineNanos) {
        this.source = source;
        this.start = start;
        this.end = end;
        this.deadlineNanos = deadlineNanos;
    }

    @Override
    public int length() {
        return end - start;
    }

    @Override
    public char charAt(final int index) {
        if (--readsBeforeCheck <= 0) {
            readsBeforeCheck = READS_BETWEEN_CHECKS;
            if (System.nanoTime() - deadlineNanos > 0) {
                throw new TimeoutException();
            }
        }
        if (index < 0 || index >= length()) {
            throw new IndexOutOfBoundsException(index);
        }
        return source.charAt(start + index);
    }

    @Nonnull
    @Override
    public CharSequence subSequence(final int from, final int to) {
        if (from < 0 || to > length() || from > to) {
            throw new IndexOutOfBoundsException("from " + from + " to " + to);
        }
        return new TimeLimitedCharSequence(source, start + from, start + to, deadlineNanos);
    }

    @Nonnull
    @Override
    public String toString() {
        return source.substring(start, end);
    }

    /** Thrown when the view is read after its deadline. */
    public static final class TimeoutException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        TimeoutException() {
            super("Reading took longer than allowed");
        }
    }
}

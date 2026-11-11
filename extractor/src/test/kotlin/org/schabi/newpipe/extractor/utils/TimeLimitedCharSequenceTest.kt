package org.schabi.newpipe.extractor.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.regex.Pattern

/**
 * A regex reading a [TimeLimitedCharSequence] gives up at the deadline instead of
 * backtracking for minutes, and reads it like the string itself until then.
 */
internal class TimeLimitedCharSequenceTest {

    @Test
    fun stopsABacktrackingPatternAtTheDeadline() {
        // the shape of the player regexes: on one long line without a match, every "{" is
        // tried for every "=function", each time up to the end of the line
        val catastrophic = Pattern.compile("(\\w*)=function(.*)\\{.*set\\(\"alr\",\"yes\"\\);\\w&&")
        val input: CharSequence = TimeLimitedCharSequence(
            "x=function(a){b}".repeat(5_000),
            Duration.ofMillis(200),
        )

        assertTimeoutPreemptively(Duration.ofSeconds(10)) {
            assertThrows(TimeLimitedCharSequence.TimeoutException::class.java) {
                catastrophic.matcher(input).find()
            }
        }
    }

    @Test
    fun matchesLikeTheStringBeforeTheDeadline() {
        val text = "var abc=function(a){return a.split(\"\")};"
        val matcher = Pattern.compile("var (\\w+)=function")
            .matcher(TimeLimitedCharSequence(text, Duration.ofSeconds(10)))

        assertTrue(matcher.find())
        assertEquals("abc", matcher.group(1))
    }

    @Test
    fun subSequenceReadsTheSameCharacters() {
        val sequence: CharSequence = TimeLimitedCharSequence("0123456789", Duration.ofSeconds(10))

        assertEquals("345", sequence.subSequence(3, 6).toString())
        assertEquals('4', sequence.subSequence(3, 6)[1])
        assertThrows(IndexOutOfBoundsException::class.java) { sequence.subSequence(6, 3) }
        assertThrows(IndexOutOfBoundsException::class.java) { sequence[10] }
    }
}

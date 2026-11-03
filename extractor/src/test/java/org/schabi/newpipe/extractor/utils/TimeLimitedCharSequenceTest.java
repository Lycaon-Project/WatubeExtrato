package org.schabi.newpipe.extractor.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A regex reading a {@link TimeLimitedCharSequence} gives up at the deadline instead of
 * backtracking for minutes, and reads it like the string itself until then.
 */
class TimeLimitedCharSequenceTest {

    @Test
    void stopsABacktrackingPatternAtTheDeadline() {
        // the shape of the player regexes: on one long line without a match, every "{" is
        // tried for every "=function", each time up to the end of the line
        final Pattern catastrophic =
                Pattern.compile("(\\w*)=function(.*)\\{.*set\\(\"alr\",\"yes\"\\);\\w&&");
        final CharSequence input = new TimeLimitedCharSequence(
                "x=function(a){b}".repeat(5_000), Duration.ofMillis(200));

        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> assertThrows(
                TimeLimitedCharSequence.TimeoutException.class,
                () -> catastrophic.matcher(input).find()));
    }

    @Test
    void matchesLikeTheStringBeforeTheDeadline() {
        final String text = "var abc=function(a){return a.split(\"\")};";
        final Matcher matcher = Pattern.compile("var (\\w+)=function")
                .matcher(new TimeLimitedCharSequence(text, Duration.ofSeconds(10)));

        assertTrue(matcher.find());
        assertEquals("abc", matcher.group(1));
    }

    @Test
    void subSequenceReadsTheSameCharacters() {
        final CharSequence sequence =
                new TimeLimitedCharSequence("0123456789", Duration.ofSeconds(10));

        assertEquals("345", sequence.subSequence(3, 6).toString());
        assertEquals('4', sequence.subSequence(3, 6).charAt(1));
        assertThrows(IndexOutOfBoundsException.class, () -> sequence.subSequence(6, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> sequence.charAt(10));
    }
}

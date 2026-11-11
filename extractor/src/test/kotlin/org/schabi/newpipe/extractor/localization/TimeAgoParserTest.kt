package org.schabi.newpipe.extractor.localization

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import java.time.LocalDateTime
import java.time.Month
import java.time.temporal.ChronoUnit
import java.util.function.Function
import java.util.stream.Stream

internal class TimeAgoParserTest {

    @ParameterizedTest
    @MethodSource
    fun parseTimeAgo(testData: ParseTimeAgoTestData) {
        val now = LocalDateTime.of(2020, Month.JANUARY, 1, 1, 1, 1)
        val parser = TimeAgoPatternsManager.getTimeAgoParserFor(Localization.DEFAULT, now)!!
        val expected = testData.expectedApplyToNow.apply(now)

        assertAll(
            Stream.of(testData.textualDateLong, testData.textualDateShort)
                .map { textualDate ->
                    Executable {
                        assertEquals(
                            expected,
                            parser.parse(textualDate).localDateTime,
                            "Expected $expected for $textualDate",
                        )
                    }
                },
        )
    }

    class ParseTimeAgoTestData(
        val expectedApplyToNow: Function<LocalDateTime, LocalDateTime>,
        val textualDateLong: String,
        val textualDateShort: String,
    ) {
        companion object {
            const val AGO_SUFFIX = " ago"

            fun lessThanDay(
                duration: Duration,
                textualDateLong: String,
                textualDateShort: String,
            ): ParseTimeAgoTestData = ParseTimeAgoTestData(
                Function { d -> d.minus(duration) },
                textualDateLong + AGO_SUFFIX,
                textualDateShort + AGO_SUFFIX,
            )

            fun greaterThanDay(
                expectedApplyToNow: Function<LocalDateTime, LocalDateTime>,
                textualDateLong: String,
                textualDateShort: String,
            ): ParseTimeAgoTestData = ParseTimeAgoTestData(
                expectedApplyToNow.andThen { d -> d.truncatedTo(ChronoUnit.DAYS) },
                textualDateLong + AGO_SUFFIX,
                textualDateShort + AGO_SUFFIX,
            )
        }
    }

    companion object {
        @JvmStatic
        fun parseTimeAgo(): Stream<Arguments> = Stream.of(
            ParseTimeAgoTestData.lessThanDay(Duration.ofSeconds(1), "1 second", "1 sec"),
            ParseTimeAgoTestData.lessThanDay(Duration.ofSeconds(12), "12 second", "12 sec"),
            ParseTimeAgoTestData.lessThanDay(Duration.ofMinutes(1), "1 minute", "1 min"),
            ParseTimeAgoTestData.lessThanDay(Duration.ofMinutes(23), "23 minutes", "23 min"),
            ParseTimeAgoTestData.lessThanDay(Duration.ofHours(1), "1 hour", "1 hr"),
            ParseTimeAgoTestData.lessThanDay(Duration.ofHours(8), "8 hour", "8 hr"),
            ParseTimeAgoTestData.greaterThanDay({ d -> d.minusDays(1) }, "1 day", "1 day"),
            ParseTimeAgoTestData.greaterThanDay({ d -> d.minusDays(3) }, "3 days", "3 day"),
            ParseTimeAgoTestData.greaterThanDay({ d -> d.minusWeeks(1) }, "1 week", "1 wk"),
            ParseTimeAgoTestData.greaterThanDay({ d -> d.minusWeeks(3) }, "3 weeks", "3 wk"),
            ParseTimeAgoTestData.greaterThanDay({ d -> d.minusMonths(1) }, "1 month", "1 mo"),
            ParseTimeAgoTestData.greaterThanDay({ d -> d.minusMonths(3) }, "3 months", "3 mo"),
            ParseTimeAgoTestData.greaterThanDay(
                { d -> d.minusYears(1).minusDays(1) },
                "1 year",
                "1 yr",
            ),
            ParseTimeAgoTestData.greaterThanDay(
                { d -> d.minusYears(3).minusDays(1) },
                "3 years",
                "3 yr",
            ),
        ).map { Arguments.of(it) }
    }
}

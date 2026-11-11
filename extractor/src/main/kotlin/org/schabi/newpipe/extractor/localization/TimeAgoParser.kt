package org.schabi.newpipe.extractor.localization

import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.timeago.PatternsHolder
import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * A helper class that is meant to be used by services that need to parse durations such as
 * `23 seconds` and/or upload dates in the format `2 days ago` or similar.
 *
 * Creates a helper to parse upload dates in the format '2 days ago'.
 *
 * Instantiate a new [TimeAgoParser] every time you extract a new batch of items.
 *
 * @param patternsHolder An object that holds the "time ago" patterns, special cases, and the
 * language word separator.
 * @param now            The current time
 */
public open class TimeAgoParser(
    private val patternsHolder: PatternsHolder,
    private val now: LocalDateTime,
) {

    /**
     * Parses a textual date in the format '2 days ago' into a Calendar representation which is
     * then wrapped in a [DateWrapper] object.
     *
     * Beginning with days ago, the date is considered as an approximation.
     *
     * The text comes from the network: it is length-bounded (every phrase of the language is
     * looked for in it) and never quoted in the exception.
     *
     * @param textualDate The original date as provided by the streaming service
     * @return The parsed time (can be approximated)
     * @throws ParsingException if the time unit could not be recognized
     */
    @Throws(ParsingException::class)
    public fun parse(textualDate: String): DateWrapper {
        if (textualDate.length > MAX_TEXTUAL_DATE_LENGTH) {
            throw ParsingException(UNPARSABLE_DATE)
        }
        // Locale.ROOT, as for the phrases: a Turkish locale would turn "I" into a dotless "ı"
        val lowerCaseDate = textualDate.lowercase(Locale.ROOT)
        for ((chronoUnit, caseMapToAmount) in patternsHolder.specialCases()) {
            for ((caseText, caseAmount) in caseMapToAmount) {
                if (textualDateMatches(textualDate, lowerCaseDate, caseText)) {
                    return getResultFor(caseAmount, chronoUnit)
                }
            }
        }

        return getResultFor(
            parseTimeAgoAmount(textualDate),
            parseChronoUnit(textualDate, lowerCaseDate),
        )
    }

    /**
     * The number in the textual date. If there is no valid number in it, assume it is 1 (as in
     * 'a second ago').
     */
    @Throws(ParsingException::class)
    private fun parseTimeAgoAmount(textualDate: String): Int {
        val digits = NON_DIGITS.replace(textualDate, "")
        if (digits.isEmpty()) {
            return 1
        }
        // a number too large for any real date is an error, not "1"
        return digits.toIntOrNull() ?: throw ParsingException("Time amount out of range")
    }

    @Throws(ParsingException::class)
    private fun parseChronoUnit(textualDate: String, lowerCaseDate: String): ChronoUnit {
        for ((chronoUnit, agoPhrases) in patternsHolder.asMap()) {
            if (agoPhrases.any { textualDateMatches(textualDate, lowerCaseDate, it) }) {
                return chronoUnit
            }
        }
        throw ParsingException(UNPARSABLE_DATE)
    }

    private fun textualDateMatches(
        textualDate: String,
        lowerCaseDate: String,
        agoPhrase: String,
    ): Boolean {
        if (textualDate == agoPhrase) {
            return true
        }

        val separator = patternsHolder.wordSeparator()
        val lowerCasePhrase = agoPhrase.lowercase(Locale.ROOT)
        if (separator.isEmpty()) {
            return lowerCaseDate.contains(lowerCasePhrase)
        }

        return PHRASE_PATTERNS.computeIfAbsent(separator + '\u0000' + lowerCasePhrase) {
            phrasePattern(separator, lowerCasePhrase)
        }.matcher(lowerCaseDate).find()
    }

    /**
     * A huge amount from the network (e.g. "999999999 years ago") would overflow the date:
     * it is reported as a parsing error instead of crashing the extraction.
     */
    @Throws(ParsingException::class)
    private fun getResultFor(timeAgoAmount: Int, chronoUnit: ChronoUnit): DateWrapper {
        try {
            val localDateTime = if (chronoUnit == ChronoUnit.YEARS) {
                // minusDays is needed to prevent `PrettyTime` from showing '12 months ago'.
                now.minusYears(timeAgoAmount.toLong()).minusDays(1)
            } else {
                now.minus(timeAgoAmount.toLong(), chronoUnit)
            }
            val isApproximate = chronoUnit.isDateBased
            val resolvedDateTime =
                if (isApproximate) localDateTime.truncatedTo(ChronoUnit.DAYS) else localDateTime
            return DateWrapper(resolvedDateTime, isApproximate)
        } catch (_: DateTimeException) {
            throw ParsingException(UNPARSABLE_DATE)
        } catch (_: ArithmeticException) {
            throw ParsingException(UNPARSABLE_DATE)
        }
    }

    private companion object {
        private const val UNPARSABLE_DATE = "Unable to parse the date"

        /** Real "time ago" texts are a few dozen characters long. */
        private const val MAX_TEXTUAL_DATE_LENGTH = 1_000

        private val NON_DIGITS = Regex("\\D+")

        // parse() runs for every item of a list and tries every phrase of the language: each
        // phrase pattern is compiled once, not once per item and phrase. The keys only come from
        // the bundled patterns, so the map stays small.
        private val PHRASE_PATTERNS = ConcurrentHashMap<String, Pattern>()

        private fun phrasePattern(separator: String, lowerCasePhrase: String): Pattern {
            val escapedPhrase = Pattern.quote(lowerCasePhrase)
            val escapedSeparator = if (separator == " ") {
                // From JDK8 → \h - Treat horizontal spaces as a normal one
                // (non-breaking space, thin space, etc.)
                // Also split the string on numbers to be able to parse strings like "2wk"
                "[ \\t\\xA0\\u1680\\u180e\\u2000-\\u200a\\u202f\\u205f\\u3000\\d]"
            } else {
                Pattern.quote(separator)
            }

            // (^|separator)pattern($|separator)
            // Check if the pattern is surrounded by separators or start/end of the string.
            return Pattern.compile(
                "(^|" + escapedSeparator + ")" + escapedPhrase + "(\$|" + escapedSeparator + ")",
            )
        }
    }
}

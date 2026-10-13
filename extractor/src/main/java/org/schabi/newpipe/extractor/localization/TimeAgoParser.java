package org.schabi.newpipe.extractor.localization;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.timeago.PatternsHolder;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * A helper class that is meant to be used by services that need to parse durations such as
 * {@code 23 seconds} and/or upload dates in the format {@code 2 days ago} or similar.
 */
public class TimeAgoParser {
    // parse() runs for every item of a list and tries every phrase of the language: each phrase
    // pattern is compiled once, not once per item and phrase
    private static final Map<String, Pattern> PHRASE_PATTERNS = new ConcurrentHashMap<>();

    private final PatternsHolder patternsHolder;
    private final LocalDateTime now;

    /**
     * Creates a helper to parse upload dates in the format '2 days ago'.
     * <p>
     * Instantiate a new {@link TimeAgoParser} every time you extract a new batch of items.
     * </p>
     *
     * @param patternsHolder An object that holds the "time ago" patterns, special cases, and the
     *                       language word separator.
     * @param now            The current time
     */
    public TimeAgoParser(final PatternsHolder patternsHolder, final LocalDateTime now) {
        this.patternsHolder = patternsHolder;
        this.now = now;
    }

    /**
     * Parses a textual date in the format '2 days ago' into a Calendar representation which is then
     * wrapped in a {@link DateWrapper} object.
     * <p>
     * Beginning with days ago, the date is considered as an approximation.
     *
     * @param textualDate The original date as provided by the streaming service
     * @return The parsed time (can be approximated)
     * @throws ParsingException if the time unit could not be recognized
     */
    public DateWrapper parse(final String textualDate) throws ParsingException {
        final String lowerCaseDate = textualDate.toLowerCase();
        for (final var caseUnitEntry : patternsHolder.specialCases().entrySet()) {
            final ChronoUnit chronoUnit = caseUnitEntry.getKey();
            for (final var caseMapToAmountEntry : caseUnitEntry.getValue().entrySet()) {
                final String caseText = caseMapToAmountEntry.getKey();
                final int caseAmount = caseMapToAmountEntry.getValue();

                if (textualDateMatches(textualDate, lowerCaseDate, caseText)) {
                    return getResultFor(caseAmount, chronoUnit);
                }
            }
        }

        return getResultFor(parseTimeAgoAmount(textualDate),
                parseChronoUnit(textualDate, lowerCaseDate));
    }

    private int parseTimeAgoAmount(final String textualDate) {
        try {
            return Integer.parseInt(textualDate.replaceAll("\\D+", ""));
        } catch (final NumberFormatException ignored) {
            // If there is no valid number in the textual date,
            // assume it is 1 (as in 'a second ago').
            return 1;
        }
    }

    private ChronoUnit parseChronoUnit(final String textualDate, final String lowerCaseDate)
            throws ParsingException {
        return patternsHolder.asMap().entrySet().stream()
                .filter(e -> e.getValue().stream()
                        .anyMatch(agoPhrase ->
                                textualDateMatches(textualDate, lowerCaseDate, agoPhrase)))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() ->
                        new ParsingException("Unable to parse the date: " + textualDate));
    }

    private boolean textualDateMatches(final String textualDate,
                                       final String lowerCaseDate,
                                       final String agoPhrase) {
        if (textualDate.equals(agoPhrase)) {
            return true;
        }

        final String separator = patternsHolder.wordSeparator();
        final String lowerCasePhrase = agoPhrase.toLowerCase();
        if (separator.isEmpty()) {
            return lowerCaseDate.contains(lowerCasePhrase);
        }

        return PHRASE_PATTERNS.computeIfAbsent(separator + '\u0000' + lowerCasePhrase,
                key -> phrasePattern(separator, lowerCasePhrase)).matcher(lowerCaseDate).find();
    }

    private static Pattern phrasePattern(final String separator, final String lowerCasePhrase) {
        final String escapedPhrase = Pattern.quote(lowerCasePhrase);
        final String escapedSeparator = separator.equals(" ")
                // From JDK8 → \h - Treat horizontal spaces as a normal one
                // (non-breaking space, thin space, etc.)
                // Also split the string on numbers to be able to parse strings like "2wk"
                ? "[ \\t\\xA0\\u1680\\u180e\\u2000-\\u200a\\u202f\\u205f\\u3000\\d]"
                : Pattern.quote(separator);

        // (^|separator)pattern($|separator)
        // Check if the pattern is surrounded by separators or start/end of the string.
        return Pattern.compile(
                "(^|" + escapedSeparator + ")" + escapedPhrase + "($|" + escapedSeparator + ")");
    }

    private DateWrapper getResultFor(final int timeAgoAmount, final ChronoUnit chronoUnit) {
        final var localDateTime = chronoUnit == ChronoUnit.YEARS
                // minusDays is needed to prevent `PrettyTime` from showing '12 months ago'.
                ? now.minusYears(timeAgoAmount).minusDays(1)
                : now.minus(timeAgoAmount, chronoUnit);
        final boolean isApproximate = chronoUnit.isDateBased();
        final var resolvedDateTime =
                isApproximate ? localDateTime.truncatedTo(ChronoUnit.DAYS) : localDateTime;
        return new DateWrapper(resolvedDateTime, isApproximate);
    }
}

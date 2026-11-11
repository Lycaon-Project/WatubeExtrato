package org.schabi.newpipe.extractor.localization

import org.schabi.newpipe.extractor.exceptions.ParsingException
import java.io.Serializable
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

/**
 * A wrapper class that provides a field to describe if the date/time is precise or just an
 * approximation.
 */
public open class DateWrapper @JvmOverloads constructor(
    /** The wrapped [Instant]. */
    public val instant: Instant,
    /**
     * If the date is considered is precise or just an approximation (e.g. service only
     * returns an approximation like 2 weeks ago instead of a precise date).
     */
    public val isApproximation: Boolean = false,
) : Serializable {

    public constructor(offsetDateTime: OffsetDateTime) : this(offsetDateTime, false)

    public constructor(offsetDateTime: OffsetDateTime, isApproximation: Boolean) :
        this(offsetDateTime.toInstant(), isApproximation)

    public constructor(dateTime: LocalDateTime, isApproximation: Boolean) :
        this(dateTime.atZone(ZoneId.systemDefault()).toInstant(), isApproximation)

    /**
     * @return the wrapped [Instant] as an [OffsetDateTime] set to UTC.
     */
    public fun offsetDateTime(): OffsetDateTime = instant.atOffset(ZoneOffset.UTC)

    /**
     * The wrapped [Instant] as a [LocalDateTime] in the current time zone.
     */
    public val localDateTime: LocalDateTime
        get() = getLocalDateTime(ZoneId.systemDefault())

    /**
     * @return the wrapped [Instant] as a [LocalDateTime] in the given time zone.
     */
    public fun getLocalDateTime(zoneId: ZoneId): LocalDateTime = LocalDateTime.ofInstant(instant, zoneId)

    override fun toString(): String = "DateWrapper{instant=$instant, isApproximation=$isApproximation}"

    public companion object {
        private const val serialVersionUID: Long = 1L

        /**
         * Parses a date string that matches the ISO-8601 [OffsetDateTime] pattern, e.g.
         * "2011-12-03T10:15:30+01:00".
         *
         * The date comes from the network: neither it nor the parser error (which quotes it) is
         * kept in the exception.
         *
         * @param date The date string
         * @return a non-approximate [DateWrapper], or null if the string is null
         * @throws ParsingException if the string does not match the expected format
         */
        @JvmStatic
        @Throws(ParsingException::class)
        public fun fromOffsetDateTime(date: String?): DateWrapper? = try {
            date?.let { DateWrapper(OffsetDateTime.parse(it)) }
        } catch (_: DateTimeParseException) {
            throw ParsingException("Could not parse date")
        }

        /**
         * Parses a date string that matches the ISO-8601 [Instant] pattern, e.g.
         * "2011-12-03T10:15:30Z".
         *
         * @param date The date string
         * @return a non-approximate [DateWrapper], or null if the string is null
         * @throws ParsingException if the string does not match the expected format
         */
        @JvmStatic
        @Throws(ParsingException::class)
        public fun fromInstant(date: String?): DateWrapper? = try {
            date?.let { DateWrapper(Instant.parse(it)) }
        } catch (_: DateTimeParseException) {
            throw ParsingException("Could not parse date")
        }
    }
}

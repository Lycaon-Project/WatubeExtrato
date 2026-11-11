/*
 * Created by Christian Schabesberger on 10.08.18.
 *
 * Copyright (C) 2016 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * StreamExtractor.java is part of NewPipe Extractor.
 *
 * NewPipe Extractor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe Extractor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe Extractor.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.schabi.newpipe.extractor.stream

import org.schabi.newpipe.extractor.Extractor
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.InfoItemExtractor
import org.schabi.newpipe.extractor.InfoItemsCollector
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.MetaInfo
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.channel.ChannelExtractor
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.LinkHandler
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.utils.Parser
import java.io.IOException
import java.util.Locale

/**
 * Scrapes information from a video/audio streaming service (eg, YouTube).
 */
public abstract class StreamExtractor(
    service: StreamingService,
    linkHandler: LinkHandler,
) : Extractor(service, linkHandler) {

    /**
     * The original textual date provided by the service. Should be used as a fallback if
     * [uploadDate] isn't provided by the service, or it fails for some reason.
     *
     * If the stream is a live stream, `null` should be returned.
     *
     * @return The original textual date provided by the service, or `null`.
     * @throws ParsingException if there is an error in the extraction
     * @see uploadDate
     */
    @get:Throws(ParsingException::class)
    public open val textualUploadDate: String?
        get() = null

    /**
     * A more general [java.time.Instant] instance set to the date provided by the service.
     * Implementations usually will just parse the date returned from the [textualUploadDate].
     *
     * If the stream is a live stream, `null` should be returned.
     *
     * @return The date this item was uploaded, or `null`.
     * @throws ParsingException if there is an error in the extraction
     * or the extracted date couldn't be parsed.
     * @see textualUploadDate
     */
    @get:Throws(ParsingException::class)
    public open val uploadDate: DateWrapper?
        get() = null

    /**
     * This will return the thumbnails of the stream.
     *
     * @return the thumbnails of the stream
     */
    @get:Throws(ParsingException::class)
    public abstract val thumbnails: List<Image>

    /**
     * This is the stream description.
     *
     * @return The description of the stream/video or [Description.EMPTY_DESCRIPTION] if the
     * description is empty.
     */
    @get:Throws(ParsingException::class)
    public open val description: Description
        get() = Description.EMPTY_DESCRIPTION

    /**
     * Get the age limit.
     *
     * @return The age which limits the content or [NO_AGE_LIMIT] if there is no limit
     * @throws ParsingException if an error occurs while parsing
     */
    @get:Throws(ParsingException::class)
    public open val ageLimit: Int
        get() = NO_AGE_LIMIT

    /**
     * This should return the length of a video in seconds.
     *
     * @return The length of the stream in seconds or 0 when it has no length (e.g. a livestream).
     */
    @get:Throws(ParsingException::class)
    public open val length: Long
        get() = 0

    /**
     * If the url you are currently handling contains a time stamp/seek, you can return the
     * position it represents here.
     * If the url has no time stamp simply return zero.
     *
     * @return the timestamp in seconds or 0 when there is no timestamp
     */
    @get:Throws(ParsingException::class)
    public open val timeStamp: Long
        get() = 0

    /**
     * The count of how many people have watched the video/listened to the audio stream.
     * If the current stream has no view count or its not available simply return -1
     *
     * @return amount of views or -1 if not available.
     */
    @get:Throws(ParsingException::class)
    public open val viewCount: Long
        get() = -1

    /**
     * The amount of likes a video/audio stream got.
     * If the current stream has no likes or its not available simply return -1
     *
     * @return the amount of likes the stream got or -1 if not available.
     */
    @get:Throws(ParsingException::class)
    public open val likeCount: Long
        get() = -1

    /**
     * The amount of dislikes a video/audio stream got.
     * If the current stream has no dislikes or its not available simply return -1
     *
     * @return the amount of likes the stream got or -1 if not available.
     */
    @get:Throws(ParsingException::class)
    public open val dislikeCount: Long
        get() = -1

    /**
     * The Url to the page of the creator/uploader of the stream. This must not be a homepage,
     * but the page offered by the service the extractor handles. This url will be handled by the
     * [ChannelExtractor], so be sure to implement that one before you return a value here,
     * otherwise NewPipe will crash if one selects this url.
     *
     * @return the url to the page of the creator/uploader of the stream or an empty string
     */
    @get:Throws(ParsingException::class)
    public abstract val uploaderUrl: String

    /**
     * The name of the creator/uploader of the stream.
     * If the name is not available you can simply return an empty string.
     *
     * @return the name of the creator/uploader of the stream or an empty tring
     */
    @get:Throws(ParsingException::class)
    public abstract val uploaderName: String

    /**
     * Whether the uploader has been verified by the service's provider.
     * If there is no verification implemented, return `false`.
     *
     * @return whether the uploader has been verified by the service's provider
     */
    @get:Throws(ParsingException::class)
    public open val isUploaderVerified: Boolean
        get() = false

    /**
     * The subscriber count of the uploader.
     * If the subscriber count is not implemented, or is unavailable, return `-1`.
     *
     * @return the subscriber count of the uploader or [UNKNOWN_SUBSCRIBER_COUNT] if not
     * available
     */
    @get:Throws(ParsingException::class)
    public open val uploaderSubscriberCount: Long
        get() = UNKNOWN_SUBSCRIBER_COUNT

    /**
     * The image files/profile pictures/avatars of the creator/uploader of the stream.
     *
     * If they are not available in the stream on specific cases, you must return an empty list for
     * these ones, like it is made by default.
     *
     * @return the avatars of the sub-channel of the stream or an empty list (default)
     */
    @get:Throws(ParsingException::class)
    public open val uploaderAvatars: List<Image>
        get() = emptyList()

    /**
     * The Url to the page of the sub-channel of the stream. This must not be a homepage,
     * but the page offered by the service the extractor handles. This url will be handled by the
     * [ChannelExtractor], so be sure to implement that one before you return a value here,
     * otherwise NewPipe will crash if one selects this url.
     *
     * @return the url to the page of the sub-channel of the stream or an empty String
     */
    @get:Throws(ParsingException::class)
    public open val subChannelUrl: String
        get() = ""

    /**
     * The name of the sub-channel of the stream.
     * If the name is not available you can simply return an empty string.
     *
     * @return the name of the sub-channel of the stream or an empty String
     */
    @get:Throws(ParsingException::class)
    public open val subChannelName: String
        get() = ""

    /**
     * The avatars of the sub-channel of the stream.
     *
     * If they are not available in the stream on specific cases, you must return an empty list for
     * these ones, like it is made by default.
     *
     * If the concept of sub-channels doesn't apply to the stream's service, keep the default
     * implementation.
     *
     * @return the avatars of the sub-channel of the stream or an empty list (default)
     */
    @get:Throws(ParsingException::class)
    public open val subChannelAvatars: List<Image>
        get() = emptyList()

    /**
     * Get the dash mpd url. If you don't know what a dash MPD is you can read about it
     * [here](https://www.brendanlong.com/the-structure-of-an-mpeg-dash-mpd.html).
     *
     * @return the url as a string or an empty string or an empty string if not available
     * @throws ParsingException if an error occurs while reading
     */
    @get:Throws(ParsingException::class)
    public open val dashMpdUrl: String
        get() = ""

    /**
     * I am not sure if this is in use, and how this is used. However the frontend is missing
     * support for HLS streams. Prove me if I am wrong. Please open an
     * [issue](https://github.com/teamnewpipe/newpipe/issues),
     * or fix this description if you know whats up with this.
     *
     * @return The Url to the hls stream or an empty string if not available.
     */
    @get:Throws(ParsingException::class)
    public open val hlsUrl: String
        get() = ""

    /**
     * This should return a list of available [AudioStream]s.
     * You can also return null or an empty list, however be aware that if you don't return anything
     * in getVideoStreams(), getVideoOnlyStreams() and getDashMpdUrl() either the Collector will
     * handle this as a failed extraction procedure.
     *
     * @return a list of audio only streams in the format of AudioStream
     */
    @get:Throws(IOException::class, ExtractionException::class)
    public abstract val audioStreams: List<AudioStream>?

    /**
     * This should return a list of available [VideoStream]s.
     * Be aware this is the list of video streams which do contain an audio stream.
     * You can also return null or an empty list, however be aware that if you don't return anything
     * in getAudioStreams(), getVideoOnlyStreams() and getDashMpdUrl() either the Collector will
     * handle this as a failed extraction procedure.
     *
     * @return a list of combined video and streams in the format of AudioStream
     */
    @get:Throws(IOException::class, ExtractionException::class)
    public abstract val videoStreams: List<VideoStream>?

    /**
     * This should return a list of available [VideoStream]s.
     * Be aware this is the list of video streams which do NOT contain an audio stream.
     * You can also return null or an empty list, however be aware that if you don't return anything
     * in getAudioStreams(), getVideoStreams() and getDashMpdUrl() either the Collector will handle
     * this as a failed extraction procedure.
     *
     * @return a list of video and streams in the format of AudioStream
     */
    @get:Throws(IOException::class, ExtractionException::class)
    public abstract val videoOnlyStreams: List<VideoStream>?

    /**
     * This will return a list of available [SubtitlesStream]s.
     * If no subtitles are available an empty list can be returned.
     *
     * @return a list of available subtitles or an empty list
     */
    @get:Throws(IOException::class, ExtractionException::class)
    public open val subtitlesDefault: List<SubtitlesStream>
        get() = emptyList()

    /**
     * This will return a list of available [SubtitlesStream]s given by a specific type.
     * If no subtitles in that specific format are available an empty list can be returned.
     *
     * @param format the media format by which the subtitles should be filtered
     * @return a list of available subtitles or an empty list
     */
    @Throws(IOException::class, ExtractionException::class)
    public open fun getSubtitles(format: MediaFormat?): List<SubtitlesStream> = emptyList()

    /**
     * Get the [StreamType].
     *
     * @return the type of the stream
     */
    @get:Throws(ParsingException::class)
    public abstract val streamType: StreamType

    /**
     * Should return a list of streams related to the current handled. Many services show suggested
     * streams. If you don't like suggested streams you should implement them anyway since they can
     * be disabled by the user later in the frontend. The first related stream might be what was
     * previously known as a next stream.
     * If related streams aren't available simply return `null`.
     *
     * @return a list of InfoItems showing the related videos/streams
     */
    @get:Throws(IOException::class, ExtractionException::class)
    public open val relatedItems: InfoItemsCollector<out InfoItem, out InfoItemExtractor>?
        get() = null

    /**
     * Should return a list of Frameset object that contains preview of stream frames
     *
     * @return list of preview frames or empty list if frames preview is not supported or not found
     * for specified stream
     */
    @get:Throws(ExtractionException::class)
    public open val frames: List<Frameset>
        get() = emptyList()

    /**
     * Should analyse the webpage's document and extracts any error message there might be.
     *
     * @return Error message; `null` if there is no error message.
     */
    public open val errorMessage: String?
        get() = null

    //////////////////////////////////////////////////////////////////
    ///  Helper
    //////////////////////////////////////////////////////////////////

    /**
     * Override this function if the format of timestamp in the url is not the same format as that
     * from youtube.
     *
     * The URL is never quoted in the exceptions, and a value too large to be a timestamp is
     * rejected with a [ParsingException] instead of a `NumberFormatException`.
     *
     * @return the time stamp/seek for the video in seconds
     */
    @Throws(ParsingException::class)
    protected open fun getTimestampSeconds(regexPattern: String): Long {
        val timestamp: String = try {
            Parser.matchGroup1(regexPattern, originalUrl)
        } catch (_: Parser.RegexException) {
            // catch this instantly since a url does not necessarily have a timestamp

            // -2 because the testing system will consequently know that the regex failed
            // not good, I know
            return -2
        }

        if (timestamp.isEmpty()) {
            return 0
        }

        var secondsString: String
        var minutesString = ""
        var hoursString = ""
        val labelledSeconds = group1OrNull(SECONDS_PATTERN, timestamp)
        if (labelledSeconds == null) {
            // it could be that time is given in another method:
            // if nothing was obtained, treat as unlabelled seconds
            secondsString = group1OrNull(UNLABELLED_SECONDS_PATTERN, timestamp)
                ?: throw ParsingException("Could not get timestamp.")
        } else {
            secondsString = labelledSeconds
            // Like before, the hours are only looked for when the minutes were found
            val labelledMinutes = group1OrNull(MINUTES_PATTERN, timestamp)
            if (labelledMinutes != null) {
                minutesString = labelledMinutes
                hoursString = group1OrNull(HOURS_PATTERN, timestamp).orEmpty()
            }
        }

        val seconds = parseTimePart(secondsString)
        val minutes = parseTimePart(minutesString)
        val hours = parseTimePart(hoursString)

        return seconds + (SECONDS_PER_MINUTE * minutes) + (SECONDS_PER_HOUR * hours)
    }

    /**
     * The host of the stream (Eg. peertube.cpy.re).
     * If the host is not available, or if the service doesn't use
     * a federated system, but a centralised system,
     * you can simply return an empty string.
     *
     * @return the host of the stream or an empty string.
     */
    @get:Throws(ParsingException::class)
    public open val host: String
        get() = ""

    /**
     * The privacy of the stream (Eg. Public, Private, Unlisted…).
     *
     * @return the privacy of the stream.
     */
    @get:Throws(ParsingException::class)
    public open val privacy: Privacy
        get() = Privacy.PUBLIC

    /**
     * The name of the category of the stream.
     * If the category is not available you can simply return an empty string.
     *
     * @return the category of the stream or an empty string.
     */
    @get:Throws(ParsingException::class)
    public open val category: String
        get() = ""

    /**
     * The name of the licence of the stream.
     * If the licence is not available you can simply return an empty string.
     *
     * @return the licence of the stream or an empty String.
     */
    @get:Throws(ParsingException::class)
    public open val licence: String
        get() = ""

    /**
     * The locale language of the stream.
     * If the language is not available you can simply return null.
     * If the language is provided by a language code, you can return
     * new Locale(language_code);
     *
     * @return the locale language of the stream or `null`.
     */
    @get:Throws(ParsingException::class)
    public open val languageInfo: Locale?
        get() = null

    /**
     * The list of tags of the stream.
     * If the tag list is not available you can simply return an empty list.
     *
     * @return the list of tags of the stream or Collections.emptyList().
     */
    @get:Throws(ParsingException::class)
    public open val tags: List<String>
        get() = emptyList()

    /**
     * The support information of the stream.
     * see: https://framatube.org/videos/watch/ee408ec8-07cd-4e35-b884-fb681a4b9d37
     * (support button).
     * If the support information are not available,
     * you can simply return an empty String.
     *
     * @return the support information of the stream or an empty string.
     */
    @get:Throws(ParsingException::class)
    public open val supportInfo: String
        get() = ""

    /**
     * The list of stream segments by timestamps for the stream.
     * If the segment list is not available you can simply return an empty list.
     *
     * @return The list of segments of the stream or an empty list.
     */
    @get:Throws(ParsingException::class)
    public open val streamSegments: List<StreamSegment>
        get() = emptyList()

    /**
     * Meta information about the stream.
     *
     * This can be information about the stream creator (e.g. if the creator is a public
     * broadcaster) or further information on the topic (e.g. hints that the video might contain
     * conspiracy theories or contains information about a current health situation like the
     * Covid-19 pandemic).
     *
     * The meta information often contains links to external sources like Wikipedia or the WHO.
     *
     * @return The meta info of the stream or an empty list if not provided.
     */
    @get:Throws(ParsingException::class)
    public open val metaInfo: List<MetaInfo>
        get() = emptyList()

    /**
     * Whether the stream is a short-form content.
     *
     * Short-form contents are contents in the style of TikTok, YouTube Shorts, or Instagram Reels
     * videos.
     *
     * @return whether the stream is a short-form content
     */
    @get:Throws(ParsingException::class)
    public open val isShortFormContent: Boolean
        get() = false

    /**
     * Get the availability of the stream.
     *
     * @return The stream's availability
     * @throws ParsingException if there is an error in the extraction
     */
    @get:Throws(ParsingException::class)
    public open val contentAvailability: ContentAvailability
        get() = ContentAvailability.UNKNOWN

    /**
     * The URL for server adaptive bitrate streaming.
     *
     * @return The stream's SABR URL.
     * @throws ParsingException if there is an error in the extraction
     */
    @get:Throws(ParsingException::class)
    public open val serverAbrStreamingUrl: String?
        get() = null

    /**
     * The ustreamerConfig for server adaptive bitrate streaming.
     *
     * @return The stream's ustreamerConfig.
     * @throws ParsingException if there is an error in the extraction
     */
    @get:Throws(ParsingException::class)
    public open val ustreamerConfig: String?
        get() = null

    public enum class Privacy {
        PUBLIC,
        UNLISTED,
        PRIVATE,
        INTERNAL,
        OTHER,
    }

    public companion object {
        public const val NO_AGE_LIMIT: Int = 0
        public const val UNKNOWN_SUBSCRIBER_COUNT: Long = -1

        private const val SECONDS_PER_MINUTE = 60L
        private const val SECONDS_PER_HOUR = 3600L

        // Compiled once: getTimestampSeconds runs for every opened stream
        private val SECONDS_PATTERN = Regex("(\\d+)s")
        private val MINUTES_PATTERN = Regex("(\\d+)m")
        private val HOURS_PATTERN = Regex("(\\d+)h")
        private val UNLABELLED_SECONDS_PATTERN = Regex("t=(\\d+)")

        private fun group1OrNull(pattern: Regex, input: String): String? =
            pattern.find(input)?.groupValues?.get(1)

        /** An empty part counts as zero; a part too large for an `Int` is not a timestamp. */
        @Throws(ParsingException::class)
        private fun parseTimePart(part: String): Int = if (part.isEmpty()) {
            0
        } else {
            part.toIntOrNull() ?: throw ParsingException("Could not get timestamp.")
        }
    }
}

/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.media_ccc.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonParser
import org.schabi.newpipe.extractor.json.JsonParserException
import org.schabi.newpipe.extractor.linkhandler.LinkHandler
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.services.media_ccc.MediaCCCLinkGuards
import org.schabi.newpipe.extractor.services.media_ccc.linkHandler.MediaCCCConferenceLinkHandlerFactory
import org.schabi.newpipe.extractor.services.media_ccc.linkHandler.MediaCCCStreamLinkHandlerFactory
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.Description
import org.schabi.newpipe.extractor.stream.Stream
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.extractor.stream.VideoStream
import org.schabi.newpipe.extractor.utils.JsonUtils
import org.schabi.newpipe.extractor.utils.LocaleCompat
import java.io.IOException
import java.util.Locale

public open class MediaCCCStreamExtractor(
    service: StreamingService,
    linkHandler: LinkHandler,
) : StreamExtractor(service, linkHandler) {

    private lateinit var data: JsonObject
    private lateinit var conferenceData: JsonObject

    override val textualUploadDate: String?
        get() = data.getString("release_date")

    @get:Throws(ParsingException::class)
    override val uploadDate: DateWrapper?
        get() = DateWrapper.fromOffsetDateTime(textualUploadDate)

    override val thumbnails: List<Image>
        get() = MediaCCCParsingHelper.getThumbnailsFromStreamItem(data)

    override val description: Description
        get() = Description.of(data.getString("description"), Description.Type.PLAIN_TEXT)

    override val length: Long
        get() = data.getInt("length").toLong()

    override val viewCount: Long
        get() = data.getInt("view_count").toLong()

    /** Built from the conference slug, checked before it is placed in the URL. */
    @get:Throws(ParsingException::class)
    override val uploaderUrl: String
        get() = MediaCCCConferenceLinkHandlerFactory.CONFERENCE_PATH + MediaCCCLinkGuards.requireSlug(uploaderName)

    /** The conference slug, taken from the conference API URL. */
    @get:Throws(ParsingException::class)
    override val uploaderName: String
        get() {
            val conferenceUrl = data.getString("conference_url")
                ?: throw ParsingException("Could not get the conference of the event")
            return CONFERENCE_API_PREFIX.replaceFirst(conferenceUrl, "")
        }

    override val uploaderAvatars: List<Image>
        get() = MediaCCCParsingHelper.getImageListFromLogoImageUrl(conferenceData.getString("logo_url"))

    @get:Throws(ExtractionException::class)
    override val audioStreams: List<AudioStream>
        get() {
            val audioStreams = ArrayList<AudioStream>()
            for (recording in recordingsOfType("audio")) {
                val mimeType = recording.getString("mime_type").orEmpty()
                // First we need to resolve the actual video data from the CDN
                val mediaFormat = when {
                    mimeType.endsWith("opus") -> MediaFormat.OPUS
                    mimeType.endsWith("mpeg") -> MediaFormat.MP3
                    mimeType.endsWith("ogg") -> MediaFormat.OGG
                    else -> null
                }
                val builder = AudioStream.Builder()
                    .setId(recording.getString("filename", Stream.ID_UNKNOWN))
                    .setContent(recordingUrl(recording) ?: continue, true)
                    .setMediaFormat(mediaFormat)
                    .setAverageBitrate(AudioStream.UNKNOWN_BITRATE)

                val language = recording.getString("language")
                // If the language contains a - symbol, this means that the stream has an audio
                // track with multiple languages, so there is no specific language for this stream
                // Don't set the audio language in this case
                if (language != null && !language.contains("-")) {
                    // the language is not quoted in the message
                    builder.setAudioLocale(
                        LocaleCompat.forLanguageTag(language).orElseThrow {
                            ParsingException("Cannot convert the language of a recording to a locale")
                        },
                    )
                }

                // Not checking containsSimilarStream here, since MediaCCC does not provide enough
                // information to decide whether two streams are similar. Hence that method would
                // always return false, e.g. even for different language variations.
                audioStreams.add(builder.build())
            }
            return audioStreams
        }

    @get:Throws(ExtractionException::class)
    override val videoStreams: List<VideoStream>
        get() {
            val videoStreams = ArrayList<VideoStream>()
            for (recording in recordingsOfType("video")) {
                val mimeType = recording.getString("mime_type").orEmpty()
                // First we need to resolve the actual video data from the CDN
                val mediaFormat = when {
                    mimeType.endsWith("webm") -> MediaFormat.WEBM
                    mimeType.endsWith("mp4") -> MediaFormat.MPEG_4
                    else -> null
                }

                // Not checking containsSimilarStream here, since MediaCCC does not provide enough
                // information to decide whether two streams are similar. Hence that method would
                // always return false, e.g. even for different language variations.
                videoStreams.add(
                    VideoStream.Builder()
                        .setId(recording.getString("filename", Stream.ID_UNKNOWN))
                        .setContent(recordingUrl(recording) ?: continue, true)
                        .setIsVideoOnly(false)
                        .setMediaFormat(mediaFormat)
                        .setResolution(recording.getInt("height").toString() + "p")
                        .build(),
                )
            }
            return videoStreams
        }

    /** The recordings whose MIME type starts with [type]; a recording without a MIME type is skipped. */
    private fun recordingsOfType(type: String): List<JsonObject> = data.getArray("recordings")
        .filterIsInstance<JsonObject>()
        .filter { it.getString("mime_type")?.startsWith(type) == true }

    /** The recording file, only when it is a web link (a recording without one is skipped). */
    private fun recordingUrl(recording: JsonObject): String? =
        MediaCCCLinkGuards.webUrlOrNull(recording.getString("recording_url"))

    override val videoOnlyStreams: List<VideoStream>
        get() = emptyList()

    override val streamType: StreamType
        get() = StreamType.VIDEO_STREAM

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        // the event slug is checked before it is placed in the API URL
        val videoUrl = MediaCCCStreamLinkHandlerFactory.VIDEO_API_ENDPOINT + MediaCCCLinkGuards.requireSlug(id)
        data = parseObject(downloader.get(videoUrl).responseBody())
        // The conference URL comes from the answer: it is requested only when it is the API URL of
        // a conference on media.ccc.de, so that the answer cannot send the request to another host.
        val conferenceUrl = MediaCCCLinkGuards.requireConferenceApiUrl(data.getString("conference_url"))
        conferenceData = parseObject(downloader.get(conferenceUrl).responseBody())
    }

    /** Neither the requested URL nor the answer is quoted in the error. */
    @Throws(ExtractionException::class)
    private fun parseObject(body: String): JsonObject = try {
        JsonParser.`object`().from(body)
    } catch (e: JsonParserException) {
        throw ExtractionException("Could not parse json returned by the event API", e)
    }

    @get:Throws(ParsingException::class)
    override val name: String
        get() = data.getString("title") ?: throw ParsingException("Could not get event name")

    /** The event page given by the API when it is a web link, otherwise the link that was opened. */
    @get:Throws(ParsingException::class)
    override val originalUrl: String
        get() = MediaCCCLinkGuards.webUrlOrNull(data.getString("frontend_link")) ?: super.originalUrl

    @get:Throws(ParsingException::class)
    override val languageInfo: Locale?
        get() = Localization.getLocaleFromThreeLetterCode(
            data.getString("original_language") ?: throw ParsingException("Could not get the language"),
        )

    override val tags: List<String>
        get() = JsonUtils.getStringListFromJsonArray(data.getArray("tags"))

    private companion object {
        private val CONFERENCE_API_PREFIX = Regex("https://(api\\.)?media\\.ccc\\.de/public/conferences/")
    }
}

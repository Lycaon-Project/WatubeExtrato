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
import org.schabi.newpipe.extractor.linkhandler.LinkHandler
import org.schabi.newpipe.extractor.services.media_ccc.MediaCCCLinkGuards
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.Description
import org.schabi.newpipe.extractor.stream.Stream
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.extractor.stream.VideoStream
import java.io.IOException

public open class MediaCCCLiveStreamExtractor(
    service: StreamingService,
    linkHandler: LinkHandler,
) : StreamExtractor(service, linkHandler) {

    private var conference: JsonObject? = null
    private var group: String = ""
    private var room: JsonObject? = null

    private val fetchedConference: JsonObject
        get() = checkNotNull(conference) { "Page is not fetched. Make sure you call fetchPage()" }

    private val fetchedRoom: JsonObject
        get() = checkNotNull(room) { "Page is not fetched. Make sure you call fetchPage()" }

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        val doc = MediaCCCParsingHelper.getLiveStreams(downloader, extractorLocalization)
        // Find the correct room
        for (conferenceObject in doc.filterIsInstance<JsonObject>()) {
            for (groupObject in conferenceObject.getArray("groups").filterIsInstance<JsonObject>()) {
                val roomObject = groupObject.getArray("rooms").filterIsInstance<JsonObject>()
                    .firstOrNull { id == conferenceObject.getString("mandator") + "/" + it.getString("slug") }
                    ?: continue
                conference = conferenceObject
                group = groupObject.getString("group").orEmpty()
                room = roomObject
                return
            }
        }
        // the ID is not quoted in the message
        throw ExtractionException("Could not find a room matching the live stream ID")
    }

    @get:Throws(ParsingException::class)
    override val name: String
        get() = fetchedRoom.getString("display") ?: throw ParsingException("Could not get room name")

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = MediaCCCParsingHelper.getThumbnailsFromLiveStreamItem(fetchedRoom)

    @get:Throws(ParsingException::class)
    override val description: Description
        get() = Description(
            fetchedConference.getString("description") + " - " + group,
            Description.Type.PLAIN_TEXT,
        )

    override val viewCount: Long
        get() = -1

    /** Built from the conference slug, checked before it is placed in the URL. */
    @get:Throws(ParsingException::class)
    override val uploaderUrl: String
        get() = STREAMING_HOME + MediaCCCLinkGuards.requireSlug(fetchedConference.getString("slug"))

    @get:Throws(ParsingException::class)
    override val uploaderName: String
        get() = fetchedConference.getString("conference")
            ?: throw ParsingException("Could not get conference name")

    /**
     * The URL of the first DASH stream found.
     *
     * There can be several DASH streams, so the URL of the first one found is returned by this
     * property. You can find the other DASH video streams by using [videoStreams].
     */
    @get:Throws(ParsingException::class)
    override val dashMpdUrl: String
        get() = getManifestOfDeliveryMethodWanted("dash")

    /**
     * The URL of the first HLS stream found.
     *
     * There can be several HLS streams, so the URL of the first one found is returned by this
     * property. You can find the other HLS video streams by using [videoStreams].
     */
    override val hlsUrl: String
        get() = getManifestOfDeliveryMethodWanted("hls")

    /** The first manifest of [deliveryMethod] which is a web link, or an empty string. */
    private fun getManifestOfDeliveryMethodWanted(deliveryMethod: String): String =
        fetchedRoom.getArray(STREAMS).filterIsInstance<JsonObject>()
            .map { it.getObject(URLS) }
            .firstOrNull { it.has(deliveryMethod) }
            ?.let { MediaCCCLinkGuards.webUrlOrNull(it.getObject(deliveryMethod).getString(URL)) }
            .orEmpty()

    @get:Throws(IOException::class, ExtractionException::class)
    override val audioStreams: List<AudioStream>
        get() = getStreams("audio") { dto, url ->
            val builder = AudioStream.Builder()
                .setId(dto.urlValue.getString("tech", Stream.ID_UNKNOWN))
                .setContent(url, true)
                .setAverageBitrate(AudioStream.UNKNOWN_BITRATE)
            if (dto.urlKey == "hls") {
                // We don't know with the type string what media format will
                // have HLS streams.
                // However, the tech string may contain some information
                // about the media format used.
                builder.setDeliveryMethod(DeliveryMethod.HLS).build()
            } else {
                builder.setMediaFormat(MediaFormat.getFromSuffix(dto.urlKey)).build()
            }
        }

    @get:Throws(IOException::class, ExtractionException::class)
    override val videoStreams: List<VideoStream>
        get() = getStreams("video") { dto, url ->
            val videoSize = dto.streamJsonObj.getArray("videoSize")
            val builder = VideoStream.Builder()
                .setId(dto.urlValue.getString("tech", Stream.ID_UNKNOWN))
                .setContent(url, true)
                .setIsVideoOnly(false)
                .setResolution(videoSize.getInt(0).toString() + "x" + videoSize.getInt(1))
            if (dto.urlKey == "hls") {
                // We don't know with the type string what media format will
                // have HLS streams.
                // However, the tech string may contain some information
                // about the media format used.
                builder.setDeliveryMethod(DeliveryMethod.HLS).build()
            } else {
                builder.setMediaFormat(MediaFormat.getFromSuffix(dto.urlKey)).build()
            }
        }

    /**
     * Ties together the stream JSON object, its URL key and its URL value, before they are
     * converted by [getStreams] to a proper [Stream] object based on the wanted stream type.
     */
    private class MediaCCCLiveStreamMapperDTO(
        val streamJsonObj: JsonObject,
        val urlKey: String,
        val urlValue: JsonObject,
    )

    /**
     * The streams of [streamType]; a stream whose URL is not a web link is skipped.
     * The DASH manifest is left out: it is given by [dashMpdUrl].
     */
    private fun <T : Stream> getStreams(
        streamType: String,
        converter: (MediaCCCLiveStreamMapperDTO, String) -> T,
    ): List<T> = fetchedRoom.getArray(STREAMS).filterIsInstance<JsonObject>()
        // Only process streams of requested type
        .filter { it.getString("type") == streamType }
        // Flatmap Urls and ensure that we use only process JsonObjects
        .flatMap { streamJsonObj ->
            streamJsonObj.getObject(URLS).entries.mapNotNull { (key, value) ->
                (value as? JsonObject)?.let { MediaCCCLiveStreamMapperDTO(streamJsonObj, key, it) }
            }
        }
        // The DASH manifest will be extracted with getDashMpdUrl
        .filter { it.urlKey != "dash" }
        .mapNotNull { dto -> MediaCCCLinkGuards.webUrlOrNull(dto.urlValue.getString(URL))?.let { converter(dto, it) } }

    override val videoOnlyStreams: List<VideoStream>
        get() = emptyList()

    @get:Throws(ParsingException::class)
    override val streamType: StreamType
        get() = StreamType.LIVE_STREAM

    override val category: String
        get() = group

    private companion object {
        private const val STREAMS = "streams"
        private const val URLS = "urls"
        private const val URL = "url"
        private const val STREAMING_HOME = "https://streaming.media.ccc.de/"
    }
}

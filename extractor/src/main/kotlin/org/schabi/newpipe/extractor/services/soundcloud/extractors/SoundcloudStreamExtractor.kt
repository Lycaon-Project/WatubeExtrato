package org.schabi.newpipe.extractor.services.soundcloud.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.GeographicRestrictionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.SoundCloudGoPlusContentException
import org.schabi.newpipe.extractor.json.JsonArray
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.linkhandler.LinkHandler
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper.SOUNDCLOUD_API_V2_URL
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper.clientId
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper.getAllImagesFromArtworkOrAvatarUrl
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper.getAllImagesFromTrackObject
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper.getAvatarUrl
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper.parseDate
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.Description
import org.schabi.newpipe.extractor.stream.Stream
import org.schabi.newpipe.extractor.stream.Stream.Companion.ID_UNKNOWN
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.extractor.stream.VideoStream
import org.schabi.newpipe.extractor.utils.Utils
import java.io.IOException
import java.util.Collections

public open class SoundcloudStreamExtractor(service: StreamingService, linkHandler: LinkHandler) :
    StreamExtractor(service, linkHandler) {

    private lateinit var track: JsonObject
    private var isAvailable = true

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        track = SoundcloudParsingHelper.resolveFor(downloader, url)

        val policy = track.getString("policy", "")
        if (policy != "ALLOW" && policy != "MONETIZE") {
            isAvailable = false

            if (policy == "SNIP") {
                throw SoundCloudGoPlusContentException()
            }

            if (policy == "BLOCK") {
                throw GeographicRestrictionException("This track is not available in user's country")
            }

            // the policy given by the server is not quoted
            throw ContentNotAvailableException("Content not available")
        }
    }

    override val id: String
        get() = track.getLong("id").toString()

    @get:Throws(ParsingException::class)
    override val name: String
        get() = track.getString("title") ?: throw ParsingException("Could not get name")

    override val textualUploadDate: String?
        get() = track.getString("created_at")

    @get:Throws(ParsingException::class)
    override val uploadDate: DateWrapper?
        get() = parseDate(textualUploadDate)

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = getAllImagesFromTrackObject(track)

    override val description: Description
        get() = Description.of(track.getString("description"), Description.Type.PLAIN_TEXT)

    override val length: Long
        get() = track.getLong("duration") / MILLIS_PER_SECOND

    @get:Throws(ParsingException::class)
    override val timeStamp: Long
        get() {
            val timestamp = getTimestampSeconds(TIMESTAMP_PATTERN)
            return if (timestamp == NO_TIMESTAMP) 0 else timestamp
        }

    override val viewCount: Long
        get() = track.getLong("playback_count")

    override val likeCount: Long
        get() = track.getLong("likes_count", -1)

    override val uploaderUrl: String
        get() = SoundcloudParsingHelper.getUploaderUrl(track)

    override val uploaderName: String
        get() = SoundcloudParsingHelper.getUploaderName(track)

    @get:Throws(ParsingException::class)
    override val isUploaderVerified: Boolean
        get() = track.getObject("user").getBoolean("verified")

    override val uploaderAvatars: List<Image>
        get() = getAllImagesFromArtworkOrAvatarUrl(getAvatarUrl(track))

    /**
     * Streams can be streamable and downloadable - or explicitly not.
     * For playing the track, it is only necessary to have a streamable track.
     * If this is not the case, this track might not be published yet.
     */
    @get:Throws(ExtractionException::class)
    override val audioStreams: List<AudioStream>
        get() {
            val audioStreams = ArrayList<AudioStream>()
            if (!track.getBoolean("streamable") || !isAvailable) {
                return audioStreams
            }

            val transcodings = track.getObject("media").getArray("transcodings")
            if (transcodings.isNotEmpty()) {
                // Get information about what stream formats are available
                extractAudioStreams(transcodings, audioStreams)
            }
            return audioStreams
        }

    /**
     * The stream URL given by the [endpointUrl] of a transcoding, or `null` when the endpoint is
     * not a https link to SoundCloud (the client ID is never sent elsewhere) or when the stream
     * URL is not a https link.
     */
    @Throws(IOException::class, ExtractionException::class)
    private fun getTranscodingUrl(endpointUrl: String): String? {
        if (!SoundcloudParsingHelper.isApiUrl(endpointUrl)) {
            return null
        }
        var apiStreamUrl = endpointUrl + (if ('?' in endpointUrl) "&" else "?") + "client_id=" + clientId()

        val trackAuthorization = track.getString("track_authorization")
        if (!trackAuthorization.isNullOrEmpty()) {
            apiStreamUrl += "&track_authorization=" + Utils.encodeUrlUtf8(trackAuthorization)
        }

        val response = SoundcloudParsingHelper.requireDownloader().get(apiStreamUrl)
            .also(SoundcloudParsingHelper::noteRefusedClientId)
            .responseBody()
        val urlObject = try {
            SoundcloudParsingHelper.parseObject(response)
        } catch (e: ParsingException) {
            throw ParsingException("Could not parse streamable URL", e)
        }

        val streamUrl = urlObject.getString("url") ?: return null
        return streamUrl.takeIf { it.startsWith(HTTPS) && SoundcloudParsingHelper.secureWebUrlOrNull(it) == it }
    }

    private fun extractAudioStreams(transcodings: JsonArray, audioStreams: MutableList<AudioStream>) {
        for (transcoding in transcodings) {
            if (transcoding !is JsonObject) {
                continue
            }
            try {
                extractAudioStream(transcoding, audioStreams)?.let { audioStream ->
                    if (!Stream.containSimilarStream(audioStream, audioStreams)) {
                        audioStreams.add(audioStream)
                    }
                }
            } catch (_: ExtractionException) {
                // Something went wrong when trying to get and add this audio stream,
                // skip to the next one
            } catch (_: IOException) {
                // Same as above
            }
        }
    }

    /**
     * The audio stream of a transcoding, or `null` to skip it. Its URL costs a request: it is
     * only asked for a known format that [found] does not have yet.
     */
    @Throws(IOException::class, ExtractionException::class)
    private fun extractAudioStream(transcoding: JsonObject, found: List<AudioStream>): AudioStream? {
        val url = transcoding.getString("url")
        if (url.isNullOrEmpty()) {
            return null
        }

        val preset = transcoding.getString("preset", ID_UNKNOWN) ?: ID_UNKNOWN
        val protocol = transcoding.getObject("format").getString("protocol").orEmpty()

        if (protocol.contains("encrypted")) {
            // Skip DRM-protected streams, which have encrypted in their protocol name
            return null
        }

        val builder = AudioStream.Builder().setId(preset)

        val delivery = if (protocol == "hls") DeliveryMethod.HLS else DeliveryMethod.PROGRESSIVE_HTTP
        builder.setDeliveryMethod(delivery)

        val format = when {
            preset.contains("mp3") -> MediaFormat.MP3.also { builder.setAverageBitrate(MP3_BITRATE) }
            preset.contains("opus") -> MediaFormat.OPUS.also { builder.setAverageBitrate(OPUS_BITRATE) }
            preset.contains("aac_160k") -> MediaFormat.M4A.also { builder.setAverageBitrate(AAC_BITRATE) }
            // Unknown format, skip to the next audio stream
            else -> return null
        }
        builder.setMediaFormat(format)
        // the same as Stream.containSimilarStream, before the request rather than after it
        if (found.any { it.format?.id == format.id && it.deliveryMethod == delivery && it.isUrl }) {
            return null
        }

        // a stream URL that is missing or not a https link is skipped
        val streamUrl = getTranscodingUrl(url) ?: return null
        builder.setContent(streamUrl, true)
        return builder.build()
    }

    override val videoStreams: List<VideoStream>
        get() = Collections.emptyList()

    override val videoOnlyStreams: List<VideoStream>
        get() = Collections.emptyList()

    override val streamType: StreamType
        get() = StreamType.AUDIO_STREAM

    /** Only the numeric ID of the track is placed in the API URL. */
    @get:Throws(IOException::class, ExtractionException::class)
    override val relatedItems: StreamInfoItemsCollector?
        get() {
            val collector = StreamInfoItemsCollector(serviceId)
            val apiUrl = SOUNDCLOUD_API_V2_URL + "tracks/" + Utils.encodeUrlUtf8(id) +
                "/related?client_id=" + Utils.encodeUrlUtf8(clientId())

            SoundcloudParsingHelper.getStreamsFromApi(collector, apiUrl)
            return collector
        }

    override val privacy: Privacy
        get() = if (track.getString("sharing") == "public") Privacy.PUBLIC else Privacy.PRIVATE

    override val category: String
        get() = track.getString("genre").orEmpty()

    override val licence: String
        get() = track.getString("license").orEmpty()

    /** Tags are separated by spaces, but they can be multiple words escaped by quotes ". */
    override val tags: List<String>
        get() {
            val tagList = track.getString("tag_list").orEmpty().split(" ")
            val tags = ArrayList<String>()
            val escapedTag = StringBuilder()
            var isEscaped = false
            for (tag in tagList) {
                if (tag.startsWith("\"")) {
                    escapedTag.append(tag.replace("\"", ""))
                    isEscaped = true
                } else if (isEscaped) {
                    if (tag.endsWith("\"")) {
                        escapedTag.append(" ").append(tag.replace("\"", ""))
                        isEscaped = false
                        tags.add(escapedTag.toString())
                    } else {
                        escapedTag.append(" ").append(tag)
                    }
                } else if (tag.isNotEmpty()) {
                    tags.add(tag)
                }
            }
            return tags
        }

    private companion object {
        const val MILLIS_PER_SECOND = 1000L
        const val TIMESTAMP_PATTERN = "(#t=\\d{0,3}h?\\d{0,3}m?\\d{1,3}s?)"

        /** Returned by getTimestampSeconds when the URL has no timestamp. */
        const val NO_TIMESTAMP = -2L
        const val HTTPS = "https://"
        const val MP3_BITRATE = 128
        const val OPUS_BITRATE = 64
        const val AAC_BITRATE = 160
    }
}

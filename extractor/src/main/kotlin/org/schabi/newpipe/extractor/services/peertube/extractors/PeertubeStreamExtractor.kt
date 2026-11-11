/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.peertube.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.json.JsonArray
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonParser
import org.schabi.newpipe.extractor.json.JsonParserException
import org.schabi.newpipe.extractor.linkhandler.LinkHandler
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.services.peertube.PeertubeLinkGuards
import org.schabi.newpipe.extractor.services.peertube.PeertubeParsingHelper
import org.schabi.newpipe.extractor.services.peertube.linkHandler.PeertubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.services.peertube.linkHandler.PeertubeStreamLinkHandlerFactory
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.Description
import org.schabi.newpipe.extractor.stream.Frameset
import org.schabi.newpipe.extractor.stream.Stream
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector
import org.schabi.newpipe.extractor.stream.StreamSegment
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.extractor.stream.SubtitlesStream
import org.schabi.newpipe.extractor.stream.VideoStream
import org.schabi.newpipe.extractor.utils.JsonUtils
import org.schabi.newpipe.extractor.utils.Utils
import java.io.IOException
import java.util.Locale

/**
 * @throws ParsingException if the link does not lead to a PeerTube instance reachable over https
 */
public open class PeertubeStreamExtractor @Throws(ParsingException::class) constructor(
    service: StreamingService,
    linkHandler: LinkHandler,
) : StreamExtractor(service, linkHandler) {

    private val instanceBaseUrl: String = PeertubeLinkGuards.requireBaseUrl(linkHandler.baseUrl)
    private lateinit var json: JsonObject

    /** Requested on the first read, not with the page: a caller may never need them. */
    private var subtitles: List<SubtitlesStream>? = null
    private val audioStreamList = ArrayList<AudioStream>()
    private val videoStreamList = ArrayList<VideoStream>()

    private var subtitlesException: ParsingException? = null

    /** The checked video ID, the only part of the link placed in the API URLs. */
    private val videoApiUrl: String
        @Throws(ParsingException::class)
        get() = instanceBaseUrl + PeertubeStreamLinkHandlerFactory.VIDEO_API_ENDPOINT +
            PeertubeLinkGuards.requireVideoId(id)

    @get:Throws(ParsingException::class)
    override val textualUploadDate: String?
        get() = JsonUtils.getString(json, "publishedAt")

    @get:Throws(ParsingException::class)
    override val uploadDate: DateWrapper?
        get() = DateWrapper.fromInstant(textualUploadDate)

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = PeertubeParsingHelper.getThumbnailsFromPlaylistOrVideoItem(instanceBaseUrl, json)

    /**
     * The description. When the instance shortened it, the full one is requested (one more
     * request, as in the original code), on the first read only.
     */
    @get:Throws(ParsingException::class)
    override val description: Description
        get() = cachedDescription ?: fetchDescription().also { cachedDescription = it }

    private var cachedDescription: Description? = null

    @Throws(ParsingException::class)
    private fun fetchDescription(): Description {
        var text = try {
            JsonUtils.getString(json, "description")
        } catch (_: ParsingException) {
            return Description.EMPTY_DESCRIPTION
        }
        if (text.length == SHORT_DESCRIPTION_LENGTH && text.endsWith("...")) {
            // If description is shortened, get full description
            try {
                val response = downloader.get("$videoApiUrl/description")
                text = JsonUtils.getString(JsonParser.`object`().from(response.responseBody()), "description")
            } catch (_: IOException) {
                // Something went wrong when getting the full description, use the shortened one
            } catch (_: ReCaptchaException) {
                // same as above
            } catch (_: JsonParserException) {
                // same as above
            } catch (_: ParsingException) {
                // same as above: no description in the answer
            }
        }
        // raw markdown written on the instance: an app renders it as untrusted text (no raw
        // HTML, no javascript: links)
        return Description.of(text, Description.Type.MARKDOWN)
    }

    @get:Throws(ParsingException::class)
    override val ageLimit: Int
        get() = if (JsonUtils.getBoolean(json, "nsfw")) ADULT_AGE_LIMIT else NO_AGE_LIMIT

    override val length: Long
        get() = json.getLong("duration")

    @get:Throws(ParsingException::class)
    override val timeStamp: Long
        get() {
            val timestamp = getTimestampSeconds("((#|&|\\?)start=\\d{0,3}h?\\d{0,3}m?\\d{1,3}s?)")
            // -2: regex for timestamp was not found
            return if (timestamp == TIMESTAMP_NOT_FOUND) 0 else timestamp
        }

    override val viewCount: Long
        get() = json.getLong("views")

    override val likeCount: Long
        get() = json.getLong("likes")

    override val dislikeCount: Long
        get() = json.getLong("dislikes")

    @get:Throws(ParsingException::class)
    override val uploaderUrl: String
        get() {
            val name = JsonUtils.getString(json, ACCOUNT_NAME)
            val host = JsonUtils.getString(json, ACCOUNT_HOST)
            return service.channelLHFactory?.fromId("accounts/$name@$host", instanceBaseUrl)?.url
                ?: throw ParsingException("Could not get uploader URL")
        }

    @get:Throws(ParsingException::class)
    override val uploaderName: String
        get() = JsonUtils.getString(json, "account.displayName")

    override val uploaderAvatars: List<Image>
        get() = PeertubeParsingHelper.getAvatarsFromOwnerAccountOrVideoChannelObject(
            instanceBaseUrl,
            json.getObject("account"),
        )

    /** The channel URL given by the instance, only when it is a web link. */
    @get:Throws(ParsingException::class)
    override val subChannelUrl: String
        get() = PeertubeLinkGuards.requireWebUrl(JsonUtils.getString(json, "channel.url"), "channel URL")

    @get:Throws(ParsingException::class)
    override val subChannelName: String
        get() = JsonUtils.getString(json, "channel.displayName")

    override val subChannelAvatars: List<Image>
        get() = PeertubeParsingHelper.getAvatarsFromOwnerAccountOrVideoChannelObject(
            instanceBaseUrl,
            json.getObject("channel"),
        )

    /** The HLS master playlist, only when it is a web link; empty otherwise. */
    override val hlsUrl: String
        get() {
            assertPageFetched()
            val files = json.getObject(FILES)
            val playlistUrl = if (streamType == StreamType.VIDEO_STREAM && files.isNotEmpty()) {
                files.getString(PLAYLIST_URL, "")
            } else {
                json.getArray(STREAMING_PLAYLISTS).getObject(0).getString(PLAYLIST_URL, "")
            }
            return PeertubeLinkGuards.webUrlOrNull(playlistUrl).orEmpty()
        }

    @get:Throws(ParsingException::class)
    override val audioStreams: List<AudioStream>
        get() {
            assertPageFetched()

            /*
            Some videos have audio streams; others don't.
            So an audio stream may be available if a video stream is available.
            Audio streams are also not returned as separated streams for livestreams.
            That's why the extraction of audio streams is only run when there are video streams
            extracted and when the content is not a livestream.
             */
            if (audioStreamList.isEmpty() && videoStreamList.isEmpty() &&
                streamType == StreamType.VIDEO_STREAM
            ) {
                extractStreams()
            }
            return audioStreamList
        }

    @get:Throws(ExtractionException::class)
    override val videoStreams: List<VideoStream>
        get() {
            assertPageFetched()
            if (videoStreamList.isEmpty()) {
                if (streamType == StreamType.VIDEO_STREAM) {
                    extractStreams()
                } else {
                    extractLiveVideoStreams()
                }
            }
            return videoStreamList
        }

    override val videoOnlyStreams: List<VideoStream>
        get() = emptyList()

    @get:Throws(ParsingException::class)
    override val subtitlesDefault: List<SubtitlesStream>
        get() {
            return loadedSubtitles()
        }

    @Throws(ParsingException::class)
    override fun getSubtitles(format: MediaFormat?): List<SubtitlesStream> {
        return loadedSubtitles().filter { it.format == format }
    }

    override val streamType: StreamType
        get() = if (json.getBoolean("isLive")) StreamType.LIVE_STREAM else StreamType.VIDEO_STREAM

    @get:Throws(IOException::class, ExtractionException::class)
    override val relatedItems: StreamInfoItemsCollector?
        get() {
            val tags = tags
            val apiUrl = if (tags.isEmpty()) {
                // the account is checked by the link handler before its videos are requested
                val name = JsonUtils.getString(json, ACCOUNT_NAME)
                val host = JsonUtils.getString(json, ACCOUNT_HOST)
                val account = PeertubeLinkGuards.requireActorName("$name@$host")
                "$instanceBaseUrl/api/v1/accounts/$account/videos?start=0&count=$RELATED_ITEMS_COUNT"
            } else {
                getRelatedItemsUrl(tags)
            }

            if (Utils.isBlank(apiUrl)) {
                return null
            }
            val collector = StreamInfoItemsCollector(serviceId)
            getStreamsFromApi(collector, apiUrl)
            return collector
        }

    override val tags: List<String>
        get() = JsonUtils.getStringListFromJsonArray(json.getArray("tags"))

    override val supportInfo: String
        get() = try {
            JsonUtils.getString(json, "support")
        } catch (_: ParsingException) {
            ""
        }

    /** Requested on the first read only. */
    @get:Throws(ParsingException::class)
    override val streamSegments: List<StreamSegment>
        get() = cachedSegments ?: fetchStreamSegments().also { cachedSegments = it }

    private var cachedSegments: List<StreamSegment>? = null

    @Throws(ParsingException::class)
    private fun fetchStreamSegments(): List<StreamSegment> {
        // the causes are not kept: their messages may quote the request URL
        val segmentsJson = try {
            fetchSubApiContent("chapters")
        } catch (_: IOException) {
            throw ParsingException("Could not get stream segments")
        } catch (_: ReCaptchaException) {
            throw ParsingException("Could not get stream segments")
        }
        val segments = ArrayList<StreamSegment>()
        if (segmentsJson != null && segmentsJson.has("chapters")) {
            for (segment in segmentsJson.getArray("chapters")) {
                // a missing chapter gives an empty object, as JsonArray.getObject() did
                val segmentObject = segment as? JsonObject ?: JsonObject()
                segments.add(StreamSegment(segmentObject.getString("title"), segmentObject.getInt("timecode")))
            }
        }
        return segments
    }

    /** Requested on the first read only. */
    @get:Throws(ExtractionException::class)
    override val frames: List<Frameset>
        get() = cachedFrames ?: fetchFrames().also { cachedFrames = it }

    private var cachedFrames: List<Frameset>? = null

    @Throws(ExtractionException::class)
    private fun fetchFrames(): List<Frameset> {
        // the causes are not kept: their messages may quote the request URL
        val storyboards = try {
            fetchSubApiContent("storyboards")
        } catch (_: IOException) {
            throw ExtractionException("Could not get frames")
        } catch (_: ReCaptchaException) {
            throw ExtractionException("Could not get frames")
        }
        if (storyboards == null || !storyboards.has("storyboards")) {
            return emptyList()
        }
        val framesets = ArrayList<Frameset>()
        for (storyboard in storyboards.getArray("storyboards")) {
            if (storyboard is JsonObject) {
                toFrameset(storyboard)?.let(framesets::add)
            }
        }
        return framesets
    }

    /**
     * A storyboard, or `null` when its sizes are not positive (the Java code divided by zero) or
     * its image is not on the instance.
     */
    private fun toFrameset(storyboard: JsonObject): Frameset? {
        val url = PeertubeLinkGuards.instanceUrlOrNull(instanceBaseUrl, storyboard.getString("storyboardPath"))
        val width = storyboard.getInt("spriteWidth")
        val height = storyboard.getInt("spriteHeight")
        if (url == null || width <= 0 || height <= 0) {
            return null
        }
        val framesPerPageX = storyboard.getInt("totalWidth") / width
        val framesPerPageY = storyboard.getInt("totalHeight") / height
        // the sizes come from the instance: the product is computed in Long and bounded
        val count = framesPerPageX.toLong() * framesPerPageY
        if (framesPerPageX <= 0 || framesPerPageY <= 0 || count > MAX_FRAMES) {
            return null
        }
        val durationPerFrame = storyboard.getInt("spriteDuration") * MILLIS_PER_SECOND
        return Frameset(
            // there is only one composite image per video containing all frames
            listOf(url),
            width,
            height,
            count.toInt(),
            durationPerFrame,
            framesPerPageX,
            framesPerPageY,
        )
    }

    /** Related videos sharing a tag; the tags are percent-encoded. */
    private fun getRelatedItemsUrl(tags: List<String>): String {
        val params = StringBuilder("start=0&count=$RELATED_ITEMS_COUNT&sort=-createdAt")
        for (tag in tags) {
            params.append("&tagsOneOf=").append(Utils.encodeUrlUtf8(tag))
        }
        return instanceBaseUrl + PeertubeSearchQueryHandlerFactory.SEARCH_ENDPOINT_VIDEOS + "?" + params
    }

    @Throws(IOException::class, ReCaptchaException::class, ParsingException::class)
    private fun getStreamsFromApi(collector: StreamInfoItemsCollector, apiUrl: String) {
        val body = downloader.get(apiUrl).responseBody()
        if (Utils.isBlank(body)) {
            return
        }
        val relatedVideosJson = try {
            JsonParser.`object`().from(body)
        } catch (e: JsonParserException) {
            throw ParsingException("Could not parse json data for related videos", e)
        }
        collectStreamsFrom(collector, relatedVideosJson)
    }

    @Throws(ParsingException::class)
    private fun collectStreamsFrom(collector: StreamInfoItemsCollector, jsonObject: JsonObject) {
        val contents = try {
            JsonUtils.getValue(jsonObject, "data") as? JsonArray
        } catch (e: ParsingException) {
            throw ParsingException("Could not extract related videos", e)
        } ?: throw ParsingException("Could not extract related videos")

        val streamUrl = url
        for (c in contents) {
            if (c is JsonObject) {
                val extractor = PeertubeStreamInfoItemExtractor(c, instanceBaseUrl)
                // Do not add the same stream in related streams
                if (extractor.url != streamUrl) {
                    collector.commit(extractor)
                }
            }
        }
    }

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        val response = downloader.get(videoApiUrl)
        json = try {
            JsonParser.`object`().from(response.responseBody())
        } catch (e: JsonParserException) {
            throw ExtractionException("Could not extract PeerTube stream data", e)
        }
        PeertubeParsingHelper.validate(json)
    }

    /** The captions, requested once: a failure is kept as well, so it is not requested again. */
    @Throws(ParsingException::class)
    private fun loadedSubtitles(): List<SubtitlesStream> {
        subtitlesException?.let { throw it }
        subtitles?.let { return it }
        val loaded = ArrayList<SubtitlesStream>()
        try {
            val response = downloader.get("$videoApiUrl/captions")
            val captionsJson = JsonParser.`object`().from(response.responseBody())
            for (c in JsonUtils.getArray(captionsJson, "data")) {
                if (c is JsonObject) {
                    toSubtitlesStream(c)?.let(loaded::add)
                }
            }
        } catch (_: Exception) {
            // the cause is not kept: its message may quote the request URL
            throw ParsingException("Could not get subtitles").also { subtitlesException = it }
        }
        return loaded.also { subtitles = it }
    }

    /** A caption, or `null` when its format is unknown or its file is not on the instance. */
    @Throws(ParsingException::class)
    private fun toSubtitlesStream(caption: JsonObject): SubtitlesStream? {
        val url = PeertubeLinkGuards.instanceUrlOrNull(instanceBaseUrl, JsonUtils.getString(caption, "captionPath"))
        val languageCode = JsonUtils.getString(caption, "language.id")
        if (url == null || languageCode.isEmpty()) {
            return null
        }
        val format = MediaFormat.getFromSuffix(url.substringAfterLast('.')) ?: return null
        return SubtitlesStream.Builder()
            .setContent(url, true)
            .setMediaFormat(format)
            .setLanguageCode(languageCode)
            .setAutoGenerated(false)
            .build()
    }

    @Throws(ParsingException::class)
    private fun extractLiveVideoStreams() {
        try {
            for (stream in json.getArray(STREAMING_PLAYLISTS)) {
                if (stream !is JsonObject) {
                    continue
                }
                val playlistUrl = PeertubeLinkGuards.webUrlOrNull(stream.getString(PLAYLIST_URL)) ?: continue
                // Don't use the containsSimilarStream method because it will always return
                // false so if there are multiples HLS URLs returned, only the first will be
                // extracted in this case.
                videoStreamList.add(
                    VideoStream.Builder()
                        .setId(stream.getInt("id", -1).toString())
                        .setContent(playlistUrl, true)
                        .setIsVideoOnly(false)
                        .setResolution("")
                        .setMediaFormat(MediaFormat.MPEG_4)
                        .setDeliveryMethod(DeliveryMethod.HLS)
                        .build(),
                )
            }
        } catch (e: Exception) {
            throw ParsingException("Could not get video streams", e)
        }
    }

    @Throws(ParsingException::class)
    private fun extractStreams() {
        // Progressive streams
        getStreamsFromArray(json.getArray(FILES), "")

        // HLS streams
        try {
            for (playlist in json.getArray(STREAMING_PLAYLISTS)) {
                if (playlist is JsonObject) {
                    getStreamsFromArray(
                        playlist.getArray(FILES),
                        PeertubeLinkGuards.webUrlOrNull(playlist.getString(PLAYLIST_URL)),
                    )
                }
            }
        } catch (e: Exception) {
            throw ParsingException("Could not get streams", e)
        }
    }

    /**
     * The files of a video. Their URLs may lead to another host than the instance (object storage,
     * as PeerTube allows), but must be web links: other schemes are dropped.
     */
    @Throws(ParsingException::class)
    private fun getStreamsFromArray(streams: JsonArray, playlistUrl: String?) {
        try {
            /*
            Starting with version 3.4.0 of PeerTube, the HLS playlist of stream resolutions
            contains the UUID of the streams, so we can't use the same method to get the URL of
            the HLS playlist without fetching the master playlist.
            These UUIDs are the same as the ones returned into the fileUrl and fileDownloadUrl
            strings.
            */
            val isInstanceUsingRandomUuidsForHlsStreams =
                !playlistUrl.isNullOrEmpty() && playlistUrl.endsWith("-master.m3u8")
            for (stream in streams.take(MAX_FILES)) {
                if (stream !is JsonObject) {
                    continue
                }
                // Extract stream version of streams first
                val idSuffix = if (stream.has(FILE_URL)) FILE_URL else FILE_DOWNLOAD_URL
                val url = PeertubeLinkGuards.webUrlOrNull(JsonUtils.getString(stream, idSuffix))
                if (url.isNullOrEmpty()) {
                    // Not a valid stream URL: this file is skipped, the next ones are kept
                    continue
                }

                val resolution = JsonUtils.getString(stream, "resolution.label")
                val source = StreamSource(stream, isInstanceUsingRandomUuidsForHlsStreams, resolution, idSuffix, url)
                if (resolution.lowercase(Locale.ROOT).contains("audio")) {
                    // An audio stream
                    addNewAudioStream(source, playlistUrl)
                } else {
                    // A video stream
                    addNewVideoStream(source, playlistUrl)
                }
            }
        } catch (e: Exception) {
            throw ParsingException("Could not get streams from array", e)
        }
    }

    /** What the stream builders need to know about one file of the video. */
    private class StreamSource(
        val json: JsonObject,
        val isInstanceUsingRandomUuidsForHlsStreams: Boolean,
        val resolution: String,
        val idSuffix: String,
        val url: String,
    ) {
        val extension: String = url.substringAfterLast('.')
        val format: MediaFormat? = MediaFormat.getFromSuffix(extension)
        val id: String = "$resolution-$extension"
    }

    @Throws(ParsingException::class)
    private fun getHlsPlaylistUrlFromFragmentedFileUrl(source: StreamSource): String {
        val streamUrl = if (FILE_DOWNLOAD_URL == source.idSuffix) {
            PeertubeLinkGuards.requireWebUrl(JsonUtils.getString(source.json, FILE_URL), "stream URL")
        } else {
            source.url
        }
        return streamUrl.replace("-fragmented." + source.extension, ".m3u8")
    }

    /** The resolution ID is placed in the URL as a number only. */
    @Throws(ParsingException::class)
    private fun getHlsPlaylistUrlFromMasterPlaylist(source: StreamSource, playlistUrl: String): String =
        playlistUrl.replace("master", JsonUtils.getNumber(source.json, RESOLUTION_ID).toLong().toString())

    @Throws(ParsingException::class)
    private fun getHlsStreamUrl(source: StreamSource, playlistUrl: String): String =
        if (source.isInstanceUsingRandomUuidsForHlsStreams) {
            getHlsPlaylistUrlFromFragmentedFileUrl(source)
        } else {
            getHlsPlaylistUrlFromMasterPlaylist(source, playlistUrl)
        }

    /** The torrent of a file, only when it is a web link. */
    @Throws(ParsingException::class)
    private fun getTorrentUrl(source: StreamSource): String? =
        PeertubeLinkGuards.webUrlOrNull(JsonUtils.getString(source.json, "torrentUrl"))

    @Throws(ParsingException::class)
    private fun addNewAudioStream(source: StreamSource, playlistUrl: String?) {
        // Add progressive HTTP streams first
        audioStreamList.add(
            AudioStream.Builder()
                .setId("${source.id}-${source.idSuffix}-${DeliveryMethod.PROGRESSIVE_HTTP}")
                .setContent(source.url, true)
                .setMediaFormat(source.format)
                .setAverageBitrate(AudioStream.UNKNOWN_BITRATE)
                .build(),
        )

        // Then add HLS streams
        if (!playlistUrl.isNullOrEmpty()) {
            val audioStream = AudioStream.Builder()
                .setId("${source.id}-${DeliveryMethod.HLS}")
                .setContent(getHlsStreamUrl(source, playlistUrl), true)
                .setDeliveryMethod(DeliveryMethod.HLS)
                .setMediaFormat(source.format)
                .setAverageBitrate(AudioStream.UNKNOWN_BITRATE)
                .setManifestUrl(playlistUrl)
                .build()
            if (!Stream.containSimilarStream(audioStream, audioStreamList)) {
                audioStreamList.add(audioStream)
            }
        }

        // Finally, add torrent URLs
        val torrentUrl = getTorrentUrl(source)
        if (!torrentUrl.isNullOrEmpty()) {
            audioStreamList.add(
                AudioStream.Builder()
                    .setId("${source.id}-${source.idSuffix}-${DeliveryMethod.TORRENT}")
                    .setContent(torrentUrl, true)
                    .setDeliveryMethod(DeliveryMethod.TORRENT)
                    .setMediaFormat(source.format)
                    .setAverageBitrate(AudioStream.UNKNOWN_BITRATE)
                    .build(),
            )
        }
    }

    @Throws(ParsingException::class)
    private fun addNewVideoStream(source: StreamSource, playlistUrl: String?) {
        // Check if the hasAudio property is present to keep compatibility with instances < 6.3.0,
        // otherwise any video stream with an audio track will be marked as video only for these
        // instances
        val isVideoOnly = source.json.has("hasAudio") && !source.json.getBoolean("hasAudio")

        // Add progressive HTTP streams first
        videoStreamList.add(
            VideoStream.Builder()
                .setId("${source.id}-${source.idSuffix}-${DeliveryMethod.PROGRESSIVE_HTTP}")
                .setContent(source.url, true)
                .setIsVideoOnly(isVideoOnly)
                .setResolution(source.resolution)
                .setMediaFormat(source.format)
                .build(),
        )

        // Then add HLS streams
        if (!playlistUrl.isNullOrEmpty()) {
            val videoStream = VideoStream.Builder()
                .setId("${source.id}-${DeliveryMethod.HLS}")
                .setContent(getHlsStreamUrl(source, playlistUrl), true)
                .setIsVideoOnly(isVideoOnly)
                .setDeliveryMethod(DeliveryMethod.HLS)
                .setResolution(source.resolution)
                .setMediaFormat(source.format)
                .setManifestUrl(playlistUrl)
                .build()
            if (!Stream.containSimilarStream(videoStream, videoStreamList)) {
                videoStreamList.add(videoStream)
            }
        }

        // Add finally torrent URLs
        val torrentUrl = getTorrentUrl(source)
        if (!torrentUrl.isNullOrEmpty()) {
            videoStreamList.add(
                VideoStream.Builder()
                    .setId("${source.id}-${source.idSuffix}-${DeliveryMethod.TORRENT}")
                    .setContent(torrentUrl, true)
                    .setIsVideoOnly(isVideoOnly)
                    .setDeliveryMethod(DeliveryMethod.TORRENT)
                    .setResolution(source.resolution)
                    .setMediaFormat(source.format)
                    .build(),
            )
        }
    }

    /**
     * Fetch content from a sub-API of the video.
     * @param subPath the API subpath after the video id,
     * e.g. "storyboards" for "/api/v1/videos/{id}/storyboards"
     * @return the [JsonObject] of the sub-API or null if the API does not exist
     * which is the case if the instance has an outdated PeerTube version.
     * @throws ParsingException if the API response could not be parsed to a [JsonObject]
     * @throws IOException if the API response could not be fetched
     * @throws ReCaptchaException if the API response is a reCaptcha
     */
    @Throws(ParsingException::class, IOException::class, ReCaptchaException::class)
    private fun fetchSubApiContent(subPath: String): JsonObject? {
        val response = downloader.get("$videoApiUrl/$subPath")
        if (response.responseCode() == HTTP_BAD_REQUEST) {
            // Chapter or segments support was added with PeerTube v6.0.0
            // This instance does not support it yet.
            return null
        }
        if (response.responseCode() != HTTP_OK) {
            throw ParsingException("Could not get segments from API. Response code: " + response.responseCode())
        }
        return try {
            JsonParser.`object`().from(response.responseBody())
        } catch (e: JsonParserException) {
            throw ParsingException("Could not parse json data for segments", e)
        }
    }

    @get:Throws(ParsingException::class)
    override val name: String
        get() = JsonUtils.getString(json, "name")

    @get:Throws(ParsingException::class)
    override val host: String
        get() = JsonUtils.getString(json, ACCOUNT_HOST)

    override val privacy: Privacy
        get() = when (json.getObject("privacy").getInt("id")) {
            PRIVACY_PUBLIC -> Privacy.PUBLIC
            PRIVACY_UNLISTED -> Privacy.UNLISTED
            PRIVACY_PRIVATE -> Privacy.PRIVATE
            PRIVACY_INTERNAL -> Privacy.INTERNAL
            else -> Privacy.OTHER
        }

    @get:Throws(ParsingException::class)
    override val category: String
        get() = JsonUtils.getString(json, "category.label")

    @get:Throws(ParsingException::class)
    override val licence: String
        get() = JsonUtils.getString(json, "licence.label")

    // Locale(String) is deprecated on Java 19+, but Locale.of() is missing from Android
    @Suppress("DEPRECATION")
    override val languageInfo: Locale?
        get() = try {
            Locale(JsonUtils.getString(json, "language.id"))
        } catch (_: ParsingException) {
            null
        }

    private companion object {
        private const val ACCOUNT_HOST = "account.host"
        private const val ACCOUNT_NAME = "account.name"
        private const val FILES = "files"
        private const val FILE_DOWNLOAD_URL = "fileDownloadUrl"
        private const val FILE_URL = "fileUrl"
        private const val PLAYLIST_URL = "playlistUrl"
        private const val RESOLUTION_ID = "resolution.id"
        private const val STREAMING_PLAYLISTS = "streamingPlaylists"

        /** Length of a description the instance shortened (ending with "..."). */
        private const val SHORT_DESCRIPTION_LENGTH = 250
        private const val ADULT_AGE_LIMIT = 18
        private const val TIMESTAMP_NOT_FOUND = -2L
        private const val RELATED_ITEMS_COUNT = 8
        private const val MILLIS_PER_SECOND = 1000

        /** Frames of one storyboard: a sprite sheet holds a few hundred. */
        private const val MAX_FRAMES = 100_000L

        /** Files of one video or playlist: an instance lists one per resolution. */
        private const val MAX_FILES = 64
        private const val HTTP_OK = 200
        private const val HTTP_BAD_REQUEST = 400

        private const val PRIVACY_PUBLIC = 1
        private const val PRIVACY_UNLISTED = 2
        private const val PRIVACY_PRIVATE = 3
        private const val PRIVACY_INTERNAL = 4
    }
}

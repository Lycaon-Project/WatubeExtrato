/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.Image.ResolutionLevel
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonWriter
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.YOUTUBEI_V1_URL
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.extractPlaylistTypeFromPlaylistId
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getValidJsonResponseBody
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getYouTubeHeaders
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.prepareDesktopJsonBuilder
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeLinkGuards
import org.schabi.newpipe.extractor.stream.Description
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector
import org.schabi.newpipe.extractor.utils.ImageSuffix
import org.schabi.newpipe.extractor.utils.JsonUtils
import org.schabi.newpipe.extractor.utils.Utils
import java.io.IOException

/**
 * A [YoutubePlaylistExtractor] for a mix (auto-generated playlist).
 * It handles URLs in the format of `youtube.com/watch?v=videoId&list=playlistId`
 */
public open class YoutubeMixPlaylistExtractor(
    service: StreamingService,
    linkHandler: ListLinkHandler,
) : PlaylistExtractor(service, linkHandler) {

    private var initialData: JsonObject? = null
    private var playlistData: JsonObject? = null

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        val localization = extractorLocalization
        val url = Utils.stringToURL(this.url)
        val videoId = Utils.getQueryValue(url, "v")
        val playlistIndex = Utils.getQueryValue(url, "index")?.toIntOrNull()

        val jsonBody = prepareDesktopJsonBuilder(localization, extractorContentCountry)
            .value("playlistId", id)
        if (videoId != null) {
            jsonBody.value("videoId", videoId)
        }
        if (playlistIndex != null) {
            jsonBody.value("playlistIndex", playlistIndex)
        }
        val body = JsonWriter.string(jsonBody.done()).toByteArray(Charsets.UTF_8)

        // Cookie is required due to consent
        val response = downloader.postWithContentTypeJson(NEXT_URL, getYouTubeHeaders(), body, localization)

        val json = JsonUtils.toJsonObject(getValidJsonResponseBody(response))
        val playlist = json.getObject("contents")
            .getObject("twoColumnWatchNextResults")
            .getObject("playlist")
            .getObject("playlist")
        if (playlist.isEmpty()) {
            val ex = ExtractionException("Could not get playlistData")
            if (!YoutubeParsingHelper.isConsentAccepted()) {
                throw ContentNotAvailableException("Consent is required in some countries to view Mix playlists", ex)
            }
            throw ex
        }
        initialData = json
        playlistData = playlist
    }

    @get:Throws(ParsingException::class)
    override val name: String
        get() = YoutubeParsingHelper.getTextAtKey(fetchedPlaylistData(), "title")
            ?.takeIf { it.isNotEmpty() }
            ?: throw ParsingException("Could not get playlist name")

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = try {
            thumbnailsFromPlaylistId(fetchedPlaylistData().getString("playlistId"))
        } catch (e: Exception) {
            try {
                // Fallback to thumbnail of current video. Always the case for channel mixes
                thumbnailsFromVideoId(
                    initialData?.getObject("currentVideoEndpoint")
                        ?.getObject("watchEndpoint")
                        ?.getString("videoId"),
                )
            } catch (_: Exception) {
                throw ParsingException("Could not get playlist thumbnails", e)
            }
        }

    // YouTube mixes are auto-generated by YouTube
    override val uploaderUrl: String
        get() = ""

    // YouTube mixes are auto-generated by YouTube
    override val uploaderName: String
        get() = "YouTube"

    // YouTube mixes are auto-generated by YouTube
    override val uploaderAvatars: List<Image>
        get() = emptyList()

    override val isUploaderVerified: Boolean
        get() = false

    // Auto-generated playlists always start with 25 videos and are endless
    override val streamCount: Long
        get() = ListExtractor.ITEM_COUNT_INFINITE

    override val description: Description
        get() = Description.EMPTY_DESCRIPTION

    @get:Throws(ParsingException::class)
    override val playlistType: PlaylistInfo.PlaylistType
        get() = extractPlaylistTypeFromPlaylistId(fetchedPlaylistData().getString("playlistId"))

    @get:Throws(IOException::class, ExtractionException::class)
    override val initialPage: InfoItemsPage<StreamInfoItem>
        get() {
            val playlist = fetchedPlaylistData()
            val collector = StreamInfoItemsCollector(serviceId)
            collectStreamsFrom(collector, playlist.getArray("contents"))
            return InfoItemsPage(collector, getNextPageFrom(playlist))
        }

    @Throws(ExtractionException::class)
    private fun getNextPageFrom(playlistJson: JsonObject): Page {
        val lastStream = playlistJson.getArray("contents")
            .filterIsInstance<JsonObject>()
            .lastOrNull { it.has(PLAYLIST_PANEL_VIDEO_RENDERER) }
            ?: throw ExtractionException("Could not extract next page url")

        val watchEndpoint = lastStream.getObject(PLAYLIST_PANEL_VIDEO_RENDERER)
            .getObject("navigationEndpoint")
            .getObject("watchEndpoint")
        val body = JsonWriter.string(
            prepareDesktopJsonBuilder(extractorLocalization, extractorContentCountry)
                .value("videoId", watchEndpoint.getString("videoId"))
                .value("playlistId", watchEndpoint.getString("playlistId"))
                .value("playlistIndex", watchEndpoint.getInt("index"))
                .value("params", watchEndpoint.getString("params"))
                .done(),
        ).toByteArray(Charsets.UTF_8)

        // no cookie: the visitor cookie of the first answer was never sent back, so it is not kept
        return Page(NEXT_URL, null, null, null, body)
    }

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<StreamInfoItem> {
        val pageUrl = page?.url
        require(!pageUrl.isNullOrEmpty()) { "Page doesn't contain an URL" }
        // Only the endpoint used by this extractor is accepted
        require(pageUrl == NEXT_URL) { "Page URL is not a YouTube next URL" }

        val collector = StreamInfoItemsCollector(serviceId)
        // Cookie is required due to consent
        val response = downloader.postWithContentTypeJson(
            pageUrl,
            getYouTubeHeaders(),
            page.body,
            extractorLocalization,
        )
        val ajaxJson = JsonUtils.toJsonObject(getValidJsonResponseBody(response))
        val playlistJson = ajaxJson.getObject("contents")
            .getObject("twoColumnWatchNextResults")
            .getObject("playlist")
            .getObject("playlist")
        val allStreams = playlistJson.getArray("contents")
        // Sublist because YouTube returns up to 24 previous streams in the mix
        // +1 because the stream of "currentIndex" was already extracted in previous request
        // Clamped, as the index comes from the response and may be out of range
        val start = (playlistJson.getInt("currentIndex") + 1).coerceIn(0, allStreams.size)
        collectStreamsFrom(collector, allStreams.subList(start, allStreams.size))
        return InfoItemsPage(collector, getNextPageFrom(playlistJson))
    }

    private fun collectStreamsFrom(collector: StreamInfoItemsCollector, streams: List<Any?>) {
        val timeAgoParser = timeAgoParser
        for (stream in streams.filterIsInstance<JsonObject>()) {
            // Never parse advertising or promoted renderers as items
            if (YoutubeItemGuards.isAd(stream) || !stream.has(PLAYLIST_PANEL_VIDEO_RENDERER)) {
                continue
            }
            collector.commit(
                YoutubeStreamInfoItemExtractor(stream.getObject(PLAYLIST_PANEL_VIDEO_RENDERER), timeAgoParser),
            )
        }
    }

    @Throws(ParsingException::class)
    private fun thumbnailsFromPlaylistId(playlistId: String?): List<Image> =
        thumbnailsFromVideoId(YoutubeParsingHelper.extractVideoIdFromMixId(playlistId))

    @Throws(ParsingException::class)
    private fun thumbnailsFromVideoId(videoId: String?): List<Image> {
        val baseUrl = "https://i.ytimg.com/vi/" + YoutubeLinkGuards.requireVideoId(videoId) + "/"
        return IMAGE_URL_SUFFIXES_AND_RESOLUTIONS.map {
            Image(baseUrl + it.suffix, it.height, it.width, it.resolutionLevel)
        }
    }

    private fun fetchedPlaylistData(): JsonObject =
        checkNotNull(playlistData) { "Page is not fetched. Make sure you call fetchPage()" }

    public companion object {
        /**
         * YouTube identifies mixes based on this cookie. With this information it can generate
         * continuations without duplicates.
         *
         * The extractor neither keeps nor sends it: continuations work without it, and a page
         * saved by an app holds no visitor identifier. Kept for the apps that refer to it.
         */
        public const val COOKIE_NAME: String = "VISITOR_INFO1_LIVE"

        private const val PLAYLIST_PANEL_VIDEO_RENDERER = "playlistPanelVideoRenderer"
        private val NEXT_URL = YOUTUBEI_V1_URL + "next?" + DISABLE_PRETTY_PRINT_PARAMETER

        // sqdefault and maxresdefault image resolutions are not available on all videos, so do not
        // add them in the list of available resolutions
        private val IMAGE_URL_SUFFIXES_AND_RESOLUTIONS = listOf(
            ImageSuffix("default.jpg", 90, 120, ResolutionLevel.LOW),
            ImageSuffix("mqdefault.jpg", 180, 320, ResolutionLevel.MEDIUM),
            ImageSuffix("hqdefault.jpg", 360, 480, ResolutionLevel.MEDIUM),
        )
    }
}

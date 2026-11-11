/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.extractors

import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.MetaInfo
import org.schabi.newpipe.extractor.MultiInfoItemsCollector
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonArray
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonWriter
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getTextFromObject
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getValidJsonResponseBody
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getYoutubeMusicClientVersion
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getYoutubeMusicHeaders
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.MUSIC_ALBUMS
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.MUSIC_ARTISTS
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.MUSIC_PLAYLISTS
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.MUSIC_SONGS
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.MUSIC_VIDEOS
import org.schabi.newpipe.extractor.utils.JsonUtils
import java.io.IOException

public open class YoutubeMusicSearchExtractor(
    service: StreamingService,
    linkHandler: SearchQueryHandler,
) : SearchExtractor(service, linkHandler) {

    private val searchType: String? = linkHandler.contentFilters.firstOrNull()

    private var initialData: JsonObject? = null

    private val itemSectionRendererContents: List<JsonObject> by lazy {
        fetchedInitialData()
            .getObject("contents")
            .getObject("tabbedSearchResultsRenderer")
            .getArray("tabs")
            .getObject(0)
            .getObject("tabRenderer")
            .getObject("content")
            .getObject("sectionListRenderer")
            .getArray("contents")
            .filterIsInstance<JsonObject>()
            .map { it.getObject("itemSectionRenderer") }
            .filter { !it.isEmpty() }
            .map { it.getArray("contents").getObject(0) }
    }

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        val params = when (searchType) {
            MUSIC_SONGS -> "Eg-KAQwIARAAGAAgACgAMABqChAEEAUQAxAKEAk%3D"
            MUSIC_VIDEOS -> "Eg-KAQwIABABGAAgACgAMABqChAEEAUQAxAKEAk%3D"
            MUSIC_ALBUMS -> "Eg-KAQwIABAAGAEgACgAMABqChAEEAUQAxAKEAk%3D"
            MUSIC_PLAYLISTS -> "Eg-KAQwIABAAGAAgACgBMABqChAEEAUQAxAKEAk%3D"
            MUSIC_ARTISTS -> "Eg-KAQwIABAAGAAgASgAMABqChAEEAUQAxAKEAk%3D"
            else -> null
        }
        val json = musicRequestBody(withQuery = true, params = params)
        initialData = JsonUtils.toJsonObject(
            getValidJsonResponseBody(
                downloader.postWithContentTypeJson(MUSIC_SEARCH_URL, getYoutubeMusicHeaders(), json),
            ),
        )
    }

    /** Builds the WEB_REMIX request body, with the search query only for the first page. */
    private fun musicRequestBody(withQuery: Boolean, params: String?): ByteArray {
        // TODO: provide a way to enable restricted mode with:
        //  .value("enableSafetyMode", boolean)
        val writer = JsonWriter.string()
            .`object`()
            .`object`("context")
            .`object`("client")
            .value("clientName", "WEB_REMIX")
            .value("clientVersion", getYoutubeMusicClientVersion())
            .value("hl", "en-GB")
            .value("gl", extractorContentCountry.countryCode)
            .value("platform", "DESKTOP")
            .value("utcOffsetMinutes", 0)
            .end()
            .`object`("request")
            .array("internalExperimentFlags")
            .end()
            .value("useSsl", true)
            .end()
            .`object`("user")
            .value("lockedSafetyMode", false)
            .end()
            .end()
        if (withQuery) {
            writer.value("query", searchString).value("params", params)
        }
        return writer.end().done().toByteArray(Charsets.UTF_8)
    }

    @get:Throws(ParsingException::class)
    override val searchSuggestion: String
        get() {
            for (obj in itemSectionRendererContents) {
                val didYouMeanRenderer = obj.getObject("didYouMeanRenderer")
                if (!didYouMeanRenderer.isEmpty()) {
                    return getTextFromObject(didYouMeanRenderer.getObject("correctedQuery")).orEmpty()
                }
                // NOTE: As of 2025-07 "showing results for ..." does not seem to be returned by
                // the backend anymore, however the code is still present in the JS frontend.
                val showingResultsForRenderer = obj.getObject("showingResultsForRenderer")
                if (!showingResultsForRenderer.isEmpty()) {
                    return JsonUtils.getString(
                        showingResultsForRenderer,
                        "correctedQueryEndpoint.searchEndpoint.query",
                    )
                }
            }
            return ""
        }

    @get:Throws(ParsingException::class)
    override val isCorrectedSearch: Boolean
        get() = itemSectionRendererContents.any { it.has("showingResultsForRenderer") }

    @get:Throws(ParsingException::class)
    override val metaInfo: List<MetaInfo>
        get() = emptyList()

    @get:Throws(IOException::class, ExtractionException::class)
    override val initialPage: InfoItemsPage<InfoItem>
        get() {
            val collector = MultiInfoItemsCollector(serviceId)
            val contents = JsonUtils.getArray(
                JsonUtils.getArray(fetchedInitialData(), "contents.tabbedSearchResultsRenderer.tabs").getObject(0),
                "tabRenderer.content.sectionListRenderer.contents",
            )
            var nextPage: Page? = null
            for (content in contents.filterIsInstance<JsonObject>()) {
                if (content.has("musicShelfRenderer")) {
                    val musicShelfRenderer = content.getObject("musicShelfRenderer")
                    collectMusicStreamsFrom(collector, musicShelfRenderer.getArray("contents"))
                    nextPage = getNextPageFrom(musicShelfRenderer.getArray("continuations"))
                }
            }
            return InfoItemsPage(collector, nextPage)
        }

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<InfoItem> {
        val pageUrl = page?.url
        require(!pageUrl.isNullOrEmpty()) { "Page doesn't contain an URL" }
        // Only continuation URLs built by this extractor are accepted
        require(pageUrl.startsWith(MUSIC_SEARCH_CONTINUATION_URL_PREFIX)) { "Page URL is not a search URL" }

        val collector = MultiInfoItemsCollector(serviceId)
        val json = musicRequestBody(withQuery = false, params = null)
        val ajaxJson = JsonUtils.toJsonObject(
            getValidJsonResponseBody(downloader.postWithContentTypeJson(pageUrl, getYoutubeMusicHeaders(), json)),
        )
        val musicShelfContinuation = ajaxJson.getObject("continuationContents")
            .getObject("musicShelfContinuation")
        collectMusicStreamsFrom(collector, musicShelfContinuation.getArray("contents"))
        return InfoItemsPage(collector, getNextPageFrom(musicShelfContinuation.getArray("continuations")))
    }

    private fun collectMusicStreamsFrom(collector: MultiInfoItemsCollector, videos: JsonArray) {
        val searchType = searchType ?: return
        for (item in videos.filterIsInstance<JsonObject>()) {
            // Never parse advertising or promoted renderers as items
            if (YoutubeItemGuards.isAd(item)) {
                continue
            }
            val infoItem = item.getObject("musicResponsiveListItemRenderer", null) ?: continue
            val displayPolicy = infoItem.getString("musicItemRendererDisplayPolicy", "")
            if (displayPolicy == "MUSIC_ITEM_RENDERER_DISPLAY_POLICY_GREY_OUT") {
                // No info about URL available
                continue
            }
            val descriptionElements = infoItem.getArray("flexColumns")
                .getObject(1)
                .getObject("musicResponsiveListItemFlexColumnRenderer")
                .getObject("text")
                .getArray("runs")
            when (searchType) {
                MUSIC_SONGS, MUSIC_VIDEOS -> collector.commit(
                    YoutubeMusicSongOrVideoInfoItemExtractor(infoItem, descriptionElements, searchType),
                )
                MUSIC_ARTISTS -> collector.commit(YoutubeMusicArtistInfoItemExtractor(infoItem))
                MUSIC_ALBUMS, MUSIC_PLAYLISTS -> collector.commit(
                    YoutubeMusicAlbumOrPlaylistInfoItemExtractor(infoItem, descriptionElements, searchType),
                )
            }
        }
    }

    private fun getNextPageFrom(continuations: JsonArray): Page? {
        if (continuations.isEmpty()) {
            return null
        }
        // The token is put in the URL query: refuse anything that is not URL-safe
        val continuation = YoutubeItemGuards.urlSafeTokenOrNull(
            continuations.getObject(0).getObject("nextContinuationData").getString("continuation"),
        ) ?: return null
        return Page(
            MUSIC_SEARCH_CONTINUATION_URL_PREFIX + continuation + "&continuation=" + continuation +
                "&" + DISABLE_PRETTY_PRINT_PARAMETER,
        )
    }

    private fun fetchedInitialData(): JsonObject =
        checkNotNull(initialData) { "Page is not fetched. Make sure you call fetchPage()" }

    private companion object {
        private const val MUSIC_SEARCH_BASE_URL = "https://music.youtube.com/youtubei/v1/search?"
        private const val MUSIC_SEARCH_CONTINUATION_URL_PREFIX = MUSIC_SEARCH_BASE_URL + "ctoken="
        private val MUSIC_SEARCH_URL = MUSIC_SEARCH_BASE_URL + DISABLE_PRETTY_PRINT_PARAMETER
    }
}

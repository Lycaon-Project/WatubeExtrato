/*
 * Created by Christian Schabesberger on 22.07.2018
 *
 * Copyright (C) 2018 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * YoutubeSearchExtractor.kt is part of NewPipe Extractor.
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
 * along with NewPipe Extractor.  If not, see <http://www.gnu.org/licenses/>.
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
import org.schabi.newpipe.extractor.services.youtube.YoutubeMetaInfoHelper
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.YOUTUBEI_V1_URL
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getJsonPostResponse
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getTextFromObject
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.prepareDesktopJsonBuilder
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.ALL
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.CHANNELS
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.PLAYLISTS
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.VIDEOS
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.getSearchParameter
import org.schabi.newpipe.extractor.utils.JsonUtils
import java.io.IOException

public open class YoutubeSearchExtractor(
    service: StreamingService,
    linkHandler: SearchQueryHandler,
) : SearchExtractor(service, linkHandler) {

    private val searchType: String? = linkHandler.contentFilters.firstOrNull()

    // Save whether we should extract video, channel and playlist results depending on the
    // requested search type, as YouTube returns sometimes videos inside channel search results
    // If no search type is provided or ALL filter is requested, extract everything
    private val extractAll = searchType == null || searchType == ALL
    private val extractVideoResults = extractAll || searchType == VIDEOS
    private val extractChannelResults = extractAll || searchType == CHANNELS
    private val extractPlaylistResults = extractAll || searchType == PLAYLISTS

    private var initialData: JsonObject? = null

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        val localization = extractorLocalization
        val params = getSearchParameter(searchType)
        val jsonBody = prepareDesktopJsonBuilder(localization, extractorContentCountry)
            .value("query", searchString)
        if (!params.isNullOrEmpty()) {
            jsonBody.value("params", params)
        }
        val body = JsonWriter.string(jsonBody.done()).toByteArray(Charsets.UTF_8)
        initialData = getJsonPostResponse("search", body, localization)
    }

    @get:Throws(ParsingException::class)
    override val url: String
        get() = super.url + "&gl=" + extractorContentCountry.countryCode

    @get:Throws(ParsingException::class)
    override val searchSuggestion: String
        get() {
            val firstContent = itemSectionRenderer().getArray("contents").getObject(0)
            val didYouMeanRenderer = firstContent.getObject("didYouMeanRenderer")
            if (!didYouMeanRenderer.isEmpty()) {
                return JsonUtils.getString(didYouMeanRenderer, "correctedQueryEndpoint.searchEndpoint.query")
            }
            return getTextFromObject(firstContent.getObject("showingResultsForRenderer").getObject("correctedQuery"))
                .orEmpty()
        }

    @get:Throws(ParsingException::class)
    override val isCorrectedSearch: Boolean
        get() = !itemSectionRenderer().getArray("contents").getObject(0)
            .getObject("showingResultsForRenderer")
            .isEmpty()

    @get:Throws(ParsingException::class)
    override val metaInfo: List<MetaInfo>
        get() = YoutubeMetaInfoHelper.getMetaInfo(sections())

    private fun sections(): JsonArray = fetchedInitialData().getObject("contents")
        .getObject("twoColumnSearchResultsRenderer")
        .getObject("primaryContents")
        .getObject("sectionListRenderer")
        .getArray("contents")

    private fun itemSectionRenderer(): JsonObject = sections().getObject(0).getObject("itemSectionRenderer")

    @get:Throws(IOException::class, ExtractionException::class)
    override val initialPage: InfoItemsPage<InfoItem>
        get() {
            val collector = MultiInfoItemsCollector(serviceId)
            var nextPage: Page? = null
            for (section in sections().filterIsInstance<JsonObject>()) {
                if (section.has("itemSectionRenderer")) {
                    collectStreamsFrom(collector, section.getObject("itemSectionRenderer").getArray("contents"))
                } else if (section.has("continuationItemRenderer")) {
                    nextPage = getNextPageFrom(section.getObject("continuationItemRenderer"))
                }
            }
            return InfoItemsPage(collector, nextPage)
        }

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<InfoItem> {
        require(page != null && !page.url.isNullOrEmpty()) { "Page doesn't contain an URL" }

        val localization = extractorLocalization
        val collector = MultiInfoItemsCollector(serviceId)
        val json = JsonWriter.string(
            prepareDesktopJsonBuilder(localization, extractorContentCountry)
                .value("continuation", page.id)
                .done(),
        ).toByteArray(Charsets.UTF_8)

        val ajaxJson = getJsonPostResponse("search", json, localization)
        val continuationItems = ajaxJson.getArray("onResponseReceivedCommands")
            .getObject(0)
            .getObject("appendContinuationItemsAction")
            .getArray("continuationItems")
        val contents = continuationItems.getObject(0)
            .getObject("itemSectionRenderer")
            .getArray("contents")
        collectStreamsFrom(collector, contents)

        return InfoItemsPage(
            collector,
            getNextPageFrom(continuationItems.getObject(1).getObject("continuationItemRenderer")),
        )
    }

    @Throws(NothingFoundException::class)
    private fun collectStreamsFrom(collector: MultiInfoItemsCollector, contents: JsonArray) {
        val timeAgoParser = timeAgoParser
        // the "no results" message is kept for the end: results found around it still count
        var nothingFoundMessage: String? = null
        for (item in contents.filterIsInstance<JsonObject>()) {
            // Never parse advertising or promoted renderers as items
            if (YoutubeItemGuards.isAd(item)) {
                continue
            }
            when {
                item.has("backgroundPromoRenderer") -> nothingFoundMessage =
                    getTextFromObject(item.getObject("backgroundPromoRenderer").getObject("bodyText"))
                item.has("videoRenderer") && extractVideoResults ->
                    collector.commit(YoutubeStreamInfoItemExtractor(item.getObject("videoRenderer"), timeAgoParser))
                item.has("channelRenderer") && extractChannelResults ->
                    collector.commit(YoutubeChannelInfoItemExtractor(item.getObject("channelRenderer")))
                item.has("playlistRenderer") && extractPlaylistResults ->
                    collector.commit(YoutubePlaylistInfoItemExtractor(item.getObject("playlistRenderer")))
                item.has("showRenderer") && extractPlaylistResults ->
                    collector.commit(YoutubeShowRendererInfoItemExtractor(item.getObject("showRenderer")))
                item.has("lockupViewModel") -> collectLockup(collector, item.getObject("lockupViewModel"))
            }
        }
        if (nothingFoundMessage != null && collector.items.isEmpty()) {
            throw NothingFoundException(nothingFoundMessage)
        }
    }

    private fun collectLockup(collector: MultiInfoItemsCollector, lockupViewModel: JsonObject) {
        when (lockupViewModel.getString("contentType")) {
            "LOCKUP_CONTENT_TYPE_PLAYLIST", "LOCKUP_CONTENT_TYPE_PODCAST" -> if (extractPlaylistResults) {
                collector.commit(YoutubeMixOrPlaylistLockupInfoItemExtractor(lockupViewModel))
            }
            "LOCKUP_CONTENT_TYPE_VIDEO" -> if (extractVideoResults) {
                collector.commit(YoutubeStreamInfoItemLockupExtractor(lockupViewModel, timeAgoParser))
            }
        }
    }

    private fun getNextPageFrom(continuationItemRenderer: JsonObject): Page? {
        if (continuationItemRenderer.isEmpty()) {
            return null
        }
        val token = continuationItemRenderer.getObject("continuationEndpoint")
            .getObject("continuationCommand")
            .getString("token")
        return Page(SEARCH_URL, token)
    }

    private fun fetchedInitialData(): JsonObject =
        checkNotNull(initialData) { "Page is not fetched. Make sure you call fetchPage()" }

    private companion object {
        private val SEARCH_URL = YOUTUBEI_V1_URL + "search?" + DISABLE_PRETTY_PRINT_PARAMETER
    }
}

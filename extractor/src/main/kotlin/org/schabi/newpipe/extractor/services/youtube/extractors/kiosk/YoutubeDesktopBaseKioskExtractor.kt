/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.extractors.kiosk

import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonArray
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonWriter
import org.schabi.newpipe.extractor.kiosk.KioskExtractor
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.services.youtube.InnertubeClientRequestInfo
import org.schabi.newpipe.extractor.services.youtube.YoutubeChannelHelper
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.YOUTUBEI_V1_URL
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getClientVersion
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getJsonPostResponse
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.prepareJsonBuilder
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeItemGuards
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamInfoItemExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamInfoItemLockupExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector
import java.io.IOException

/**
 * Base class of the kiosks extracted from YouTube desktop channel-like browse pages.
 *
 * Sealed: only the kiosks of this package extend it.
 */
public sealed class YoutubeDesktopBaseKioskExtractor(
    streamingService: StreamingService,
    linkHandler: ListLinkHandler,
    kioskId: String,
    protected val browseId: String,
    protected val params: String,
) : KioskExtractor<StreamInfoItem>(streamingService, linkHandler, kioskId) {

    protected var responseData: YoutubeChannelHelper.ChannelResponseData? = null

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        responseData = YoutubeChannelHelper.getChannelResponse(
            browseId,
            params,
            extractorLocalization,
            extractorContentCountry,
        )
    }

    private fun fetchedJsonResponse(): JsonObject =
        checkNotNull(responseData) { "Page is not fetched. Make sure you call fetchPage()" }.jsonResponse

    @get:Throws(ParsingException::class)
    override val name: String
        get() {
            val response = fetchedJsonResponse()
            return YoutubeChannelHelper.getChannelName(
                YoutubeChannelHelper.getChannelHeader(response),
                null,
                response,
            )
        }

    @get:Throws(IOException::class, ExtractionException::class)
    override val initialPage: InfoItemsPage<StreamInfoItem>
        get() {
            val response = fetchedJsonResponse()
            val tabRendererContent = response.getObject("contents")
                .getObject("twoColumnBrowseResultsRenderer")
                .getArray("tabs")
                .getObject(0)
                .getObject("tabRenderer")
                .getObject("content")
            val tabContents = when {
                tabRendererContent.has("sectionListRenderer") -> tabRendererContent
                    .getObject("sectionListRenderer")
                    .getArray("contents")
                    .getObject(0)
                    .getObject("itemSectionRenderer")
                    .getArray("contents")
                    .getObject(0)
                    .getObject("shelfRenderer")
                    .getObject("content")
                    .getObject("gridRenderer")
                    .getArray("items")
                tabRendererContent.has("richGridRenderer") ->
                    tabRendererContent.getObject("richGridRenderer").getArray("contents")
                else -> JsonArray()
            }
            val visitorData = response.getObject("responseContext").getString("visitorData")
            return collectStreamItems(tabContents, visitorData)
        }

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<StreamInfoItem> {
        val body = requireNotNull(page?.body) { "Page is null or doesn't contain a body" }
        val continuationResponse = getJsonPostResponse("browse", body, extractorLocalization)
        val continuationItems = continuationResponse.getArray("onResponseReceivedActions")
            .filterIsInstance<JsonObject>()
            .firstOrNull { it.has("appendContinuationItemsAction") }
            ?.getObject("appendContinuationItemsAction")
            ?.getArray("continuationItems")
            ?: JsonArray()
        // The page ID is the visitor data
        return collectStreamItems(continuationItems, page.id)
    }

    @Throws(IOException::class, ExtractionException::class)
    private fun collectStreamItems(items: JsonArray, visitorData: String?): InfoItemsPage<StreamInfoItem> {
        val collector = StreamInfoItemsCollector(serviceId)
        if (items.isEmpty()) {
            return InfoItemsPage(collector, null)
        }
        val timeAgoParser = timeAgoParser
        for (content in items.filterIsInstance<JsonObject>()) {
            // Never parse advertising or promoted renderers as items
            if (YoutubeItemGuards.isAd(content)) {
                continue
            }
            when {
                content.has("richItemRenderer") -> {
                    val richItem = content.getObject("richItemRenderer").getObject("content")
                    if (richItem.has("videoRenderer") && !YoutubeItemGuards.isAd(richItem)) {
                        collector.commit(
                            YoutubeStreamInfoItemExtractor(richItem.getObject("videoRenderer"), timeAgoParser),
                        )
                    }
                }
                content.has("gridVideoRenderer") -> collector.commit(
                    YoutubeStreamInfoItemExtractor(content.getObject("gridVideoRenderer"), timeAgoParser),
                )
                content.has("lockupViewModel") -> {
                    // lockupViewModels are not used yet, but may be in the future
                    val lockupViewModel = content.getObject("lockupViewModel")
                    if (lockupViewModel.getString("contentType") == "LOCKUP_CONTENT_TYPE_VIDEO") {
                        collector.commit(YoutubeStreamInfoItemLockupExtractor(lockupViewModel, timeAgoParser))
                    }
                }
            }
        }
        val lastContent = items.getObject(items.size - 1)
        val nextPage = if (lastContent.has("continuationItemRenderer")) {
            getNextPageFrom(lastContent.getObject("continuationItemRenderer"), visitorData)
        } else {
            null
        }
        return InfoItemsPage(collector, nextPage)
    }

    @Throws(IOException::class, ExtractionException::class)
    private fun getNextPageFrom(continuation: JsonObject, visitorData: String?): Page? {
        if (continuation.isEmpty()) {
            return null
        }
        val continuationToken = continuation.getObject("continuationEndpoint")
            .getObject("continuationCommand")
            .getString("token")

        // Visitor data is required to get videos in continuations, so we need to apply it to the
        // next page and save it as an ID so it can be applied to future continuations
        val webClientRequestInfo = InnertubeClientRequestInfo.ofWebClient()
        webClientRequestInfo.clientInfo.clientVersion = getClientVersion()
        webClientRequestInfo.clientInfo.visitorData = visitorData

        val body = JsonWriter.string(
            prepareJsonBuilder(extractorLocalization, extractorContentCountry, webClientRequestInfo, null)
                .value("continuation", continuationToken)
                .done(),
        ).toByteArray(Charsets.UTF_8)

        // The URL is not needed and used, it is only provided to make Page.isValid return true
        return Page(BROWSE_URL, visitorData, null, null, body)
    }

    private companion object {
        private val BROWSE_URL = YOUTUBEI_V1_URL + "browse?" + DISABLE_PRETTY_PRINT_PARAMETER
    }
}

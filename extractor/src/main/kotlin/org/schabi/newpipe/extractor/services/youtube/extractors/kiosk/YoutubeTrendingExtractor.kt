/*
 * Created by Christian Schabesberger on 12.08.17.
 *
 * Copyright (C) 2018 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * YoutubeTrendingExtractor.kt is part of NewPipe Extractor.
 *
 * NewPipe Extractor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe Extractor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe Extractor. If not, see <https://www.gnu.org/licenses/>.
 */

package org.schabi.newpipe.extractor.services.youtube.extractors.kiosk

import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonWriter
import org.schabi.newpipe.extractor.kiosk.KioskExtractor
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getJsonPostResponse
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getTextAtKey
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.prepareDesktopJsonBuilder
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeItemGuards
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamInfoItemExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector
import java.io.IOException

public open class YoutubeTrendingExtractor(
    service: StreamingService,
    linkHandler: ListLinkHandler,
    kioskId: String,
) : KioskExtractor<StreamInfoItem>(service, linkHandler, kioskId) {

    private var initialData: JsonObject? = null

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        val body = JsonWriter.string(
            prepareDesktopJsonBuilder(extractorLocalization, extractorContentCountry)
                .value("browseId", "FEtrending")
                .value("params", VIDEOS_TAB_PARAMS)
                .done(),
        ).toByteArray(Charsets.UTF_8)
        initialData = getJsonPostResponse("browse", body, extractorLocalization)
    }

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<StreamInfoItem> = InfoItemsPage.emptyPage()

    private fun fetchedInitialData(): JsonObject =
        checkNotNull(initialData) { "Page is not fetched. Make sure you call fetchPage()" }

    @get:Throws(ParsingException::class)
    override val name: String
        get() {
            val header = fetchedInitialData().getObject("header")
            val name = when {
                header.has("feedTabbedHeaderRenderer") ->
                    getTextAtKey(header.getObject("feedTabbedHeaderRenderer"), "title")
                header.has("c4TabbedHeaderRenderer") ->
                    getTextAtKey(header.getObject("c4TabbedHeaderRenderer"), "title")
                header.has("pageHeaderRenderer") ->
                    getTextAtKey(header.getObject("pageHeaderRenderer"), "pageTitle")
                else -> null
            }
            if (name.isNullOrEmpty()) {
                throw ParsingException("Could not get Trending name")
            }
            return name
        }

    @get:Throws(IOException::class, ExtractionException::class)
    override val initialPage: InfoItemsPage<StreamInfoItem>
        get() {
            val collector = StreamInfoItemsCollector(serviceId)
            val tab = trendingTab()
            val tabContent = tab.getObject("content")
            val isVideoTab = tab.getObject("endpoint").getObject("browseEndpoint")
                .getString("params", "") == VIDEOS_TAB_PARAMS

            val videoItems: List<JsonObject> = when {
                tabContent.has("richGridRenderer") -> tabContent.getObject("richGridRenderer")
                    .getArray("contents")
                    .filterIsInstance<JsonObject>()
                    // Filter Trending shorts and Recently trending sections
                    .filter { it.has("richItemRenderer") }
                    .map { it.getObject("richItemRenderer").getObject("content") }
                tabContent.has("sectionListRenderer") -> {
                    val shelves = tabContent.getObject("sectionListRenderer")
                        .getArray("contents")
                        .filterIsInstance<JsonObject>()
                        .flatMap {
                            it.getObject("itemSectionRenderer").getArray("contents").filterIsInstance<JsonObject>()
                        }
                        .map { it.getObject("shelfRenderer") }
                    val items = if (isVideoTab) {
                        // The first shelf of the Videos tab contains the normal trends
                        listOfNotNull(shelves.firstOrNull())
                    } else {
                        // Filter Trending shorts and Recently trending sections which have a
                        // title, contrary to normal trends
                        shelves.filter { !it.has("title") }
                    }
                    items.flatMap {
                        it.getObject("content")
                            .getObject("expandedShelfContentsRenderer")
                            .getArray("items")
                            .filterIsInstance<JsonObject>()
                    }
                }
                else -> emptyList()
            }

            val timeAgoParser = timeAgoParser
            for (item in videoItems) {
                // Never parse advertising or promoted renderers as items
                if (YoutubeItemGuards.isAd(item)) {
                    continue
                }
                collector.commit(YoutubeStreamInfoItemExtractor(item.getObject("videoRenderer"), timeAgoParser))
            }
            return InfoItemsPage(collector, null)
        }

    @Throws(ParsingException::class)
    private fun trendingTab(): JsonObject = fetchedInitialData().getObject("contents")
        .getObject("twoColumnBrowseResultsRenderer")
        .getArray("tabs")
        .filterIsInstance<JsonObject>()
        .map { it.getObject("tabRenderer") }
        // There should be at most one tab selected
        .firstOrNull { it.getBoolean("selected") && it.has("content") }
        ?: throw ParsingException("Could not get \"Now\" or \"Videos\" trending tab")

    public companion object {
        public const val KIOSK_ID: String = "Trending"

        private const val VIDEOS_TAB_PARAMS = "4gIOGgxtb3N0X3BvcHVsYXI%3D"
    }
}

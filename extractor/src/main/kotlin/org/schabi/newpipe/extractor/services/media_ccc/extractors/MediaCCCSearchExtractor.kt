/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.media_ccc.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.MetaInfo
import org.schabi.newpipe.extractor.MultiInfoItemsCollector
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.channel.ChannelInfoItemExtractor
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonParser
import org.schabi.newpipe.extractor.json.JsonParserException
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.services.media_ccc.extractors.infoItems.MediaCCCStreamInfoItemExtractor
import org.schabi.newpipe.extractor.services.media_ccc.linkHandler.MediaCCCConferencesListLinkHandlerFactory
import org.schabi.newpipe.extractor.services.media_ccc.linkHandler.MediaCCCSearchQueryHandlerFactory.ALL
import org.schabi.newpipe.extractor.services.media_ccc.linkHandler.MediaCCCSearchQueryHandlerFactory.CONFERENCES
import org.schabi.newpipe.extractor.services.media_ccc.linkHandler.MediaCCCSearchQueryHandlerFactory.EVENTS
import org.schabi.newpipe.extractor.utils.ExtractorLogger
import java.io.IOException
import java.util.Locale

public open class MediaCCCSearchExtractor(
    service: StreamingService,
    linkHandler: SearchQueryHandler,
) : SearchExtractor(service, linkHandler) {

    private var doc: JsonObject? = null

    /** `null` when the kiosk could not be created: conferences are then left out of the results. */
    private val conferenceKiosk: MediaCCCConferenceKiosk? = try {
        MediaCCCConferenceKiosk(
            service,
            MediaCCCConferencesListLinkHandlerFactory.fromId(MediaCCCConferenceKiosk.KIOSK_ID),
            MediaCCCConferenceKiosk.KIOSK_ID,
        )
    } catch (e: Exception) {
        ExtractorLogger.e(TAG, "Could not create the conferences kiosk", e)
        null
    }

    override val searchSuggestion: String
        get() = ""

    override val isCorrectedSearch: Boolean
        get() = false

    override val metaInfo: List<MetaInfo>
        get() = emptyList()

    private fun includes(filter: String): Boolean {
        val filters = linkHandler.contentFilters
        return filters.isEmpty() || filters.contains(filter) || filters.contains(ALL)
    }

    override val initialPage: InfoItemsPage<InfoItem>
        get() {
            val searchItems = MultiInfoItemsCollector(serviceId)
            val kiosk = conferenceKiosk
            if (includes(CONFERENCES) && kiosk != null) {
                searchConferences(searchString, kiosk.initialPage.items, searchItems)
            }
            if (includes(EVENTS)) {
                for (event in doc?.getArray("events").orEmpty()) {
                    // Ensure only uploaded talks are shown in the search results.
                    // If the release date is null, the talk has not been held or uploaded yet
                    // and no streams are going to be available anyway.
                    if (event is JsonObject && event.getString("release_date") != null) {
                        searchItems.commit(MediaCCCStreamInfoItemExtractor(event))
                    }
                }
            }
            return InfoItemsPage(searchItems, null)
        }

    override fun getPage(page: Page?): InfoItemsPage<InfoItem> = InfoItemsPage.emptyPage()

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        if (includes(EVENTS)) {
            val site = downloader.get(url, extractorLocalization).responseBody()
            doc = try {
                JsonParser.`object`().from(site)
            } catch (e: JsonParserException) {
                throw ExtractionException("Could not parse JSON.", e)
            }
        }
        if (includes(CONFERENCES)) {
            conferenceKiosk?.fetchPage()
        }
    }

    private fun searchConferences(
        searchString: String,
        channelItems: List<ChannelInfoItem>,
        collector: MultiInfoItemsCollector,
    ) {
        // Locale.ROOT, so that the match does not depend on the language of the device
        val query = searchString.uppercase(Locale.ROOT)
        for (item in channelItems) {
            if (item.name.uppercase(Locale.ROOT).contains(query)) {
                collector.commit(ConferenceItemExtractor(item))
            }
        }
    }

    /** Gives back a conference already extracted by the kiosk. */
    private class ConferenceItemExtractor(private val item: ChannelInfoItem) : ChannelInfoItemExtractor {
        override val description: String?
            get() = item.description

        override val subscriberCount: Long
            get() = item.subscriberCount

        override val streamCount: Long
            get() = item.streamCount

        override val isVerified: Boolean
            get() = false

        override val name: String
            get() = item.name

        override val url: String
            get() = item.url

        override val thumbnails: List<Image>
            get() = item.thumbnails
    }

    private companion object {
        private const val TAG = "MediaCCCSearchExtractor"
    }
}

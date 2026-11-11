/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.peertube.extractors

import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.kiosk.KioskExtractor
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.services.peertube.PeertubeLinkGuards
import org.schabi.newpipe.extractor.services.peertube.PeertubeParsingHelper
import org.schabi.newpipe.extractor.services.peertube.PeertubeParsingHelper.COUNT_KEY
import org.schabi.newpipe.extractor.services.peertube.PeertubeParsingHelper.ITEMS_PER_PAGE
import org.schabi.newpipe.extractor.services.peertube.PeertubeParsingHelper.START_KEY
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector
import java.io.IOException

public open class PeertubeTrendingExtractor(
    streamingService: StreamingService,
    linkHandler: ListLinkHandler,
    kioskId: String,
) : KioskExtractor<StreamInfoItem>(streamingService, linkHandler, kioskId) {

    @get:Throws(ParsingException::class)
    override val name: String
        get() = id

    @get:Throws(IOException::class, ExtractionException::class)
    override val initialPage: InfoItemsPage<StreamInfoItem>
        get() = getPage(Page("$url&$START_KEY=0&$COUNT_KEY=$ITEMS_PER_PAGE"))

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<StreamInfoItem> {
        val pageUrl = page?.url
        require(!pageUrl.isNullOrEmpty()) { "Page doesn't contain an URL" }

        // the address of the instance is checked before its answer is used
        val instanceBaseUrl = PeertubeLinkGuards.requireBaseUrl(baseUrl)
        val json = PeertubeParsingHelper.fetchJsonPage(downloader, pageUrl, "kiosk info")
        val total = json.getLong("total")
        val collector = StreamInfoItemsCollector(serviceId)
        PeertubeParsingHelper.collectItemsFrom(collector, json, instanceBaseUrl)
        return InfoItemsPage(collector, PeertubeParsingHelper.getNextPage(pageUrl, total))
    }

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        // nothing to fetch: the pages are requested by getPage()
    }
}

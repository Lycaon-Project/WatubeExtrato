/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.peertube.extractors

import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.MetaInfo
import org.schabi.newpipe.extractor.MultiInfoItemsCollector
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.services.peertube.PeertubeLinkGuards
import org.schabi.newpipe.extractor.services.peertube.PeertubeParsingHelper
import org.schabi.newpipe.extractor.services.peertube.PeertubeParsingHelper.COUNT_KEY
import org.schabi.newpipe.extractor.services.peertube.PeertubeParsingHelper.ITEMS_PER_PAGE
import org.schabi.newpipe.extractor.services.peertube.PeertubeParsingHelper.START_KEY
import java.io.IOException

public open class PeertubeSearchExtractor(
    service: StreamingService,
    linkHandler: SearchQueryHandler,
    // if we should use PeertubeSepiaStreamInfoItemExtractor
    private val sepia: Boolean,
) : SearchExtractor(service, linkHandler) {

    public constructor(service: StreamingService, linkHandler: SearchQueryHandler) :
        this(service, linkHandler, false)

    override val searchSuggestion: String
        get() = ""

    override val isCorrectedSearch: Boolean
        get() = false

    override val metaInfo: List<MetaInfo>
        get() = emptyList()

    @get:Throws(IOException::class, ExtractionException::class)
    override val initialPage: InfoItemsPage<InfoItem>
        get() = getPage(Page("$url&$START_KEY=0&$COUNT_KEY=$ITEMS_PER_PAGE"))

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<InfoItem> {
        val pageUrl = page?.url
        require(!pageUrl.isNullOrEmpty()) { "Page doesn't contain an URL" }

        // the address of the instance (or of SepiaSearch) is checked before its answer is used
        val instanceBaseUrl = PeertubeLinkGuards.requireBaseUrl(baseUrl)
        val json = PeertubeParsingHelper.fetchJsonPage(downloader, pageUrl, "search info")
        val total = json.getLong("total")
        val collector = MultiInfoItemsCollector(serviceId)
        PeertubeParsingHelper.collectItemsFrom(collector, json, instanceBaseUrl, sepia)
        return InfoItemsPage(collector, PeertubeParsingHelper.getNextPage(pageUrl, total))
    }

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        // nothing to fetch: the pages are requested by getPage()
    }
}

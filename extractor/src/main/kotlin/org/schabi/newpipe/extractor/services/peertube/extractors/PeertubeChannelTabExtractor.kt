/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.peertube.extractors

import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.MultiInfoItemsCollector
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.services.peertube.PeertubeLinkGuards
import org.schabi.newpipe.extractor.services.peertube.PeertubeParsingHelper
import org.schabi.newpipe.extractor.services.peertube.PeertubeParsingHelper.COUNT_KEY
import org.schabi.newpipe.extractor.services.peertube.PeertubeParsingHelper.ITEMS_PER_PAGE
import org.schabi.newpipe.extractor.services.peertube.PeertubeParsingHelper.START_KEY
import org.schabi.newpipe.extractor.services.peertube.linkHandler.PeertubeChannelLinkHandlerFactory
import org.schabi.newpipe.extractor.services.peertube.linkHandler.PeertubeChannelTabLinkHandlerFactory
import java.io.IOException

/**
 * @throws ParsingException if the link does not lead to a PeerTube instance reachable over https
 */
public open class PeertubeChannelTabExtractor @Throws(ParsingException::class) constructor(
    service: StreamingService,
    linkHandler: ListLinkHandler,
) : ChannelTabExtractor(service, linkHandler) {

    private val instanceBaseUrl: String = PeertubeLinkGuards.requireBaseUrl(linkHandler.baseUrl)

    override fun onFetchPage(downloader: Downloader) {
        // nothing to fetch: the pages are requested by getPage()
    }

    @get:Throws(IOException::class, ExtractionException::class)
    override val initialPage: InfoItemsPage<InfoItem>
        get() {
            val actorId = PeertubeLinkGuards.requireActorReference(id)
            return getPage(
                Page(
                    instanceBaseUrl + PeertubeChannelLinkHandlerFactory.API_ENDPOINT + actorId +
                        PeertubeChannelTabLinkHandlerFactory.getUrlSuffix(name) +
                        "?" + START_KEY + "=0&" + COUNT_KEY + "=" + ITEMS_PER_PAGE,
                ),
            )
        }

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<InfoItem> {
        val pageUrl = page?.url
        require(!pageUrl.isNullOrEmpty()) { "Page doesn't contain an URL" }

        val pageJson = PeertubeParsingHelper.fetchJsonPage(downloader, pageUrl, "account channel list")
        val collector = MultiInfoItemsCollector(serviceId)
        PeertubeParsingHelper.collectItemsFrom(collector, pageJson, instanceBaseUrl)
        return InfoItemsPage(collector, PeertubeParsingHelper.getNextPage(pageUrl, pageJson.getLong("total")))
    }
}

/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.extractors

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.feed.FeedExtractor
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector
import java.io.IOException

public open class YoutubeFeedExtractor(
    service: StreamingService,
    linkHandler: ListLinkHandler,
) : FeedExtractor(service, linkHandler) {

    private lateinit var document: Document

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        // The ID was validated by the link handler factory
        val feedUrl = YoutubeParsingHelper.getFeedUrlFrom(linkHandler.id)

        val response = downloader.get(feedUrl)
        if (response.responseCode() == HTTP_NOT_FOUND) {
            throw ContentNotAvailableException("Could not get feed: 404 - not found")
        }
        document = Jsoup.parse(response.responseBody())
    }

    override val initialPage: InfoItemsPage<StreamInfoItem>
        get() {
            val collector = StreamInfoItemsCollector(serviceId)
            for (entryElement in document.select("feed > entry").take(MAX_FEED_ENTRIES)) {
                collector.commit(YoutubeFeedInfoItemExtractor(entryElement))
            }
            return InfoItemsPage(collector, null)
        }

    override val id: String
        get() = url.replace(WEBSITE_CHANNEL_BASE_URL, "")

    override val url: String
        get() {
            // links to YouTube only: the ID of the channel is taken from it
            val authorUriText = document.selectFirst("feed > author > uri")?.text()
            if (!authorUriText.isNullOrEmpty() && YoutubeParsingHelper.isYoutubeLink(authorUriText)) {
                return authorUriText
            }
            return document.selectFirst("feed > link[rel*=alternate]")?.attr("href")
                ?.takeIf(YoutubeParsingHelper::isYoutubeLink).orEmpty()
        }

    override val name: String
        get() = document.selectFirst("feed > author > name")?.text().orEmpty()

    override fun getPage(page: Page?): InfoItemsPage<StreamInfoItem> = InfoItemsPage.emptyPage()

    private companion object {
        private const val WEBSITE_CHANNEL_BASE_URL = "https://www.youtube.com/channel/"
        private const val HTTP_NOT_FOUND = 404

        /** The feed lists 15 videos: a longer one is not read further. */
        private const val MAX_FEED_ENTRIES = 100
    }
}

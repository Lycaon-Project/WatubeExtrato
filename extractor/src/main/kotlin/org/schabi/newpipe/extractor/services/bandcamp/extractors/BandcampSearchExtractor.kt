// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later
//
// SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
// SPDX-License-Identifier: GPL-3.0-or-later
//

package org.schabi.newpipe.extractor.services.bandcamp.extractors

import org.jsoup.Jsoup
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage
import org.schabi.newpipe.extractor.MetaInfo
import org.schabi.newpipe.extractor.MultiInfoItemsCollector
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.BASE_URL
import org.schabi.newpipe.extractor.services.bandcamp.extractors.streaminfoitem.BandcampSearchStreamInfoItemExtractor
import java.io.IOException

public open class BandcampSearchExtractor(service: StreamingService, linkHandler: SearchQueryHandler) :
    SearchExtractor(service, linkHandler) {

    override val searchSuggestion: String
        get() = ""

    override val isCorrectedSearch: Boolean
        get() = false

    @get:Throws(ParsingException::class)
    override val metaInfo: List<MetaInfo>
        get() = emptyList()

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<InfoItem> {
        // Only search pages of Bandcamp are requested, never another host
        val pageUrl = page?.url
        if (pageUrl == null || !pageUrl.startsWith(SEARCH_URL_PREFIX)) {
            throw ParsingException("Invalid Bandcamp search page")
        }

        val collector = MultiInfoItemsCollector(serviceId)

        // TODO: idk why this cookie works. See https://github.com/TeamNewPipe/NewPipe/issues/13476
        // Replace with something more reliable
        val headers = mapOf("Cookie" to listOf("identity"))
        val response = downloader.get(pageUrl, headers).responseBody()
        val d = Jsoup.parse(response)

        for (searchResult in d.getElementsByClass("searchresult")) {
            val type = searchResult.getElementsByClass("result-info")
                .asSequence()
                .flatMap { it.getElementsByClass("itemtype").asSequence() }
                .firstOrNull()
                ?.text()
                .orEmpty()

            when (type) {
                "ARTIST" -> collector.commit(BandcampChannelInfoItemExtractor(searchResult))
                "ALBUM" -> collector.commit(BandcampPlaylistInfoItemExtractor(searchResult))
                "TRACK" -> collector.commit(BandcampSearchStreamInfoItemExtractor(searchResult, null))
                // don't display fan results ("FAN") or other things
                else -> Unit
            }
        }

        // Count pages
        val pages = d.getElementsByClass("pagelist").firstOrNull()?.getElementsByTag("li")
            ?: return InfoItemsPage(collector, null)

        // Find current page
        var currentPage = -1
        for (i in pages.indices) {
            if (pages[i].getElementsByTag("span").isNotEmpty()) {
                currentPage = i + 1
                break
            }
        }

        // The page number is replaced as a whole: the Java code replaced only the last digit,
        // which broke from page 10 on. Without a current page, there is no next page.
        val nextUrl = if (currentPage in 1 until pages.size) {
            PAGE_NUMBER.replace(pageUrl, "page=${currentPage + 1}")
        } else {
            null
        }

        return InfoItemsPage(collector, nextUrl?.let { Page(it) })
    }

    @get:Throws(IOException::class, ExtractionException::class)
    override val initialPage: InfoItemsPage<InfoItem>
        get() = getPage(Page(url))

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        // Nothing to fetch: the first page is requested by initialPage
    }

    private companion object {
        private const val SEARCH_URL_PREFIX = "$BASE_URL/search?"
        private val PAGE_NUMBER = Regex("page=\\d{1,9}$")
    }
}

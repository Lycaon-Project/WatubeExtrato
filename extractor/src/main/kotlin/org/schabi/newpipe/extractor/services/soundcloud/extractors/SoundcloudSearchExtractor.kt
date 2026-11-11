package org.schabi.newpipe.extractor.services.soundcloud.extractors

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
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper
import org.schabi.newpipe.extractor.services.soundcloud.linkHandler.SoundcloudSearchQueryHandlerFactory.ITEMS_PER_PAGE
import org.schabi.newpipe.extractor.utils.Parser
import org.schabi.newpipe.extractor.utils.Utils
import java.io.IOException
import java.net.MalformedURLException

public open class SoundcloudSearchExtractor(service: StreamingService, linkHandler: SearchQueryHandler) :
    SearchExtractor(service, linkHandler) {

    private lateinit var initialSearchObject: JsonObject

    override val searchSuggestion: String
        get() = ""

    override val isCorrectedSearch: Boolean
        get() = false

    override val metaInfo: List<MetaInfo>
        get() = emptyList()

    @get:Throws(IOException::class, ExtractionException::class)
    override val initialPage: InfoItemsPage<InfoItem>
        get() {
            val items = collectItems(initialSearchObject.getArray(COLLECTION))
            return if (initialSearchObject.getInt(TOTAL_RESULTS) > ITEMS_PER_PAGE) {
                InfoItemsPage(items, getNextPageFromCurrentUrl(url) { ITEMS_PER_PAGE })
            } else {
                InfoItemsPage(items, null)
            }
        }

    /** Only https links to SoundCloud are requested. */
    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<InfoItem> {
        val pageUrl = page?.url
        require(!pageUrl.isNullOrEmpty()) { "Page doesn't contain an URL" }

        val response = downloader.get(SoundcloudParsingHelper.requireApiUrl(pageUrl), extractorLocalization)
            .also(SoundcloudParsingHelper::noteRefusedClientId)
            .responseBody()
        val result = SoundcloudParsingHelper.parseObject(response)
        val searchCollection = result.getArray(COLLECTION)
        val totalResults = result.getInt(TOTAL_RESULTS)

        val items = collectItems(searchCollection)
        if (getOffsetFromUrl(pageUrl) + ITEMS_PER_PAGE < totalResults) {
            return InfoItemsPage(
                items,
                getNextPageFromCurrentUrl(pageUrl) { currentOffset -> currentOffset + ITEMS_PER_PAGE },
            )
        }
        return InfoItemsPage(items, null)
    }

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        val response = this.downloader.get(url, extractorLocalization)
            .also(SoundcloudParsingHelper::noteRefusedClientId)
            .responseBody()
        initialSearchObject = SoundcloudParsingHelper.parseObject(response)

        if (initialSearchObject.getArray(COLLECTION).isEmpty()) {
            throw NothingFoundException("Nothing found")
        }
    }

    private fun collectItems(searchCollection: JsonArray): MultiInfoItemsCollector {
        val collector = MultiInfoItemsCollector(serviceId)

        for (result in searchCollection) {
            if (result !is JsonObject) {
                continue
            }

            when (result.getString("kind", "")) {
                "user" -> collector.commit(SoundcloudChannelInfoItemExtractor(result))
                "track" -> collector.commit(SoundcloudStreamInfoItemExtractor(result))
                "playlist" -> collector.commit(SoundcloudPlaylistInfoItemExtractor(result))
            }
        }

        return collector
    }

    @Throws(ParsingException::class)
    private fun getNextPageFromCurrentUrl(currentUrl: String, newPageOffsetCalculator: (Int) -> Int): Page {
        val currentPageOffset = getOffsetFromUrl(currentUrl)

        return Page(
            currentUrl.replace(
                "&offset=$currentPageOffset",
                "&offset=" + newPageOffsetCalculator(currentPageOffset),
            ),
        )
    }

    /** A missing or non-numeric offset is a [ParsingException], where the Java code failed unchecked. */
    @Throws(ParsingException::class)
    private fun getOffsetFromUrl(url: String): Int {
        val query = try {
            Utils.stringToURL(url).query
        } catch (e: MalformedURLException) {
            throw ParsingException("Could not get offset from page URL", e)
        }
        return query?.let { Parser.compatParseMap(it)["offset"] }?.toIntOrNull()
            ?: throw ParsingException("Could not get offset from page URL")
    }

    private companion object {
        const val COLLECTION = "collection"
        const val TOTAL_RESULTS = "total_results"
    }
}

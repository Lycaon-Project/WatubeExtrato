package org.schabi.newpipe.extractor.services.soundcloud.extractors

import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.MultiInfoItemsCollector
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper.SOUNDCLOUD_API_V2_URL
import java.io.IOException

public open class SoundcloudChannelTabExtractor(service: StreamingService, linkHandler: ListLinkHandler) :
    ChannelTabExtractor(service, linkHandler) {

    private val userId: String = linkHandler.id

    @get:Throws(ParsingException::class)
    private val endpoint: String
        get() = when (name) {
            ChannelTabs.TRACKS -> "/tracks"
            ChannelTabs.PLAYLISTS -> "/playlists_without_albums"
            ChannelTabs.ALBUMS -> "/albums"
            ChannelTabs.LIKES -> "/likes"
            else -> throw ParsingException("Unsupported tab: $name")
        }

    override fun onFetchPage(downloader: Downloader) {
        // Nothing to fetch: the first page is requested by initialPage
    }

    override val id: String
        get() = userId

    /** Only a numeric user ID is placed in the API URL. */
    @get:Throws(IOException::class, ExtractionException::class)
    override val initialPage: InfoItemsPage<InfoItem>
        get() = getPage(
            Page(
                USERS_ENDPOINT + SoundcloudParsingHelper.requireNumericId(userId) + endpoint +
                    "?client_id=" + SoundcloudParsingHelper.clientId() + "&limit=20" +
                    "&linked_partitioning=1",
            ),
        )

    /**
     * Skips at most [MAX_EMPTY_PAGES] empty pages and never visits a page twice. Only https links
     * to SoundCloud are requested.
     */
    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<InfoItem> {
        val firstPageUrl = page?.url
        require(!firstPageUrl.isNullOrEmpty()) { "Page doesn't contain an URL" }

        val collector = MultiInfoItemsCollector(serviceId)
        val visitedPages = HashSet<String>()

        var currentPageUrl: String = firstPageUrl
        var nextPageUrl = ""
        var emptyPageCount = 0

        while (currentPageUrl.isNotEmpty()) {
            if (!visitedPages.add(currentPageUrl)) {
                // Prevent infinite loops when the API points back to an already visited page.
                break
            }

            val itemsBefore = collector.items.size
            val candidateNextPage = SoundcloudParsingHelper.getInfoItemsFromApi(collector, currentPageUrl)
            if (collector.items.size > itemsBefore) {
                nextPageUrl = candidateNextPage
                break
            }

            emptyPageCount++
            if (emptyPageCount >= MAX_EMPTY_PAGES) {
                // Give up after too many empty responses
                break
            }

            // An empty candidate (no more tokens from SoundCloud) ends the loop
            currentPageUrl = candidateNextPage
        }

        val nextPage = if (nextPageUrl.isEmpty()) null else Page(nextPageUrl)
        return InfoItemsPage(collector, nextPage)
    }

    private companion object {
        const val USERS_ENDPOINT = SOUNDCLOUD_API_V2_URL + "users/"

        /** Empty page cap against infinite pagination loops. */
        const val MAX_EMPTY_PAGES = 3
    }
}

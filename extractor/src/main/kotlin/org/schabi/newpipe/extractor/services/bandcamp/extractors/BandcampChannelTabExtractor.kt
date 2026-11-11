package org.schabi.newpipe.extractor.services.bandcamp.extractors

import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage
import org.schabi.newpipe.extractor.MultiInfoItemsCollector
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonArray
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.services.bandcamp.extractors.streaminfoitem.BandcampDiscographStreamInfoItemExtractor
import java.io.IOException

public open class BandcampChannelTabExtractor(service: StreamingService, linkHandler: ListLinkHandler) :
    ChannelTabExtractor(service, linkHandler) {

    private var discography: JsonArray? = null

    private val filter: String = when (val tab = linkHandler.contentFilters.firstOrNull()) {
        ChannelTabs.TRACKS -> "track"
        ChannelTabs.ALBUMS -> "album"
        // tab names are fixed by the services, never read from a link
        else -> throw IllegalArgumentException("Unsupported channel tab: $tab")
    }

    @Throws(ParsingException::class)
    override fun onFetchPage(downloader: Downloader) {
        if (discography == null) {
            discography = BandcampExtractorHelper.getArtistDetails(id).getArray("discography")
        }
    }

    @get:Throws(IOException::class, ExtractionException::class)
    override val initialPage: InfoItemsPage<InfoItem>
        get() {
            val items = checkNotNull(discography) { "Page is not fetched. Make sure you call fetchPage()" }
            val collector = MultiInfoItemsCollector(serviceId)
            val uploaderUrl = url

            for (discograph in items) {
                // A discograph is as an item appears in a discography
                if (discograph !is JsonObject) {
                    continue
                }

                val itemType = discograph.getString("item_type", "")
                if (itemType != filter) {
                    continue
                }

                when (itemType) {
                    "track" -> collector.commit(
                        BandcampDiscographStreamInfoItemExtractor(discograph, uploaderUrl),
                    )
                    "album" -> collector.commit(BandcampAlbumInfoItemExtractor(discograph, uploaderUrl))
                }
            }

            return InfoItemsPage(collector, null)
        }

    /** There is only one page: an empty page is returned (the Java code returned `null`). */
    override fun getPage(page: Page?): InfoItemsPage<InfoItem> = InfoItemsPage.emptyPage()

    public companion object {
        @JvmStatic
        public fun fromDiscography(
            service: StreamingService,
            linkHandler: ListLinkHandler,
            discography: JsonArray,
        ): BandcampChannelTabExtractor {
            val tabExtractor = BandcampChannelTabExtractor(service, linkHandler)
            tabExtractor.discography = discography
            return tabExtractor
        }
    }
}

// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

package org.schabi.newpipe.extractor.services.bandcamp.extractors

import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonParser
import org.schabi.newpipe.extractor.json.JsonParserException
import org.schabi.newpipe.extractor.kiosk.KioskExtractor
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.BASE_API_URL
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector
import java.io.IOException

public open class BandcampRadioExtractor(
    streamingService: StreamingService,
    linkHandler: ListLinkHandler,
    kioskId: String,
) : KioskExtractor<StreamInfoItem>(streamingService, linkHandler, kioskId) {

    private lateinit var json: JsonObject

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        json = try {
            JsonParser.`object`().from(this.downloader.get(RADIO_API_URL).responseBody())
        } catch (e: JsonParserException) {
            throw ExtractionException("Could not parse Bandcamp Radio API response", e)
        }
    }

    @get:Throws(ParsingException::class)
    override val name: String
        get() = KIOSK_RADIO

    override val initialPage: InfoItemsPage<StreamInfoItem>
        get() {
            val collector = StreamInfoItemsCollector(serviceId)

            val radioShows = json.getArray("results")

            for (i in 0 until radioShows.size) {
                val radioShow = radioShows.getObject(i)
                collector.commit(BandcampRadioInfoItemExtractor(radioShow))
            }

            return InfoItemsPage(collector, null)
        }

    /** There is only one page: an empty page is returned (the Java code returned `null`). */
    override fun getPage(page: Page?): InfoItemsPage<StreamInfoItem> = InfoItemsPage.emptyPage()

    public companion object {
        public const val KIOSK_RADIO: String = "Radio"
        public const val RADIO_API_URL: String = "$BASE_API_URL/bcweekly/3/list"
    }
}

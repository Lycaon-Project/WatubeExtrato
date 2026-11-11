/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.media_ccc.extractors

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
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector
import java.io.IOException
import java.time.Instant

public open class MediaCCCRecentKiosk(
    streamingService: StreamingService,
    linkHandler: ListLinkHandler,
    kioskId: String,
) : KioskExtractor<StreamInfoItem>(streamingService, linkHandler, kioskId) {

    private lateinit var doc: JsonObject

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        val site = downloader.get(RECENT_EVENTS_URL, extractorLocalization).responseBody()
        doc = try {
            JsonParser.`object`().from(site)
        } catch (e: JsonParserException) {
            throw ExtractionException("Could not parse json.", e)
        }
    }

    @get:Throws(IOException::class, ExtractionException::class)
    override val initialPage: InfoItemsPage<StreamInfoItem>
        get() {
            // Streams in the recent kiosk are not ordered by the release date.
            // Sort them to have the latest stream at the beginning of the list
            // (same order as before: nullsLast, then reversed).
            val comparator = compareBy<StreamInfoItem, Instant?>(nullsLast()) { it.uploadDate?.instant }
                .reversed()
            val collector = StreamInfoItemsCollector(serviceId, comparator)
            for (event in doc.getArray("events")) {
                if (event !is JsonObject) {
                    continue
                }
                val extractor = MediaCCCRecentKioskExtractor(event)
                // #813 / voc/voctoweb#609 -> returns faulty data -> filter it out
                if (extractor.duration > 0) {
                    collector.commit(extractor)
                }
            }
            return InfoItemsPage(collector, null)
        }

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<StreamInfoItem> = InfoItemsPage.emptyPage()

    @get:Throws(ParsingException::class)
    override val name: String
        get() = KIOSK_ID

    public companion object {
        public const val KIOSK_ID: String = "recent"
        private const val RECENT_EVENTS_URL = "https://api.media.ccc.de/public/events/recent"
    }
}

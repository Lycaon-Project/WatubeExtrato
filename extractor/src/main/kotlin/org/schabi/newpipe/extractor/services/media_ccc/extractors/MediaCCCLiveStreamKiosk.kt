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
import org.schabi.newpipe.extractor.json.JsonArray
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.kiosk.KioskExtractor
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector
import java.io.IOException

public open class MediaCCCLiveStreamKiosk(
    streamingService: StreamingService,
    linkHandler: ListLinkHandler,
    kioskId: String,
) : KioskExtractor<StreamInfoItem>(streamingService, linkHandler, kioskId) {

    private lateinit var doc: JsonArray

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        doc = MediaCCCParsingHelper.getLiveStreams(downloader, extractorLocalization)
    }

    @get:Throws(IOException::class, ExtractionException::class)
    override val initialPage: InfoItemsPage<StreamInfoItem>
        get() {
            val collector = StreamInfoItemsCollector(serviceId)
            for (conference in doc) {
                if (conference is JsonObject && conference.getBoolean("isCurrentlyStreaming")) {
                    collectRooms(collector, conference)
                }
            }
            return InfoItemsPage(collector, null)
        }

    private fun collectRooms(collector: StreamInfoItemsCollector, conference: JsonObject) {
        for (groupItem in conference.getArray("groups")) {
            val groupObject = groupItem as? JsonObject ?: continue
            val group = groupObject.getString("group")
            for (room in groupObject.getArray("rooms")) {
                if (room is JsonObject) {
                    collector.commit(MediaCCCLiveStreamKioskExtractor(conference, group, room))
                }
            }
        }
    }

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<StreamInfoItem> = InfoItemsPage.emptyPage()

    @get:Throws(ParsingException::class)
    override val name: String
        get() = KIOSK_ID

    public companion object {
        public const val KIOSK_ID: String = "live"
    }
}

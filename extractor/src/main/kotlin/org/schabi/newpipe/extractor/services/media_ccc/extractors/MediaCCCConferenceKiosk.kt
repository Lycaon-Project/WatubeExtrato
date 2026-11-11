/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.media_ccc.extractors

import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.channel.ChannelInfoItemsCollector
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonParser
import org.schabi.newpipe.extractor.json.JsonParserException
import org.schabi.newpipe.extractor.kiosk.KioskExtractor
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.services.media_ccc.extractors.infoItems.MediaCCCConferenceInfoItemExtractor
import java.io.IOException

public open class MediaCCCConferenceKiosk(
    streamingService: StreamingService,
    linkHandler: ListLinkHandler,
    kioskId: String,
) : KioskExtractor<ChannelInfoItem>(streamingService, linkHandler, kioskId) {

    private lateinit var doc: JsonObject

    override val initialPage: InfoItemsPage<ChannelInfoItem>
        get() {
            val collector = ChannelInfoItemsCollector(serviceId)
            for (conference in doc.getArray("conferences")) {
                if (conference is JsonObject) {
                    collector.commit(MediaCCCConferenceInfoItemExtractor(conference))
                }
            }
            return InfoItemsPage(collector, null)
        }

    override fun getPage(page: Page?): InfoItemsPage<ChannelInfoItem> = InfoItemsPage.emptyPage()

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        // the URL comes from the link handler factory, which only builds the fixed API address
        val site = downloader.get(linkHandler.url, extractorLocalization).responseBody()
        doc = try {
            JsonParser.`object`().from(site)
        } catch (e: JsonParserException) {
            throw ExtractionException("Could not parse json.", e)
        }
    }

    /** The API does not give a name: falls back to the kiosk ID, as the other kiosks do. */
    @get:Throws(ParsingException::class)
    override val name: String
        get() = doc.getString("Conferences") ?: KIOSK_ID

    public companion object {
        public const val KIOSK_ID: String = "conferences"
    }
}

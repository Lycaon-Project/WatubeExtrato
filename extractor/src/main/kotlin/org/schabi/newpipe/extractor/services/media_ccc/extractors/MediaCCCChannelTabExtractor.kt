/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.media_ccc.extractors

import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.MultiInfoItemsCollector
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.services.media_ccc.extractors.infoItems.MediaCCCStreamInfoItemExtractor
import java.io.IOException

/**
 * MediaCCC does not really have channel tabs, but rather a list of videos for each conference,
 * so this class just acts as a videos channel tab extractor.
 *
 * @param conferenceData will be not-null if conference data has already been fetched by
 * [MediaCCCConferenceExtractor]. Otherwise, if this parameter is `null`, conference data will be
 * fetched anew.
 */
public open class MediaCCCChannelTabExtractor(
    service: StreamingService,
    linkHandler: ListLinkHandler,
    private var conferenceData: JsonObject?,
) : ChannelTabExtractor(service, linkHandler) {

    @Throws(ExtractionException::class, IOException::class)
    override fun onFetchPage(downloader: Downloader) {
        if (conferenceData == null) {
            // only fetch conference data if we don't have it already
            conferenceData = MediaCCCConferenceExtractor.fetchConferenceData(downloader, id)
        }
    }

    override val initialPage: InfoItemsPage<InfoItem>
        get() {
            // will surely be != null after onFetchPage
            val data = checkNotNull(conferenceData) { "Page is not fetched. Make sure you call fetchPage()" }
            val collector = MultiInfoItemsCollector(serviceId)
            for (event in data.getArray("events")) {
                if (event is JsonObject) {
                    collector.commit(MediaCCCStreamInfoItemExtractor(event))
                }
            }
            return InfoItemsPage(collector, null)
        }

    override fun getPage(page: Page?): InfoItemsPage<InfoItem> = InfoItemsPage.emptyPage()
}

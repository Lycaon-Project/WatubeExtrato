/*
 * Created by Christian Schabesberger on 12.08.17.
 *
 * Copyright (C) 2017 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * KioskInfo.java is part of NewPipe Extractor.
 *
 * NewPipe Extractor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe Extractor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe Extractor.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.schabi.newpipe.extractor.kiosk

import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage
import org.schabi.newpipe.extractor.ListInfo
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.utils.ExtractorHelper
import java.io.IOException

public class KioskInfo private constructor(
    serviceId: Int,
    linkHandler: ListLinkHandler,
    name: String,
) : ListInfo<StreamInfoItem>(serviceId, linkHandler, name) {

    public companion object {
        private const val serialVersionUID: Long = 1L

        @JvmStatic
        @Throws(IOException::class, ExtractionException::class)
        public fun getMoreItems(
            service: StreamingService,
            url: String,
            page: Page?,
        ): InfoItemsPage<StreamInfoItem> =
            service.kioskList.getExtractorByUrl(url, page).asStreamKiosk().getPage(page)

        @JvmStatic
        @Throws(IOException::class, ExtractionException::class)
        public fun getInfo(url: String): KioskInfo = getInfo(NewPipe.getServiceByUrl(url), url)

        @JvmStatic
        @Throws(IOException::class, ExtractionException::class)
        public fun getInfo(service: StreamingService, url: String): KioskInfo {
            val extractor = service.kioskList.getExtractorByUrl(url, null)
            extractor.fetchPage()
            return getInfo(extractor)
        }

        /**
         * Get KioskInfo from KioskExtractor
         *
         * @param extractor an extractor where fetchPage() was already got called on.
         */
        @JvmStatic
        @Throws(ExtractionException::class)
        public fun getInfo(extractor: KioskExtractor<*>): KioskInfo {
            val info = KioskInfo(extractor.serviceId, extractor.linkHandler, extractor.name)

            val itemsPage = ExtractorHelper.getItemsPageOrLogError(info, extractor.asStreamKiosk())
            info.relatedItems = itemsPage.items
            info.nextPage = itemsPage.nextPage

            return info
        }

        /** Every kiosk lists streams (the Java API used raw types for them). */
        @Suppress("UNCHECKED_CAST")
        private fun KioskExtractor<*>.asStreamKiosk(): KioskExtractor<StreamInfoItem> =
            this as KioskExtractor<StreamInfoItem>
    }
}

package org.schabi.newpipe.extractor.feed

import org.schabi.newpipe.extractor.ListInfo
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.utils.ExtractorHelper
import java.io.IOException

public open class FeedInfo(
    serviceId: Int,
    id: String,
    url: String,
    originalUrl: String,
    name: String,
    contentFilter: List<String>?,
    sortFilter: String?,
) : ListInfo<StreamInfoItem>(serviceId, id, url, originalUrl, name, contentFilter, sortFilter) {

    public companion object {
        private const val serialVersionUID: Long = 1L

        @JvmStatic
        @Throws(IOException::class, ExtractionException::class)
        public fun getInfo(url: String): FeedInfo = getInfo(NewPipe.getServiceByUrl(url), url)

        @JvmStatic
        @Throws(IOException::class, ExtractionException::class)
        public fun getInfo(service: StreamingService, url: String): FeedInfo {
            val extractor = service.getFeedExtractor(url)
                ?: throw IllegalArgumentException(
                    "Service \"" + service.serviceInfo.name + "\" doesn't support FeedExtractor.",
                )

            extractor.fetchPage()
            return getInfo(extractor)
        }

        @JvmStatic
        @Throws(IOException::class, ExtractionException::class)
        public fun getInfo(extractor: FeedExtractor): FeedInfo {
            extractor.fetchPage()

            val info = FeedInfo(
                extractor.serviceId,
                extractor.id,
                extractor.url,
                extractor.originalUrl,
                extractor.name,
                null,
                null,
            )

            val itemsPage = ExtractorHelper.getItemsPageOrLogError(info, extractor)
            info.relatedItems = itemsPage.items
            info.nextPage = itemsPage.nextPage

            return info
        }
    }
}

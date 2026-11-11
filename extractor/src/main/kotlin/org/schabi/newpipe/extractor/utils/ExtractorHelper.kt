package org.schabi.newpipe.extractor.utils

import org.schabi.newpipe.extractor.Info
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamInfo

public object ExtractorHelper {

    @JvmStatic
    public fun <T : InfoItem> getItemsPageOrLogError(
        info: Info,
        extractor: ListExtractor<T>,
    ): InfoItemsPage<T> =
        try {
            val page = extractor.initialPage
            info.addAllErrors(page.errors)
            page
        } catch (e: Exception) {
            info.addError(e)
            InfoItemsPage.emptyPage()
        }

    @JvmStatic
    public fun getRelatedItemsOrLogError(
        info: StreamInfo,
        extractor: StreamExtractor,
    ): List<InfoItem> =
        try {
            val collector = extractor.relatedItems
            if (collector == null) {
                emptyList()
            } else {
                info.addAllErrors(collector.errors)
                collector.items
            }
        } catch (e: Exception) {
            info.addError(e)
            emptyList()
        }
}

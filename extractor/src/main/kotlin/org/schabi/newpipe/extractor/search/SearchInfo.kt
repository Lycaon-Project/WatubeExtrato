package org.schabi.newpipe.extractor.search

import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage
import org.schabi.newpipe.extractor.ListInfo
import org.schabi.newpipe.extractor.MetaInfo
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler
import org.schabi.newpipe.extractor.utils.ExtractorHelper
import java.io.IOException

public open class SearchInfo(
    serviceId: Int,
    qIHandler: SearchQueryHandler,
    public val searchString: String,
) : ListInfo<InfoItem>(serviceId, qIHandler, "Search") {

    public var searchSuggestion: String? = null

    public var isCorrectedSearch: Boolean = false
        private set

    public var metaInfo: List<MetaInfo> = emptyList()

    /** The Java name of the setter of [isCorrectedSearch], kept for Java and Kotlin callers. */
    public fun setIsCorrectedSearch(isCorrectedSearch: Boolean) {
        this.isCorrectedSearch = isCorrectedSearch
    }

    public companion object {
        private const val serialVersionUID: Long = 1L

        @JvmStatic
        @Throws(ExtractionException::class, IOException::class)
        public fun getInfo(service: StreamingService, searchQuery: SearchQueryHandler): SearchInfo {
            val extractor = service.getSearchExtractor(searchQuery)
            extractor.fetchPage()
            return getInfo(extractor)
        }

        /** Every field is optional: a failure is recorded in [errors] and the others still run. */
        @JvmStatic
        @Throws(ExtractionException::class, IOException::class)
        public fun getInfo(extractor: SearchExtractor): SearchInfo {
            val info = SearchInfo(extractor.serviceId, extractor.linkHandler, extractor.searchString)

            info.tryOrAddError { info.originalUrl = extractor.originalUrl }
            info.tryOrAddError { info.searchSuggestion = extractor.searchSuggestion }
            info.tryOrAddError { info.setIsCorrectedSearch(extractor.isCorrectedSearch) }
            info.tryOrAddError { info.metaInfo = extractor.metaInfo }

            val page = ExtractorHelper.getItemsPageOrLogError(info, extractor)
            info.relatedItems = page.items
            info.nextPage = page.nextPage

            return info
        }

        @JvmStatic
        @Throws(IOException::class, ExtractionException::class)
        public fun getMoreItems(
            service: StreamingService,
            query: SearchQueryHandler,
            page: Page?,
        ): InfoItemsPage<InfoItem> = service.getSearchExtractor(query).getPage(page)

        private inline fun SearchInfo.tryOrAddError(block: () -> Unit) {
            try {
                block()
            } catch (e: Exception) {
                addError(e)
            }
        }
    }
}

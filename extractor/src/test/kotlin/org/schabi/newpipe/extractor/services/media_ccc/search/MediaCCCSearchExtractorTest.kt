package org.schabi.newpipe.extractor.services.media_ccc.search

import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ServiceList.MediaCCC
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.services.DefaultSearchExtractorTest
import org.schabi.newpipe.extractor.services.media_ccc.linkHandler.MediaCCCSearchQueryHandlerFactory.CONFERENCES
import org.schabi.newpipe.extractor.services.media_ccc.linkHandler.MediaCCCSearchQueryHandlerFactory.EVENTS

class MediaCCCSearchExtractorTest {

    class All : DefaultSearchExtractorTest() {

        override fun createExtractor(): SearchExtractor = MediaCCC.getSearchExtractor(QUERY)

        override fun expectedService(): StreamingService = MediaCCC
        override fun expectedName(): String = QUERY
        override fun expectedId(): String = QUERY
        override fun expectedUrlContains(): String = "media.ccc.de/public/events/search?q=$QUERY"
        override fun expectedOriginalUrlContains(): String = "media.ccc.de/public/events/search?q=$QUERY"

        override fun expectedSearchString(): String = QUERY
        override fun expectedSearchSuggestion(): String? = null

        override fun expectedHasMoreItems(): Boolean = false

        companion object {
            private const val QUERY = "kde"
        }
    }

    class Conferences : DefaultSearchExtractorTest() {

        override fun createExtractor(): SearchExtractor =
            MediaCCC.getSearchExtractor(QUERY, listOf(CONFERENCES), "")

        override fun expectedService(): StreamingService = MediaCCC
        override fun expectedName(): String = QUERY
        override fun expectedId(): String = QUERY
        override fun expectedUrlContains(): String = "media.ccc.de/public/events/search?q=$QUERY"
        override fun expectedOriginalUrlContains(): String = "media.ccc.de/public/events/search?q=$QUERY"

        override fun expectedSearchString(): String = QUERY
        override fun expectedSearchSuggestion(): String? = null

        override fun expectedInfoItemType(): InfoItem.InfoType? = InfoItem.InfoType.CHANNEL
        override fun expectedHasMoreItems(): Boolean = false

        companion object {
            private const val QUERY = "c3"
        }
    }

    class Events : DefaultSearchExtractorTest() {

        override fun createExtractor(): SearchExtractor =
            MediaCCC.getSearchExtractor(QUERY, listOf(EVENTS), "")

        override fun expectedService(): StreamingService = MediaCCC
        override fun expectedName(): String = QUERY
        override fun expectedId(): String = QUERY
        override fun expectedUrlContains(): String = "media.ccc.de/public/events/search?q=$QUERY"
        override fun expectedOriginalUrlContains(): String = "media.ccc.de/public/events/search?q=$QUERY"

        override fun expectedSearchString(): String = QUERY
        override fun expectedSearchSuggestion(): String? = null

        override fun expectedInfoItemType(): InfoItem.InfoType? = InfoItem.InfoType.STREAM
        override fun expectedHasMoreItems(): Boolean = false

        companion object {
            private const val QUERY = "linux"
        }
    }
}

package org.schabi.newpipe.extractor.services.peertube.search

import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ServiceList.PeerTube
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.services.DefaultSearchExtractorTest
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest
import org.schabi.newpipe.extractor.services.DefaultTests.assertNoDuplicatedItems
import org.schabi.newpipe.extractor.services.peertube.PeertubeInstance
import org.schabi.newpipe.extractor.services.peertube.linkHandler.PeertubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.services.peertube.linkHandler.PeertubeSearchQueryHandlerFactory.VIDEOS

class PeertubeSearchExtractorTest {

    class All : DefaultSearchExtractorTest() {

        override fun createExtractor(): SearchExtractor {
            // setting instance might break test when running in parallel
            PeerTube.instance = PeertubeInstance("https://framatube.org", "Framatube")
            return PeerTube.getSearchExtractor(QUERY)
        }

        override fun expectedService(): StreamingService = PeerTube
        override fun expectedName(): String = QUERY
        override fun expectedId(): String = QUERY
        override fun expectedUrlContains(): String = "/search/videos?search=$QUERY"
        override fun expectedOriginalUrlContains(): String = "/search/videos?search=$QUERY"

        override fun expectedSearchString(): String = QUERY
        override fun expectedSearchSuggestion(): String? = null

        companion object {
            private const val QUERY = "fsf"
        }
    }

    class SepiaSearch : DefaultSearchExtractorTest() {

        override fun createExtractor(): SearchExtractor {
            // setting instance might break test when running in parallel
            PeerTube.instance = PeertubeInstance("https://framatube.org", "Framatube")
            return PeerTube.getSearchExtractor(
                QUERY,
                listOf(PeertubeSearchQueryHandlerFactory.SEPIA_VIDEOS),
                ""
            )
        }

        override fun expectedService(): StreamingService = PeerTube
        override fun expectedName(): String = QUERY
        override fun expectedId(): String = QUERY
        override fun expectedUrlContains(): String = "/search/videos?search=$QUERY"
        override fun expectedOriginalUrlContains(): String = "/search/videos?search=$QUERY"

        override fun expectedSearchString(): String = QUERY
        override fun expectedSearchSuggestion(): String? = null

        companion object {
            private const val QUERY = "kde"
        }
    }

    class PagingTest : DefaultSimpleExtractorTest<SearchExtractor>() {

        override fun createExtractor(): SearchExtractor =
            PeerTube.getSearchExtractor("internet", listOf(VIDEOS), "")

        @Test
        fun duplicatedItemsCheck() {
            val page1 = extractor().initialPage
            val page2 = extractor().getPage(page1.nextPage)

            assertNoDuplicatedItems(PeerTube, page1, page2)
        }
    }
}

package org.schabi.newpipe.extractor.services.youtube

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.services.BaseListExtractorTest
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest
import org.schabi.newpipe.extractor.services.DefaultTests.assertNoMoreItems
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestRelatedItems
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeFeedExtractor

class YoutubeFeedExtractorTest {

    class Kurzgesagt :
        DefaultSimpleExtractorTest<YoutubeFeedExtractor>(),
        BaseListExtractorTest,
        InitYoutubeTest {

        override fun createExtractor(): YoutubeFeedExtractor =
            YouTube.getFeedExtractor("https://www.youtube.com/user/Kurzgesagt") as YoutubeFeedExtractor

        @Test
        override fun testServiceId() {
            assertEquals(YouTube.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertTrue(extractor().name.startsWith("Kurzgesagt"))
        }

        @Test
        override fun testId() {
            assertEquals("UCsXVk37bltHxD1rDPwtNM8Q", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://www.youtube.com/channel/UCsXVk37bltHxD1rDPwtNM8Q", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals("https://www.youtube.com/user/Kurzgesagt", extractor().originalUrl)
        }

        @Test
        override fun testRelatedItems() {
            defaultTestRelatedItems(extractor())
        }

        @Test
        override fun testMoreRelatedItems() {
            assertNoMoreItems(extractor())
        }
    }

    class NotAvailable : InitYoutubeTest {

        @Test
        fun AccountTerminatedFetch() {
            val extractor = YouTube
                .getFeedExtractor("https://www.youtube.com/channel/UCTGjY2I-ZUGnwVoWAGRd7XQ") as YoutubeFeedExtractor
            assertThrows(ContentNotAvailableException::class.java) { extractor.fetchPage() }
        }
    }
}

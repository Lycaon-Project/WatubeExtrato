package org.schabi.newpipe.extractor.services.peertube

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ServiceList.PeerTube
import org.schabi.newpipe.extractor.services.BaseListExtractorTest
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestMoreItems
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestRelatedItems
import org.schabi.newpipe.extractor.services.peertube.extractors.PeertubeTrendingExtractor

class PeertubeTrendingExtractorTest {

    class Trending :
        DefaultSimpleExtractorTest<PeertubeTrendingExtractor>(),
        BaseListExtractorTest {

        override fun createExtractor(): PeertubeTrendingExtractor {
            // setting instance might break test when running in parallel
            PeerTube.instance = PeertubeInstance("https://framatube.org", "Framatube")
            return PeerTube.kioskList.getExtractorById("Trending", null) as PeertubeTrendingExtractor
        }

        @Test
        override fun testServiceId() {
            assertEquals(PeerTube.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertEquals("Trending", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("Trending", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://framatube.org/api/v1/videos?sort=-trending", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals("https://framatube.org/api/v1/videos?sort=-trending", extractor().originalUrl)
        }

        @Test
        override fun testRelatedItems() {
            defaultTestRelatedItems(extractor())
        }

        @Test
        override fun testMoreRelatedItems() {
            defaultTestMoreItems(extractor())
        }
    }
}

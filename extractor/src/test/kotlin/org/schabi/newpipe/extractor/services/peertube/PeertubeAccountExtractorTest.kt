package org.schabi.newpipe.extractor.services.peertube

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ExtractorAsserts
import org.schabi.newpipe.extractor.ExtractorAsserts.assertEmpty
import org.schabi.newpipe.extractor.ExtractorAsserts.assertTabsContain
import org.schabi.newpipe.extractor.ServiceList.PeerTube
import org.schabi.newpipe.extractor.channel.ChannelExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.services.BaseChannelExtractorTest
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestImageCollection
import org.schabi.newpipe.extractor.services.peertube.extractors.PeertubeAccountExtractor

/**
 * Test for [PeertubeAccountExtractor]
 */
class PeertubeAccountExtractorTest {

    abstract class Base :
        DefaultSimpleExtractorTest<ChannelExtractor>(),
        BaseChannelExtractorTest {

        override fun createExtractor(): ChannelExtractor {
            // setting instance might break test when running in parallel
            PeerTube.instance = peertubeInstance()
            return PeerTube.getChannelExtractor(extractorUrl())
        }

        protected abstract fun peertubeInstance(): PeertubeInstance

        protected abstract fun extractorUrl(): String
    }

    class Framasoft : Base() {

        override fun peertubeInstance(): PeertubeInstance =
            PeertubeInstance("https://framatube.org", "Framatube")

        override fun extractorUrl(): String = "https://framatube.org/accounts/framasoft"

        @Test
        override fun testServiceId() {
            assertEquals(PeerTube.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertEquals("Framasoft", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("accounts/framasoft", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://framatube.org/accounts/framasoft", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals("https://framatube.org/accounts/framasoft", extractor().originalUrl)
        }

        @Test
        override fun testDescription() {
            assertNull(extractor().description)
        }

        @Test
        override fun testAvatars() {
            defaultTestImageCollection(extractor().avatars)
        }

        @Test
        override fun testBanners() {
            assertEmpty(extractor().banners)
        }

        @Test
        override fun testFeedUrl() {
            assertEquals("https://framatube.org/feeds/videos.xml?accountId=3", extractor().feedUrl)
        }

        @Test
        override fun testSubscriberCount() {
            ExtractorAsserts.assertGreaterOrEqual(700, extractor().subscriberCount)
        }

        @Test
        override fun testVerified() {
            assertFalse(extractor().isVerified)
        }

        @Test
        override fun testTabs() {
            assertTabsContain(extractor().tabs, ChannelTabs.VIDEOS, ChannelTabs.CHANNELS)
        }

        @Test
        override fun testTags() {
            assertTrue(extractor().tags.isEmpty())
        }
    }

    class FreeSoftwareFoundation : Base() {

        override fun peertubeInstance(): PeertubeInstance =
            PeertubeInstance("https://framatube.org", "Framatube")

        override fun extractorUrl(): String = "https://framatube.org/api/v1/accounts/fsf"

        @Test
        override fun testServiceId() {
            assertEquals(PeerTube.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertEquals("Free Software Foundation", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("accounts/fsf", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://framatube.org/accounts/fsf", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals("https://framatube.org/api/v1/accounts/fsf", extractor().originalUrl)
        }

        @Test
        override fun testDescription() {
            assertNotNull(extractor().description)
        }

        @Test
        override fun testAvatars() {
            defaultTestImageCollection(extractor().avatars)
        }

        @Test
        override fun testBanners() {
            assertEmpty(extractor().banners)
        }

        @Test
        override fun testFeedUrl() {
            assertEquals("https://framatube.org/feeds/videos.xml?accountId=8178", extractor().feedUrl)
        }

        @Test
        override fun testSubscriberCount() {
            ExtractorAsserts.assertGreaterOrEqual(100, extractor().subscriberCount)
        }

        @Test
        override fun testVerified() {
            assertFalse(extractor().isVerified)
        }

        @Test
        override fun testTabs() {
            assertTabsContain(extractor().tabs, ChannelTabs.VIDEOS, ChannelTabs.CHANNELS)
        }

        @Test
        override fun testTags() {
            assertTrue(extractor().tags.isEmpty())
        }
    }
}

package org.schabi.newpipe.extractor.services.soundcloud

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ExtractorAsserts.assertEmpty
import org.schabi.newpipe.extractor.ExtractorAsserts.assertTabsContain
import org.schabi.newpipe.extractor.ServiceList.SoundCloud
import org.schabi.newpipe.extractor.channel.ChannelExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.services.BaseChannelExtractorTest
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestImageCollection

/**
 * Test for [org.schabi.newpipe.extractor.services.soundcloud.extractors.SoundcloudChannelExtractor]
 */
class SoundcloudChannelExtractorTest {
    abstract class Base :
        DefaultSimpleExtractorTest<ChannelExtractor>(),
        BaseChannelExtractorTest {

        @Throws(Exception::class)
        override fun createExtractor(): ChannelExtractor = SoundCloud.getChannelExtractor(extractorUrl())

        protected abstract fun extractorUrl(): String
    }

    class LilUzi : Base() {

        override fun extractorUrl(): String = "http://soundcloud.com/liluzivert/sets"

        @Test
        override fun testServiceId() {
            assertEquals(SoundCloud.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertEquals("Lil Uzi Vert", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("10494998", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://soundcloud.com/liluzivert", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals("http://soundcloud.com/liluzivert/sets", extractor().originalUrl)
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
            defaultTestImageCollection(extractor().banners)
        }

        @Test
        override fun testFeedUrl() {
            assertEmpty(extractor().feedUrl)
        }

        @Test
        override fun testSubscriberCount() {
            assertTrue(extractor().subscriberCount >= 1e6, "Wrong subscriber count")
        }

        override fun testVerified() {
            assertTrue(extractor().isVerified)
        }

        @Test
        override fun testTabs() {
            assertTabsContain(
                extractor().tabs, ChannelTabs.TRACKS, ChannelTabs.PLAYLISTS,
                ChannelTabs.ALBUMS, ChannelTabs.LIKES
            )
        }

        @Test
        override fun testTags() {
            assertTrue(extractor().tags.isEmpty())
        }
    }

    class DubMatix : Base() {

        override fun extractorUrl(): String = "https://soundcloud.com/dubmatix"

        @Test
        override fun testServiceId() {
            assertEquals(SoundCloud.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertEquals("dubmatix", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("542134", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://soundcloud.com/dubmatix", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals("https://soundcloud.com/dubmatix", extractor().originalUrl)
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
            defaultTestImageCollection(extractor().banners)
        }

        @Test
        override fun testFeedUrl() {
            assertEmpty(extractor().feedUrl)
        }

        @Test
        override fun testSubscriberCount() {
            assertTrue(extractor().subscriberCount >= 2e6, "Wrong subscriber count")
        }

        override fun testVerified() {
            assertTrue(extractor().isVerified)
        }

        @Test
        override fun testTabs() {
            assertTabsContain(
                extractor().tabs, ChannelTabs.TRACKS, ChannelTabs.PLAYLISTS,
                ChannelTabs.ALBUMS, ChannelTabs.LIKES
            )
        }

        @Test
        override fun testTags() {
            assertTrue(extractor().tags.isEmpty())
        }
    }
}

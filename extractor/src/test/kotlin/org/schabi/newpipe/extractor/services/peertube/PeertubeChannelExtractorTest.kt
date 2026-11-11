package org.schabi.newpipe.extractor.services.peertube

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ExtractorAsserts.assertEmpty
import org.schabi.newpipe.extractor.ExtractorAsserts.assertGreaterOrEqual
import org.schabi.newpipe.extractor.ExtractorAsserts.assertTabsContain
import org.schabi.newpipe.extractor.ServiceList.PeerTube
import org.schabi.newpipe.extractor.channel.ChannelExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.services.BaseChannelExtractorTest
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestImageCollection
import org.schabi.newpipe.extractor.services.peertube.extractors.PeertubeChannelExtractor

/**
 * Test for [PeertubeChannelExtractor]
 */
class PeertubeChannelExtractorTest {

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

    class LaQuadratureDuNet : Base() {
        override fun peertubeInstance(): PeertubeInstance =
            PeertubeInstance("https://framatube.org", "Framatube")

        override fun extractorUrl(): String =
            "https://framatube.org/video-channels/lqdn_channel@video.lqdn.fr/videos"

        @Test
        override fun testServiceId() {
            assertEquals(PeerTube.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertEquals("La Quadrature du Net", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("video-channels/lqdn_channel@video.lqdn.fr", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://framatube.org/video-channels/lqdn_channel@video.lqdn.fr", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals(
                "https://framatube.org/video-channels/lqdn_channel@video.lqdn.fr/videos",
                extractor().originalUrl
            )
        }

        @Test
        override fun testDescription() {
            assertNotNull(extractor().description)
        }

        @Test
        fun testParentChannelName() {
            assertEquals("lqdn", extractor().parentChannelName)
        }

        @Test
        fun testParentChannelUrl() {
            assertEquals("https://video.lqdn.fr/accounts/lqdn", extractor().parentChannelUrl)
        }

        @Test
        fun testParentChannelAvatarUrl() {
            defaultTestImageCollection(extractor().parentChannelAvatars)
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
            assertEquals("https://framatube.org/feeds/videos.xml?videoChannelId=1126", extractor().feedUrl)
        }

        @Test
        override fun testSubscriberCount() {
            assertGreaterOrEqual(230, extractor().subscriberCount)
        }

        @Test
        override fun testVerified() {
            assertFalse(extractor().isVerified)
        }

        @Test
        override fun testTabs() {
            assertTabsContain(extractor().tabs, ChannelTabs.VIDEOS, ChannelTabs.PLAYLISTS)
        }

        @Test
        override fun testTags() {
            assertTrue(extractor().tags.isEmpty())
        }
    }

    class ChatSceptique : Base() {
        override fun peertubeInstance(): PeertubeInstance =
            PeertubeInstance("https://framatube.org", "Framatube")

        override fun extractorUrl(): String =
            "https://framatube.org/api/v1/video-channels/chatsceptique@skeptikon.fr"

        @Test
        override fun testServiceId() {
            assertEquals(PeerTube.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertEquals("Chat Sceptique", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("video-channels/chatsceptique@skeptikon.fr", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://framatube.org/video-channels/chatsceptique@skeptikon.fr", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals(
                "https://framatube.org/api/v1/video-channels/chatsceptique@skeptikon.fr",
                extractor().originalUrl
            )
        }

        @Test
        override fun testDescription() {
            assertNotNull(extractor().description)
        }

        @Test
        fun testParentChannelName() {
            assertEquals("nathan", extractor().parentChannelName)
        }

        @Test
        fun testParentChannelUrl() {
            assertEquals("https://skeptikon.fr/accounts/nathan", extractor().parentChannelUrl)
        }

        @Test
        fun testParentChannelAvatars() {
            defaultTestImageCollection(extractor().parentChannelAvatars)
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
            assertEquals("https://framatube.org/feeds/videos.xml?videoChannelId=137", extractor().feedUrl)
        }

        @Test
        override fun testSubscriberCount() {
            assertGreaterOrEqual(700, extractor().subscriberCount)
        }

        @Test
        override fun testVerified() {
            assertFalse(extractor().isVerified)
        }

        @Test
        override fun testTabs() {
            assertTabsContain(extractor().tabs, ChannelTabs.VIDEOS, ChannelTabs.PLAYLISTS)
        }

        @Test
        override fun testTags() {
            assertTrue(extractor().tags.isEmpty())
        }
    }

    class DocumentaryChannel : Base() {
        // https://github.com/TeamNewPipe/NewPipe/issues/11369

        override fun peertubeInstance(): PeertubeInstance =
            PeertubeInstance("https://kolektiva.media", "kolektiva.media")

        override fun extractorUrl(): String = "https://kolektiva.media/video-channels/documentary_channel"

        @Test
        override fun testServiceId() {
            assertEquals(PeerTube.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertEquals("Documentary Channel", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("video-channels/documentary_channel", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://kolektiva.media/video-channels/documentary_channel", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals(
                "https://kolektiva.media/video-channels/documentary_channel",
                extractor().originalUrl
            )
        }

        @Test
        override fun testDescription() {
            assertNotNull(extractor().description)
        }

        @Test
        fun testParentChannelName() {
            assertEquals("consumedmind", extractor().parentChannelName)
        }

        @Test
        fun testParentChannelUrl() {
            assertEquals("https://kolektiva.media/accounts/consumedmind", extractor().parentChannelUrl)
        }

        @Test
        fun testParentChannelAvatars() {
            defaultTestImageCollection(extractor().parentChannelAvatars)
        }

        @Test
        override fun testAvatars() {
            defaultTestImageCollection(extractor().avatars)
        }

        @Test
        override fun testBanners() {
            assertGreaterOrEqual(1, extractor().banners.size.toLong())
        }

        @Test
        override fun testFeedUrl() {
            assertEquals("https://kolektiva.media/feeds/videos.xml?videoChannelId=2994", extractor().feedUrl)
        }

        @Test
        override fun testSubscriberCount() {
            assertGreaterOrEqual(25, extractor().subscriberCount)
        }

        @Test
        override fun testVerified() {
            assertFalse(extractor().isVerified)
        }

        @Test
        override fun testTabs() {
            assertTabsContain(extractor().tabs, ChannelTabs.VIDEOS, ChannelTabs.PLAYLISTS)
        }

        @Test
        override fun testTags() {
            assertTrue(extractor().tags.isEmpty())
        }
    }
}

package org.schabi.newpipe.extractor.services.peertube

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.InitNewPipeTest
import org.schabi.newpipe.extractor.ServiceList.PeerTube
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory
import org.schabi.newpipe.extractor.services.peertube.linkHandler.PeertubeTrendingLinkHandlerFactory

/**
 * Test for [PeertubeTrendingLinkHandlerFactory]
 */
class PeertubeTrendingLinkHandlerFactoryTest {

    @Test
    fun getUrl() {
        assertEquals(
            "https://peertube.mastodon.host/api/v1/videos?sort=-trending",
            linkHandlerFactory.fromId("Trending").url
        )
        assertEquals(
            "https://peertube.mastodon.host/api/v1/videos?sort=-likes",
            linkHandlerFactory.fromId("Most liked").url
        )
        assertEquals(
            "https://peertube.mastodon.host/api/v1/videos?sort=-publishedAt",
            linkHandlerFactory.fromId("Recently added").url
        )
        assertEquals(
            "https://peertube.mastodon.host/api/v1/videos?sort=-publishedAt&isLocal=true",
            linkHandlerFactory.fromId("Local").url
        )
    }

    @Test
    fun getId() {
        assertEquals("Trending", linkHandlerFactory.fromUrl("https://peertube.mastodon.host/videos/trending").id)
        assertEquals("Most liked", linkHandlerFactory.fromUrl("https://peertube.mastodon.host/videos/most-liked").id)
        assertEquals(
            "Recently added",
            linkHandlerFactory.fromUrl("https://peertube.mastodon.host/videos/recently-added").id
        )
        assertEquals("Local", linkHandlerFactory.fromUrl("https://peertube.mastodon.host/videos/local").id)
    }

    @Test
    fun acceptUrl() {
        assertTrue(linkHandlerFactory.acceptUrl("https://peertube.mastodon.host/videos/trending"))
        assertTrue(linkHandlerFactory.acceptUrl("https://peertube.mastodon.host/videos/trending?adsf=fjaj#fhe"))
        assertTrue(linkHandlerFactory.acceptUrl("https://peertube.mastodon.host/videos/most-liked"))
        assertTrue(linkHandlerFactory.acceptUrl("https://peertube.mastodon.host/videos/most-liked?adsf=fjaj#fhe"))
        assertTrue(linkHandlerFactory.acceptUrl("https://peertube.mastodon.host/videos/recently-added"))
        assertTrue(linkHandlerFactory.acceptUrl("https://peertube.mastodon.host/videos/recently-added?adsf=fjaj#fhe"))
        assertTrue(linkHandlerFactory.acceptUrl("https://peertube.mastodon.host/videos/local"))
        assertTrue(linkHandlerFactory.acceptUrl("https://peertube.mastodon.host/videos/local?adsf=fjaj#fhe"))

        PeertubeLinkHandlerFactoryTestHelper.assertDoNotAcceptNonURLs(linkHandlerFactory)
    }

    companion object {
        private lateinit var linkHandlerFactory: LinkHandlerFactory

        @JvmStatic
        @BeforeAll
        fun setUp() {
            // setting instance might break test when running in parallel
            InitNewPipeTest.initEmpty()
            PeerTube.instance = PeertubeInstance("https://peertube.mastodon.host", "PeerTube on Mastodon.host")
            linkHandlerFactory = PeertubeTrendingLinkHandlerFactory.getInstance()
        }
    }
}

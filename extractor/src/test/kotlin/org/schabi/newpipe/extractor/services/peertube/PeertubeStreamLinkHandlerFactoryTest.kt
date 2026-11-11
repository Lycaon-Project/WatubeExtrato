package org.schabi.newpipe.extractor.services.peertube

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.InitNewPipeTest
import org.schabi.newpipe.extractor.ServiceList.PeerTube
import org.schabi.newpipe.extractor.services.peertube.linkHandler.PeertubeStreamLinkHandlerFactory

/**
 * Test for [PeertubeStreamLinkHandlerFactory]
 */
class PeertubeStreamLinkHandlerFactoryTest {

    @Test
    fun getId() {
        assertEquals(
            "986aac60-1263-4f73-9ce5-36b18225cb60",
            linkHandler.fromUrl("https://peertube.mastodon.host/videos/watch/986aac60-1263-4f73-9ce5-36b18225cb60").id
        )
        assertEquals(
            "986aac60-1263-4f73-9ce5-36b18225cb60",
            linkHandler.fromUrl(
                "https://peertube.mastodon.host/videos/watch/986aac60-1263-4f73-9ce5-36b18225cb60?fsdafs=fsafa"
            ).id
        )
        assertEquals(
            "986aac60-1263-4f73-9ce5-36b18225cb60",
            linkHandler.fromUrl(
                "https://peertube.mastodon.host/api/v1/videos/watch/986aac60-1263-4f73-9ce5-36b18225cb60"
            ).id
        )
        assertEquals(
            "986aac60-1263-4f73-9ce5-36b18225cb60",
            linkHandler.fromUrl(
                "https://peertube.mastodon.host/api/v1/videos/watch/986aac60-1263-4f73-9ce5-36b18225cb60?fsdafs=fsafa"
            ).id
        )
        assertEquals(
            "9c9de5e8-0a1e-484a-b099-e80766180a6d",
            linkHandler.fromUrl("https://framatube.org/videos/watch/9c9de5e8-0a1e-484a-b099-e80766180a6d").id
        )
        assertEquals(
            "9c9de5e8-0a1e-484a-b099-e80766180a6d",
            linkHandler.fromUrl("https://framatube.org/videos/embed/9c9de5e8-0a1e-484a-b099-e80766180a6d").id
        )
        assertEquals(
            "9c9de5e8-0a1e-484a-b099-e80766180a6d",
            linkHandler.fromUrl("https://framatube.org/w/9c9de5e8-0a1e-484a-b099-e80766180a6d").id
        )
    }

    @Test
    fun getUrl() {
        assertEquals(
            "https://framatube.org/videos/watch/9c9de5e8-0a1e-484a-b099-e80766180a6d",
            linkHandler.fromId("9c9de5e8-0a1e-484a-b099-e80766180a6d").url
        )
        assertEquals(
            "https://framatube.org/videos/watch/9c9de5e8-0a1e-484a-b099-e80766180a6d",
            linkHandler.fromUrl("https://framatube.org/api/v1/videos/9c9de5e8-0a1e-484a-b099-e80766180a6d").url
        )
        assertEquals(
            "https://framatube.org/videos/watch/9c9de5e8-0a1e-484a-b099-e80766180a6d",
            linkHandler.fromUrl("https://framatube.org/videos/embed/9c9de5e8-0a1e-484a-b099-e80766180a6d").url
        )
        assertEquals(
            "https://framatube.org/videos/watch/9c9de5e8-0a1e-484a-b099-e80766180a6d",
            linkHandler.fromUrl("https://framatube.org/videos/watch/9c9de5e8-0a1e-484a-b099-e80766180a6d").url
        )
        assertEquals(
            "https://framatube.org/videos/watch/9c9de5e8-0a1e-484a-b099-e80766180a6d",
            linkHandler.fromUrl("https://framatube.org/w/9c9de5e8-0a1e-484a-b099-e80766180a6d").url
        )
    }

    @Test
    fun testAcceptUrl() {
        assertTrue(
            linkHandler.acceptUrl("https://peertube.mastodon.host/videos/watch/986aac60-1263-4f73-9ce5-36b18225cb60")
        )
        assertTrue(
            linkHandler.acceptUrl(
                "https://peertube.mastodon.host/videos/watch/986aac60-1263-4f73-9ce5-36b18225cb60?fsdafs=fsafa"
            )
        )
        assertTrue(linkHandler.acceptUrl("https://framatube.org/api/v1/videos/9c9de5e8-0a1e-484a-b099-e80766180a6d"))
        assertTrue(linkHandler.acceptUrl("https://framatube.org/videos/embed/9c9de5e8-0a1e-484a-b099-e80766180a6d"))
        assertTrue(linkHandler.acceptUrl("https://framatube.org/videos/watch/9c9de5e8-0a1e-484a-b099-e80766180a6d"))
        assertTrue(linkHandler.acceptUrl("https://framatube.org/w/9c9de5e8-0a1e-484a-b099-e80766180a6d"))

        // make sure playlists aren't accepted
        assertFalse(linkHandler.acceptUrl("https://framatube.org/w/p/dacdc4ef-5160-4846-9b70-a655880da667"))
        assertFalse(
            linkHandler.acceptUrl("https://framatube.org/videos/watch/playlist/dacdc4ef-5160-4846-9b70-a655880da667")
        )

        PeertubeLinkHandlerFactoryTestHelper.assertDoNotAcceptNonURLs(linkHandler)
    }

    companion object {
        private lateinit var linkHandler: PeertubeStreamLinkHandlerFactory

        @JvmStatic
        @BeforeAll
        fun setUp() {
            InitNewPipeTest.initEmpty()
            PeerTube.instance = PeertubeInstance("https://framatube.org", "Framatube")
            linkHandler = PeertubeStreamLinkHandlerFactory.getInstance()
        }
    }
}

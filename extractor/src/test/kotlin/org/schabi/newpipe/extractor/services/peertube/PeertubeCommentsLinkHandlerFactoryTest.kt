package org.schabi.newpipe.extractor.services.peertube

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.InitNewPipeTest
import org.schabi.newpipe.extractor.services.peertube.PeertubeLinkHandlerFactoryTestHelper.assertDoNotAcceptNonURLs
import org.schabi.newpipe.extractor.services.peertube.linkHandler.PeertubeCommentsLinkHandlerFactory

/**
 * Test for [PeertubeCommentsLinkHandlerFactory]
 */
class PeertubeCommentsLinkHandlerFactoryTest {

    @Test
    fun acceptUrlTest() {
        assertTrue(linkHandler.acceptUrl("https://framatube.org/videos/watch/kkGMgK9ZtnKfYAgnEtQxbv"))
        assertTrue(linkHandler.acceptUrl("https://framatube.org/w/kkGMgK9ZtnKfYAgnEtQxbv"))
        assertTrue(
            linkHandler.acceptUrl(
                "https://framatube.org/api/v1/videos/kkGMgK9ZtnKfYAgnEtQxbv/comment-threads" +
                    "?start=0&count=10&sort=-createdAt"
            )
        )
        assertTrue(linkHandler.acceptUrl("https://framatube.org/videos/watch/9c9de5e8-0a1e-484a-b099-e80766180a6d"))
        assertTrue(linkHandler.acceptUrl("https://framatube.org/w/9c9de5e8-0a1e-484a-b099-e80766180a6d"))
        assertTrue(
            linkHandler.acceptUrl(
                "https://framatube.org/api/v1/videos/9c9de5e8-0a1e-484a-b099-e80766180a6d/comment-threads" +
                    "?start=0&count=10&sort=-createdAt"
            )
        )
        assertDoNotAcceptNonURLs(linkHandler)
    }

    @Test
    fun getIdFromUrl() {
        assertEquals(
            "kkGMgK9ZtnKfYAgnEtQxbv",
            linkHandler.fromUrl("https://framatube.org/w/kkGMgK9ZtnKfYAgnEtQxbv").id
        )
        assertEquals(
            "kkGMgK9ZtnKfYAgnEtQxbv",
            linkHandler.fromUrl("https://framatube.org/videos/watch/kkGMgK9ZtnKfYAgnEtQxbv").id
        )
        assertEquals(
            "kkGMgK9ZtnKfYAgnEtQxbv",
            linkHandler.fromUrl("https://framatube.org/api/v1/videos/kkGMgK9ZtnKfYAgnEtQxbv/comment-threads").id
        )
        assertEquals(
            "kkGMgK9ZtnKfYAgnEtQxbv",
            linkHandler.fromUrl(
                "https://framatube.org/api/v1/videos/kkGMgK9ZtnKfYAgnEtQxbv/comment-threads" +
                    "?start=0&count=10&sort=-createdAt"
            ).id
        )
        assertEquals(
            "9c9de5e8-0a1e-484a-b099-e80766180a6d",
            linkHandler.fromUrl("https://framatube.org/w/9c9de5e8-0a1e-484a-b099-e80766180a6d").id
        )
        assertEquals(
            "9c9de5e8-0a1e-484a-b099-e80766180a6d",
            linkHandler.fromUrl("https://framatube.org/videos/watch/9c9de5e8-0a1e-484a-b099-e80766180a6d").id
        )
        assertEquals(
            "9c9de5e8-0a1e-484a-b099-e80766180a6d",
            linkHandler.fromUrl(
                "https://framatube.org/api/v1/videos/9c9de5e8-0a1e-484a-b099-e80766180a6d/comment-threads"
            ).id
        )
        assertEquals(
            "9c9de5e8-0a1e-484a-b099-e80766180a6d",
            linkHandler.fromUrl(
                "https://framatube.org/api/v1/videos/9c9de5e8-0a1e-484a-b099-e80766180a6d/comment-threads" +
                    "?start=0&count=10&sort=-createdAt"
            ).id
        )
    }

    companion object {
        private lateinit var linkHandler: PeertubeCommentsLinkHandlerFactory

        @JvmStatic
        @BeforeAll
        fun setUp() {
            InitNewPipeTest.initEmpty()
            linkHandler = PeertubeCommentsLinkHandlerFactory.getInstance()
        }
    }
}

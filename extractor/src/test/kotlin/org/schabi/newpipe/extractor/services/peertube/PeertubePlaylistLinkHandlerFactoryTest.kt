package org.schabi.newpipe.extractor.services.peertube

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.schabi.newpipe.extractor.InitNewPipeTest
import org.schabi.newpipe.extractor.services.peertube.PeertubeLinkHandlerFactoryTestHelper.assertDoNotAcceptNonURLs
import org.schabi.newpipe.extractor.services.peertube.linkHandler.PeertubePlaylistLinkHandlerFactory

/**
 * Test for [PeertubePlaylistLinkHandlerFactory]
 */
class PeertubePlaylistLinkHandlerFactoryTest {

    @Test
    fun acceptUrlTest() {
        assertTrue(linkHandler.acceptUrl("https://framatube.org/videos/watch/playlist/d8ca79f9-e4c7-4269-8183-d78ed269c909"))
        assertTrue(linkHandler.acceptUrl("https://framatube.org/w/p/d8ca79f9-e4c7-4269-8183-d78ed269c909"))
        assertTrue(linkHandler.acceptUrl("https://framatube.org/videos/watch/playlist/d8ca79f9-e4c7-4269-8183-d78ed269c909/videos"))
        assertTrue(linkHandler.acceptUrl("https://framatube.org/videos/watch/playlist/dacdc4ef-5160-4846-9b70-a655880da667"))
        assertTrue(linkHandler.acceptUrl("https://framatube.org/w/p/dacdc4ef-5160-4846-9b70-a655880da667"))
        assertTrue(linkHandler.acceptUrl("https://framatube.org/videos/watch/playlist/96b0ee2b-a5a7-4794-8769-58d8ccb79ab7"))
        assertTrue(linkHandler.acceptUrl("https://framatube.org/w/p/96b0ee2b-a5a7-4794-8769-58d8ccb79ab7"))
        assertDoNotAcceptNonURLs(linkHandler)
    }

    @Test
    fun getIdFromUrl() {
        assertEquals("d8ca79f9-e4c7-4269-8183-d78ed269c909", linkHandler.getId("https://framatube.org/videos/watch/playlist/d8ca79f9-e4c7-4269-8183-d78ed269c909"))
        assertEquals("d8ca79f9-e4c7-4269-8183-d78ed269c909", linkHandler.getId("https://framatube.org/w/p/d8ca79f9-e4c7-4269-8183-d78ed269c909"))
        assertEquals("dacdc4ef-5160-4846-9b70-a655880da667", linkHandler.getId("https://framatube.org/videos/watch/playlist/dacdc4ef-5160-4846-9b70-a655880da667"))
        assertEquals("dacdc4ef-5160-4846-9b70-a655880da667", linkHandler.getId("https://framatube.org/w/p/dacdc4ef-5160-4846-9b70-a655880da667"))
        assertEquals("bfc145f5-1be7-48a6-9b9e-4f1967199dad", linkHandler.getId("https://framatube.org/videos/watch/playlist/bfc145f5-1be7-48a6-9b9e-4f1967199dad"))
        assertEquals("bfc145f5-1be7-48a6-9b9e-4f1967199dad", linkHandler.getId("https://framatube.org/w/p/bfc145f5-1be7-48a6-9b9e-4f1967199dad"))
        assertEquals("96b0ee2b-a5a7-4794-8769-58d8ccb79ab7", linkHandler.getId("https://framatube.org/videos/watch/playlist/96b0ee2b-a5a7-4794-8769-58d8ccb79ab7"))
        assertEquals("96b0ee2b-a5a7-4794-8769-58d8ccb79ab7", linkHandler.getId("https://framatube.org/w/p/96b0ee2b-a5a7-4794-8769-58d8ccb79ab7"))
    }

    @Test
    fun getUrl() {
        assertDoesNotThrow(Executable { linkHandler.fromUrl("https://framatube.org/videos/watch/playlist/d8ca79f9-e4c7-4269-8183-d78ed269c909").url })
        assertDoesNotThrow(Executable { linkHandler.fromUrl("https://framatube.org/w/p/d8ca79f9-e4c7-4269-8183-d78ed269c909").url })
        assertDoesNotThrow(Executable { linkHandler.fromUrl("https://framatube.org/w/p/sLFbqXsw7sPR3AfvqQSBZB").url })
    }

    companion object {
        private lateinit var linkHandler: PeertubePlaylistLinkHandlerFactory

        @JvmStatic
        @BeforeAll
        fun setUp() {
            InitNewPipeTest.initEmpty()
            linkHandler = PeertubePlaylistLinkHandlerFactory.getInstance()
        }
    }
}

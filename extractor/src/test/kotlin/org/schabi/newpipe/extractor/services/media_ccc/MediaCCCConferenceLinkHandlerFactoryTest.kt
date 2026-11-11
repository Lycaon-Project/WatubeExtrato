package org.schabi.newpipe.extractor.services.media_ccc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.InitNewPipeTest
import org.schabi.newpipe.extractor.services.media_ccc.linkHandler.MediaCCCConferenceLinkHandlerFactory

class MediaCCCConferenceLinkHandlerFactoryTest {

    @Test
    fun getId() {
        assertEquals("jh20", linkHandler.fromUrl("https://media.ccc.de/c/jh20#278").id)
        assertEquals("jh20", linkHandler.fromUrl("https://media.ccc.de/b/jh20?a=b").id)
        assertEquals("jh20", linkHandler.fromUrl("https://api.media.ccc.de/public/conferences/jh20&a=b&b=c").id)
    }

    @Test
    fun getUrl() {
        assertEquals("https://media.ccc.de/c/jh20", linkHandler.fromUrl("https://media.ccc.de/c/jh20#278").url)
        assertEquals("https://media.ccc.de/c/jh20", linkHandler.fromUrl("https://media.ccc.de/b/jh20?a=b").url)
        assertEquals(
            "https://media.ccc.de/c/jh20",
            linkHandler.fromUrl("https://api.media.ccc.de/public/conferences/jh20&a=b&b=c").url
        )
        assertEquals("https://media.ccc.de/c/jh20", linkHandler.fromId("jh20").url)
    }

    companion object {
        private lateinit var linkHandler: MediaCCCConferenceLinkHandlerFactory

        @JvmStatic
        @BeforeAll
        fun setUp() {
            InitNewPipeTest.initEmpty()
            linkHandler = MediaCCCConferenceLinkHandlerFactory.getInstance()
        }
    }
}

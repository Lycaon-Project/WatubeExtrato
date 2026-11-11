package org.schabi.newpipe.extractor.services.media_ccc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.InitNewPipeTest
import org.schabi.newpipe.extractor.services.media_ccc.linkHandler.MediaCCCStreamLinkHandlerFactory

class MediaCCCStreamLinkHandlerFactoryTest {

    @Test
    fun getId() {
        assertEquals(
            "jhremote20-3001-abschlusspraesentation_jugend_hackt_remote_2020",
            linkHandler.fromUrl(
                "https://media.ccc.de/v/jhremote20-3001-abschlusspraesentation_jugend_hackt_remote_2020"
            ).id
        )
        assertEquals(
            "jhremote20-3001-abschlusspraesentation_jugend_hackt_remote_2020",
            linkHandler.fromUrl(
                "https://media.ccc.de/v/jhremote20-3001-abschlusspraesentation_jugend_hackt_remote_2020?a=b"
            ).id
        )
        assertEquals(
            "jhremote20-3001-abschlusspraesentation_jugend_hackt_remote_2020",
            linkHandler.fromUrl(
                "https://media.ccc.de/v/jhremote20-3001-abschlusspraesentation_jugend_hackt_remote_2020#3"
            ).id
        )
        assertEquals(
            "jhremote20-3001-abschlusspraesentation_jugend_hackt_remote_2020",
            linkHandler.fromUrl(
                "https://api.media.ccc.de/public/events/jhremote20-3001-abschlusspraesentation_jugend_hackt_remote_2020&a=b"
            ).id
        )
    }

    @Test
    fun getUrl() {
        assertEquals(
            "https://media.ccc.de/v/jhremote20-3001-abschlusspraesentation_jugend_hackt_remote_2020",
            linkHandler.fromUrl(
                "https://media.ccc.de/v/jhremote20-3001-abschlusspraesentation_jugend_hackt_remote_2020"
            ).url
        )
        assertEquals(
            "https://media.ccc.de/v/jhremote20-3001-abschlusspraesentation_jugend_hackt_remote_2020",
            linkHandler.fromUrl(
                "https://api.media.ccc.de/public/events/jhremote20-3001-abschlusspraesentation_jugend_hackt_remote_2020?b=a&a=b"
            ).url
        )
        assertEquals(
            "https://media.ccc.de/v/jhremote20-3001-abschlusspraesentation_jugend_hackt_remote_2020",
            linkHandler.fromId("jhremote20-3001-abschlusspraesentation_jugend_hackt_remote_2020").url
        )
    }

    companion object {
        private lateinit var linkHandler: MediaCCCStreamLinkHandlerFactory

        @JvmStatic
        @BeforeAll
        fun setUp() {
            InitNewPipeTest.initEmpty()
            linkHandler = MediaCCCStreamLinkHandlerFactory.getInstance()
        }
    }
}

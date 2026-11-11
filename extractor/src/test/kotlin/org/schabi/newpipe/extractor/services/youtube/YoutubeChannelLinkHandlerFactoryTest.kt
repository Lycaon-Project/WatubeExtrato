package org.schabi.newpipe.extractor.services.youtube

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.InitNewPipeTest
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeChannelLinkHandlerFactory

/**
 * Test for [YoutubeChannelLinkHandlerFactory]
 */
class YoutubeChannelLinkHandlerFactoryTest {

    @Test
    fun acceptUrlTest() {
        assertTrue(linkHandler.acceptUrl("https://www.youtube.com/user/Gronkh"))
        assertTrue(linkHandler.acceptUrl("https://www.youtube.com/user/Netzkino/videos"))

        assertTrue(linkHandler.acceptUrl("https://www.youtube.com/c/creatoracademy"))
        assertTrue(linkHandler.acceptUrl("https://www.youtube.com/c/%EB%85%B8%EB%A7%88%EB%93%9C%EC%BD%94%EB%8D%94NomadCoders"))

        assertTrue(linkHandler.acceptUrl("https://youtube.com/DIMENSI0N"))

        assertTrue(linkHandler.acceptUrl("https://www.youtube.com/channel/UClq42foiSgl7sSpLupnugGA"))
        assertTrue(linkHandler.acceptUrl("https://www.youtube.com/channel/UClq42foiSgl7sSpLupnugGA/videos?disable_polymer=1"))

        assertTrue(linkHandler.acceptUrl("https://redirect.invidious.io/user/Gronkh"))
        assertTrue(linkHandler.acceptUrl("https://invidious.nerdvpn.de/user/Netzkino/videos"))

        assertTrue(linkHandler.acceptUrl("https://redirect.invidious.io/channel/UClq42foiSgl7sSpLupnugGA"))
        assertTrue(linkHandler.acceptUrl("https://invidious.nerdvpn.de/channel/UClq42foiSgl7sSpLupnugGA/videos?disable_polymer=1"))
        assertTrue(linkHandler.acceptUrl("https://www.youtube.com/watchismo"))

        assertTrue(linkHandler.acceptUrl("https://www.youtube.com/@YouTube"))

        // do not accept URLs which are not channels
        assertFalse(linkHandler.acceptUrl("https://www.youtube.com/watch?v=jZViOEv90dI&t=100"))
        assertFalse(linkHandler.acceptUrl("https://www.youtube.com/watch"))
        assertFalse(linkHandler.acceptUrl("http://www.youtube.com/watch_popup?v=uEJuoEs1UxY"))
        assertFalse(linkHandler.acceptUrl("http://www.youtube.com/watch_popup"))
        assertFalse(linkHandler.acceptUrl("http://www.youtube.com/attribution_link?a=JdfC0C9V6ZI&u=%2Fwatch%3Fv%3DEhxJLojIE_o%26feature%3Dshare"))
        assertFalse(linkHandler.acceptUrl("http://www.youtube.com/attribution_link"))
        assertFalse(linkHandler.acceptUrl("https://www.youtube.com/playlist?list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1d"))
        assertFalse(linkHandler.acceptUrl("https://www.youtube.com/playlist"))
        assertFalse(linkHandler.acceptUrl("https://www.youtube.com/embed/jZViOEv90dI"))
        assertFalse(linkHandler.acceptUrl("https://www.youtube.com/embed"))
        assertFalse(linkHandler.acceptUrl("https://www.youtube.com/feed/subscriptions?list=PLz8YL4HVC87WJQDzVoY943URKQCsHS9XV"))
        assertFalse(linkHandler.acceptUrl("https://www.youtube.com/feed"))
        assertFalse(linkHandler.acceptUrl("https://www.youtube.com"))
        assertFalse(linkHandler.acceptUrl("https://m.youtube.com/select_site"))
    }

    @Test
    fun getIdFromUrl() {
        assertEquals("user/Gronkh", linkHandler.fromUrl("https://www.youtube.com/user/Gronkh").id)
        assertEquals("user/Netzkino", linkHandler.fromUrl("https://www.youtube.com/user/Netzkino/videos").id)

        assertEquals("channel/UClq42foiSgl7sSpLupnugGA", linkHandler.fromUrl("https://www.youtube.com/channel/UClq42foiSgl7sSpLupnugGA").id)
        assertEquals("channel/UClq42foiSgl7sSpLupnugGA", linkHandler.fromUrl("https://www.youtube.com/channel/UClq42foiSgl7sSpLupnugGA/videos?disable_polymer=1").id)

        assertEquals("user/Gronkh", linkHandler.fromUrl("https://redirect.invidious.io/user/Gronkh").id)
        assertEquals("user/Netzkino", linkHandler.fromUrl("https://invidious.nerdvpn.de/user/Netzkino/videos").id)

        assertEquals("channel/UClq42foiSgl7sSpLupnugGA", linkHandler.fromUrl("https://redirect.invidious.io/channel/UClq42foiSgl7sSpLupnugGA").id)
        assertEquals("channel/UClq42foiSgl7sSpLupnugGA", linkHandler.fromUrl("https://invidious.nerdvpn.de/channel/UClq42foiSgl7sSpLupnugGA/videos?disable_polymer=1").id)

        assertEquals("c/creatoracademy", linkHandler.fromUrl("https://www.youtube.com/c/creatoracademy").id)
        assertEquals("c/YouTubeCreators", linkHandler.fromUrl("https://www.youtube.com/c/YouTubeCreators").id)
        assertEquals("c/%EB%85%B8%EB%A7%88%EB%93%9C%EC%BD%94%EB%8D%94NomadCoders", linkHandler.fromUrl("https://www.youtube.com/c/%EB%85%B8%EB%A7%88%EB%93%9C%EC%BD%94%EB%8D%94NomadCoders").id)

        assertEquals("@Gronkh", linkHandler.fromUrl("https://www.youtube.com/@Gronkh?ucbcb=1").id)
        assertEquals("@YouTubeCreators", linkHandler.fromUrl("https://www.youtube.com/@YouTubeCreators/shorts").id)

        assertEquals("PewDiePie", linkHandler.fromUrl("https://www.youtube.com/PewDiePie").id)
        assertEquals("DreamTraps", linkHandler.fromUrl("https://www.youtube.com/DreamTraps").id)
    }

    companion object {
        private lateinit var linkHandler: YoutubeChannelLinkHandlerFactory

        @JvmStatic
        @BeforeAll
        fun setUp() {
            InitNewPipeTest.initEmpty()
            linkHandler = YoutubeChannelLinkHandlerFactory.getInstance()
        }
    }
}

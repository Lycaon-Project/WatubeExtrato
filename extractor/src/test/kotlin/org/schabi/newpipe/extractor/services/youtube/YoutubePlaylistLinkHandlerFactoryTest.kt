package org.schabi.newpipe.extractor.services.youtube

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.schabi.newpipe.extractor.InitNewPipeTest
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubePlaylistLinkHandlerFactory
import java.util.Optional

/**
 * Test for [YoutubePlaylistLinkHandlerFactory]
 */
class YoutubePlaylistLinkHandlerFactoryTest {

    @Test
    fun getIdWithNullAsUrl() {
        // a platform-typed null reaches the parameter check of fromId, which must reject it
        assertThrows(NullPointerException::class.java) { linkHandler.fromId(Optional.empty<String>().orElse(null)) }
    }

    @ParameterizedTest
    @CsvSource(
        "PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC,https://www.youtube.com/playlist?list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC",
        "PLz8YL4HVC87WJQDzVoY943URKQCsHS9XV,https://www.youtube.com/playlist?list=PLz8YL4HVC87WJQDzVoY943URKQCsHS9XV",
        "PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC,https://www.youtube.com/playlist?list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC&t=100",
        "PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC,https://WWW.youtube.com/playlist?list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC&t=100",
        "PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC,HTTPS://www.youtube.com/playlist?list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC&t=100",
        "PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC,https://www.youtube.com/watch?v=0JFM3PRZH-k&index=8&list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC",
        "PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC,http://www.youtube.com/watch?v=0JFM3PRZH-k&index=8&list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC",
        "PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC,https://m.youtube.com/playlist?list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC",
        "PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC,https://youtube.com/playlist?list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC",
        "PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC,www.youtube.com/playlist?list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC",
        "PLz8YL4HVC87WJQDzVoY943URKQCsHS9XV,www.youtube.com/playlist?list=PLz8YL4HVC87WJQDzVoY943URKQCsHS9XV",
        "OLAK5uy_lEBUW9iTwqf0IlYPxZ8LrzpgqjAHZgZpM,https://music.youtube.com/playlist?list=OLAK5uy_lEBUW9iTwqf0IlYPxZ8LrzpgqjAHZgZpM"
    )
    fun getIdfromYt(expectedId: String, url: String) {
        assertEquals(expectedId, linkHandler.fromUrl(url).id)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "https://www.youtube.com/playlist?list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC",
            "https://www.youtube.com/playlist?list=PLz8YL4HVC87WJQDzVoY943URKQCsHS9XV",
            "https://WWW.youtube.com/playlist?list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dCI",
            "HTTPS://www.youtube.com/playlist?list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC",
            "https://www.youtube.com/watch?v=0JFM3PRZH-k&index=8&list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC",
            "http://www.youtube.com/watch?v=0JFM3PRZH-k&index=8&list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC",
            "https://m.youtube.com/playlist?list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC",
            "https://youtube.com/playlist?list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC",
            "www.youtube.com/playlist?list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC",
            "www.youtube.com/playlist?list=PLz8YL4HVC87WJQDzVoY943URKQCsHS9XV",
            "https://music.youtube.com/playlist?list=OLAK5uy_lEBUW9iTwqf0IlYPxZ8LrzpgqjAHZgZpM",
            // YouTube Music playlist
            "https://www.youtube.com/playlist?list=RDCLAK5uy_ly6s4irLuZAcjEDwJmqcA_UtSipMyGgbQ",
            // YouTube Mix
            "https://www.youtube.com/watch?v=2kZVEUGLgy4&list=RDdoEcQv1wlsI&index=2, "
        ]
    )
    fun acceptYtUrl(url: String) {
        assertTrue(linkHandler.acceptUrl(url))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "https://www.youtube.com/feed/trending?list=PLz8YL4HVC87WJQDzVoY943URKQCsHS9XV",
            "https://www.youtube.com/feed/subscriptions?list=PLz8YL4HVC87WJQDzVoY943URKQCsHS9XV",
            "ftp://www.youtube.com/feed/trending?list=PLz8YL4HVC87WJQDzVoY943URKQCsHS9XV",
            "www.youtube.com:22/feed/trending?list=PLz8YL4HVC87WJQDzVoY943URKQCsHS9XV",
            "youtube  .    com/feed/trending?list=PLz8YL4HVC87WJQDzVoY943URKQCsHS9XV",
            "?list=PLz8YL4HVC87WJQDzVoY943URKQCsHS9XV"
        ]
    )
    fun deniesInvalidYtUrl(url: String) {
        assertFalse(linkHandler.acceptUrl(url))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "https://redirect.invidious.io/playlist?list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC",
            "https://redirect.invidious.io/playlist?list=PLz8YL4HVC87WJQDzVoY943URKQCsHS9XV",
            "https://invidious.nerdvpn.de/playlist?list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dCI",
            "https://YEWTU.BE/playlist?list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dCI",
            "HTTPS://yewtu.be/playlist?list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC",
            "https://yewtu.be/watch?v=0JFM3PRZH-k&index=8&list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC",
            "http://yewtu.be/watch?v=0JFM3PRZH-k&index=8&list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC",
            "https://yewtu.be/playlist?list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC",
            "yewtu.be/playlist?list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC",
            "yewtu.be/playlist?list=PLz8YL4HVC87WJQDzVoY943URKQCsHS9XV"
        ]
    )
    fun acceptInvidioUrl(url: String) {
        assertTrue(linkHandler.acceptUrl(url))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "https://redirect.invidious.io/feed/trending?list=PLz8YL4HVC87WJQDzVoY943URKQCsHS9XV",
            "https://invidious.nerdvpn.de/feed/subscriptions?list=PLz8YL4HVC87WJQDzVoY943URKQCsHS9XV",
            "ftp:/redirect.invidious.io/feed/trending?list=PLz8YL4HVC87WJQDzVoY943URKQCsHS9XV",
            "redirect.invidious.io:22/feed/trending?list=PLz8YL4HVC87WJQDzVoY943URKQCsHS9XV",
            "invidious  .    nerdvpn.de/feed/trending?list=PLz8YL4HVC87WJQDzVoY943URKQCsHS9XV",
            "?list=PLz8YL4HVC87WJQDzVoY943URKQCsHS9XV"
        ]
    )
    fun deniesInvalidInvidioUrl(url: String) {
        assertFalse(linkHandler.acceptUrl(url))
    }

    @ParameterizedTest
    @CsvSource(
        "PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC,https://redirect.invidious.io/playlist?list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC",
        "PLz8YL4HVC87WJQDzVoY943URKQCsHS9XV,https://invidious.nerdvpn.de/playlist?list=PLz8YL4HVC87WJQDzVoY943URKQCsHS9XV",
        "PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC,https://redirect.invidious.io/playlist?list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC&t=100",
        "PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC,https://invidious.nerdvpn.de/playlist?list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC&t=100",
        "PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC,HTTPS://invidious.nerdvpn.de/playlist?list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC&t=100",
        "PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC,https://invidious.nerdvpn.de/watch?v=0JFM3PRZH-k&index=8&list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC",
        "PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC,http://invidious.nerdvpn.de/watch?v=0JFM3PRZH-k&index=8&list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC",
        "PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC,https://invidious.nerdvpn.de/playlist?list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC",
        "PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC,invidious.nerdvpn.de/playlist?list=PLW5y1tjAOzI3orQNF1yGGVL5x-pR2K1dC",
        "PLz8YL4HVC87WJQDzVoY943URKQCsHS9XV,invidious.nerdvpn.de/playlist?list=PLz8YL4HVC87WJQDzVoY943URKQCsHS9XV"
    )
    fun getInvidioIdfromUrl(expectedId: String, url: String) {
        assertEquals(expectedId, linkHandler.fromUrl(url).id)
    }

    @Test
    fun fromUrlIsMixVideo() {
        val videoId = "_AzeUSL9lZc"
        var url = "https://www.youtube.com/watch?v=$videoId&list=RD$videoId"
        assertEquals(url, linkHandler.fromUrl(url).url)

        val mixVideoId = "qHtzO49SDmk"
        url = "https://www.youtube.com/watch?v=$mixVideoId&list=RD$videoId"
        assertEquals(url, linkHandler.fromUrl(url).url)
    }

    @Test
    fun fromUrlIsMixPlaylist() {
        val videoId = "_AzeUSL9lZc"
        val url = "https://www.youtube.com/watch?v=$videoId&list=RD$videoId"
        assertEquals(url, linkHandler.fromUrl("https://www.youtube.com/watch?list=RD$videoId").url)
    }

    companion object {
        private lateinit var linkHandler: YoutubePlaylistLinkHandlerFactory

        @JvmStatic
        @BeforeAll
        fun setUp() {
            InitNewPipeTest.initEmpty()
            linkHandler = YoutubePlaylistLinkHandlerFactory.getInstance()
        }
    }
}

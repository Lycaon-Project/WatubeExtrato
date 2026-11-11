package org.schabi.newpipe.extractor.services.peertube.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ServiceList.PeerTube
import org.schabi.newpipe.extractor.services.peertube.PeertubeInstance
import org.schabi.newpipe.extractor.services.peertube.linkHandler.PeertubeSearchQueryHandlerFactory

class PeertubeSearchQHTest {

    @Test
    fun testVideoSearch() {
        assertEquals(
            "https://peertube.mastodon.host/api/v1/search/videos?search=asdf",
            PeerTube.searchQHFactory.fromQuery("asdf").url
        )
        assertEquals(
            "https://peertube.mastodon.host/api/v1/search/videos?search=hans",
            PeerTube.searchQHFactory.fromQuery("hans").url
        )
        assertEquals(
            "https://peertube.mastodon.host/api/v1/search/videos?search=Poifj%26jaijf",
            PeerTube.searchQHFactory.fromQuery("Poifj&jaijf").url
        )
        assertEquals(
            "https://peertube.mastodon.host/api/v1/search/videos?search=G%C3%BCl%C3%BCm",
            PeerTube.searchQHFactory.fromQuery("Gülüm").url
        )
        assertEquals(
            "https://peertube.mastodon.host/api/v1/search/videos?search=%3Fj%24%29H%C2%A7B",
            PeerTube.searchQHFactory.fromQuery("?j\$)H§B").url
        )
    }

    @Test
    fun testSepiaVideoSearch() {
        assertEquals(
            "https://sepiasearch.org/api/v1/search/videos?search=%3Fj%24%29H%C2%A7B",
            PeerTube.searchQHFactory.fromQuery(
                "?j\$)H§B",
                listOf(PeertubeSearchQueryHandlerFactory.SEPIA_VIDEOS),
                ""
            ).url
        )
        assertEquals(
            "https://anotherpeertubeindex.com/api/v1/search/videos?search=%3Fj%24%29H%C2%A7B",
            PeerTube.searchQHFactory.fromQuery(
                "?j\$)H§B",
                listOf(PeertubeSearchQueryHandlerFactory.SEPIA_VIDEOS),
                "",
                "https://anotherpeertubeindex.com"
            ).url
        )
    }

    @Test
    fun testPlaylistSearch() {
        assertEquals(
            "https://peertube.mastodon.host/api/v1/search/video-playlists?search=asdf",
            PeerTube.searchQHFactory.fromQuery(
                "asdf",
                listOf(PeertubeSearchQueryHandlerFactory.PLAYLISTS),
                ""
            ).url
        )
        assertEquals(
            "https://peertube.mastodon.host/api/v1/search/video-playlists?search=hans",
            PeerTube.searchQHFactory.fromQuery(
                "hans",
                listOf(PeertubeSearchQueryHandlerFactory.PLAYLISTS),
                ""
            ).url
        )
    }

    @Test
    fun testChannelSearch() {
        assertEquals(
            "https://peertube.mastodon.host/api/v1/search/video-channels?search=asdf",
            PeerTube.searchQHFactory.fromQuery(
                "asdf",
                listOf(PeertubeSearchQueryHandlerFactory.CHANNELS),
                ""
            ).url
        )
        assertEquals(
            "https://peertube.mastodon.host/api/v1/search/video-channels?search=hans",
            PeerTube.searchQHFactory.fromQuery(
                "hans",
                listOf(PeertubeSearchQueryHandlerFactory.CHANNELS),
                ""
            ).url
        )
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun setUpClass() {
            // setting instance might break test when running in parallel
            PeerTube.instance = PeertubeInstance("https://peertube.mastodon.host", "PeerTube on Mastodon.host")
        }
    }
}

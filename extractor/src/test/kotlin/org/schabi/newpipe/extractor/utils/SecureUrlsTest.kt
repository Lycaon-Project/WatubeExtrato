package org.schabi.newpipe.extractor.utils

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.io.IOException

/**
 * Https for every exchange with YouTube and Google, and the events the app counts.
 */
internal class SecureUrlsTest {
    private val events = ArrayList<PrivacyEvents.Event>()

    @BeforeEach
    fun listen() {
        PrivacyEvents.setListener { event -> events.add(event) }
    }

    @AfterEach
    fun stopListening() {
        PrivacyEvents.setListener(null)
    }

    @Test
    fun googleHostsAndOnlyThem() {
        assertTrue(SecureUrls.isGoogleHost("www.youtube.com"))
        assertTrue(SecureUrls.isGoogleHost("YOUTU.BE"))
        assertTrue(SecureUrls.isGoogleHost("i.ytimg.com"))
        assertTrue(SecureUrls.isGoogleHost("yt3.ggpht.com"))
        assertFalse(SecureUrls.isGoogleHost("notyoutube.com"))
        assertFalse(SecureUrls.isGoogleHost("youtube.com.evil.example"))
        assertFalse(SecureUrls.isGoogleHost(null))
        // a trailing dot names the same host
        assertTrue(SecureUrls.isGoogleHost("www.youtube.com."))
    }

    @Test
    fun theOtherServicesAreHttpsOnlyToo() {
        assertTrue(SecureUrls.isHttpsOnlyHost("api-v2.soundcloud.com"))
        assertTrue(SecureUrls.isHttpsOnlyHost("f4.bcbits.com"))
        assertTrue(SecureUrls.isHttpsOnlyHost("media.ccc.de"))
        assertFalse(SecureUrls.isHttpsOnlyHost("framatube.org"))
        assertThrows(IOException::class.java) {
            SecureUrls.checkRequest("http://api-v2.soundcloud.com/tracks")
        }
        assertEquals(
            "https://artist.bandcamp.com/album/x",
            SecureUrls.upgradeToHttps("http://artist.bandcamp.com/album/x"),
        )
    }

    @Test
    fun ambiguousRequestsAreRefused() {
        // trailing dot: still YouTube, still refused in plain http
        assertThrows(IOException::class.java) {
            SecureUrls.checkRequest("http://www.youtube.com./watch")
        }
        // java.net.URL and the HTTP client do not read a backslash the same way
        assertThrows(IOException::class.java) {
            SecureUrls.checkRequest("http://www.youtube.com\\@evil.example/")
        }
        assertThrows(IOException::class.java) {
            SecureUrls.checkRequest("https://www.youtube.com\\@evil.example/")
        }
    }

    @Test
    fun aFailingListenerDoesNotBreakTheExtraction() {
        PrivacyEvents.setListener { _ -> throw IllegalStateException("storage unavailable") }
        assertEquals(
            "https://example.org/a",
            UrlPrivacy.removeTrackingParameters("https://example.org/a?utm_source=x"),
        )
    }

    @Test
    fun youtubeLinksAreUpgraded() {
        assertEquals(
            "https://www.youtube.com/watch?v=abc",
            SecureUrls.upgradeToHttps("http://www.youtube.com/watch?v=abc"),
        )
        assertEquals("https://youtu.be/abc", SecureUrls.upgradeToHttps("HTTP://youtu.be/abc"))
        assertEquals(
            listOf(PrivacyEvents.Event.HTTPS_UPGRADED, PrivacyEvents.Event.HTTPS_UPGRADED),
            events,
        )
    }

    @Test
    fun otherLinksAreLeftAlone() {
        // nothing tells that another site can be reached over https
        assertEquals(
            "http://example.org/page",
            SecureUrls.upgradeToHttps("http://example.org/page"),
        )
        assertEquals("https://www.youtube.com/", SecureUrls.upgradeToHttps("https://www.youtube.com/"))
        assertNull(SecureUrls.upgradeToHttps(null))
        assertTrue(events.isEmpty())
    }

    @Test
    fun plainHttpRequestsToGoogleAreRefused() {
        assertThrows(IOException::class.java) {
            SecureUrls.checkRequest("http://www.youtube.com/youtubei/v1/player")
        }
        assertThrows(IOException::class.java) { SecureUrls.checkRequest("not an url") }
        assertDoesNotThrow(Executable { SecureUrls.checkRequest("https://www.youtube.com/youtubei/v1") })
        assertDoesNotThrow(Executable { SecureUrls.checkRequest("http://peertube.example/api/v1") })
        assertEquals(
            listOf(
                PrivacyEvents.Event.INSECURE_REQUEST_REFUSED,
                PrivacyEvents.Event.INSECURE_REQUEST_REFUSED,
            ),
            events,
        )
    }

    @Test
    fun theDownloaderNeverSendsThem() {
        val sent = ArrayList<String>()
        val downloader = object : Downloader() {
            override fun execute(request: Request): Response {
                sent.add(request.url())
                return Response(200, "OK", null, "", request.url())
            }
        }
        assertThrows(IOException::class.java) { downloader.get("http://www.youtube.com/watch") }
        assertThrows(IOException::class.java) {
            downloader.post("http://i.ytimg.com/x", null, ByteArray(0))
        }
        assertDoesNotThrow(Executable { downloader.get("https://www.youtube.com/watch") })
        assertEquals(listOf("https://www.youtube.com/watch"), sent)
    }

    @Test
    fun removedTrackersAreCounted() {
        assertEquals(
            "https://example.org/a?id=1",
            UrlPrivacy.removeTrackingParameters("https://example.org/a?utm_source=x&id=1&fbclid=y"),
        )
        assertEquals(
            listOf(PrivacyEvents.Event.TRACKER_REMOVED, PrivacyEvents.Event.TRACKER_REMOVED),
            events,
        )
    }
}

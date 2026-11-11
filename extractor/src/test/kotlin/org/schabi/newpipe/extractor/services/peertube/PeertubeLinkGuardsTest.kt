package org.schabi.newpipe.extractor.services.peertube

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ServiceList.PeerTube
import org.schabi.newpipe.extractor.exceptions.ParsingException

/**
 * An instance names other hosts (instances, storage hosts): only public names are reached, over
 * https, and only the instance the user chose may be on their own network.
 */
internal class PeertubeLinkGuardsTest {

    @AfterEach
    fun restoreTheDefaultInstance() {
        PeerTube.instance = PeertubeInstance.DEFAULT_INSTANCE
    }

    @Test
    fun `public names are public hosts`() {
        listOf("framatube.org", "peertube.example.com", "video.xn--80ak6aa92e.xn--p1ai", "framatube.org.")
            .forEach { assertTrue(PeertubeLinkGuards.isPublicHost(it), it) }
    }

    @Test
    fun `addresses and local names are not`() {
        listOf(
            null, "", "localhost", "intranet", "10.0.0.1", "192.168.1.1", "169.254.169.254",
            "2130706433", "0x7f.1", "127.1", "[::1]", "[fd00::1]", "nas.local", "router.lan",
            "box.home.arpa", "db.internal", "app.localhost", "a..org",
        ).forEach { assertFalse(PeertubeLinkGuards.isPublicHost(it), it.toString()) }
    }

    @Test
    fun `media of an instance are reached over https on public hosts only`() {
        assertEquals(
            "https://storage.example.com/videos/a.mp4",
            PeertubeLinkGuards.webUrlOrNull("http://storage.example.com/videos/a.mp4"),
        )
        assertEquals(
            "https://framatube.org/w/abc",
            PeertubeLinkGuards.webUrlOrNull("https://framatube.org/w/abc"),
        )
        listOf(
            "https://192.168.1.1/a.mp4",
            "http://169.254.169.254/latest/meta-data",
            "http://127.0.0.1:8080/w/abc",
            "https://nas.local/a.mp4",
            "javascript:alert(1)",
            "file:///sdcard/a.mp4",
        ).forEach { assertNull(PeertubeLinkGuards.webUrlOrNull(it), it) }
    }

    @Test
    fun `requests never go to a local host the user did not choose`() {
        assertThrows(ParsingException::class.java) { PeertubeLinkGuards.requireBaseUrl("https://10.0.0.5") }
        assertThrows(ParsingException::class.java) { PeertubeLinkGuards.requireBaseUrl("http://localhost:9000") }
        assertEquals("https://framatube.org", PeertubeLinkGuards.requireBaseUrl("http://framatube.org"))
    }

    @Test
    fun `the instance the user chose may be on their network`() {
        PeerTube.instance = PeertubeInstance("http://localhost:9000", "Local")

        assertEquals("http://localhost:9000", PeertubeLinkGuards.requireBaseUrl("http://localhost:9000"))
        assertEquals("http://localhost:9000/w/abc", PeertubeLinkGuards.webUrlOrNull("http://localhost:9000/w/abc"))
    }
}

/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.schabi.newpipe.extractor.InitNewPipeTest
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeChannelLinkHandlerFactory
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeLinkGuards
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubePlaylistLinkHandlerFactory
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeStreamLinkHandlerFactory
import org.schabi.newpipe.extractor.utils.Utils
import java.net.URI
import java.util.Locale

/** Untrusted links must be rejected cleanly: no crash, no injection, no leak of the link. */
class YoutubeLinkHardeningTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun setUp() {
            InitNewPipeTest.initEmpty()
        }
    }

    private val streams = YoutubeStreamLinkHandlerFactory.getInstance()
    private val playlists = YoutubePlaylistLinkHandlerFactory.getInstance()
    private val channels = YoutubeChannelLinkHandlerFactory.getInstance()

    @Test
    fun `query parameter without a value does not crash`() {
        val url = URI("https://www.youtube.com/watch?v").toURL()
        assertEquals("", Utils.getQueryValue(url, "v"))
        assertFalse(streams.acceptUrl("https://www.youtube.com/watch?v"))
    }

    @Test
    fun `malformed percent-encoding does not crash`() {
        // java.net.URI would refuse this link: parse it the way the link handlers do
        val url = Utils.stringToURL("https://www.youtube.com/watch?v=%E0%A4%A&list=PL1234567890")
        assertNull(Utils.getQueryValue(url, "v"))
        assertEquals("PL1234567890", Utils.getQueryValue(url, "list"))
        assertFalse(streams.acceptUrl("https://www.youtube.com/watch?v=%zz"))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            // dotless ı, which equalsIgnoreCase matches with i
            "https://www.youtube-nocookıe.com/embed/9Dpqou5cI08",
            "https://youtuʙe/9Dpqou5cI08",
        ],
    )
    fun `look-alike hosts are rejected`(url: String) {
        assertFalse(streams.acceptUrl(url))
    }

    @Test
    fun `turkish locale keeps accepting music and invidious links`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals(
                "O0EDx9WAelc",
                streams.getId("https://music.youtube.com/watch?v=O0EDx9WAelc"),
            )
            assertEquals(
                "TglNG-yjabU",
                streams.getId("https://invidious.nerdvpn.de/watch?v=TglNG-yjabU"),
            )
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun `attribution link target cannot point to another host`() {
        val target = "%40evil.example%2Fwatch%3Fv%3D9Dpqou5cI08"
        assertThrows(ParsingException::class.java) {
            streams.getId("https://www.youtube.com/attribution_link?u=$target")
        }
    }

    @Test
    fun `mix links from other hosts are rejected`() {
        assertThrows(ParsingException::class.java) {
            playlists.fromUrl("https://evil.example/watch?v=9Dpqou5cI08&list=RD9Dpqou5cI08")
        }
    }

    @Test
    fun `mix link cannot inject query parameters through v`() {
        val handler = playlists.fromUrl(
            "https://www.youtube.com/watch?v=9Dpqou5cI08%26pp%3Devil&list=RD9Dpqou5cI08",
        )
        assertEquals("https://www.youtube.com/watch?v=9Dpqou5cI08&list=RD9Dpqou5cI08", handler.url)
    }

    @Test
    fun `ids are validated before being placed in a URL`() {
        assertThrows(ParsingException::class.java) { streams.getUrl("9Dpqou5cI08&pp=evil") }
        assertThrows(ParsingException::class.java) { playlists.getUrl("PL123&pp=evil") }
        assertThrows(ParsingException::class.java) { channels.getUrl("channel/UC1?x=y") }
        assertThrows(ParsingException::class.java) { channels.getUrl("@handle/../watch") }
        assertThrows(ParsingException::class.java) { channels.getUrl("c/name%zz") }
    }

    @Test
    fun `channel paths given by YouTube still build a URL`() {
        assertEquals("https://www.youtube.com/@handle/videos", channels.getUrl("@handle/videos"))
        assertEquals(
            "https://www.youtube.com/c/%EB%85%B8NomadCoders",
            channels.getUrl("c/%EB%85%B8NomadCoders"),
        )
    }

    @Test
    fun `oversized links are rejected`() {
        val huge = "https://www.youtube.com/watch?v=9Dpqou5cI08&x=" + "a".repeat(10_000)
        assertFalse(streams.acceptUrl(huge))
        assertThrows(ParsingException::class.java) { streams.fromUrl(huge) }
        assertThrows(ParsingException::class.java) { playlists.fromUrl(huge) }
    }

    @Test
    fun `fromUrl errors do not quote the link`() {
        val secret = "personal-token-42"
        val notAccepted = assertThrows(ParsingException::class.java) {
            streams.fromUrl("https://www.youtube.com/feed/history?token=$secret")
        }
        assertFalse(notAccepted.message.orEmpty().contains(secret))
        val malformed = assertThrows(ParsingException::class.java) {
            channels.fromUrl("https://www.youtube.com:bad/@x?token=$secret")
        }
        assertFalse(generateSequence<Throwable>(malformed) { it.cause }
            .any { it.message.orEmpty().contains(secret) })
    }

    @Test
    fun `error messages do not quote the link`() {
        val secret = "personal-token-42"
        val error = assertThrows(ParsingException::class.java) {
            streams.getId("https://www.youtube.com/feed/history?token=$secret")
        }
        assertFalse(error.message.orEmpty().contains(secret))
    }

    /** java.net.URL keeps `\` in the host, OkHttp and browsers read it as `/`. */
    @ParameterizedTest
    @ValueSource(
        strings = [
            "https://evil.example\\.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://evil.example\\@www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ\u0000",
            "https://www.you%74ube.com/watch?v=dQw4w9WgXcQ",
            "https:///watch?v=dQw4w9WgXcQ",
        ],
    )
    fun `hosts read differently by java and okhttp are refused`(url: String) {
        assertNull(YoutubeLinkGuards.parseUrlOrNull(url))
        assertFalse(streams.acceptUrl(url))
    }

    @Test
    fun `plain links still parse`() {
        assertEquals("www.youtube.com", YoutubeLinkGuards.parseUrlOrNull("https://www.youtube.com/watch?v=x")?.host)
        assertEquals("youtube.com.", YoutubeLinkGuards.parseUrlOrNull("https://youtube.com./watch?v=x")?.host)
    }
}

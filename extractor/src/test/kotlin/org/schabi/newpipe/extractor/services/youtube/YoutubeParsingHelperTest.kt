package org.schabi.newpipe.extractor.services.youtube

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.stream.AudioTrackType

class YoutubeParsingHelperTest : InitYoutubeTest {

    @Test
    fun testIsHardcodedClientVersionValid() {
        assertTrue(
            YoutubeParsingHelper.isHardcodedClientVersionValid(),
            "Hardcoded client version is not valid anymore"
        )
    }

    @Test
    fun testIsHardcodedYoutubeMusicClientVersionValid() {
        assertTrue(
            YoutubeParsingHelper.isHardcodedYoutubeMusicClientVersionValid(),
            "Hardcoded YouTube Music client version is not valid anymore"
        )
    }

    @Test
    fun testParseDurationString() {
        assertEquals(1162567, YoutubeParsingHelper.parseDurationString("12:34:56:07"))
        assertEquals(4445767, YoutubeParsingHelper.parseDurationString("1,234:56:07"))
        assertEquals(754, YoutubeParsingHelper.parseDurationString("12:34 "))
    }

    @Test
    fun testConvertFromGoogleCacheUrl() {
        assertEquals(
            "https://mohfw.gov.in/",
            YoutubeParsingHelper.extractCachedUrlIfNeeded(
                "https://webcache.googleusercontent.com/search?q=cache:https://mohfw.gov.in/"
            )
        )
        assertEquals(
            "https://www.infektionsschutz.de/coronavirus-sars-cov-2.html",
            YoutubeParsingHelper.extractCachedUrlIfNeeded("https://www.infektionsschutz.de/coronavirus-sars-cov-2.html")
        )
    }

    @Test
    fun extractAudioTrackType() {
        val originalXtags = "ChEKBWFjb250EghvcmlnaW5hbAoNCgRsYW5nEgVlbi1VUw"
        val dubbedXtags = "ChQKBWFjb250EgtkdWJiZWQtYXV0bwoKCgRsYW5nEgJhcg"
        val descriptiveXtags = "ChQKBWFjb250EgtkZXNjcmlwdGl2ZQ"
        val noTrackXtags: String? = null

        assertEquals(AudioTrackType.ORIGINAL, YoutubeParsingHelper.extractAudioTrackType(originalXtags))
        assertEquals(AudioTrackType.DUBBED, YoutubeParsingHelper.extractAudioTrackType(dubbedXtags))
        assertEquals(AudioTrackType.DESCRIPTIVE, YoutubeParsingHelper.extractAudioTrackType(descriptiveXtags))
        assertNull(YoutubeParsingHelper.extractAudioTrackType(noTrackXtags))
    }

    @Test
    fun testValidClientVersionOrNull() {
        assertEquals("2.20261019.01.00", YoutubeParsingHelper.validClientVersionOrNull("2.20261019.01.00"))
        // Served by sw.js during a canary rollout: must be kept, or every extraction falls back
        // to the HTML page and then to the hardcoded version.
        val canary = "2.20261020.01.00-canary_experiment_2.20261019.01.00"
        assertEquals(canary, YoutubeParsingHelper.validClientVersionOrNull(canary))
        assertNull(YoutubeParsingHelper.validClientVersionOrNull(null))
        assertNull(YoutubeParsingHelper.validClientVersionOrNull(""))
        assertNull(YoutubeParsingHelper.validClientVersionOrNull("2.2026\r\nX-Injected: 1"))
        assertNull(YoutubeParsingHelper.validClientVersionOrNull("2.20261019.01.00-Canary"))
        assertNull(YoutubeParsingHelper.validClientVersionOrNull("2.20261019.01.00-canary\"}"))
    }
}

package org.schabi.newpipe.extractor.services.soundcloud

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.InitNewPipeTest
import java.util.concurrent.TimeUnit

class SoundcloudParsingHelperTest : InitNewPipeTest {
    @Test
    fun `a refused client id is scraped again, unless it is brand new`() {
        val twoMinutes = TimeUnit.MINUTES.toNanos(2)
        assertTrue(SoundcloudParsingHelper.isClientIdRefusal(401, twoMinutes))
        assertTrue(SoundcloudParsingHelper.isClientIdRefusal(403, twoMinutes))
        // a track blocked in the country answers 403 too: a fresh ID is not scraped again
        assertFalse(SoundcloudParsingHelper.isClientIdRefusal(403, TimeUnit.SECONDS.toNanos(5)))
        assertFalse(SoundcloudParsingHelper.isClientIdRefusal(404, twoMinutes))
        assertFalse(SoundcloudParsingHelper.isClientIdRefusal(200, twoMinutes))
    }

    @Test
    fun resolveUrlWithEmbedPlayerTest() {
        assertEquals(
            "https://soundcloud.com/trapcity",
            SoundcloudParsingHelper.resolveUrlWithEmbedPlayer("https://api.soundcloud.com/users/26057743")
        )
        assertEquals(
            "https://soundcloud.com/nocopyrightsounds",
            SoundcloudParsingHelper.resolveUrlWithEmbedPlayer("https://api.soundcloud.com/users/16069159")
        )
        assertEquals(
            "https://soundcloud.com/trapcity",
            SoundcloudParsingHelper.resolveUrlWithEmbedPlayer("https://api-v2.soundcloud.com/users/26057743")
        )
        assertEquals(
            "https://soundcloud.com/nocopyrightsounds",
            SoundcloudParsingHelper.resolveUrlWithEmbedPlayer("https://api-v2.soundcloud.com/users/16069159")
        )
    }

    @Test
    fun resolveIdWithWidgetApiTest() {
        assertEquals("26057743", SoundcloudParsingHelper.resolveIdWithWidgetApi("https://soundcloud.com/trapcity"))
        assertEquals(
            "16069159",
            SoundcloudParsingHelper.resolveIdWithWidgetApi("https://soundcloud.com/nocopyrightsounds")
        )

        assertEquals(
            "26057743",
            SoundcloudParsingHelper.resolveIdWithWidgetApi("https://on.soundcloud.com/Rr2JyfFcYwbawpw49")
        )
        assertEquals(
            "1818813498",
            SoundcloudParsingHelper.resolveIdWithWidgetApi("https://on.soundcloud.com/a8QmYdMnmxnsSTEp9")
        )
        assertEquals("1468401502", SoundcloudParsingHelper.resolveIdWithWidgetApi("https://on.soundcloud.com/rdt7e"))
    }
}

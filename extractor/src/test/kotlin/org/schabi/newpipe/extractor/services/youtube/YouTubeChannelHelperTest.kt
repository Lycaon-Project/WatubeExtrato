package org.schabi.newpipe.extractor.services.youtube

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.schabi.newpipe.extractor.exceptions.ExtractionException

internal class YouTubeChannelHelperTest : InitYoutubeTest {

    @ParameterizedTest
    @ValueSource(
        strings = [
            "@TheDailyShow",
            "thedailyshow",
            "channel/UCwWhs_6x42TyRM4Wstoq8HA",
            "UCwWhs_6x42TyRM4Wstoq8HA"
        ]
    )
    fun resolveSuccessfulTheDailyShow(idOrPath: String) {
        val id = assertDoesNotThrow<String> { YoutubeChannelHelper.resolveChannelId(idOrPath) }
        assertEquals("UCwWhs_6x42TyRM4Wstoq8HA", id)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "@Gronkh",
            "gronkh",
            "channel/UCYJ61XIK64sp6ZFFS8sctxw",
            "UCYJ61XIK64sp6ZFFS8sctxw"
        ]
    )
    fun resolveSuccessfulGronkh(idOrPath: String) {
        val id = assertDoesNotThrow<String> { YoutubeChannelHelper.resolveChannelId(idOrPath) }
        assertEquals("UCYJ61XIK64sp6ZFFS8sctxw", id)
    }

    @Test
    fun resolveFailNonExistingTag() {
        assertThrows(ExtractionException::class.java) {
            YoutubeChannelHelper.resolveChannelId("@nonExistingHandleThatWillNeverExist15464")
        }
    }
}

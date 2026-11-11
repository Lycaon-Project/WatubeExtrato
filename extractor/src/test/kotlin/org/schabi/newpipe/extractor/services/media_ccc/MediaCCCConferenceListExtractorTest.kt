package org.schabi.newpipe.extractor.services.media_ccc

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.schabi.newpipe.extractor.ExtractorAsserts
import org.schabi.newpipe.extractor.ServiceList.MediaCCC
import org.schabi.newpipe.extractor.kiosk.KioskExtractor
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest
import org.schabi.newpipe.extractor.services.media_ccc.extractors.MediaCCCConferenceKiosk

/**
 * Test [MediaCCCConferenceKiosk]
 */
class MediaCCCConferenceListExtractorTest : DefaultSimpleExtractorTest<KioskExtractor<*>>() {

    override fun createExtractor(): KioskExtractor<*> =
        MediaCCC.kioskList.getExtractorById("conferences", null)

    @Test
    fun getConferencesListTest() {
        ExtractorAsserts.assertGreaterOrEqual(174, extractor().initialPage.items.size.toLong())
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "FrOSCon 2016",
            "ChaosWest @ 35c3",
            "CTreffOS chaOStalks",
            "Datenspuren 2015",
            "Chaos Singularity 2017",
            "SIGINT10",
            "Vintage Computing Festival Berlin 2015",
            "FIfFKon 2015",
            "33C3: trailers",
            "Blinkenlights"
        ]
    )
    fun conferenceTypeTest(name: String) {
        val itemList = extractor().initialPage.items
        assertTrue(itemList.any { name == it.name })
    }
}

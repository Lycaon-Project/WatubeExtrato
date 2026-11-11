package org.schabi.newpipe.extractor.services.media_ccc

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.schabi.newpipe.extractor.ServiceList.MediaCCC
import org.schabi.newpipe.extractor.kiosk.KioskExtractor
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest

class MediaCCCLiveStreamListExtractorTest : DefaultSimpleExtractorTest<KioskExtractor<*>>() {

    override fun createExtractor(): KioskExtractor<*> =
        MediaCCC.kioskList.getExtractorById("live", null)

    @Test
    fun getConferencesListTest() {
        assertDoesNotThrow(Executable { extractor().initialPage.items })
    }
}

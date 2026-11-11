package org.schabi.newpipe.extractor.services.media_ccc

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.schabi.newpipe.extractor.ExtractorAsserts.assertGreater
import org.schabi.newpipe.extractor.InitNewPipeTest
import org.schabi.newpipe.extractor.ServiceList.MediaCCC
import org.schabi.newpipe.extractor.kiosk.KioskExtractor
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.utils.Utils.isNullOrEmpty

class MediaCCCRecentListExtractorTest :
    DefaultSimpleExtractorTest<KioskExtractor<*>>(),
    InitNewPipeTest {

    override fun createExtractor(): KioskExtractor<*> =
        MediaCCC.kioskList.getExtractorById("recent", null)

    @Test
    fun testStreamList() {
        val items = extractor().initialPage.items.map { it as StreamInfoItem }
        assertFalse(items.isEmpty(), "No items returned")
        assertAll(items.flatMap { getAllConditionsForItem(it) })
    }

    private fun getAllConditionsForItem(item: StreamInfoItem): List<Executable> = listOf(
        Executable {
            assertFalse(
                isNullOrEmpty(item.name),
                "Name=[" + item.name + "] of " + item + " is empty or null"
            )
        },
        Executable {
            assertGreater(
                0,
                item.duration,
                "Duration[=" + item.duration + "] of " + item + " is <= 0"
            )
        }
    )
}

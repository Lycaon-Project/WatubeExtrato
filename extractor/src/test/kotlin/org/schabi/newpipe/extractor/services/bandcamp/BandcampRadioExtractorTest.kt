// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

package org.schabi.newpipe.extractor.services.bandcamp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ServiceList.Bandcamp
import org.schabi.newpipe.extractor.services.BaseListExtractorTest
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest
import org.schabi.newpipe.extractor.services.DefaultTests
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampRadioExtractor

/**
 * Tests for [BandcampRadioExtractor]
 */
class BandcampRadioExtractorTest :
    DefaultSimpleExtractorTest<BandcampRadioExtractor>(),
    BaseListExtractorTest {

    override fun createExtractor(): BandcampRadioExtractor =
        Bandcamp.kioskList.getExtractorById("Radio", null) as BandcampRadioExtractor

    @Test
    fun testRadioCount() {
        val list = extractor().initialPage.items
        assertTrue(list.size > 300)
    }

    @Disabled(
        "broken upstream as well (TeamNewPipe/NewPipeExtractor dev 6faf4068, 2026-10): " +
            "recorded radio shows come without a title"
    )
    @Test
    override fun testRelatedItems() {
        DefaultTests.defaultTestRelatedItems(extractor())
    }

    @Test
    override fun testMoreRelatedItems() {
        // All items are on one page
    }

    @Test
    override fun testServiceId() {
        assertEquals(Bandcamp.serviceId, extractor().serviceId)
    }

    @Test
    override fun testName() {
        assertEquals("Radio", extractor().name)
    }

    @Test
    override fun testId() {
        assertEquals("Radio", extractor().id)
    }

    @Test
    override fun testUrl() {
        assertEquals("https://bandcamp.com/api/bcweekly/3/list", extractor().url)
    }

    @Test
    override fun testOriginalUrl() {
        assertEquals("https://bandcamp.com/api/bcweekly/3/list", extractor().originalUrl)
    }
}

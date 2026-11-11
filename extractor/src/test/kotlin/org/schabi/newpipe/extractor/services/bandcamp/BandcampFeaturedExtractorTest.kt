// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

package org.schabi.newpipe.extractor.services.bandcamp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ServiceList.Bandcamp
import org.schabi.newpipe.extractor.services.BaseListExtractorTest
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest
import org.schabi.newpipe.extractor.services.DefaultTests
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampFeaturedExtractor

/**
 * Tests for [BandcampFeaturedExtractor]
 */
class BandcampFeaturedExtractorTest :
    DefaultSimpleExtractorTest<BandcampFeaturedExtractor>(),
    BaseListExtractorTest {

    override fun createExtractor(): BandcampFeaturedExtractor =
        Bandcamp.kioskList.getDefaultKioskExtractor() as BandcampFeaturedExtractor

    @Test
    fun testFeaturedCount() {
        val list = extractor().initialPage.items
        assertTrue(list.size > 5)
    }

    @Test
    fun testHttps() {
        val list = extractor().initialPage.items
        assertTrue(list[0].url.contains("https://"))
    }

    @Test
    fun testMorePages() {
        val page2 = extractor().initialPage.nextPage
        val page3 = extractor().getPage(page2).nextPage

        assertTrue(extractor().getPage(page2).items.size > 5)

        // Compare first item of second page with first item of third page
        assertNotEquals(
            extractor().getPage(page2).items[0],
            extractor().getPage(page3).items[0]
        )
    }

    override fun testRelatedItems() {
        DefaultTests.defaultTestRelatedItems(extractor())
    }

    override fun testMoreRelatedItems() {
        // more items not implemented
    }

    override fun testServiceId() {
        assertEquals(Bandcamp.serviceId, extractor().serviceId)
    }

    override fun testName() {
        assertEquals("Featured", extractor().name)
    }

    override fun testId() {
        assertEquals("", extractor().id)
    }

    override fun testUrl() {
        assertEquals("", extractor().url)
    }

    override fun testOriginalUrl() {
        assertEquals("", extractor().originalUrl)
    }
}

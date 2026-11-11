package org.schabi.newpipe.extractor.services.soundcloud

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ServiceList.SoundCloud
import org.schabi.newpipe.extractor.services.BaseListExtractorTest
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestMoreItems
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestRelatedItems
import org.schabi.newpipe.extractor.services.soundcloud.extractors.SoundcloudChartsExtractor

class SoundcloudChartsExtractorTest {
    class NewAndHot :
        DefaultSimpleExtractorTest<SoundcloudChartsExtractor>(),
        BaseListExtractorTest {

        override fun createExtractor(): SoundcloudChartsExtractor =
            SoundCloud.kioskList.getExtractorById("New & hot", null) as SoundcloudChartsExtractor

        @Test
        override fun testServiceId() {
            assertEquals(SoundCloud.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertEquals("New & hot", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("New & hot", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://soundcloud.com/charts/new", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals("https://soundcloud.com/charts/new", extractor().originalUrl)
        }

        @Test
        override fun testRelatedItems() {
            defaultTestRelatedItems(extractor())
        }

        @Test
        override fun testMoreRelatedItems() {
            defaultTestMoreItems(extractor())
        }
    }
}

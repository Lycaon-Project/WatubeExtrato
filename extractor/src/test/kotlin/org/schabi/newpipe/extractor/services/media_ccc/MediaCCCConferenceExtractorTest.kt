package org.schabi.newpipe.extractor.services.media_ccc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ExtractorAsserts.assertContainsImageUrlInImageCollection
import org.schabi.newpipe.extractor.ServiceList.MediaCCC
import org.schabi.newpipe.extractor.channel.ChannelExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest
import org.schabi.newpipe.extractor.services.media_ccc.extractors.MediaCCCConferenceExtractor

/**
 * Test [MediaCCCConferenceExtractor] and
 * [org.schabi.newpipe.extractor.services.media_ccc.extractors.MediaCCCChannelTabExtractor]
 */
class MediaCCCConferenceExtractorTest {

    abstract class Base : DefaultSimpleExtractorTest<ChannelExtractor>() {

        private var tabExtractor: ChannelTabExtractor? = null

        override fun fetchExtractor(extractor: ChannelExtractor) {
            super.fetchExtractor(extractor)
            val tab = MediaCCC.getChannelTabExtractor(extractor.tabs[0])
            tab.fetchPage()
            tabExtractor = tab
        }

        protected fun tabExtractor(): ChannelTabExtractor {
            if (tabExtractor == null) {
                extractor() // Initialize extractor to also init TabExtractor
            }
            return checkNotNull(tabExtractor)
        }
    }

    class FrOSCon2017 : Base() {

        override fun createExtractor(): ChannelExtractor =
            MediaCCC.getChannelExtractor("https://media.ccc.de/c/froscon2017")

        @Test
        fun testName() {
            assertEquals("FrOSCon 2017", extractor().name)
        }

        @Test
        fun testGetUrl() {
            assertEquals("https://media.ccc.de/c/froscon2017", extractor().url)
        }

        @Test
        fun testGetOriginalUrl() {
            assertEquals("https://media.ccc.de/c/froscon2017", extractor().originalUrl)
        }

        @Test
        fun testGetThumbnails() {
            assertContainsImageUrlInImageCollection(
                "https://static.media.ccc.de/media/events/froscon/2017/logo.png",
                extractor().avatars
            )
        }

        @Test
        fun testGetInitalPage() {
            assertEquals(97, tabExtractor().initialPage.items.size)
        }
    }

    class Oscal2019 : Base() {

        override fun createExtractor(): ChannelExtractor =
            MediaCCC.getChannelExtractor("https://media.ccc.de/c/oscal19")

        @Test
        fun testName() {
            assertEquals("Open Source Conference Albania 2019", extractor().name)
        }

        @Test
        fun testGetUrl() {
            assertEquals("https://media.ccc.de/c/oscal19", extractor().url)
        }

        @Test
        fun testGetOriginalUrl() {
            assertEquals("https://media.ccc.de/c/oscal19", extractor().originalUrl)
        }

        @Test
        fun testGetThumbnailUrl() {
            assertContainsImageUrlInImageCollection(
                "https://static.media.ccc.de/media/events/oscal/2019/oscal-19.png",
                extractor().avatars
            )
        }

        @Test
        fun testGetInitalPage() {
            assertTrue(tabExtractor().initialPage.items.size >= 21)
        }
    }
}

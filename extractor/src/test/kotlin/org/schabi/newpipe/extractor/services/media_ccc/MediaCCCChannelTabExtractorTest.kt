package org.schabi.newpipe.extractor.services.media_ccc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ServiceList.MediaCCC
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest

/**
 * Test that it is possible to create and use a channel tab extractor
 * ([org.schabi.newpipe.extractor.services.media_ccc.extractors.MediaCCCChannelTabExtractor]) without
 * passing through the conference extractor
 */
class MediaCCCChannelTabExtractorTest {

    class CCCamp2023 : DefaultSimpleExtractorTest<ChannelTabExtractor>() {

        override fun createExtractor(): ChannelTabExtractor =
            MediaCCC.getChannelTabExtractorFromId("camp2023", ChannelTabs.VIDEOS)

        @Test
        fun testName() {
            assertEquals(ChannelTabs.VIDEOS, extractor().name)
        }

        @Test
        fun testGetUrl() {
            assertEquals("https://media.ccc.de/c/camp2023", extractor().url)
        }

        @Test
        fun testGetOriginalUrl() {
            assertEquals("https://media.ccc.de/c/camp2023", extractor().originalUrl)
        }

        @Test
        fun testGetInitalPage() {
            assertEquals(177, extractor().initialPage.items.size)
        }
    }
}

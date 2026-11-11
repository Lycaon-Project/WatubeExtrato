// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

package org.schabi.newpipe.extractor.services.bandcamp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ExtractorAsserts.assertTabsContain
import org.schabi.newpipe.extractor.ServiceList.Bandcamp
import org.schabi.newpipe.extractor.channel.ChannelExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.services.BaseChannelExtractorTest
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest

class BandcampChannelExtractorTest :
    DefaultSimpleExtractorTest<ChannelExtractor>(),
    BaseChannelExtractorTest {

    override fun createExtractor(): ChannelExtractor =
        Bandcamp.getChannelExtractor("https://toupie.bandcamp.com/releases")

    @Test
    override fun testDescription() {
        assertEquals("making music:)", extractor().description)
    }

    @Test
    override fun testAvatars() {
        BandcampTestUtils.testImages(extractor().avatars)
    }

    @Test
    override fun testBanners() {
        BandcampTestUtils.testImages(extractor().banners)
    }

    @Test
    override fun testFeedUrl() {
        assertNull(extractor().feedUrl)
    }

    @Test
    override fun testSubscriberCount() {
        assertEquals(-1, extractor().subscriberCount)
    }

    @Test
    override fun testVerified() {
        assertFalse(extractor().isVerified)
    }

    @Test
    override fun testServiceId() {
        assertEquals(Bandcamp.serviceId, extractor().serviceId)
    }

    @Test
    override fun testName() {
        assertEquals("toupie", extractor().name)
    }

    @Test
    override fun testId() {
        assertEquals("2450875064", extractor().id)
    }

    @Test
    override fun testUrl() {
        assertEquals("https://toupie.bandcamp.com", extractor().url)
    }

    @Test
    override fun testOriginalUrl() {
        assertEquals("https://toupie.bandcamp.com", extractor().url)
    }

    @Disabled(
        "broken upstream as well (TeamNewPipe/NewPipeExtractor dev 6faf4068, 2026-10): " +
            "the recorded page no longer lists an albums tab"
    )
    @Test
    override fun testTabs() {
        assertTabsContain(extractor().tabs, ChannelTabs.ALBUMS)
    }

    @Test
    override fun testTags() {
        assertTrue(extractor().tags.isEmpty())
    }
}

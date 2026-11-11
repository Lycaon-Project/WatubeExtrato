// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

package org.schabi.newpipe.extractor.services.bandcamp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.InitNewPipeTest
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper
import org.schabi.newpipe.extractor.services.bandcamp.linkHandler.BandcampChannelLinkHandlerFactory

/**
 * Test for [BandcampChannelLinkHandlerFactory]
 */
class BandcampChannelLinkHandlerFactoryTest : InitNewPipeTest {
    private lateinit var linkHandler: BandcampChannelLinkHandlerFactory

    @BeforeAll
    override fun setUp() {
        super.setUp()
        linkHandler = BandcampChannelLinkHandlerFactory.getInstance()
    }

    @Test
    fun testAcceptUrl() {
        // Bandcamp URLs
        assertTrue(linkHandler.acceptUrl("http://zachbenson.bandcamp.com"))
        assertTrue(linkHandler.acceptUrl("https://zachbenson.bandcamp.com/"))
        assertTrue(linkHandler.acceptUrl("https://billwurtz.bandcamp.com/releases"))
        assertTrue(linkHandler.acceptUrl("https://interovgm.bandcamp.com/releases"))
        assertTrue(linkHandler.acceptUrl("https://interovgm.bandcamp.com/releases/"))

        assertTrue(linkHandler.acceptUrl("http://zachbenson.bandcamp.com/"))

        assertFalse(linkHandler.acceptUrl("https://bandcamp.com"))
        assertFalse(linkHandler.acceptUrl("https://zachbenson.bandcamp.com/track/kitchen"))
        assertFalse(linkHandler.acceptUrl("https://daily.bandcamp.com/"))
        assertFalse(linkHandler.acceptUrl("https://DAILY.BANDCAMP.COM"))
        assertFalse(
            linkHandler.acceptUrl(
                "https://daily.bandcamp.com/best-of-2020/" +
                    "bandcamp-daily-staffers-on-their-favorite-albums-of-2020"
            )
        )

        // External URLs are never loaded just to be recognized
        assertFalse(linkHandler.acceptUrl("https://example.com/releases"))
        assertFalse(linkHandler.acceptUrl("https://192.168.1.1/music"))
    }

    @Test
    fun testCustomDomainsAreRecognizedOnceConfirmed() {
        assertFalse(linkHandler.acceptUrl("https://diskak.usopop.com/releases"))

        // confirmed by the page, once the user chose to open it
        assertTrue(BandcampExtractorHelper.verifyCustomDomain("https://diskak.usopop.com/"))
        assertTrue(linkHandler.acceptUrl("https://diskak.usopop.com/"))
        assertTrue(linkHandler.acceptUrl("https://diskak.usopop.com/releases"))
        assertTrue(linkHandler.acceptUrl("https://diskak.usopop.com/RELEASES"))

        // given by Bandcamp's own API
        assertEquals("https://lobstertheremin.com", linkHandler.getUrl("2735462545"))
        assertTrue(linkHandler.acceptUrl("https://lobstertheremin.com"))
        assertTrue(linkHandler.acceptUrl("https://lobstertheremin.com/music"))
        assertTrue(linkHandler.acceptUrl("https://lobstertheremin.com/music/"))

        assertFalse(BandcampExtractorHelper.verifyCustomDomain("https://bandcamp.com/"))
    }

    @Test
    fun testGetId() {
        assertEquals("1196681540", linkHandler.getId("https://macbenson.bandcamp.com/"))
        assertEquals("1196681540", linkHandler.getId("http://macbenson.bandcamp.com/"))
        assertEquals("1581461772", linkHandler.getId("https://shirakumon.bandcamp.com/releases"))
        assertEquals("3321800855", linkHandler.getId("https://infiniteammo.bandcamp.com/"))
        assertEquals("3775652329", linkHandler.getId("https://npet.bandcamp.com/"))

        assertEquals("2735462545", linkHandler.getId("http://lobstertheremin.com/"))
        assertEquals("2735462545", linkHandler.getId("https://lobstertheremin.com/music/"))
        assertEquals("3826445168", linkHandler.getId("https://diskak.usopop.com/releases"))
    }

    @Test
    fun testGetUrl() {
        assertEquals("https://macbenson.bandcamp.com", linkHandler.getUrl("1196681540"))
        assertEquals("https://shirakumon.bandcamp.com", linkHandler.getUrl("1581461772"))
        assertEquals("https://infiniteammo.bandcamp.com", linkHandler.getUrl("3321800855"))

        assertEquals("https://lobstertheremin.com", linkHandler.getUrl("2735462545"))
    }

    @Test
    fun testGetUrlWithInvalidId() {
        assertThrows(ParsingException::class.java) { linkHandler.getUrl("0") }
    }

    @Test
    fun testGetIdWithInvalidUrl() {
        assertThrows(ParsingException::class.java) { linkHandler.getUrl("https://bandcamp.com") }
    }
}

// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

package org.schabi.newpipe.extractor.services.bandcamp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.services.bandcamp.linkHandler.BandcampFeaturedLinkHandlerFactory

/**
 * Tests for [BandcampFeaturedLinkHandlerFactory]
 */
class BandcampFeaturedLinkHandlerFactoryTest {

    companion object {
        private lateinit var linkHandler: BandcampFeaturedLinkHandlerFactory

        @JvmStatic
        @BeforeAll
        fun setUp() {
            linkHandler = BandcampFeaturedLinkHandlerFactory.getInstance()
        }
    }

    @Test
    fun testAcceptUrl() {
        assertTrue(linkHandler.acceptUrl("http://bandcamp.com/?show=1"))
        assertTrue(linkHandler.acceptUrl("https://bandcamp.com/?show=1"))
        assertTrue(linkHandler.acceptUrl("http://bandcamp.com/?show=2"))
        assertTrue(linkHandler.acceptUrl("https://bandcamp.com/api/mobile/24/bootstrap_data"))
        assertTrue(linkHandler.acceptUrl("https://bandcamp.com/api/bcweekly/3/list"))

        assertFalse(linkHandler.acceptUrl("https://bandcamp.com/?show="))
        assertFalse(linkHandler.acceptUrl("https://bandcamp.com/?show=a"))
        assertFalse(linkHandler.acceptUrl("https://bandcamp.com/"))
    }

    @Test
    fun testGetUrl() {
        assertEquals("https://bandcamp.com/api/mobile/24/bootstrap_data", linkHandler.getUrl("Featured"))
        assertEquals("https://bandcamp.com/api/bcweekly/3/list", linkHandler.getUrl("Radio"))
    }

    @Test
    fun testGetId() {
        assertEquals("Featured", linkHandler.getId("http://bandcamp.com/api/mobile/24/bootstrap_data"))
        assertEquals("Featured", linkHandler.getId("https://bandcamp.com/api/mobile/24/bootstrap_data"))
        assertEquals("Radio", linkHandler.getId("http://bandcamp.com/?show=1"))
        assertEquals("Radio", linkHandler.getId("https://bandcamp.com/api/bcweekly/3/list"))
    }
}

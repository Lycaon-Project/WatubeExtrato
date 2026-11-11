// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

package org.schabi.newpipe.extractor.services.bandcamp

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.InitNewPipeTest
import org.schabi.newpipe.extractor.services.bandcamp.linkHandler.BandcampCommentsLinkHandlerFactory

/**
 * Test for [BandcampCommentsLinkHandlerFactory]
 */
class BandcampCommentsLinkHandlerFactoryTest {

    companion object {
        private lateinit var linkHandler: BandcampCommentsLinkHandlerFactory

        @JvmStatic
        @BeforeAll
        fun setUp() {
            InitNewPipeTest.initEmpty()
            linkHandler = BandcampCommentsLinkHandlerFactory.getInstance()
        }
    }

    @Test
    fun testAcceptUrl() {
        assertFalse(linkHandler.acceptUrl("http://interovgm.com/releases/"))
        assertFalse(linkHandler.acceptUrl("https://interovgm.com/releases"))
        assertFalse(linkHandler.acceptUrl("http://zachbenson.bandcamp.com"))
        assertFalse(linkHandler.acceptUrl("https://bandcamp.com"))
        assertFalse(linkHandler.acceptUrl("https://zachbenson.bandcamp.com/"))
        assertFalse(linkHandler.acceptUrl("https://example.com/track/sampletrack"))

        assertTrue(linkHandler.acceptUrl("http://bandcamP.com/?show=38"))
        assertTrue(
            linkHandler.acceptUrl("https://powertothequeerkids.bandcamp.com/album/power-to-the-queer-kids")
        )
        assertTrue(linkHandler.acceptUrl("https://zachbenson.bandcamp.com/track/kitchen"))
        assertTrue(linkHandler.acceptUrl("http://ZachBenson.Bandcamp.COM/Track/U-I-Tonite/"))
        assertTrue(linkHandler.acceptUrl("https://interovgm.bandcamp.com/track/title"))
        assertTrue(linkHandler.acceptUrl("https://goodgoodblood-tl.bandcamp.com/track/when-it-all-wakes-up"))
    }
}

package org.schabi.newpipe.extractor.services.soundcloud

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.InitNewPipeTest
import org.schabi.newpipe.extractor.services.soundcloud.linkHandler.SoundcloudChartsLinkHandlerFactory

/**
 * Test for [SoundcloudChartsLinkHandlerFactory]
 */
class SoundcloudChartsLinkHandlerFactoryTest {

    companion object {
        private lateinit var linkHandler: SoundcloudChartsLinkHandlerFactory

        @JvmStatic
        @BeforeAll
        fun setUp() {
            InitNewPipeTest.initEmpty()
            linkHandler = SoundcloudChartsLinkHandlerFactory.getInstance()
        }
    }

    @Test
    fun getUrl() {
        assertEquals("https://soundcloud.com/charts/new", linkHandler.fromId("New & hot").url)
    }

    @Test
    fun getId() {
        assertEquals(
            "New & hot",
            linkHandler.fromUrl("HTTP://www.soundcloud.com/charts/new/?genre=all-music&country=all-countries").id
        )
    }

    @Test
    fun acceptUrl() {
        assertTrue(linkHandler.acceptUrl("https://soundcloud.com/charts"))
        assertTrue(linkHandler.acceptUrl("https://soundcloud.com/charts/"))
        assertTrue(linkHandler.acceptUrl("https://www.soundcloud.com/charts/new"))
        assertTrue(linkHandler.acceptUrl("http://soundcloud.com/charts/top?genre=all-music"))
        assertTrue(
            linkHandler.acceptUrl("HTTP://www.soundcloud.com/charts/new/?genre=all-music&country=all-countries")
        )

        assertFalse(linkHandler.acceptUrl("kdskjfiiejfia"))
        assertFalse(linkHandler.acceptUrl("soundcloud.com/charts askjkf"))
        assertFalse(linkHandler.acceptUrl("    soundcloud.com/charts"))
        assertFalse(linkHandler.acceptUrl(""))
    }
}

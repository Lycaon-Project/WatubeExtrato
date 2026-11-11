package org.schabi.newpipe.extractor.services.peertube

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.InitNewPipeTest
import org.schabi.newpipe.extractor.ServiceList.PeerTube
import org.schabi.newpipe.extractor.services.peertube.PeertubeLinkHandlerFactoryTestHelper.assertDoNotAcceptNonURLs
import org.schabi.newpipe.extractor.services.peertube.linkHandler.PeertubeChannelLinkHandlerFactory

/**
 * Test for [PeertubeChannelLinkHandlerFactory]
 */
class PeertubeChannelLinkHandlerFactoryTest {

    @Test
    fun acceptUrlTest() {
        assertTrue(linkHandler.acceptUrl("https://peertube.stream/accounts/kranti@videos.squat.net"))
        assertTrue(linkHandler.acceptUrl("https://peertube.stream/a/kranti@videos.squat.net"))
        assertTrue(linkHandler.acceptUrl("https://peertube.stream/api/v1/accounts/kranti@videos.squat.net/videos"))
        assertTrue(
            linkHandler.acceptUrl("https://peertube.stream/video-channels/kranti_channel@videos.squat.net/videos")
        )
        assertTrue(linkHandler.acceptUrl("https://peertube.stream/c/kranti_channel@videos.squat.net/videos"))
        assertTrue(
            linkHandler.acceptUrl("https://peertube.stream/api/v1/video-channels/7682d9f2-07be-4622-862e-93ec812e2ffa")
        )
        assertTrue(linkHandler.acceptUrl("https://kolektiva.media/video-channels/documentary_channel"))
        assertDoNotAcceptNonURLs(linkHandler)
    }

    @Test
    fun getId() {
        assertEquals(
            "accounts/kranti@videos.squat.net",
            linkHandler.fromUrl("https://peertube.stream/accounts/kranti@videos.squat.net").id
        )
        assertEquals(
            "accounts/kranti@videos.squat.net",
            linkHandler.fromUrl("https://peertube.stream/a/kranti@videos.squat.net").id
        )
        assertEquals(
            "accounts/kranti@videos.squat.net",
            linkHandler.fromUrl("https://peertube.stream/accounts/kranti@videos.squat.net/videos").id
        )
        assertEquals(
            "accounts/kranti@videos.squat.net",
            linkHandler.fromUrl("https://peertube.stream/a/kranti@videos.squat.net/videos").id
        )
        assertEquals(
            "accounts/kranti@videos.squat.net",
            linkHandler.fromUrl("https://peertube.stream/api/v1/accounts/kranti@videos.squat.net").id
        )
        assertEquals(
            "accounts/kranti@videos.squat.net",
            linkHandler.fromUrl("https://peertube.stream/api/v1/accounts/kranti@videos.squat.net/videos").id
        )
        assertEquals(
            "video-channels/kranti_channel@videos.squat.net",
            linkHandler.fromUrl("https://peertube.stream/video-channels/kranti_channel@videos.squat.net/videos").id
        )
        assertEquals(
            "video-channels/kranti_channel@videos.squat.net",
            linkHandler.fromUrl("https://peertube.stream/c/kranti_channel@videos.squat.net/videos").id
        )
        assertEquals(
            "video-channels/kranti_channel@videos.squat.net",
            linkHandler.fromUrl("https://peertube.stream/c/kranti_channel@videos.squat.net/video-playlists").id
        )
        assertEquals(
            "video-channels/kranti_channel@videos.squat.net",
            linkHandler.fromUrl("https://peertube.stream/api/v1/video-channels/kranti_channel@videos.squat.net").id
        )

        // instance URL ending with an "a": https://kolektiva.media
        assertEquals(
            "video-channels/documentary_channel",
            linkHandler.fromUrl("https://kolektiva.media/video-channels/documentary_channel/videos").id
        )
        assertEquals(
            "video-channels/documentary_channel",
            linkHandler.fromUrl("https://kolektiva.media/c/documentary_channel/videos").id
        )
        assertEquals(
            "video-channels/documentary_channel",
            linkHandler.fromUrl("https://kolektiva.media/c/documentary_channel/video-playlists").id
        )
        assertEquals(
            "video-channels/documentary_channel",
            linkHandler.fromUrl("https://kolektiva.media/api/v1/video-channels/documentary_channel").id
        )
    }

    @Test
    fun getUrl() {
        assertEquals(
            "https://peertube.stream/video-channels/kranti_channel@videos.squat.net",
            linkHandler.fromId("video-channels/kranti_channel@videos.squat.net").url
        )
        assertEquals(
            "https://peertube.stream/accounts/kranti@videos.squat.net",
            linkHandler.fromId("accounts/kranti@videos.squat.net").url
        )
        assertEquals(
            "https://peertube.stream/accounts/kranti@videos.squat.net",
            linkHandler.fromId("kranti@videos.squat.net").url
        )
        assertEquals(
            "https://peertube.stream/video-channels/kranti_channel@videos.squat.net",
            linkHandler.fromUrl("https://peertube.stream/api/v1/video-channels/kranti_channel@videos.squat.net").url
        )
    }

    companion object {
        private lateinit var linkHandler: PeertubeChannelLinkHandlerFactory

        @JvmStatic
        @BeforeAll
        fun setUp() {
            InitNewPipeTest.initEmpty()
            PeerTube.instance = PeertubeInstance("https://peertube.stream", "PeerTube on peertube.stream")
            linkHandler = PeertubeChannelLinkHandlerFactory.getInstance()
        }
    }
}

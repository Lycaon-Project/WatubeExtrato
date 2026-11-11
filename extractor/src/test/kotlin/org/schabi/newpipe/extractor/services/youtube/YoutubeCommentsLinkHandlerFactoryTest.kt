package org.schabi.newpipe.extractor.services.youtube

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.InitNewPipeTest
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeCommentsLinkHandlerFactory
import java.util.Optional

class YoutubeCommentsLinkHandlerFactoryTest {

    @Test
    fun getIdWithNullAsUrl() {
        // a platform-typed null reaches the parameter check of fromId, which must reject it
        assertThrows(NullPointerException::class.java) { linkHandler.fromId(Optional.empty<String>().orElse(null)) }
    }

    @Test
    fun getIdFromYt() {
        assertEquals("VM_6n762j6M", linkHandler.fromUrl("https://www.youtube.com/watch?v=VM_6n762j6M").id)
        assertEquals("VM_6n762j6M", linkHandler.fromUrl("https://m.youtube.com/watch?v=VM_6n762j6M").id)
        assertEquals("VM_6n762j6M", linkHandler.fromUrl("https://youtube.com/watch?v=VM_6n762j6M").id)
        assertEquals("VM_6n762j6M", linkHandler.fromUrl("https://WWW.youtube.com/watch?v=VM_6n762j6M").id)
        assertEquals("VM_6n762j6M", linkHandler.fromUrl("https://youtu.be/VM_6n762j6M").id)
        assertEquals("VM_6n762j6M", linkHandler.fromUrl("https://youtu.be/VM_6n762j6M&t=20").id)
    }

    @Test
    fun testAcceptUrl() {
        assertTrue(linkHandler.acceptUrl("https://www.youtube.com/watch?v=VM_6n762j6M&t=20"))
        assertTrue(linkHandler.acceptUrl("https://WWW.youtube.com/watch?v=VM_6n762j6M&t=20"))
        assertTrue(linkHandler.acceptUrl("https://youtube.com/watch?v=VM_6n762j6M&t=20"))
        assertTrue(linkHandler.acceptUrl("https://youtu.be/VM_6n762j6M&t=20"))
    }

    @Test
    fun testDeniesUrl() {
        assertFalse(linkHandler.acceptUrl("https://www.you com/watch?v=VM_6n762j6M"))
        assertFalse(linkHandler.acceptUrl("https://com/watch?v=VM_6n762j6M"))
        assertFalse(linkHandler.acceptUrl("htt ://com/watch?v=VM_6n762j6M"))
        assertFalse(linkHandler.acceptUrl("ftp://www.youtube.com/watch?v=VM_6n762j6M"))
    }

    @Test
    fun getIdFromInvidious() {
        assertEquals("VM_6n762j6M", linkHandler.fromUrl("https://redirect.invidious.io/watch?v=VM_6n762j6M").id)
        assertEquals("VM_6n762j6M", linkHandler.fromUrl("https://redirect.invidious.io/watch?v=VM_6n762j6M").id)
        assertEquals("VM_6n762j6M", linkHandler.fromUrl("https://REDIRECT.INVIDIOUS.IO/watch?v=VM_6n762j6M").id)
        assertEquals("VM_6n762j6M", linkHandler.fromUrl("https://redirect.invidious.io/VM_6n762j6M").id)
        assertEquals("VM_6n762j6M", linkHandler.fromUrl("https://redirect.invidious.io/VM_6n762j6M&t=20").id)
    }

    @Test
    fun getIdFromY2ube() {
        assertEquals("VM_6n762j6M", linkHandler.fromUrl("https://y2u.be/VM_6n762j6M").id)
        assertEquals("VM_6n762j6M", linkHandler.fromUrl("https://Y2U.Be/VM_6n762j6M").id)
    }

    companion object {
        private lateinit var linkHandler: YoutubeCommentsLinkHandlerFactory

        @JvmStatic
        @BeforeAll
        fun setUp() {
            InitNewPipeTest.initEmpty()
            linkHandler = YoutubeCommentsLinkHandlerFactory.getInstance()
        }
    }
}

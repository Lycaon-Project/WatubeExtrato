// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

package org.schabi.newpipe.extractor.services.bandcamp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.InitNewPipeTest
import org.schabi.newpipe.extractor.ServiceList.Bandcamp
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.services.DefaultSearchExtractorTest
import org.schabi.newpipe.extractor.stream.StreamInfoItem

private const val NOISE_QUERY = "noise"

/**
 * Test for [org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampSearchExtractor]
 */
class BandcampSearchExtractorTest : InitNewPipeTest {

    /**
     * Tests whether searching bandcamp for "best friend's basement" returns
     * the accordingly named song by Zach Benson
     */
    @Test
    fun testStreamSearch() {
        val extractor = Bandcamp.getSearchExtractor("best friend's basement")

        val page = extractor.initialPage
        val bestFriendsBasement = page.items[0] as StreamInfoItem

        // The track by Zach Benson should be the first result, no?
        assertEquals("Best Friend's Basement", bestFriendsBasement.name)
        assertEquals("Zach Benson", bestFriendsBasement.uploaderName)
        BandcampTestUtils.testImages(bestFriendsBasement.thumbnails)
        assertEquals(InfoItem.InfoType.STREAM, bestFriendsBasement.infoType)
    }

    /**
     * Tests whether searching bandcamp for "C418" returns the artist's profile
     */
    @Test
    fun testChannelSearch() {
        val extractor = Bandcamp.getSearchExtractor("C418")
        val c418 = extractor.initialPage.items[0]

        // C418's artist profile should be the first result, no?
        assertEquals("C418", c418.name)
        BandcampTestUtils.testImages(c418.thumbnails)
        assertEquals("https://c418.bandcamp.com", c418.url)
    }

    /**
     * Tests whether searching bandcamp for "minecraft volume alpha" returns the corresponding album
     */
    @Test
    fun testAlbumSearch() {
        val extractor = Bandcamp.getSearchExtractor("minecraft volume alpha")
        val minecraft = extractor.initialPage.items[0]

        // Minecraft volume alpha should be the first result, no?
        assertEquals("Minecraft - Volume Alpha", minecraft.name)
        BandcampTestUtils.testImages(minecraft.thumbnails)
        assertEquals("https://c418.bandcamp.com/album/minecraft-volume-alpha", minecraft.url)

        // Verify that playlist tracks counts get extracted correctly
        assertEquals(24, (minecraft as PlaylistInfoItem).streamCount)
    }

    /**
     * Tests searches with multiple pages
     */
    @Test
    fun testMultiplePages() {
        // A query practically guaranteed to have the maximum amount of pages
        val extractor = Bandcamp.getSearchExtractor("e")

        val page2 = extractor.initialPage.nextPage
        assertEquals("https://bandcamp.com/search?q=e&page=2", page2!!.url)

        val page3 = extractor.getPage(page2).nextPage
        assertEquals("https://bandcamp.com/search?q=e&page=3", page3!!.url)
    }

    class DefaultTest : DefaultSearchExtractorTest() {

        override fun createExtractor(): SearchExtractor = Bandcamp.getSearchExtractor(NOISE_QUERY)

        override fun expectedService(): StreamingService = Bandcamp
        override fun expectedName(): String = NOISE_QUERY
        override fun expectedId(): String = NOISE_QUERY
        override fun expectedUrlContains(): String = "bandcamp.com/search?q=$NOISE_QUERY"
        override fun expectedOriginalUrlContains(): String = "bandcamp.com/search?q=$NOISE_QUERY"
        override fun expectedSearchString(): String = NOISE_QUERY
        override fun expectedSearchSuggestion(): String? = null
    }
}

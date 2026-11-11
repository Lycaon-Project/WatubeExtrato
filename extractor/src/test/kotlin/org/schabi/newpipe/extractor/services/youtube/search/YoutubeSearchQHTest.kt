package org.schabi.newpipe.extractor.services.youtube.search

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.CHANNELS
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.MUSIC_SONGS
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.PLAYLISTS
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.VIDEOS

class YoutubeSearchQHTest {

    @Test
    @Throws(Exception::class)
    fun testRegularValues() {
        val factory = YouTube.searchQHFactory
        assertEquals("https://www.youtube.com/results?search_query=asdf&sp=8AEB", factory.fromQuery("asdf").url)
        assertEquals("https://www.youtube.com/results?search_query=hans&sp=8AEB", factory.fromQuery("hans").url)
        assertEquals(
            "https://www.youtube.com/results?search_query=Poifj%26jaijf&sp=8AEB",
            factory.fromQuery("Poifj&jaijf").url,
        )
        assertEquals(
            "https://www.youtube.com/results?search_query=G%C3%BCl%C3%BCm&sp=8AEB",
            factory.fromQuery("Gülüm").url,
        )
        assertEquals(
            "https://www.youtube.com/results?search_query=%3Fj%24%29H%C2%A7B&sp=8AEB",
            factory.fromQuery("?j\$)H§B").url,
        )

        assertEquals("https://music.youtube.com/search?q=asdf", factory.fromQuery("asdf", L_MUSIC_SONGS, "").url)
        assertEquals("https://music.youtube.com/search?q=hans", factory.fromQuery("hans", L_MUSIC_SONGS, "").url)
        assertEquals(
            "https://music.youtube.com/search?q=Poifj%26jaijf",
            factory.fromQuery("Poifj&jaijf", L_MUSIC_SONGS, "").url,
        )
        assertEquals(
            "https://music.youtube.com/search?q=G%C3%BCl%C3%BCm",
            factory.fromQuery("Gülüm", L_MUSIC_SONGS, "").url,
        )
        assertEquals(
            "https://music.youtube.com/search?q=%3Fj%24%29H%C2%A7B",
            factory.fromQuery("?j\$)H§B", L_MUSIC_SONGS, "").url,
        )
    }

    @Test
    @Throws(Exception::class)
    fun testGetContentFilter() {
        val factory = YouTube.searchQHFactory
        assertEquals(VIDEOS, factory.fromQuery("", listOf(VIDEOS), "").contentFilters[0])
        assertEquals(CHANNELS, factory.fromQuery("asdf", listOf(CHANNELS), "").contentFilters[0])

        assertEquals(MUSIC_SONGS, factory.fromQuery("asdf", L_MUSIC_SONGS, "").contentFilters[0])
    }

    @Test
    @Throws(Exception::class)
    fun testWithContentFilter() {
        val factory = YouTube.searchQHFactory
        assertEquals(
            "https://www.youtube.com/results?search_query=asdf&sp=EgIQAfABAQ%253D%253D",
            factory.fromQuery("asdf", listOf(VIDEOS), "").url,
        )
        assertEquals(
            "https://www.youtube.com/results?search_query=asdf&sp=EgIQAvABAQ%253D%253D",
            factory.fromQuery("asdf", listOf(CHANNELS), "").url,
        )
        assertEquals(
            "https://www.youtube.com/results?search_query=asdf&sp=EgIQA_ABAQ%253D%253D",
            factory.fromQuery("asdf", listOf(PLAYLISTS), "").url,
        )
        assertEquals(
            "https://www.youtube.com/results?search_query=asdf&sp=8AEB",
            factory.fromQuery("asdf", listOf("fjiijie"), "").url,
        )

        assertEquals("https://music.youtube.com/search?q=asdf", factory.fromQuery("asdf", L_MUSIC_SONGS, "").url)
    }

    @Test
    fun testGetAvailableContentFilter() {
        assertArrayEquals(
            arrayOf(
                "all",
                "videos",
                "channels",
                "playlists",
                "music_songs",
                "music_videos",
                "music_albums",
                "music_playlists",
            ),
            YouTube.searchQHFactory.availableContentFilter,
        )
    }

    @Test
    fun testGetAvailableSortFilter() {
        val contentFilter = YouTube.searchQHFactory.availableSortFilter
        assertEquals(0, contentFilter.size)
    }

    private companion object {
        val L_MUSIC_SONGS = listOf(MUSIC_SONGS)
    }
}

package org.schabi.newpipe.extractor.services.soundcloud.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.InitNewPipeTest
import org.schabi.newpipe.extractor.ServiceList.SoundCloud
import org.schabi.newpipe.extractor.services.soundcloud.linkHandler.SoundcloudSearchQueryHandlerFactory.PLAYLISTS
import org.schabi.newpipe.extractor.services.soundcloud.linkHandler.SoundcloudSearchQueryHandlerFactory.TRACKS
import org.schabi.newpipe.extractor.services.soundcloud.linkHandler.SoundcloudSearchQueryHandlerFactory.USERS

class SoundcloudSearchQHTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun setUp() {
            InitNewPipeTest.initEmpty()
        }

        private fun removeClientId(url: String): String {
            val splitUrl = url.split(Regex("client_id=[a-zA-Z0-9]*&"))
            return splitUrl[0] + splitUrl[1]
        }
    }

    @Test
    fun testRegularValues() {
        assertEquals(
            "https://api-v2.soundcloud.com/search?q=asdf&limit=10&offset=0",
            removeClientId(SoundCloud.searchQHFactory.fromQuery("asdf").url)
        )
        assertEquals(
            "https://api-v2.soundcloud.com/search?q=hans&limit=10&offset=0",
            removeClientId(SoundCloud.searchQHFactory.fromQuery("hans").url)
        )
        assertEquals(
            "https://api-v2.soundcloud.com/search?q=Poifj%26jaijf&limit=10&offset=0",
            removeClientId(SoundCloud.searchQHFactory.fromQuery("Poifj&jaijf").url)
        )
        assertEquals(
            "https://api-v2.soundcloud.com/search?q=G%C3%BCl%C3%BCm&limit=10&offset=0",
            removeClientId(SoundCloud.searchQHFactory.fromQuery("Gülüm").url)
        )
        assertEquals(
            "https://api-v2.soundcloud.com/search?q=%3Fj%24%29H%C2%A7B&limit=10&offset=0",
            removeClientId(SoundCloud.searchQHFactory.fromQuery("?j$)H§B").url)
        )
    }

    @Test
    fun testGetContentFilter() {
        assertEquals(
            "tracks",
            SoundCloud.searchQHFactory.fromQuery("", listOf("tracks"), "").contentFilters[0]
        )
        assertEquals(
            "users",
            SoundCloud.searchQHFactory.fromQuery("asdf", listOf("users"), "").contentFilters[0]
        )
    }

    @Test
    fun testWithContentfilter() {
        assertEquals(
            "https://api-v2.soundcloud.com/search/tracks?q=asdf&limit=10&offset=0",
            removeClientId(SoundCloud.searchQHFactory.fromQuery("asdf", listOf(TRACKS), "").url)
        )
        assertEquals(
            "https://api-v2.soundcloud.com/search/users?q=asdf&limit=10&offset=0",
            removeClientId(SoundCloud.searchQHFactory.fromQuery("asdf", listOf(USERS), "").url)
        )
        assertEquals(
            "https://api-v2.soundcloud.com/search/playlists?q=asdf&limit=10&offset=0",
            removeClientId(SoundCloud.searchQHFactory.fromQuery("asdf", listOf(PLAYLISTS), "").url)
        )
        assertEquals(
            "https://api-v2.soundcloud.com/search?q=asdf&limit=10&offset=0",
            removeClientId(SoundCloud.searchQHFactory.fromQuery("asdf", listOf("fjiijie"), "").url)
        )
    }

    @Test
    fun testGetAvailableContentFilter() {
        val contentFilter = SoundCloud.searchQHFactory.availableContentFilter
        assertEquals(4, contentFilter.size)
        assertEquals("all", contentFilter[0])
        assertEquals("tracks", contentFilter[1])
        assertEquals("users", contentFilter[2])
        assertEquals("playlists", contentFilter[3])
    }

    @Test
    fun testGetAvailableSortFilter() {
        val contentFilter = SoundCloud.searchQHFactory.availableSortFilter
        assertEquals(0, contentFilter.size)
    }
}

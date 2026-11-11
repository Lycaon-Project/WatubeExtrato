package org.schabi.newpipe.extractor.services.soundcloud.search

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ServiceList.SoundCloud
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.services.DefaultSearchExtractorTest
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest
import org.schabi.newpipe.extractor.services.DefaultTests.assertNoDuplicatedItems
import org.schabi.newpipe.extractor.services.soundcloud.linkHandler.SoundcloudSearchQueryHandlerFactory.PLAYLISTS
import org.schabi.newpipe.extractor.services.soundcloud.linkHandler.SoundcloudSearchQueryHandlerFactory.TRACKS
import org.schabi.newpipe.extractor.services.soundcloud.linkHandler.SoundcloudSearchQueryHandlerFactory.USERS
import org.schabi.newpipe.extractor.utils.Utils

private const val LIL_UZI_QUERY = "lill uzi vert"
private const val GUETTA_QUERY = "David Guetta"
private const val NO_RESULT_QUERY = "wpghüä"

class SoundcloudSearchExtractorTest {

    class All : DefaultSearchExtractorTest() {
        override fun createExtractor(): SearchExtractor = SoundCloud.getSearchExtractor(LIL_UZI_QUERY)

        override fun expectedService(): StreamingService = SoundCloud
        override fun expectedName(): String = LIL_UZI_QUERY
        override fun expectedId(): String = LIL_UZI_QUERY
        override fun expectedUrlContains(): String = "soundcloud.com/search?q=" + Utils.encodeUrlUtf8(LIL_UZI_QUERY)
        override fun expectedOriginalUrlContains(): String =
            "soundcloud.com/search?q=" + Utils.encodeUrlUtf8(LIL_UZI_QUERY)
        override fun expectedSearchString(): String = LIL_UZI_QUERY
        override fun expectedSearchSuggestion(): String? = null
    }

    class Tracks : DefaultSearchExtractorTest() {
        override fun createExtractor(): SearchExtractor =
            SoundCloud.getSearchExtractor(LIL_UZI_QUERY, listOf(TRACKS), "")

        override fun expectedService(): StreamingService = SoundCloud
        override fun expectedName(): String = LIL_UZI_QUERY
        override fun expectedId(): String = LIL_UZI_QUERY
        override fun expectedUrlContains(): String =
            "soundcloud.com/search/tracks?q=" + Utils.encodeUrlUtf8(LIL_UZI_QUERY)
        override fun expectedOriginalUrlContains(): String =
            "soundcloud.com/search/tracks?q=" + Utils.encodeUrlUtf8(LIL_UZI_QUERY)
        override fun expectedSearchString(): String = LIL_UZI_QUERY
        override fun expectedSearchSuggestion(): String? = null
        override fun expectedInfoItemType(): InfoItem.InfoType = InfoItem.InfoType.STREAM
    }

    class Users : DefaultSearchExtractorTest() {
        override fun createExtractor(): SearchExtractor =
            SoundCloud.getSearchExtractor(LIL_UZI_QUERY, listOf(USERS), "")

        override fun expectedService(): StreamingService = SoundCloud
        override fun expectedName(): String = LIL_UZI_QUERY
        override fun expectedId(): String = LIL_UZI_QUERY
        override fun expectedUrlContains(): String =
            "soundcloud.com/search/users?q=" + Utils.encodeUrlUtf8(LIL_UZI_QUERY)
        override fun expectedOriginalUrlContains(): String =
            "soundcloud.com/search/users?q=" + Utils.encodeUrlUtf8(LIL_UZI_QUERY)
        override fun expectedSearchString(): String = LIL_UZI_QUERY
        override fun expectedSearchSuggestion(): String? = null
        override fun expectedInfoItemType(): InfoItem.InfoType = InfoItem.InfoType.CHANNEL
    }

    class Playlists : DefaultSearchExtractorTest() {
        override fun createExtractor(): SearchExtractor =
            SoundCloud.getSearchExtractor(LIL_UZI_QUERY, listOf(PLAYLISTS), "")

        override fun expectedService(): StreamingService = SoundCloud
        override fun expectedName(): String = LIL_UZI_QUERY
        override fun expectedId(): String = LIL_UZI_QUERY
        override fun expectedUrlContains(): String =
            "soundcloud.com/search/playlists?q=" + Utils.encodeUrlUtf8(LIL_UZI_QUERY)
        override fun expectedOriginalUrlContains(): String =
            "soundcloud.com/search/playlists?q=" + Utils.encodeUrlUtf8(LIL_UZI_QUERY)
        override fun expectedSearchString(): String = LIL_UZI_QUERY
        override fun expectedSearchSuggestion(): String? = null
        override fun expectedInfoItemType(): InfoItem.InfoType = InfoItem.InfoType.PLAYLIST
    }

    class PagingTest : DefaultSimpleExtractorTest<SearchExtractor>() {

        override fun createExtractor(): SearchExtractor =
            SoundCloud.getSearchExtractor("cirque du soleil", listOf(TRACKS), "")

        @Test
        fun duplicatedItemsCheck() {
            val page1 = extractor().initialPage
            val page2 = extractor().getPage(page1.nextPage)

            assertNoDuplicatedItems(SoundCloud, page1, page2)
        }
    }

    class UserVerified : DefaultSearchExtractorTest() {

        override fun createExtractor(): SearchExtractor =
            SoundCloud.getSearchExtractor(GUETTA_QUERY, listOf(USERS), "")

        override fun expectedService(): StreamingService = SoundCloud
        override fun expectedName(): String = GUETTA_QUERY
        override fun expectedId(): String = GUETTA_QUERY
        override fun expectedUrlContains(): String =
            "soundcloud.com/search/users?q=" + Utils.encodeUrlUtf8(GUETTA_QUERY)
        override fun expectedOriginalUrlContains(): String =
            "soundcloud.com/search/users?q=" + Utils.encodeUrlUtf8(GUETTA_QUERY)
        override fun expectedSearchString(): String = GUETTA_QUERY
        override fun expectedSearchSuggestion(): String? = null

        override fun expectedInfoItemType(): InfoItem.InfoType = InfoItem.InfoType.CHANNEL

        @Test
        fun testIsVerified() {
            val items = extractor().initialPage.items
            var verified = false
            for (item in items) {
                if (item.url == "https://soundcloud.com/davidguetta") {
                    verified = (item as ChannelInfoItem).isVerified
                    break
                }
            }
            assertTrue(verified)
        }
    }

    class NoNextPage : DefaultSearchExtractorTest() {

        override fun createExtractor(): SearchExtractor = SoundCloud.getSearchExtractor(NO_RESULT_QUERY)

        override fun expectedHasMoreItems(): Boolean = false
        override fun expectedService(): StreamingService = SoundCloud
        override fun expectedName(): String = NO_RESULT_QUERY
        override fun expectedId(): String = NO_RESULT_QUERY
        override fun expectedUrlContains(): String =
            "soundcloud.com/search?q=" + Utils.encodeUrlUtf8(NO_RESULT_QUERY)
        override fun expectedOriginalUrlContains(): String =
            "soundcloud.com/search?q=" + Utils.encodeUrlUtf8(NO_RESULT_QUERY)
        override fun expectedSearchString(): String = NO_RESULT_QUERY
        override fun expectedSearchSuggestion(): String? = null
    }
}

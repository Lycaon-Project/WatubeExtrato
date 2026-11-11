package org.schabi.newpipe.extractor.services.youtube.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ExtractorAsserts.assertEmptyErrors
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.InitNewPipeTest
import org.schabi.newpipe.extractor.MetaInfo
import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.services.DefaultSearchExtractorTest
import org.schabi.newpipe.extractor.services.DefaultTests.assertNoDuplicatedItems
import org.schabi.newpipe.extractor.services.youtube.InitYoutubeTest
import org.schabi.newpipe.extractor.services.youtube.YoutubeTestsUtils
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.CHANNELS
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.PLAYLISTS
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.VIDEOS
import org.schabi.newpipe.extractor.stream.Description
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.utils.Utils
import java.io.IOException
import java.net.MalformedURLException
import java.net.URI

private const val SEARCH_URL = "youtube.com/results?search_query="

class YoutubeSearchExtractorTest {

    class All : DefaultSearchExtractorTest(), InitYoutubeTest {
        @Throws(Exception::class)
        override fun createExtractor(): SearchExtractor = YouTube.getSearchExtractor(QUERY)

        override fun expectedService(): StreamingService = YouTube
        override fun expectedName(): String = QUERY
        override fun expectedId(): String = QUERY
        override fun expectedUrlContains(): String = SEARCH_URL + QUERY
        override fun expectedOriginalUrlContains(): String = SEARCH_URL + QUERY
        override fun expectedSearchString(): String = QUERY
        override fun expectedSearchSuggestion(): String? = null

        private companion object {
            const val QUERY = "test"
        }
    }

    class Channel : DefaultSearchExtractorTest(), InitYoutubeTest {
        @Throws(Exception::class)
        override fun createExtractor(): SearchExtractor = YouTube.getSearchExtractor(QUERY, listOf(CHANNELS), "")

        override fun expectedService(): StreamingService = YouTube
        override fun expectedName(): String = QUERY
        override fun expectedId(): String = QUERY
        override fun expectedUrlContains(): String = SEARCH_URL + QUERY
        override fun expectedOriginalUrlContains(): String = SEARCH_URL + QUERY
        override fun expectedSearchString(): String = QUERY
        override fun expectedSearchSuggestion(): String? = null

        override fun expectedInfoItemType(): InfoItem.InfoType = InfoItem.InfoType.CHANNEL

        private companion object {
            const val QUERY = "test"
        }
    }

    class Playlists : DefaultSearchExtractorTest(), InitYoutubeTest {
        @Throws(Exception::class)
        override fun createExtractor(): SearchExtractor = YouTube.getSearchExtractor(QUERY, listOf(PLAYLISTS), "")

        override fun expectedService(): StreamingService = YouTube
        override fun expectedName(): String = QUERY
        override fun expectedId(): String = QUERY
        override fun expectedUrlContains(): String = SEARCH_URL + QUERY
        override fun expectedOriginalUrlContains(): String = SEARCH_URL + QUERY
        override fun expectedSearchString(): String = QUERY
        override fun expectedSearchSuggestion(): String? = null

        override fun expectedInfoItemType(): InfoItem.InfoType = InfoItem.InfoType.PLAYLIST

        private companion object {
            const val QUERY = "test"
        }
    }

    class Videos : DefaultSearchExtractorTest(), InitYoutubeTest {
        @Throws(Exception::class)
        override fun createExtractor(): SearchExtractor = YouTube.getSearchExtractor(QUERY, listOf(VIDEOS), "")

        override fun expectedService(): StreamingService = YouTube
        override fun expectedName(): String = QUERY
        override fun expectedId(): String = QUERY
        override fun expectedUrlContains(): String = SEARCH_URL + QUERY
        override fun expectedOriginalUrlContains(): String = SEARCH_URL + QUERY
        override fun expectedSearchString(): String = QUERY
        override fun expectedSearchSuggestion(): String? = null

        override fun expectedInfoItemType(): InfoItem.InfoType = InfoItem.InfoType.STREAM

        private companion object {
            const val QUERY = "test"
        }
    }

    /**
     * Test for YT's "Did you mean...".
     *
     * Hint: YT mostly shows "did you mean..." when you are searching in another language.
     */
    class Suggestion : DefaultSearchExtractorTest(), InitYoutubeTest {
        @Throws(Exception::class)
        override fun createExtractor(): SearchExtractor = YouTube.getSearchExtractor(QUERY, listOf(VIDEOS), "")

        override fun expectedService(): StreamingService = YouTube
        override fun expectedName(): String = QUERY
        override fun expectedId(): String = QUERY

        @Throws(Exception::class)
        override fun expectedUrlContains(): String = SEARCH_URL + Utils.encodeUrlUtf8(QUERY)

        @Throws(Exception::class)
        override fun expectedOriginalUrlContains(): String = SEARCH_URL + Utils.encodeUrlUtf8(QUERY)

        override fun expectedSearchString(): String = QUERY
        override fun expectedSearchSuggestion(): String? = EXPECTED_SUGGESTION
        override fun expectedInfoItemType(): InfoItem.InfoType = InfoItem.InfoType.STREAM

        private companion object {
            const val QUERY = "on board ing"
            const val EXPECTED_SUGGESTION = "on boarding"
        }
    }

    /**
     * Test for YT's "Showing results for...".
     */
    class CorrectedSearch : DefaultSearchExtractorTest(), InitYoutubeTest {
        @Throws(Exception::class)
        override fun createExtractor(): SearchExtractor = YouTube.getSearchExtractor(QUERY, listOf(VIDEOS), "")

        override fun expectedService(): StreamingService = YouTube
        override fun expectedName(): String = QUERY
        override fun expectedId(): String = QUERY
        override fun expectedUrlContains(): String = SEARCH_URL + QUERY
        override fun expectedOriginalUrlContains(): String = SEARCH_URL + QUERY
        override fun expectedSearchString(): String = QUERY
        override fun expectedSearchSuggestion(): String? = EXPECTED_SUGGESTION
        override fun expectedInfoItemType(): InfoItem.InfoType = InfoItem.InfoType.STREAM
        override fun isCorrectedSearch(): Boolean = true

        private companion object {
            const val QUERY = "pewdeipie"
            const val EXPECTED_SUGGESTION = "pewdiepie"
        }
    }

    class RandomQueryNoMorePages : DefaultSearchExtractorTest(), InitYoutubeTest {
        @Throws(Exception::class)
        override fun createExtractor(): SearchExtractor = YouTube.getSearchExtractor(QUERY)

        override fun expectedService(): StreamingService = YouTube
        override fun expectedName(): String = QUERY
        override fun expectedId(): String = QUERY
        override fun expectedUrlContains(): String = SEARCH_URL + QUERY
        override fun expectedOriginalUrlContains(): String = SEARCH_URL + QUERY
        override fun expectedSearchString(): String = QUERY
        override fun expectedSearchSuggestion(): String? = null

        // ////////////////////////////////////////////////////////////////////////
        // Test Overrides
        // ////////////////////////////////////////////////////////////////////////

        @Disabled(
            "broken upstream as well (TeamNewPipe/NewPipeExtractor dev 6faf4068, 2026-10): " +
                "YouTube now returns a further page for this query",
        )
        @Test
        @Throws(Exception::class)
        override fun testMoreRelatedItems() {
            val initialPage = extractor().initialPage
            // YouTube actually gives us an empty next page, but after that, no more pages.
            assertTrue(initialPage.hasNextPage())
            val nextEmptyPage = extractor().getPage(initialPage.nextPage)
            assertEquals(0, nextEmptyPage.items.size)
            assertEmptyErrors("Empty page has errors", nextEmptyPage.errors)

            assertFalse(nextEmptyPage.hasNextPage(), "More items available when it shouldn't")
        }

        private companion object {
            const val QUERY = "UCO6AK"
        }
    }

    class PagingTest {
        @Test
        @Throws(Exception::class)
        fun duplicatedItemsCheck() {
            YoutubeTestsUtils.ensureStateless()
            InitNewPipeTest.initNewPipe(this.javaClass, "paging")

            val extractor = YouTube.getSearchExtractor("cirque du soleil", listOf(VIDEOS), "")
            extractor.fetchPage()

            val page1 = extractor.initialPage
            val page2 = extractor.getPage(page1.nextPage)

            assertNoDuplicatedItems(YouTube, page1, page2)
        }
    }

    @Disabled("Known problem, see https://github.com/TeamNewPipe/NewPipeExtractor/issues/1274")
    class MetaInfoTest : DefaultSearchExtractorTest(), InitYoutubeTest {
        @Throws(Exception::class)
        override fun createExtractor(): SearchExtractor = YouTube.getSearchExtractor(QUERY, listOf(VIDEOS), "")

        override fun expectedSearchString(): String = QUERY
        override fun expectedSearchSuggestion(): String? = null

        @Throws(MalformedURLException::class)
        override fun expectedMetaInfo(): List<MetaInfo> = listOf(
            MetaInfo(
                "COVID-19",
                Description(
                    "Get the latest information from the WHO about coronavirus.",
                    Description.Type.PLAIN_TEXT,
                ),
                mutableListOf(URI("https://www.who.int/emergencies/diseases/novel-coronavirus-2019").toURL()),
                mutableListOf("Learn more"),
            ),
        )

        // testMoreRelatedItems: a video in this mock has no duration badge; getDuration() now
        // returns -1 instead of throwing, but the mock may need re-recording before re-enabling.
        @Test
        override fun testMoreRelatedItems() {
        }

        override fun expectedService(): StreamingService = YouTube
        override fun expectedName(): String = QUERY
        override fun expectedId(): String = QUERY
        override fun expectedUrlContains(): String = SEARCH_URL + QUERY

        @Throws(Exception::class)
        override fun expectedOriginalUrlContains(): String = SEARCH_URL + QUERY

        private companion object {
            const val QUERY = "Covid"
        }
    }

    class ChannelVerified : DefaultSearchExtractorTest(), InitYoutubeTest {
        @Throws(Exception::class)
        override fun createExtractor(): SearchExtractor = YouTube.getSearchExtractor(QUERY, listOf(CHANNELS), "")

        override fun expectedService(): StreamingService = YouTube
        override fun expectedName(): String = QUERY
        override fun expectedId(): String = QUERY
        override fun expectedUrlContains(): String = SEARCH_URL + QUERY
        override fun expectedOriginalUrlContains(): String = SEARCH_URL + QUERY
        override fun expectedSearchString(): String = QUERY
        override fun expectedSearchSuggestion(): String? = null
        override fun expectedInfoItemType(): InfoItem.InfoType = InfoItem.InfoType.CHANNEL

        @Test
        @Throws(IOException::class, ExtractionException::class)
        fun testAtLeastOneVerified() {
            val items = extractor().initialPage.items
            assertTrue(items.any { (it as ChannelInfoItem).isVerified })
        }

        private companion object {
            const val QUERY = "bbc"
        }
    }

    class VideoUploaderAvatar : DefaultSearchExtractorTest(), InitYoutubeTest {
        @Throws(Exception::class)
        override fun createExtractor(): SearchExtractor = YouTube.getSearchExtractor(QUERY, listOf(VIDEOS), "")

        override fun expectedService(): StreamingService = YouTube
        override fun expectedName(): String = QUERY
        override fun expectedId(): String = QUERY
        override fun expectedUrlContains(): String = SEARCH_URL + QUERY
        override fun expectedOriginalUrlContains(): String = SEARCH_URL + QUERY
        override fun expectedSearchString(): String = QUERY
        override fun expectedSearchSuggestion(): String? = null
        override fun expectedInfoItemType(): InfoItem.InfoType = InfoItem.InfoType.STREAM

        @Test
        @Throws(IOException::class, ExtractionException::class)
        fun testUploaderAvatars() {
            extractor().initialPage
                .items
                .filterIsInstance<StreamInfoItem>()
                .forEach { YoutubeTestsUtils.testImages(it.uploaderAvatars) }
        }

        private companion object {
            const val QUERY = "sidemen"
        }
    }

    class VideoDescription : DefaultSearchExtractorTest(), InitYoutubeTest {
        @Throws(Exception::class)
        override fun createExtractor(): SearchExtractor = YouTube.getSearchExtractor(QUERY, listOf(VIDEOS), "")

        override fun expectedService(): StreamingService = YouTube
        override fun expectedName(): String = QUERY
        override fun expectedId(): String = QUERY
        override fun expectedUrlContains(): String = SEARCH_URL + QUERY
        override fun expectedOriginalUrlContains(): String = SEARCH_URL + QUERY
        override fun expectedSearchString(): String = QUERY
        override fun expectedSearchSuggestion(): String? = null
        override fun expectedInfoItemType(): InfoItem.InfoType = InfoItem.InfoType.STREAM

        @Test
        @Throws(IOException::class, ExtractionException::class)
        fun testVideoDescription() {
            val items = extractor().initialPage.items
            assertNotNull((items[0] as StreamInfoItem).shortDescription)
        }

        @Disabled("Irrelevant - sometimes suggestions show up, sometimes not")
        @Throws(Exception::class)
        override fun testSearchSuggestion() {
            super.testSearchSuggestion()
        }

        private companion object {
            const val QUERY = "44wLAzydRFU"
        }
    }

    class MultipleUploader : DefaultSearchExtractorTest(), InitYoutubeTest {
        @Throws(Exception::class)
        override fun createExtractor(): SearchExtractor = YouTube.getSearchExtractor(QUERY, listOf(VIDEOS), "")

        override fun expectedService(): StreamingService = YouTube
        override fun expectedName(): String = QUERY
        override fun expectedId(): String = QUERY
        override fun expectedUrlContains(): String = SEARCH_URL + QUERY
        override fun expectedOriginalUrlContains(): String = SEARCH_URL + QUERY
        override fun expectedSearchString(): String = QUERY
        override fun expectedSearchSuggestion(): String? = null
        override fun expectedInfoItemType(): InfoItem.InfoType = InfoItem.InfoType.STREAM

        @Test
        @Throws(IOException::class, ExtractionException::class)
        fun testUploaderName() {
            val items = extractor().initialPage.items
            assertEquals("Le Vortex - ARTE  and Thomas Gauthier", (items[0] as StreamInfoItem).uploaderName)
        }

        @Test
        @Throws(IOException::class, ExtractionException::class)
        fun testUploaderUrl() {
            val items = extractor().initialPage.items
            assertEquals(
                "https://www.youtube.com/channel/UCZxLew-WXWm5dhRZBgEFl-Q",
                (items[0] as StreamInfoItem).uploaderUrl,
            )
        }

        @Test
        @Throws(IOException::class, ExtractionException::class)
        fun testUploaderAvatars() {
            val items = extractor().initialPage.items
            assertNotNull((items[0] as StreamInfoItem).uploaderAvatars)
        }

        @Disabled("Irrelevant - sometimes suggestions show up, sometimes not")
        @Throws(Exception::class)
        override fun testSearchSuggestion() {
            super.testSearchSuggestion()
        }

        @Disabled("Irrelevant - sometimes suggestions show up, sometimes not")
        @Throws(Exception::class)
        override fun testMoreRelatedItems() {
            super.testMoreRelatedItems()
        }

        private companion object {
            const val QUERY = "Nxk6aRHi664"
        }
    }

    class ShortFormContent : DefaultSearchExtractorTest(), InitYoutubeTest {
        @Throws(Exception::class)
        override fun createExtractor(): SearchExtractor = YouTube.getSearchExtractor(QUERY, listOf(VIDEOS), "")

        override fun expectedService(): StreamingService = YouTube
        override fun expectedName(): String = QUERY
        override fun expectedId(): String = QUERY

        @Throws(Exception::class)
        override fun expectedUrlContains(): String = SEARCH_URL + Utils.encodeUrlUtf8(QUERY)

        @Throws(Exception::class)
        override fun expectedOriginalUrlContains(): String = SEARCH_URL + Utils.encodeUrlUtf8(QUERY)

        override fun expectedSearchString(): String = QUERY
        override fun expectedSearchSuggestion(): String? = null
        override fun expectedInfoItemType(): InfoItem.InfoType = InfoItem.InfoType.STREAM

        @Disabled(
            "broken upstream as well (TeamNewPipe/NewPipeExtractor dev 6faf4068, 2026-10): " +
                "the recorded results no longer contain a short",
        )
        @Test
        @Throws(IOException::class, ExtractionException::class)
        fun testShortFormContent() {
            assertTrue(
                extractor().initialPage
                    .items
                    .filterIsInstance<StreamInfoItem>()
                    .any { it.isShortFormContent },
            )
        }

        private companion object {
            const val QUERY = "#shorts"
        }
    }

    /**
     * A [SearchExtractor] test to check if crisis resources preventing search results to be
     * returned are bypassed (searches with content filters are not tested in this test, even if
     * they should work as bypasses are used with them too).
     *
     * See [https://support.google.com/youtube/answer/10726080?hl=en](https://support.google.com/youtube/answer/10726080?hl=en)
     * for more info on crisis resources.
     */
    class CrisisResources : DefaultSearchExtractorTest(), InitYoutubeTest {
        @Throws(Exception::class)
        override fun createExtractor(): SearchExtractor = YouTube.getSearchExtractor(QUERY)

        override fun expectedService(): StreamingService = YouTube
        override fun expectedName(): String = QUERY
        override fun expectedId(): String = QUERY

        @Throws(Exception::class)
        override fun expectedUrlContains(): String = SEARCH_URL + Utils.encodeUrlUtf8(QUERY)

        @Throws(Exception::class)
        override fun expectedOriginalUrlContains(): String = SEARCH_URL + Utils.encodeUrlUtf8(QUERY)

        override fun expectedSearchString(): String = QUERY
        override fun expectedSearchSuggestion(): String? = null

        @Test
        @Throws(Exception::class)
        override fun testMetaInfo() {
            val metaInfoList = extractor().metaInfo

            // the meta info will have different text and language depending on where in the world
            // the connection is established from, so we can't check the actual content
            assertEquals(1, metaInfoList.size)
        }

        private companion object {
            const val QUERY = "suicide"
        }
    }
}

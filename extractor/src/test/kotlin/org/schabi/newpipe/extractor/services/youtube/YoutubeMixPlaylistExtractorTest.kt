package org.schabi.newpipe.extractor.services.youtube

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ExtractorAsserts
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonWriter
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.YOUTUBEI_V1_URL
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.prepareDesktopJsonBuilder
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeMixPlaylistExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class YoutubeMixPlaylistExtractorTest {

    abstract class Base :
        DefaultSimpleExtractorTest<YoutubeMixPlaylistExtractor>(),
        InitYoutubeTest {

        @BeforeAll
        override fun setUp() {
            super<InitYoutubeTest>.setUp()
            YoutubeParsingHelper.setConsentAccepted(true)
        }

        override fun createExtractor(): YoutubeMixPlaylistExtractor =
            YouTube.getPlaylistExtractor(extractorUrl()) as YoutubeMixPlaylistExtractor

        protected abstract fun extractorUrl(): String

        protected fun assertThumbnailsContain(videoId: String) {
            YoutubeTestsUtils.testImages(extractor().thumbnails)
            extractor().thumbnails.forEach { thumbnail ->
                ExtractorAsserts.assertContains(videoId, thumbnail.url)
            }
        }

        protected fun assertNameContainsMix(videoTitle: String) {
            val name = extractor().name
            ExtractorAsserts.assertContains("Mix", name)
            ExtractorAsserts.assertContains(videoTitle, name)
        }

        protected fun assertInitialPage() {
            val streams = extractor().initialPage
            assertFalse(streams.items.isEmpty())
            assertTrue(streams.hasNextPage())
        }

        /** Requests the page described by the given `next` request body fields. */
        protected fun assertPageOfBody(videoId: String, playlistId: String, playlistIndex: Int? = null) {
            val builder = prepareDesktopJsonBuilder(
                NewPipe.getPreferredLocalization(),
                NewPipe.getPreferredContentCountry()
            )
                .value("videoId", videoId)
                .value("playlistId", playlistId)
            if (playlistIndex != null) {
                builder.value("playlistIndex", playlistIndex)
            }
            val body = JsonWriter.string(builder.value("params", "OAE%3D").done()).toByteArray(Charsets.UTF_8)

            val streams = extractor().getPage(
                Page(
                    YOUTUBEI_V1_URL + "next?" + DISABLE_PRETTY_PRINT_PARAMETER,
                    null,
                    null,
                    DUMMY_COOKIE,
                    body
                )
            )
            assertFalse(streams.items.isEmpty())
            assertTrue(streams.hasNextPage())
        }

        protected fun assertContinuations() {
            var streams: InfoItemsPage<StreamInfoItem> = extractor().initialPage
            val urls = HashSet<String>()

            // Should work infinitely, but for testing purposes only 3 times
            for (i in 0 until 3) {
                assertTrue(streams.hasNextPage())
                assertFalse(streams.items.isEmpty())

                for (item in streams.items) {
                    // TODO Duplicates are appearing
                    // assertFalse(urls.contains(item.url))
                    urls.add(item.url)
                }

                streams = extractor().getPage(streams.nextPage)
            }
            assertTrue(streams.hasNextPage())
            assertFalse(streams.items.isEmpty())
        }
    }

    class Mix : Base() {
        override fun extractorUrl(): String = "https://www.youtube.com/watch?v=$VIDEO_ID&list=RD$VIDEO_ID"

        @Test
        fun getServiceId() {
            assertEquals(YouTube.serviceId, extractor().serviceId)
        }

        @Test
        fun getName() {
            assertNameContainsMix(VIDEO_TITLE)
        }

        @Test
        fun getThumbnails() {
            assertThumbnailsContain(VIDEO_ID)
        }

        @Test
        fun getInitialPage() {
            assertInitialPage()
        }

        @Test
        fun getPage() {
            assertPageOfBody(VIDEO_ID, "RD$VIDEO_ID")
        }

        @Test
        fun getContinuations() {
            assertContinuations()
        }

        @Test
        fun getStreamCount() {
            assertEquals(ListExtractor.ITEM_COUNT_INFINITE, extractor().streamCount)
        }

        @Test
        fun getPlaylistType() {
            assertEquals(PlaylistInfo.PlaylistType.MIX_STREAM, extractor().playlistType)
        }

        private companion object {
            const val VIDEO_ID = "FAqYW76GLPA"
            const val VIDEO_TITLE = "Mix – "
        }
    }

    class MixWithIndex : Base() {
        override fun extractorUrl(): String =
            "https://www.youtube.com/watch?v=$VIDEO_ID_AT_INDEX&list=RD$VIDEO_ID&index=$INDEX"

        @Test
        fun getName() {
            assertNameContainsMix(VIDEO_TITLE)
        }

        @Test
        fun getThumbnails() {
            assertThumbnailsContain(VIDEO_ID)
        }

        @Test
        fun getInitialPage() {
            assertInitialPage()
        }

        @Test
        fun getPage() {
            assertPageOfBody(VIDEO_ID, "RD$VIDEO_ID", INDEX)
        }

        @Test
        fun getContinuations() {
            assertContinuations()
        }

        @Test
        fun getStreamCount() {
            assertEquals(ListExtractor.ITEM_COUNT_INFINITE, extractor().streamCount)
        }

        @Test
        fun getPlaylistType() {
            assertEquals(PlaylistInfo.PlaylistType.MIX_STREAM, extractor().playlistType)
        }

        private companion object {
            const val VIDEO_ID = "FAqYW76GLPA"
            const val VIDEO_TITLE = "Mix – "
            const val INDEX = 7 // YT starts the index with 1...
            const val VIDEO_ID_AT_INDEX = "F90Cw4l-8NY"
        }
    }

    class MyMix : Base() {
        override fun extractorUrl(): String = "https://www.youtube.com/watch?v=$VIDEO_ID&list=RDMM$VIDEO_ID"

        @Test
        fun getServiceId() {
            assertEquals(YouTube.serviceId, extractor().serviceId)
        }

        @Test
        fun getName() {
            assertEquals("My Mix", extractor().name)
        }

        @Test
        fun getThumbnails() {
            assertThumbnailsContain(VIDEO_ID)
        }

        @Test
        fun getInitialPage() {
            assertInitialPage()
        }

        @Test
        fun getPage() {
            assertPageOfBody(VIDEO_ID, "RDMM$VIDEO_ID")
        }

        @Test
        fun getContinuations() {
            assertContinuations()
        }

        @Test
        fun getStreamCount() {
            assertEquals(ListExtractor.ITEM_COUNT_INFINITE, extractor().streamCount)
        }

        @Test
        fun getPlaylistType() {
            assertEquals(PlaylistInfo.PlaylistType.MIX_STREAM, extractor().playlistType)
        }

        private companion object {
            const val VIDEO_ID = "YVkUvmDQ3HY"
        }
    }

    class InvalidPageEmpty : Base() {
        override fun extractorUrl(): String = "https://www.youtube.com/watch?v=$VIDEO_ID&list=RD$VIDEO_ID"

        @Test
        fun getPageEmptyUrl() {
            assertThrows(IllegalArgumentException::class.java) { extractor().getPage(Page("")) }
        }

        private companion object {
            const val VIDEO_ID = "WRz2MxhAdJo"
        }
    }

    class InvalidVideoId : Base() {
        override fun extractorUrl(): String = "https://www.youtube.com/watch?v=abcde&list=RDabcde"

        override fun fetchExtractor(extractor: YoutubeMixPlaylistExtractor) {
            // Do nothing, done by test below
        }

        @Test
        fun invalidVideoId() {
            // Rejected when the link is parsed, before any request is sent to YouTube
            assertThrows(ParsingException::class.java) { YouTube.getPlaylistExtractor(extractorUrl()) }
        }
    }

    class GenreMix : Base() {
        override fun extractorUrl(): String =
            "https://www.youtube.com/watch?v=$VIDEO_ID&list=RDGMEMYH9CUrFO7CfLJpaD7UR85w"

        @Test
        fun getServiceId() {
            assertEquals(YouTube.serviceId, extractor().serviceId)
        }

        @Test
        fun getName() {
            assertEquals(MIX_TITLE, extractor().name)
        }

        @Test
        fun getThumbnails() {
            assertThumbnailsContain(VIDEO_ID)
        }

        @Test
        fun getInitialPage() {
            assertInitialPage()
        }

        @Test
        fun getPage() {
            assertPageOfBody(VIDEO_ID, "RD$VIDEO_ID")
        }

        @Test
        fun getContinuations() {
            assertContinuations()
        }

        @Test
        fun getStreamCount() {
            assertEquals(ListExtractor.ITEM_COUNT_INFINITE, extractor().streamCount)
        }

        @Test
        fun getPlaylistType() {
            assertEquals(PlaylistInfo.PlaylistType.MIX_GENRE, extractor().playlistType)
        }

        private companion object {
            const val VIDEO_ID = "kINJeTNFbpg"
            const val MIX_TITLE = "Mix – Electronic music"
        }
    }

    class Music : Base() {
        override fun extractorUrl(): String = "https://m.youtube.com/watch?v=$VIDEO_ID&list=RDAMVM$VIDEO_ID"

        @Test
        fun getServiceId() {
            assertEquals(YouTube.serviceId, extractor().serviceId)
        }

        @Test
        fun getName() {
            assertEquals(MIX_TITLE, extractor().name)
        }

        @Test
        fun getThumbnailUrl() {
            assertThumbnailsContain(VIDEO_ID)
        }

        @Test
        fun getInitialPage() {
            assertInitialPage()
        }

        @Test
        fun getPage() {
            assertPageOfBody(VIDEO_ID, "RD$VIDEO_ID")
        }

        @Disabled(
            "broken upstream as well (TeamNewPipe/NewPipeExtractor dev 6faf4068, 2026-10): the continuation " +
                "request was never recorded"
        )
        @Test
        fun getContinuations() {
            assertContinuations()
        }

        @Test
        fun getStreamCount() {
            assertEquals(ListExtractor.ITEM_COUNT_INFINITE, extractor().streamCount)
        }

        @Test
        fun getPlaylistType() {
            assertEquals(PlaylistInfo.PlaylistType.MIX_MUSIC, extractor().playlistType)
        }

        private companion object {
            const val VIDEO_ID = "dQw4w9WgXcQ"
            const val MIX_TITLE =
                "Mix – Rick Astley - Never Gonna Give You Up (Official Video) (4K Remaster)"
        }
    }

    private companion object {
        val DUMMY_COOKIE: Map<String, String> = mapOf(YoutubeMixPlaylistExtractor.COOKIE_NAME to "whatever")
    }
}

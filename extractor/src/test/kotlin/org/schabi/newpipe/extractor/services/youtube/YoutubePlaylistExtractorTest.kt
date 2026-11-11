package org.schabi.newpipe.extractor.services.youtube

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ExtractorAsserts
import org.schabi.newpipe.extractor.ExtractorAsserts.assertContains
import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.services.BasePlaylistExtractorTest
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest
import org.schabi.newpipe.extractor.services.DefaultTests.assertNoMoreItems
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestGetPageInNewExtractor
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestListOfItems
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestMoreItems
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestRelatedItems
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubePlaylistExtractor
import org.schabi.newpipe.extractor.stream.ContentAvailability
import org.schabi.newpipe.extractor.utils.Utils

/**
 * Test for [YoutubePlaylistExtractor]
 */
class YoutubePlaylistExtractorTest {

    class NotAvailable : InitYoutubeTest {

        @Test
        fun nonExistentFetch() {
            val extractor = YouTube.getPlaylistExtractor(
                "https://www.youtube.com/playlist?list=PL11111111111111111111111111111111"
            )
            assertThrows(ContentNotAvailableException::class.java) { extractor.fetchPage() }
        }

        @Test
        fun invalidId() {
            val extractor = YouTube.getPlaylistExtractor("https://www.youtube.com/playlist?list=INVALID_ID")
            assertThrows(ContentNotAvailableException::class.java) { extractor.fetchPage() }
        }
    }

    abstract class Base :
        DefaultSimpleExtractorTest<YoutubePlaylistExtractor>(),
        BasePlaylistExtractorTest,
        InitYoutubeTest {

        override fun createExtractor(): YoutubePlaylistExtractor =
            YouTube.getPlaylistExtractor(urlForExtraction()) as YoutubePlaylistExtractor

        protected abstract fun urlForExtraction(): String
    }

    class TimelessPopHits : Base() {
        override fun urlForExtraction(): String =
            "http://www.youtube.com/watch?v=lp-EO5I60KA&list=PLMC9KNkIncKtPzgY-5rmhvj7fax8fdxoj"

        @Test
        override fun testServiceId() {
            assertEquals(YouTube.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertTrue(extractor().name.startsWith("Pop Music Playlist"))
        }

        @Test
        override fun testId() {
            assertEquals("PLMC9KNkIncKtPzgY-5rmhvj7fax8fdxoj", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals(
                "https://www.youtube.com/playlist?list=PLMC9KNkIncKtPzgY-5rmhvj7fax8fdxoj",
                extractor().url
            )
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals(
                "http://www.youtube.com/watch?v=lp-EO5I60KA&list=PLMC9KNkIncKtPzgY-5rmhvj7fax8fdxoj",
                extractor().originalUrl
            )
        }

        @Test
        override fun testRelatedItems() {
            defaultTestRelatedItems(extractor())
        }

        @Test
        override fun testMoreRelatedItems() {
            defaultTestMoreItems(extractor())
        }

        @Test
        override fun testThumbnails() {
            YoutubeTestsUtils.testImages(extractor().thumbnails)
        }

        @Test
        override fun testBanners() {
            YoutubeTestsUtils.testImages(extractor().banners)
        }

        @Test
        fun testUploaderUrl() {
            assertEquals("https://www.youtube.com/channel/UCs72iRpTEuwV3y6pdWYLgiw", extractor().uploaderUrl)
        }

        @Test
        override fun testUploaderName() {
            ExtractorAsserts.assertContains("Just Hits", extractor().uploaderName)
        }

        @Test
        override fun testUploaderAvatars() {
            YoutubeTestsUtils.testImages(extractor().uploaderAvatars)
        }

        @Test
        override fun testStreamCount() {
            ExtractorAsserts.assertGreater(100, extractor().streamCount)
        }

        @Test
        override fun testUploaderVerified() {
            assertFalse(extractor().isUploaderVerified)
        }

        @Test
        fun getPlaylistType() {
            assertEquals(PlaylistInfo.PlaylistType.NORMAL, extractor().playlistType)
        }

        @Test
        fun testDescription() {
            assertContains("pop songs list", extractor().description.content)
        }
    }

    class HugePlaylist : Base() {
        override fun urlForExtraction(): String =
            "https://www.youtube.com/watch?v=8SbUC-UaAxE&list=PLWwAypAcFRgKAIIFqBr9oy-ZYZnixa_Fj"

        @Test
        fun testGetPageInNewExtractor() {
            val newExtractor = YouTube.getPlaylistExtractor(extractor().url)
            defaultTestGetPageInNewExtractor(extractor(), newExtractor)
        }

        @Test
        override fun testServiceId() {
            assertEquals(YouTube.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertEquals(
                "I Wanna Rock Super Gigantic Playlist 1: Hardrock, AOR, Metal and more !!! 5000 music videos !!!",
                extractor().name
            )
        }

        @Test
        override fun testId() {
            assertEquals("PLWwAypAcFRgKAIIFqBr9oy-ZYZnixa_Fj", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals(
                "https://www.youtube.com/playlist?list=PLWwAypAcFRgKAIIFqBr9oy-ZYZnixa_Fj",
                extractor().url
            )
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals(
                "https://www.youtube.com/watch?v=8SbUC-UaAxE&list=PLWwAypAcFRgKAIIFqBr9oy-ZYZnixa_Fj",
                extractor().originalUrl
            )
        }

        @Test
        override fun testRelatedItems() {
            defaultTestRelatedItems(extractor())
        }

        @Test
        override fun testMoreRelatedItems() {
            var currentPage = defaultTestMoreItems(extractor())

            // test for 2 more levels
            for (i in 0 until 2) {
                currentPage = extractor().getPage(currentPage.nextPage)
                defaultTestListOfItems(YouTube, currentPage.items, currentPage.errors)
            }
        }

        @Test
        override fun testThumbnails() {
            YoutubeTestsUtils.testImages(extractor().thumbnails)
        }

        @Test
        override fun testBanners() {
            YoutubeTestsUtils.testImages(extractor().banners)
        }

        @Test
        fun testUploaderUrl() {
            assertEquals("https://www.youtube.com/channel/UCHSPWoY1J5fbDVbcnyeqwdw", extractor().uploaderUrl)
        }

        @Test
        override fun testUploaderName() {
            assertEquals("Tomas Nilsson TOMPA571", extractor().uploaderName)
        }

        @Test
        override fun testUploaderAvatars() {
            YoutubeTestsUtils.testImages(extractor().uploaderAvatars)
        }

        @Test
        override fun testStreamCount() {
            ExtractorAsserts.assertGreater(100, extractor().streamCount)
        }

        @Test
        override fun testUploaderVerified() {
            assertFalse(extractor().isUploaderVerified)
        }

        @Test
        fun getPlaylistType() {
            assertEquals(PlaylistInfo.PlaylistType.NORMAL, extractor().playlistType)
        }

        @Test
        fun testDescription() {
            assertContains("I Wanna Rock Super Gigantic Playlist", extractor().description.content)
        }
    }

    class LearningPlaylist : Base() {
        override fun urlForExtraction(): String =
            "https://www.youtube.com/playlist?list=PL8dPuuaLjXtOAKed_MxxWBNaPno5h3Zs8"

        @Test
        override fun testServiceId() {
            assertEquals(YouTube.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertTrue(extractor().name.startsWith("Anatomy & Physiology"))
        }

        @Test
        override fun testId() {
            assertEquals("PL8dPuuaLjXtOAKed_MxxWBNaPno5h3Zs8", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals(
                "https://www.youtube.com/playlist?list=PL8dPuuaLjXtOAKed_MxxWBNaPno5h3Zs8",
                extractor().url
            )
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals(
                "https://www.youtube.com/playlist?list=PL8dPuuaLjXtOAKed_MxxWBNaPno5h3Zs8",
                extractor().originalUrl
            )
        }

        @Test
        override fun testRelatedItems() {
            defaultTestRelatedItems(extractor())
        }

        @Test
        override fun testMoreRelatedItems() {
            assertFalse(extractor().initialPage.hasNextPage())
        }

        @Test
        override fun testThumbnails() {
            YoutubeTestsUtils.testImages(extractor().thumbnails)
        }

        @Test
        override fun testBanners() {
            YoutubeTestsUtils.testImages(extractor().banners)
        }

        @Test
        fun testUploaderUrl() {
            assertEquals("https://www.youtube.com/channel/UCX6b17PVsYBQ0ip5gyeme-Q", extractor().uploaderUrl)
        }

        @Test
        override fun testUploaderName() {
            ExtractorAsserts.assertContains("CrashCourse", extractor().uploaderName)
        }

        @Test
        override fun testUploaderAvatars() {
            YoutubeTestsUtils.testImages(extractor().uploaderAvatars)
        }

        @Test
        override fun testStreamCount() {
            ExtractorAsserts.assertGreater(40, extractor().streamCount)
        }

        @Test
        override fun testUploaderVerified() {
            assertFalse(extractor().isUploaderVerified)
        }

        @Test
        fun getPlaylistType() {
            assertEquals(PlaylistInfo.PlaylistType.NORMAL, extractor().playlistType)
        }

        @Test
        fun testDescription() {
            assertContains("47 episodes", extractor().description.content)
        }
    }

    class ShortsUI : Base() {
        override fun urlForExtraction(): String =
            "https://www.youtube.com/playlist?list=UUSHBR8-60-B28hp2BmDPdntcQ"

        @Test
        override fun testServiceId() {
            assertEquals(YouTube.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertEquals("Short videos", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("UUSHBR8-60-B28hp2BmDPdntcQ", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals(
                "https://www.youtube.com/playlist?list=UUSHBR8-60-B28hp2BmDPdntcQ",
                extractor().url
            )
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals(
                "https://www.youtube.com/playlist?list=UUSHBR8-60-B28hp2BmDPdntcQ",
                extractor().originalUrl
            )
        }

        @Test
        override fun testRelatedItems() {
            defaultTestRelatedItems(extractor())
        }

        @Test
        override fun testMoreRelatedItems() {
            defaultTestMoreItems(extractor())
        }

        @Test
        override fun testThumbnails() {
            YoutubeTestsUtils.testImages(extractor().thumbnails)
        }

        @Test
        override fun testBanners() {
            YoutubeTestsUtils.testImages(extractor().banners)
        }

        @Test
        override fun testUploaderName() {
            assertEquals("YouTube", extractor().uploaderName)
        }

        @Test
        override fun testUploaderAvatars() {
            YoutubeTestsUtils.testImages(extractor().uploaderAvatars)
        }

        @Test
        override fun testStreamCount() {
            ExtractorAsserts.assertGreater(250, extractor().streamCount)
        }

        @Test
        override fun testUploaderVerified() {
            // YouTube doesn't provide this information for playlists
            assertFalse(extractor().isUploaderVerified)
        }

        @Test
        fun getPlaylistType() {
            assertEquals(PlaylistInfo.PlaylistType.NORMAL, extractor().playlistType)
        }

        @Test
        fun testDescription() {
            assertTrue(Utils.isNullOrEmpty(extractor().description.content))
        }
    }

    class ContinuationsTests : InitYoutubeTest {
        @Test
        fun testNoContinuations() {
            val extractor = YouTube.getPlaylistExtractor(
                "https://www.youtube.com/playlist?list=PLXJg25X-OulsVsnvZ7RVtSDW-id9_RzAO"
            ) as YoutubePlaylistExtractor
            extractor.fetchPage()
            assertNoMoreItems(extractor)
        }

        @Test
        fun testOnlySingleContinuation() {
            val extractor = YouTube.getPlaylistExtractor(
                "https://www.youtube.com/playlist?list=PLoumn5BIsUDeGF1vy5Nylf_RJKn5aL_nr"
            ) as YoutubePlaylistExtractor
            extractor.fetchPage()
            val page = defaultTestMoreItems(extractor)
            assertFalse(page.hasNextPage(), "More items available when it shouldn't")
        }
    }

    class MembersOnlyTests : InitYoutubeTest {
        @Test
        fun testOnlyMembersOnlyVideos() {
            val extractor = YouTube.getPlaylistExtractor(
                // autogenerated playlist with only membersOnly videos
                "https://www.youtube.com/playlist?list=UUMOQuLXlFNAeDJMSmuzHU5axw"
            ) as YoutubePlaylistExtractor
            extractor.fetchPage()
            val allItems = extractor.initialPage.items
            val membershipVideos = allItems.filter { item ->
                item.contentAvailability != ContentAvailability.MEMBERSHIP
            }
            assertFalse(allItems.isEmpty())
            assertTrue(membershipVideos.isEmpty())
        }
    }

    class CoursePlaylistTest : InitYoutubeTest {
        @Test
        fun uploaderName() {
            val extractor = YouTube.getPlaylistExtractor(
                "https://www.youtube.com/playlist?list=PLWxziGKTUvQFIsbbFcTZz7jOT4TMGnZBh"
            ) as YoutubePlaylistExtractor
            extractor.fetchPage()
            val allItems = extractor.initialPage.items
            assertEquals(14, allItems.size)
            assertEquals(extractor.uploaderName, allItems[0].uploaderName)
        }
    }
}

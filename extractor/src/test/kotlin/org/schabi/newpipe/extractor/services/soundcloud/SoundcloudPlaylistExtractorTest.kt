package org.schabi.newpipe.extractor.services.soundcloud

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ExtractorAsserts
import org.schabi.newpipe.extractor.ExtractorAsserts.assertEmpty
import org.schabi.newpipe.extractor.ExtractorAsserts.assertIsSecureUrl
import org.schabi.newpipe.extractor.ServiceList.SoundCloud
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor
import org.schabi.newpipe.extractor.services.BasePlaylistExtractorTest
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestGetPageInNewExtractor
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestImageCollection
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestListOfItems
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestMoreItems
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestRelatedItems

/**
 * Test for [PlaylistExtractor]
 */
class SoundcloudPlaylistExtractorTest {
    abstract class Base :
        DefaultSimpleExtractorTest<PlaylistExtractor>(),
        BasePlaylistExtractorTest {

        override fun createExtractor(): PlaylistExtractor = SoundCloud.getPlaylistExtractor(extractorUrl())

        protected abstract fun extractorUrl(): String
    }

    class LuvTape : Base() {
        override fun extractorUrl(): String = "https://soundcloud.com/liluzivert/sets/the-perfect-luv-tape-r?test=123"

        @Test
        override fun testServiceId() {
            assertEquals(SoundCloud.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertEquals("THE PERFECT LUV TAPE®️", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("246349810", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://soundcloud.com/liluzivert/sets/the-perfect-luv-tape-r", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals(
                "https://soundcloud.com/liluzivert/sets/the-perfect-luv-tape-r?test=123",
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
            defaultTestImageCollection(extractor().thumbnails)
        }

        @Test
        override fun testBanners() {
            // SoundCloud playlists do not have a banner
            assertEmpty(extractor().banners)
        }

        @Test
        fun testUploaderUrl() {
            val uploaderUrl = extractor().uploaderUrl
            assertIsSecureUrl(uploaderUrl)
            ExtractorAsserts.assertContains("liluzivert", uploaderUrl)
        }

        @Test
        override fun testUploaderName() {
            assertTrue(extractor().uploaderName.contains("Lil Uzi Vert"))
        }

        @Test
        override fun testUploaderAvatars() {
            defaultTestImageCollection(extractor().uploaderAvatars)
        }

        @Test
        override fun testStreamCount() {
            ExtractorAsserts.assertGreaterOrEqual(10, extractor().streamCount)
        }

        @Test
        override fun testUploaderVerified() {
            assertTrue(extractor().isUploaderVerified)
        }
    }

    class RandomHouseMusic : Base() {
        override fun extractorUrl(): String = "https://soundcloud.com/micky96/sets/house"

        @Test
        override fun testServiceId() {
            assertEquals(SoundCloud.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertEquals("House", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("123062856", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://soundcloud.com/micky96/sets/house", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals("https://soundcloud.com/micky96/sets/house", extractor().originalUrl)
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
            defaultTestImageCollection(extractor().thumbnails)
        }

        @Test
        override fun testBanners() {
            // SoundCloud playlists do not have a banner
            assertEmpty(extractor().banners)
        }

        @Test
        fun testUploaderUrl() {
            val uploaderUrl = extractor().uploaderUrl
            assertIsSecureUrl(uploaderUrl)
            ExtractorAsserts.assertContains("micky96", uploaderUrl)
        }

        @Test
        override fun testUploaderName() {
            assertEquals("_mickyyy", extractor().uploaderName)
        }

        @Test
        override fun testUploaderAvatars() {
            defaultTestImageCollection(extractor().uploaderAvatars)
        }

        @Test
        override fun testStreamCount() {
            ExtractorAsserts.assertGreaterOrEqual(10, extractor().streamCount)
        }

        @Test
        override fun testUploaderVerified() {
            assertFalse(extractor().isUploaderVerified)
        }
    }

    class EDMxxx : Base() {
        override fun extractorUrl(): String = "https://soundcloud.com/user350509423/sets/edm-xxx"

        @Test
        fun testGetPageInNewExtractor() {
            val newExtractor = SoundCloud.getPlaylistExtractor(extractor().url)
            defaultTestGetPageInNewExtractor(extractor(), newExtractor)
        }

        @Test
        override fun testServiceId() {
            assertEquals(SoundCloud.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertEquals("EDM xXx", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("136000376", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://soundcloud.com/user350509423/sets/edm-xxx", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals("https://soundcloud.com/user350509423/sets/edm-xxx", extractor().originalUrl)
        }

        @Test
        override fun testRelatedItems() {
            defaultTestRelatedItems(extractor())
        }

        @Test
        override fun testMoreRelatedItems() {
            var currentPage = defaultTestMoreItems(extractor())
            // Test for 2 more levels
            for (i in 0 until 2) {
                currentPage = extractor().getPage(currentPage.nextPage)
                defaultTestListOfItems(SoundCloud, currentPage.items, currentPage.errors)
            }
        }

        @Test
        override fun testThumbnails() {
            defaultTestImageCollection(extractor().thumbnails)
        }

        @Test
        override fun testBanners() {
            // SoundCloud playlists do not have a banner
            assertEmpty(extractor().banners)
        }

        @Test
        fun testUploaderUrl() {
            val uploaderUrl = extractor().uploaderUrl
            assertIsSecureUrl(uploaderUrl)
            ExtractorAsserts.assertContains("user350509423", uploaderUrl)
        }

        @Test
        override fun testUploaderName() {
            assertEquals("Chaazyy", extractor().uploaderName)
        }

        @Test
        override fun testUploaderAvatars() {
            defaultTestImageCollection(extractor().uploaderAvatars)
        }

        @Test
        override fun testStreamCount() {
            ExtractorAsserts.assertGreaterOrEqual(370, extractor().streamCount)
        }

        @Test
        override fun testUploaderVerified() {
            assertFalse(extractor().isUploaderVerified)
        }
    }

    class SmallPlaylist : Base() {
        override fun extractorUrl(): String = "https://soundcloud.com/breezy-123/sets/empty-playlist?test=123"

        @Test
        override fun testServiceId() {
            assertEquals(SoundCloud.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertEquals("EMPTY PLAYLIST", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("23483459", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://soundcloud.com/breezy-123/sets/empty-playlist", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals(
                "https://soundcloud.com/breezy-123/sets/empty-playlist?test=123",
                extractor().originalUrl
            )
        }

        @Test
        override fun testRelatedItems() {
            defaultTestRelatedItems(extractor())
        }

        @Test
        @Disabled("Test broken? Playlist has 2 entries, each page has 1 entry meaning it has 2 pages.")
        override fun testMoreRelatedItems() {
            try {
                defaultTestMoreItems(extractor())
            } catch (@Suppress("TooGenericExceptionCaught") ignored: Throwable) {
                return
            }

            fail<Unit>("This playlist doesn't have more items, it should throw an error")
        }

        @Test
        override fun testThumbnails() {
            defaultTestImageCollection(extractor().thumbnails)
        }

        @Test
        override fun testBanners() {
            // SoundCloud playlists do not have a banner
            assertEmpty(extractor().banners)
        }

        @Test
        fun testUploaderUrl() {
            val uploaderUrl = extractor().uploaderUrl
            assertIsSecureUrl(uploaderUrl)
            ExtractorAsserts.assertContains("breezy-123", uploaderUrl)
        }

        @Test
        override fun testUploaderName() {
            assertEquals("breezy-123", extractor().uploaderName)
        }

        @Test
        override fun testUploaderAvatars() {
            defaultTestImageCollection(extractor().uploaderAvatars)
        }

        @Test
        override fun testStreamCount() {
            assertEquals(2, extractor().streamCount)
        }

        @Test
        override fun testUploaderVerified() {
            assertFalse(extractor().isUploaderVerified)
        }
    }
}

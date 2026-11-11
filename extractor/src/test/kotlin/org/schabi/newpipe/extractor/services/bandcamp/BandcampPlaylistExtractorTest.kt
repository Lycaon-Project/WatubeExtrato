// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

package org.schabi.newpipe.extractor.services.bandcamp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.schabi.newpipe.extractor.ExtractorAsserts.assertContains
import org.schabi.newpipe.extractor.ExtractorAsserts.assertContainsOnlyEquivalentImages
import org.schabi.newpipe.extractor.ExtractorAsserts.assertEmpty
import org.schabi.newpipe.extractor.ExtractorAsserts.assertNotOnlyContainsEquivalentImages
import org.schabi.newpipe.extractor.InitNewPipeTest
import org.schabi.newpipe.extractor.ServiceList.Bandcamp
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor
import org.schabi.newpipe.extractor.services.BasePlaylistExtractorTest
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest
import org.schabi.newpipe.extractor.stream.Description

/**
 * Tests for [org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampPlaylistExtractor]
 */
class BandcampPlaylistExtractorTest {

    private fun init(useCase: String) {
        InitNewPipeTest.initNewPipe(javaClass, useCase)
    }

    /**
     * Test whether playlists contain the correct amount of items
     */
    @Test
    fun testCount() {
        init("count")
        val extractor = Bandcamp.getPlaylistExtractor("https://macbenson.bandcamp.com/album/coming-of-age")
        extractor.fetchPage()

        assertEquals(5, extractor.streamCount)
    }

    /**
     * Tests whether different stream thumbnails (track covers) get loaded correctly
     */
    @Test
    fun testDifferentTrackCovers() {
        init("differentTrackCovers")
        val extractor = Bandcamp.getPlaylistExtractor(
            "https://zachbensonarchive.bandcamp.com/album/results-of-boredom"
        )
        extractor.fetchPage()

        val l = extractor.initialPage.items
        assertContainsOnlyEquivalentImages(extractor.thumbnails, l[0].thumbnails)
        assertNotOnlyContainsEquivalentImages(extractor.thumbnails, l[5].thumbnails)
    }

    /**
     * Tests that no attempt to load every track's cover individually is made
     */
    @Test
    @Timeout(10)
    fun testDifferentTrackCoversDuration() {
        init("differentTrackCoversDuration")
        val extractor = Bandcamp.getPlaylistExtractor(
            "https://infiniteammo.bandcamp.com/album/night-in-the-woods-vol-1-at-the-end-of-everything"
        )
        extractor.fetchPage()

        /* All tracks on this album have the same cover art, but I don't know any albums with more
         * than 10 tracks that has at least one track with a cover art different from the rest.
         */
        val l = extractor.initialPage.items
        assertContainsOnlyEquivalentImages(extractor.thumbnails, l[0].thumbnails)
        assertContainsOnlyEquivalentImages(extractor.thumbnails, l[5].thumbnails)
    }

    /**
     * Test playlists with locked content
     */
    @Test
    fun testLockedContent() {
        init("lockedContent")
        val extractor = Bandcamp.getPlaylistExtractor("https://billwurtz.bandcamp.com/album/high-enough")

        assertThrows(ContentNotAvailableException::class.java) { extractor.fetchPage() }
    }

    /**
     * Test playlist with just one track
     */
    @Test
    fun testSingleStreamPlaylist() {
        init("singleStreamPlaylist")
        val extractor = Bandcamp.getPlaylistExtractor("https://zachjohnson1.bandcamp.com/album/endless")
        extractor.fetchPage()

        assertEquals(1, extractor.streamCount)
    }

    class ComingOfAge :
        DefaultSimpleExtractorTest<PlaylistExtractor>(),
        BasePlaylistExtractorTest {

        override fun createExtractor(): PlaylistExtractor =
            Bandcamp.getPlaylistExtractor("https://macbenson.bandcamp.com/album/coming-of-age")

        @Test
        override fun testThumbnails() {
            BandcampTestUtils.testImages(extractor().thumbnails)
        }

        @Test
        override fun testBanners() {
            assertEmpty(extractor().banners)
        }

        @Test
        fun testUploaderUrl() {
            assertTrue(extractor().uploaderUrl.contains("macbenson.bandcamp.com"))
        }

        @Test
        override fun testUploaderName() {
            assertEquals("mac benson", extractor().uploaderName)
        }

        @Test
        override fun testUploaderAvatars() {
            BandcampTestUtils.testImages(extractor().uploaderAvatars)
        }

        @Test
        override fun testStreamCount() {
            assertEquals(5, extractor().streamCount)
        }

        @Test
        fun testDescription() {
            val description = extractor().description
            assertNotEquals(Description.EMPTY_DESCRIPTION, description)
            assertContains("Artwork by Shona Radcliffe", description.content) // about
            assertContains(
                "All tracks written, produced and recorded by Mac Benson",
                description.content
            ) // credits
            assertContains("all rights reserved", description.content) // license
        }

        @Test
        override fun testUploaderVerified() {
            assertFalse(extractor().isUploaderVerified)
        }

        @Test
        fun testInitialPage() {
            assertNotNull(extractor().initialPage.items[0])
        }

        @Test
        override fun testServiceId() {
            assertEquals(Bandcamp.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertEquals("Coming of Age", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("https://macbenson.bandcamp.com/album/coming-of-age", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://macbenson.bandcamp.com/album/coming-of-age", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals("https://macbenson.bandcamp.com/album/coming-of-age", extractor().originalUrl)
        }

        @Test
        fun testNextPageUrl() {
            // getPage is non-null in the Kotlin API: there is only one page, so the page is empty
            val nextPage = extractor().getPage(extractor().initialPage.nextPage)
            assertTrue(nextPage.items.isEmpty())
            assertFalse(nextPage.hasNextPage())
        }

        @Test
        override fun testRelatedItems() {
            // DefaultTests.defaultTestRelatedItems(extractor);
            // Would fail because BandcampPlaylistStreamInfoItemExtractor.uploaderName returns an empty String
        }

        @Test
        override fun testMoreRelatedItems() {
        }
    }
}

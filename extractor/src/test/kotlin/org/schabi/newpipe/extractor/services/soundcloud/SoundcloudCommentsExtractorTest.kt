package org.schabi.newpipe.extractor.services.soundcloud

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList.SoundCloud
import org.schabi.newpipe.extractor.comments.CommentsExtractor
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest

// This track is known to reproduce issue #1243: it has comments, but when pagination
// exhausts the pages the API returns next_href=null, which previously caused a crash.
private const val TRACK_WITH_COMMENTS_URL = "https://soundcloud.com/user-722618400/a-real-playa"
private const val TRACK_WITH_NO_COMMENTS_URL = "https://soundcloud.com/user285130010/jdkskls"

class SoundcloudCommentsExtractorTest {

    /**
     * Regression test for [issue #1243](https://github.com/TeamNewPipe/NewPipeExtractor/issues/1243):
     * when the SoundCloud API returns `"next_href": null` (no more pages),
     * a subsequent call to [CommentsExtractor.getPage] with a null URL must not throw
     * [IllegalArgumentException] ("Page doesn't contain an URL"). Instead the extractor
     * must return [InfoItemsPage.emptyPage].
     *
     * The crash manifests during pagination: the last page of comments stores
     * `new Page(null)` as the next page, and when Paging 3 tries to fetch it the
     * exception propagates and kills the app.
     */
    @Nested
    inner class TrackWithComments : DefaultSimpleExtractorTest<CommentsExtractor>() {

        override fun createExtractor(): CommentsExtractor =
            SoundCloud.getCommentsExtractor(TRACK_WITH_COMMENTS_URL)!!

        /**
         * The initial page must load successfully without throwing any exception.
         */
        @Test
        fun testGetInitialPageSucceeds() {
            val page = extractor().initialPage
            // The track has comments; we only assert the call itself does not throw
            // and that the result is a valid (non-null) page.
            assertTrue(page.errors.isEmpty(), "Expected no extractor errors on initial page")
        }

        /**
         * Regression test for issue #1243: calling [CommentsExtractor.getPage] with a
         * [Page] whose URL is null (which is what gets stored when `next_href` is
         * absent in the API response) must return [InfoItemsPage.emptyPage] rather than
         * throw [IllegalArgumentException].
         */
        @Test
        fun testGetPageWithNullUrlReturnsEmptyPage() {
            val page = extractor().getPage(Page(null as String?))
            assertTrue(page.items.isEmpty(), "Expected empty items when page URL is null")
            assertFalse(page.hasNextPage(), "Expected no next page when page URL is null")
        }
    }

    /**
     * Tests a SoundCloud track that has no comments.
     *
     * Verifies that the extractor handles an empty collection gracefully:
     * the initial page must load without error, return no items, and have no next page.
     */
    @Nested
    inner class TrackWithNoComments : DefaultSimpleExtractorTest<CommentsExtractor>() {

        override fun createExtractor(): CommentsExtractor =
            SoundCloud.getCommentsExtractor(TRACK_WITH_NO_COMMENTS_URL)!!

        /**
         * The initial page must load successfully, return an empty items list,
         * and report no next page.
         */
        @Test
        fun testGetInitialPageIsEmpty() {
            val page = extractor().initialPage
            assertTrue(page.errors.isEmpty(), "Expected no extractor errors on initial page")
            assertTrue(page.items.isEmpty(), "Expected no comments for a track with no comments")
            assertFalse(page.hasNextPage(), "Expected no next page for a track with no comments")
        }
    }
}

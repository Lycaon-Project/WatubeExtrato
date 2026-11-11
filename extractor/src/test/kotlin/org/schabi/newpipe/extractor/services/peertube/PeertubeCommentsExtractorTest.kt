package org.schabi.newpipe.extractor.services.peertube

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage
import org.schabi.newpipe.extractor.ServiceList.PeerTube
import org.schabi.newpipe.extractor.comments.CommentsInfo
import org.schabi.newpipe.extractor.comments.CommentsInfoItem
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestImageCollection
import org.schabi.newpipe.extractor.services.peertube.extractors.PeertubeCommentsExtractor
import org.schabi.newpipe.extractor.utils.Utils

class PeertubeCommentsExtractorTest {

    class Default : DefaultSimpleExtractorTest<PeertubeCommentsExtractor>() {

        override fun createExtractor(): PeertubeCommentsExtractor =
            PeerTube.getCommentsExtractor("https://framatube.org/w/kkGMgK9ZtnKfYAgnEtQxbv")
                as PeertubeCommentsExtractor

        @Test
        fun testGetComments() {
            val comment = "I love this"
            var comments = extractor().initialPage
            var result = findInComments(comments, comment)

            while (comments.hasNextPage() && !result) {
                comments = extractor().getPage(comments.nextPage)
                result = findInComments(comments, comment)
            }

            assertTrue(result)
        }

        @Test
        fun testGetCommentsFromCommentsInfo() {
            val comment = "I love this. ❤"

            val commentsInfo = CommentsInfo.getInfo("https://framatube.org/w/kkGMgK9ZtnKfYAgnEtQxbv")
            assertNotNull(commentsInfo)
            checkNotNull(commentsInfo)
            assertEquals("Comments", commentsInfo.name)

            var result = findInComments(commentsInfo.relatedItems, comment)

            var nextPage = commentsInfo.nextPage
            var moreItems = InfoItemsPage<CommentsInfoItem>(null, nextPage, null)
            while (moreItems.hasNextPage() && !result) {
                moreItems = CommentsInfo.getMoreItems(PeerTube, commentsInfo, checkNotNull(nextPage))
                result = findInComments(moreItems.items, comment)
                nextPage = moreItems.nextPage
            }

            assertTrue(result)
        }

        @Test
        fun testGetCommentsAllData() {
            extractor().initialPage
                .items
                .forEach { commentsInfoItem ->
                    assertFalse(Utils.isBlank(commentsInfoItem.uploaderUrl))
                    assertFalse(Utils.isBlank(commentsInfoItem.uploaderName))
                    defaultTestImageCollection(commentsInfoItem.uploaderAvatars)
                    assertFalse(Utils.isBlank(commentsInfoItem.commentId))
                    assertFalse(Utils.isBlank(commentsInfoItem.commentText.content))
                    assertFalse(Utils.isBlank(commentsInfoItem.name))
                    assertFalse(Utils.isBlank(commentsInfoItem.textualUploadDate))
                    defaultTestImageCollection(commentsInfoItem.thumbnails)
                    assertFalse(Utils.isBlank(commentsInfoItem.url))
                    assertEquals(-1, commentsInfoItem.likeCount)
                    assertTrue(Utils.isBlank(commentsInfoItem.textualLikeCount))
                }
        }

        private fun findInComments(comments: InfoItemsPage<CommentsInfoItem>, comment: String): Boolean =
            findInComments(comments.items, comment)

        private fun findInComments(comments: List<CommentsInfoItem>, comment: String): Boolean =
            comments.any { it.commentText.content.contains(comment) }
    }

    class DeletedComments : DefaultSimpleExtractorTest<PeertubeCommentsExtractor>() {

        override fun createExtractor(): PeertubeCommentsExtractor =
            PeerTube.getCommentsExtractor("https://framatube.org/videos/watch/217eefeb-883d-45be-b7fc-a788ad8507d3")
                as PeertubeCommentsExtractor

        @Test
        fun testGetComments() {
            val comments = extractor().initialPage
            assertTrue(comments.errors.isEmpty())
        }

        @Test
        fun testGetCommentsFromCommentsInfo() {
            val commentsInfo = CommentsInfo.getInfo("https://framatube.org/videos/watch/217eefeb-883d-45be-b7fc-a788ad8507d3")
            assertNotNull(commentsInfo)
            assertTrue(checkNotNull(commentsInfo).errors.isEmpty())
        }
    }

    /**
     * Test a video that has comments with nested replies.
     */
    class NestedComments : DefaultSimpleExtractorTest<PeertubeCommentsExtractor>() {

        private var comments: InfoItemsPage<CommentsInfoItem>? = null

        override fun createExtractor(): PeertubeCommentsExtractor =
            PeerTube.getCommentsExtractor("https://framatube.org/w/kkGMgK9ZtnKfYAgnEtQxbv")
                as PeertubeCommentsExtractor

        override fun fetchExtractor(extractor: PeertubeCommentsExtractor) {
            comments = extractor.initialPage
        }

        protected fun comments(): InfoItemsPage<CommentsInfoItem> {
            if (comments == null) {
                extractor() // Initialize extractor to also init Comments
            }
            return checkNotNull(comments)
        }

        @Test
        fun testGetComments() {
            assertFalse(comments().items.isEmpty())

            val nestedCommentHead = findCommentWithId("34293", comments().items)
            assertNotNull(nestedCommentHead)

            assertTrue(
                findNestedCommentWithId("34294", checkNotNull(nestedCommentHead)),
                "The nested comment replies were not found"
            )
        }

        @Test
        fun testHasCreatorReply() {
            // NOTE: There is currently no creator replied comment
            assertCreatorReply("34293", false)
        }

        private fun assertCreatorReply(id: String, expected: Boolean) {
            val comment = findCommentWithId(id, comments().items)
            assertNotNull(comment)
            assertEquals(expected, checkNotNull(comment).hasCreatorReply())
        }
    }

    companion object {
        private fun findCommentWithId(id: String, comments: List<CommentsInfoItem>): CommentsInfoItem? =
            comments.firstOrNull { it.commentId == id }

        private fun findNestedCommentWithId(id: String, comment: CommentsInfoItem): Boolean {
            if (comment.commentId == id) {
                return true
            }

            return checkNotNull(PeerTube.getCommentsExtractor(comment.url))
                .getPage(comment.replies)
                .items
                .map { c ->
                    try {
                        findNestedCommentWithId(id, c)
                    } catch (ignored: Exception) {
                        false
                    }
                }
                .fold(false) { a, b -> a || b }
        }
    }
}

package org.schabi.newpipe.extractor.services.youtube

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ExtractorAsserts.assertContains
import org.schabi.newpipe.extractor.ExtractorAsserts.assertGreater
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage
import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.comments.CommentsInfo
import org.schabi.newpipe.extractor.comments.CommentsInfoItem
import org.schabi.newpipe.extractor.comments.CommentsInfoItem.Companion.UNKNOWN_REPLY_COUNT
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest
import org.schabi.newpipe.extractor.services.DefaultTests
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeCommentsExtractor
import org.schabi.newpipe.extractor.utils.Utils
import java.util.Locale

class YoutubeCommentsExtractorTest {

    abstract class Base :
        DefaultSimpleExtractorTest<YoutubeCommentsExtractor>(),
        InitYoutubeTest {

        override fun createExtractor(): YoutubeCommentsExtractor =
            YouTube.getCommentsExtractor(extractorUrl()) as YoutubeCommentsExtractor

        protected abstract fun extractorUrl(): String
    }

    /**
     * Test a "normal" YouTube
     */
    class Thomas : Base() {
        override fun extractorUrl(): String = URL

        @Test
        fun testGetComments() {
            assertTrue(getCommentsHelper(extractor()))
        }

        private fun getCommentsHelper(extractor: YoutubeCommentsExtractor): Boolean {
            var comments = extractor.initialPage
            var result = findInComments(comments, COMMENT_CONTENT)

            while (comments.hasNextPage() && !result) {
                comments = extractor.getPage(comments.nextPage)
                result = findInComments(comments, COMMENT_CONTENT)
            }

            return result
        }

        @Test
        fun testGetCommentsFromCommentsInfo() {
            assertTrue(getCommentsFromCommentsInfoHelper(URL))
        }

        private fun getCommentsFromCommentsInfoHelper(url: String): Boolean {
            val commentsInfo = CommentsInfo.getInfo(url)
            assertNotNull(commentsInfo)

            assertEquals("Comments", commentsInfo!!.name)
            var result = findInComments(commentsInfo.relatedItems, COMMENT_CONTENT)

            var nextPage = commentsInfo.nextPage
            var moreItems = InfoItemsPage<CommentsInfoItem>(null, nextPage, null)
            while (moreItems.hasNextPage() && !result) {
                moreItems = CommentsInfo.getMoreItems(YouTube, commentsInfo, nextPage!!)
                result = findInComments(moreItems.items, COMMENT_CONTENT)
                nextPage = moreItems.nextPage
            }
            return result
        }

        @Test
        fun testGetCommentsAllData() {
            val comments = extractor().initialPage
            assertTrue(extractor().commentsCount > 5) // at least 5 comments

            DefaultTests.defaultTestListOfItems(YouTube, comments.items, comments.errors)
            for (c in comments.items) {
                assertFalse(Utils.isBlank(c.uploaderUrl))
                assertFalse(Utils.isBlank(c.uploaderName))
                YoutubeTestsUtils.testImages(c.uploaderAvatars)
                assertFalse(Utils.isBlank(c.commentId))
                assertFalse(Utils.isBlank(c.commentText.content))
                assertFalse(Utils.isBlank(c.name))
                assertFalse(Utils.isBlank(c.textualUploadDate))
                assertNotNull(c.uploadDate)
                YoutubeTestsUtils.testImages(c.thumbnails)
                assertFalse(Utils.isBlank(c.url))
                assertTrue(c.likeCount >= 0)
            }
        }

        private fun findInComments(comments: InfoItemsPage<CommentsInfoItem>, comment: String): Boolean =
            findInComments(comments.items, comment)

        private fun findInComments(comments: List<CommentsInfoItem>, comment: String): Boolean =
            comments.any { c -> c.commentText.content.contains(comment) }

        private companion object {
            const val URL = "https://www.youtube.com/watch?v=D00Au7k3i6o"
            const val COMMENT_CONTENT = "Category: Education"
        }
    }

    /**
     * Test a video with an empty comment
     */
    class EmptyComment : Base() {
        override fun extractorUrl(): String = URL

        @Test
        fun testGetCommentsAllData() {
            val comments = extractor().initialPage

            DefaultTests.defaultTestListOfItems(YouTube, comments.items, comments.errors)
            for (c in comments.items) {
                assertFalse(Utils.isBlank(c.uploaderUrl))
                assertFalse(Utils.isBlank(c.uploaderName))
                YoutubeTestsUtils.testImages(c.uploaderAvatars)
                assertFalse(Utils.isBlank(c.commentId))
                assertFalse(Utils.isBlank(c.name))
                assertFalse(Utils.isBlank(c.textualUploadDate))
                assertNotNull(c.uploadDate)
                YoutubeTestsUtils.testImages(c.thumbnails)
                assertFalse(Utils.isBlank(c.url))
                assertTrue(c.likeCount >= 0)
                if (c.commentId == "Ugga_h1-EXdHB3gCoAEC") { // comment without text
                    assertTrue(Utils.isBlank(c.commentText.content))
                } else {
                    assertFalse(Utils.isBlank(c.commentText.content))
                }
            }
        }

        private companion object {
            const val URL = "https://www.youtube.com/watch?v=VM_6n762j6M"
        }
    }

    class HeartedByCreator : Base() {
        override fun extractorUrl(): String = URL

        @Test
        fun testGetCommentsAllData() {
            val comments = extractor().initialPage

            DefaultTests.defaultTestListOfItems(YouTube, comments.items, comments.errors)

            var heartedByUploader = false

            for (c in comments.items) {
                assertFalse(Utils.isBlank(c.uploaderUrl))
                assertFalse(Utils.isBlank(c.uploaderName))
                YoutubeTestsUtils.testImages(c.uploaderAvatars)
                assertFalse(Utils.isBlank(c.commentId))
                assertFalse(Utils.isBlank(c.name))
                assertFalse(Utils.isBlank(c.textualUploadDate))
                assertNotNull(c.uploadDate)
                YoutubeTestsUtils.testImages(c.thumbnails)
                assertFalse(Utils.isBlank(c.url))
                assertTrue(c.likeCount >= 0)
                assertFalse(Utils.isBlank(c.commentText.content))
                if (c.isHeartedByUploader) {
                    heartedByUploader = true
                }
            }
            assertTrue(heartedByUploader, "No comments was hearted by uploader")
        }

        private companion object {
            const val URL = "https://www.youtube.com/watch?v=RwTdoQNVMTY"
        }
    }

    class Pinned : Base() {
        override fun extractorUrl(): String = URL

        @Test
        fun testGetCommentsAllData() {
            val comments = extractor().initialPage

            DefaultTests.defaultTestListOfItems(YouTube, comments.items, comments.errors)

            for (c in comments.items) {
                assertFalse(Utils.isBlank(c.uploaderUrl))
                assertFalse(Utils.isBlank(c.uploaderName))
                YoutubeTestsUtils.testImages(c.uploaderAvatars)
                assertFalse(Utils.isBlank(c.commentId))
                assertFalse(Utils.isBlank(c.name))
                assertFalse(Utils.isBlank(c.textualUploadDate))
                assertNotNull(c.uploadDate)
                YoutubeTestsUtils.testImages(c.thumbnails)
                assertFalse(Utils.isBlank(c.url))
                assertTrue(c.likeCount >= 0)
                assertFalse(Utils.isBlank(c.commentText.content))
            }

            assertTrue(comments.items[0].isPinned, "First comment isn't pinned")
        }

        private companion object {
            const val URL = "https://www.youtube.com/watch?v=bjFtFMilb34"
        }
    }

    /**
     * Checks if the likes/votes are handled correctly<br/>
     * A pinned comment with >15K likes is used for the test
     */
    class LikesVotes : Base() {
        override fun extractorUrl(): String = URL

        @Test
        fun testGetCommentsFirst() {
            val comments = extractor().initialPage

            DefaultTests.defaultTestListOfItems(YouTube, comments.items, comments.errors)

            val pinnedComment = comments.items[0]

            assertTrue(pinnedComment.isPinned, "First comment isn't pinned")
            assertTrue(pinnedComment.likeCount > 0, "The first pinned comment has no likes")
            assertFalse(Utils.isBlank(pinnedComment.textualLikeCount), "The first pinned comment has no vote count")
        }

        private companion object {
            const val URL = "https://www.youtube.com/watch?v=QqsLTNkzvaY"
        }
    }

    /**
     * Checks if the vote count works localized<br/>
     * A pinned comment with >15K likes is used for the test
     */
    class LocalizedVoteCount : Base() {
        override fun extractorUrl(): String = URL

        override fun fetchExtractor(extractor: YoutubeCommentsExtractor) {
            // Force non english local here
            extractor.forceLocalization(Localization.fromLocale(Locale.GERMANY))
            super.fetchExtractor(extractor)
        }

        @Test
        fun testGetCommentsFirst() {
            val comments = extractor().initialPage

            DefaultTests.defaultTestListOfItems(YouTube, comments.items, comments.errors)

            val pinnedComment = comments.items[0]

            assertTrue(pinnedComment.isPinned, "First comment isn't pinned")
            assertFalse(Utils.isBlank(pinnedComment.textualLikeCount), "The first pinned comment has no vote count")
        }

        private companion object {
            const val URL = "https://www.youtube.com/watch?v=QqsLTNkzvaY"
        }
    }

    class RepliesTest : Base() {
        override fun extractorUrl(): String = URL

        @Test
        fun testGetCommentsFirstReplies() {
            val comments = extractor().initialPage

            DefaultTests.defaultTestListOfItems(YouTube, comments.items, comments.errors)

            val firstComment = comments.items[0]

            assertTrue(firstComment.isPinned, "First comment isn't pinned")

            val replies = extractor().getPage(firstComment.replies)

            assertEquals(
                "First",
                replies.items[0].commentText.content,
                "First reply comment did not match"
            )
        }

        @Test
        fun testGetCommentsReplyCount() {
            val comments = extractor().initialPage

            DefaultTests.defaultTestListOfItems(YouTube, comments.items, comments.errors)

            val firstComment = comments.items[0]

            assertNotEquals(
                UNKNOWN_REPLY_COUNT,
                firstComment.replyCount,
                "Could not get the reply count of the first comment"
            )
            assertGreater(300, firstComment.replyCount.toLong())
        }

        @Test
        fun testCommentsCount() {
            assertTrue(extractor().commentsCount > 18800)
        }

        private companion object {
            const val URL = "https://www.youtube.com/watch?v=xaQJbozY_Is"
        }
    }

    class ChannelOwnerTest : Base() {
        override fun extractorUrl(): String = URL

        @Test
        fun testGetCommentsAllData() {
            val comments = extractor().initialPage

            DefaultTests.defaultTestListOfItems(YouTube, comments.items, comments.errors)

            var channelOwner = false

            for (c in comments.items) {
                assertFalse(Utils.isBlank(c.uploaderUrl))
                assertFalse(Utils.isBlank(c.uploaderName))
                YoutubeTestsUtils.testImages(c.uploaderAvatars)
                assertFalse(Utils.isBlank(c.commentId))
                assertFalse(Utils.isBlank(c.name))
                assertFalse(Utils.isBlank(c.textualUploadDate))
                assertNotNull(c.uploadDate)
                YoutubeTestsUtils.testImages(c.thumbnails)
                assertFalse(Utils.isBlank(c.url))
                assertTrue(c.likeCount >= 0)
                assertFalse(Utils.isBlank(c.commentText.content))
                if (c.isChannelOwner) {
                    channelOwner = true
                }
            }
            assertTrue(channelOwner, "No comments was made by the channel owner")
        }

        private companion object {
            const val URL = "https://www.youtube.com/watch?v=bem4adjGKjE"
        }
    }

    class CreatorReply : Base() {
        override fun extractorUrl(): String = URL

        @Test
        fun testGetCommentsAllData() {
            val comments = extractor().initialPage

            DefaultTests.defaultTestListOfItems(YouTube, comments.items, comments.errors)

            var creatorReply = false

            for (c in comments.items) {
                assertFalse(Utils.isBlank(c.uploaderUrl))
                assertFalse(Utils.isBlank(c.uploaderName))
                YoutubeTestsUtils.testImages(c.uploaderAvatars)
                assertFalse(Utils.isBlank(c.commentId))
                assertFalse(Utils.isBlank(c.name))
                assertFalse(Utils.isBlank(c.textualUploadDate))
                assertNotNull(c.uploadDate)
                YoutubeTestsUtils.testImages(c.thumbnails)
                assertFalse(Utils.isBlank(c.url))
                assertTrue(c.likeCount >= 0)
                assertFalse(Utils.isBlank(c.commentText.content))
                if (c.hasCreatorReply()) {
                    creatorReply = true
                }
            }
            assertTrue(creatorReply, "No comments was replied to by creator")
        }

        private companion object {
            const val URL = "https://www.youtube.com/watch?v=bem4adjGKjE"
        }
    }

    class Formatting : Base() {
        override fun extractorUrl(): String = URL

        @Test
        fun testGetCommentsFormatting() {
            val comments = extractor().initialPage

            DefaultTests.defaultTestListOfItems(YouTube, comments.items, comments.errors)

            val firstComment = comments.items[1]

            assertContains("<s>", firstComment.commentText.content)
            assertContains("<b>", firstComment.commentText.content)
        }

        private companion object {
            const val URL = "https://www.youtube.com/watch?v=zYpyS2HaZHM"
        }
    }

    class EditedCommentTest : Base() {
        override fun extractorUrl(): String = URL

        @Test
        fun testEditedCommentFlagIsExtracted() {
            val comments = extractor().initialPage

            DefaultTests.defaultTestListOfItems(YouTube, comments.items, comments.errors)

            val hasEditedComment = comments.items.any { comment -> comment.isEdited }

            assertTrue(
                hasEditedComment,
                "No comments is edited on this video. Ensure test video has edited comment near the top."
            )
        }

        private companion object {
            const val URL = "https://www.youtube.com/watch?v=VsFjP58j5i8"
        }
    }
}

package org.schabi.newpipe.extractor.services.bandcamp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ServiceList.Bandcamp
import org.schabi.newpipe.extractor.comments.CommentsExtractor
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest
import org.schabi.newpipe.extractor.services.DefaultTests
import org.schabi.newpipe.extractor.utils.Utils

class BandcampCommentsExtractorTest : DefaultSimpleExtractorTest<CommentsExtractor>() {

    override fun createExtractor(): CommentsExtractor =
        Bandcamp.getCommentsExtractor("https://floatingpoints.bandcamp.com/album/promises")!!

    @Test
    fun hasComments() {
        assertTrue(extractor().initialPage.items.size >= 3)
    }

    @Test
    fun testGetCommentsAllData() {
        val comments = extractor().initialPage
        assertTrue(comments.hasNextPage())

        DefaultTests.defaultTestListOfItems(Bandcamp, comments.items, comments.errors)
        for (c in comments.items) {
            assertFalse(Utils.isBlank(c.uploaderName))
            BandcampTestUtils.testImages(c.uploaderAvatars)
            assertFalse(Utils.isBlank(c.commentText.content))
            assertFalse(Utils.isBlank(c.name))
            BandcampTestUtils.testImages(c.thumbnails)
            assertFalse(Utils.isBlank(c.url))
            assertEquals(-1, c.likeCount)
            assertTrue(Utils.isBlank(c.textualLikeCount))
        }
    }
}

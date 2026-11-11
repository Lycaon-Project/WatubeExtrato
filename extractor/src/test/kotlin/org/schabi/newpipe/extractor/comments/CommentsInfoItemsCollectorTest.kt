package org.schabi.newpipe.extractor.comments

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.exceptions.ParsingException

internal class CommentsInfoItemsCollectorTest {

    private fun comment(uploaderVerified: Boolean): CommentsInfoItemExtractor =
        object : CommentsInfoItemExtractor {
            override val isUploaderVerified: Boolean
                get() = uploaderVerified

            override val name: String
                get() = "comment"

            override val url: String
                get() = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"

            override val thumbnails: List<Image>
                get() = listOf()
        }

    @Test
    @Throws(ParsingException::class)
    fun uploaderVerifiedIsCopiedToTheItem() {
        val collector = CommentsInfoItemsCollector(0)
        assertTrue(collector.extract(comment(true)).isUploaderVerified)
        assertFalse(collector.extract(comment(false)).isUploaderVerified)
    }
}

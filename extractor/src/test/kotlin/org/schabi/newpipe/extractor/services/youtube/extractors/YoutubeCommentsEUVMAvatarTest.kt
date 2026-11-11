package org.schabi.newpipe.extractor.services.youtube.extractors

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.localization.TimeAgoPatternsManager

/**
 * The avatar of a comment author without an `avatar` object: its size comes from the
 * `=s<N>` parameter of its URL, and the URL itself is only handed over in https.
 */
internal class YoutubeCommentsEUVMAvatarTest {

    @Test
    @Throws(ParsingException::class)
    fun sizeIsReadFromTheUrl() {
        val avatar = onlyAvatarOf("https://$AVATAR=s88-c-k-c0x00ffffff-no-rj")
        assertEquals(88, avatar.height)
        assertEquals(88, avatar.width)
        assertEquals(Image.ResolutionLevel.LOW, avatar.estimatedResolutionLevel)
    }

    @Test
    @Throws(ParsingException::class)
    fun originalSizeOrOversizedValueIsUnknown() {
        // =s0 asks for the original size, which the URL does not tell
        assertEquals(Image.HEIGHT_UNKNOWN, onlyAvatarOf("https://$AVATAR=s0-c-k").height)
        // more digits than any avatar size: never parsed, so it can not overflow
        val oversized = onlyAvatarOf("https://$AVATAR=s99999999999999-c-k")
        assertEquals(Image.HEIGHT_UNKNOWN, oversized.height)
        assertEquals(Image.WIDTH_UNKNOWN, oversized.width)
        assertEquals(Image.ResolutionLevel.UNKNOWN, oversized.estimatedResolutionLevel)
    }

    @Test
    @Throws(ParsingException::class)
    fun urlIsOnlyHandedOverInHttps() {
        assertEquals("https://$AVATAR=s48", onlyAvatarOf("http://$AVATAR=s48").url)
        assertEquals("https://$AVATAR=s48", onlyAvatarOf("//$AVATAR=s48").url)
    }

    @Test
    @Throws(ParsingException::class)
    fun missingUrlGivesNoAvatar() {
        assertTrue(avatarsOf(null).isEmpty())
        assertTrue(avatarsOf("").isEmpty())
    }

    private companion object {
        const val AVATAR = "yt3.ggpht.com/ytc/AIdro_abc"

        @Throws(ParsingException::class)
        fun avatarsOf(avatarThumbnailUrl: String?): List<Image> {
            val author = JsonObject()
            if (avatarThumbnailUrl != null) {
                author["avatarThumbnailUrl"] = avatarThumbnailUrl
            }
            val payload = JsonObject()
            payload["author"] = author
            return YoutubeCommentsEUVMInfoItemExtractor(
                JsonObject(),
                null,
                payload,
                JsonObject(),
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                requireNotNull(TimeAgoPatternsManager.getTimeAgoParserFor(Localization.DEFAULT)),
            ).uploaderAvatars
        }

        @Throws(ParsingException::class)
        fun onlyAvatarOf(avatarThumbnailUrl: String?): Image {
            val avatars = avatarsOf(avatarThumbnailUrl)
            assertEquals(1, avatars.size)
            return avatars[0]
        }
    }
}

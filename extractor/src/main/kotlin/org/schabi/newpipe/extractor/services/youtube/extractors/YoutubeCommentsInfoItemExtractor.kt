/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.comments.CommentsInfoItem
import org.schabi.newpipe.extractor.comments.CommentsInfoItemExtractor
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.localization.TimeAgoParser
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getImagesFromThumbnailsArray
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getTextFromObject
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeChannelLinkHandlerFactory
import org.schabi.newpipe.extractor.stream.Description
import org.schabi.newpipe.extractor.utils.JsonUtils
import org.schabi.newpipe.extractor.utils.Utils

public open class YoutubeCommentsInfoItemExtractor(
    private val commentRenderer: JsonObject,
    private val commentRepliesRenderer: JsonObject?,
    override val url: String,
    private val timeAgoParser: TimeAgoParser,
) : CommentsInfoItemExtractor {

    private val authorThumbnails: List<Image>
        @Throws(ParsingException::class)
        get() = try {
            getImagesFromThumbnailsArray(JsonUtils.getArray(commentRenderer, "authorThumbnail.thumbnails"))
        } catch (e: Exception) {
            throw ParsingException("Could not get author thumbnails", e)
        }

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = authorThumbnails

    override val name: String
        get() = authorName

    @get:Throws(ParsingException::class)
    override val textualUploadDate: String?
        get() = try {
            getTextFromObject(JsonUtils.getObject(commentRenderer, PUBLISHED_TIME_TEXT))
        } catch (e: Exception) {
            throw ParsingException("Could not get publishedTimeText", e)
        }

    @get:Throws(ParsingException::class)
    override val uploadDate: DateWrapper?
        get() = textualUploadDate?.takeIf { it.isNotEmpty() }?.let { timeAgoParser.parse(it) }

    /**
     * The method tries first to get the exact like count by using the accessibility data
     * returned. But if the parsing of this accessibility data fails, the method parses internally
     * a localized string.
     *
     * - More than 1k likes will result in an inaccurate number
     * - This will fail for other languages than English. However as long as the Extractor
     *   only uses "en-GB" (as seen in
     *   [org.schabi.newpipe.extractor.services.youtube.YoutubeService.supportedLocalizations]),
     *   everything will work fine.
     *
     * Consider using [textualLikeCount]
     */
    @get:Throws(ParsingException::class)
    override val likeCount: Int
        get() {
            // Try first to get the exact like count by using the accessibility data
            val likeCount = try {
                Utils.removeNonDigitCharacters(
                    JsonUtils.getString(
                        commentRenderer,
                        "actionButtons.commentActionButtonsRenderer.likeButton.toggleButtonRenderer" +
                            ".accessibilityData.accessibilityData.label",
                    ),
                )
            } catch (_: Exception) {
                // Use the approximate like count returned into the voteCount object
                // This may return a language dependent version, e.g. in German: 3,3 Mio
                return likeCountFromTextualLikeCount()
            }

            if (Utils.isBlank(likeCount)) {
                return 0
            }
            return likeCount.toIntOrNull()
                ?: throw ParsingException("Unexpected error while parsing like count as Integer")
        }

    @Throws(ParsingException::class)
    private fun likeCountFromTextualLikeCount(): Int {
        val textualLikeCount = textualLikeCount
        return try {
            if (Utils.isBlank(textualLikeCount)) {
                0
            } else {
                Utils.mixedNumberWordToLong(textualLikeCount.orEmpty()).toInt()
            }
        } catch (e: Exception) {
            throw ParsingException("Unexpected error while converting textual like count to like count", e)
        }
    }

    /*
     * Example results as of 2021-05-20:
     * Language = English
     * 3.3M
     * 48K
     * 1.4K
     * 270K
     * 19
     * 6
     *
     * Language = German
     * 3,3 Mio
     * 48.189
     * 1419
     * 270.984
     * 19
     * 6
     */
    @get:Throws(ParsingException::class)
    override val textualLikeCount: String?
        get() {
            // If a comment has no likes voteCount is not set
            if (!commentRenderer.has("voteCount")) {
                return ""
            }
            return try {
                val voteCountObj = JsonUtils.getObject(commentRenderer, "voteCount")
                if (voteCountObj.isEmpty()) "" else getTextFromObject(voteCountObj)
            } catch (e: Exception) {
                throw ParsingException("Could not get the vote count", e)
            }
        }

    @get:Throws(ParsingException::class)
    override val commentText: Description
        get() = try {
            val contentText = JsonUtils.getObject(commentRenderer, "contentText")
            if (contentText.isEmpty()) {
                // completely empty comments as described in
                // https://github.com/TeamNewPipe/NewPipeExtractor/issues/380#issuecomment-668808584
                Description.EMPTY_DESCRIPTION
            } else {
                val commentText = getTextFromObject(contentText, true).orEmpty()
                // YouTube adds U+FEFF in some comments.
                // eg. https://www.youtube.com/watch?v=Nj4F63E59io<feff>
                Description.of(Utils.removeUTF8BOM(commentText), Description.Type.HTML)
            }
        } catch (e: Exception) {
            throw ParsingException("Could not get comment text", e)
        }

    @get:Throws(ParsingException::class)
    override val commentId: String?
        get() = try {
            JsonUtils.getString(commentRenderer, "commentId")
        } catch (e: Exception) {
            throw ParsingException("Could not get comment id", e)
        }

    @get:Throws(ParsingException::class)
    override val uploaderAvatars: List<Image>
        get() = authorThumbnails

    override val isHeartedByUploader: Boolean
        get() = commentRenderer.getObject("actionButtons")
            .getObject("commentActionButtonsRenderer")
            .has("creatorHeart")

    override val isPinned: Boolean
        get() = commentRenderer.has("pinnedCommentBadge")

    override val isUploaderVerified: Boolean
        get() = commentRenderer.has("authorCommentBadge")

    override val uploaderName: String?
        get() = authorName

    override val uploaderUrl: String?
        get() = try {
            // The channel link handler validates the ID before placing it in the URL
            YoutubeChannelLinkHandlerFactory.getInstance().getUrl(
                "channel/" + JsonUtils.getString(commentRenderer, "authorEndpoint.browseEndpoint.browseId"),
            )
        } catch (_: Exception) {
            ""
        }

    override val replyCount: Int
        get() = if (commentRenderer.has("replyCount")) {
            commentRenderer.getInt("replyCount")
        } else {
            CommentsInfoItem.UNKNOWN_REPLY_COUNT
        }

    override val replies: Page?
        get() {
            val repliesRenderer = commentRepliesRenderer ?: return null
            return try {
                val id = JsonUtils.getString(
                    JsonUtils.getArray(repliesRenderer, "contents").getObject(0),
                    "continuationItemRenderer.continuationEndpoint.continuationCommand.token",
                )
                Page(url, id)
            } catch (_: Exception) {
                null
            }
        }

    override val isChannelOwner: Boolean
        get() = commentRenderer.getBoolean("authorIsChannelOwner")

    @get:Throws(ParsingException::class)
    override val isEdited: Boolean
        get() {
            if (!commentRenderer.has(PUBLISHED_TIME_TEXT)) {
                return false
            }
            return try {
                val publishedTimeText = JsonUtils.getObject(commentRenderer, PUBLISHED_TIME_TEXT)
                publishedTimeText.has("runs") && publishedTimeText.getArray("runs").size > 1
            } catch (e: Exception) {
                throw ParsingException("Could not check whether the comment is edited", e)
            }
        }

    override fun hasCreatorReply(): Boolean =
        commentRepliesRenderer?.has("viewRepliesCreatorThumbnail") == true

    /** The author name, or an empty string when it is missing. */
    private val authorName: String
        get() = try {
            getTextFromObject(JsonUtils.getObject(commentRenderer, "authorText")).orEmpty()
        } catch (_: Exception) {
            ""
        }

    private companion object {
        private const val PUBLISHED_TIME_TEXT = "publishedTimeText"
    }
}

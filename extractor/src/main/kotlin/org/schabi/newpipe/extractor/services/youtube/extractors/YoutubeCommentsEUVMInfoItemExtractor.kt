/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.comments.CommentsInfoItemExtractor
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.localization.TimeAgoParser
import org.schabi.newpipe.extractor.services.youtube.YoutubeDescriptionHelper.attributedDescriptionToHtml
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.fixThumbnailUrl
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getImagesFromThumbnailsArray
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeChannelLinkHandlerFactory
import org.schabi.newpipe.extractor.stream.Description
import org.schabi.newpipe.extractor.utils.Utils

/**
 * A [CommentsInfoItemExtractor] for YouTube comment data returned in a view model and entity
 * updates.
 */
internal class YoutubeCommentsEUVMInfoItemExtractor(
    private val commentViewModel: JsonObject,
    private val commentRepliesRenderer: JsonObject?,
    private val commentEntityPayload: JsonObject,
    private val engagementToolbarStateEntityPayload: JsonObject,
    private val videoUrl: String,
    private val timeAgoParser: TimeAgoParser,
) : CommentsInfoItemExtractor {

    override val name: String
        // A comment whose author has no name keeps an empty one rather than being dropped
        get() = uploaderName.orEmpty()

    override val url: String
        get() = videoUrl

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = uploaderAvatars

    @get:Throws(ParsingException::class)
    override val likeCount: Int
        get() {
            val textualLikeCount = textualLikeCount
            return try {
                if (Utils.isBlank(textualLikeCount)) {
                    0
                } else {
                    Utils.mixedNumberWordToLong(textualLikeCount.orEmpty()).toInt()
                }
            } catch (e: Exception) {
                throw ParsingException(
                    "Unexpected error while converting textual like count to like count",
                    e,
                )
            }
        }

    override val textualLikeCount: String?
        get() = commentEntityPayload.getObject("toolbar").getString("likeCountNotliked")

    @get:Throws(ParsingException::class)
    override val commentText: Description
        get() {
            // Comments' text work in the same way as an attributed video description
            val text = attributedDescriptionToHtml(
                commentEntityPayload.getObject(PROPERTIES).getObject("content"),
            )
            return Description.of(text, Description.Type.HTML)
        }

    @get:Throws(ParsingException::class)
    override val textualUploadDate: String?
        get() = commentEntityPayload.getObject(PROPERTIES).getString(PUBLISHED_TIME)

    @get:Throws(ParsingException::class)
    override val uploadDate: DateWrapper?
        get() = textualUploadDate?.takeIf { it.isNotEmpty() }?.let { timeAgoParser.parse(it) }

    @get:Throws(ParsingException::class)
    override val commentId: String
        get() = commentEntityPayload.getObject(PROPERTIES).getString("commentId")
            ?.takeIf { it.isNotEmpty() }
            ?: commentViewModel.getString("commentId")?.takeIf { it.isNotEmpty() }
            ?: throw ParsingException("Could not get comment ID")

    @get:Throws(ParsingException::class)
    override val uploaderUrl: String
        get() {
            val author = commentEntityPayload.getObject(AUTHOR)
            val channelId = author.getString("channelId")?.takeIf { it.isNotEmpty() }
                ?: browseIdOf(author.getObject("channelCommand"))
                ?: browseIdOf(author.getObject("avatar").getObject("endpoint"))
                ?: throw ParsingException("Could not get channel ID")
            // The channel link handler validates the ID before placing it in the URL
            return YoutubeChannelLinkHandlerFactory.getInstance().getUrl("channel/$channelId")
        }

    override val uploaderName: String?
        get() = commentEntityPayload.getObject(AUTHOR).getString("displayName")

    @get:Throws(ParsingException::class)
    override val uploaderAvatars: List<Image>
        get() {
            val avatar = commentEntityPayload.getObject("avatar", null)
            if (avatar != null) {
                return getImagesFromThumbnailsArray(avatar.getObject("image").getArray("sources"))
            }

            // YouTube avatar URLs carry their size in an =s<N> query parameter,
            // so the size is parsed from the URL instead of being hard-coded.
            val rawUrl = commentEntityPayload.getObject(AUTHOR).getString("avatarThumbnailUrl")
            if (rawUrl.isNullOrEmpty()) {
                return emptyList()
            }
            // https only, like every other thumbnail: the avatar is never fetched in the clear
            val url = fixThumbnailUrl(rawUrl)
            val size = AVATAR_SIZE_URL_PARAMETER.find(url)?.groupValues?.get(1)?.toIntOrNull()
                ?: Image.HEIGHT_UNKNOWN
            return listOf(Image(url, size, size, Image.ResolutionLevel.fromHeight(size)))
        }

    override val isHeartedByUploader: Boolean
        get() = "TOOLBAR_HEART_STATE_HEARTED" ==
            engagementToolbarStateEntityPayload.getString("heartState")

    override val isPinned: Boolean
        get() = commentViewModel.has("pinnedText")

    @get:Throws(ParsingException::class)
    override val isUploaderVerified: Boolean
        get() {
            val author = commentEntityPayload.getObject(AUTHOR)
            return author.getBoolean("isVerified") || author.getBoolean("isArtist")
        }

    // As YouTube allows replies up to 750 comments, we cannot check if the count returned is a
    // mixed number or a real number
    // Assume it is a mixed one, as it matches how numbers of most properties are returned
    @get:Throws(ParsingException::class)
    override val replyCount: Int
        get() {
            val replyCountString = commentEntityPayload.getObject("toolbar").getString("replyCount")
            if (replyCountString.isNullOrEmpty()) {
                return 0
            }
            return Utils.mixedNumberWordToLong(replyCountString).toInt()
        }

    @get:Throws(ParsingException::class)
    override val replies: Page?
        get() {
            if (commentRepliesRenderer.isNullOrEmpty()) {
                return null
            }
            val continuation = commentRepliesRenderer.getArray("contents")
                .filterIsInstance<JsonObject>()
                .firstNotNullOfOrNull { it.getObject("continuationItemRenderer", null) }
                ?.getObject("continuationEndpoint")
                ?.getObject("continuationCommand")
                ?.getString("token")
                ?: throw ParsingException("Could not get comment replies continuation")
            return Page(videoUrl, continuation)
        }

    override val isChannelOwner: Boolean
        get() = commentEntityPayload.getObject(AUTHOR).getBoolean("isCreator")

    override fun hasCreatorReply(): Boolean =
        commentRepliesRenderer?.has("viewRepliesCreatorThumbnail") == true

    @get:Throws(ParsingException::class)
    override val isEdited: Boolean
        get() {
            val properties = commentEntityPayload.getObject(PROPERTIES)
            if (!properties.has(PUBLISHED_TIME)) {
                return false
            }
            val publishedTime = properties.getString(PUBLISHED_TIME, "").orEmpty()
            return publishedTime.contains("(") && publishedTime.contains(")")
        }

    private fun browseIdOf(endpointHolder: JsonObject): String? =
        endpointHolder.getObject("innertubeCommand")
            .getObject("browseEndpoint")
            .getString("browseId")
            ?.takeIf { it.isNotEmpty() }

    private companion object {
        private const val AUTHOR = "author"
        private const val PROPERTIES = "properties"
        private const val PUBLISHED_TIME = "publishedTime"

        /**
         * YouTube avatar URLs encode their size as an `=s<N>` parameter
         * (e.g. `...=s88-c-k-c0x00ffffff-no-nd-rj`). At most 4 digits, so that a malformed
         * value can never overflow [String.toInt]; `=s0` means "original size" and is left
         * unknown.
         */
        private val AVATAR_SIZE_URL_PARAMETER = Regex("=s([1-9]\\d{0,3})(?!\\d)")
    }
}

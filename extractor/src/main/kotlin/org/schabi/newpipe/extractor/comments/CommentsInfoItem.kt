package org.schabi.newpipe.extractor.comments

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.stream.Description

public open class CommentsInfoItem(
    serviceId: Int,
    url: String,
    name: String,
) : InfoItem(InfoType.COMMENT, serviceId, url, name) {

    public var commentId: String? = null
    public var commentText: Description = Description.EMPTY_DESCRIPTION
    public var uploaderName: String? = null
    public var uploaderAvatars: List<Image> = emptyList()
    public var uploaderUrl: String? = null
    public var isUploaderVerified: Boolean = false
    public var textualUploadDate: String? = null
    public var uploadDate: DateWrapper? = null

    /**
     * The comment's like count or [NO_LIKE_COUNT] if it is unavailable
     */
    public var likeCount: Int = 0
    public var textualLikeCount: String? = null
    public var isHeartedByUploader: Boolean = false
    public var isPinned: Boolean = false

    /**
     * The playback position of the stream to which this comment belongs, in seconds, or
     * [NO_STREAM_POSITION] if not available. This is not supported by all services.
     */
    public var streamPosition: Int = 0
    public var replyCount: Int = 0
    public var replies: Page? = null
    public var isChannelOwner: Boolean = false
    public var isEdited: Boolean = false

    private var creatorReply = false

    public fun setCreatorReply(creatorReply: Boolean) {
        this.creatorReply = creatorReply
    }

    public fun hasCreatorReply(): Boolean = creatorReply

    public companion object {
        private const val serialVersionUID: Long = 1L

        public const val NO_LIKE_COUNT: Int = -1
        public const val NO_STREAM_POSITION: Int = -1
        public const val UNKNOWN_REPLY_COUNT: Int = -1
    }
}

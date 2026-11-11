package org.schabi.newpipe.extractor.comments

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.InfoItemExtractor
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.stream.Description

public interface CommentsInfoItemExtractor : InfoItemExtractor {

    /**
     * Return the like count of the comment,
     * or [CommentsInfoItem.NO_LIKE_COUNT] if it is unavailable.
     *
     * NOTE: Currently only implemented for YT
     * [org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeCommentsInfoItemExtractor.getLikeCount]
     * with limitations (only approximate like count is returned)
     *
     * @return the comment's like count
     * or [CommentsInfoItem.NO_LIKE_COUNT] if it is unavailable
     * @see org.schabi.newpipe.extractor.stream.StreamExtractor.likeCount
     */
    @get:Throws(ParsingException::class)
    public val likeCount: Int
        get() = CommentsInfoItem.NO_LIKE_COUNT

    /**
     * The unmodified like count given by the service
     *
     * It may be language dependent
     */
    @get:Throws(ParsingException::class)
    public val textualLikeCount: String?
        get() = ""

    /**
     * The text of the comment
     */
    @get:Throws(ParsingException::class)
    public val commentText: Description
        get() = Description.EMPTY_DESCRIPTION

    /**
     * The upload date given by the service, unmodified
     *
     * @see org.schabi.newpipe.extractor.stream.StreamExtractor.textualUploadDate
     */
    @get:Throws(ParsingException::class)
    public val textualUploadDate: String?
        get() = ""

    /**
     * The upload date wrapped with DateWrapper class
     *
     * @see org.schabi.newpipe.extractor.stream.StreamExtractor.uploadDate
     */
    @get:Throws(ParsingException::class)
    public val uploadDate: DateWrapper?
        get() = null

    @get:Throws(ParsingException::class)
    public val commentId: String?
        get() = ""

    @get:Throws(ParsingException::class)
    public val uploaderUrl: String?
        get() = ""

    @get:Throws(ParsingException::class)
    public val uploaderName: String?
        get() = ""

    @get:Throws(ParsingException::class)
    public val uploaderAvatars: List<Image>
        get() = emptyList()

    /**
     * Whether the comment has been hearted by the uploader
     */
    @get:Throws(ParsingException::class)
    public val isHeartedByUploader: Boolean
        get() = false

    /**
     * Whether the comment is pinned
     */
    @get:Throws(ParsingException::class)
    public val isPinned: Boolean
        get() = false

    /**
     * Whether the uploader is verified by the service
     */
    @get:Throws(ParsingException::class)
    public val isUploaderVerified: Boolean
        get() = false

    /**
     * The playback position of the stream to which this comment belongs.
     *
     * @see CommentsInfoItem.streamPosition
     */
    @get:Throws(ParsingException::class)
    public val streamPosition: Int
        get() = CommentsInfoItem.NO_STREAM_POSITION

    /**
     * The count of comment replies.
     *
     * @return the count of the replies
     * or [CommentsInfoItem.UNKNOWN_REPLY_COUNT] if replies are not supported
     */
    @get:Throws(ParsingException::class)
    public val replyCount: Int
        get() = CommentsInfoItem.UNKNOWN_REPLY_COUNT

    /**
     * The continuation page which is used to get comment replies from.
     *
     * @return the continuation Page for the replies, or null if replies are not supported
     */
    @get:Throws(ParsingException::class)
    public val replies: Page?
        get() = null

    /**
     * Whether the comment was made by the channel owner.
     */
    @get:Throws(ParsingException::class)
    public val isChannelOwner: Boolean
        get() = false

    /**
     * Whether the comment was replied to by the creator.
     */
    @Throws(ParsingException::class)
    public fun hasCreatorReply(): Boolean = false

    /**
     * Whether the comment is edited.
     */
    @get:Throws(ParsingException::class)
    public val isEdited: Boolean
        get() = false
}

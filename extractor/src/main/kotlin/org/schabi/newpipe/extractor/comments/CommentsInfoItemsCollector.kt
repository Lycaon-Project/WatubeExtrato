package org.schabi.newpipe.extractor.comments

import org.schabi.newpipe.extractor.InfoItemsCollector
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.utils.ExtractorLogger

public class CommentsInfoItemsCollector(serviceId: Int) :
    InfoItemsCollector<CommentsInfoItem, CommentsInfoItemExtractor>(serviceId) {

    /** A copy of the collected comments. */
    public val commentsInfoItemList: List<CommentsInfoItem>
        get() = ArrayList(items)

    @Throws(ParsingException::class)
    override fun extract(extractor: CommentsInfoItemExtractor): CommentsInfoItem {
        val resultItem = CommentsInfoItem(serviceId, extractor.url, extractor.name)

        // optional information
        collect { resultItem.commentId = extractor.commentId }
        collect { resultItem.commentText = extractor.commentText }
        collect { resultItem.uploaderName = extractor.uploaderName }
        collect { resultItem.uploaderAvatars = extractor.uploaderAvatars }
        collect { resultItem.uploaderUrl = extractor.uploaderUrl }
        collect { resultItem.textualUploadDate = extractor.textualUploadDate }
        collect { resultItem.uploadDate = extractor.uploadDate }
        collect { resultItem.likeCount = extractor.likeCount }
        collect { resultItem.textualLikeCount = extractor.textualLikeCount }
        collect { resultItem.thumbnails = extractor.thumbnails }
        collect { resultItem.isHeartedByUploader = extractor.isHeartedByUploader }
        collect { resultItem.isPinned = extractor.isPinned }
        collect { resultItem.streamPosition = extractor.streamPosition }
        collect { resultItem.replyCount = extractor.replyCount }
        collect { resultItem.replies = extractor.replies }
        collect { resultItem.isChannelOwner = extractor.isChannelOwner }
        collect { resultItem.isUploaderVerified = extractor.isUploaderVerified }
        collect { resultItem.setCreatorReply(extractor.hasCreatorReply()) }
        collect { resultItem.isEdited = extractor.isEdited }
        return resultItem
    }

    override fun commit(extractor: CommentsInfoItemExtractor) {
        try {
            addItem(extract(extractor))
        } catch (e: ParsingException) {
            addError(e)
        } catch (e: Exception) {
            // not odd data but a bug: still kept out of the list, and logged
            ExtractorLogger.e(javaClass.simpleName, "Unexpected error while extracting a comment", e)
            addError(e)
        }
    }

    /** Records a comment that could not even be handed to [commit]. */
    internal fun addSkippedItemError(error: Exception) {
        addError(error)
    }

    /** Runs one optional extraction: a failure is recorded, not thrown. */
    private inline fun collect(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            addError(e)
        }
    }
}

package org.schabi.newpipe.extractor.comments

import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler

public abstract class CommentsExtractor(
    service: StreamingService,
    uiHandler: ListLinkHandler,
) : ListExtractor<CommentsInfoItem>(service, uiHandler) {

    /**
     * Warning: This property is experimental and may get removed in a future release.
     *
     * @return `true` if the comments are disabled otherwise `false` (default)
     */
    @get:Throws(ExtractionException::class)
    public open val isCommentsDisabled: Boolean
        get() = false

    /**
     * @return the total number of comments
     */
    @get:Throws(ExtractionException::class)
    public open val commentsCount: Int
        get() = -1

    @get:Throws(ParsingException::class)
    override val name: String
        get() = "Comments"
}

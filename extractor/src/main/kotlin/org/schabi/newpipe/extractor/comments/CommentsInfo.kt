package org.schabi.newpipe.extractor.comments

import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage
import org.schabi.newpipe.extractor.ListInfo
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.utils.ExtractorHelper
import java.io.IOException

public class CommentsInfo private constructor(
    serviceId: Int,
    listUrlIdHandler: ListLinkHandler,
    name: String,
) : ListInfo<CommentsInfoItem>(serviceId, listUrlIdHandler, name) {

    /** The extractor these comments come from; never serialized. */
    @Transient
    public var commentsExtractor: CommentsExtractor? = null

    /**
     * `true` if the comments are disabled otherwise `false` (default)
     *
     * @see CommentsExtractor.isCommentsDisabled
     */
    public var isCommentsDisabled: Boolean = false

    /**
     * The total number of comments.
     */
    public var commentsCount: Int = 0

    public companion object {
        private const val serialVersionUID: Long = 1L

        /**
         * The comments of [url], or `null` for services without a comments extractor.
         */
        @JvmStatic
        @Throws(IOException::class, ExtractionException::class)
        public fun getInfo(url: String): CommentsInfo? = getInfo(NewPipe.getServiceByUrl(url), url)

        @JvmStatic
        @Throws(ExtractionException::class, IOException::class)
        public fun getInfo(service: StreamingService, url: String): CommentsInfo? =
            getInfo(service.getCommentsExtractor(url))

        @JvmStatic
        @Throws(IOException::class, ExtractionException::class)
        public fun getInfo(commentsExtractor: CommentsExtractor?): CommentsInfo? {
            // for services which do not have a comments extractor
            if (commentsExtractor == null) {
                return null
            }

            commentsExtractor.fetchPage()

            val name = commentsExtractor.name
            val serviceId = commentsExtractor.serviceId
            val listUrlIdHandler = commentsExtractor.linkHandler
            val commentsInfo = CommentsInfo(serviceId, listUrlIdHandler, name)
            commentsInfo.commentsExtractor = commentsExtractor
            val initialCommentsPage: InfoItemsPage<CommentsInfoItem> =
                ExtractorHelper.getItemsPageOrLogError(commentsInfo, commentsExtractor)
            commentsInfo.isCommentsDisabled = commentsExtractor.isCommentsDisabled
            commentsInfo.relatedItems = initialCommentsPage.items
            try {
                commentsInfo.commentsCount = commentsExtractor.commentsCount
            } catch (e: Exception) {
                commentsInfo.addError(e)
            }
            commentsInfo.nextPage = initialCommentsPage.nextPage

            return commentsInfo
        }

        @JvmStatic
        @Throws(ExtractionException::class, IOException::class)
        public fun getMoreItems(
            commentsInfo: CommentsInfo,
            page: Page,
        ): InfoItemsPage<CommentsInfoItem> =
            getMoreItems(NewPipe.getService(commentsInfo.serviceId), commentsInfo.url, page)

        @JvmStatic
        @Throws(IOException::class, ExtractionException::class)
        public fun getMoreItems(
            service: StreamingService,
            commentsInfo: CommentsInfo,
            page: Page,
        ): InfoItemsPage<CommentsInfoItem> = getMoreItems(service, commentsInfo.url, page)

        @JvmStatic
        @Throws(IOException::class, ExtractionException::class)
        public fun getMoreItems(
            service: StreamingService,
            url: String,
            page: Page,
        ): InfoItemsPage<CommentsInfoItem> =
            // Services without comments return no extractor: clean error instead of an NPE
            service.getCommentsExtractor(url)?.getPage(page)
                ?: throw ExtractionException("Comments are not supported by this service")
    }
}

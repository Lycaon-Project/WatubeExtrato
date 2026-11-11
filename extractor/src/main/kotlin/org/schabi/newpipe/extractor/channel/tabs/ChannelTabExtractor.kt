package org.schabi.newpipe.extractor.channel.tabs

import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler

/**
 * A [ListExtractor] of [InfoItem]s for tabs of channels.
 */
public abstract class ChannelTabExtractor protected constructor(
    service: StreamingService,
    linkHandler: ListLinkHandler,
) : ListExtractor<InfoItem>(service, linkHandler) {

    /**
     * The tab name is its content filter. No checked exception, like the Java original (Java callers rely
     * on it); a handler rebuilt without filters gets a clean unchecked error instead of an index error.
     */
    override val name: String
        get() = checkNotNull(linkHandler.contentFilters.firstOrNull()) { "Channel tab has no content filter" }
}

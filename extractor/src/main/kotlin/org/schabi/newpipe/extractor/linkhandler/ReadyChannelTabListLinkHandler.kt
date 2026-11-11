package org.schabi.newpipe.extractor.linkhandler

import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor
import java.io.Serializable

/**
 * A [ListLinkHandler] which can be used to be returned from
 * [ChannelInfo.getTabs()][org.schabi.newpipe.extractor.channel.ChannelInfo.getTabs] when a
 * specific tab's data has already been fetched.
 *
 * This class allows passing a builder for a [ChannelTabExtractor] that can hold references
 * to variables.
 *
 * Note: a service that wishes to use this class in one of its
 * [ChannelExtractor][org.schabi.newpipe.extractor.channel.ChannelExtractor]s must also add the
 * following snippet of code in the service's [StreamingService.getChannelTabExtractor]:
 * ```
 * if (linkHandler instanceof ReadyChannelTabListLinkHandler) {
 *     return ((ReadyChannelTabListLinkHandler) linkHandler).getChannelTabExtractor(this);
 * }
 * ```
 */
public open class ReadyChannelTabListLinkHandler(
    url: String,
    channelId: String,
    channelTab: String,
    private val extractorBuilder: ChannelTabExtractorBuilder,
) : ListLinkHandler(url, url, channelId, listOf(channelTab), "") {

    public fun interface ChannelTabExtractorBuilder : Serializable {
        public fun build(service: StreamingService, linkHandler: ListLinkHandler): ChannelTabExtractor
    }

    public fun getChannelTabExtractor(service: StreamingService): ChannelTabExtractor =
        extractorBuilder.build(service, ListLinkHandler(this))

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

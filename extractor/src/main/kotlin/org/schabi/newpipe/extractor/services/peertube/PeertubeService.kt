/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.peertube

import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.StreamingService.ServiceInfo.MediaCapability.COMMENTS
import org.schabi.newpipe.extractor.StreamingService.ServiceInfo.MediaCapability.VIDEO
import org.schabi.newpipe.extractor.channel.ChannelExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor
import org.schabi.newpipe.extractor.comments.CommentsExtractor
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.kiosk.KioskList
import org.schabi.newpipe.extractor.linkhandler.LinkHandler
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.services.peertube.extractors.PeertubeAccountExtractor
import org.schabi.newpipe.extractor.services.peertube.extractors.PeertubeChannelExtractor
import org.schabi.newpipe.extractor.services.peertube.extractors.PeertubeChannelTabExtractor
import org.schabi.newpipe.extractor.services.peertube.extractors.PeertubeCommentsExtractor
import org.schabi.newpipe.extractor.services.peertube.extractors.PeertubePlaylistExtractor
import org.schabi.newpipe.extractor.services.peertube.extractors.PeertubeSearchExtractor
import org.schabi.newpipe.extractor.services.peertube.extractors.PeertubeStreamExtractor
import org.schabi.newpipe.extractor.services.peertube.extractors.PeertubeSuggestionExtractor
import org.schabi.newpipe.extractor.services.peertube.extractors.PeertubeTrendingExtractor
import org.schabi.newpipe.extractor.services.peertube.linkHandler.PeertubeChannelLinkHandlerFactory
import org.schabi.newpipe.extractor.services.peertube.linkHandler.PeertubeChannelTabLinkHandlerFactory
import org.schabi.newpipe.extractor.services.peertube.linkHandler.PeertubeCommentsLinkHandlerFactory
import org.schabi.newpipe.extractor.services.peertube.linkHandler.PeertubePlaylistLinkHandlerFactory
import org.schabi.newpipe.extractor.services.peertube.linkHandler.PeertubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.services.peertube.linkHandler.PeertubeStreamLinkHandlerFactory
import org.schabi.newpipe.extractor.services.peertube.linkHandler.PeertubeTrendingLinkHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.subscription.SubscriptionExtractor
import org.schabi.newpipe.extractor.suggestion.SuggestionExtractor
import java.util.EnumSet

public open class PeertubeService(
    id: Int,
    /**
     * The instance every request without a link of its own goes to. Changed from the settings
     * while extractors run on other threads: they always read the latest one.
     */
    @Volatile
    public var instance: PeertubeInstance,
) : StreamingService(id, "PeerTube", EnumSet.of(VIDEO, COMMENTS)) {

    public constructor(id: Int) : this(id, PeertubeInstance.DEFAULT_INSTANCE)

    override val streamLHFactory: LinkHandlerFactory
        get() = PeertubeStreamLinkHandlerFactory

    override val channelLHFactory: ListLinkHandlerFactory
        get() = PeertubeChannelLinkHandlerFactory

    override val channelTabLHFactory: ListLinkHandlerFactory
        get() = PeertubeChannelTabLinkHandlerFactory

    override val playlistLHFactory: ListLinkHandlerFactory
        get() = PeertubePlaylistLinkHandlerFactory

    override val searchQHFactory: SearchQueryHandlerFactory
        get() = PeertubeSearchQueryHandlerFactory

    override val commentsLHFactory: ListLinkHandlerFactory
        get() = PeertubeCommentsLinkHandlerFactory

    override fun getSearchExtractor(queryHandler: SearchQueryHandler): SearchExtractor =
        PeertubeSearchExtractor(
            this,
            queryHandler,
            queryHandler.contentFilters.firstOrNull()?.startsWith("sepia_") == true,
        )

    override val suggestionExtractor: SuggestionExtractor
        get() = PeertubeSuggestionExtractor(this)

    override val subscriptionExtractor: SubscriptionExtractor?
        get() = null

    @Throws(ExtractionException::class)
    override fun getChannelExtractor(linkHandler: ListLinkHandler): ChannelExtractor =
        if (linkHandler.url.contains("/video-channels/")) {
            PeertubeChannelExtractor(this, linkHandler)
        } else {
            PeertubeAccountExtractor(this, linkHandler)
        }

    @Throws(ExtractionException::class)
    override fun getChannelTabExtractor(linkHandler: ListLinkHandler): ChannelTabExtractor =
        PeertubeChannelTabExtractor(this, linkHandler)

    @Throws(ExtractionException::class)
    override fun getPlaylistExtractor(linkHandler: ListLinkHandler): PlaylistExtractor =
        PeertubePlaylistExtractor(this, linkHandler)

    @Throws(ExtractionException::class)
    override fun getStreamExtractor(linkHandler: LinkHandler): StreamExtractor =
        PeertubeStreamExtractor(this, linkHandler)

    @Throws(ExtractionException::class)
    override fun getCommentsExtractor(linkHandler: ListLinkHandler): CommentsExtractor =
        PeertubeCommentsExtractor(this, linkHandler)

    override val baseUrl: String
        get() = instance.url

    @get:Throws(ExtractionException::class)
    override val kioskList: KioskList
        get() {
            val h = PeertubeTrendingLinkHandlerFactory
            val kioskFactory = KioskList.KioskExtractorFactory { _, _, id ->
                PeertubeTrendingExtractor(this, h.fromId(id), id)
            }

            val list = KioskList(this)

            // add kiosks here e.g.:
            try {
                list.addKioskEntry(kioskFactory, h, PeertubeTrendingLinkHandlerFactory.KIOSK_TRENDING)
                list.addKioskEntry(kioskFactory, h, PeertubeTrendingLinkHandlerFactory.KIOSK_MOST_LIKED)
                list.addKioskEntry(kioskFactory, h, PeertubeTrendingLinkHandlerFactory.KIOSK_RECENT)
                list.addKioskEntry(kioskFactory, h, PeertubeTrendingLinkHandlerFactory.KIOSK_LOCAL)
                list.setDefaultKiosk(PeertubeTrendingLinkHandlerFactory.KIOSK_TRENDING)
            } catch (e: Exception) {
                throw ExtractionException(e)
            }

            return list
        }
}

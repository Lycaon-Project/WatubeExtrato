// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

package org.schabi.newpipe.extractor.services.bandcamp

import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.StreamingService.ServiceInfo.MediaCapability.AUDIO
import org.schabi.newpipe.extractor.StreamingService.ServiceInfo.MediaCapability.COMMENTS
import org.schabi.newpipe.extractor.channel.ChannelExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor
import org.schabi.newpipe.extractor.comments.CommentsExtractor
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.kiosk.KioskList
import org.schabi.newpipe.extractor.linkhandler.LinkHandler
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory
import org.schabi.newpipe.extractor.linkhandler.ReadyChannelTabListLinkHandler
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampChannelExtractor
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampChannelTabExtractor
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampCommentsExtractor
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.BASE_URL
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampFeaturedExtractor
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampFeaturedExtractor.Companion.FEATURED_API_URL
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampFeaturedExtractor.Companion.KIOSK_FEATURED
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampPlaylistExtractor
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampRadioExtractor
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampRadioExtractor.Companion.KIOSK_RADIO
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampRadioExtractor.Companion.RADIO_API_URL
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampRadioStreamExtractor
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampSearchExtractor
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampStreamExtractor
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampSuggestionExtractor
import org.schabi.newpipe.extractor.services.bandcamp.linkHandler.BandcampChannelLinkHandlerFactory
import org.schabi.newpipe.extractor.services.bandcamp.linkHandler.BandcampChannelTabLinkHandlerFactory
import org.schabi.newpipe.extractor.services.bandcamp.linkHandler.BandcampCommentsLinkHandlerFactory
import org.schabi.newpipe.extractor.services.bandcamp.linkHandler.BandcampFeaturedLinkHandlerFactory
import org.schabi.newpipe.extractor.services.bandcamp.linkHandler.BandcampPlaylistLinkHandlerFactory
import org.schabi.newpipe.extractor.services.bandcamp.linkHandler.BandcampSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.services.bandcamp.linkHandler.BandcampStreamLinkHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.subscription.SubscriptionExtractor
import org.schabi.newpipe.extractor.suggestion.SuggestionExtractor
import java.util.EnumSet

public open class BandcampService(id: Int) :
    StreamingService(id, "Bandcamp", EnumSet.of(AUDIO, COMMENTS)) {

    override val baseUrl: String
        get() = BASE_URL

    override val streamLHFactory: LinkHandlerFactory
        get() = BandcampStreamLinkHandlerFactory

    override val channelLHFactory: ListLinkHandlerFactory
        get() = BandcampChannelLinkHandlerFactory

    override val channelTabLHFactory: ListLinkHandlerFactory
        get() = BandcampChannelTabLinkHandlerFactory

    override val playlistLHFactory: ListLinkHandlerFactory
        get() = BandcampPlaylistLinkHandlerFactory

    override val searchQHFactory: SearchQueryHandlerFactory
        get() = BandcampSearchQueryHandlerFactory

    override val commentsLHFactory: ListLinkHandlerFactory
        get() = BandcampCommentsLinkHandlerFactory

    override fun getSearchExtractor(queryHandler: SearchQueryHandler): SearchExtractor =
        BandcampSearchExtractor(this, queryHandler)

    override val suggestionExtractor: SuggestionExtractor
        get() = BandcampSuggestionExtractor(this)

    override val subscriptionExtractor: SubscriptionExtractor?
        get() = null

    @get:Throws(ExtractionException::class)
    override val kioskList: KioskList
        get() {
            val kioskList = KioskList(this)
            val h: ListLinkHandlerFactory = BandcampFeaturedLinkHandlerFactory

            try {
                kioskList.addKioskEntry(
                    { _, _, kioskId ->
                        BandcampFeaturedExtractor(this, h.fromUrl(FEATURED_API_URL), kioskId)
                    },
                    h,
                    KIOSK_FEATURED,
                )

                kioskList.addKioskEntry(
                    { _, _, kioskId ->
                        BandcampRadioExtractor(this, h.fromUrl(RADIO_API_URL), kioskId)
                    },
                    h,
                    KIOSK_RADIO,
                )

                kioskList.setDefaultKiosk(KIOSK_FEATURED)
            } catch (e: Exception) {
                throw ExtractionException(e)
            }

            return kioskList
        }

    override fun getChannelExtractor(linkHandler: ListLinkHandler): ChannelExtractor =
        BandcampChannelExtractor(this, linkHandler)

    override fun getChannelTabExtractor(linkHandler: ListLinkHandler): ChannelTabExtractor =
        if (linkHandler is ReadyChannelTabListLinkHandler) {
            linkHandler.getChannelTabExtractor(this)
        } else {
            BandcampChannelTabExtractor(this, linkHandler)
        }

    override fun getPlaylistExtractor(linkHandler: ListLinkHandler): PlaylistExtractor =
        BandcampPlaylistExtractor(this, linkHandler)

    override fun getStreamExtractor(linkHandler: LinkHandler): StreamExtractor =
        if (BandcampExtractorHelper.isRadioUrl(linkHandler.url)) {
            BandcampRadioStreamExtractor(this, linkHandler)
        } else {
            BandcampStreamExtractor(this, linkHandler)
        }

    override fun getCommentsExtractor(linkHandler: ListLinkHandler): CommentsExtractor =
        BandcampCommentsExtractor(this, linkHandler)
}

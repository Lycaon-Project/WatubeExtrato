package org.schabi.newpipe.extractor.services.soundcloud

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
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.services.soundcloud.extractors.SoundcloudChannelExtractor
import org.schabi.newpipe.extractor.services.soundcloud.extractors.SoundcloudChannelTabExtractor
import org.schabi.newpipe.extractor.services.soundcloud.extractors.SoundcloudChartsExtractor
import org.schabi.newpipe.extractor.services.soundcloud.extractors.SoundcloudCommentsExtractor
import org.schabi.newpipe.extractor.services.soundcloud.extractors.SoundcloudPlaylistExtractor
import org.schabi.newpipe.extractor.services.soundcloud.extractors.SoundcloudSearchExtractor
import org.schabi.newpipe.extractor.services.soundcloud.extractors.SoundcloudStreamExtractor
import org.schabi.newpipe.extractor.services.soundcloud.extractors.SoundcloudSubscriptionExtractor
import org.schabi.newpipe.extractor.services.soundcloud.extractors.SoundcloudSuggestionExtractor
import org.schabi.newpipe.extractor.services.soundcloud.linkHandler.SoundcloudChannelLinkHandlerFactory
import org.schabi.newpipe.extractor.services.soundcloud.linkHandler.SoundcloudChannelTabLinkHandlerFactory
import org.schabi.newpipe.extractor.services.soundcloud.linkHandler.SoundcloudChartsLinkHandlerFactory
import org.schabi.newpipe.extractor.services.soundcloud.linkHandler.SoundcloudCommentsLinkHandlerFactory
import org.schabi.newpipe.extractor.services.soundcloud.linkHandler.SoundcloudPlaylistLinkHandlerFactory
import org.schabi.newpipe.extractor.services.soundcloud.linkHandler.SoundcloudSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.services.soundcloud.linkHandler.SoundcloudStreamLinkHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.subscription.SubscriptionExtractor
import java.util.EnumSet

public open class SoundcloudService(id: Int) :
    StreamingService(id, "SoundCloud", EnumSet.of(AUDIO, COMMENTS)) {

    override val baseUrl: String
        get() = "https://soundcloud.com"

    override val searchQHFactory: SearchQueryHandlerFactory
        get() = SoundcloudSearchQueryHandlerFactory

    override val streamLHFactory: LinkHandlerFactory
        get() = SoundcloudStreamLinkHandlerFactory

    override val channelLHFactory: ListLinkHandlerFactory
        get() = SoundcloudChannelLinkHandlerFactory

    override val channelTabLHFactory: ListLinkHandlerFactory
        get() = SoundcloudChannelTabLinkHandlerFactory

    override val playlistLHFactory: ListLinkHandlerFactory
        get() = SoundcloudPlaylistLinkHandlerFactory

    // Country selector here: https://soundcloud.com/charts/top?genre=all-music
    override val supportedCountries: List<ContentCountry>
        get() = SUPPORTED_COUNTRIES

    override fun getStreamExtractor(linkHandler: LinkHandler): StreamExtractor =
        SoundcloudStreamExtractor(this, linkHandler)

    override fun getChannelExtractor(linkHandler: ListLinkHandler): ChannelExtractor =
        SoundcloudChannelExtractor(this, linkHandler)

    override fun getChannelTabExtractor(linkHandler: ListLinkHandler): ChannelTabExtractor =
        SoundcloudChannelTabExtractor(this, linkHandler)

    override fun getPlaylistExtractor(linkHandler: ListLinkHandler): PlaylistExtractor =
        SoundcloudPlaylistExtractor(this, linkHandler)

    override fun getSearchExtractor(queryHandler: SearchQueryHandler): SearchExtractor =
        SoundcloudSearchExtractor(this, queryHandler)

    override val suggestionExtractor: SoundcloudSuggestionExtractor
        get() = SoundcloudSuggestionExtractor(this)

    @get:Throws(ExtractionException::class)
    override val kioskList: KioskList
        get() {
            val list = KioskList(this)
            val h = SoundcloudChartsLinkHandlerFactory

            // add kiosks here e.g.:
            try {
                list.addKioskEntry(
                    { _, url, id -> SoundcloudChartsExtractor(this, h.fromUrl(url), id) },
                    h,
                    KIOSK_NEW_AND_HOT,
                )
                list.setDefaultKiosk(KIOSK_NEW_AND_HOT)
            } catch (e: Exception) {
                throw ExtractionException(e)
            }

            return list
        }

    override val subscriptionExtractor: SubscriptionExtractor
        get() = SoundcloudSubscriptionExtractor(this)

    override val commentsLHFactory: ListLinkHandlerFactory
        get() = SoundcloudCommentsLinkHandlerFactory

    @Throws(ExtractionException::class)
    override fun getCommentsExtractor(linkHandler: ListLinkHandler): CommentsExtractor =
        SoundcloudCommentsExtractor(this, linkHandler)

    private companion object {
        const val KIOSK_NEW_AND_HOT = "New & hot"

        val SUPPORTED_COUNTRIES: List<ContentCountry> =
            ContentCountry.listFrom("AU", "CA", "DE", "FR", "GB", "IE", "NL", "NZ", "US")
    }
}

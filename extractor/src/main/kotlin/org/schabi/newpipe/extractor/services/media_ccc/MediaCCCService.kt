/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.media_ccc

import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.StreamingService.ServiceInfo.MediaCapability.AUDIO
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
import org.schabi.newpipe.extractor.linkhandler.ReadyChannelTabListLinkHandler
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.services.media_ccc.extractors.MediaCCCChannelTabExtractor
import org.schabi.newpipe.extractor.services.media_ccc.extractors.MediaCCCConferenceExtractor
import org.schabi.newpipe.extractor.services.media_ccc.extractors.MediaCCCConferenceKiosk
import org.schabi.newpipe.extractor.services.media_ccc.extractors.MediaCCCLiveStreamExtractor
import org.schabi.newpipe.extractor.services.media_ccc.extractors.MediaCCCLiveStreamKiosk
import org.schabi.newpipe.extractor.services.media_ccc.extractors.MediaCCCParsingHelper
import org.schabi.newpipe.extractor.services.media_ccc.extractors.MediaCCCRecentKiosk
import org.schabi.newpipe.extractor.services.media_ccc.extractors.MediaCCCSearchExtractor
import org.schabi.newpipe.extractor.services.media_ccc.extractors.MediaCCCStreamExtractor
import org.schabi.newpipe.extractor.services.media_ccc.linkHandler.MediaCCCConferenceLinkHandlerFactory
import org.schabi.newpipe.extractor.services.media_ccc.linkHandler.MediaCCCConferencesListLinkHandlerFactory
import org.schabi.newpipe.extractor.services.media_ccc.linkHandler.MediaCCCLiveListLinkHandlerFactory
import org.schabi.newpipe.extractor.services.media_ccc.linkHandler.MediaCCCRecentListLinkHandlerFactory
import org.schabi.newpipe.extractor.services.media_ccc.linkHandler.MediaCCCSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.services.media_ccc.linkHandler.MediaCCCStreamLinkHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.subscription.SubscriptionExtractor
import org.schabi.newpipe.extractor.suggestion.SuggestionExtractor
import java.util.EnumSet

public open class MediaCCCService(id: Int) : StreamingService(id, "media.ccc.de", EnumSet.of(AUDIO, VIDEO)) {

    override fun getSearchExtractor(queryHandler: SearchQueryHandler): SearchExtractor =
        MediaCCCSearchExtractor(this, queryHandler)

    override val streamLHFactory: LinkHandlerFactory
        get() = MediaCCCStreamLinkHandlerFactory

    override val channelLHFactory: ListLinkHandlerFactory
        get() = MediaCCCConferenceLinkHandlerFactory

    // there is just one channel tab in MediaCCC, the one containing conferences, so there is
    // no need for a specific channel tab link handler, but we can just use the channel one
    override val channelTabLHFactory: ListLinkHandlerFactory
        get() = MediaCCCConferenceLinkHandlerFactory

    override val playlistLHFactory: ListLinkHandlerFactory?
        get() = null

    override val searchQHFactory: SearchQueryHandlerFactory
        get() = MediaCCCSearchQueryHandlerFactory

    @Throws(ExtractionException::class)
    override fun getStreamExtractor(linkHandler: LinkHandler): StreamExtractor =
        if (MediaCCCParsingHelper.isLiveStreamId(linkHandler.id)) {
            MediaCCCLiveStreamExtractor(this, linkHandler)
        } else {
            MediaCCCStreamExtractor(this, linkHandler)
        }

    @Throws(ExtractionException::class)
    override fun getChannelExtractor(linkHandler: ListLinkHandler): ChannelExtractor =
        MediaCCCConferenceExtractor(this, linkHandler)

    @Throws(ExtractionException::class)
    override fun getChannelTabExtractor(linkHandler: ListLinkHandler): ChannelTabExtractor =
        if (linkHandler is ReadyChannelTabListLinkHandler) {
            // conference data has already been fetched, let the ReadyChannelTabListLinkHandler
            // create a MediaCCCChannelTabExtractor with that data
            linkHandler.getChannelTabExtractor(this)
        } else {
            // conference data has not been fetched yet, so pass null instead
            MediaCCCChannelTabExtractor(this, linkHandler, null)
        }

    /** media.ccc.de has no playlists (the Java code returned `null`). */
    @Throws(ExtractionException::class)
    override fun getPlaylistExtractor(linkHandler: ListLinkHandler): PlaylistExtractor =
        throw ExtractionException("media.ccc.de has no playlists")

    /** media.ccc.de has no search suggestions: always an empty list, without any request. */
    override val suggestionExtractor: SuggestionExtractor
        get() = NoSuggestionExtractor(this)

    @get:Throws(ExtractionException::class)
    override val kioskList: KioskList
        get() {
            val list = KioskList(this)
            val conferencesLHF = MediaCCCConferencesListLinkHandlerFactory
            val recentLHF = MediaCCCRecentListLinkHandlerFactory
            val liveLHF = MediaCCCLiveListLinkHandlerFactory

            // add kiosks here e.g.:
            try {
                list.addKioskEntry(
                    { _, url, kioskId -> MediaCCCConferenceKiosk(this, conferencesLHF.fromUrl(url), kioskId) },
                    conferencesLHF,
                    MediaCCCConferenceKiosk.KIOSK_ID,
                )
                list.addKioskEntry(
                    { _, url, kioskId -> MediaCCCRecentKiosk(this, recentLHF.fromUrl(url), kioskId) },
                    recentLHF,
                    MediaCCCRecentKiosk.KIOSK_ID,
                )
                list.addKioskEntry(
                    { _, url, kioskId -> MediaCCCLiveStreamKiosk(this, liveLHF.fromUrl(url), kioskId) },
                    liveLHF,
                    MediaCCCLiveStreamKiosk.KIOSK_ID,
                )
                list.setDefaultKiosk(MediaCCCRecentKiosk.KIOSK_ID)
            } catch (e: Exception) {
                throw ExtractionException(e)
            }
            return list
        }

    override val subscriptionExtractor: SubscriptionExtractor?
        get() = null

    override val commentsLHFactory: ListLinkHandlerFactory?
        get() = null

    /** media.ccc.de has no comments (the Java code returned `null`). */
    @Throws(ExtractionException::class)
    override fun getCommentsExtractor(linkHandler: ListLinkHandler): CommentsExtractor =
        throw ExtractionException("media.ccc.de has no comments")

    override val baseUrl: String
        get() = "https://media.ccc.de"

    private class NoSuggestionExtractor(service: StreamingService) : SuggestionExtractor(service) {
        override fun suggestionList(query: String): List<String> = emptyList()
    }
}

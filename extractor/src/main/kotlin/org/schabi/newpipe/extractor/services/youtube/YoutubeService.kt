/*
 * Created by Christian Schabesberger on 23.08.15.
 *
 * Copyright (C) 2018 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * YoutubeService.kt is part of NewPipe Extractor.
 *
 * NewPipe Extractor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe Extractor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe Extractor.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.schabi.newpipe.extractor.services.youtube

import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.StreamingService.ServiceInfo.MediaCapability.AUDIO
import org.schabi.newpipe.extractor.StreamingService.ServiceInfo.MediaCapability.COMMENTS
import org.schabi.newpipe.extractor.StreamingService.ServiceInfo.MediaCapability.LIVE
import org.schabi.newpipe.extractor.StreamingService.ServiceInfo.MediaCapability.VIDEO
import org.schabi.newpipe.extractor.channel.ChannelExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor
import org.schabi.newpipe.extractor.comments.CommentsExtractor
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.feed.FeedExtractor
import org.schabi.newpipe.extractor.kiosk.KioskList
import org.schabi.newpipe.extractor.linkhandler.LinkHandler
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory
import org.schabi.newpipe.extractor.linkhandler.ReadyChannelTabListLinkHandler
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeChannelExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeChannelTabExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeCommentsExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeFeedExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeMixPlaylistExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeMusicSearchExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubePlaylistExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeSearchExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeSubscriptionExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeSuggestionExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.kiosk.YoutubeLiveExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.kiosk.YoutubeTrendingExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.kiosk.YoutubeTrendingGamingVideosExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.kiosk.YoutubeTrendingMoviesAndShowsTrailersExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.kiosk.YoutubeTrendingMusicExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.kiosk.YoutubeTrendingPodcastsEpisodesExtractor
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeChannelLinkHandlerFactory
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeChannelTabLinkHandlerFactory
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeCommentsLinkHandlerFactory
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeLiveLinkHandlerFactory
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubePlaylistLinkHandlerFactory
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeStreamLinkHandlerFactory
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeTrendingGamingVideosLinkHandlerFactory
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeTrendingLinkHandlerFactory
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeTrendingMoviesAndShowsTrailersLinkHandlerFactory
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeTrendingMusicLinkHandlerFactory
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeTrendingPodcastsEpisodesLinkHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.subscription.SubscriptionExtractor
import org.schabi.newpipe.extractor.suggestion.SuggestionExtractor
import java.util.EnumSet

public open class YoutubeService(id: Int) :
    StreamingService(id, "YouTube", EnumSet.of(AUDIO, VIDEO, LIVE, COMMENTS)) {

    override val baseUrl: String
        get() = "https://youtube.com"

    override val streamLHFactory: LinkHandlerFactory
        get() = YoutubeStreamLinkHandlerFactory

    override val channelLHFactory: ListLinkHandlerFactory
        get() = YoutubeChannelLinkHandlerFactory

    override val channelTabLHFactory: ListLinkHandlerFactory
        get() = YoutubeChannelTabLinkHandlerFactory

    override val playlistLHFactory: ListLinkHandlerFactory
        get() = YoutubePlaylistLinkHandlerFactory

    override val searchQHFactory: SearchQueryHandlerFactory
        get() = YoutubeSearchQueryHandlerFactory

    override fun getStreamExtractor(linkHandler: LinkHandler): StreamExtractor =
        YoutubeStreamExtractor(this, linkHandler)

    override fun getChannelExtractor(linkHandler: ListLinkHandler): ChannelExtractor =
        YoutubeChannelExtractor(this, linkHandler)

    override fun getChannelTabExtractor(linkHandler: ListLinkHandler): ChannelTabExtractor =
        if (linkHandler is ReadyChannelTabListLinkHandler) {
            linkHandler.getChannelTabExtractor(this)
        } else {
            YoutubeChannelTabExtractor(this, linkHandler)
        }

    override fun getPlaylistExtractor(linkHandler: ListLinkHandler): PlaylistExtractor =
        if (YoutubeParsingHelper.isYoutubeMixId(linkHandler.id)) {
            YoutubeMixPlaylistExtractor(this, linkHandler)
        } else {
            YoutubePlaylistExtractor(this, linkHandler)
        }

    override fun getSearchExtractor(queryHandler: SearchQueryHandler): SearchExtractor {
        val contentFilters = queryHandler.contentFilters

        return if (contentFilters.isNotEmpty() && contentFilters[0].startsWith("music_")) {
            YoutubeMusicSearchExtractor(this, queryHandler)
        } else {
            YoutubeSearchExtractor(this, queryHandler)
        }
    }

    override val suggestionExtractor: SuggestionExtractor
        get() = YoutubeSuggestionExtractor(this)

    @get:Throws(ExtractionException::class)
    override val kioskList: KioskList
        get() {
            val list = KioskList(this)
            val trendingLHF: ListLinkHandlerFactory = YoutubeTrendingLinkHandlerFactory
            val runningLivesLHF: ListLinkHandlerFactory = YoutubeLiveLinkHandlerFactory
            val trendingPodcastsEpisodesLHF: ListLinkHandlerFactory =
                YoutubeTrendingPodcastsEpisodesLinkHandlerFactory
            val trendingGamingVideosLHF: ListLinkHandlerFactory =
                YoutubeTrendingGamingVideosLinkHandlerFactory
            val trendingMoviesAndShowsLHF: ListLinkHandlerFactory =
                YoutubeTrendingMoviesAndShowsTrailersLinkHandlerFactory
            val trendingMusicLHF: ListLinkHandlerFactory = YoutubeTrendingMusicLinkHandlerFactory

            try {
                list.addKioskEntry(
                    { _, url, id -> YoutubeLiveExtractor(this, runningLivesLHF.fromUrl(url), id) },
                    runningLivesLHF,
                    YoutubeLiveLinkHandlerFactory.KIOSK_ID,
                )
                list.addKioskEntry(
                    { _, url, id ->
                        YoutubeTrendingPodcastsEpisodesExtractor(
                            this, trendingPodcastsEpisodesLHF.fromUrl(url), id,
                        )
                    },
                    trendingPodcastsEpisodesLHF,
                    YoutubeTrendingPodcastsEpisodesLinkHandlerFactory.KIOSK_ID,
                )
                list.addKioskEntry(
                    { _, url, id ->
                        YoutubeTrendingGamingVideosExtractor(this, trendingGamingVideosLHF.fromUrl(url), id)
                    },
                    trendingGamingVideosLHF,
                    YoutubeTrendingGamingVideosLinkHandlerFactory.KIOSK_ID,
                )
                list.addKioskEntry(
                    { _, url, id ->
                        YoutubeTrendingMoviesAndShowsTrailersExtractor(
                            this, trendingMoviesAndShowsLHF.fromUrl(url), id,
                        )
                    },
                    trendingMoviesAndShowsLHF,
                    YoutubeTrendingMoviesAndShowsTrailersLinkHandlerFactory.KIOSK_ID,
                )
                list.addKioskEntry(
                    { _, url, id -> YoutubeTrendingMusicExtractor(this, trendingMusicLHF.fromUrl(url), id) },
                    trendingMusicLHF,
                    YoutubeTrendingMusicLinkHandlerFactory.KIOSK_ID,
                )
                // Deprecated (i.e. removed from the interface of YouTube) since July 21, 2025
                list.addKioskEntry(
                    { _, url, id -> YoutubeTrendingExtractor(this, trendingLHF.fromUrl(url), id) },
                    trendingLHF,
                    YoutubeTrendingExtractor.KIOSK_ID,
                )
                list.setDefaultKiosk(YoutubeLiveLinkHandlerFactory.KIOSK_ID)
            } catch (e: Exception) {
                throw ExtractionException(e)
            }

            return list
        }

    override val subscriptionExtractor: SubscriptionExtractor
        get() = YoutubeSubscriptionExtractor(this)

    @Throws(ExtractionException::class)
    override fun getFeedExtractor(url: String): FeedExtractor =
        YoutubeFeedExtractor(this, channelLHFactory.fromUrl(url))

    override val commentsLHFactory: ListLinkHandlerFactory
        get() = YoutubeCommentsLinkHandlerFactory

    @Throws(ExtractionException::class)
    override fun getCommentsExtractor(linkHandler: ListLinkHandler): CommentsExtractor =
        YoutubeCommentsExtractor(this, linkHandler)

    // //////////////////////////////////////////////////////////////////////////
    // Localization
    // //////////////////////////////////////////////////////////////////////////

    override val supportedLocalizations: List<Localization>
        get() = SUPPORTED_LANGUAGES

    override val supportedCountries: List<ContentCountry>
        get() = SUPPORTED_COUNTRIES

    private companion object {
        // https://www.youtube.com/picker_ajax?action_language_json=1
        private val SUPPORTED_LANGUAGES: List<Localization> = Localization.listFrom(
            "en-GB",
            /*"af", "am", "ar", "az", "be", "bg", "bn", "bs", "ca", "cs", "da", "de",
            "el", "en", "en-GB", "es", "es-419", "es-US", "et", "eu", "fa", "fi", "fil", "fr",
            "fr-CA", "gl", "gu", "hi", "hr", "hu", "hy", "id", "is", "it", "iw", "ja",
            "ka", "kk", "km", "kn", "ko", "ky", "lo", "lt", "lv", "mk", "ml", "mn",
            "mr", "ms", "my", "ne", "nl", "no", "pa", "pl", "pt", "pt-PT", "ro", "ru",
            "si", "sk", "sl", "sq", "sr", "sr-Latn", "sv", "sw", "ta", "te", "th", "tr",
            "uk", "ur", "uz", "vi", "zh-CN", "zh-HK", "zh-TW", "zu"*/
        )

        // https://www.youtube.com/picker_ajax?action_country_json=1
        private val SUPPORTED_COUNTRIES: List<ContentCountry> = ContentCountry.listFrom(
            "DZ", "AR", "AU", "AT", "AZ", "BH", "BD", "BY", "BE", "BO", "BA", "BR", "BG", "KH",
            "CA", "CL", "CO", "CR", "HR", "CY", "CZ", "DK", "DO", "EC", "EG", "SV", "EE", "FI",
            "FR", "GE", "DE", "GH", "GR", "GT", "HN", "HK", "HU", "IS", "IN", "ID", "IQ", "IE",
            "IL", "IT", "JM", "JP", "JO", "KZ", "KE", "KW", "LA", "LV", "LB", "LY", "LI", "LT",
            "LU", "MY", "MT", "MX", "ME", "MA", "NP", "NL", "NZ", "NI", "NG", "MK", "NO", "OM",
            "PK", "PA", "PG", "PY", "PE", "PH", "PL", "PT", "PR", "QA", "RO", "RU", "SA", "SN",
            "RS", "SG", "SK", "SI", "ZA", "KR", "ES", "LK", "SE", "CH", "TW", "TZ", "TH", "TN",
            "TR", "UG", "UA", "AE", "GB", "US", "UY", "VE", "VN", "YE", "ZW",
        )
    }
}

/*
 * SPDX-FileCopyrightText: 2017-2018 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.linkHandler

import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory

// Link handlers of the YouTube kiosks. Each one maps to a single fixed page, so the ID is a
// constant and a link is accepted only when its host and path (and query, if any) match exactly.

private const val CHARTS_HOST = "charts.youtube.com"

/** Whether [url] is a YouTube or Invidious link to exactly [path] (and [query], if given). */
private fun isYoutubeLinkTo(url: String?, path: String, query: String? = null): Boolean {
    val parsed = YoutubeLinkGuards.parseUrlOrNull(url) ?: return false
    return YoutubeLinkGuards.isYoutubeOrInvidiousHttpUrl(parsed) &&
        parsed.path == path &&
        (query == null || parsed.query == query)
}

public object YoutubeTrendingLinkHandlerFactory : ListLinkHandlerFactory() {

    private const val PATH = "/feed/trending"

    @Throws(ParsingException::class)
    override fun getUrl(id: String?, contentFilter: List<String>?, sortFilter: String?): String =
        "https://www.youtube.com$PATH"

    @Throws(ParsingException::class)
    override fun getId(url: String?): String = "Trending"

    override fun onAcceptUrl(url: String?): Boolean = isYoutubeLinkTo(url, PATH)
}

public object YoutubeLiveLinkHandlerFactory : ListLinkHandlerFactory() {

    public const val KIOSK_ID: String = "live"
    private const val LIVE_CHANNEL_PATH = "/channel/UC4R8DWoMoI7CAwX8_LjQHig/livetab"
    private const val LIVE_CHANNEL_TAB_PARAMS = "ss=CKEK"

    @Throws(ParsingException::class)
    override fun getUrl(id: String?, contentFilter: List<String>?, sortFilter: String?): String =
        "https://www.youtube.com$LIVE_CHANNEL_PATH?$LIVE_CHANNEL_TAB_PARAMS"

    @Throws(ParsingException::class)
    override fun getId(url: String?): String = KIOSK_ID

    override fun onAcceptUrl(url: String?): Boolean =
        isYoutubeLinkTo(url, LIVE_CHANNEL_PATH, LIVE_CHANNEL_TAB_PARAMS)
}

public object YoutubeTrendingGamingVideosLinkHandlerFactory : ListLinkHandlerFactory() {

    public const val KIOSK_ID: String = "trending_gaming"
    private const val PATH = "/gaming/trending"

    @Throws(ParsingException::class)
    override fun getUrl(id: String?, contentFilter: List<String>?, sortFilter: String?): String =
        "https://www.youtube.com$PATH"

    @Throws(ParsingException::class)
    override fun getId(url: String?): String = KIOSK_ID

    override fun onAcceptUrl(url: String?): Boolean = isYoutubeLinkTo(url, PATH)
}

public object YoutubeTrendingPodcastsEpisodesLinkHandlerFactory : ListLinkHandlerFactory() {

    public const val KIOSK_ID: String = "trending_podcasts_episodes"
    private const val PATH = "/podcasts/popularepisodes"

    @Throws(ParsingException::class)
    override fun getUrl(id: String?, contentFilter: List<String>?, sortFilter: String?): String =
        "https://www.youtube.com$PATH"

    @Throws(ParsingException::class)
    override fun getId(url: String?): String = KIOSK_ID

    override fun onAcceptUrl(url: String?): Boolean = isYoutubeLinkTo(url, PATH)
}

public object YoutubeTrendingMusicLinkHandlerFactory : ListLinkHandlerFactory() {

    public const val KIOSK_ID: String = "trending_music"
    private const val PATH = "/charts/TrendingVideos"

    @Throws(ParsingException::class)
    override fun getUrl(id: String?, contentFilter: List<String>?, sortFilter: String?): String =
        "https://$CHARTS_HOST$PATH/RightNow"

    @Throws(ParsingException::class)
    override fun getId(url: String?): String = KIOSK_ID

    /** Also accepts the URLs without their `/RightNow` part. */
    @Throws(ParsingException::class)
    override fun onAcceptUrl(url: String?): Boolean {
        val parsed = YoutubeLinkGuards.parseUrlOrNull(url) ?: return false
        return YoutubeLinkGuards.isHttpUrlOnHost(parsed, CHARTS_HOST) &&
            parsed.path.startsWith(PATH)
    }
}

public object YoutubeTrendingMoviesAndShowsTrailersLinkHandlerFactory : ListLinkHandlerFactory() {

    public const val KIOSK_ID: String = "trending_movies_and_shows"
    private const val PATH = "/charts/TrendingTrailers"

    @Throws(ParsingException::class)
    override fun getUrl(id: String?, contentFilter: List<String>?, sortFilter: String?): String =
        "https://$CHARTS_HOST$PATH"

    @Throws(ParsingException::class)
    override fun getId(url: String?): String = KIOSK_ID

    @Throws(ParsingException::class)
    override fun onAcceptUrl(url: String?): Boolean {
        val parsed = YoutubeLinkGuards.parseUrlOrNull(url) ?: return false
        return YoutubeLinkGuards.isHttpUrlOnHost(parsed, CHARTS_HOST) && parsed.path == PATH
    }
}

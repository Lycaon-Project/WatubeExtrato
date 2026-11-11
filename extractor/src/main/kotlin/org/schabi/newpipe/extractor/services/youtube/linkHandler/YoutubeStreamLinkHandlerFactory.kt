/*
 * SPDX-FileCopyrightText: 2016-2018 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.linkHandler

import org.schabi.newpipe.extractor.exceptions.FoundAdException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeLinkGuards.hostKey
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeLinkGuards.queryValue
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeLinkGuards.requireVideoIdPrefix
import org.schabi.newpipe.extractor.utils.Utils
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

public object YoutubeStreamLinkHandlerFactory : LinkHandlerFactory() {

    private const val AD_HOST = "googleads.g.doubleclick.net"
    private const val NOCOOKIE_HOST = "WWW.YOUTUBE-NOCOOKIE.COM"
    private const val EMBED_PATH = "embed/"

    private val VND_SCHEMES = setOf("vnd.youtube", "vnd.youtube.launch")
    private val SUBPATHS = listOf("embed/", "live/", "shorts/", "watch/", "v/", "w/")
    private val YOUTUBE_HOSTS =
        setOf("YOUTUBE.COM", "WWW.YOUTUBE.COM", "M.YOUTUBE.COM", "MUSIC.YOUTUBE.COM")
    private val SHORT_LINK_HOSTS = setOf("Y2U.BE", "YOUTU.BE")
    private val INVIDIOUS_HOSTS = setOf(
        "REDIRECT.INVIDIOUS.IO", "YEWTU.BE", "PIPED.KAVIN.ROCKS", "PIPED.VIDEO",
        "INV.NADEKO.NET", "INVIDIOUS.NERDVPN.DE", "YT.CHOCOLATEMOO53.COM",
        "INVIDIOUS.TIEKOETTER.COM", "INVIDIOUS.F5.SI",
    )

    @JvmStatic
    public fun getInstance(): YoutubeStreamLinkHandlerFactory = this

    @Throws(ParsingException::class)
    override fun getUrl(id: String?): String =
        "https://www.youtube.com/watch?v=${YoutubeLinkGuards.requireVideoId(id)}"

    @Throws(ParsingException::class)
    override fun getId(url: String?): String {
        var urlString = YoutubeLinkGuards.requireBoundedUrl(url)

        val schemeSpecificPart = vndSchemeSpecificPartOrNull(urlString)
        if (schemeSpecificPart != null) {
            if (!schemeSpecificPart.startsWith("//")) {
                return requireVideoIdPrefix(schemeSpecificPart)
            }
            YoutubeLinkGuards.videoIdPrefixOrNull(schemeSpecificPart.substring(2))
                ?.let { return it }
            urlString = "https:$schemeSpecificPart"
        }

        val parsed = YoutubeLinkGuards.parseUrl(urlString)
        if (!isAcceptedHttpUrl(parsed)) {
            if (parsed.host.equals(AD_HOST, ignoreCase = true)) {
                throw FoundAdException("Found an ad link")
            }
            throw ParsingException("The URL is not a YouTube URL")
        }
        if (YoutubePlaylistLinkHandlerFactory.acceptUrl(urlString)) {
            throw ParsingException("The URL is a playlist, not a video")
        }

        return idFromHost(parsed, parsed.path.removePrefix("/"))
            ?: throw ParsingException("The URL does not contain a video ID")
    }

    @Throws(FoundAdException::class)
    override fun onAcceptUrl(url: String?): Boolean = YoutubeLinkGuards.accepts { getId(url) }

    private fun vndSchemeSpecificPartOrNull(url: String): String? {
        val uri = try {
            URI(url)
        } catch (_: URISyntaxException) {
            return null
        }
        return uri.schemeSpecificPart.takeIf { uri.scheme in VND_SCHEMES }
    }

    private fun isAcceptedHttpUrl(url: URL): Boolean = Utils.isHTTP(url) && (
        YoutubeParsingHelper.isYoutubeURL(url) ||
            YoutubeParsingHelper.isYoutubeServiceURL(url) ||
            YoutubeParsingHelper.isInvidiousURL(url) ||
            YoutubeParsingHelper.isY2ubeURL(url)
        )

    @Throws(ParsingException::class)
    private fun idFromHost(url: URL, path: String): String? = when (hostKey(url)) {
        NOCOOKIE_HOST ->
            if (path.startsWith(EMBED_PATH)) {
                requireVideoIdPrefix(path.substring(EMBED_PATH.length))
            } else {
                null
            }
        in YOUTUBE_HOSTS -> idFromYoutubePath(url, path)
        in SHORT_LINK_HOSTS -> requireVideoIdPrefix(queryValue(url, "v") ?: path)
        in INVIDIOUS_HOSTS -> idFromInvidiousPath(url, path)
        else -> null
    }

    @Throws(ParsingException::class)
    private fun idFromYoutubePath(url: URL, path: String): String {
        if (path == "attribution_link") {
            return idFromAttributionLink(url)
        }
        return idFromSubpaths(path) ?: requireVideoIdPrefix(queryValue(url, "v"))
    }

    /**
     * `attribution_link?u=/watch?v=ID`: the `u` value must stay a path on youtube.com, so that
     * `u=@evil.example/…` cannot turn the rebuilt link into one on another host.
     */
    @Throws(ParsingException::class)
    private fun idFromAttributionLink(url: URL): String {
        val target = queryValue(url, "u")
        if (target == null || !target.startsWith("/") || target.startsWith("//")) {
            throw ParsingException("The attribution link has no valid target")
        }
        val decoded = YoutubeLinkGuards.parseUrl("https://www.youtube.com$target")
        return requireVideoIdPrefix(queryValue(decoded, "v"))
    }

    @Throws(ParsingException::class)
    private fun idFromInvidiousPath(url: URL, path: String): String {
        if (path == "watch") {
            queryValue(url, "v")?.let { return requireVideoIdPrefix(it) }
        }
        idFromSubpaths(path)?.let { return it }
        queryValue(url, "v")?.let { return requireVideoIdPrefix(it) }
        return requireVideoIdPrefix(path)
    }

    @Throws(ParsingException::class)
    private fun idFromSubpaths(path: String): String? = SUBPATHS
        .firstOrNull { path.startsWith(it) }
        ?.let { requireVideoIdPrefix(path.substring(it.length)) }
}

/** Comments are fetched from the video page, so links and IDs are those of the videos. */
public object YoutubeCommentsLinkHandlerFactory : ListLinkHandlerFactory() {

    @JvmStatic
    public fun getInstance(): YoutubeCommentsLinkHandlerFactory = this

    @Throws(ParsingException::class)
    override fun getUrl(id: String?): String = YoutubeStreamLinkHandlerFactory.getUrl(id)

    @Throws(ParsingException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
    ): String = getUrl(id)

    @Throws(ParsingException::class)
    override fun getId(url: String?): String = YoutubeStreamLinkHandlerFactory.getId(url)

    @Throws(FoundAdException::class)
    override fun onAcceptUrl(url: String?): Boolean =
        YoutubeStreamLinkHandlerFactory.onAcceptUrl(url)
}

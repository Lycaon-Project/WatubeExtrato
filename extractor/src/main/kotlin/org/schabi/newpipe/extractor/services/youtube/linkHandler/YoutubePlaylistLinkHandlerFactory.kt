/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.linkHandler

import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.LinkHandler
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeLinkGuards.queryValue

public object YoutubePlaylistLinkHandlerFactory : ListLinkHandlerFactory() {

    private val PLAYLIST_PATHS = setOf("/watch", "/playlist")

    /** Playlist IDs found in links: at least 10 characters, unlike `WL` or `LL` built in code. */
    private val LINK_PLAYLIST_ID = Regex("[a-zA-Z0-9_-]{10,}")

    @JvmStatic
    public fun getInstance(): YoutubePlaylistLinkHandlerFactory = this

    @Throws(ParsingException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
    ): String = "https://www.youtube.com/playlist?list=${YoutubeLinkGuards.requirePlaylistId(id)}"

    @Throws(ParsingException::class)
    override fun getId(url: String?): String {
        val parsed = YoutubeLinkGuards.parseUrl(url)
        if (!YoutubeLinkGuards.isYoutubeOrInvidiousHttpUrl(parsed)) {
            throw ParsingException("The URL is not a YouTube URL")
        }
        if (parsed.path !in PLAYLIST_PATHS) {
            throw ParsingException("The URL is neither a video nor a playlist URL")
        }
        val listId = queryValue(parsed, "list")
            ?: throw ParsingException("The URL does not include a playlist")
        if (!LINK_PLAYLIST_ID.matches(listId)) {
            throw ParsingException("The playlist ID of the URL does not match the list pattern")
        }
        return listId
    }

    override fun onAcceptUrl(url: String?): Boolean = YoutubeLinkGuards.accepts { getId(url) }

    /**
     * For a mix (auto-generated playlist) link, returns a [ListLinkHandler] whose URL is
     * `https://www.youtube.com/watch?v=videoId&list=playlistId`; otherwise defers to the parent.
     *
     * Only YouTube or Invidious links are accepted, and both IDs are validated before being
     * placed in the rebuilt URL, so a link cannot inject other query parameters.
     */
    @Throws(ParsingException::class)
    override fun fromUrl(url: String?): ListLinkHandler {
        val parsed = YoutubeLinkGuards.parseUrl(url)
        val listId = queryValue(parsed, "list")
        if (listId == null || !YoutubeParsingHelper.isYoutubeMixId(listId)) {
            return super.fromUrl(url)
        }
        if (!YoutubeLinkGuards.isYoutubeOrInvidiousHttpUrl(parsed)) {
            throw ParsingException("The URL is not a YouTube URL")
        }
        val mixId = YoutubeLinkGuards.requirePlaylistId(listId)
        // An invalid v is rejected here instead of being forwarded to YouTube
        val videoId = queryValue(parsed, "v")
            ?.let { YoutubeLinkGuards.requireVideoIdPrefix(it) }
            ?: YoutubeParsingHelper.extractVideoIdFromMixId(mixId)
        val newUrl = "https://www.youtube.com/watch?v=$videoId&list=$mixId"
        // parseUrl() succeeded, so the link is present and bounded
        val originalUrl = YoutubeLinkGuards.requireBoundedUrl(url)
        return ListLinkHandler(LinkHandler(originalUrl, newUrl, mixId))
    }
}

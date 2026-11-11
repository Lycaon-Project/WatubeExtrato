/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.peertube.linkHandler

import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory
import org.schabi.newpipe.extractor.services.peertube.PeertubeLinkGuards
import org.schabi.newpipe.extractor.utils.Parser
import java.util.regex.Pattern

public object PeertubePlaylistLinkHandlerFactory : ListLinkHandlerFactory() {

    private val ID_PATTERN: Pattern = Pattern.compile("(/videos/watch/playlist/|/w/p/)([^/?&#]*)")
    private val API_ID_PATTERN: Pattern = Pattern.compile("/video-playlists/([^/?&#]*)")

    @JvmStatic
    public fun getInstance(): PeertubePlaylistLinkHandlerFactory = this

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
    ): String = getUrl(id, contentFilter, sortFilter, ServiceList.PeerTube.baseUrl)

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
        baseUrl: String?,
    ): String = PeertubeLinkGuards.requireBaseUrl(baseUrl) + "/api/v1/video-playlists/" +
        PeertubeLinkGuards.requireVideoId(id)

    /** The ID is checked to be a playlist ID (UUID or short UUID) before it is returned. */
    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getId(url: String?): String {
        val link = PeertubeLinkGuards.requireBoundedUrl(url)
        val id = try {
            Parser.matchGroup(ID_PATTERN, link, 2)
        } catch (_: ParsingException) {
            // might also be an API url, no reason to throw an exception here
            Parser.matchGroup1(API_ID_PATTERN, link)
        }
        return PeertubeLinkGuards.requireVideoId(id)
    }

    override fun onAcceptUrl(url: String?): Boolean {
        if (PeertubeLinkGuards.parseWebUrlOrNull(url) == null) {
            return false
        }
        return try {
            getId(url)
            true
        } catch (_: ParsingException) {
            false
        }
    }
}

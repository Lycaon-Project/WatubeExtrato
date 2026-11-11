/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.peertube.linkHandler

import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.exceptions.FoundAdException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory
import org.schabi.newpipe.extractor.services.peertube.PeertubeLinkGuards
import org.schabi.newpipe.extractor.utils.Parser
import java.util.regex.Pattern

public object PeertubeStreamLinkHandlerFactory : LinkHandlerFactory() {

    // we exclude p/ because /w/p/ is playlist, not video
    private val ID_PATTERN: Pattern = Pattern.compile("(/w/|(/videos/(watch/|embed/)?))(?!p/)([^/?&#]*)")

    public const val VIDEO_API_ENDPOINT: String = "/api/v1/videos/"

    // From PeerTube 3.3.0, the default path is /w/.
    // We still use /videos/watch/ for compatibility reasons:
    // /videos/watch/ is still accepted by >=3.3.0 but /w/ isn't by <3.3.0
    private const val VIDEO_PATH = "/videos/watch/"

    @JvmStatic
    public fun getInstance(): PeertubeStreamLinkHandlerFactory = this

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(id: String?): String = getUrl(id, ServiceList.PeerTube.baseUrl)

    /**
     * The base URL is that of the link (another instance than the user's one is allowed, PeerTube
     * being federated), but must be the bare https address of an instance.
     */
    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(id: String?, baseUrl: String?): String =
        PeertubeLinkGuards.requireBaseUrl(baseUrl) + VIDEO_PATH + PeertubeLinkGuards.requireVideoId(id)

    /** The ID is checked to be a video ID (UUID, short UUID) before it is returned. */
    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getId(url: String?): String {
        // the id is read from the path only: a query or a fragment cannot make a link a video
        val path = PeertubeLinkGuards.parseWebUrlOrNull(PeertubeLinkGuards.requireBoundedUrl(url))?.path
            ?: throw ParsingException("Invalid PeerTube link")
        return PeertubeLinkGuards.requireVideoId(Parser.matchGroup(ID_PATTERN, path, 4))
    }

    @Throws(FoundAdException::class)
    override fun onAcceptUrl(url: String?): Boolean {
        if (url == null || url.contains("/playlist/") || PeertubeLinkGuards.parseWebUrlOrNull(url) == null) {
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

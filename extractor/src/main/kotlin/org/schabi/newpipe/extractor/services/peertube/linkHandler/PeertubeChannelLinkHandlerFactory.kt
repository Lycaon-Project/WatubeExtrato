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

public object PeertubeChannelLinkHandlerFactory : ListLinkHandlerFactory() {

    private val ID_PATTERN = Regex("((accounts|a)|(video-channels|c))/([^/?&#]*)")
    private val ID_URL_PATTERN: Pattern = Pattern.compile("/((accounts|a)|(video-channels|c))/([^/?&#]*)")
    private val CHANNEL_PATHS = listOf("/accounts/", "/a/", "/video-channels/", "/c/")

    public const val API_ENDPOINT: String = "/api/v1/"

    @JvmStatic
    public fun getInstance(): PeertubeChannelLinkHandlerFactory = this

    /** The ID is checked to be `accounts/name` or `video-channels/name` before it is returned. */
    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getId(url: String?): String {
        // the id is read from the path only: a query or a fragment cannot make a link a channel
        val path = PeertubeLinkGuards.parseWebUrlOrNull(PeertubeLinkGuards.requireBoundedUrl(url))?.path
            ?: throw ParsingException("Invalid PeerTube link")
        return PeertubeLinkGuards.requireActorId(fixId(Parser.matchGroup(ID_URL_PATTERN, path, 0)))
    }

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
    ): String = getUrl(id, contentFilter, sortFilter, ServiceList.PeerTube.baseUrl)

    /**
     * The base URL is that of the link (another instance than the user's one is allowed, PeerTube
     * being federated), but must be the bare https address of an instance.
     */
    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
        baseUrl: String?,
    ): String {
        val instance = PeertubeLinkGuards.requireBaseUrl(baseUrl)
        val channelId = id ?: throw ParsingException("No channel ID given")
        return if (ID_PATTERN.matches(channelId)) {
            instance + "/" + PeertubeLinkGuards.requireActorId(fixId(channelId))
        } else {
            // This is needed for compatibility with older versions were we didn't support
            // video channels yet
            instance + "/accounts/" + PeertubeLinkGuards.requireActorName(channelId)
        }
    }

    override fun onAcceptUrl(url: String?): Boolean {
        if (PeertubeLinkGuards.parseWebUrlOrNull(url) == null) {
            return false
        }
        // parseWebUrlOrNull() succeeded, so the link is not null
        val link = url.orEmpty()
        return CHANNEL_PATHS.any { link.contains(it) }
    }

    /**
     * Fix id
     *
     * a/:accountName and c/:channelName ids are supported
     * by the PeerTube web client (>= v3.3.0)
     * but not by the API.
     *
     * @param id the id to fix
     * @return the fixed id
     */
    private fun fixId(id: String): String {
        val cleanedId = id.removePrefix("/")
        return when {
            cleanedId.startsWith("a/") -> "accounts" + cleanedId.substring(1)
            cleanedId.startsWith("c/") -> "video-channels" + cleanedId.substring(1)
            else -> cleanedId
        }
    }
}

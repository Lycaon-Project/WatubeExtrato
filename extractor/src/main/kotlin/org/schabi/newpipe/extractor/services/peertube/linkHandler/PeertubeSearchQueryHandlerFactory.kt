/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.peertube.linkHandler

import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory
import org.schabi.newpipe.extractor.services.peertube.PeertubeLinkGuards
import org.schabi.newpipe.extractor.utils.Utils

public object PeertubeSearchQueryHandlerFactory : SearchQueryHandlerFactory() {

    public const val VIDEOS: String = "videos"
    public const val SEPIA_VIDEOS: String = "sepia_videos" // sepia is the global index
    public const val PLAYLISTS: String = "playlists"
    public const val CHANNELS: String = "channels"
    public const val SEPIA_BASE_URL: String = "https://sepiasearch.org"
    public const val SEARCH_ENDPOINT_PLAYLISTS: String = "/api/v1/search/video-playlists"
    public const val SEARCH_ENDPOINT_VIDEOS: String = "/api/v1/search/videos"
    public const val SEARCH_ENDPOINT_CHANNELS: String = "/api/v1/search/video-channels"

    @JvmStatic
    public fun getInstance(): PeertubeSearchQueryHandlerFactory = this

    /**
     * @param id the search query (named like the parameter of [ListLinkHandlerFactory.getUrl])
     */
    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
    ): String {
        val baseUrl = if (contentFilter?.firstOrNull()?.startsWith("sepia_") == true) {
            SEPIA_BASE_URL
        } else {
            ServiceList.PeerTube.baseUrl
        }
        return getUrl(id, contentFilter, sortFilter, baseUrl)
    }

    /**
     * The query is percent-encoded, so it can only ever be the value of the search parameter.
     *
     * @param id the search query (named like the parameter of [ListLinkHandlerFactory.getUrl])
     */
    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
        baseUrl: String?,
    ): String {
        val query = id ?: throw ParsingException("No search query given")
        val endpoint = when (contentFilter?.firstOrNull()) {
            null, VIDEOS, SEPIA_VIDEOS -> SEARCH_ENDPOINT_VIDEOS
            CHANNELS -> SEARCH_ENDPOINT_CHANNELS
            else -> SEARCH_ENDPOINT_PLAYLISTS
        }
        return PeertubeLinkGuards.requireBaseUrl(baseUrl) + endpoint + "?search=" +
            Utils.encodeUrlUtf8(query)
    }

    override val availableContentFilter: Array<String>
        get() = arrayOf(
            VIDEOS,
            PLAYLISTS,
            CHANNELS,
            SEPIA_VIDEOS,
        )
}

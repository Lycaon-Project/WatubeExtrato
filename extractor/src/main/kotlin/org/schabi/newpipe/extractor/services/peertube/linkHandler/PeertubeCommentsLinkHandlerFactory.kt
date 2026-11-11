/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.peertube.linkHandler

import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.exceptions.FoundAdException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory
import org.schabi.newpipe.extractor.services.peertube.PeertubeLinkGuards

public object PeertubeCommentsLinkHandlerFactory : ListLinkHandlerFactory() {

    private const val VIDEOS_API_ENDPOINT = "/api/v1/videos/"
    private const val COMMENT_THREADS = "/comment-threads"

    @JvmStatic
    public fun getInstance(): PeertubeCommentsLinkHandlerFactory = this

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getId(url: String?): String =
        PeertubeStreamLinkHandlerFactory.getId(url) // the same id is needed

    @Throws(FoundAdException::class)
    override fun onAcceptUrl(url: String?): Boolean {
        if (PeertubeLinkGuards.parseWebUrlOrNull(url) == null) {
            return false
        }
        // parseWebUrlOrNull() succeeded, so the link is not null
        val link = url.orEmpty()
        return link.contains("/videos/") || link.contains("/w/")
    }

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
    ): String = PeertubeLinkGuards.requireBaseUrl(baseUrl) + VIDEOS_API_ENDPOINT +
        PeertubeLinkGuards.requireVideoId(id) + COMMENT_THREADS
}

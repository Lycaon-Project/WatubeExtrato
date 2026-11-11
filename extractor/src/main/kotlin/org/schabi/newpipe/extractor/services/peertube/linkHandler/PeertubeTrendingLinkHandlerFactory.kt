/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.peertube.linkHandler

import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory
import org.schabi.newpipe.extractor.services.peertube.PeertubeLinkGuards
import java.util.Collections

public object PeertubeTrendingLinkHandlerFactory : ListLinkHandlerFactory() {

    public const val KIOSK_TRENDING: String = "Trending"
    public const val KIOSK_MOST_LIKED: String = "Most liked"
    public const val KIOSK_RECENT: String = "Recently added"
    public const val KIOSK_LOCAL: String = "Local"

    @JvmField
    public val KIOSK_MAP: Map<String, String> = Collections.unmodifiableMap(
        mapOf(
            KIOSK_TRENDING to "%s/api/v1/videos?sort=-trending",
            KIOSK_MOST_LIKED to "%s/api/v1/videos?sort=-likes",
            KIOSK_RECENT to "%s/api/v1/videos?sort=-publishedAt",
            KIOSK_LOCAL to "%s/api/v1/videos?sort=-publishedAt&isLocal=true",
        ),
    )

    /** Web paths of the kiosks, checked in this order. */
    private val KIOSK_PATHS = listOf(
        "/videos/trending" to KIOSK_TRENDING,
        "/videos/most-liked" to KIOSK_MOST_LIKED,
        "/videos/recently-added" to KIOSK_RECENT,
        "/videos/local" to KIOSK_LOCAL,
    )

    @JvmStatic
    public fun getInstance(): PeertubeTrendingLinkHandlerFactory = this

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
    ): String {
        val template = KIOSK_MAP[id] ?: throw ParsingException("Unknown PeerTube kiosk")
        return String.format(template, PeertubeLinkGuards.requireBaseUrl(baseUrl))
    }

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getId(url: String?): String {
        val cleanUrl = PeertubeLinkGuards.requireBoundedUrl(url).replace(ServiceList.PeerTube.baseUrl, "%s")
        KIOSK_PATHS.firstOrNull { (path, _) -> cleanUrl.contains(path) }?.let { return it.second }
        return KIOSK_MAP.entries.firstOrNull { it.value == cleanUrl }?.key
            ?: throw ParsingException("no id found for this url")
    }

    override fun onAcceptUrl(url: String?): Boolean {
        if (PeertubeLinkGuards.parseWebUrlOrNull(url) == null) {
            return false
        }
        // parseWebUrlOrNull() succeeded, so the link is not null
        val link = url.orEmpty()
        return link.contains("/videos?") || KIOSK_PATHS.any { (path, _) -> link.contains(path) }
    }
}

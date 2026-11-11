/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.peertube.linkHandler

import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.UnsupportedTabException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory

public object PeertubeChannelTabLinkHandlerFactory : ListLinkHandlerFactory() {

    @JvmStatic
    public fun getInstance(): PeertubeChannelTabLinkHandlerFactory = this

    @JvmStatic
    @Throws(UnsupportedTabException::class)
    public fun getUrlSuffix(tab: String): String = when (tab) {
        ChannelTabs.VIDEOS -> "/videos"
        ChannelTabs.CHANNELS -> "/video-channels" // only available on accounts
        ChannelTabs.PLAYLISTS -> "/video-playlists" // only available on channels
        else -> throw UnsupportedTabException(tab)
    }

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getId(url: String?): String = PeertubeChannelLinkHandlerFactory.getId(url)

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
    ): String = PeertubeChannelLinkHandlerFactory.getUrl(id) + getUrlSuffix(firstTab(contentFilter))

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
        baseUrl: String?,
    ): String = PeertubeChannelLinkHandlerFactory.getUrl(id, null, null, baseUrl) +
        getUrlSuffix(firstTab(contentFilter))

    @Throws(ParsingException::class)
    override fun onAcceptUrl(url: String?): Boolean = PeertubeChannelLinkHandlerFactory.onAcceptUrl(url)

    override val availableContentFilter: Array<String>
        get() = arrayOf(
            ChannelTabs.VIDEOS,
            ChannelTabs.CHANNELS,
            ChannelTabs.PLAYLISTS,
        )

    @Throws(ParsingException::class)
    private fun firstTab(contentFilter: List<String>?): String =
        contentFilter?.firstOrNull() ?: throw ParsingException("No channel tab given")
}

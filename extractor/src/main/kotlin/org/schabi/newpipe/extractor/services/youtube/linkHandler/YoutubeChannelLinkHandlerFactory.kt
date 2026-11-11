/*
 * SPDX-FileCopyrightText: 2016-2018 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.linkHandler

import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.UnsupportedTabException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory

public object YoutubeChannelLinkHandlerFactory : ListLinkHandlerFactory() {

    private val EXCLUDED_SEGMENTS = Regex(
        "playlist|watch|attribution_link|watch_popup|embed|feed|select_site|account|" +
            "reporthistory|redirect|results|shorts|live|hashtag|channel|user|c|signin|logout|" +
            "upload|premium|t|s",
    )
    private val ID_PREFIXES = listOf("user/", "channel/", "c/")

    @JvmStatic
    public fun getInstance(): YoutubeChannelLinkHandlerFactory = this

    /**
     * Returns the URL to a channel from an ID.
     *
     * @param id the channel ID including e.g. `channel/`, or a channel path given by YouTube
     * such as `@handle/videos`
     * @return the URL to the channel
     */
    @Throws(ParsingException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
    ): String = "https://www.youtube.com/${YoutubeLinkGuards.requireChannelPath(id)}"

    @Throws(ParsingException::class)
    override fun getId(url: String?): String {
        val parsed = YoutubeLinkGuards.parseUrl(url)
        if (!YoutubeLinkGuards.isYoutubeOrInvidiousHttpUrl(parsed)) {
            throw ParsingException("The URL given is not a YouTube URL")
        }
        val path = parsed.path.removePrefix("/")
        // Same segments as String.split("/") in Java, which drops trailing empty segments
        val segments = path.split("/").dropLastWhile { it.isEmpty() }
        val first = segments.firstOrNull().orEmpty()

        if (isHandle(first) || isCustomShortChannelUrl(segments)) {
            // youtube.com/@yourhandle and custom short channel URLs like youtube.com/yourname
            return YoutubeLinkGuards.requireChannelId(first)
        }
        if (ID_PREFIXES.none { path.startsWith(it) }) {
            throw ParsingException("The given URL is not a channel, a user or a handle URL")
        }
        val id = segments.getOrNull(1)
        if (id.isNullOrBlank()) {
            throw ParsingException("The given ID is not a YouTube channel or user ID")
        }
        return YoutubeLinkGuards.requireChannelId("$first/$id")
    }

    override fun onAcceptUrl(url: String?): Boolean = YoutubeLinkGuards.accepts { getId(url) }

    private fun isHandle(firstSegment: String): Boolean = firstSegment.startsWith("@")

    private fun isCustomShortChannelUrl(segments: List<String>): Boolean =
        segments.size == 1 && segments[0].isNotEmpty() && !EXCLUDED_SEGMENTS.matches(segments[0])
}

public object YoutubeChannelTabLinkHandlerFactory : ListLinkHandlerFactory() {

    @JvmStatic
    public fun getInstance(): YoutubeChannelTabLinkHandlerFactory = this

    @JvmStatic
    @Throws(UnsupportedTabException::class)
    public fun getUrlSuffix(tab: String): String = when (tab) {
        ChannelTabs.VIDEOS -> "/videos"
        ChannelTabs.SHORTS -> "/shorts"
        ChannelTabs.LIVESTREAMS -> "/streams"
        ChannelTabs.ALBUMS -> "/releases"
        ChannelTabs.PLAYLISTS -> "/playlists"
        ChannelTabs.COURSES -> "/courses"
        ChannelTabs.PODCASTS -> "/podcasts"
        else -> throw UnsupportedTabException(tab)
    }

    @Throws(ParsingException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
    ): String {
        val tab = contentFilter?.firstOrNull()
            ?: throw ParsingException("No channel tab given")
        return YoutubeChannelLinkHandlerFactory.getUrl(id, emptyList(), "") + getUrlSuffix(tab)
    }

    @Throws(ParsingException::class)
    override fun getId(url: String?): String = YoutubeChannelLinkHandlerFactory.getId(url)

    @Throws(ParsingException::class)
    override fun onAcceptUrl(url: String?): Boolean =
        YoutubeChannelLinkHandlerFactory.onAcceptUrl(url)

    override val availableContentFilter: Array<String>
        get() = arrayOf(
            ChannelTabs.VIDEOS,
            ChannelTabs.SHORTS,
            ChannelTabs.LIVESTREAMS,
            ChannelTabs.ALBUMS,
            ChannelTabs.COURSES,
            ChannelTabs.PODCASTS,
            ChannelTabs.PLAYLISTS,
        )
}

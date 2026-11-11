/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.linkHandler

import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory
import org.schabi.newpipe.extractor.utils.Utils

public object YoutubeSearchQueryHandlerFactory : SearchQueryHandlerFactory() {

    public const val ALL: String = "all"
    public const val VIDEOS: String = "videos"
    public const val CHANNELS: String = "channels"
    public const val PLAYLISTS: String = "playlists"
    public const val MUSIC_SONGS: String = "music_songs"
    public const val MUSIC_VIDEOS: String = "music_videos"
    public const val MUSIC_ALBUMS: String = "music_albums"
    public const val MUSIC_PLAYLISTS: String = "music_playlists"
    public const val MUSIC_ARTISTS: String = "music_artists"

    private const val SEARCH_URL = "https://www.youtube.com/results?search_query="
    private const val MUSIC_SEARCH_URL = "https://music.youtube.com/search?q="
    private const val DEFAULT_PARAMETER = "8AEB"

    /** A search text is cut there: nobody types more, and the request stays bounded. */
    private const val MAX_QUERY_LENGTH = 500

    private val MUSIC_FILTERS =
        setOf(MUSIC_SONGS, MUSIC_VIDEOS, MUSIC_ALBUMS, MUSIC_PLAYLISTS, MUSIC_ARTISTS)

    @JvmStatic
    public fun getInstance(): YoutubeSearchQueryHandlerFactory = this

    /**
     * Builds the search page URL. The query is percent-encoded, so it can only ever be the
     * value of the search parameter.
     *
     * @param id the search query (named like the parameter of [ListLinkHandlerFactory.getUrl])
     */
    @Throws(ParsingException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
    ): String {
        val query = Utils.encodeUrlUtf8(
            (id ?: throw ParsingException("No search query given")).take(MAX_QUERY_LENGTH),
        )
        return when (val filter = contentFilter?.firstOrNull().orEmpty()) {
            VIDEOS, CHANNELS, PLAYLISTS ->
                // The parameter sits in the URL query, so its "=" signs are encoded twice
                SEARCH_URL + query + "&sp=" + getSearchParameter(filter).replace("%", "%25")
            in MUSIC_FILTERS -> MUSIC_SEARCH_URL + query
            else -> "$SEARCH_URL$query&sp=$DEFAULT_PARAMETER"
        }
    }

    override val availableContentFilter: Array<String>
        get() = arrayOf(
            ALL,
            VIDEOS,
            CHANNELS,
            PLAYLISTS,
            MUSIC_SONGS,
            MUSIC_VIDEOS,
            MUSIC_ALBUMS,
            MUSIC_PLAYLISTS,
            // MUSIC_ARTISTS
        )

    @JvmStatic
    public fun getSearchParameter(contentFilter: String?): String = when (contentFilter.orEmpty()) {
        VIDEOS -> "EgIQAfABAQ%3D%3D"
        CHANNELS -> "EgIQAvABAQ%3D%3D"
        PLAYLISTS -> "EgIQA_ABAQ%3D%3D"
        in MUSIC_FILTERS -> ""
        else -> DEFAULT_PARAMETER
    }
}

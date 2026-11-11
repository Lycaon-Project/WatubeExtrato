/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItemExtractor
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.extractPlaylistTypeFromPlaylistUrl
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getTextFromObject
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getThumbnailsFromInfoItem
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeLinkGuards

public open class YoutubeMixOrPlaylistInfoItemExtractor(
    private val mixInfoItem: JsonObject,
) : PlaylistInfoItemExtractor {

    @get:Throws(ParsingException::class)
    override val name: String
        get() = getTextFromObject(mixInfoItem.getObject("title"))?.takeIf { it.isNotEmpty() }
            ?: throw ParsingException("Could not get name")

    @get:Throws(ParsingException::class)
    override val url: String
        get() {
            val url = mixInfoItem.getString("shareUrl")
            if (url.isNullOrEmpty()) {
                throw ParsingException("Could not get url")
            }
            // The URL is given as is by YouTube: only accept a bounded link to YouTube
            return YoutubeLinkGuards.requireBoundedUrl(url).takeIf(YoutubeParsingHelper::isYoutubeLink)
                ?: throw ParsingException("Could not get url")
        }

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = getThumbnailsFromInfoItem(mixInfoItem)

    // this will be a list of uploaders for mixes
    @get:Throws(ParsingException::class)
    override val uploaderName: String?
        get() = getTextFromObject(mixInfoItem.getObject("longBylineText"))

    // They're auto-generated, so there's no uploader
    @get:Throws(ParsingException::class)
    override val uploaderUrl: String?
        get() = null

    // They're auto-generated, so there's no uploader
    @get:Throws(ParsingException::class)
    override val isUploaderVerified: Boolean
        get() = false

    @get:Throws(ParsingException::class)
    override val streamCount: Long
        get() {
            val countString = getTextFromObject(mixInfoItem.getObject("videoCountShortText"))
                ?: throw ParsingException("Could not extract item count for playlist/mix info item")

            // un-parsable integer: this is a mix with infinite items and "50+" as count string
            // (though YouTube Music mixes do not necessarily have an infinite count of songs)
            return countString.toIntOrNull()?.toLong() ?: ListExtractor.ITEM_COUNT_INFINITE
        }

    @get:Throws(ParsingException::class)
    override val playlistType: PlaylistInfo.PlaylistType
        get() = extractPlaylistTypeFromPlaylistUrl(url)
}

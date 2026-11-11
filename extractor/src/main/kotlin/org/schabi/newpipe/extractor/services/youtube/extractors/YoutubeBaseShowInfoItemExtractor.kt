/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItemExtractor
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.THUMBNAIL_OVERLAYS
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getTextFromObject
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getThumbnailsFromInfoItem
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getYoutubeUrlFromNavigationEndpoint
import org.schabi.newpipe.extractor.utils.Utils

/**
 * The base [PlaylistInfoItemExtractor] for shows playlists UI elements.
 */
internal abstract class YoutubeBaseShowInfoItemExtractor(
    protected val showRenderer: JsonObject,
) : PlaylistInfoItemExtractor {

    @get:Throws(ParsingException::class)
    override val name: String
        get() = showRenderer.getString("title") ?: throw ParsingException("Could not get name")

    @get:Throws(ParsingException::class)
    override val url: String
        get() = getYoutubeUrlFromNavigationEndpoint(showRenderer.getObject("navigationEndpoint"))
            ?: throw ParsingException("Could not get url")

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = getThumbnailsFromInfoItem(
            showRenderer.getObject("thumbnailRenderer").getObject("showCustomThumbnailRenderer"),
        )

    @get:Throws(ParsingException::class)
    override val streamCount: Long
        get() {
            // The stream count should be always returned in the first text object for English
            // localizations, but the complete text is parsed for reliability purposes
            val streamCountText: String = getTextFromObject(
                showRenderer.getObject(THUMBNAIL_OVERLAYS)
                    .getObject("thumbnailOverlayBottomPanelRenderer")
                    .getObject("text"),
            ) ?: throw ParsingException("Could not get stream count")

            // The data returned could be a human/shortened number, but no show with more than 1000
            // videos has been found at the time this code was written
            return Utils.removeNonDigitCharacters(streamCountText).toLongOrNull()
                ?: throw ParsingException("Could not convert stream count to a long")
        }
}

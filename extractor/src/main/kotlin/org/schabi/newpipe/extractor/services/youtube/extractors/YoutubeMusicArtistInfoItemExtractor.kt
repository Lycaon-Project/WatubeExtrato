/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.channel.ChannelInfoItemExtractor
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getImagesFromThumbnailsArray
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getTextFromObject
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getYoutubeUrlFromNavigationEndpoint
import org.schabi.newpipe.extractor.utils.Parser
import org.schabi.newpipe.extractor.utils.Utils

public open class YoutubeMusicArtistInfoItemExtractor(
    private val artistInfoItem: JsonObject,
) : ChannelInfoItemExtractor {

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = try {
            getImagesFromThumbnailsArray(
                artistInfoItem.getObject("thumbnail")
                    .getObject("musicThumbnailRenderer")
                    .getObject("thumbnail")
                    .getArray("thumbnails"),
            )
        } catch (e: Exception) {
            throw ParsingException("Could not get thumbnails", e)
        }

    @get:Throws(ParsingException::class)
    override val name: String
        get() = getTextFromObject(
            artistInfoItem.getArray("flexColumns")
                .getObject(0)
                .getObject("musicResponsiveListItemFlexColumnRenderer")
                .getObject("text"),
        )?.takeIf { it.isNotEmpty() } ?: throw ParsingException("Could not get name")

    @get:Throws(ParsingException::class)
    override val url: String
        get() = getYoutubeUrlFromNavigationEndpoint(artistInfoItem.getObject("navigationEndpoint"))
            ?.takeIf { it.isNotEmpty() }
            ?: throw ParsingException("Could not get URL")

    @get:Throws(ParsingException::class)
    override val subscriberCount: Long
        get() {
            val flexColumns = artistInfoItem.getArray("flexColumns")
            val runs = flexColumns.getObject((flexColumns.size - 1).coerceAtLeast(0))
                .getObject("musicResponsiveListItemFlexColumnRenderer")
                .getObject("text")
                .getArray("runs")
            // NOTE: YoutubeParsingHelper#getTextFromObject would use all entries from the run
            // array, which is not wanted as only the last entry contains the actual
            // subscriberCount
            val subscriberCount = runs.getObject((runs.size - 1).coerceAtLeast(0)).getString("text")
            if (subscriberCount.isNullOrEmpty()) {
                throw ParsingException("Could not get subscriber count")
            }
            return try {
                Utils.mixedNumberWordToLong(subscriberCount)
            } catch (_: Parser.RegexException) {
                // probably subscriberCount == "No subscribers" or similar
                0
            }
        }

    override val streamCount: Long
        get() = -1

    // An artist on YouTube Music is always verified
    override val isVerified: Boolean
        get() = true

    override val description: String?
        get() = null
}

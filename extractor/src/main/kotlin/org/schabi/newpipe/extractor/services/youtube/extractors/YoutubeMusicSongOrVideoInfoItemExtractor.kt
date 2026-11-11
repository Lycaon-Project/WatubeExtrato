/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonArray
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getImagesFromThumbnailsArray
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getTextFromObject
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getYoutubeUrlFromNavigationEndpoint
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeLinkGuards
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.MUSIC_SONGS
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.MUSIC_VIDEOS
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.extractor.utils.Parser
import org.schabi.newpipe.extractor.utils.Utils

public open class YoutubeMusicSongOrVideoInfoItemExtractor(
    private val songOrVideoInfoItem: JsonObject,
    private val descriptionElements: JsonArray,
    private val searchType: String?,
) : StreamInfoItemExtractor {

    @get:Throws(ParsingException::class)
    override val url: String
        get() {
            val id = songOrVideoInfoItem.getObject("playlistItemData").getString("videoId")
            if (id.isNullOrEmpty()) {
                throw ParsingException("Could not get URL")
            }
            // The ID comes from the response: validated before being placed in the URL
            return "https://music.youtube.com/watch?v=" + YoutubeLinkGuards.requireVideoId(id)
        }

    @get:Throws(ParsingException::class)
    override val name: String
        get() = getTextFromObject(
            songOrVideoInfoItem.getArray("flexColumns")
                .getObject(0)
                .getObject("musicResponsiveListItemFlexColumnRenderer")
                .getObject("text"),
        )?.takeIf { it.isNotEmpty() } ?: throw ParsingException("Could not get name")

    override val streamType: StreamType
        get() = StreamType.VIDEO_STREAM

    override val isAd: Boolean
        get() = false

    @get:Throws(ParsingException::class)
    override val duration: Long
        get() {
            val duration = descriptionElementText(descriptionElements.size - 1)
                ?: throw ParsingException("Could not get duration")
            return YoutubeParsingHelper.parseDurationString(duration).toLong()
        }

    @get:Throws(ParsingException::class)
    override val uploaderName: String
        get() = descriptionElementText(0) ?: throw ParsingException("Could not get uploader name")

    @get:Throws(ParsingException::class)
    override val uploaderUrl: String?
        get() {
            if (searchType == MUSIC_VIDEOS) {
                val artistItem = songOrVideoInfoItem.getObject("menu")
                    .getObject("menuRenderer")
                    .getArray("items")
                    .filterIsInstance<JsonObject>()
                    .map { it.getObject("menuNavigationItemRenderer") }
                    .firstOrNull { it.getObject("icon").getString("iconType", "") == "ARTIST" }
                    ?: return null
                return getYoutubeUrlFromNavigationEndpoint(artistItem.getObject("navigationEndpoint"))
            }

            val navigationEndpointHolder = songOrVideoInfoItem.getArray("flexColumns")
                .getObject(1)
                .getObject("musicResponsiveListItemFlexColumnRenderer")
                .getObject("text")
                .getArray("runs")
                .getObject(0)

            if (!navigationEndpointHolder.has("navigationEndpoint")) {
                return null
            }

            return getYoutubeUrlFromNavigationEndpoint(navigationEndpointHolder.getObject("navigationEndpoint"))
                ?.takeIf { it.isNotEmpty() }
                ?: throw ParsingException("Could not get uploader URL")
        }

    // We don't have the ability to know this information on YouTube Music
    override val isUploaderVerified: Boolean
        get() = false

    override val textualUploadDate: String?
        get() = null

    override val uploadDate: DateWrapper?
        get() = null

    @get:Throws(ParsingException::class)
    override val viewCount: Long
        get() {
            if (searchType == MUSIC_SONGS) {
                return -1
            }
            val viewCount = descriptionElementText(descriptionElements.size - 3)
                ?: throw ParsingException("Could not get view count")
            return try {
                Utils.mixedNumberWordToLong(viewCount)
            } catch (_: Parser.RegexException) {
                // probably viewCount == "No views" or similar
                0
            }
        }

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = try {
            getImagesFromThumbnailsArray(
                songOrVideoInfoItem.getObject("thumbnail")
                    .getObject("musicThumbnailRenderer")
                    .getObject("thumbnail")
                    .getArray("thumbnails"),
            )
        } catch (e: Exception) {
            throw ParsingException("Could not get thumbnails", e)
        }

    /** Non-empty text of the description element at [index], `null` when out of range. */
    private fun descriptionElementText(index: Int): String? =
        if (index < 0) null else descriptionElements.getObject(index).getString("text")?.takeIf { it.isNotEmpty() }
}

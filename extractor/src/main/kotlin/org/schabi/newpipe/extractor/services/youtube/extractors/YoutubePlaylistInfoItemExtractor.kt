/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItemExtractor
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getImagesFromThumbnailsArray
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getTextFromObject
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getYoutubeUrlFromObject
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubePlaylistLinkHandlerFactory
import org.schabi.newpipe.extractor.utils.Utils

public open class YoutubePlaylistInfoItemExtractor(
    private val playlistInfoItem: JsonObject,
) : PlaylistInfoItemExtractor {

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = try {
            val thumbnails = playlistInfoItem.getArray("thumbnails")
                .getObject(0)
                .getArray("thumbnails")
                .ifEmpty { playlistInfoItem.getObject("thumbnail").getArray("thumbnails") }
            getImagesFromThumbnailsArray(thumbnails)
        } catch (e: Exception) {
            throw ParsingException("Could not get thumbnails", e)
        }

    @get:Throws(ParsingException::class)
    override val name: String
        get() = getTextFromObject(playlistInfoItem.getObject("title"))
            ?: throw ParsingException("Could not get name")

    @get:Throws(ParsingException::class)
    override val url: String
        get() = try {
            YoutubePlaylistLinkHandlerFactory.getInstance()
                .getUrl(playlistInfoItem.getString("playlistId"))
        } catch (e: Exception) {
            throw ParsingException("Could not get url", e)
        }

    @get:Throws(ParsingException::class)
    override val uploaderName: String?
        get() = getTextFromObject(playlistInfoItem.getObject("longBylineText"))

    @get:Throws(ParsingException::class)
    override val uploaderUrl: String?
        get() = getYoutubeUrlFromObject(playlistInfoItem.getObject("longBylineText"))

    @get:Throws(ParsingException::class)
    override val isUploaderVerified: Boolean
        get() = try {
            YoutubeParsingHelper.isVerified(playlistInfoItem.getArray("ownerBadges"))
        } catch (e: Exception) {
            throw ParsingException("Could not get uploader verification info", e)
        }

    @get:Throws(ParsingException::class)
    override val streamCount: Long
        get() {
            val videoCountText = playlistInfoItem.getString("videoCount")
                ?: getTextFromObject(playlistInfoItem.getObject("videoCountText"))
                ?: getTextFromObject(playlistInfoItem.getObject("videoCountShortText"))
                ?: throw ParsingException("Could not get stream count")

            return Utils.removeNonDigitCharacters(videoCountText).toLongOrNull()
                ?: throw ParsingException("Could not get stream count")
        }
}

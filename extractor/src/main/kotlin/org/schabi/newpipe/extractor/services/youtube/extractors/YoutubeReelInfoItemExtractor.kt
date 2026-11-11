/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getTextFromObject
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getThumbnailsFromInfoItem
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeStreamLinkHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.extractor.utils.Utils
import java.util.Locale

/**
 * A [StreamInfoItemExtractor] for YouTube's `reelItemRenderer`s.
 *
 * `reelItemRenderer`s were returned on YouTube for their short-form contents on almost every
 * place and every major client. They provide a limited amount of information and do not provide
 * the exact view count, any uploader info (name, URL, avatar, verified status) and the upload date.
 *
 * At the time this documentation has been updated, they are being replaced by
 * `shortsLockupViewModel`s. See [YoutubeShortsLockupInfoItemExtractor] for an
 * extractor for this new UI data type.
 */
public open class YoutubeReelInfoItemExtractor(
    private val reelInfo: JsonObject,
) : StreamInfoItemExtractor {

    @get:Throws(ParsingException::class)
    override val name: String
        get() = getTextFromObject(reelInfo.getObject("headline"))
            ?: throw ParsingException("Could not get name")

    @get:Throws(ParsingException::class)
    override val url: String
        get() = try {
            YoutubeStreamLinkHandlerFactory.getInstance().getUrl(reelInfo.getString("videoId"))
        } catch (e: Exception) {
            throw ParsingException("Could not get URL", e)
        }

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = getThumbnailsFromInfoItem(reelInfo)

    @get:Throws(ParsingException::class)
    override val streamType: StreamType
        get() = StreamType.VIDEO_STREAM

    @get:Throws(ParsingException::class)
    override val viewCount: Long
        get() {
            val viewCountText = getTextFromObject(reelInfo.getObject("viewCountText"))
            if (viewCountText.isNullOrEmpty()) {
                throw ParsingException("Could not get short view count")
            }
            // This approach is language dependent
            if (viewCountText.lowercase(Locale.ROOT).contains("no views")) {
                return 0
            }
            return Utils.mixedNumberWordToLong(viewCountText)
        }

    override val isShortFormContent: Boolean
        get() = true

    // All the following properties cannot be obtained from reelItemRenderers

    @get:Throws(ParsingException::class)
    override val isAd: Boolean
        get() = false

    @get:Throws(ParsingException::class)
    override val duration: Long
        get() = -1

    @get:Throws(ParsingException::class)
    override val uploaderName: String?
        get() = null

    @get:Throws(ParsingException::class)
    override val uploaderUrl: String?
        get() = null

    @get:Throws(ParsingException::class)
    override val isUploaderVerified: Boolean
        get() = false

    @get:Throws(ParsingException::class)
    override val textualUploadDate: String?
        get() = null

    @get:Throws(ParsingException::class)
    override val uploadDate: DateWrapper?
        get() = null
}

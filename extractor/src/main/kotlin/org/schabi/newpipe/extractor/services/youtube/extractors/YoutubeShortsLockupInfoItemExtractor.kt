/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getImagesFromThumbnailsArray
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeStreamLinkHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.extractor.utils.Utils
import java.util.Locale

/**
 * A [StreamInfoItemExtractor] for YouTube's `shortsLockupViewModel`s.
 *
 * `shortsLockupViewModel`s are returned on YouTube for their short-form contents on almost
 * every place and every major client. They provide a limited amount of information and do not
 * provide the exact view count, any uploader info (name, URL, avatar, verified status) and the
 * upload date.
 *
 * At the time this documentation has been written, this data UI type is not fully used (rolled
 * out), so `reelItemRenderer`s are also returned. See [YoutubeReelInfoItemExtractor]
 * for an extractor for this UI data type.
 */
internal open class YoutubeShortsLockupInfoItemExtractor(
    private val shortsLockupViewModel: JsonObject,
) : StreamInfoItemExtractor {

    @get:Throws(ParsingException::class)
    override val name: String
        get() = shortsLockupViewModel.getObject("overlayMetadata")
            .getObject("primaryText")
            .getString("content")
            ?: throw ParsingException("Could not get name")

    @get:Throws(ParsingException::class)
    override val url: String
        get() {
            val videoId = shortsLockupViewModel.getObject("onTap")
                .getObject("innertubeCommand")
                .getObject("reelWatchEndpoint")
                .getString("videoId")
                ?.takeIf { it.isNotEmpty() }
                ?: shortsLockupViewModel.getObject("inlinePlayerData")
                    .getObject("onVisible")
                    .getObject("innertubeCommand")
                    .getObject("watchEndpoint")
                    .getString("videoId")
                    ?.takeIf { it.isNotEmpty() }
                ?: throw ParsingException("Could not get video ID")

            return try {
                YoutubeStreamLinkHandlerFactory.getInstance().getUrl(videoId)
            } catch (e: Exception) {
                throw ParsingException("Could not get URL", e)
            }
        }

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() {
            if (shortsLockupViewModel.has("thumbnail")) {
                return getImagesFromThumbnailsArray(
                    shortsLockupViewModel.getObject("thumbnail").getArray("sources"),
                )
            }
            return getImagesFromThumbnailsArray(
                shortsLockupViewModel.getObject("thumbnailViewModel")
                    .getObject("thumbnailViewModel")
                    .getObject("image")
                    .getArray("sources"),
            )
        }

    @get:Throws(ParsingException::class)
    override val streamType: StreamType
        get() = StreamType.VIDEO_STREAM

    @get:Throws(ParsingException::class)
    override val viewCount: Long
        get() {
            val viewCountText = shortsLockupViewModel.getObject("overlayMetadata")
                .getObject("secondaryText")
                .getString("content")
            if (viewCountText.isNullOrEmpty()) {
                throw ParsingException("Could not get short view count")
            }
            if (viewCountText.contains("✪")) {
                // If secondary text content contains ✪, this short should be a members first or a
                // members only one, we can't extract its view count in this case
                return -1
            }
            // This approach is language dependent
            if (viewCountText.lowercase(Locale.ROOT).contains("no views")) {
                return 0
            }
            return Utils.mixedNumberWordToLong(viewCountText)
        }

    override val isShortFormContent: Boolean
        get() = true

    // All the following properties cannot be obtained from shortsLockupViewModels

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

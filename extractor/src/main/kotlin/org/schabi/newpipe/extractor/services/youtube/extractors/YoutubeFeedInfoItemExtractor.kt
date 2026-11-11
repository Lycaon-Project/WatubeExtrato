/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.extractors

import org.jsoup.nodes.Element
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.Image.ResolutionLevel
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor
import org.schabi.newpipe.extractor.stream.StreamType

public open class YoutubeFeedInfoItemExtractor(
    private val entryElement: Element,
) : StreamInfoItemExtractor {

    // It is not possible to determine the stream type using the feed endpoint.
    // All entries are considered a video stream.
    override val streamType: StreamType
        get() = StreamType.VIDEO_STREAM

    override val isAd: Boolean
        get() = false

    // Not available when fetching through the feed endpoint.
    override val duration: Long
        get() = -1

    @get:Throws(ParsingException::class)
    override val viewCount: Long
        get() = firstByTag("media:statistics").attr("views").toLongOrNull()
            ?: throw ParsingException("Could not get view count")

    @get:Throws(ParsingException::class)
    override val uploaderName: String
        get() = firstBySelector("author > name").text()

    @get:Throws(ParsingException::class)
    override val uploaderUrl: String
        get() = firstBySelector("author > uri").text()

    @get:Throws(ParsingException::class)
    override val isUploaderVerified: Boolean
        get() = false

    @get:Throws(ParsingException::class)
    override val textualUploadDate: String
        get() = firstByTag("published").text()

    @get:Throws(ParsingException::class)
    override val uploadDate: DateWrapper?
        get() = DateWrapper.fromOffsetDateTime(textualUploadDate)

    @get:Throws(ParsingException::class)
    override val name: String
        get() = firstByTag("title").text()

    @get:Throws(ParsingException::class)
    override val url: String
        get() = firstByTag("link").attr("href").takeIf(YoutubeParsingHelper::isYoutubeLink)
            ?: throw ParsingException("Could not get url")

    override val thumbnails: List<Image>
        get() {
            val thumbnailElement = entryElement.getElementsByTag("media:thumbnail").first()
                ?: return emptyList()

            val feedThumbnailUrl = thumbnailElement.attr("url")

            // If the thumbnail URL is empty, it means that no thumbnail is available, return an
            // empty list in this case; one outside of YouTube's image hosts is not loaded either
            if (feedThumbnailUrl.isEmpty() || !YoutubeParsingHelper.isYoutubeImageUrl(feedThumbnailUrl)) {
                return emptyList()
            }

            // The hqdefault thumbnail has some black bars at the top and at the bottom, while the
            // mqdefault doesn't, so return the mqdefault one. It should always exist, according to
            // https://stackoverflow.com/a/20542029/9481500.
            val newFeedThumbnailUrl = feedThumbnailUrl.replace("hqdefault", "mqdefault")

            val height: Int
            val width: Int
            // If the new thumbnail URL is equal to the feed one, it means that a different image
            // resolution is used on feeds, so use the height and width provided instead of the
            // mqdefault ones
            if (newFeedThumbnailUrl == feedThumbnailUrl) {
                height = thumbnailElement.attr("height").toIntOrNull() ?: Image.HEIGHT_UNKNOWN
                width = thumbnailElement.attr("width").toIntOrNull() ?: Image.WIDTH_UNKNOWN
            } else {
                height = MQ_DEFAULT_HEIGHT
                width = MQ_DEFAULT_WIDTH
            }

            return listOf(
                Image(newFeedThumbnailUrl, height, width, ResolutionLevel.fromHeight(height)),
            )
        }

    @Throws(ParsingException::class)
    private fun firstByTag(tag: String): Element = entryElement.getElementsByTag(tag).first()
        ?: throw ParsingException("Could not get the $tag element of the feed entry")

    @Throws(ParsingException::class)
    private fun firstBySelector(cssQuery: String): Element = entryElement.selectFirst(cssQuery)
        ?: throw ParsingException("Could not get the uploader of the feed entry")

    private companion object {
        // Kept as in the original code (the values look swapped, but callers rely on them)
        private const val MQ_DEFAULT_HEIGHT = 320
        private const val MQ_DEFAULT_WIDTH = 180
    }
}

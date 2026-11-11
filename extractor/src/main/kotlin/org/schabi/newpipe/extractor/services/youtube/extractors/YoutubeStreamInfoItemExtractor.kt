/*
 * Copyright (C) 2016 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * YoutubeStreamInfoItemExtractor.kt is part of NewPipe Extractor.
 *
 * NewPipe Extractor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe Extractor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe Extractor.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.schabi.newpipe.extractor.services.youtube.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.localization.TimeAgoParser
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.BADGES
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.LABEL
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.METADATA_BADGE_RENDERER
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.STYLE
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.THUMBNAIL_OVERLAYS
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getImagesFromThumbnailsArray
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getTextFromObject
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getThumbnailsFromInfoItem
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getYoutubeUrlFromNavigationEndpoint
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeStreamLinkHandlerFactory
import org.schabi.newpipe.extractor.stream.ContentAvailability
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.extractor.utils.JsonUtils
import org.schabi.newpipe.extractor.utils.Parser
import org.schabi.newpipe.extractor.utils.Utils
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.regex.Pattern

/**
 * Creates an extractor of StreamInfoItems from a YouTube page.
 *
 * @param videoInfo The JSON page element
 * @param timeAgoParser A parser of the textual dates or `null`.
 */
public open class YoutubeStreamInfoItemExtractor(
    private val videoInfo: JsonObject,
    private val timeAgoParser: TimeAgoParser?,
) : StreamInfoItemExtractor {

    private val isPremiere: Boolean = videoInfo.has("upcomingEventData")

    private val cachedStreamType: StreamType by lazy { computeStreamType() }

    @get:Throws(ParsingException::class)
    override val streamType: StreamType
        get() = cachedStreamType

    private fun computeStreamType(): StreamType {
        val liveBadge = videoInfo.getArray(BADGES).filterIsInstance<JsonObject>().any { badge ->
            val badgeRenderer = badge.getObject(METADATA_BADGE_RENDERER)
            "BADGE_STYLE_TYPE_LIVE_NOW" == badgeRenderer.getString(STYLE) ||
                "LIVE NOW" == badgeRenderer.getString(LABEL)
        }
        if (liveBadge) {
            return StreamType.LIVE_STREAM
        }
        val liveOverlay = videoInfo.getArray(THUMBNAIL_OVERLAYS).filterIsInstance<JsonObject>().any {
            "LIVE" == it.getObject(TIME_STATUS_RENDERER).getString(STYLE)
        }
        return if (liveOverlay) StreamType.LIVE_STREAM else StreamType.VIDEO_STREAM
    }

    @get:Throws(ParsingException::class)
    override val isAd: Boolean
        get() = isPremium() || name == "[Private video]" || name == "[Deleted video]"

    @get:Throws(ParsingException::class)
    override val url: String
        get() = try {
            YoutubeStreamLinkHandlerFactory.getInstance().getUrl(videoInfo.getString("videoId"))
        } catch (e: Exception) {
            throw ParsingException("Could not get url", e)
        }

    @get:Throws(ParsingException::class)
    override val name: String
        get() {
            val title = videoInfo.getObject("title")
            val name = getTextFromObject(title)
            if (!name.isNullOrEmpty()) {
                return name
            }
            // Videos can have no title, e.g. https://www.youtube.com/watch?v=nc1kN8ZSfGQ
            if (!title.isEmpty() && !title.has("runs")) {
                return ""
            }
            throw ParsingException("Could not get name")
        }

    @get:Throws(ParsingException::class)
    override val duration: Long
        get() {
            if (streamType == StreamType.LIVE_STREAM) {
                return -1
            }
            val duration = getTextFromObject(videoInfo.getObject("lengthText"))
                // Available in playlists for videos
                ?.takeIf { it.isNotEmpty() }
                ?: videoInfo.getString("lengthSeconds")?.takeIf { it.isNotEmpty() }

            if (duration != null) {
                return YoutubeParsingHelper.parseDurationString(duration).toLong()
            }

            for (timeOverlayText in timeOverlayTexts()) {
                try {
                    return YoutubeParsingHelper.parseDurationString(timeOverlayText).toLong()
                } catch (_: ParsingException) {
                    // try next
                }
            }

            if (isPremiere) {
                // Premieres can be livestreams, so the duration is not available in this case
                return -1
            }
            throw ParsingException("Could not get duration")
        }

    private fun timeOverlayTexts(): List<String> =
        videoInfo.getArray(THUMBNAIL_OVERLAYS)
            .filterIsInstance<JsonObject>()
            .filter { it.has(TIME_STATUS_RENDERER) }
            .mapNotNull { overlay ->
                getTextFromObject(overlay.getObject(TIME_STATUS_RENDERER).getObject("text"))
                    ?.takeIf { it.isNotEmpty() }
            }

    @get:Throws(ParsingException::class)
    override val uploaderName: String?
        get() = sequenceOf("longBylineText", "ownerText", "shortBylineText")
            .mapNotNull { key -> getTextFromObject(videoInfo.getObject(key))?.takeIf { it.isNotEmpty() } }
            .firstOrNull()
            ?: throw ParsingException("Could not get uploader name")

    @get:Throws(ParsingException::class)
    override val uploaderUrl: String?
        get() = sequenceOf("longBylineText", "ownerText", "shortBylineText")
            .mapNotNull { key ->
                getYoutubeUrlFromNavigationEndpoint(
                    videoInfo.getObject(key).getArray("runs").getObject(0).getObject("navigationEndpoint"),
                )?.takeIf { it.isNotEmpty() }
            }
            .firstOrNull()
            ?: throw ParsingException("Could not get uploader url")

    @get:Throws(ParsingException::class)
    override val uploaderAvatars: List<Image>
        get() = when {
            videoInfo.has("channelThumbnailSupportedRenderers") -> getImagesFromThumbnailsArray(
                JsonUtils.getArray(videoInfo, CHANNEL_THUMBNAIL_SUPPORTED_RENDERERS_PATH),
            )
            videoInfo.has("channelThumbnail") -> getImagesFromThumbnailsArray(
                JsonUtils.getArray(videoInfo, "channelThumbnail.thumbnails"),
            )
            else -> emptyList()
        }

    @get:Throws(ParsingException::class)
    override val isUploaderVerified: Boolean
        get() = YoutubeParsingHelper.isVerified(videoInfo.getArray("ownerBadges"))

    @get:Throws(ParsingException::class)
    override val textualUploadDate: String?
        get() {
            if (streamType == StreamType.LIVE_STREAM) {
                return null
            }
            if (isPremiere) {
                val localDateTime = LocalDateTime.ofInstant(instantFromPremiere(), ZoneId.systemDefault())
                return PREMIERE_DATE_FORMATTER.format(localDateTime)
            }
            val publishedTimeText = getTextFromObject(videoInfo.getObject("publishedTimeText"))
            if (publishedTimeText.isNullOrEmpty() && videoInfo.has("videoInfo")) {
                // Returned in playlists, in the form: view count separator upload date
                return videoInfo.getObject("videoInfo").getArray("runs").getObject(2)
                    .getString("text")?.takeIf { it.isNotEmpty() }
            }
            return publishedTimeText?.takeIf { it.isNotEmpty() }
        }

    @get:Throws(ParsingException::class)
    override val uploadDate: DateWrapper?
        get() {
            if (streamType == StreamType.LIVE_STREAM) {
                return null
            }
            if (isPremiere) {
                return DateWrapper(instantFromPremiere())
            }
            val textualUploadDate = textualUploadDate
            if (timeAgoParser != null && !textualUploadDate.isNullOrEmpty()) {
                try {
                    return timeAgoParser.parse(textualUploadDate)
                } catch (e: ParsingException) {
                    throw ParsingException("Could not get upload date", e)
                }
            }
            return null
        }

    @get:Throws(ParsingException::class)
    override val viewCount: Long
        get() {
            if (isPremium() || isPremiere) {
                return -1
            }
            // Ignore all exceptions, as the view count can be hidden by creators, and so cannot be
            // found in this case
            val viewCountText = getTextFromObject(videoInfo.getObject("viewCountText"))
            if (!viewCountText.isNullOrEmpty()) {
                viewCountOrNull { viewCountFromViewCountText(viewCountText, false) }?.let { return it }
            }
            // Try parsing the real view count from accessibility data, if that is not a running
            // livestream (the view count is returned and not the count of people watching the
            // livestream currently)
            if (streamType != StreamType.LIVE_STREAM) {
                viewCountOrNull { viewCountFromAccessibilityData() }?.let { return it }
            }
            // Fallback to a short view count, always used for livestreams (see why above)
            if (videoInfo.has("videoInfo")) {
                // Returned in playlists, in the form: view count separator upload date
                val text = videoInfo.getObject("videoInfo").getArray("runs").getObject(0)
                    .getString("text", "").orEmpty()
                viewCountOrNull { viewCountFromViewCountText(text, true) }?.let { return it }
            }
            if (videoInfo.has("shortViewCountText")) {
                // Returned everywhere but in playlists, used by the website to show view counts
                val shortViewCountText = getTextFromObject(videoInfo.getObject("shortViewCountText"))
                if (!shortViewCountText.isNullOrEmpty()) {
                    viewCountOrNull { viewCountFromViewCountText(shortViewCountText, true) }
                        ?.let { return it }
                }
            }
            // No view count extracted: return -1, as the view count can be hidden by creators on videos
            return -1
        }

    private inline fun viewCountOrNull(block: () -> Long): Long? =
        try {
            block()
        } catch (_: Exception) {
            null
        }

    @Throws(NumberFormatException::class, ParsingException::class)
    private fun viewCountFromViewCountText(viewCountText: String, isMixedNumber: Boolean): Long {
        // These approaches are language dependent
        val lowercase = viewCountText.lowercase(Locale.ROOT)
        if (lowercase.contains(NO_VIEWS_LOWERCASE)) {
            return 0
        } else if (lowercase.contains("recommended")) {
            return -1
        }
        return if (isMixedNumber) {
            Utils.mixedNumberWordToLong(viewCountText)
        } else {
            Utils.removeNonDigitCharacters(viewCountText).toLong()
        }
    }

    @Throws(NumberFormatException::class, Parser.RegexException::class)
    private fun viewCountFromAccessibilityData(): Long {
        // These approaches are language dependent
        val accessibilityLabel = videoInfo.getObject("title")
            .getObject("accessibility")
            .getObject("accessibilityData")
            .getString(LABEL, "")
            .orEmpty()
        if (accessibilityLabel.lowercase(Locale.ROOT).endsWith(NO_VIEWS_LOWERCASE)) {
            return 0
        }
        return Utils.removeNonDigitCharacters(
            Parser.matchGroup1(ACCESSIBILITY_DATA_VIEW_COUNT_REGEX, accessibilityLabel),
        ).toLong()
    }

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = getThumbnailsFromInfoItem(videoInfo)

    private fun isPremium(): Boolean = videoInfo.getArray(BADGES).filterIsInstance<JsonObject>().any {
        "Premium" == it.getObject(METADATA_BADGE_RENDERER).getString(LABEL)
    }

    @Throws(ParsingException::class)
    private fun instantFromPremiere(): Instant {
        val startTime = videoInfo.getObject("upcomingEventData").getString("startTime")
        val epochSeconds = startTime?.toLongOrNull()
            ?: throw ParsingException("Could not parse date from premiere")
        return try {
            Instant.ofEpochSecond(epochSeconds)
        } catch (e: Exception) {
            throw ParsingException("Could not parse date from premiere", e)
        }
    }

    @get:Throws(ParsingException::class)
    override val shortDescription: String?
        get() = when {
            videoInfo.has("detailedMetadataSnippets") -> getTextFromObject(
                videoInfo.getArray("detailedMetadataSnippets").getObject(0).getObject("snippetText"),
            )
            videoInfo.has("descriptionSnippet") -> getTextFromObject(videoInfo.getObject("descriptionSnippet"))
            else -> null
        }

    @get:Throws(ParsingException::class)
    override val isShortFormContent: Boolean
        get() = try {
            val navigationEndpoint = videoInfo.getObject("navigationEndpoint")
            val webPageType = navigationEndpoint.getObject("commandMetadata")
                .getObject("webCommandMetadata")
                .getString("webPageType")
            webPageType == "WEB_PAGE_TYPE_SHORTS" ||
                navigationEndpoint.has("reelWatchEndpoint") ||
                hasShortsTimeOverlay()
        } catch (e: Exception) {
            throw ParsingException("Could not determine if this is short-form content", e)
        }

    private fun hasShortsTimeOverlay(): Boolean =
        videoInfo.getArray(THUMBNAIL_OVERLAYS)
            .filterIsInstance<JsonObject>()
            .filter { it.has(TIME_STATUS_RENDERER) }
            .map { it.getObject(TIME_STATUS_RENDERER) }
            .any { timeOverlay ->
                timeOverlay.getString(STYLE, "").orEmpty().equals("SHORTS", ignoreCase = true) ||
                    timeOverlay.getObject("icon").getString("iconType", "").orEmpty()
                        .lowercase(Locale.ROOT)
                        .contains("shorts")
            }

    private fun isMembersOnly(): Boolean = videoInfo.getArray(BADGES).filterIsInstance<JsonObject>().any {
        "BADGE_STYLE_TYPE_MEMBERS_ONLY" == it.getObject(METADATA_BADGE_RENDERER).getString(STYLE)
    }

    @get:Throws(ParsingException::class)
    override val contentAvailability: ContentAvailability
        get() = when {
            isPremiere -> ContentAvailability.UPCOMING
            isMembersOnly() -> ContentAvailability.MEMBERSHIP
            isPremium() -> ContentAvailability.PAID
            else -> ContentAvailability.AVAILABLE
        }

    public companion object {
        public const val TIME_STATUS_RENDERER: String = "thumbnailOverlayTimeStatusRenderer"

        private const val NO_VIEWS_LOWERCASE = "no views"
        private const val CHANNEL_THUMBNAIL_SUPPORTED_RENDERERS_PATH =
            "channelThumbnailSupportedRenderers.channelThumbnailWithLinkRenderer.thumbnail.thumbnails"
        private val ACCESSIBILITY_DATA_VIEW_COUNT_REGEX: Pattern = Pattern.compile("""([\d,]+) views$""")
        private val PREMIERE_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}

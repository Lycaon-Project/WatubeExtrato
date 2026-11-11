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
import org.schabi.newpipe.extractor.localization.TimeAgoParser
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.BADGES
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeChannelLinkHandlerFactory
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeStreamLinkHandlerFactory
import org.schabi.newpipe.extractor.stream.ContentAvailability
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.extractor.utils.JsonUtils
import org.schabi.newpipe.extractor.utils.Utils
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Extractor of YouTube lockup view models for stream items.
 *
 * The following features are currently not implemented:
 * - Shorts: appear in related items without a duration badge; [duration] returns -1
 * - YouTube Premium Paid content
 *
 * @param lockupViewModel The JSON page element
 * @param timeAgoParser A parser of the textual dates or `null`.
 */
public open class YoutubeStreamInfoItemLockupExtractor(
    private val lockupViewModel: JsonObject,
    private val timeAgoParser: TimeAgoParser?,
) : StreamInfoItemExtractor {

    private val cachedMetadataRows: JsonArray = lockupViewModel.getObject("metadata")
        .getObject("lockupMetadataViewModel")
        .getObject("metadata")
        .getObject("contentMetadataViewModel")
        .getArray("metadataRows")

    private var cachedStreamType: StreamType? = null
    private var cachedName: String? = null
    private var cachedDateText: String? = null
    private var cachedChannelImageViewModel: ChannelImageViewModel? = null

    /**
     * Whether this is a lockup view model for a channel or a course playlist, `false` by default.
     *
     * Some cases to parse properly the date and the views count requires to know this.
     */
    protected open val isChannelOrCoursePlaylistLockupItem: Boolean
        get() = false

    @get:Throws(ParsingException::class)
    override val streamType: StreamType
        get() = cachedStreamType ?: determineStreamType().also { cachedStreamType = it }

    @Throws(ParsingException::class)
    private fun determineStreamType(): StreamType {
        val overlays = JsonUtils.getArray(lockupViewModel, OVERLAYS_PATH).filterIsInstance<JsonObject>()

        // thumbnailOverlayBadgeViewModel path (legacy/alternate overlay structure)
        val liveBadge = overlays
            .flatMap {
                it.getObject("thumbnailOverlayBadgeViewModel").getArray("thumbnailBadges")
                    .filterIsInstance<JsonObject>()
            }
            .map { it.getObject("thumbnailBadgeViewModel") }
            .any { viewModel ->
                LIVE_BADGE_STYLE == viewModel.getString("badgeStyle") ||
                    // Fallback: Check if there is a live icon
                    viewModel.getObject("icon").getArray("sources").filterIsInstance<JsonObject>()
                        .any { "LIVE" == it.getObject("clientResource").getString("imageName") }
            }
        if (liveBadge) {
            return StreamType.LIVE_STREAM
        }

        // thumbnailBottomOverlayViewModel path (used in lockup format for both duration and live)
        val liveBottomBadge = bottomOverlayBadges(overlays)
            .any { LIVE_BADGE_STYLE == it.getObject("thumbnailBadgeViewModel").getString("badgeStyle") }
        return if (liveBottomBadge) StreamType.LIVE_STREAM else StreamType.VIDEO_STREAM
    }

    private fun bottomOverlayBadges(overlays: List<JsonObject>): List<JsonObject> =
        overlays.flatMap {
            it.getObject("thumbnailBottomOverlayViewModel").getArray(BADGES).filterIsInstance<JsonObject>()
        }

    @get:Throws(ParsingException::class)
    override val isAd: Boolean
        get() = name.let { it == "[Private video]" || it == "[Deleted video]" }

    @get:Throws(ParsingException::class)
    override val url: String
        get() = try {
            val videoId = lockupViewModel.getString("contentId")?.takeIf { it.isNotEmpty() }
                ?: JsonUtils.getString(lockupViewModel, WATCH_ENDPOINT_VIDEO_ID_PATH)
            YoutubeStreamLinkHandlerFactory.getInstance().getUrl(videoId)
        } catch (e: Exception) {
            throw ParsingException("Could not get url", e)
        }

    @get:Throws(ParsingException::class)
    override val name: String
        get() {
            cachedName?.let { return it }
            val name = JsonUtils.getString(lockupViewModel, "metadata.lockupMetadataViewModel.title.content")
            if (name.isNotEmpty()) {
                cachedName = name
                return name
            }
            throw ParsingException("Could not get name")
        }

    @get:Throws(ParsingException::class)
    override val duration: Long
        get() {
            // Duration cannot be extracted for live streams, but only for normal videos
            // Exact duration cannot be extracted for premieres, an approximation is only available
            // in accessibility context label
            if (isLive() || isPremiere()) {
                return -1
            }
            val overlays = JsonUtils.getArray(lockupViewModel, OVERLAYS_PATH).filterIsInstance<JsonObject>()
            val potentialDurations = bottomOverlayBadges(overlays)
                .mapNotNull { it.getObject("thumbnailBadgeViewModel").getString("text") }

            var parsingException: ParsingException? = null
            for (potentialDuration in potentialDurations) {
                if (!potentialDuration.any { it.isDigit() }) {
                    continue
                }
                try {
                    return YoutubeParsingHelper.parseDurationString(potentialDuration).toLong()
                } catch (e: ParsingException) {
                    parsingException = e
                }
            }
            // e.g. only "SHORTS" or "CC" badge was present, no duration available
            return parsingException?.let { throw ParsingException("Could not get duration", it) } ?: -1
        }

    @get:Throws(ParsingException::class)
    override val uploaderName: String?
        get() {
            val metadataRows = metadataPartsFromMetadataRows()
            if (metadataRows.isEmpty()) {
                throw ParsingException("Could not get uploader name: no metadata row")
            }
            return textContentFromMetadataPart(metadataRows[0].getObject(0))
                ?.takeIf { it.isNotEmpty() }
                ?: throw ParsingException("Could not get uploader name")
        }

    @get:Throws(ParsingException::class)
    override val uploaderUrl: String?
        get() {
            val innerTubeCommand = channelImageViewModel()
                .forUploaderUrlExtraction()
                .getObject("rendererContext")
                .getObject("commandContext")
                .getObject("onTap")
                .getObject("innertubeCommand")
            val browseEndpoint = innerTubeCommand.getObject("browseEndpoint")
            val channelId = browseEndpoint.getString("browseId")
            if (channelId != null && channelId.startsWith("UC")) {
                return YoutubeChannelLinkHandlerFactory.getInstance().getUrl("channel/$channelId")
            }
            val canonicalBaseUrl = browseEndpoint.getString("canonicalBaseUrl")
            if (!canonicalBaseUrl.isNullOrEmpty()) {
                return uploaderUrlFromRelativeUrl(canonicalBaseUrl)
            }
            val webCommandMetadataUrl = innerTubeCommand.getObject("commandMetadata")
                .getObject("webCommandMetadata")
                .getString("url")
            if (!webCommandMetadataUrl.isNullOrEmpty()) {
                return uploaderUrlFromRelativeUrl(webCommandMetadataUrl)
            }
            throw ParsingException("Could not get uploader url")
        }

    @Throws(ParsingException::class)
    private fun uploaderUrlFromRelativeUrl(relativeUrl: String): String =
        YoutubeChannelLinkHandlerFactory.getInstance().getUrl(relativeUrl.removePrefix("/"))

    @get:Throws(ParsingException::class)
    override val uploaderAvatars: List<Image>
        get() = YoutubeParsingHelper.getImagesFromThumbnailsArray(
            JsonUtils.getArray(channelImageViewModel().forAvatarExtraction(), "avatarViewModel.image.sources"),
        )

    @get:Throws(ParsingException::class)
    override val isUploaderVerified: Boolean
        get() {
            val metadataRows = metadataPartsFromMetadataRows()
            if (metadataRows.isEmpty()) {
                throw ParsingException("Could not get uploader verified status: no metadata row")
            }
            return YoutubeParsingHelper.hasArtistOrVerifiedIconBadgeAttachment(
                metadataRows[0].getObject(0).getObject("text").getArray("attachmentRuns"),
            )
        }

    @get:Throws(ParsingException::class)
    override val textualUploadDate: String?
        get() {
            // Live streams have no upload date
            if (isLive()) {
                return null
            }
            val dateText = dateText()
            return if (isPremiere()) dateFromPremiere(dateText) else dateText
        }

    private fun dateFromPremiere(dateText: String): String =
        // This approach is language dependent
        // Remove the premieres text from the upload date metadata part
        dateText.replace(PREMIERES_VIDEOS_TEXT, "").replace(PREMIERES_LIVES_TEXT, "")

    @get:Throws(ParsingException::class)
    override val uploadDate: DateWrapper?
        get() {
            val parser = timeAgoParser ?: return null
            // Prevent NPE when e.g. a live stream is shown
            val textualUploadDate = textualUploadDate ?: return null
            if (isPremiere()) {
                return try {
                    // As we request a UTC offset of 0 minutes, we get the UTC date
                    val dateTime = LocalDateTime.parse(textualUploadDate, PREMIERES_DATE_FORMATTER)
                    DateWrapper(dateTime.atZone(ZoneOffset.UTC).toInstant(), false)
                } catch (e: DateTimeParseException) {
                    throw ParsingException("Could not parse premiere upload date", e)
                }
            }
            return parser.parse(textualUploadDate)
        }

    @get:Throws(ParsingException::class)
    override val viewCount: Long
        get() {
            if (isChannelsMembersOnlyOrFirst()) {
                // Members only or members first contents do not return their view count
                // Check done here as there should be no metadata row for running members-only
                // livestreams on channels
                return -1
            }
            val metadataPartsRows = metadataPartsFromMetadataRows()
            if (metadataPartsRows.isEmpty()) {
                // No metadata part row is returned for running livestreams with no viewers on
                // channels (course playlists should not have livestreams)
                if (isLive() && isChannelOrCoursePlaylistLockupItem) {
                    return 0
                }
                throw ParsingException("Could not get view count: no metadata part from metadata rows")
            }
            if (isPremiere()) {
                // The number of people returned for premieres is the one currently waiting
                // Check done here as isPremiere relies on metadataPartsRows
                return -1
            }
            if (isLive() && metadataPartsRows.size == 1 && !isChannelOrCoursePlaylistLockupItem) {
                // If there is only one metadata part on channel lockup items for running
                // livestreams, this should be the watching count (course playlists should not have
                // livestreams). If this is not a channel lockup item, this should be a livestream
                // without any viewer
                return 0
            }
            // YouTube uses 2 rows for stream items outside channels and course playlists: one for
            // author(s) then one for views and upload date (in standard cases for this row).
            // However, on channels, it uses mostly 1 row except for collaborations when there are
            // two, but the views and upload date metadata row is always the latest one.
            val metadataPartsRow = metadataPartsRows.last()
            if (metadataPartsRow.isEmpty()) {
                throw ParsingException("Could not get view count: no metadata part in the metadata parts array")
            }
            // View count is always returned as the first metadata part, then date text
            // For members only content, view count is not returned, so there is only the date
            // text in the metadata row
            val viewCountText = textContentFromMetadataPart(metadataPartsRow.getObject(0))
            if (viewCountText.isNullOrEmpty()) {
                throw ParsingException("Could not get view count")
            }
            return viewCountFromViewCountText(viewCountText)
        }

    @Throws(NumberFormatException::class, ParsingException::class)
    private fun viewCountFromViewCountText(viewCountText: String): Long {
        // These approaches are language dependent
        val lowercase = viewCountText.lowercase(Locale.ROOT)
        if (lowercase.contains(NO_VIEWS_LOWERCASE)) {
            return 0
        } else if (lowercase.contains("recommended")) {
            return -1
        }
        return Utils.mixedNumberWordToLong(viewCountText)
    }

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = YoutubeParsingHelper.getImagesFromThumbnailsArray(
            JsonUtils.getArray(lockupViewModel, "contentImage.thumbnailViewModel.image.sources"),
        )

    @get:Throws(ParsingException::class)
    override val contentAvailability: ContentAvailability
        get() = when {
            // In the case we get a running members-only livestream, checking for isLive first
            // would return an incorrect content availability
            isChannelsMembersOnlyOrFirst() -> ContentAvailability.MEMBERSHIP
            // Check that it is a running livestream first as in the case of a livestream, no date
            // text is available so isPremiere would throw an exception
            isLive() -> ContentAvailability.AVAILABLE
            isPremiere() -> ContentAvailability.UPCOMING
            else -> ContentAvailability.AVAILABLE
        }

    @Throws(ParsingException::class)
    private fun channelImageViewModel(): ChannelImageViewModel =
        cachedChannelImageViewModel ?: determineChannelImageViewModel().also { cachedChannelImageViewModel = it }

    @Throws(ParsingException::class)
    private fun determineChannelImageViewModel(): ChannelImageViewModel {
        val image = lockupViewModel.getObject("metadata")
            .getObject("lockupMetadataViewModel")
            .getObject("image")
        image.getObject("decoratedAvatarViewModel", null)?.let { return SingleChannelImageViewModel(it) }
        image.getObject("avatarStackViewModel", null)?.let { return MultiChannelImageViewModel(it) }
        throw ParsingException("Failed to determine channel image view model")
    }

    private fun textContentFromMetadataPart(metadataPart: JsonObject): String? =
        metadataPart.getObject("text").getString("content")

    @Throws(ParsingException::class)
    private fun isLive(): Boolean = streamType != StreamType.VIDEO_STREAM

    // Also returned for members first contents
    private fun isChannelsMembersOnlyOrFirst(): Boolean =
        cachedMetadataRows.filterIsInstance<JsonObject>()
            .flatMap { it.getArray("badges").filterIsInstance<JsonObject>() }
            .any { "BADGE_MEMBERS_ONLY" == it.getObject("badgeViewModel").getString("badgeStyle") }

    @Throws(ParsingException::class)
    private fun isPremiere(): Boolean {
        val dateText = dateText()
        return dateText.contains(PREMIERES_VIDEOS_TEXT) || dateText.contains(PREMIERES_LIVES_TEXT)
    }

    @Throws(ParsingException::class)
    private fun dateText(): String {
        cachedDateText?.let { return it }
        val metadataPartsRows = metadataPartsFromMetadataRows()
        if (metadataPartsRows.isEmpty()) {
            throw ParsingException("Could not get date text: no metadata part from metadata rows")
        }
        // YouTube uses 2 rows for stream items outside channels and course playlists: author(s)
        // then one for views and upload date (in standard cases for this row). However, on
        // channels, this is mostly 1 row except for collaborations when there are two, but the
        // views and upload date metadata row is always the latest one.
        val metadataPartsRow = metadataPartsRows.last()
        if (metadataPartsRow.isEmpty()) {
            throw ParsingException("Could not get date text: no metadata part in the metadata parts array")
        }
        // View count is always returned as the first metadata part, then date text
        // For members only content, view count is not returned, so there is only the date text
        // in the metadata row
        val dateText = textContentFromMetadataPart(metadataPartsRow.getObject(metadataPartsRow.size - 1))
            ?: throw ParsingException("Could not get date text")
        cachedDateText = dateText
        return dateText
    }

    private fun metadataPartsFromMetadataRows(): List<JsonArray> =
        cachedMetadataRows.filterIsInstance<JsonObject>()
            .filter { it.has("metadataParts") }
            .map { it.getArray("metadataParts") }

    internal abstract class ChannelImageViewModel(protected val viewModel: JsonObject) {
        abstract fun forUploaderUrlExtraction(): JsonObject

        abstract fun forAvatarExtraction(): JsonObject
    }

    internal class SingleChannelImageViewModel(viewModel: JsonObject) : ChannelImageViewModel(viewModel) {
        override fun forUploaderUrlExtraction(): JsonObject = viewModel

        override fun forAvatarExtraction(): JsonObject = viewModel.getObject("avatar")
    }

    internal class MultiChannelImageViewModel(viewModel: JsonObject) : ChannelImageViewModel(viewModel) {
        override fun forUploaderUrlExtraction(): JsonObject =
            viewModel.getObject("rendererContext")
                .getObject("commandContext")
                .getObject("onTap")
                .getObject("innertubeCommand")
                .getObject("showDialogCommand")
                .getObject("panelLoadingStrategy")
                .getObject("inlineContent")
                .getObject("dialogViewModel")
                .getObject("customContent")
                .getObject("listViewModel")
                .getArray("listItems")
                .filterIsInstance<JsonObject>()
                .firstOrNull()
                ?.getObject("listItemViewModel")
                ?: JsonObject()

        override fun forAvatarExtraction(): JsonObject = viewModel.getArray("avatars").getObject(0)
    }

    private companion object {
        private const val NO_VIEWS_LOWERCASE = "no views"

        // This approach is language dependant (en-GB)
        // Leading end space is voluntary included
        private const val PREMIERES_VIDEOS_TEXT = "Premieres "
        private const val PREMIERES_LIVES_TEXT = "Scheduled for "
        private const val LIVE_BADGE_STYLE = "THUMBNAIL_OVERLAY_BADGE_STYLE_LIVE"
        private const val OVERLAYS_PATH = "contentImage.thumbnailViewModel.overlays"
        private const val WATCH_ENDPOINT_VIDEO_ID_PATH =
            "rendererContext.commandContext.onTap.innertubeCommand.watchEndpoint.videoId"
        private val PREMIERES_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm")
    }
}

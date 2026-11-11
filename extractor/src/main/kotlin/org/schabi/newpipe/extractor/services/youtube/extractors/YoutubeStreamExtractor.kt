/*
 * Created by Christian Schabesberger on 06.08.15.
 *
 * Copyright (C) 2019 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * YoutubeStreamExtractor.kt is part of NewPipe Extractor.
 *
 * NewPipe Extractor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe Extractor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe Extractor. If not, see <https://www.gnu.org/licenses/>.
 */

package org.schabi.newpipe.extractor.services.youtube.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.InfoItemExtractor
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.MetaInfo
import org.schabi.newpipe.extractor.MultiInfoItemsCollector
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.AccountTerminatedException
import org.schabi.newpipe.extractor.exceptions.AgeRestrictedContentException
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.GeographicRestrictionException
import org.schabi.newpipe.extractor.exceptions.PaidContentException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.PrivateContentException
import org.schabi.newpipe.extractor.exceptions.SignInConfirmNotBotException
import org.schabi.newpipe.extractor.exceptions.YoutubeMusicPremiumContentException
import org.schabi.newpipe.extractor.json.JsonArray
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonWriter
import org.schabi.newpipe.extractor.linkhandler.LinkHandler
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.localization.TimeAgoPatternsManager
import org.schabi.newpipe.extractor.services.youtube.ItagItem
import org.schabi.newpipe.extractor.services.youtube.ItagItem.Companion.APPROX_DURATION_MS_UNKNOWN
import org.schabi.newpipe.extractor.services.youtube.ItagItem.Companion.CONTENT_LENGTH_UNKNOWN
import org.schabi.newpipe.extractor.services.youtube.YoutubeDescriptionHelper.attributedDescriptionToHtml
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
import org.schabi.newpipe.extractor.services.youtube.YoutubeMetaInfoHelper
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.BADGES
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.CONTENT_CHECK_OK
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.CPN
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.LABEL
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.METADATA_BADGE_RENDERER
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.RACY_CHECK_OK
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.VIDEO_ID
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.fixThumbnailUrl
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.generateContentPlaybackNonce
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getImagesFromThumbnailsArray
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getJsonPostResponse
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getTextFromObject
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.prepareDesktopJsonBuilder
import org.schabi.newpipe.extractor.services.youtube.YoutubeStreamHelper
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeChannelLinkHandlerFactory
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeLinkGuards
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.Description
import org.schabi.newpipe.extractor.stream.Frameset
import org.schabi.newpipe.extractor.stream.Stream
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamSegment
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.extractor.stream.SubtitlesStream
import org.schabi.newpipe.extractor.stream.VideoStream
import org.schabi.newpipe.extractor.utils.JsonUtils
import org.schabi.newpipe.extractor.utils.LocaleCompat
import org.schabi.newpipe.extractor.utils.Parser
import org.schabi.newpipe.extractor.utils.PrivacyEvents
import org.schabi.newpipe.extractor.utils.Utils
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

public open class YoutubeStreamExtractor(
    service: StreamingService,
    linkHandler: LinkHandler,
) : StreamExtractor(service, linkHandler) {

    private var playerResponse: JsonObject? = null
    private var nextResponse: JsonObject? = null
    private var visionOsStreamingData: JsonObject? = null

    private var videoPrimaryInfoRenderer: JsonObject? = null
    private var videoSecondaryInfoRenderer: JsonObject? = null
    private var playerMicroFormatRenderer: JsonObject = JsonObject()
    private var playerCaptionsTracklistRenderer: JsonObject = JsonObject()
    private var thumbnailsArray: JsonArray = JsonArray()
    private var cachedAgeLimit = -1
    private var cachedStreamType: StreamType? = null

    // We need to store the contentPlaybackNonces because we need to append them to videoplayback
    // URLs (with the cpn parameter).
    // Also because a nonce should be unique, it should be different between clients used, so
    // two different strings are used.
    private var visionOsCpn: String = ""

    ////////////////////////////////////////////////////////////////////////////
    // Impl
    ////////////////////////////////////////////////////////////////////////////

    @get:Throws(ParsingException::class)
    override val name: String
        get() {
            assertPageFetched()
            // Try to get the video's original title, which is untranslated
            return player().getObject(VIDEO_DETAILS).getString(TITLE)?.takeIf { it.isNotEmpty() }
                ?: getTextFromObject(getVideoPrimaryInfoRenderer().getObject(TITLE))?.takeIf { it.isNotEmpty() }
                ?: throw ParsingException("Could not get name")
        }

    override val textualUploadDate: String?
        get() {
            val microformat = playerMicroFormatRenderer
            microformat.getString("uploadDate", "").orEmpty()
                .ifEmpty { microformat.getString("publishDate", "").orEmpty() }
                .takeIf { it.isNotEmpty() }
                ?.let { return it }

            val liveDetails = microformat.getObject("liveBroadcastDetails")
            // an ended live stream, then a running live stream
            liveDetails.getString("endTimestamp", "").orEmpty()
                .ifEmpty { liveDetails.getString("startTimestamp", "").orEmpty() }
                .takeIf { it.isNotEmpty() }
                ?.let { return it }
            if (streamType == StreamType.LIVE_STREAM) {
                // this should never be reached, but a live stream without upload date is valid
                return null
            }

            val rendererDateText = getTextFromObject(getVideoPrimaryInfoRenderer().getObject("dateText"))
                ?: return null
            return when {
                // Premiered on 21 Feb 2020
                rendererDateText.startsWith(PREMIERED_ON) -> rendererDateText.substring(PREMIERED_ON.length)
                // Premiered 20 hours ago / Premiered Feb 21, 2020
                rendererDateText.startsWith(PREMIERED) -> rendererDateText.substring(PREMIERED.length)
                else -> rendererDateText
            }
        }

    @get:Throws(ParsingException::class)
    override val uploadDate: DateWrapper?
        get() {
            val dateText = textualUploadDate ?: return null
            try {
                return DateWrapper.fromOffsetDateTime(dateText)
            } catch (_: ParsingException) {
                // Try other patterns first
            }
            try {
                // Premiered 20 hours ago
                TimeAgoPatternsManager.getTimeAgoParserFor(Localization("en"))?.parse(dateText)?.let { return it }
            } catch (_: ParsingException) {
                // Try other patterns first
            }
            val date = parseDateOrNull(dateText, MONTH_FIRST_DATE_FORMATTER)
                ?: parseDateOrNull(dateText, DAY_FIRST_DATE_FORMATTER)
                ?: throw ParsingException("Could not parse upload date")
            return DateWrapper(date.atStartOfDay(), true)
        }

    // TODO: this parses English formatted dates only, we need a better approach to parse the
    //  textual date
    private fun parseDateOrNull(date: String, formatter: DateTimeFormatter): LocalDate? = try {
        LocalDate.parse(date, formatter)
    } catch (_: DateTimeParseException) {
        null
    }

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() {
            assertPageFetched()
            return try {
                getImagesFromThumbnailsArray(thumbnailsArray)
            } catch (e: Exception) {
                throw ParsingException("Could not get thumbnails", e)
            }
        }

    @get:Throws(ParsingException::class)
    override val description: Description
        get() {
            assertPageFetched()
            val secondaryInfoRenderer = getVideoSecondaryInfoRenderer()
            // Description with more info on links
            val htmlDescription = getTextFromObject(secondaryInfoRenderer.getObject("description"), true)
            if (!htmlDescription.isNullOrEmpty()) {
                return Description(htmlDescription, Description.Type.HTML)
            }
            val attributedDescription =
                attributedDescriptionToHtml(secondaryInfoRenderer.getObject("attributedDescription"))
            if (!attributedDescription.isNullOrEmpty()) {
                return Description(attributedDescription, Description.Type.HTML)
            }
            // Raw non-html description
            val description = player().getObject(VIDEO_DETAILS).getString("shortDescription")
                ?: getTextFromObject(playerMicroFormatRenderer.getObject("description"))
            return Description.of(description, Description.Type.PLAIN_TEXT)
        }

    @get:Throws(ParsingException::class)
    override val ageLimit: Int
        get() {
            if (cachedAgeLimit != -1) {
                return cachedAgeLimit
            }
            val ageRestricted = getVideoSecondaryInfoRenderer()
                .getObject("metadataRowContainer")
                .getObject("metadataRowContainerRenderer")
                .getArray("rows")
                .filterIsInstance<JsonObject>()
                .flatMap { it.getObject("metadataRowRenderer").getArray("contents").filterIsInstance<JsonObject>() }
                .flatMap { it.getArray("runs").filterIsInstance<JsonObject>() }
                .any { it.getString("text", "").orEmpty().contains("Age-restricted") }
            cachedAgeLimit = if (ageRestricted) AGE_RESTRICTED_LIMIT else NO_AGE_LIMIT
            return cachedAgeLimit
        }

    @get:Throws(ParsingException::class)
    override val length: Long
        get() {
            assertPageFetched()
            return player().getObject(VIDEO_DETAILS).getString("lengthSeconds")?.toLongOrNull()
                ?: durationFromFirstAdaptiveFormat(listOf(visionOsStreamingData)).toLong()
        }

    @Throws(ParsingException::class)
    private fun durationFromFirstAdaptiveFormat(streamingDatas: List<JsonObject?>): Int {
        // a client's streaming data is null when it was not fetched
        for (streamingData in streamingDatas.filterNotNull()) {
            val adaptiveFormats = streamingData.getArray(ADAPTIVE_FORMATS)
            if (adaptiveFormats.isEmpty()) {
                continue
            }
            val durationMs = adaptiveFormats.getObject(0).getString("approxDurationMs")?.toLongOrNull()
                ?: continue
            return (durationMs / MILLIS_PER_SECOND).roundToInt()
        }
        throw ParsingException("Could not get duration")
    }

    /**
     * Attempts to parse (and return) the offset to start playing the video from.
     *
     * @return the offset (in seconds), or 0 if no timestamp is found.
     */
    @get:Throws(ParsingException::class)
    override val timeStamp: Long
        get() {
            val timestamp = getTimestampSeconds(TIMESTAMP_REGEX)
            // -2: regex for timestamp was not found
            return if (timestamp == -2L) 0 else timestamp
        }

    @get:Throws(ParsingException::class)
    override val viewCount: Long
        get() {
            val views = getTextFromObject(
                getVideoPrimaryInfoRenderer().getObject("viewCount")
                    .getObject("videoViewCountRenderer")
                    .getObject("viewCount"),
            )?.takeIf { it.isNotEmpty() }
                ?: player().getObject(VIDEO_DETAILS).getString("viewCount")?.takeIf { it.isNotEmpty() }
                ?: throw ParsingException("Could not get view count")
            if (views.lowercase(Locale.ROOT).contains("no views")) {
                return 0
            }
            return Utils.removeNonDigitCharacters(views).toLongOrNull()
                ?: throw ParsingException("Could not get view count")
        }

    @get:Throws(ParsingException::class)
    override val likeCount: Long
        get() {
            assertPageFetched()
            // If ratings are not allowed, there is no like count available
            if (!player().getObject(VIDEO_DETAILS).getBoolean("allowRatings")) {
                return -1L
            }
            val topLevelButtons = getVideoPrimaryInfoRenderer()
                .getObject("videoActions")
                .getObject("menuRenderer")
                .getArray("topLevelButtons")
                .filterIsInstance<JsonObject>()
            try {
                return likeCountFromLikeButtonViewModel(topLevelButtons)
            } catch (_: ParsingException) {
                // A segmentedLikeDislikeButtonRenderer could be returned instead of a
                // segmentedLikeDislikeButtonViewModel, so ignore extraction errors relative to
                // segmentedLikeDislikeButtonViewModel object
            }
            try {
                return likeCountFromLikeButtonRenderer(topLevelButtons)
            } catch (e: ParsingException) {
                throw ParsingException("Could not get like count", e)
            }
        }

    @get:Throws(ParsingException::class)
    override val uploaderUrl: String
        get() {
            assertPageFetched()
            // Do not use the id in the videoSecondaryRenderer object to get real id of the
            // uploader. The difference between the real id of the channel and the displayed id is
            // especially visible for music channels and autogenerated channels.
            val uploaderId = player().getObject(VIDEO_DETAILS).getString("channelId")
            if (uploaderId.isNullOrEmpty() || !UPLOADER_ID.matches(uploaderId)) {
                throw ParsingException("Could not get uploader url")
            }
            return YoutubeChannelLinkHandlerFactory.getInstance().getUrl("channel/$uploaderId")
        }

    @get:Throws(ParsingException::class)
    override val uploaderName: String
        get() {
            assertPageFetched()
            // Do not use the name in the videoSecondaryRenderer object to get real name of the
            // uploader. The difference between the real name of the channel and the displayed
            // name is especially visible for music channels and autogenerated channels.
            return player().getObject(VIDEO_DETAILS).getString("author")?.takeIf { it.isNotEmpty() }
                ?: throw ParsingException("Could not get uploader name")
        }

    @get:Throws(ParsingException::class)
    override val isUploaderVerified: Boolean
        get() {
            val videoOwnerRenderer = videoOwnerRenderer()
            if (videoOwnerRenderer.has(BADGES)) {
                return YoutubeParsingHelper.isVerified(videoOwnerRenderer.getArray(BADGES))
            }
            val channel = YoutubeParsingHelper.getFirstCollaborator(videoOwnerRenderer.getObject("navigationEndpoint"))
                ?: return false
            return YoutubeParsingHelper.hasArtistOrVerifiedIconBadgeAttachment(
                channel.getObject(TITLE).getArray("attachmentRuns"),
            )
        }

    @get:Throws(ParsingException::class)
    override val uploaderAvatars: List<Image>
        get() {
            assertPageFetched()
            val owner = videoOwnerRenderer()
            val imageList = if (owner.has("avatarStack")) {
                getImagesFromThumbnailsArray(
                    owner.getObject("avatarStack")
                        .getObject("avatarStackViewModel")
                        .getArray("avatars")
                        // only consider the first collaborator, which is the video owner
                        .getObject(0)
                        .getObject("avatarViewModel")
                        .getObject("image")
                        .getArray("sources"),
                )
            } else {
                getImagesFromThumbnailsArray(owner.getObject(THUMBNAIL).getArray(THUMBNAILS))
            }
            if (imageList.isEmpty() && cachedAgeLimit == NO_AGE_LIMIT) {
                throw ParsingException("Could not get uploader avatars")
            }
            return imageList
        }

    @get:Throws(ParsingException::class)
    override val uploaderSubscriberCount: Long
        get() {
            val owner = getVideoSecondaryInfoRenderer().getObject("owner")
            if (!owner.has("videoOwnerRenderer")) {
                // Videos without visual metadata have no owner renderer at all
                throw ParsingException("Could not get uploader subscriber count")
            }
            val videoOwnerRenderer = owner.getObject("videoOwnerRenderer")
            val subscriberCountText = if (videoOwnerRenderer.has("subscriberCountText")) {
                getTextFromObject(videoOwnerRenderer.getObject("subscriberCountText"))
            } else {
                // The subtitle is in the form: handle • subscriber count
                YoutubeParsingHelper.getFirstCollaborator(videoOwnerRenderer.getObject("navigationEndpoint"))
                    ?.getObject("subtitle")
                    ?.getString("content")
                    ?.split("•")
                    ?.getOrNull(1)
            }
            if (subscriberCountText.isNullOrEmpty()) {
                return UNKNOWN_SUBSCRIBER_COUNT
            }
            return try {
                Utils.mixedNumberWordToLong(subscriberCountText)
            } catch (e: NumberFormatException) {
                throw ParsingException("Could not get uploader subscriber count", e)
            }
        }

    private fun videoOwnerRenderer(): JsonObject =
        getVideoSecondaryInfoRenderer().getObject("owner").getObject("videoOwnerRenderer")

    @get:Throws(ParsingException::class)
    override val hlsUrl: String
        get() {
            assertPageFetched()
            // Return HLS manifest of an Apple client first because on livestreams, the HLS
            // manifest returned has separated audio and video streams
            // Also, on videos, non-Apple clients do not have an HLS manifest URL in their player
            // response
            val manifestUrl = visionOsStreamingData?.getString("hlsManifestUrl")
            if (manifestUrl.isNullOrEmpty()) {
                return ""
            }
            if (!YoutubeParsingHelper.isTrustedStreamingUrl(manifestUrl)) {
                PrivacyEvents.report(PrivacyEvents.Event.STREAM_URL_REFUSED)
                return ""
            }
            return "$manifestUrl?"
        }

    @get:Throws(IOException::class, ExtractionException::class)
    override val audioStreams: List<AudioStream>
        get() {
            assertPageFetched()
            return getItags(ADAPTIVE_FORMATS, ItagItem.ItagType.AUDIO, "audio") { buildAudioStream(it) }
        }

    @get:Throws(IOException::class, ExtractionException::class)
    override val videoStreams: List<VideoStream>
        get() {
            assertPageFetched()
            return getItags(FORMATS, ItagItem.ItagType.VIDEO, "video") { buildVideoStream(it, false) }
        }

    @get:Throws(IOException::class, ExtractionException::class)
    override val videoOnlyStreams: List<VideoStream>
        get() {
            assertPageFetched()
            return getItags(ADAPTIVE_FORMATS, ItagItem.ItagType.VIDEO_ONLY, "video-only") {
                buildVideoStream(it, true)
            }
        }

    override val serverAbrStreamingUrl: String?
        get() {
            assertPageFetched()
            // a video without SABR data has no URL
            val sabrUrl = visionOsStreamingData?.getString(SERVER_ABR_STREAMING_URL)
            if (!YoutubeParsingHelper.isTrustedStreamingUrl(sabrUrl)) {
                if (!sabrUrl.isNullOrEmpty()) {
                    PrivacyEvents.report(PrivacyEvents.Event.STREAM_URL_REFUSED)
                }
                return null
            }
            return "$sabrUrl&cpn=$visionOsCpn"
        }

    override val ustreamerConfig: String?
        get() {
            assertPageFetched()
            return player().getObject("playerConfig")
                .getObject("mediaCommonConfig")
                .getObject("mediaUstreamerRequestConfig")
                .getString("videoPlaybackUstreamerConfig")
        }

    @get:Throws(IOException::class, ExtractionException::class)
    override val subtitlesDefault: List<SubtitlesStream>
        get() = getSubtitles(MediaFormat.TTML)

    @Throws(IOException::class, ExtractionException::class)
    override fun getSubtitles(format: MediaFormat?): List<SubtitlesStream> {
        assertPageFetched()
        val mediaFormat = format ?: return emptyList()
        // We cannot store the subtitles list because the media format may change
        // TODO: use translationLanguages to apply auto translation to different language from a
        //  source language
        return playerCaptionsTracklistRenderer.getArray("captionTracks")
            .filterIsInstance<JsonObject>()
            .mapNotNull { captionTrack -> subtitlesStreamOrNull(captionTrack, mediaFormat) }
    }

    private fun subtitlesStreamOrNull(captionTrack: JsonObject, format: MediaFormat): SubtitlesStream? {
        val languageCode = captionTrack.getString("languageCode") ?: return null
        val vssId = captionTrack.getString("vssId") ?: return null
        // security: a caption track only ever comes from YouTube over https
        val baseUrl = captionTrack.getString("baseUrl")
            ?.takeIf { isHttpsUrlOnHosts(it, CAPTION_HOSTS) }
            ?: return null
        // Remove preexisting format if exists, then translation language
        val cleanUrl = baseUrl.replace(FMT_PARAMETER_REGEX, "").replace(TLANG_PARAMETER_REGEX, "")
        // YouTube ships a user-facing "name" per track (e.g. "English - CC1" vs
        // "English - DTVCC1"); pass it through so tracks that share a language code can still be
        // told apart in the picker (see TeamNewPipe/NewPipe#9218).
        // getTextFromObject returns null when the field is missing/empty.
        val trackName = getTextFromObject(captionTrack.getObject("name"))
        return SubtitlesStream.Builder()
            .setContent(cleanUrl + "&fmt=" + format.suffix, true)
            .setMediaFormat(format)
            .setLanguageCode(languageCode)
            .setName(trackName)
            .setAutoGenerated(vssId.startsWith("a."))
            .build()
    }

    override val streamType: StreamType
        get() {
            assertPageFetched()
            return checkNotNull(cachedStreamType) { "Page is not fetched. Make sure you call fetchPage()" }
        }

    private fun computeStreamType(response: JsonObject): StreamType = when {
        response.getObject(PLAYABILITY_STATUS).has("liveStreamability") -> StreamType.LIVE_STREAM
        response.getObject(VIDEO_DETAILS).getBoolean("isPostLiveDvr", false) -> StreamType.POST_LIVE_STREAM
        else -> StreamType.VIDEO_STREAM
    }

    @get:Throws(IOException::class, ExtractionException::class)
    override val relatedItems: MultiInfoItemsCollector?
        get() {
            assertPageFetched()
            if (ageLimit != NO_AGE_LIMIT) {
                return null
            }
            try {
                val collector = MultiInfoItemsCollector(serviceId)
                val results = next().getObject("contents")
                    .getObject("twoColumnWatchNextResults")
                    .getObject("secondaryResults")
                    .getObject("secondaryResults")
                    .getArray("results")
                for (result in results.filterIsInstance<JsonObject>()) {
                    // Never parse advertising or promoted renderers as items
                    if (YoutubeItemGuards.isAd(result)) {
                        continue
                    }
                    relatedItemExtractorOrNull(result)?.let { collector.commit(it) }
                }
                return collector
            } catch (e: Exception) {
                throw ParsingException("Could not get related videos", e)
            }
        }

    private fun relatedItemExtractorOrNull(result: JsonObject): InfoItemExtractor? {
        val timeAgoParser = timeAgoParser
        return when {
            result.has("compactVideoRenderer") ->
                YoutubeStreamInfoItemExtractor(result.getObject("compactVideoRenderer"), timeAgoParser)
            result.has("compactRadioRenderer") ->
                YoutubeMixOrPlaylistInfoItemExtractor(result.getObject("compactRadioRenderer"))
            result.has("compactPlaylistRenderer") ->
                YoutubeMixOrPlaylistInfoItemExtractor(result.getObject("compactPlaylistRenderer"))
            result.has("lockupViewModel") -> {
                val lockupViewModel = result.getObject("lockupViewModel")
                when (lockupViewModel.getString("contentType")) {
                    "LOCKUP_CONTENT_TYPE_PLAYLIST", "LOCKUP_CONTENT_TYPE_PODCAST" ->
                        YoutubeMixOrPlaylistLockupInfoItemExtractor(lockupViewModel)
                    "LOCKUP_CONTENT_TYPE_VIDEO" -> YoutubeStreamInfoItemLockupExtractor(lockupViewModel, timeAgoParser)
                    else -> null
                }
            }
            else -> null
        }
    }

    override val errorMessage: String?
        get() = playerResponse?.let {
            getTextFromObject(
                it.getObject(PLAYABILITY_STATUS)
                    .getObject("errorScreen")
                    .getObject("playerErrorMessageRenderer")
                    .getObject("reason"),
            )
        }

    ////////////////////////////////////////////////////////////////////////////
    // Fetch page
    ////////////////////////////////////////////////////////////////////////////

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        val videoId = id
        val localization = extractorLocalization
        val contentCountry = extractorContentCountry

        // one visitor_id request for this video: both player requests share it, and it is not
        // kept for the next video (that would link the videos watched)
        val visitorData = YoutubeStreamHelper.getVisionOsVisitorData(localization, contentCountry)
        val response = fetchVisionOsClient(localization, contentCountry, videoId, visitorData)
        cachedStreamType = computeStreamType(response)

        fetchWebClientMetadataAndSetThumbnails(localization, contentCountry, videoId, response, visitorData)

        val nextBody = JsonWriter.string(
            prepareDesktopJsonBuilder(localization, contentCountry)
                .value(VIDEO_ID, videoId)
                .value(CONTENT_CHECK_OK, true)
                .value(RACY_CHECK_OK, true)
                .done(),
        ).toByteArray(Charsets.UTF_8)
        nextResponse = getJsonPostResponse(NEXT, nextBody, localization)
    }

    @Throws(IOException::class, ExtractionException::class)
    private fun fetchVisionOsClient(
        localization: Localization,
        contentCountry: ContentCountry,
        videoId: String,
        visitorData: String,
    ): JsonObject {
        val cpn = generateContentPlaybackNonce()
        visionOsCpn = cpn
        val response = YoutubeStreamHelper.getVisionOsPlayerResponse(
            contentCountry, localization, videoId, cpn, visitorData,
        )
        playerResponse = response

        checkPlayabilityStatus(response.getObject(PLAYABILITY_STATUS))
        if (isPlayerResponseNotValid(response, videoId)) {
            throw ExtractionException("VISIONOS player response is not valid")
        }
        visionOsStreamingData = response.getObject(STREAMING_DATA)
        playerCaptionsTracklistRenderer = response.getObject(CAPTIONS).getObject(PLAYER_CAPTIONS_TRACKLIST_RENDERER)
        return response
    }

    private fun fetchWebClientMetadataAndSetThumbnails(
        localization: Localization,
        contentCountry: ContentCountry,
        videoId: String,
        visionOsResponse: JsonObject,
        visitorData: String,
    ) {
        // Thumbnails from the main player response, used when the WEB one cannot be used
        val fallbackThumbnails = visionOsResponse.getObject(VIDEO_DETAILS).getObject(THUMBNAIL).getArray(THUMBNAILS)
        val webPlayerResponse = try {
            YoutubeStreamHelper.getWebMetadataPlayerResponse(localization, contentCountry, videoId, visitorData)
        } catch (_: Exception) {
            // Ignore exceptions related to WEB client fetching or parsing, as it is not compulsory
            // to play contents
            null
        }

        // Important note: we do not checkPlayabilityStatus() here, because we use this request
        // exclusively for metadata, not for extracting streams. It turns out that when YouTube
        // returns a playability status error, the metadata may still be there.
        if (webPlayerResponse == null || isPlayerResponseNotValid(webPlayerResponse, videoId)) {
            playerMicroFormatRenderer = JsonObject()
            thumbnailsArray = fallbackThumbnails
            return
        }

        // The microformat JSON object of the content is only returned on the WEB client, so we
        // need to store it instead of getting it directly from the playerResponse
        playerMicroFormatRenderer = webPlayerResponse.getObject("microformat").getObject("playerMicroformatRenderer")

        // Try to use web player response thumbnails first, as they should contain higher quality
        // ones than mobile clients
        val thumbnailWebJsonObj = webPlayerResponse.getObject(VIDEO_DETAILS).getObject(THUMBNAIL)
        thumbnailsArray = if (thumbnailWebJsonObj.containsKey(THUMBNAILS)) {
            thumbnailWebJsonObj.getArray(THUMBNAILS)
        } else {
            fallbackThumbnails
        }
    }

    @Throws(ParsingException::class)
    private fun likeCountFromLikeButtonRenderer(topLevelButtons: List<JsonObject>): Long {
        var likesString: String? = null
        val likeToggleButtonRenderer = topLevelButtons
            .map {
                it.getObject("segmentedLikeDislikeButtonRenderer")
                    .getObject("likeButton")
                    .getObject("toggleButtonRenderer")
            }
            .firstOrNull { it.isNotEmpty() }

        if (likeToggleButtonRenderer != null) {
            // Use one of the accessibility strings available (this one has the same path as the
            // one used for comments' like count extraction)
            likesString = likeToggleButtonRenderer.getObject("accessibilityData")
                .getObject("accessibilityData")
                .getString(LABEL)
                // Use the other accessibility string available which contains the exact like count
                ?: likeToggleButtonRenderer.getObject("accessibility").getString(LABEL)
                // Last method: use the defaultText's accessibility data, which contains the exact
                // like count too, except when it is equal to 0, where a localized string is
                // returned instead
                ?: likeToggleButtonRenderer.getObject("defaultText")
                    .getObject("accessibility")
                    .getObject("accessibilityData")
                    .getString(LABEL)

            // This check only works with English localizations!
            if (likesString != null && likesString.lowercase(Locale.ROOT).contains("no likes")) {
                return 0L
            }
        }

        // If ratings are allowed and the likes string is null, it means that we couldn't extract
        // the full like count from accessibility data
        if (likesString == null) {
            throw ParsingException("Could not get like count from accessibility data")
        }
        return Utils.removeNonDigitCharacters(likesString).toLongOrNull()
            ?: throw ParsingException("Could not parse like count from accessibility data")
    }

    @Throws(ParsingException::class)
    private fun likeCountFromLikeButtonViewModel(topLevelButtons: List<JsonObject>): Long {
        // Try first with the current video actions buttons data structure
        val likeToggleButtonViewModel = topLevelButtons
            .map {
                it.getObject("segmentedLikeDislikeButtonViewModel")
                    .getObject("likeButtonViewModel")
                    .getObject("likeButtonViewModel")
                    .getObject("toggleButtonViewModel")
                    .getObject("toggleButtonViewModel")
                    .getObject("defaultButtonViewModel")
                    .getObject("buttonViewModel")
            }
            .firstOrNull { it.isNotEmpty() }
            ?: throw ParsingException("Could not find buttonViewModel object")

        val accessibilityText = likeToggleButtonViewModel.getString("accessibilityText")
            ?: throw ParsingException("Could not find buttonViewModel's accessibilityText string")

        // The like count is always returned as a number in this element, even for videos with no
        // likes
        return Utils.removeNonDigitCharacters(accessibilityText).toLongOrNull()
            ?: throw ParsingException("Could not parse like count from buttonViewModel")
    }

    private fun player(): JsonObject =
        checkNotNull(playerResponse) { "Page is not fetched. Make sure you call fetchPage()" }

    private fun next(): JsonObject =
        checkNotNull(nextResponse) { "Page is not fetched. Make sure you call fetchPage()" }

    private fun getVideoPrimaryInfoRenderer(): JsonObject =
        videoPrimaryInfoRenderer ?: getVideoInfoRenderer("videoPrimaryInfoRenderer")
            .also { videoPrimaryInfoRenderer = it }

    private fun getVideoSecondaryInfoRenderer(): JsonObject =
        videoSecondaryInfoRenderer ?: getVideoInfoRenderer("videoSecondaryInfoRenderer")
            .also { videoSecondaryInfoRenderer = it }

    private fun getVideoInfoRenderer(videoRendererName: String): JsonObject = next().getObject("contents")
        .getObject("twoColumnWatchNextResults")
        .getObject("results")
        .getObject("results")
        .getArray("contents")
        .filterIsInstance<JsonObject>()
        .firstOrNull { it.has(videoRendererName) }
        ?.getObject(videoRendererName)
        ?: JsonObject()

    ////////////////////////////////////////////////////////////////////////////
    // Streams
    ////////////////////////////////////////////////////////////////////////////

    @Throws(ParsingException::class)
    private fun <T : Stream> getItags(
        streamingDataKey: String,
        itagTypeWanted: ItagItem.ItagType,
        streamTypeExceptionMessage: String,
        streamBuilder: (ItagInfo) -> T,
    ): List<T> {
        try {
            val videoId = id
            val streamList = ArrayList<T>()
            streamsFromStreamingDataKey(videoId, visionOsStreamingData, streamingDataKey, itagTypeWanted, visionOsCpn)
                .map(streamBuilder)
                .forEach { stream ->
                    if (!Stream.containSimilarStream(stream, streamList)) {
                        streamList.add(stream)
                    }
                }
            return streamList
        } catch (e: Exception) {
            throw ParsingException("Could not get $streamTypeExceptionMessage streams", e)
        }
    }

    /**
     * Builds an [AudioStream] from an [ItagInfo]: the itag id as its id, the content and its
     * `isUrl` value, the media format, average bitrate and audio track data of the [ItagItem],
     * and the DASH delivery method for OTF streams, live streams and ended live streams.
     */
    private fun buildAudioStream(itagInfo: ItagInfo): AudioStream {
        val itagItem = itagInfo.itagItem
        val builder = AudioStream.Builder()
            .setId(itagItem.id.toString())
            .setContent(itagInfo.content, itagInfo.isUrl)
            .setMediaFormat(itagItem.mediaFormat)
            .setAverageBitrate(itagItem.averageBitrate)
            .setAudioTrackId(itagItem.audioTrackId)
            .setAudioTrackName(itagItem.audioTrackName)
            .setAudioLocale(itagItem.audioLocale)
            .setAudioTrackType(itagItem.audioTrackType)
            .setItagItem(itagItem)

        val streamType = cachedStreamType
        if (streamType == StreamType.LIVE_STREAM || streamType == StreamType.POST_LIVE_STREAM || !itagInfo.isUrl) {
            // For YouTube videos on OTF streams and for all streams of post-live streams
            // and live streams, only the DASH delivery method can be used.
            builder.setDeliveryMethod(DeliveryMethod.DASH)
        }
        return builder.build()
    }

    /**
     * Builds a [VideoStream] from an [ItagInfo]: the itag id as its id, the content and its
     * `isUrl` value, the media format, whether it is video-only, the resolution string of the
     * [ItagItem] (or an empty string), and the DASH delivery method for OTF streams, live streams
     * and ended live streams.
     */
    private fun buildVideoStream(itagInfo: ItagInfo, areStreamsVideoOnly: Boolean): VideoStream {
        val itagItem = itagInfo.itagItem
        val builder = VideoStream.Builder()
            .setId(itagItem.id.toString())
            .setContent(itagInfo.content, itagInfo.isUrl)
            .setMediaFormat(itagItem.mediaFormat)
            .setIsVideoOnly(areStreamsVideoOnly)
            .setItagItem(itagItem)
            .setResolution(itagItem.getResolutionString() ?: "")

        if (cachedStreamType != StreamType.VIDEO_STREAM || !itagInfo.isUrl) {
            // For YouTube videos on OTF streams and for all streams of post-live streams
            // and live streams, only the DASH delivery method can be used.
            builder.setDeliveryMethod(DeliveryMethod.DASH)
        }
        return builder.build()
    }

    private fun streamsFromStreamingDataKey(
        videoId: String,
        streamingData: JsonObject?,
        streamingDataKey: String,
        itagTypeWanted: ItagItem.ItagType,
        contentPlaybackNonce: String,
    ): List<ItagInfo> {
        if (streamingData == null || !streamingData.has(streamingDataKey)) {
            return emptyList()
        }
        return streamingData.getArray(streamingDataKey)
            .filterIsInstance<JsonObject>()
            .mapNotNull { formatData ->
                try {
                    val itagItem = ItagItem.getItag(formatData.getInt("itag"))
                    if (itagItem.itagType == itagTypeWanted) {
                        buildItagInfo(videoId, formatData, itagItem, itagItem.itagType, contentPlaybackNonce)
                    } else {
                        null
                    }
                } catch (_: ExtractionException) {
                    // If the itag is not supported, the n parameter of HTML5 clients cannot be
                    // decoded or buildItagInfo fails, we end up here
                    null
                } catch (_: RuntimeException) {
                    // A malformed format (a number which is not one, a missing field, an
                    // unexpected MIME type) only loses this format, not the whole list
                    null
                }
            }
    }

    @Throws(ExtractionException::class)
    private fun buildItagInfo(
        videoId: String,
        formatData: JsonObject,
        itagItem: ItagItem,
        itagType: ItagItem.ItagType,
        contentPlaybackNonce: String,
    ): ItagInfo {
        var streamUrl = rawStreamUrl(videoId, formatData, itagItem)

        // Decode the n parameter if it is present
        // If it cannot be decoded, the stream cannot be used as streaming URLs return HTTP 403
        // responses if it has not the right value
        // Exceptions thrown by
        // YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated are so
        // propagated to the parent which ignores streams in this case
        streamUrl = YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(videoId, streamUrl)

        // security: a format whose URL leads anywhere but YouTube's video servers is dropped
        // (the caller ignores the formats that throw), whatever the response contained
        if (!streamUrl.startsWith(SABR_URL_PREFIX) && !YoutubeParsingHelper.isTrustedStreamingUrl(streamUrl)) {
            PrivacyEvents.report(PrivacyEvents.Event.STREAM_URL_REFUSED)
            throw ParsingException("Streaming URL is not a YouTube video server URL")
        }

        // Add the content playback nonce to the stream URL
        streamUrl += "&$CPN=$contentPlaybackNonce"

        fillItagItem(formatData, itagItem, itagType)

        val itagInfo = ItagInfo(streamUrl, itagItem)
        itagInfo.isUrl = if (cachedStreamType == StreamType.VIDEO_STREAM) {
            !formatData.getString("type", "").equals("FORMAT_STREAM_TYPE_OTF", ignoreCase = true)
        } else {
            // We are currently not able to generate DASH manifests for running
            // livestreams, so because of the requirements of StreamInfo
            // objects, return these streams as DASH URL streams (even if they
            // are not playable).
            // Ended livestreams are returned as non URL streams
            cachedStreamType != StreamType.POST_LIVE_STREAM
        }
        return itagInfo
    }

    @Throws(ExtractionException::class)
    private fun rawStreamUrl(videoId: String, formatData: JsonObject, itagItem: ItagItem): String {
        if (formatData.has("url")) {
            return formatData.getString("url") ?: throw ParsingException("Could not get the URL of a stream")
        }
        // This url has an obfuscated signature
        val cipherString = formatData.getString(CIPHER, formatData.getString(SIGNATURE_CIPHER))
        if (cipherString.isNullOrEmpty()) {
            // HACK: SABR formats share a common URL (`serverAbrStreamingUrl`) and do not have
            // individual URLs
            return SABR_URL_PREFIX + itagItem.id
        }
        val cipher = Parser.compatParseMap(cipherString)
        val signature = YoutubeJavaScriptPlayerManager.deobfuscateSignature(videoId, cipher["s"] ?: "")
        val cipherUrl = cipher["url"]
        if (cipherUrl.isNullOrEmpty()) {
            throw ParsingException("Could not get the URL of a ciphered stream")
        }
        // the signature comes out of the player code: encoded, it cannot add parameters
        return cipherUrl + "&" + Utils.encodeUrlUtf8(cipher["sp"] ?: "sig") + "=" + Utils.encodeUrlUtf8(signature)
    }

    private fun fillItagItem(formatData: JsonObject, itagItem: ItagItem, itagType: ItagItem.ItagType) {
        val initRange = formatData.getObject("initRange")
        val indexRange = formatData.getObject("indexRange")
        val mimeType = formatData.getString("mimeType").orEmpty()
        val codec = if (mimeType.contains("codecs")) mimeType.split("\"").getOrElse(1) { "" } else ""

        itagItem.bitrate = formatData.getInt("bitrate")
        itagItem.width = formatData.getInt("width")
        itagItem.height = formatData.getInt("height")
        itagItem.initStart = initRange.getString("start")?.toIntOrNull() ?: -1
        itagItem.initEnd = initRange.getString("end")?.toIntOrNull() ?: -1
        itagItem.indexStart = indexRange.getString("start")?.toIntOrNull() ?: -1
        itagItem.indexEnd = indexRange.getString("end")?.toIntOrNull() ?: -1
        itagItem.quality = formatData.getString("quality")
        itagItem.codec = codec
        itagItem.isDrc = formatData.getBoolean("isDrc", false)
        itagItem.lastModified = formatData.getString("lastModified")?.toLongOrNull() ?: -1L
        itagItem.xtags = formatData.getString("xtags")

        val streamType = cachedStreamType
        if (streamType == StreamType.LIVE_STREAM || streamType == StreamType.POST_LIVE_STREAM) {
            itagItem.targetDurationSec = formatData.getInt("targetDurationSec")
        }

        if (itagType == ItagItem.ItagType.VIDEO || itagType == ItagItem.ItagType.VIDEO_ONLY) {
            itagItem.setFps(formatData.getInt("fps"))
        } else if (itagType == ItagItem.ItagType.AUDIO) {
            fillAudioItagItem(formatData, itagItem)
        }

        // YouTube return the content length and the approximate duration as strings
        itagItem.contentLength = formatData.getString("contentLength")?.toLongOrNull() ?: CONTENT_LENGTH_UNKNOWN
        itagItem.approxDurationMs = formatData.getString("approxDurationMs")?.toLongOrNull()
            ?: APPROX_DURATION_MS_UNKNOWN
    }

    @Throws(ParsingException::class)
    private fun fillAudioItagItem(formatData: JsonObject, itagItem: ItagItem) {
        // YouTube return the audio sample rate as a string
        itagItem.sampleRate = formatData.getString("audioSampleRate")?.toIntOrNull()
            ?: throw ParsingException("Could not get audio sample rate")
        // Most audio streams have two audio channels, so use this value if the real count cannot
        // be extracted. Doing this prevents an exception when generating the
        // AudioChannelConfiguration element of DASH manifests of audio streams in
        // YoutubeDashManifestCreatorUtils
        itagItem.audioChannels = formatData.getInt("audioChannels", DEFAULT_AUDIO_CHANNELS)

        val audioTrack = formatData.getObject("audioTrack")
        val audioTrackId = audioTrack.getString("id")
        if (!audioTrackId.isNullOrEmpty()) {
            itagItem.audioTrackId = audioTrackId
            val audioTrackIdLastLocaleCharacter = audioTrackId.indexOf('.')
            if (audioTrackIdLastLocaleCharacter != -1) {
                // Audio tracks IDs are in the form LANGUAGE_CODE.TRACK_NUMBER
                LocaleCompat.forLanguageTag(audioTrackId.substring(0, audioTrackIdLastLocaleCharacter))
                    .ifPresent { itagItem.audioLocale = it }
            }
            itagItem.audioTrackType = YoutubeParsingHelper.extractAudioTrackType(itagItem.xtags)
        }
        itagItem.audioTrackName = audioTrack.getString("displayName")
        itagItem.isAutoGenerated = audioTrack.getBoolean("isAutoDubbed", false)
    }

    /**
     * {@inheritDoc}
     * Should return a list of Frameset object that contains preview of stream frames
     *
     * **Warning:** When using this method be aware that the YouTube API very rarely returns
     * framesets, that are slightly too small e.g. framesPerPageX = 5, frameWidth = 160, but the url
     * contains a storyboard that is only 795 pixels wide (5*160 &gt; 795). You will need to handle
     * this "manually" to avoid errors.
     *
     * @see <a href="https://github.com/TeamNewPipe/NewPipe/pull/11596">TeamNewPipe/NewPipe#11596</a>
     */
    @get:Throws(ExtractionException::class)
    override val frames: List<Frameset>
        get() {
            try {
                val storyboards = player().getObject("storyboards")
                val storyboardsRenderer = storyboards.getObject(
                    if (storyboards.has("playerLiveStoryboardSpecRenderer")) {
                        "playerLiveStoryboardSpecRenderer"
                    } else {
                        "playerStoryboardSpecRenderer"
                    },
                )
                val spec = storyboardsRenderer.getString("spec")?.split('|') ?: return emptyList()
                val url = spec[0]
                // Storyboards are images served by YouTube: any other host is ignored
                if (!isHttpsUrlOnHosts(url, STORYBOARD_HOSTS)) {
                    return emptyList()
                }
                return (1 until spec.size).mapNotNull { framesetOrNull(url, it - 1, spec[it]) }
            } catch (e: Exception) {
                throw ExtractionException("Could not get frames", e)
            }
        }

    private fun framesetOrNull(url: String, level: Int, levelSpec: String): Frameset? {
        val parts = levelSpec.split('#')
        if (parts.size != STORYBOARD_SPEC_PARTS) {
            return null
        }
        val durationPerFrame = parts[5].toIntOrNull()?.takeIf { it != 0 } ?: return null
        val totalCount = parts[2].toIntOrNull() ?: return null
        val framesPerPageX = parts[3].toIntOrNull() ?: return null
        val framesPerPageY = parts[4].toIntOrNull() ?: return null
        val framesPerPage = framesPerPageX * framesPerPageY
        if (framesPerPage <= 0) {
            return null
        }
        // the page name goes into the path and the signature into the query: neither may add
        // another part to the link
        if (parts[6].isEmpty() || !STORYBOARD_TOKEN.matches(parts[6]) || !STORYBOARD_TOKEN.matches(parts[7])) {
            return null
        }
        val baseUrl = url.replace("\$L", level.toString()).replace("\$N", parts[6]) + "&sigh=" + parts[7]
        val urls = if (baseUrl.contains("\$M")) {
            val totalPages = ceil(totalCount / framesPerPage.toDouble()).toInt().coerceIn(0, MAX_STORYBOARD_PAGES)
            List(totalPages) { baseUrl.replace("\$M", it.toString()) }
        } else {
            listOf(baseUrl)
        }
        return Frameset(
            urls,
            frameWidth = parts[0].toIntOrNull() ?: return null,
            frameHeight = parts[1].toIntOrNull() ?: return null,
            totalCount = totalCount,
            durationPerFrame = durationPerFrame,
            framesPerPageX = framesPerPageX,
            framesPerPageY = framesPerPageY,
        )
    }

    override val privacy: Privacy
        get() {
            val isUnlisted = playerMicroFormatRenderer.getBoolean("isUnlisted") ||
                getVideoPrimaryInfoRenderer().getArray(BADGES)
                    .filterIsInstance<JsonObject>()
                    .any {
                        it.getObject(METADATA_BADGE_RENDERER).getObject("icon")
                            .getString("iconType") == "PRIVACY_UNLISTED"
                    }
            return if (isUnlisted) Privacy.UNLISTED else Privacy.PUBLIC
        }

    override val category: String
        get() = playerMicroFormatRenderer.getString("category").orEmpty()

    @get:Throws(ParsingException::class)
    override val licence: String
        get() {
            val metadataRowRenderer = getVideoSecondaryInfoRenderer()
                .getObject("metadataRowContainer")
                .getObject("metadataRowContainerRenderer")
                .getArray("rows")
                .getObject(0)
                .getObject("metadataRowRenderer")
            val license = getTextFromObject(metadataRowRenderer.getArray("contents").getObject(0))
            return if (license != null && getTextFromObject(metadataRowRenderer.getObject(TITLE)) == "Licence") {
                license
            } else {
                "YouTube licence"
            }
        }

    override val languageInfo: Locale?
        get() = null

    override val tags: List<String>
        get() = JsonUtils.getStringListFromJsonArray(player().getObject(VIDEO_DETAILS).getArray("keywords"))

    @get:Throws(ParsingException::class)
    override val metaInfo: List<MetaInfo>
        get() = YoutubeMetaInfoHelper.getMetaInfo(
            next().getObject("contents")
                .getObject("twoColumnWatchNextResults")
                .getObject("results")
                .getObject("results")
                .getArray("contents"),
        )

    @get:Throws(ParsingException::class)
    override val streamSegments: List<StreamSegment>
        get() {
            val response = next()
            if (!response.has("engagementPanels")) {
                return emptyList()
            }
            val segmentsArray = response.getArray("engagementPanels")
                .filterIsInstance<JsonObject>()
                .map { it.getObject("engagementPanelSectionListRenderer") }
                // Check if the panel is the correct one
                .firstOrNull {
                    it.getString("panelIdentifier") == "engagement-panel-macro-markers-description-chapters"
                }
                // Extract the data
                ?.getObject("content")
                ?.getObject("macroMarkersListRenderer")
                ?.getArray("contents")
                // If no data was found exit
                ?: return emptyList()

            val duration = length
            val segments = ArrayList<StreamSegment>()
            for (segmentJson in segmentsArray.filterIsInstance<JsonObject>()) {
                val segmentRenderer = segmentJson.getObject("macroMarkersListItemRenderer")
                val startTimeSeconds = segmentRenderer.getObject("onTap")
                    .getObject("watchEndpoint")
                    .getInt("startTimeSeconds", -1)
                if (startTimeSeconds == -1) {
                    throw ParsingException("Could not get stream segment start time.")
                }
                if (startTimeSeconds > duration) {
                    break
                }
                val title = getTextFromObject(segmentRenderer.getObject(TITLE))
                if (title.isNullOrEmpty()) {
                    throw ParsingException("Could not get stream segment title.")
                }
                val segment = StreamSegment(title, startTimeSeconds)
                segment.url = "$url?t=$startTimeSeconds"
                if (segmentRenderer.has(THUMBNAIL)) {
                    // Assume that the thumbnail with the highest resolution is at the last position
                    segmentRenderer.getObject(THUMBNAIL).getArray(THUMBNAILS)
                        .filterIsInstance<JsonObject>()
                        .lastOrNull()
                        ?.getString("url")
                        ?.let { segment.previewUrl = fixThumbnailUrl(it) }
                }
                segments.add(segment)
            }
            return segments
        }

    private companion object {
        private const val PREMIERED = "Premiered "
        private const val PREMIERED_ON = "Premiered on "
        private const val FORMATS = "formats"
        private const val ADAPTIVE_FORMATS = "adaptiveFormats"
        private const val STREAMING_DATA = "streamingData"
        private const val SERVER_ABR_STREAMING_URL = "serverAbrStreamingUrl"
        private const val SABR_URL_PREFIX = "sabr://"
        private const val NEXT = "next"
        private const val SIGNATURE_CIPHER = "signatureCipher"
        private const val CIPHER = "cipher"
        private const val PLAYER_CAPTIONS_TRACKLIST_RENDERER = "playerCaptionsTracklistRenderer"
        private const val CAPTIONS = "captions"
        private const val PLAYABILITY_STATUS = "playabilityStatus"
        private const val THUMBNAIL = "thumbnail"
        private const val THUMBNAILS = "thumbnails"
        private const val VIDEO_DETAILS = "videoDetails"
        private const val TITLE = "title"

        private const val AGE_RESTRICTED_LIMIT = 18
        private const val MILLIS_PER_SECOND = 1000f
        private const val DEFAULT_AUDIO_CHANNELS = 2
        private const val STORYBOARD_SPEC_PARTS = 8

        /** The page name and the signature of a storyboard level, placed in its URL. */
        private val STORYBOARD_TOKEN = Regex("[A-Za-z0-9\$_.=%-]{0,512}")

        /** A channel ID placed in the uploader URL. */
        private val UPLOADER_ID = Regex("[A-Za-z0-9_-]{1,64}")
        private const val MAX_STORYBOARD_PAGES = 2000
        private const val HTTPS_PORT = 443

        private const val TIMESTAMP_REGEX = """((#|&|\?)t=\d*h?\d*m?\d+s?)"""
        private val FMT_PARAMETER_REGEX = Regex("&fmt=[^&]*")
        private val TLANG_PARAMETER_REGEX = Regex("&tlang=[^&]*")
        private val MONTH_FIRST_DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH)
        private val DAY_FIRST_DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

        private val CAPTION_HOSTS = listOf("youtube.com")
        private val STORYBOARD_HOSTS = listOf("ytimg.com")

        /** Whether [url] is an HTTPS URL, without credentials, on one of [hosts] or a subdomain. */
        private fun isHttpsUrlOnHosts(url: String, hosts: List<String>): Boolean {
            val parsed = YoutubeLinkGuards.parseUrlOrNull(url) ?: return false
            if (!parsed.protocol.equals("https", ignoreCase = true) || parsed.userInfo != null) {
                return false
            }
            if (parsed.port != -1 && parsed.port != HTTPS_PORT) {
                return false
            }
            val host = parsed.host.lowercase(Locale.ROOT)
            return hosts.any { host == it || host.endsWith(".$it") }
        }

        @Throws(ParsingException::class)
        private fun checkPlayabilityStatus(playabilityStatus: JsonObject) {
            val status = playabilityStatus.getString("status")
            if (status == null || status.equals("ok", ignoreCase = true)) {
                return
            }
            val reason = playabilityStatus.getString("reason")
            if (reason != null) {
                if (status.equals("login_required", ignoreCase = true)) {
                    checkLoginRequiredStatus(playabilityStatus, status, reason)
                }
                if (status.equals("unplayable", ignoreCase = true) || status.equals("error", ignoreCase = true)) {
                    checkUnplayableStatus(reason)
                }
            }
            throw ContentNotAvailableException(
                "Got error ${YoutubeParsingHelper.boundedServerText(status)}: " +
                    "\"${reason?.let(YoutubeParsingHelper::boundedServerText)}\"",
            )
        }

        @Throws(ParsingException::class)
        private fun checkLoginRequiredStatus(playabilityStatus: JsonObject, status: String, reason: String) {
            if (reason.contains("inappropriate for some users")) {
                throw AgeRestrictedContentException("This age-restricted video cannot be watched anonymously")
            }
            val isPrivate = playabilityStatus.getArray("messages")
                .filterIsInstance<String>()
                .any { it.contains("private") }
            if (isPrivate) {
                throw PrivateContentException("This video is private")
            }
            if (reason.contains("a bot")) {
                throw SignInConfirmNotBotException(
                    "YouTube probably temporarily blocked anonymous watch access with this IP , got error " +
                        "${YoutubeParsingHelper.boundedServerText(status)}: " +
                        "\"${YoutubeParsingHelper.boundedServerText(reason)}\"",
                )
            }
        }

        @Throws(ParsingException::class)
        private fun checkUnplayableStatus(reason: String) {
            when {
                reason.contains("Music Premium") -> throw YoutubeMusicPremiumContentException()
                reason.contains("payment") -> throw PaidContentException("This video is a paid video")
                reason.contains("members") -> throw PaidContentException(
                    "This video is only available for members of the channel of this video",
                )
                reason.contains("country") -> throw GeographicRestrictionException(
                    "This video is not available in client's country.",
                )
                reason.contains("closed") || reason.contains("terminated") ->
                    throw AccountTerminatedException(YoutubeParsingHelper.boundedServerText(reason))
            }
        }

        /**
         * Checks whether a player response is invalid: YouTube may return the player response of
         * another content than the one requested.
         */
        private fun isPlayerResponseNotValid(playerResponse: JsonObject, videoId: String): Boolean =
            videoId != playerResponse.getObject(VIDEO_DETAILS).getString("videoId")
    }
}

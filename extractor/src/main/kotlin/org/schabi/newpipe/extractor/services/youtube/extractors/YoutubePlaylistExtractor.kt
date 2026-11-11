/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonArray
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonWriter
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.YOUTUBEI_V1_URL
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.extractPlaylistTypeFromPlaylistUrl
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getImagesFromThumbnailsArray
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getJsonPostResponse
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getTextFromObject
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getUrlFromNavigationEndpoint
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.prepareDesktopJsonBuilder
import org.schabi.newpipe.extractor.services.youtube.protos.playlist.PlaylistProtobufContinuation.ContinuationParams
import org.schabi.newpipe.extractor.services.youtube.protos.playlist.PlaylistProtobufContinuation.PlaylistContinuation
import org.schabi.newpipe.extractor.stream.Description
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector
import org.schabi.newpipe.extractor.utils.Utils
import java.io.IOException
import java.util.Base64

public open class YoutubePlaylistExtractor(
    service: StreamingService,
    linkHandler: ListLinkHandler,
) : PlaylistExtractor(service, linkHandler) {

    private var browseMetadataResponse: JsonObject? = null
    private var initialBrowseContinuationResponse: JsonObject? = null

    private var playlistInfo: JsonObject? = null
    private var uploaderInfo: JsonObject? = null
    private var playlistHeader: JsonObject? = null

    private var isNewPlaylistInterface = false

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        val playlistId = id
        val localization = extractorLocalization
        val body = JsonWriter.string(
            prepareDesktopJsonBuilder(localization, extractorContentCountry)
                .value("browseId", "VL$playlistId")
                .value("params", "wgYCCAA%3D") // Show unavailable videos
                .done(),
        ).toByteArray(Charsets.UTF_8)

        val metadataResponse = getJsonPostResponse(
            BROWSE_ENDPOINT,
            listOf("\$fields=$SIDEBAR,$HEADER,$MICROFORMAT,alerts"),
            body,
            localization,
        )
        YoutubeParsingHelper.defaultAlertsCheck(metadataResponse)
        browseMetadataResponse = metadataResponse
        // The "old" playlist UI can be also returned with the new one
        // The new response can be detected by checking whether a header JSON object is returned
        // in the browse response (the old returns instead a sidebar one).
        isNewPlaylistInterface = metadataResponse.has(HEADER) && !metadataResponse.has(SIDEBAR)

        val playlistContinuation = PlaylistContinuation.newBuilder()
            .setParameters(
                ContinuationParams.newBuilder()
                    .setBrowseId("VL$playlistId")
                    .setPlaylistId(playlistId)
                    .setContinuationProperties(PLAYLIST_CONTINUATION_PROPERTIES_BASE64)
                    .build(),
            )
            .build()
        val continuation = Utils.encodeUrlUtf8(
            Base64.getUrlEncoder().encodeToString(playlistContinuation.toByteArray()),
        )
        initialBrowseContinuationResponse = getJsonPostResponse(
            BROWSE_ENDPOINT,
            JsonWriter.string(
                prepareDesktopJsonBuilder(localization, extractorContentCountry)
                    .value("continuation", continuation)
                    .done(),
            ).toByteArray(Charsets.UTF_8),
            localization,
        )
    }

    private fun metadataResponse(): JsonObject =
        checkNotNull(browseMetadataResponse) { "Page is not fetched. Make sure you call fetchPage()" }

    private fun sidebarItems(): List<JsonObject> = metadataResponse().getObject(SIDEBAR)
        .getObject("playlistSidebarRenderer")
        .getArray("items")
        .filterIsInstance<JsonObject>()

    @Throws(ParsingException::class)
    private fun getUploaderInfo(): JsonObject = uploaderInfo ?: sidebarItems()
        .map { it.getObject("playlistSidebarSecondaryInfoRenderer").getObject("videoOwner") }
        .firstOrNull { it.has(VIDEO_OWNER_RENDERER) }
        ?.getObject(VIDEO_OWNER_RENDERER)
        ?.also { uploaderInfo = it }
        ?: throw ParsingException("Could not get uploader info")

    @Throws(ParsingException::class)
    private fun getPlaylistInfo(): JsonObject = playlistInfo ?: sidebarItems()
        .firstOrNull { it.has("playlistSidebarPrimaryInfoRenderer") }
        ?.getObject("playlistSidebarPrimaryInfoRenderer")
        ?.also { playlistInfo = it }
        ?: throw ParsingException("Could not get playlist info")

    private fun getPlaylistHeader(): JsonObject = playlistHeader
        ?: metadataResponse().getObject(HEADER).getObject("playlistHeaderRenderer").also { playlistHeader = it }

    @get:Throws(ParsingException::class)
    override val name: String
        get() {
            val name = getTextFromObject(getPlaylistInfo().getObject(TITLE))
            if (!name.isNullOrEmpty()) {
                return name
            }
            return metadataResponse().getObject(MICROFORMAT)
                .getObject("microformatDataRenderer")
                .getString(TITLE)
                ?: throw ParsingException("Could not get playlist name")
        }

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() {
            val playlistMetadataThumbnailsArray = if (isNewPlaylistInterface) {
                getPlaylistHeader().getObject("playlistHeaderBanner")
                    .getObject("heroPlaylistThumbnailRenderer")
                    .getObject(THUMBNAIL)
                    .getArray(THUMBNAILS)
            } else {
                // getPlaylistInfo() instead of the raw cache, which may not be filled yet
                playlistInfoOrNull()?.getObject("thumbnailRenderer")
                    ?.getObject("playlistVideoThumbnailRenderer")
                    ?.getObject(THUMBNAIL)
                    ?.getArray(THUMBNAILS)
            }
            if (!playlistMetadataThumbnailsArray.isNullOrEmpty()) {
                return getImagesFromThumbnailsArray(playlistMetadataThumbnailsArray)
            }
            // This data structure is returned in both layouts
            val microFormatThumbnailsArray = metadataResponse().getObject(MICROFORMAT)
                .getObject("microformatDataRenderer")
                .getObject(THUMBNAIL)
                .getArray(THUMBNAILS)
            if (!microFormatThumbnailsArray.isEmpty()) {
                return getImagesFromThumbnailsArray(microFormatThumbnailsArray)
            }
            throw ParsingException("Could not get playlist thumbnails")
        }

    private fun playlistInfoOrNull(): JsonObject? = try {
        getPlaylistInfo()
    } catch (_: ParsingException) {
        null
    }

    @get:Throws(ParsingException::class)
    override val uploaderUrl: String
        get() = try {
            getUrlFromNavigationEndpoint(
                if (isNewPlaylistInterface) {
                    getPlaylistHeader().getObject("ownerText")
                        .getArray("runs")
                        .getObject(0)
                        .getObject("navigationEndpoint")
                } else {
                    getUploaderInfo().getObject("navigationEndpoint")
                },
            )
        } catch (e: Exception) {
            throw ParsingException("Could not get playlist uploader url", e)
        } ?: throw ParsingException("Could not get playlist uploader url")

    @get:Throws(ParsingException::class)
    override val uploaderName: String
        get() = try {
            getTextFromObject(
                if (isNewPlaylistInterface) {
                    getPlaylistHeader().getObject("ownerText")
                } else {
                    getUploaderInfo().getObject(TITLE)
                },
            )
        } catch (e: Exception) {
            throw ParsingException("Could not get playlist uploader name", e)
        } ?: throw ParsingException("Could not get playlist uploader name")

    @get:Throws(ParsingException::class)
    override val uploaderAvatars: List<Image>
        get() {
            if (isNewPlaylistInterface) {
                // The new playlist interface does not provide an uploader avatar
                return emptyList()
            }
            return try {
                getImagesFromThumbnailsArray(getUploaderInfo().getObject(THUMBNAIL).getArray(THUMBNAILS))
            } catch (e: Exception) {
                throw ParsingException("Could not get playlist uploader avatars", e)
            }
        }

    // YouTube does not provide this information
    @get:Throws(ParsingException::class)
    override val isUploaderVerified: Boolean
        get() = false

    @get:Throws(ParsingException::class)
    override val streamCount: Long
        get() {
            if (isNewPlaylistInterface) {
                val header = getPlaylistHeader()
                digitsOrNull(getTextFromObject(header.getObject("numVideosText")))?.let { return it }
                digitsOrNull(getTextFromObject(header.getArray("byline").getObject(0).getObject("text")))
                    ?.let { return it }
            }
            // These data structures are returned in both layouts
            val source = if (isNewPlaylistInterface) getPlaylistHeader() else getPlaylistInfo()
            for (key in listOf("briefStats", "stats")) {
                val stats = source.getArray(key)
                if (!stats.isEmpty()) {
                    val statsText = getTextFromObject(stats.getObject(0)) ?: continue
                    return digitsOrNull(statsText) ?: throw ParsingException("Could not get stream count")
                }
            }
            return ITEM_COUNT_UNKNOWN
        }

    private fun digitsOrNull(text: String?): Long? = text?.let { Utils.removeNonDigitCharacters(it).toLongOrNull() }

    @get:Throws(ParsingException::class)
    override val description: Description
        get() = Description.of(
            getTextFromObject(getPlaylistInfo().getObject("description"), true),
            Description.Type.HTML,
        )

    @get:Throws(IOException::class, ExtractionException::class)
    override val initialPage: InfoItemsPage<StreamInfoItem>
        get() {
            val collector = StreamInfoItemsCollector(serviceId)
            val firstAction = checkNotNull(initialBrowseContinuationResponse) {
                "Page is not fetched. Make sure you call fetchPage()"
            }.getArray(ON_RESPONSE_RECEIVED_ACTIONS).getObject(0)

            // New structure with lockup view models uses appendContinuationItemsAction for the
            // initial continuation too
            val initialItems = firstAction.getObject("reloadContinuationItemsCommand")
                .getArray(CONTINUATION_ITEMS)
                .ifEmpty { firstAction.getObject(APPEND_CONTINUATION_ITEMS_ACTION).getArray(CONTINUATION_ITEMS) }

            val isCoursePlaylist = getPlaylistHeader().getObject("onDescriptionTap")
                .getObject(COMMAND_EXECUTOR_COMMAND)
                .getArray("commands")
                .filterIsInstance<JsonObject>()
                .any {
                    "engagement-panel-course-metadata" ==
                        it.getObject("showEngagementPanelEndpoint").getObject("identifier").getString("tag")
                }

            collectStreamsFrom(collector, initialItems, isCoursePlaylist)
            return InfoItemsPage(collector, getNextPageFrom(initialItems, isCoursePlaylist))
        }

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<StreamInfoItem> {
        require(page != null && !page.url.isNullOrEmpty()) { "Page doesn't contain an URL" }

        val collector = StreamInfoItemsCollector(serviceId)
        val ajaxJson = getJsonPostResponse(BROWSE_ENDPOINT, page.body, extractorLocalization)
        val continuation = ajaxJson.getArray(ON_RESPONSE_RECEIVED_ACTIONS)
            .getObject(0)
            .getObject(APPEND_CONTINUATION_ITEMS_ACTION)
            .getArray(CONTINUATION_ITEMS)
        val isCoursePlaylist = page.id.equals("true", ignoreCase = true)

        collectStreamsFrom(collector, continuation, isCoursePlaylist)
        return InfoItemsPage(collector, getNextPageFrom(continuation, isCoursePlaylist))
    }

    private fun getNextPageFrom(contents: JsonArray, isCoursePlaylist: Boolean): Page? {
        val lastElement = contents.lastOrNull() as? JsonObject ?: return null
        val continuation = when {
            lastElement.has("continuationItemRenderer") -> {
                val continuationEndpoint = lastElement.getObject("continuationItemRenderer")
                    .getObject("continuationEndpoint")
                val continuationObject = if (continuationEndpoint.has(COMMAND_EXECUTOR_COMMAND)) {
                    // This structure is only used at the time this code is written in initial
                    // playlist responses. continuationItemRenderer objects return multiple
                    // commands: one containing the continuation we need and one a
                    // playlistVotingRefreshPopupCommand
                    continuationEndpoint.getObject(COMMAND_EXECUTOR_COMMAND)
                        .getArray("commands")
                        .filterIsInstance<JsonObject>()
                        .firstOrNull { it.has(CONTINUATION_COMMAND) }
                        ?: JsonObject()
                } else {
                    // At the time this code is written, this "classic" continuation structure is
                    // only returned in browse responses of continuation requests
                    continuationEndpoint
                }
                continuationObject.getObject(CONTINUATION_COMMAND).getString("token")
            }
            lastElement.has("continuationItemViewModel") -> lastElement.getObject("continuationItemViewModel")
                .getObject(CONTINUATION_COMMAND)
                .getObject("innertubeCommand")
                .getObject(CONTINUATION_COMMAND)
                .getString("token")
            else -> null
        }
        if (continuation.isNullOrEmpty()) {
            // Invalid continuation or no continuation found
            return null
        }
        val body = JsonWriter.string(
            prepareDesktopJsonBuilder(extractorLocalization, extractorContentCountry)
                .value("continuation", continuation)
                .done(),
        ).toByteArray(Charsets.UTF_8)
        return Page(BROWSE_URL, isCoursePlaylist.toString(), body)
    }

    private fun collectStreamsFrom(
        collector: StreamInfoItemsCollector,
        videos: JsonArray,
        isCoursePlaylist: Boolean,
    ) {
        val timeAgoParser = timeAgoParser
        for (video in videos.filterIsInstance<JsonObject>()) {
            // Never parse advertising or promoted renderers as items
            if (YoutubeItemGuards.isAd(video)) {
                continue
            }
            when {
                video.has(PLAYLIST_VIDEO_RENDERER) -> collector.commit(
                    coursePlaylistStreamExtractor(video.getObject(PLAYLIST_VIDEO_RENDERER), isCoursePlaylist),
                )
                video.has(RICH_ITEM_RENDERER) -> {
                    val content = video.getObject(RICH_ITEM_RENDERER).getObject("content")
                    if (content.has(REEL_ITEM_RENDERER) && !YoutubeItemGuards.isAd(content)) {
                        collector.commit(YoutubeReelInfoItemExtractor(content.getObject(REEL_ITEM_RENDERER)))
                    }
                }
                video.has(LOCKUP_VIEW_MODEL) -> collector.commit(
                    coursePlaylistLockupExtractor(video.getObject(LOCKUP_VIEW_MODEL), isCoursePlaylist),
                )
            }
        }
    }

    private fun coursePlaylistStreamExtractor(
        renderer: JsonObject,
        isCoursePlaylist: Boolean,
    ): YoutubeStreamInfoItemExtractor = object : YoutubeStreamInfoItemExtractor(renderer, timeAgoParser) {
        @get:Throws(ParsingException::class)
        override val uploaderName: String?
            get() = if (isCoursePlaylist) this@YoutubePlaylistExtractor.uploaderName else super.uploaderName

        @get:Throws(ParsingException::class)
        override val uploaderUrl: String?
            get() = if (isCoursePlaylist) this@YoutubePlaylistExtractor.uploaderUrl else super.uploaderUrl
    }

    private fun coursePlaylistLockupExtractor(
        lockupViewModel: JsonObject,
        isCoursePlaylist: Boolean,
    ): YoutubeStreamInfoItemLockupExtractor = object : YoutubeStreamInfoItemLockupExtractor(
        lockupViewModel,
        timeAgoParser,
    ) {
        override val isChannelOrCoursePlaylistLockupItem: Boolean
            get() = isCoursePlaylist

        @get:Throws(ParsingException::class)
        override val uploaderName: String?
            get() = if (isCoursePlaylist) this@YoutubePlaylistExtractor.uploaderName else super.uploaderName

        @get:Throws(ParsingException::class)
        override val uploaderUrl: String?
            get() = if (isCoursePlaylist) this@YoutubePlaylistExtractor.uploaderUrl else super.uploaderUrl
    }

    @get:Throws(ParsingException::class)
    override val playlistType: PlaylistInfo.PlaylistType
        get() = extractPlaylistTypeFromPlaylistUrl(url)

    private companion object {
        // Names of some objects in JSON response frequently used in this class
        private const val PLAYLIST_VIDEO_RENDERER = "playlistVideoRenderer"
        private const val RICH_ITEM_RENDERER = "richItemRenderer"
        private const val REEL_ITEM_RENDERER = "reelItemRenderer"
        private const val LOCKUP_VIEW_MODEL = "lockupViewModel"
        private const val SIDEBAR = "sidebar"
        private const val HEADER = "header"
        private const val VIDEO_OWNER_RENDERER = "videoOwnerRenderer"
        private const val MICROFORMAT = "microformat"
        private const val COMMAND_EXECUTOR_COMMAND = "commandExecutorCommand"
        private const val THUMBNAIL = "thumbnail"
        private const val THUMBNAILS = "thumbnails"
        private const val ON_RESPONSE_RECEIVED_ACTIONS = "onResponseReceivedActions"
        private const val CONTINUATION_ITEMS = "continuationItems"
        private const val APPEND_CONTINUATION_ITEMS_ACTION = "appendContinuationItemsAction"
        private const val CONTINUATION_COMMAND = "continuationCommand"
        private const val TITLE = "title"

        private const val BROWSE_ENDPOINT = "browse"
        private val BROWSE_URL = YOUTUBEI_V1_URL + "browse?" + DISABLE_PRETTY_PRINT_PARAMETER

        // Continuation properties requesting first page and showing unavailable videos
        private const val PLAYLIST_CONTINUATION_PROPERTIES_BASE64 = "CADCBgIIAA%3D%3D"
    }
}

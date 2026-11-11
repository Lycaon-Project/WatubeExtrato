/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.MultiInfoItemsCollector
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonArray
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonWriter
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.localization.TimeAgoParser
import org.schabi.newpipe.extractor.services.youtube.YoutubeChannelHelper
import org.schabi.newpipe.extractor.services.youtube.YoutubeChannelHelper.getChannelResponse
import org.schabi.newpipe.extractor.services.youtube.YoutubeChannelHelper.resolveChannelId
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.YOUTUBEI_V1_URL
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getJsonPostResponse
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.prepareDesktopJsonBuilder
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeChannelTabLinkHandlerFactory
import java.io.IOException

/**
 * A [ChannelTabExtractor] implementation for the YouTube service.
 *
 * It currently supports `Videos`, `Shorts`, `Live`, `Playlists`, `Albums` and `Channels` tabs.
 */
public open class YoutubeChannelTabExtractor(
    service: StreamingService,
    linkHandler: ListLinkHandler,
) : ChannelTabExtractor(service, linkHandler) {

    protected var channelHeader: YoutubeChannelHelper.ChannelHeader? = null

    private var jsonResponse: JsonObject? = null
    private var channelId: String? = null

    @Throws(ParsingException::class)
    private fun channelTabsParameters(): String = when (val name = name) {
        ChannelTabs.VIDEOS -> "EgZ2aWRlb3PyBgQKAjoA"
        ChannelTabs.SHORTS -> "EgZzaG9ydHPyBgUKA5oBAA%3D%3D"
        ChannelTabs.LIVESTREAMS -> "EgdzdHJlYW1z8gYECgJ6AA%3D%3D"
        ChannelTabs.ALBUMS -> "EghyZWxlYXNlc_IGBQoDsgEA"
        ChannelTabs.PLAYLISTS -> "EglwbGF5bGlzdHPyBgQKAkIA"
        ChannelTabs.COURSES -> "Egdjb3Vyc2Vz8gYFCgPCAQA%3D"
        ChannelTabs.PODCASTS -> "Eghwb2RjYXN0c_IGBQoDugEA"
        else -> throw ParsingException("Unsupported channel tab: $name")
    }

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        val channelIdFromId = resolveChannelId(super.id)
        val data = getChannelResponse(
            channelIdFromId,
            channelTabsParameters(),
            extractorLocalization,
            extractorContentCountry,
        )
        jsonResponse = data.jsonResponse
        channelHeader = YoutubeChannelHelper.getChannelHeader(data.jsonResponse)
        channelId = data.channelId
    }

    @get:Throws(ParsingException::class)
    override val url: String
        get() = try {
            YoutubeChannelTabLinkHandlerFactory.getInstance().getUrl("channel/$id", listOf(name), "")
        } catch (_: ParsingException) {
            super.url
        }

    @get:Throws(ParsingException::class)
    override val id: String
        get() = YoutubeChannelHelper.getChannelId(channelHeader, fetchedJsonResponse(), channelId)

    @get:Throws(ParsingException::class)
    protected open val channelName: String
        get() {
            val response = fetchedJsonResponse()
            return YoutubeChannelHelper.getChannelName(
                channelHeader,
                YoutubeChannelHelper.getChannelAgeGateRenderer(response),
                response,
            )
        }

    private fun fetchedJsonResponse(): JsonObject =
        checkNotNull(jsonResponse) { "Page is not fetched. Make sure you call fetchPage()" }

    @get:Throws(IOException::class, ExtractionException::class)
    override val initialPage: InfoItemsPage<InfoItem>
        get() {
            val collector = MultiInfoItemsCollector(serviceId)
            val items = getTabData()?.getObject("content")?.let { tabContent ->
                val sectionListContents = tabContent.getObject("sectionListRenderer").getArray("contents")
                sectionListContents.getObject(0)
                    .getObject("itemSectionRenderer")
                    .getArray("contents")
                    .getObject(0)
                    .getObject("gridRenderer")
                    .getArray("items")
                    .ifEmpty { tabContent.getObject("richGridRenderer").getArray("contents") }
                    .ifEmpty { sectionListContents }
            } ?: JsonArray()

            val header = channelHeader
            val verifiedStatus = when {
                header == null -> VerifiedStatus.UNKNOWN
                YoutubeChannelHelper.isChannelVerified(header) -> VerifiedStatus.VERIFIED
                else -> VerifiedStatus.UNVERIFIED
            }

            // If a channel tab is fetched, the next page requires channel ID and name, as channel
            // streams do not have their channel specified.
            // We also need to set the visitor data here when it should be enabled, as it is
            // required to get continuations on some channel tabs, and we need a way to pass it
            // between pages
            val channelName = channelName
            val channelUrl = url
            val continuation = collectItemsFrom(
                collector,
                items,
                ItemContext(verifiedStatus, channelName, channelUrl, timeAgoParser),
                depth = 0,
            )
            val nextPage = getNextPageFrom(continuation, listOf(channelName, channelUrl, verifiedStatus.toString()))
            return InfoItemsPage(collector, nextPage)
        }

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<InfoItem> {
        require(page != null && !page.url.isNullOrEmpty()) { "Page doesn't contain an URL" }

        val channelIds = page.ids.orEmpty()
        val collector = MultiInfoItemsCollector(serviceId)
        val ajaxJson = getJsonPostResponse("browse", page.body, extractorLocalization)
        val sectionListContinuation = ajaxJson.getArray("onResponseReceivedActions")
            .filterIsInstance<JsonObject>()
            .firstOrNull { it.has("appendContinuationItemsAction") }
            ?.getObject("appendContinuationItemsAction")
            ?: JsonObject()

        val continuation = collectItemsFrom(
            collector,
            sectionListContinuation.getArray("continuationItems"),
            itemContextFrom(channelIds),
            depth = 0,
        )
        return InfoItemsPage(collector, getNextPageFrom(continuation, channelIds))
    }

    /** The tab renderer of this tab, or `null` when it is missing or has no content. */
    internal open fun getTabData(): JsonObject? {
        val urlSuffix = YoutubeChannelTabLinkHandlerFactory.getUrlSuffix(name)
        val tabRenderer = fetchedJsonResponse().getObject("contents")
            .getObject("twoColumnBrowseResultsRenderer")
            .getArray("tabs")
            .filterIsInstance<JsonObject>()
            .filter { it.has("tabRenderer") }
            .map { it.getObject("tabRenderer") }
            .firstOrNull {
                it.getObject("endpoint")
                    .getObject("commandMetadata")
                    .getObject("webCommandMetadata")
                    .getString("url", "")
                    .orEmpty()
                    .endsWith(urlSuffix)
            }
            ?: return null

        // Check if tab has no content
        val tabContents = tabRenderer.getObject("content")
            .getObject("sectionListRenderer")
            .getArray("contents")
            .getObject(0)
            .getObject("itemSectionRenderer")
            .getArray("contents")
        val isEmptyTab = tabContents.size == 1 && tabContents.getObject(0).has("messageRenderer")
        return if (isEmptyTab) null else tabRenderer
    }

    private fun itemContextFrom(channelIds: List<String>): ItemContext {
        if (channelIds.size < 3) {
            return ItemContext(VerifiedStatus.UNKNOWN, null, null, timeAgoParser)
        }
        // An unknown value can be given if someone passes a third channel ID which is not of the
        // enum type in the getPage method, use the UNKNOWN VerifiedStatus enum value in this case
        val verifiedStatus = VerifiedStatus.entries.firstOrNull { it.name == channelIds[2] }
            ?: VerifiedStatus.UNKNOWN
        return ItemContext(verifiedStatus, channelIds[0], channelIds[1], timeAgoParser)
    }

    /**
     * Collects every item and returns the first continuation found, if any.
     */
    private fun collectItemsFrom(
        collector: MultiInfoItemsCollector,
        items: JsonArray,
        context: ItemContext,
        depth: Int,
    ): JsonObject? {
        var continuation: JsonObject? = null
        for (item in items.filterIsInstance<JsonObject>()) {
            val itemContinuation = collectItem(collector, item, context, depth)
            if (continuation == null) {
                continuation = itemContinuation
            }
        }
        return continuation
    }

    /**
     * Collects one item, recursing into shelves and lists up to [MAX_NESTING_DEPTH] levels, and
     * returns its continuation, if any.
     */
    private fun collectItem(
        collector: MultiInfoItemsCollector,
        item: JsonObject,
        context: ItemContext,
        depth: Int,
    ): JsonObject? {
        // Never parse advertising or promoted renderers as items
        if (YoutubeItemGuards.isAd(item) || depth > MAX_NESTING_DEPTH) {
            return null
        }
        when {
            item.has("richItemRenderer") -> {
                val richItem = item.getObject("richItemRenderer").getObject("content")
                if (!YoutubeItemGuards.isAd(richItem)) {
                    collectRichItem(collector, richItem, context)
                }
            }
            item.has("gridVideoRenderer") -> commitVideo(collector, item.getObject("gridVideoRenderer"), context)
            item.has("gridPlaylistRenderer") ->
                commitPlaylist(collector, item.getObject("gridPlaylistRenderer"), context)
            item.has("gridShowRenderer") -> collector.commit(
                YoutubeGridShowRendererChannelInfoItemExtractor(item.getObject("gridShowRenderer"), context),
            )
            item.has("shelfRenderer") ->
                return collectItem(collector, item.getObject("shelfRenderer").getObject("content"), context, depth + 1)
            item.has("itemSectionRenderer") -> return collectItemsFrom(
                collector,
                item.getObject("itemSectionRenderer").getArray("contents"),
                context,
                depth + 1,
            )
            item.has("horizontalListRenderer") -> return collectItemsFrom(
                collector,
                item.getObject("horizontalListRenderer").getArray("items"),
                context,
                depth + 1,
            )
            item.has("expandedShelfContentsRenderer") -> return collectItemsFrom(
                collector,
                item.getObject("expandedShelfContentsRenderer").getArray("items"),
                context,
                depth + 1,
            )
            item.has("lockupViewModel") -> commitLockup(collector, item.getObject("lockupViewModel"), context)
            item.has("continuationItemRenderer") -> return item.getObject("continuationItemRenderer")
        }
        return null
    }

    private fun collectRichItem(collector: MultiInfoItemsCollector, richItem: JsonObject, context: ItemContext) {
        when {
            richItem.has("videoRenderer") -> commitVideo(collector, richItem.getObject("videoRenderer"), context)
            richItem.has("reelItemRenderer") -> commitReel(collector, richItem.getObject("reelItemRenderer"), context)
            richItem.has("shortsLockupViewModel") ->
                commitShortsLockup(collector, richItem.getObject("shortsLockupViewModel"), context)
            richItem.has("playlistRenderer") ->
                commitPlaylist(collector, richItem.getObject("playlistRenderer"), context)
            richItem.has("lockupViewModel") -> commitLockup(collector, richItem.getObject("lockupViewModel"), context)
        }
    }

    private fun commitLockup(collector: MultiInfoItemsCollector, lockupViewModel: JsonObject, context: ItemContext) {
        when (lockupViewModel.getString("contentType")) {
            "LOCKUP_CONTENT_TYPE_PLAYLIST", "LOCKUP_CONTENT_TYPE_PODCAST" ->
                commitPlaylistLockup(collector, lockupViewModel, context)
            "LOCKUP_CONTENT_TYPE_VIDEO" -> commitVideoLockup(collector, lockupViewModel, context)
        }
    }

    private fun commitReel(collector: MultiInfoItemsCollector, reelItemRenderer: JsonObject, context: ItemContext) {
        collector.commit(
            object : YoutubeReelInfoItemExtractor(reelItemRenderer) {
                @get:Throws(ParsingException::class)
                override val uploaderName: String?
                    get() = context.channelName.ifNullOrEmpty { super.uploaderName }

                @get:Throws(ParsingException::class)
                override val uploaderUrl: String?
                    get() = context.channelUrl.ifNullOrEmpty { super.uploaderUrl }

                @get:Throws(ParsingException::class)
                override val isUploaderVerified: Boolean
                    get() = context.verifiedStatus == VerifiedStatus.VERIFIED
            },
        )
    }

    private fun commitShortsLockup(
        collector: MultiInfoItemsCollector,
        shortsLockupViewModel: JsonObject,
        context: ItemContext,
    ) {
        collector.commit(
            object : YoutubeShortsLockupInfoItemExtractor(shortsLockupViewModel) {
                @get:Throws(ParsingException::class)
                override val uploaderName: String?
                    get() = context.channelName.ifNullOrEmpty { super.uploaderName }

                @get:Throws(ParsingException::class)
                override val uploaderUrl: String?
                    get() = context.channelUrl.ifNullOrEmpty { super.uploaderUrl }

                @get:Throws(ParsingException::class)
                override val isUploaderVerified: Boolean
                    get() = context.verifiedStatus == VerifiedStatus.VERIFIED
            },
        )
    }

    private fun commitVideoLockup(
        collector: MultiInfoItemsCollector,
        lockupViewModel: JsonObject,
        context: ItemContext,
    ) {
        collector.commit(
            object : YoutubeStreamInfoItemLockupExtractor(lockupViewModel, context.timeAgoParser) {
                override val isChannelOrCoursePlaylistLockupItem: Boolean
                    get() = true

                // Uploader avatars are not available in channels' video items
                @get:Throws(ParsingException::class)
                override val uploaderAvatars: List<Image>
                    get() = emptyList()

                @get:Throws(ParsingException::class)
                override val uploaderName: String?
                    get() = context.channelName.ifNullOrEmpty { super.uploaderName }

                @get:Throws(ParsingException::class)
                override val uploaderUrl: String?
                    get() = context.channelUrl.ifNullOrEmpty { super.uploaderUrl }

                @get:Throws(ParsingException::class)
                override val isUploaderVerified: Boolean
                    get() = context.verifiedStatus == VerifiedStatus.VERIFIED
            },
        )
    }

    private fun commitPlaylistLockup(
        collector: MultiInfoItemsCollector,
        playlistLockupViewModel: JsonObject,
        context: ItemContext,
    ) {
        collector.commit(
            object : YoutubeMixOrPlaylistLockupInfoItemExtractor(playlistLockupViewModel) {
                @get:Throws(ParsingException::class)
                override val uploaderName: String?
                    get() = context.channelName.ifNullOrEmpty { super.uploaderName }

                @get:Throws(ParsingException::class)
                override val uploaderUrl: String?
                    get() = context.channelUrl.ifNullOrEmpty { super.uploaderUrl }

                @get:Throws(ParsingException::class)
                override val isUploaderVerified: Boolean
                    get() = context.verifiedStatus.toBooleanOrNull() ?: super.isUploaderVerified
            },
        )
    }

    private fun commitVideo(collector: MultiInfoItemsCollector, jsonObject: JsonObject, context: ItemContext) {
        collector.commit(
            object : YoutubeStreamInfoItemExtractor(jsonObject, context.timeAgoParser) {
                @get:Throws(ParsingException::class)
                override val uploaderName: String?
                    get() = context.channelName.ifNullOrEmpty { super.uploaderName }

                @get:Throws(ParsingException::class)
                override val uploaderUrl: String?
                    get() = context.channelUrl.ifNullOrEmpty { super.uploaderUrl }

                @get:Throws(ParsingException::class)
                override val isUploaderVerified: Boolean
                    get() = context.verifiedStatus.toBooleanOrNull() ?: super.isUploaderVerified
            },
        )
    }

    private fun commitPlaylist(collector: MultiInfoItemsCollector, jsonObject: JsonObject, context: ItemContext) {
        collector.commit(
            object : YoutubePlaylistInfoItemExtractor(jsonObject) {
                @get:Throws(ParsingException::class)
                override val uploaderName: String?
                    get() = context.channelName.ifNullOrEmpty { super.uploaderName }

                @get:Throws(ParsingException::class)
                override val uploaderUrl: String?
                    get() = context.channelUrl.ifNullOrEmpty { super.uploaderUrl }

                @get:Throws(ParsingException::class)
                override val isUploaderVerified: Boolean
                    get() = context.verifiedStatus.toBooleanOrNull() ?: super.isUploaderVerified
            },
        )
    }

    private fun getNextPageFrom(continuations: JsonObject?, channelIds: List<String>): Page? {
        if (continuations.isNullOrEmpty()) {
            return null
        }
        val continuation = continuations.getObject("continuationEndpoint")
            .getObject("continuationCommand")
            .getString("token")
        val body = JsonWriter.string(
            prepareDesktopJsonBuilder(extractorLocalization, extractorContentCountry)
                .value("continuation", continuation)
                .done(),
        ).toByteArray(Charsets.UTF_8)
        return Page(BROWSE_URL, null, channelIds, null, body)
    }

    /**
     * A [YoutubeChannelTabExtractor] for the `Videos` tab, if it has been already fetched.
     */
    public class VideosTabExtractor internal constructor(
        service: StreamingService,
        linkHandler: ListLinkHandler,
        private val tabRenderer: JsonObject,
        channelHeader: YoutubeChannelHelper.ChannelHeader?,
        override val channelName: String,
        private val tabChannelId: String,
        private val channelUrl: String,
    ) : YoutubeChannelTabExtractor(service, linkHandler) {

        init {
            this.channelHeader = channelHeader
        }

        override fun onFetchPage(downloader: Downloader) {
            // Nothing to do, the initial data was already fetched and is stored in the link handler
        }

        @get:Throws(ParsingException::class)
        override val id: String
            get() = tabChannelId

        @get:Throws(ParsingException::class)
        override val url: String
            get() = channelUrl

        override fun getTabData(): JsonObject = tabRenderer
    }

    /**
     * Enum representing the verified state of a channel
     */
    private enum class VerifiedStatus {
        VERIFIED,
        UNVERIFIED,
        UNKNOWN,
        ;

        /** `true` or `false` when known, `null` for [UNKNOWN]. */
        fun toBooleanOrNull(): Boolean? = when (this) {
            VERIFIED -> true
            UNVERIFIED -> false
            UNKNOWN -> null
        }
    }

    /** What the items of a page share: the channel they come from and the date parser. */
    private class ItemContext(
        val verifiedStatus: VerifiedStatus,
        val channelName: String?,
        val channelUrl: String?,
        val timeAgoParser: TimeAgoParser,
    )

    private class YoutubeGridShowRendererChannelInfoItemExtractor(
        gridShowRenderer: JsonObject,
        private val context: ItemContext,
    ) : YoutubeBaseShowInfoItemExtractor(gridShowRenderer) {

        @get:Throws(ParsingException::class)
        override val uploaderName: String?
            get() = context.channelName

        @get:Throws(ParsingException::class)
        override val uploaderUrl: String?
            get() = context.channelUrl

        @get:Throws(ParsingException::class)
        override val isUploaderVerified: Boolean
            get() = context.verifiedStatus.toBooleanOrNull()
                ?: throw ParsingException("Could not get uploader verification status")
    }

    private companion object {
        /** Bound for shelves nested in shelves, which only ever go a few levels deep. */
        private const val MAX_NESTING_DEPTH = 8

        private val BROWSE_URL = YOUTUBEI_V1_URL + "browse?" + DISABLE_PRETTY_PRINT_PARAMETER

        private inline fun String?.ifNullOrEmpty(fallback: () -> String?): String? =
            if (isNullOrEmpty()) fallback() else this
    }
}

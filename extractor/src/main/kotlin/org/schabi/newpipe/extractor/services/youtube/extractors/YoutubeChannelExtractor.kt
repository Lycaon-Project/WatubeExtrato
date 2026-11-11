/*
 * Created by Christian Schabesberger on 25.07.16.
 *
 * Copyright (C) 2018 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * YoutubeChannelExtractor.kt is part of NewPipe Extractor.
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
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.channel.ChannelExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonArray
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.linkhandler.ReadyChannelTabListLinkHandler
import org.schabi.newpipe.extractor.linkhandler.ReadyChannelTabListLinkHandler.ChannelTabExtractorBuilder
import org.schabi.newpipe.extractor.services.youtube.YoutubeChannelHelper
import org.schabi.newpipe.extractor.services.youtube.YoutubeChannelHelper.ChannelHeader
import org.schabi.newpipe.extractor.services.youtube.YoutubeChannelHelper.ChannelHeader.HeaderType
import org.schabi.newpipe.extractor.services.youtube.YoutubeChannelHelper.getChannelResponse
import org.schabi.newpipe.extractor.services.youtube.YoutubeChannelHelper.resolveChannelId
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getImagesFromThumbnailsArray
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getTextFromObject
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeChannelLinkHandlerFactory
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeChannelTabLinkHandlerFactory
import org.schabi.newpipe.extractor.utils.Utils
import java.io.IOException

public open class YoutubeChannelExtractor(
    service: StreamingService,
    linkHandler: ListLinkHandler,
) : ChannelExtractor(service, linkHandler) {

    private var jsonResponse: JsonObject? = null
    private var channelHeader: ChannelHeader? = null
    private var channelId: String? = null

    /**
     * If a channel is age-restricted, its pages are only accessible to logged-in and
     * age-verified users, we get an `channelAgeGateRenderer` in this case, containing only
     * the following metadata: channel name and channel avatar.
     *
     * This restriction does not seem to apply to all countries.
     */
    private var channelAgeGateRenderer: JsonObject? = null

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        val id = resolveChannelId(super.id)
        // Fetch Videos tab
        val data = getChannelResponse(id, "EgZ2aWRlb3PyBgQKAjoA", extractorLocalization, extractorContentCountry)
        jsonResponse = data.jsonResponse
        channelHeader = YoutubeChannelHelper.getChannelHeader(data.jsonResponse)
        channelId = data.channelId
        channelAgeGateRenderer = YoutubeChannelHelper.getChannelAgeGateRenderer(data.jsonResponse)
    }

    private fun fetchedJsonResponse(): JsonObject {
        assertPageFetched()
        return checkNotNull(jsonResponse) { "Page is not fetched. Make sure you call fetchPage()" }
    }

    @get:Throws(ParsingException::class)
    override val url: String
        get() = try {
            YoutubeChannelLinkHandlerFactory.getInstance().getUrl("channel/$id")
        } catch (_: ParsingException) {
            super.url
        }

    @get:Throws(ParsingException::class)
    override val id: String
        get() = YoutubeChannelHelper.getChannelId(channelHeader, fetchedJsonResponse(), channelId)

    @get:Throws(ParsingException::class)
    override val name: String
        get() = YoutubeChannelHelper.getChannelName(channelHeader, channelAgeGateRenderer, fetchedJsonResponse())

    @get:Throws(ParsingException::class)
    override val avatars: List<Image>
        get() {
            fetchedJsonResponse()
            channelAgeGateRenderer?.let {
                return getImagesFromThumbnailsArray(it.getObject(AVATAR).getArray(THUMBNAILS))
            }
            val header = channelHeader ?: throw ParsingException("Could not get avatars")
            val avatarArray = when (header.headerType) {
                HeaderType.PAGE -> {
                    val imageObj = header.json.getObject(CONTENT)
                        .getObject(PAGE_HEADER_VIEW_MODEL)
                        .getObject(IMAGE)
                    when {
                        imageObj.has(CONTENT_PREVIEW_IMAGE_VIEW_MODEL) ->
                            imageObj.getObject(CONTENT_PREVIEW_IMAGE_VIEW_MODEL).getObject(IMAGE).getArray(SOURCES)
                        imageObj.has("decoratedAvatarViewModel") -> imageObj.getObject("decoratedAvatarViewModel")
                            .getObject(AVATAR)
                            .getObject("avatarViewModel")
                            .getObject(IMAGE)
                            .getArray(SOURCES)
                        // Return an empty avatar array as a fallback
                        else -> JsonArray()
                    }
                }
                HeaderType.INTERACTIVE_TABBED -> header.json.getObject("boxArt").getArray(THUMBNAILS)
                else -> header.json.getObject(AVATAR).getArray(THUMBNAILS)
            }
            return getImagesFromThumbnailsArray(avatarArray)
        }

    override val banners: List<Image>
        get() {
            fetchedJsonResponse()
            if (channelAgeGateRenderer != null) {
                return emptyList()
            }
            val header = channelHeader ?: return emptyList()
            if (header.headerType != HeaderType.PAGE) {
                return getImagesFromThumbnailsArray(header.json.getObject(BANNER).getArray(THUMBNAILS))
            }
            val pageHeaderViewModel = header.json.getObject(CONTENT).getObject(PAGE_HEADER_VIEW_MODEL)
            if (!pageHeaderViewModel.has(BANNER)) {
                // No banner is available (this should happen on pageHeaderRenderers of system
                // channels)
                return emptyList()
            }
            return getImagesFromThumbnailsArray(
                pageHeaderViewModel.getObject(BANNER)
                    .getObject("imageBannerViewModel")
                    .getObject(IMAGE)
                    .getArray(SOURCES),
            )
        }

    // RSS feeds are accessible for age-restricted channels, no need to check whether a channel has
    // a channelAgeGateRenderer
    @get:Throws(ParsingException::class)
    override val feedUrl: String?
        get() = try {
            YoutubeParsingHelper.getFeedUrlFrom(id)
        } catch (e: Exception) {
            throw ParsingException("Could not get feed URL", e)
        }

    @get:Throws(ParsingException::class)
    override val subscriberCount: Long
        get() {
            fetchedJsonResponse()
            if (channelAgeGateRenderer != null) {
                return UNKNOWN_SUBSCRIBER_COUNT
            }
            val header = channelHeader ?: return UNKNOWN_SUBSCRIBER_COUNT
            return when (header.headerType) {
                // No subscriber count is available on interactiveTabbedHeaderRenderer header
                HeaderType.INTERACTIVE_TABBED -> UNKNOWN_SUBSCRIBER_COUNT
                HeaderType.PAGE -> subscriberCountFromPageChannelHeader(header.json)
                else -> {
                    val headerJson = header.json
                    val textObject = when {
                        headerJson.has("subscriberCountText") -> headerJson.getObject("subscriberCountText")
                        headerJson.has("subtitle") -> headerJson.getObject("subtitle")
                        else -> return UNKNOWN_SUBSCRIBER_COUNT
                    }
                    subscriberCountFromText(getTextFromObject(textObject))
                }
            }
        }

    @Throws(ParsingException::class)
    private fun subscriberCountFromPageChannelHeader(headerJson: JsonObject): Long {
        val metadataObject = headerJson.getObject(CONTENT)
            .getObject(PAGE_HEADER_VIEW_MODEL)
            .getObject(METADATA)
        if (!metadataObject.has("contentMetadataViewModel")) {
            // If the channel header has no contentMetadataViewModel (which is the case for system
            // channels using this header), return UNKNOWN_SUBSCRIBER_COUNT
            return UNKNOWN_SUBSCRIBER_COUNT
        }
        val metadataRows = metadataObject.getObject("contentMetadataViewModel").getArray("metadataRows")
        // Extract the latest metadata part, it contains in general two elements: the subscribers
        // and videos counts
        val lastMetadataRowParts = metadataRows.getObject((metadataRows.size - 1).coerceAtLeast(0))
            .getArray("metadataParts")
        if (lastMetadataRowParts.size < 2) {
            // For autogenerated channels such as artist ones, subscribers count is not returned,
            // so the metadata part has a lower size
            return UNKNOWN_SUBSCRIBER_COUNT
        }
        // The subscriber count is at the same position for all languages as of 07/07/2025
        return subscriberCountFromText(lastMetadataRowParts.getObject(0).getObject("text").getString(CONTENT))
    }

    @Throws(ParsingException::class)
    private fun subscriberCountFromText(text: String?): Long {
        if (text == null) {
            throw ParsingException("Could not get subscriber count")
        }
        return try {
            Utils.mixedNumberWordToLong(text)
        } catch (e: NumberFormatException) {
            throw ParsingException("Could not get subscriber count", e)
        }
    }

    @get:Throws(ParsingException::class)
    override val description: String?
        get() {
            val response = fetchedJsonResponse()
            if (channelAgeGateRenderer != null) {
                return null
            }
            val header = channelHeader
            if (header != null && header.headerType == HeaderType.INTERACTIVE_TABBED) {
                // In an interactiveTabbedHeaderRenderer, the real description, is only available
                // in its header
                // The other one returned in non-About tabs accessible in the
                // microformatDataRenderer object of the response may be completely different
                // The description extracted is incomplete and the original one can be only
                // accessed from the About tab
                return getTextFromObject(header.json.getObject("description"))
            }
            return response.getObject(METADATA).getObject("channelMetadataRenderer").getString("description")
        }

    override val parentChannelName: String?
        get() = ""

    override val parentChannelUrl: String?
        get() = ""

    override val parentChannelAvatars: List<Image>
        get() = emptyList()

    @get:Throws(ParsingException::class)
    override val isVerified: Boolean
        get() {
            fetchedJsonResponse()
            if (channelAgeGateRenderer != null) {
                // Verified status is unknown with channelAgeGateRenderers, return false in this case
                return false
            }
            val header = channelHeader
                ?: throw ParsingException("Could not get channel verified status, no channel header has been extracted")
            return YoutubeChannelHelper.isChannelVerified(header)
        }

    @get:Throws(ParsingException::class)
    override val tags: List<String>
        get() {
            val response = fetchedJsonResponse()
            if (channelAgeGateRenderer != null) {
                return emptyList()
            }
            return response.getObject("microformat")
                .getObject("microformatDataRenderer")
                .getArray("tags")
                .filterIsInstance<String>()
        }

    @get:Throws(ParsingException::class)
    override val tabs: List<ListLinkHandler>
        get() {
            fetchedJsonResponse()
            return if (channelAgeGateRenderer == null) {
                tabsForNonAgeRestrictedChannels()
            } else {
                tabsForAgeRestrictedChannels()
            }
        }

    @Throws(ParsingException::class)
    private fun tabsForNonAgeRestrictedChannels(): List<ListLinkHandler> {
        val channelId = channelId ?: throw ParsingException("Could not get channel ID")
        val responseTabs = fetchedJsonResponse().getObject(CONTENTS)
            .getObject("twoColumnBrowseResultsRenderer")
            .getArray("tabs")

        val tabs = ArrayList<ListLinkHandler>()
        val addNonVideosTab = { tabName: String ->
            try {
                tabs.add(YoutubeChannelTabLinkHandlerFactory.getInstance().fromQuery(channelId, listOf(tabName), ""))
            } catch (_: ParsingException) {
                // Do not add the tab if we could not create the LinkHandler
            }
        }

        val name = name
        val url = url
        val id = id

        for (tabRenderer in responseTabs.filterIsInstance<JsonObject>().filter { it.has(TAB_RENDERER) }) {
            val renderer = tabRenderer.getObject(TAB_RENDERER)
            val tabUrl = renderer.getObject("endpoint")
                .getObject("commandMetadata")
                .getObject("webCommandMetadata")
                .getString("url")
                ?: continue
            when (tabUrl.trimEnd('/').substringAfterLast('/')) {
                // Since the Videos tab has already its contents fetched, make sure it is in the
                // first position
                // YoutubeChannelTabExtractor still supports fetching this tab
                "videos" -> tabs.add(
                    0,
                    ReadyChannelTabListLinkHandler(
                        // built from the ID: the URL sent by the server only names the tab
                        YoutubeChannelTabLinkHandlerFactory.getInstance()
                            .getUrl(channelId, listOf(ChannelTabs.VIDEOS), ""),
                        channelId,
                        ChannelTabs.VIDEOS,
                        videosTabBuilder(renderer, copyOf(channelHeader), name, id, url),
                    ),
                )
                "shorts" -> addNonVideosTab(ChannelTabs.SHORTS)
                "streams" -> addNonVideosTab(ChannelTabs.LIVESTREAMS)
                "releases" -> addNonVideosTab(ChannelTabs.ALBUMS)
                "podcasts" -> addNonVideosTab(ChannelTabs.PODCASTS)
                "courses" -> addNonVideosTab(ChannelTabs.COURSES)
                "playlists" -> addNonVideosTab(ChannelTabs.PLAYLISTS)
                // Unsupported channel tab, ignore it
                else -> Unit
            }
        }
        return tabs.toList()
    }

    @Throws(ParsingException::class)
    private fun tabsForAgeRestrictedChannels(): List<ListLinkHandler> {
        // As we do not have access to the channel tabs list, consider that the channel has videos,
        // shorts and livestreams, the data only accessible without login on YouTube's desktop
        // client using uploads system playlists
        // The playlists channel tab is still available on YouTube Music, but this is not
        // implemented in the extractor
        val channelId = channelId ?: throw ParsingException("Could not get channel ID")
        val channelUrl = url
        return listOf(ChannelTabs.VIDEOS, ChannelTabs.SHORTS, ChannelTabs.LIVESTREAMS).map { tabName ->
            ReadyChannelTabListLinkHandler("$channelUrl/$tabName", channelId, tabName, SYSTEM_PLAYLIST_TAB_BUILDER)
        }
    }

    private companion object {
        // Constants of objects used multiples from channel responses
        private const val IMAGE = "image"
        private const val CONTENTS = "contents"
        private const val CONTENT_PREVIEW_IMAGE_VIEW_MODEL = "contentPreviewImageViewModel"
        private const val PAGE_HEADER_VIEW_MODEL = "pageHeaderViewModel"
        private const val TAB_RENDERER = "tabRenderer"
        private const val CONTENT = "content"
        private const val METADATA = "metadata"
        private const val AVATAR = "avatar"
        private const val THUMBNAILS = "thumbnails"
        private const val SOURCES = "sources"
        private const val BANNER = "banner"

        /** Serializable, so that the link handlers holding it can be serialized. */
        private val SYSTEM_PLAYLIST_TAB_BUILDER: ChannelTabExtractorBuilder = object : ChannelTabExtractorBuilder {
            override fun build(service: StreamingService, linkHandler: ListLinkHandler): ChannelTabExtractor =
                YoutubeChannelTabPlaylistExtractor(service, linkHandler)
        }

        /**
         * Make a copy of the channel header, to avoid keeping a reference to the extractor
         * instance, which would prevent serialization of the [ReadyChannelTabListLinkHandler].
         */
        private fun copyOf(header: ChannelHeader?): ChannelHeader? =
            header?.let { ChannelHeader(it.json, it.headerType) }

        /**
         * Built in a static context, so that the builder only holds serializable values and never
         * the extractor instance.
         */
        private fun videosTabBuilder(
            tabRenderer: JsonObject,
            channelHeader: ChannelHeader?,
            name: String,
            id: String,
            url: String,
        ): ChannelTabExtractorBuilder = object : ChannelTabExtractorBuilder {
            override fun build(service: StreamingService, linkHandler: ListLinkHandler): ChannelTabExtractor =
                YoutubeChannelTabExtractor.VideosTabExtractor(
                    service,
                    linkHandler,
                    tabRenderer,
                    channelHeader,
                    name,
                    id,
                    url,
                )
        }
    }
}

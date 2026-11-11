/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.peertube.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonParser
import org.schabi.newpipe.extractor.json.JsonParserException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor
import org.schabi.newpipe.extractor.services.peertube.PeertubeLinkGuards
import org.schabi.newpipe.extractor.services.peertube.PeertubeParsingHelper
import org.schabi.newpipe.extractor.services.peertube.PeertubeParsingHelper.COUNT_KEY
import org.schabi.newpipe.extractor.services.peertube.PeertubeParsingHelper.ITEMS_PER_PAGE
import org.schabi.newpipe.extractor.services.peertube.PeertubeParsingHelper.START_KEY
import org.schabi.newpipe.extractor.stream.Description
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector
import java.io.IOException

public open class PeertubePlaylistExtractor(
    service: StreamingService,
    linkHandler: ListLinkHandler,
) : PlaylistExtractor(service, linkHandler) {

    private lateinit var playlistInfo: JsonObject

    /** The address of the instance, checked before any URL is built from it. */
    private val instanceBaseUrl: String
        @Throws(ParsingException::class)
        get() = PeertubeLinkGuards.requireBaseUrl(baseUrl)

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = PeertubeParsingHelper.getThumbnailsFromPlaylistOrVideoItem(instanceBaseUrl, playlistInfo)

    /** The uploader URL given by the instance, only when it is a web link. */
    @get:Throws(ParsingException::class)
    override val uploaderUrl: String
        get() = PeertubeLinkGuards.requireWebUrl(
            playlistInfo.getObject("ownerAccount").getString("url"),
            "uploader URL",
        )

    @get:Throws(ParsingException::class)
    override val uploaderName: String
        get() = playlistInfo.getObject("ownerAccount").getString("displayName")
            ?: throw ParsingException("Could not get uploader name")

    @get:Throws(ParsingException::class)
    override val uploaderAvatars: List<Image>
        get() = PeertubeParsingHelper.getAvatarsFromOwnerAccountOrVideoChannelObject(
            instanceBaseUrl,
            playlistInfo.getObject("ownerAccount"),
        )

    @get:Throws(ParsingException::class)
    override val isUploaderVerified: Boolean
        get() = false

    override val streamCount: Long
        get() = playlistInfo.getLong("videosLength")

    @get:Throws(ParsingException::class)
    override val description: Description
        get() = Description.of(playlistInfo.getString("description"), Description.Type.PLAIN_TEXT)

    override val subChannelName: String
        get() = playlistInfo.getObject("videoChannel").getString("displayName").orEmpty()

    /** The channel URL given by the instance, only when it is a web link; empty otherwise. */
    override val subChannelUrl: String
        get() = PeertubeLinkGuards.webUrlOrNull(playlistInfo.getObject("videoChannel").getString("url")).orEmpty()

    @get:Throws(ParsingException::class)
    override val subChannelAvatars: List<Image>
        get() = PeertubeParsingHelper.getAvatarsFromOwnerAccountOrVideoChannelObject(
            instanceBaseUrl,
            playlistInfo.getObject("videoChannel"),
        )

    @get:Throws(IOException::class, ExtractionException::class)
    override val initialPage: InfoItemsPage<StreamInfoItem>
        get() = getPage(Page("$url/videos?$START_KEY=0&$COUNT_KEY=$ITEMS_PER_PAGE"))

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<StreamInfoItem> {
        val pageUrl = page?.url
        require(!pageUrl.isNullOrEmpty()) { "Page doesn't contain an URL" }

        val json = PeertubeParsingHelper.fetchJsonPage(downloader, pageUrl, "playlist info")
        val total = json.getLong("total")
        val collector = StreamInfoItemsCollector(serviceId)
        PeertubeParsingHelper.collectItemsFrom(collector, json, instanceBaseUrl)
        return InfoItemsPage(collector, PeertubeParsingHelper.getNextPage(pageUrl, total))
    }

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        // the URL is only requested once its instance address has been checked
        instanceBaseUrl
        val response = downloader.get(url)
        playlistInfo = try {
            JsonParser.`object`().from(response.responseBody())
        } catch (jpe: JsonParserException) {
            throw ExtractionException("Could not parse json", jpe)
        }
        PeertubeParsingHelper.validate(playlistInfo)
    }

    @get:Throws(ParsingException::class)
    override val name: String
        get() = playlistInfo.getString("displayName") ?: throw ParsingException("Could not get playlist name")
}

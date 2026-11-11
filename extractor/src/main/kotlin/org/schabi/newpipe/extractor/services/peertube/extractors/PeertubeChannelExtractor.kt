/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.peertube.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.channel.ChannelExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonParser
import org.schabi.newpipe.extractor.json.JsonParserException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.services.peertube.PeertubeLinkGuards
import org.schabi.newpipe.extractor.services.peertube.PeertubeParsingHelper
import org.schabi.newpipe.extractor.services.peertube.linkHandler.PeertubeChannelLinkHandlerFactory
import org.schabi.newpipe.extractor.services.peertube.linkHandler.PeertubeChannelTabLinkHandlerFactory
import org.schabi.newpipe.extractor.utils.JsonUtils
import java.io.IOException

/**
 * @throws ParsingException if the link does not lead to a PeerTube instance reachable over https
 */
public open class PeertubeChannelExtractor @Throws(ParsingException::class) constructor(
    service: StreamingService,
    linkHandler: ListLinkHandler,
) : ChannelExtractor(service, linkHandler) {

    private lateinit var json: JsonObject
    private val instanceBaseUrl: String = PeertubeLinkGuards.requireBaseUrl(linkHandler.baseUrl)

    override val avatars: List<Image>
        get() = PeertubeParsingHelper.getAvatarsFromOwnerAccountOrVideoChannelObject(instanceBaseUrl, json)

    override val banners: List<Image>
        get() = PeertubeParsingHelper.getBannersFromAccountOrVideoChannelObject(instanceBaseUrl, json)

    /** The numeric channel ID is the only value from the instance placed in the URL. */
    @get:Throws(ParsingException::class)
    override val feedUrl: String?
        get() {
            val channelId = (json["id"] as? Number)?.toLong()
                ?: throw ParsingException("Could not get feed URL")
            return "$instanceBaseUrl/feeds/videos.xml?videoChannelId=$channelId"
        }

    override val subscriberCount: Long
        get() = json.getLong("followersCount")

    override val description: String?
        get() = json.getString("description")

    @get:Throws(ParsingException::class)
    override val parentChannelName: String?
        get() = JsonUtils.getString(json, "ownerAccount.name")

    /** The owner URL given by the instance, only when it is a web link. */
    @get:Throws(ParsingException::class)
    override val parentChannelUrl: String?
        get() = PeertubeLinkGuards.requireWebUrl(JsonUtils.getString(json, "ownerAccount.url"), "owner URL")

    override val parentChannelAvatars: List<Image>
        get() = PeertubeParsingHelper.getAvatarsFromOwnerAccountOrVideoChannelObject(
            instanceBaseUrl,
            json.getObject("ownerAccount"),
        )

    @get:Throws(ParsingException::class)
    override val isVerified: Boolean
        get() = false

    @get:Throws(ParsingException::class)
    override val tabs: List<ListLinkHandler>
        get() = listOf(
            PeertubeChannelTabLinkHandlerFactory.fromQuery(id, listOf(ChannelTabs.VIDEOS), "", instanceBaseUrl),
            PeertubeChannelTabLinkHandlerFactory.fromQuery(id, listOf(ChannelTabs.PLAYLISTS), "", instanceBaseUrl),
        )

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        val actorId = PeertubeLinkGuards.requireActorReference(id)
        val response = downloader.get(instanceBaseUrl + PeertubeChannelLinkHandlerFactory.API_ENDPOINT + actorId)
        json = try {
            JsonParser.`object`().from(response.responseBody())
        } catch (e: JsonParserException) {
            throw ExtractionException("Unable to extract PeerTube channel data", e)
        }
    }

    @get:Throws(ParsingException::class)
    override val name: String
        get() = JsonUtils.getString(json, "displayName")
}

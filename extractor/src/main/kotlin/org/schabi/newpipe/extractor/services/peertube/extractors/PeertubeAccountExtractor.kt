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
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
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
public open class PeertubeAccountExtractor @Throws(ParsingException::class) constructor(
    service: StreamingService,
    linkHandler: ListLinkHandler,
) : ChannelExtractor(service, linkHandler) {

    private lateinit var json: JsonObject
    private val instanceBaseUrl: String = PeertubeLinkGuards.requireBaseUrl(linkHandler.baseUrl)

    override val avatars: List<Image>
        get() = PeertubeParsingHelper.getAvatarsFromOwnerAccountOrVideoChannelObject(instanceBaseUrl, json)

    override val banners: List<Image>
        get() = PeertubeParsingHelper.getBannersFromAccountOrVideoChannelObject(instanceBaseUrl, json)

    /** The numeric account ID is the only value from the instance placed in the URL. */
    @get:Throws(ParsingException::class)
    override val feedUrl: String?
        get() {
            val accountId = (json["id"] as? Number)?.toLong()
                ?: throw ParsingException("Could not get feed URL")
            return "$instanceBaseUrl/feeds/videos.xml?accountId=$accountId"
        }

    /**
     * The subscriber count cannot be retrieved directly. It needs to be calculated.
     * An accounts subscriber count is the number of the channel owner's subscriptions
     * plus the sum of all sub channels subscriptions.
     *
     * The account's channels are requested on the first read only.
     */
    @get:Throws(ParsingException::class)
    override val subscriberCount: Long
        get() = cachedSubscriberCount ?: fetchSubscriberCount().also { cachedSubscriberCount = it }

    private var cachedSubscriberCount: Long? = null

    @Throws(ParsingException::class)
    private fun fetchSubscriberCount(): Long {
        var subscribersCount = json.getLong("followersCount")
        val actorId = PeertubeLinkGuards.requireActorReference(id)
        val accountId = if (actorId.contains(ACCOUNTS)) actorId else ACCOUNTS + actorId
        val accountVideoChannelUrl = instanceBaseUrl + PeertubeChannelLinkHandlerFactory.API_ENDPOINT +
            accountId + "/video-channels"
        try {
            val responseBody = downloader.get(accountVideoChannelUrl).responseBody()
            val videoChannels = JsonParser.`object`().from(responseBody).getArray("data")
            for (videoChannel in videoChannels) {
                if (videoChannel is JsonObject) {
                    subscribersCount += videoChannel.getInt("followersCount")
                }
            }
        } catch (_: IOException) {
            // something went wrong during video channels extraction,
            // only return subscribers of ownerAccount
        } catch (_: JsonParserException) {
            // same as above
        } catch (_: ReCaptchaException) {
            // same as above
        }
        return subscribersCount
    }

    override val description: String?
        get() = json.getString("description")

    override val parentChannelName: String?
        get() = ""

    override val parentChannelUrl: String?
        get() = ""

    override val parentChannelAvatars: List<Image>
        get() = emptyList()

    @get:Throws(ParsingException::class)
    override val isVerified: Boolean
        get() = false

    @get:Throws(ParsingException::class)
    override val tabs: List<ListLinkHandler>
        get() = listOf(
            PeertubeChannelTabLinkHandlerFactory.fromQuery(id, listOf(ChannelTabs.VIDEOS), "", instanceBaseUrl),
            PeertubeChannelTabLinkHandlerFactory.fromQuery(id, listOf(ChannelTabs.CHANNELS), "", instanceBaseUrl),
        )

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        val actorId = PeertubeLinkGuards.requireActorReference(id)
        val response = downloader.get(instanceBaseUrl + PeertubeChannelLinkHandlerFactory.API_ENDPOINT + actorId)
        json = try {
            JsonParser.`object`().from(response.responseBody())
        } catch (e: JsonParserException) {
            throw ExtractionException("Unable to extract PeerTube account data", e)
        }
    }

    @get:Throws(ParsingException::class)
    override val name: String
        get() = JsonUtils.getString(json, "displayName")

    private companion object {
        private const val ACCOUNTS = "accounts/"
    }
}

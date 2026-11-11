/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.media_ccc.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.channel.ChannelExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonParser
import org.schabi.newpipe.extractor.json.JsonParserException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.linkhandler.ReadyChannelTabListLinkHandler
import org.schabi.newpipe.extractor.services.media_ccc.MediaCCCLinkGuards
import org.schabi.newpipe.extractor.services.media_ccc.linkHandler.MediaCCCConferenceLinkHandlerFactory
import java.io.IOException

public open class MediaCCCConferenceExtractor(
    service: StreamingService,
    linkHandler: ListLinkHandler,
) : ChannelExtractor(service, linkHandler) {

    private lateinit var conferenceData: JsonObject

    override val avatars: List<Image>
        get() = MediaCCCParsingHelper.getImageListFromLogoImageUrl(conferenceData.getString("logo_url"))

    override val banners: List<Image>
        get() = emptyList()

    override val feedUrl: String?
        get() = null

    override val subscriberCount: Long
        get() = -1

    override val description: String?
        get() = null

    override val parentChannelName: String?
        get() = ""

    override val parentChannelUrl: String?
        get() = ""

    override val parentChannelAvatars: List<Image>
        get() = emptyList()

    override val isVerified: Boolean
        get() = false

    @get:Throws(ParsingException::class)
    override val tabs: List<ListLinkHandler>
        get() = listOf(
            ReadyChannelTabListLinkHandler(url, id, ChannelTabs.VIDEOS, ConferenceTabBuilder(conferenceData)),
        )

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        conferenceData = fetchConferenceData(downloader, id)
    }

    @get:Throws(ParsingException::class)
    override val name: String
        get() = conferenceData.getString("title") ?: throw ParsingException("Could not get conference name")

    /**
     * Builds the videos tab from the conference data already fetched; a class rather than a
     * lambda, so that no reference to the extractor is kept in the serializable link handler.
     */
    private class ConferenceTabBuilder(
        private val conferenceData: JsonObject,
    ) : ReadyChannelTabListLinkHandler.ChannelTabExtractorBuilder {

        override fun build(service: StreamingService, linkHandler: ListLinkHandler): ChannelTabExtractor =
            MediaCCCChannelTabExtractor(service, linkHandler, conferenceData)

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    internal companion object {
        /** The slug is checked before it is placed in the API URL. */
        @Throws(IOException::class, ExtractionException::class)
        internal fun fetchConferenceData(downloader: Downloader, conferenceId: String): JsonObject {
            val conferenceUrl = MediaCCCConferenceLinkHandlerFactory.CONFERENCE_API_ENDPOINT +
                MediaCCCLinkGuards.requireSlug(conferenceId)
            return try {
                JsonParser.`object`().from(downloader.get(conferenceUrl).responseBody())
            } catch (_: JsonParserException) {
                // neither the URL nor the answer is quoted
                throw ExtractionException("Could not parse json returned by the conference API")
            }
        }
    }
}

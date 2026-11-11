/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.peertube.extractors

import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.comments.CommentsExtractor
import org.schabi.newpipe.extractor.comments.CommentsInfoItem
import org.schabi.newpipe.extractor.comments.CommentsInfoItemsCollector
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonParser
import org.schabi.newpipe.extractor.json.JsonParserException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.services.peertube.PeertubeLinkGuards
import org.schabi.newpipe.extractor.services.peertube.PeertubeParsingHelper
import org.schabi.newpipe.extractor.services.peertube.PeertubeParsingHelper.COUNT_KEY
import org.schabi.newpipe.extractor.services.peertube.PeertubeParsingHelper.ITEMS_PER_PAGE
import org.schabi.newpipe.extractor.services.peertube.PeertubeParsingHelper.START_KEY
import java.io.IOException
import java.nio.charset.StandardCharsets

public open class PeertubeCommentsExtractor(
    service: StreamingService,
    uiHandler: ListLinkHandler,
) : CommentsExtractor(service, uiHandler) {

    /**
     * Use [isReply] to access this variable.
     */
    private var isReplyCache: Boolean? = null

    @get:Throws(IOException::class, ExtractionException::class)
    override val initialPage: InfoItemsPage<CommentsInfoItem>
        get() = if (isReply()) {
            getPage(Page(originalUrl))
        } else {
            getPage(Page("$url?$START_KEY=0&$COUNT_KEY=$ITEMS_PER_PAGE"))
        }

    @Throws(ParsingException::class)
    private fun isReply(): Boolean {
        isReplyCache?.let { return it }
        val reply = !originalUrl.contains("/videos/watch/") && originalUrl.contains("/comment-threads/")
        isReplyCache = reply
        return reply
    }

    @Throws(ParsingException::class)
    private fun collectCommentsFrom(collector: CommentsInfoItemsCollector, json: JsonObject) {
        val reply = isReply()
        val instanceBaseUrl = PeertubeLinkGuards.requireBaseUrl(baseUrl)
        for (c in json.getArray("data").take(MAX_COMMENTS)) {
            if (c is JsonObject && !c.getBoolean(IS_DELETED)) {
                collector.commit(PeertubeCommentsInfoItemExtractor(c, null, url, instanceBaseUrl, reply))
            }
        }
    }

    @Throws(ParsingException::class)
    private fun collectRepliesFrom(collector: CommentsInfoItemsCollector, json: JsonObject) {
        val reply = isReply()
        val instanceBaseUrl = PeertubeLinkGuards.requireBaseUrl(baseUrl)
        for (c in json.getArray(CHILDREN).take(MAX_COMMENTS)) {
            if (c !is JsonObject) {
                continue
            }
            val item = c.getObject("comment")
            val children = c.getArray(CHILDREN)
            if (!item.getBoolean(IS_DELETED)) {
                collector.commit(PeertubeCommentsInfoItemExtractor(item, children, url, instanceBaseUrl, reply))
            }
        }
    }

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<CommentsInfoItem> {
        val pageUrl = page?.url
        require(!pageUrl.isNullOrEmpty()) { "Page doesn't contain an URL" }

        val collector = CommentsInfoItemsCollector(serviceId)
        val body = page.body
        val total: Long = if (body == null) {
            val json = PeertubeParsingHelper.fetchJsonPage(downloader, pageUrl, "comments info")
            if (isReply() || json.has(CHILDREN)) {
                collectRepliesFrom(collector, json)
                json.getArray(CHILDREN).size.toLong()
            } else {
                collectCommentsFrom(collector, json)
                json.getLong(TOTAL)
            }
        } else {
            // nested replies, already fetched with their thread (see getReplies() of the items)
            val json = try {
                JsonParser.`object`().from(String(body, StandardCharsets.UTF_8))
            } catch (e: JsonParserException) {
                throw ParsingException("Could not parse json data for nested comments info", e)
            }
            isReplyCache = true
            collectRepliesFrom(collector, json)
            json.getArray(CHILDREN).size.toLong()
        }
        return InfoItemsPage(collector, PeertubeParsingHelper.getNextPage(pageUrl, total))
    }

    override fun onFetchPage(downloader: Downloader) {
        // nothing to fetch: the pages are requested by getPage()
    }

    internal companion object {
        internal const val CHILDREN = "children"
        private const val IS_DELETED = "isDeleted"

        /** Comments read from one answer of an instance, which chooses how many it sends. */
        private const val MAX_COMMENTS = 500
        private const val TOTAL = "total"
    }
}

/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.peertube.extractors

import org.jsoup.Jsoup
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.comments.CommentsInfoItemExtractor
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonArray
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonWriter
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.services.peertube.PeertubeParsingHelper
import org.schabi.newpipe.extractor.stream.Description
import org.schabi.newpipe.extractor.utils.JsonUtils
import java.nio.charset.StandardCharsets

public open class PeertubeCommentsInfoItemExtractor(
    private val item: JsonObject,
    private val children: JsonArray?,
    private val commentsUrl: String,
    private val baseUrl: String,
    private val isReply: Boolean,
) : CommentsInfoItemExtractor {

    private var replyCountCache: Int? = null

    @get:Throws(ParsingException::class)
    override val url: String
        get() = "$commentsUrl/$commentId"

    override val thumbnails: List<Image>
        get() = uploaderAvatars

    @get:Throws(ParsingException::class)
    override val name: String
        get() = JsonUtils.getString(item, "account.displayName")

    @get:Throws(ParsingException::class)
    override val textualUploadDate: String?
        get() = JsonUtils.getString(item, "createdAt")

    @get:Throws(ParsingException::class)
    override val uploadDate: DateWrapper?
        get() = DateWrapper.fromInstant(textualUploadDate)

    @get:Throws(ParsingException::class)
    override val commentText: Description
        get() {
            val htmlText = JsonUtils.getString(item, "text")
            val text = try {
                Jsoup.parse(htmlText).body().text()
            } catch (_: Exception) {
                // Jsoup failed: strip the tags with a regex instead
                HTML_TAGS.replace(htmlText, "")
            }
            return Description.of(text, Description.Type.PLAIN_TEXT)
        }

    /** The numeric ID of the comment, `0` when missing. */
    override val commentId: String
        get() = item.getLong("id").toString()

    override val uploaderAvatars: List<Image>
        get() = PeertubeParsingHelper.getAvatarsFromOwnerAccountOrVideoChannelObject(
            baseUrl,
            item.getObject("account"),
        )

    @get:Throws(ParsingException::class)
    override val uploaderName: String
        get() = JsonUtils.getString(item, "account.name") + "@" + JsonUtils.getString(item, "account.host")

    @get:Throws(ParsingException::class)
    override val uploaderUrl: String
        get() {
            val name = JsonUtils.getString(item, "account.name")
            val host = JsonUtils.getString(item, "account.host")
            return ServiceList.PeerTube.channelLHFactory.fromId("accounts/$name@$host", baseUrl).url
        }

    /**
     * The page of replies. Its URL only holds the numeric thread ID read from the instance, so
     * that it cannot add path segments or parameters to the request.
     */
    @get:Throws(ParsingException::class)
    override val replies: Page?
        get() {
            if (replyCount == 0) {
                return null
            }
            val threadId = JsonUtils.getNumber(item, "threadId").toLong().toString()
            val repliesUrl = "$commentsUrl/$threadId"
            val nestedReplies = children
            if (isReply && !nestedReplies.isNullOrEmpty()) {
                // Nested replies are already included in the original thread's request.
                // Wrap the replies into a JsonObject, because the original thread's request body
                // is also structured like a JsonObject.
                val pageContent = JsonObject()
                pageContent[PeertubeCommentsExtractor.CHILDREN] = nestedReplies
                return Page(repliesUrl, threadId, JsonWriter.string(pageContent).toByteArray(StandardCharsets.UTF_8))
            }
            return Page(repliesUrl, threadId)
        }

    @get:Throws(ParsingException::class)
    override val replyCount: Int
        get() {
            replyCountCache?.let { return it }
            val nestedReplies = children
            val count = if (!nestedReplies.isNullOrEmpty()) {
                // The totalReplies field is inaccurate for nested replies and sometimes returns 0
                // although there are replies to that reply stored in children.
                nestedReplies.size
            } else {
                JsonUtils.getNumber(item, "totalReplies").toInt()
            }
            replyCountCache = count
            return count
        }

    override fun hasCreatorReply(): Boolean =
        item.has("totalRepliesFromVideoAuthor") && item.getInt("totalRepliesFromVideoAuthor") > 0

    private companion object {
        /** Tags and the blanks between them; only used when Jsoup fails. */
        private val HTML_TAGS = Regex("(?s)<[^>]*>(\\s*<[^>]*>)*")
    }
}

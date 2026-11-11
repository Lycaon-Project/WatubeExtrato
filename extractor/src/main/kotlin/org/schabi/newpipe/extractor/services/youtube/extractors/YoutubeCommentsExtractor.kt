/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.extractors

import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.comments.CommentsExtractor
import org.schabi.newpipe.extractor.comments.CommentsInfoItem
import org.schabi.newpipe.extractor.comments.CommentsInfoItemsCollector
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonArray
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonWriter
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.localization.TimeAgoParser
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getJsonPostResponse
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getTextFromObject
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.prepareDesktopJsonBuilder
import org.schabi.newpipe.extractor.utils.JsonUtils
import org.schabi.newpipe.extractor.utils.Utils
import java.io.IOException

public open class YoutubeCommentsExtractor(
    service: StreamingService,
    uiHandler: ListLinkHandler,
) : CommentsExtractor(service, uiHandler) {

    /**
     * Whether comments are disabled on video.
     */
    private var commentsDisabled = false

    /**
     * The second ajax `next` response.
     */
    private var ajaxJson: JsonObject? = null

    @get:Throws(IOException::class, ExtractionException::class)
    override val initialPage: InfoItemsPage<CommentsInfoItem>
        get() {
            if (commentsDisabled) {
                return infoItemsPageForDisabledComments()
            }
            return extractComments(fetchedAjaxJson())
        }

    /**
     * Finds the initial comments token and initializes commentsDisabled.
     *
     * Also sets [commentsDisabled].
     *
     * @return the continuation token or null if none was found
     */
    private fun findInitialCommentsToken(nextResponse: JsonObject): String? {
        val contents = getJsonContents(nextResponse)

        // For videos where comments are unavailable, this would be null
        if (contents == null) {
            return null
        }

        val token = contents.filterIsInstance<JsonObject>()
            // Check if the comment-section is present
            .firstOrNull { jObj ->
                try {
                    "comments-section" == JsonUtils.getString(jObj, "itemSectionRenderer.targetId")
                } catch (_: ParsingException) {
                    false
                }
            }
            // Extract the token (or null in case of error)
            ?.let { itemSectionRenderer ->
                try {
                    JsonUtils.getString(
                        itemSectionRenderer.getObject("itemSectionRenderer")
                            .getArray("contents")
                            .getObject(0),
                        "continuationItemRenderer.continuationEndpoint.continuationCommand.token",
                    )
                } catch (_: ParsingException) {
                    null
                }
            }

        // The comments are disabled if we couldn't get a token
        commentsDisabled = token == null

        return token
    }

    private fun getJsonContents(nextResponse: JsonObject): JsonArray? = try {
        JsonUtils.getArray(nextResponse, "contents.twoColumnWatchNextResults.results.results.contents")
    } catch (_: ParsingException) {
        null
    }

    @Throws(ParsingException::class)
    private fun getMutationPayloadFromEntityKey(mutations: JsonArray, commentKey: String): JsonObject =
        mutations.filterIsInstance<JsonObject>()
            .firstOrNull { commentKey == it.getString("entityKey") }
            ?.getObject("payload")
            ?: throw ParsingException("Could not get comment entity payload mutation")

    private fun infoItemsPageForDisabledComments(): InfoItemsPage<CommentsInfoItem> =
        InfoItemsPage(emptyList(), null, emptyList())

    @Throws(ExtractionException::class)
    private fun getNextPage(jsonObject: JsonObject): Page? {
        val onResponseReceivedEndpoints = jsonObject.getArray("onResponseReceivedEndpoints")

        // Prevent ArrayIndexOutOfBoundsException
        if (onResponseReceivedEndpoints.isEmpty()) {
            return null
        }

        val endpoint = onResponseReceivedEndpoints.getObject(onResponseReceivedEndpoints.size - 1)
        val continuationItemsArray = endpoint
            .getObject("reloadContinuationItemsCommand", endpoint.getObject("appendContinuationItemsAction"))
            ?.getArray("continuationItems")
        // Prevent ArrayIndexOutOfBoundsException
        if (continuationItemsArray.isNullOrEmpty()) {
            return null
        }

        val continuationItemRenderer = continuationItemsArray
            .getObject(continuationItemsArray.size - 1)
            .getObject("continuationItemRenderer")

        val jsonPath = if (continuationItemRenderer.has("button")) {
            "button.buttonRenderer.command.continuationCommand.token"
        } else {
            "continuationEndpoint.continuationCommand.token"
        }

        val continuation = try {
            JsonUtils.getString(continuationItemRenderer, jsonPath)
        } catch (_: Exception) {
            return null
        }
        return getNextPage(continuation)
    }

    @Throws(ParsingException::class)
    private fun getNextPage(continuation: String): Page = Page(url, continuation) // URL is ignored tho

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<CommentsInfoItem> {
        if (commentsDisabled) {
            return infoItemsPageForDisabledComments()
        }

        val continuation = page?.id
        require(!continuation.isNullOrEmpty()) { "Page doesn't have the continuation." }

        val localization = extractorLocalization
        val body = JsonWriter.string(
            prepareDesktopJsonBuilder(localization, extractorContentCountry)
                .value("continuation", continuation)
                .done(),
        ).toByteArray(Charsets.UTF_8)

        return extractComments(getJsonPostResponse("next", body, localization))
    }

    @Throws(ExtractionException::class)
    private fun extractComments(jsonObject: JsonObject): InfoItemsPage<CommentsInfoItem> {
        val collector = CommentsInfoItemsCollector(serviceId)
        collectCommentsFrom(collector, jsonObject)
        return InfoItemsPage(collector, getNextPage(jsonObject))
    }

    @Throws(ParsingException::class)
    private fun collectCommentsFrom(collector: CommentsInfoItemsCollector, jsonObject: JsonObject) {
        val onResponseReceivedEndpoints = jsonObject.getArray("onResponseReceivedEndpoints")
        // Prevent ArrayIndexOutOfBoundsException
        if (onResponseReceivedEndpoints.isEmpty()) {
            return
        }
        val commentsEndpoint = onResponseReceivedEndpoints.getObject(onResponseReceivedEndpoints.size - 1)

        val path = when {
            commentsEndpoint.has("reloadContinuationItemsCommand") ->
                "reloadContinuationItemsCommand.continuationItems"
            commentsEndpoint.has("appendContinuationItemsAction") ->
                "appendContinuationItemsAction.continuationItems"
            // No comments
            else -> return
        }

        val contents = try {
            JsonUtils.getArray(commentsEndpoint, path)
        } catch (_: Exception) {
            // No comments
            return
        }

        // The continuation item is skipped instead of being removed: the original array is still
        // used to get the continuation, and no copy of it is needed
        val lastIndex = contents.size - 1
        val skipLast = lastIndex >= 0 && contents.getObject(lastIndex).has("continuationItemRenderer")
        val itemCount = if (skipLast) lastIndex else contents.size

        // The mutations object, which is returned in the comments continuation
        // It contains parts of comment data when comments are returned with a view model
        val mutations = jsonObject.getObject("frameworkUpdates")
            .getObject("entityBatchUpdate")
            .getArray("mutations")
        val videoUrl = url
        val timeAgoParser = timeAgoParser

        for (index in 0 until itemCount) {
            val content = contents[index] as? JsonObject ?: continue
            // one comment that cannot be read is skipped, not the whole page
            try {
                collectCommentItem(mutations, content, collector, videoUrl, timeAgoParser)
            } catch (e: ParsingException) {
                collector.addSkippedItemError(e)
            }
        }
    }

    @Throws(ParsingException::class)
    private fun collectCommentItem(
        mutations: JsonArray,
        content: JsonObject,
        collector: CommentsInfoItemsCollector,
        videoUrl: String,
        timeAgoParser: TimeAgoParser,
    ) {
        // never parse advertising renderers as comments
        if (YoutubeItemGuards.isAd(content)) {
            return
        }
        if (content.has("commentThreadRenderer")) {
            val commentThreadRenderer = content.getObject("commentThreadRenderer")
            val repliesRenderer = commentThreadRenderer.getObject("replies")
                .getObject("commentRepliesRenderer")
            if (commentThreadRenderer.has(COMMENT_VIEW_MODEL_KEY)) {
                val commentViewModel = commentThreadRenderer.getObject(COMMENT_VIEW_MODEL_KEY)
                    .getObject(COMMENT_VIEW_MODEL_KEY)
                collector.commit(
                    viewModelExtractor(commentViewModel, repliesRenderer, mutations, videoUrl, timeAgoParser),
                )
            } else if (commentThreadRenderer.has("comment")) {
                collector.commit(
                    YoutubeCommentsInfoItemExtractor(
                        commentThreadRenderer.getObject("comment").getObject(COMMENT_RENDERER_KEY),
                        repliesRenderer,
                        videoUrl,
                        timeAgoParser,
                    ),
                )
            }
        } else if (content.has(COMMENT_VIEW_MODEL_KEY)) {
            collector.commit(
                viewModelExtractor(
                    content.getObject(COMMENT_VIEW_MODEL_KEY),
                    null,
                    mutations,
                    videoUrl,
                    timeAgoParser,
                ),
            )
        } else if (content.has(COMMENT_RENDERER_KEY)) {
            // commentRenderers are directly returned for comment replies, so there is no
            // commentRepliesRenderer to provide
            // Also, YouTube has only one comment reply level
            collector.commit(
                YoutubeCommentsInfoItemExtractor(
                    content.getObject(COMMENT_RENDERER_KEY),
                    null,
                    videoUrl,
                    timeAgoParser,
                ),
            )
        }
    }

    @Throws(ParsingException::class)
    private fun viewModelExtractor(
        commentViewModel: JsonObject,
        commentRepliesRenderer: JsonObject?,
        mutations: JsonArray,
        videoUrl: String,
        timeAgoParser: TimeAgoParser,
    ): YoutubeCommentsEUVMInfoItemExtractor = YoutubeCommentsEUVMInfoItemExtractor(
        commentViewModel,
        commentRepliesRenderer,
        getMutationPayloadFromEntityKey(mutations, commentViewModel.getString("commentKey", "").orEmpty())
            .getObject("commentEntityPayload"),
        getMutationPayloadFromEntityKey(mutations, commentViewModel.getString("toolbarStateKey", "").orEmpty())
            .getObject("engagementToolbarStateEntityPayload"),
        videoUrl,
        timeAgoParser,
    )

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        val localization = extractorLocalization
        val body = JsonWriter.string(
            prepareDesktopJsonBuilder(localization, extractorContentCountry)
                .value("videoId", id)
                .done(),
        ).toByteArray(Charsets.UTF_8)

        val initialToken = findInitialCommentsToken(getJsonPostResponse("next", body, localization))
            ?: return

        val ajaxBody = JsonWriter.string(
            prepareDesktopJsonBuilder(localization, extractorContentCountry)
                .value("continuation", initialToken)
                .done(),
        ).toByteArray(Charsets.UTF_8)

        ajaxJson = getJsonPostResponse("next", ajaxBody, localization)
    }

    @get:Throws(ExtractionException::class)
    override val isCommentsDisabled: Boolean
        get() = commentsDisabled

    @get:Throws(ExtractionException::class)
    override val commentsCount: Int
        get() {
            assertPageFetched()

            if (commentsDisabled) {
                return -1
            }

            val countText = fetchedAjaxJson().getArray("onResponseReceivedEndpoints")
                .getObject(0)
                .getObject("reloadContinuationItemsCommand")
                .getArray("continuationItems")
                .getObject(0)
                .getObject("commentsHeaderRenderer")
                .getObject("countText")

            return getTextFromObject(countText)
                ?.let { Utils.removeNonDigitCharacters(it).toIntOrNull() }
                ?: throw ExtractionException("Unable to get comments count")
        }

    private fun fetchedAjaxJson(): JsonObject =
        checkNotNull(ajaxJson) { "Page is not fetched. Make sure you call fetchPage()" }

    private companion object {
        private const val COMMENT_VIEW_MODEL_KEY = "commentViewModel"
        private const val COMMENT_RENDERER_KEY = "commentRenderer"
    }
}

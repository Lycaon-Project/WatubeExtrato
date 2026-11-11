package org.schabi.newpipe.extractor.services.youtube

import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonWriter
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.BADGES
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.defaultAlertsCheck
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getJsonPostResponse
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getTextFromObject
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.hasArtistOrVerifiedIconBadgeAttachment
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.prepareDesktopJsonBuilder
import java.io.IOException
import java.io.Serializable
import java.nio.charset.StandardCharsets

/**
 * Shared functions for extracting YouTube channel pages and tabs.
 */
public object YoutubeChannelHelper {

    private const val BROWSE_ENDPOINT = "browseEndpoint"

    /** A channel ID: "UC" and 22 characters. A name that merely starts with UC is resolved. */
    private val UC_CHANNEL_ID = Regex("UC[A-Za-z0-9_-]{22}")
    private const val BROWSE_ID = "browseId"
    private const val CAROUSEL_HEADER_RENDERER = "carouselHeaderRenderer"
    private const val C4_TABBED_HEADER_RENDERER = "c4TabbedHeaderRenderer"
    private const val CONTENT = "content"
    private const val CONTENTS = "contents"
    private const val HEADER = "header"
    private const val PAGE_HEADER_VIEW_MODEL = "pageHeaderViewModel"
    private const val TAB_RENDERER = "tabRenderer"
    private const val TITLE = "title"
    private const val TOPIC_CHANNEL_DETAILS_RENDERER = "topicChannelDetailsRenderer"
    private const val MAX_REDIRECTS = 3
    private const val HTTP_NOT_FOUND = 404

    /**
     * A channel ID given by YouTube: it ends up in URLs and requests, so it must only be made of
     * URL-safe characters (bounded).
     */
    private val CHANNEL_ID_PATTERN = Regex("[A-Za-z0-9_-]{1,128}")

    private fun isPageTypeOfChannel(webPageType: String?): Boolean =
        "WEB_PAGE_TYPE_BROWSE".equals(webPageType, ignoreCase = true) ||
            "WEB_PAGE_TYPE_CHANNEL".equals(webPageType, ignoreCase = true)

    /** [browseId] of a redirection, checked to be a channel ID which can be placed in a URL. */
    @Throws(ExtractionException::class)
    private fun requireRedirectedChannelId(browseId: String): String {
        if (!browseId.startsWith("UC") || !CHANNEL_ID_PATTERN.matches(browseId)) {
            throw ExtractionException("Redirected id is not pointing to a channel")
        }
        return browseId
    }

    /**
     * Take a YouTube channel ID or URL path, resolve it if necessary and return a channel ID.
     *
     * @param idOrPath a YouTube channel ID or URL path
     * @return a YouTube channel ID
     * @throws IOException if a channel resolve request failed
     * @throws ExtractionException if a channel resolve request response could not be parsed or is
     * invalid
     */
    @JvmStatic
    @Throws(ExtractionException::class, IOException::class)
    public fun resolveChannelId(idOrPath: String): String {
        // trailing empty parts are dropped, as String.split does in Java
        val channelId = idOrPath.split('/').dropLastWhile { it.isEmpty() }.ifEmpty { listOf("") }

        if (UC_CHANNEL_ID.matches(channelId[0])) {
            return channelId[0]
        }

        // If the URL is not a /channel URL, we need to use the navigation/resolve_url endpoint of
        // the InnerTube API to get the channel id. If this fails or if the URL is not a /channel
        // URL, then no information about the channel associated with this URL was found,
        // so the unresolved url will be returned.
        if (channelId[0] != "channel") {
            var urlToResolve: String? = "https://www.youtube.com/$idOrPath"

            var endpoint = JsonObject()
            var webPageType: String? = ""
            // Try to resolve YT channel redirects
            // It works like that:
            // @TheDailyShow
            // -> resolves to thedailyshow
            // -> resolves to the id: UCwWhs_6x42TyRM4Wstoq8HA
            // Please note that this is not always the case, some handles
            // e.g. @google or @Gronkh directly resolve the id
            var tries = 0
            while (urlToResolve != null && tries < MAX_REDIRECTS) {
                val body = JsonWriter.string(
                    prepareDesktopJsonBuilder(Localization.DEFAULT, ContentCountry.DEFAULT)
                        .value("url", urlToResolve)
                        .done(),
                ).toByteArray(StandardCharsets.UTF_8)

                val jsonResponse = getJsonPostResponse("navigation/resolve_url", body, Localization.DEFAULT)

                checkIfChannelResponseIsValid(jsonResponse)

                endpoint = jsonResponse.getObject("endpoint")

                webPageType = endpoint.getObject("commandMetadata")
                    .getObject("webCommandMetadata")
                    .getString("webPageType")

                urlToResolve = if ("WEB_PAGE_TYPE_UNKNOWN" == webPageType) {
                    endpoint.getObject("urlEndpoint").getString("url")
                } else {
                    null
                }
                tries++
            }

            val browseId = endpoint.getObject(BROWSE_ENDPOINT).getString(BROWSE_ID, "").orEmpty()

            if (isPageTypeOfChannel(webPageType) && browseId.isNotEmpty()) {
                return requireRedirectedChannelId(browseId)
            }

            // Otherwise, the code after that will run into an IndexOutOfBoundsException
            if (channelId.size < 2) {
                // the handle or path is not quoted: it tells which channel the user looks for
                throw ExtractionException("Failed to resolve channelId")
            }
        }

        // return the unresolved URL
        return channelId[1]
    }

    /**
     * Response data object for [getChannelResponse], after any redirection in the allowed
     * redirects count (`3`).
     */
    public class ChannelResponseData internal constructor(
        /**
         * The channel response as a JSON object, after all redirects.
         */
        @JvmField public val jsonResponse: JsonObject,
        /**
         * The channel ID after all redirects.
         */
        @JvmField public val channelId: String,
    )

    /**
     * Fetch a YouTube channel tab response, using the given channel ID and tab parameters.
     *
     * Redirections to other channels are supported to up to 3 redirects, which could happen for
     * instance for localized channels or for auto-generated ones. For instance, there are three IDs
     * of the auto-generated "Movies and Shows" channel, i.e. `UCuJcl0Ju-gPDoksRjK1ya-w`,
     * `UChBfWrfBXL9wS6tQtgjt_OQ` and `UCok7UTQQEP1Rsctxiv3gwSQ`, and they all redirect
     * to the `UClgRkhTL3_hImCAmdLfDE4g` one.
     *
     * @param channelId    a valid YouTube channel ID
     * @param parameters   the parameters to specify the YouTube channel tab; if invalid ones are
     *                     specified, YouTube should return the `Home` tab
     * @param localization the [Localization] to use
     * @param country      the [ContentCountry] to use
     * @return a [channel response data][ChannelResponseData]
     * @throws IOException if a channel request failed
     * @throws ExtractionException if a channel request response could not be parsed or is invalid
     */
    @JvmStatic
    @Throws(ExtractionException::class, IOException::class)
    public fun getChannelResponse(
        channelId: String,
        parameters: String,
        localization: Localization,
        country: ContentCountry,
    ): ChannelResponseData {
        var id = channelId
        var ajaxJson: JsonObject? = null

        var level = 0
        while (level < MAX_REDIRECTS) {
            val body = JsonWriter.string(
                prepareDesktopJsonBuilder(localization, country)
                    .value(BROWSE_ID, id)
                    .value("params", parameters)
                    .done(),
            ).toByteArray(StandardCharsets.UTF_8)

            val jsonResponse = getJsonPostResponse("browse", body, localization)

            checkIfChannelResponseIsValid(jsonResponse)

            val endpoint = jsonResponse.getArray("onResponseReceivedActions")
                .getObject(0)
                .getObject("navigateAction")
                .getObject("endpoint")

            val webPageType = endpoint.getObject("commandMetadata")
                .getObject("webCommandMetadata")
                .getString("webPageType")

            val browseId = endpoint.getObject(BROWSE_ENDPOINT).getString(BROWSE_ID, "").orEmpty()

            if (isPageTypeOfChannel(webPageType) && browseId.isNotEmpty()) {
                id = requireRedirectedChannelId(browseId)
                level++
            } else {
                ajaxJson = jsonResponse
                break
            }
        }

        if (ajaxJson == null) {
            throw ExtractionException("Got no channel response after 3 redirects")
        }

        defaultAlertsCheck(ajaxJson)

        return ChannelResponseData(ajaxJson, id)
    }

    /**
     * Assert that a channel JSON response does not contain an `error` JSON object.
     *
     * @param jsonResponse a channel JSON response
     * @throws ContentNotAvailableException if the channel was not found
     */
    @Throws(ContentNotAvailableException::class)
    private fun checkIfChannelResponseIsValid(jsonResponse: JsonObject) {
        val errorJsonObject = jsonResponse.getObject("error")
        if (errorJsonObject.isEmpty()) {
            return
        }
        if (errorJsonObject.getInt("code") == HTTP_NOT_FOUND) {
            throw ContentNotAvailableException("This channel doesn't exist.")
        }
        throw ContentNotAvailableException(
            "Got error:\"" + errorJsonObject.getString("status") + "\": " +
                errorJsonObject.getString("message"),
        )
    }

    /**
     * A channel header response.
     *
     * This class allows the distinction between a classic header and a carousel one, used for
     * auto-generated ones like the gaming or music topic channels and for big events such as the
     * Coachella music festival, which have a different data structure and do not return the same
     * properties.
     */
    public class ChannelHeader(
        /**
         * The channel header JSON response.
         */
        @JvmField public val json: JsonObject,
        /**
         * The type of the channel header.
         *
         * See the documentation of the [HeaderType] class for more details.
         */
        @JvmField public val headerType: HeaderType,
    ) : Serializable {

        /**
         * Types of supported YouTube channel headers.
         */
        public enum class HeaderType {

            /**
             * A `c4TabbedHeaderRenderer` channel header type.
             *
             * This header is returned on the majority of channels and contains the channel's name,
             * its banner and its avatar and its subscriber count in most cases.
             */
            C4_TABBED,

            /**
             * An `interactiveTabbedHeaderRenderer` channel header type.
             *
             * This header is returned for gaming topic channels, and only contains the channel's
             * name, its banner and a poster as its "avatar".
             */
            INTERACTIVE_TABBED,

            /**
             * A `carouselHeaderRenderer` channel header type.
             *
             * This header returns only the channel's name, its avatar and its subscriber count.
             */
            CAROUSEL,

            /**
             * A `pageHeaderRenderer` channel header type.
             *
             * This header returns only the channel's name and its avatar for system channels.
             */
            PAGE,
        }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** The first object of [contents] holding [key], if any. */
    private fun firstWith(contents: Iterable<Any?>, key: String): JsonObject? =
        contents.firstOrNull { it is JsonObject && it.has(key) } as JsonObject?

    /**
     * Get a channel header it if exists.
     *
     * @param channelResponse a full channel JSON response
     * @return a [ChannelHeader] or `null` if no supported header has been found
     */
    @JvmStatic
    public fun getChannelHeader(channelResponse: JsonObject): ChannelHeader? {
        val header = channelResponse.getObject(HEADER)

        return when {
            header.has(C4_TABBED_HEADER_RENDERER) -> ChannelHeader(
                header.getObject(C4_TABBED_HEADER_RENDERER), ChannelHeader.HeaderType.C4_TABBED,
            )

            header.has(CAROUSEL_HEADER_RENDERER) ->
                firstWith(header.getObject(CAROUSEL_HEADER_RENDERER).getArray(CONTENTS), TOPIC_CHANNEL_DETAILS_RENDERER)
                    ?.let {
                        ChannelHeader(
                            it.getObject(TOPIC_CHANNEL_DETAILS_RENDERER),
                            ChannelHeader.HeaderType.CAROUSEL,
                        )
                    }

            header.has("pageHeaderRenderer") -> ChannelHeader(
                header.getObject("pageHeaderRenderer"), ChannelHeader.HeaderType.PAGE,
            )

            header.has("interactiveTabbedHeaderRenderer") -> ChannelHeader(
                header.getObject("interactiveTabbedHeaderRenderer"),
                ChannelHeader.HeaderType.INTERACTIVE_TABBED,
            )

            else -> null
        }
    }

    /**
     * Check if a channel is verified by using its header.
     *
     * The header is mandatory, so the verified status of age-restricted channels with a
     * `channelAgeGateRenderer` cannot be checked.
     *
     * @param channelHeader the [ChannelHeader] of a non age-restricted channel
     * @return whether the channel is verified
     */
    @JvmStatic
    public fun isChannelVerified(channelHeader: ChannelHeader): Boolean =
        when (channelHeader.headerType) {
            // carouselHeaderRenderers do not contain any verification badges
            // Since they are only shown on YouTube internal channels or on channels of large
            // organizations broadcasting live events, we can assume the channel to be verified
            ChannelHeader.HeaderType.CAROUSEL -> true

            ChannelHeader.HeaderType.PAGE -> {
                val pageHeaderViewModel = channelHeader.json.getObject(CONTENT)
                    .getObject(PAGE_HEADER_VIEW_MODEL)

                val hasCircleOrMusicIcon = hasArtistOrVerifiedIconBadgeAttachment(
                    pageHeaderViewModel.getObject(TITLE)
                        .getObject("dynamicTextViewModel")
                        .getObject("text")
                        .getArray("attachmentRuns"),
                )
                // If a pageHeaderRenderer has no object in which a check verified may be
                // contained and if it has a contentPreviewImageViewModel, it should mean
                // that the header is coming from a system channel, which we can assume to
                // be verified
                hasCircleOrMusicIcon ||
                    pageHeaderViewModel.getObject("image").has("contentPreviewImageViewModel")
            }

            // If the header has an autoGenerated property, it should mean that the channel has
            // been auto generated by YouTube: we can assume the channel to be verified in this
            // case
            ChannelHeader.HeaderType.INTERACTIVE_TABBED -> channelHeader.json.has("autoGenerated")

            else -> YoutubeParsingHelper.isVerified(channelHeader.json.getArray(BADGES))
        }

    /** [id] when it is a channel ID which can be placed in a URL, `null` otherwise. */
    private fun validChannelIdOrNull(id: String?): String? =
        id?.takeIf { CHANNEL_ID_PATTERN.matches(it) }

    /**
     * Get the ID of a channel from its response.
     *
     * For [c4TabbedHeaderRenderer][ChannelHeader.HeaderType.C4_TABBED] and
     * [carouselHeaderRenderer][ChannelHeader.HeaderType.CAROUSEL] channel headers, the ID is
     * get from the header.
     *
     * For other headers or if it cannot be got, the ID from the `channelMetadataRenderer`
     * in the channel response is used.
     *
     * If the ID cannot still be get, the fallback channel ID, if provided, will be used.
     *
     * An ID given by YouTube which is not made of URL-safe characters is skipped, like a missing
     * one.
     *
     * @param channelHeader     the channel header
     * @param fallbackChannelId the fallback channel ID, which can be null
     * @return the ID of the channel
     * @throws ParsingException if the channel ID cannot be got from the channel header, the
     * channel response and the fallback channel ID
     */
    @JvmStatic
    @Throws(ParsingException::class)
    public fun getChannelId(
        channelHeader: ChannelHeader?,
        jsonResponse: JsonObject,
        fallbackChannelId: String?,
    ): String {
        if (channelHeader != null) {
            getChannelIdFromHeader(channelHeader)?.let { return it }
        }

        val externalChannelId = jsonResponse.getObject("metadata")
            .getObject("channelMetadataRenderer")
            .getString("externalChannelId")
        validChannelIdOrNull(externalChannelId)?.let { return it }

        if (!fallbackChannelId.isNullOrEmpty()) {
            return fallbackChannelId
        }
        throw ParsingException("Could not get channel ID")
    }

    private fun getChannelIdFromHeader(channelHeader: ChannelHeader): String? =
        when (channelHeader.headerType) {
            ChannelHeader.HeaderType.C4_TABBED -> {
                val channelId = channelHeader.json.getObject(HEADER)
                    .getObject(C4_TABBED_HEADER_RENDERER)
                    .getString("channelId", "")
                validChannelIdOrNull(channelId) ?: validChannelIdOrNull(
                    channelHeader.json
                        .getObject("navigationEndpoint")
                        .getObject(BROWSE_ENDPOINT)
                        .getString(BROWSE_ID),
                )
            }

            ChannelHeader.HeaderType.CAROUSEL -> validChannelIdOrNull(
                (
                    firstWith(
                        channelHeader.json.getObject(HEADER)
                            .getObject(CAROUSEL_HEADER_RENDERER)
                            .getArray(CONTENTS),
                        TOPIC_CHANNEL_DETAILS_RENDERER,
                    ) ?: JsonObject()
                    )
                    .getObject(TOPIC_CHANNEL_DETAILS_RENDERER)
                    .getObject("navigationEndpoint")
                    .getObject(BROWSE_ENDPOINT)
                    .getString(BROWSE_ID),
            )

            else -> null
        }

    @JvmStatic
    @Throws(ParsingException::class)
    public fun getChannelName(
        channelHeader: ChannelHeader?,
        channelAgeGateRenderer: JsonObject?,
        jsonResponse: JsonObject,
    ): String {
        if (channelAgeGateRenderer != null) {
            val title = channelAgeGateRenderer.getString("channelTitle")
            if (title.isNullOrEmpty()) {
                throw ParsingException("Could not get channel name")
            }
            return title
        }

        val metadataRendererTitle = jsonResponse.getObject("metadata")
            .getObject("channelMetadataRenderer")
            .getString(TITLE)
        if (!metadataRendererTitle.isNullOrEmpty()) {
            return metadataRendererTitle
        }

        return channelHeader?.let { getChannelNameFromHeader(it) }
            // The channel name from a microformatDataRenderer may be different from the one
            // displayed, especially for auto-generated channels, depending on the language
            // requested for the interface (hl parameter of InnerTube requests' payload)
            ?: jsonResponse.getObject("microformat")
                .getObject("microformatDataRenderer")
                .getString(TITLE)
            ?: throw ParsingException("Could not get channel name")
    }

    private fun getChannelNameFromHeader(header: ChannelHeader): String? {
        val channelJson = header.json
        return when (header.headerType) {
            ChannelHeader.HeaderType.PAGE -> channelJson.getObject(CONTENT)
                .getObject(PAGE_HEADER_VIEW_MODEL)
                .getObject(TITLE)
                .getObject("dynamicTextViewModel")
                .getObject("text")
                .getString(CONTENT, channelJson.getString("pageTitle"))

            ChannelHeader.HeaderType.CAROUSEL,
            ChannelHeader.HeaderType.INTERACTIVE_TABBED,
            -> getTextFromObject(channelJson.getObject(TITLE))

            ChannelHeader.HeaderType.C4_TABBED -> channelJson.getString(TITLE)
        }
    }

    @JvmStatic
    public fun getChannelAgeGateRenderer(jsonResponse: JsonObject): JsonObject? {
        for (tab in jsonResponse.getObject(CONTENTS).getObject("twoColumnBrowseResultsRenderer").getArray("tabs")) {
            val contents = (tab as? JsonObject ?: continue).getObject(TAB_RENDERER)
                .getObject(CONTENT)
                .getObject("sectionListRenderer")
                .getArray(CONTENTS)
            firstWith(contents, "channelAgeGateRenderer")?.let {
                return it.getObject("channelAgeGateRenderer")
            }
        }
        return null
    }
}

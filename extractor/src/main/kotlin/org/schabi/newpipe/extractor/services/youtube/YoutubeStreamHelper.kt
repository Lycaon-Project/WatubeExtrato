package org.schabi.newpipe.extractor.services.youtube

import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonBuilder
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonWriter
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.CONTENT_CHECK_OK
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.CPN
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.RACY_CHECK_OK
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.VIDEO_ID
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.YOUTUBEI_V1_GAPIS_URL
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.YOUTUBEI_V1_URL
import org.schabi.newpipe.extractor.utils.JsonUtils
import java.io.IOException
import java.nio.charset.StandardCharsets

public object YoutubeStreamHelper {

    private const val PLAYER = "player"

    /**
     * The video ID goes into the player request URL: URL-safe characters only, bounded (the
     * length is not pinned to 11, so that a future ID format does not break the app).
     */
    private val VIDEO_ID_PATTERN = Regex("[a-zA-Z0-9_-]{1,64}")

    /** What [YoutubeParsingHelper.generateContentPlaybackNonce] gives. */
    private val CPN_PATTERN = Regex("[a-zA-Z0-9_-]{1,64}")

    @JvmStatic
    @Throws(IOException::class, ExtractionException::class)
    public fun getWebMetadataPlayerResponse(
        localization: Localization,
        contentCountry: ContentCountry,
        videoId: String,
    ): JsonObject = getWebMetadataPlayerResponse(localization, contentCountry, videoId, null)

    /**
     * The WEB player response, for metadata only. [visitorData] is the one of the other player
     * request of the same video ([getVisionOsVisitorData]), or `null` to ask YouTube for one.
     */
    @JvmStatic
    @Throws(IOException::class, ExtractionException::class)
    public fun getWebMetadataPlayerResponse(
        localization: Localization,
        contentCountry: ContentCountry,
        videoId: String,
        visitorData: String?,
    ): JsonObject {
        requireVideoId(videoId)
        val innertubeClientRequestInfo = InnertubeClientRequestInfo.ofWebClient()
        innertubeClientRequestInfo.clientInfo.clientVersion = YoutubeParsingHelper.getClientVersion()

        val headers = YoutubeParsingHelper.getYouTubeHeaders()

        // We must always pass a valid visitorData to get valid player responses, which needs to be
        // got from YouTube
        innertubeClientRequestInfo.clientInfo.visitorData = visitorData
            ?: YoutubeParsingHelper.getVisitorDataFromInnertube(
                innertubeClientRequestInfo, localization, contentCountry, headers, YOUTUBEI_V1_URL,
                null, false,
            )

        val builder = YoutubeParsingHelper.prepareJsonBuilder(
            localization, contentCountry, innertubeClientRequestInfo, null,
        )

        addVideoIdCpnAndOkChecks(builder, videoId, null)

        val body = JsonWriter.string(builder.done()).toByteArray(StandardCharsets.UTF_8)

        val url = YOUTUBEI_V1_URL + PLAYER + "?" + DISABLE_PRETTY_PRINT_PARAMETER +
            "&\$fields=microformat,videoDetails.thumbnail.thumbnails,videoDetails.videoId"

        return JsonUtils.toJsonObject(
            YoutubeParsingHelper.getValidJsonResponseBody(
                downloader().postWithContentTypeJson(url, headers, body, localization),
            ),
        )
    }

    @JvmStatic
    @Throws(IOException::class, ExtractionException::class)
    public fun getVisionOsPlayerResponse(
        contentCountry: ContentCountry,
        localization: Localization,
        videoId: String,
        cpn: String,
    ): JsonObject {
        requirePlayerParameters(videoId, cpn)
        return getVisionOsPlayerResponse(
            contentCountry, localization, videoId, cpn, getVisionOsVisitorData(localization, contentCountry),
        )
    }

    /**
     * A visitorData for the VISIONOS client. [visitorData] of the player requests: an extraction
     * gives the same one to both player requests of ONE video, so that only one visitor_id
     * request is sent. It must never be kept for another video: that would link the videos
     * watched.
     */
    @JvmStatic
    @Throws(IOException::class, ExtractionException::class)
    public fun getVisionOsVisitorData(localization: Localization, contentCountry: ContentCountry): String =
        YoutubeParsingHelper.getVisitorDataFromInnertube(
            InnertubeClientRequestInfo.ofVisionOsClient(), localization, contentCountry,
            visionOsHeaders(localization), YOUTUBEI_V1_URL, null, false,
        )

    /** The VISIONOS player response, with the [visitorData] of [getVisionOsVisitorData]. */
    @JvmStatic
    @Throws(IOException::class, ExtractionException::class)
    public fun getVisionOsPlayerResponse(
        contentCountry: ContentCountry,
        localization: Localization,
        videoId: String,
        cpn: String,
        visitorData: String,
    ): JsonObject {
        requirePlayerParameters(videoId, cpn)
        val innertubeClientRequestInfo = InnertubeClientRequestInfo.ofVisionOsClient()
        val headers = visionOsHeaders(localization)

        // We must always pass a valid visitorData to get valid player responses, which needs to be
        // got from YouTube
        innertubeClientRequestInfo.clientInfo.visitorData = visitorData

        val builder = YoutubeParsingHelper.prepareJsonBuilder(
            localization, contentCountry, innertubeClientRequestInfo, null,
        )

        addVideoIdCpnAndOkChecks(builder, videoId, cpn)

        val body = JsonWriter.string(builder.done()).toByteArray(StandardCharsets.UTF_8)

        val url = YOUTUBEI_V1_GAPIS_URL + PLAYER + "?" + DISABLE_PRETTY_PRINT_PARAMETER +
            "&t=" + YoutubeParsingHelper.generateTParameter() + "&id=" + videoId

        return JsonUtils.toJsonObject(
            YoutubeParsingHelper.getValidJsonResponseBody(
                downloader().postWithContentTypeJson(url, headers, body, localization),
            ),
        )
    }

    private fun visionOsHeaders(localization: Localization): Map<String, List<String>> = mapOf(
        "User-Agent" to listOf(YoutubeParsingHelper.getVisionOsUserAgent(localization)),
        "X-Goog-Api-Format-Version" to listOf("2"),
    )

    /** Refuses, without quoting them, a video ID or a nonce which could change the request. */
    @Throws(ParsingException::class)
    private fun requirePlayerParameters(videoId: String, cpn: String) {
        requireVideoId(videoId)
        if (!CPN_PATTERN.matches(cpn)) {
            throw ParsingException("Invalid content playback nonce")
        }
    }

    /** Refuses, without quoting it, a video ID which could change the request URL. */
    @Throws(ParsingException::class)
    private fun requireVideoId(videoId: String) {
        if (!VIDEO_ID_PATTERN.matches(videoId)) {
            throw ParsingException("Invalid video ID")
        }
    }

    private fun downloader(): Downloader =
        NewPipe.getDownloader() ?: throw NullPointerException("downloader is null")

    private fun addVideoIdCpnAndOkChecks(
        builder: JsonBuilder<JsonObject>,
        videoId: String,
        cpn: String?,
    ) {
        builder.value(VIDEO_ID, videoId)

        if (cpn != null) {
            builder.value(CPN, cpn)
        }

        builder.value(CONTENT_CHECK_OK, true)
            .value(RACY_CHECK_OK, true)
    }
}

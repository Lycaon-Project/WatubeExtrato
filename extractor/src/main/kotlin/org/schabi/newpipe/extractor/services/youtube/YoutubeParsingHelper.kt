/*
 * Created by Christian Schabesberger on 02.03.16.
 *
 * Copyright (C) 2016 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * YoutubeParsingHelper.kt is part of NewPipe Extractor.
 *
 * NewPipe Extractor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe Extractor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe Extractor. If not, see <https://www.gnu.org/licenses/>.
 */

package org.schabi.newpipe.extractor.services.youtube

import com.google.protobuf.InvalidProtocolBufferException
import org.jsoup.nodes.Entities
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.Image.ResolutionLevel
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.AccountTerminatedException
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.json.JsonArray
import org.schabi.newpipe.extractor.json.JsonBuilder
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonParser
import org.schabi.newpipe.extractor.json.JsonParserException
import org.schabi.newpipe.extractor.json.JsonWriter
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.services.youtube.ClientsConstants.DESKTOP_CLIENT_PLATFORM
import org.schabi.newpipe.extractor.services.youtube.ClientsConstants.VISIONOS_CLIENT_VERSION
import org.schabi.newpipe.extractor.services.youtube.ClientsConstants.VISIONOS_DEVICE_MODEL
import org.schabi.newpipe.extractor.services.youtube.ClientsConstants.VISIONOS_USER_AGENT_VERSION
import org.schabi.newpipe.extractor.services.youtube.ClientsConstants.WEB_CLIENT_ID
import org.schabi.newpipe.extractor.services.youtube.ClientsConstants.WEB_CLIENT_NAME
import org.schabi.newpipe.extractor.services.youtube.ClientsConstants.WEB_HARDCODED_CLIENT_VERSION
import org.schabi.newpipe.extractor.services.youtube.ClientsConstants.WEB_REMIX_CLIENT_ID
import org.schabi.newpipe.extractor.services.youtube.ClientsConstants.WEB_REMIX_CLIENT_NAME
import org.schabi.newpipe.extractor.services.youtube.ClientsConstants.WEB_REMIX_HARDCODED_CLIENT_VERSION
import org.schabi.newpipe.extractor.services.youtube.protos.video.Xtags.XTags
import org.schabi.newpipe.extractor.stream.AudioTrackType
import org.schabi.newpipe.extractor.utils.JsonUtils
import org.schabi.newpipe.extractor.utils.Parser
import org.schabi.newpipe.extractor.utils.PrivacyEvents
import org.schabi.newpipe.extractor.utils.RandomStringFromAlphabetGenerator
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeLinkGuards
import org.schabi.newpipe.extractor.utils.SecureUrls
import org.schabi.newpipe.extractor.utils.UrlPrivacy
import org.schabi.newpipe.extractor.utils.Utils
import org.schabi.newpipe.extractor.utils.Utils.HTTP
import org.schabi.newpipe.extractor.utils.Utils.HTTPS
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import java.util.Locale
import java.util.Random
import java.util.regex.Pattern

public object YoutubeParsingHelper {

    /**
     * The base URL of requests of the `WEB` clients to the InnerTube internal API.
     */
    public const val YOUTUBEI_V1_URL: String = "https://www.youtube.com/youtubei/v1/"

    /**
     * The base URL of requests of non-web clients to the InnerTube internal API.
     */
    public const val YOUTUBEI_V1_GAPIS_URL: String = "https://youtubei.googleapis.com/youtubei/v1/"

    /**
     * The base URL of YouTube Music.
     */
    private const val YOUTUBE_MUSIC_URL = "https://music.youtube.com"

    private const val YOUTUBE_URL = "https://www.youtube.com"

    /**
     * A parameter to disable pretty-printed response of InnerTube requests, to reduce response
     * sizes.
     *
     * Sent in query parameters of the requests.
     */
    public const val DISABLE_PRETTY_PRINT_PARAMETER: String = "prettyPrint=false"

    /**
     * A parameter sent by official clients named `contentPlaybackNonce`.
     *
     * It is sent by official clients on videoplayback requests and InnerTube player requests in
     * most cases.
     *
     * It is composed of 16 characters which are generated from
     * [this alphabet][CONTENT_PLAYBACK_NONCE_ALPHABET], with the use of strong random
     * values.
     *
     * @see generateContentPlaybackNonce
     */
    public const val CPN: String = "cpn"
    public const val VIDEO_ID: String = "videoId"

    /**
     * A parameter sent by official clients named `contentCheckOk`.
     *
     * Setting it to `true` allows us to get streaming data on videos with a warning about
     * what the sensible content they contain.
     */
    public const val CONTENT_CHECK_OK: String = "contentCheckOk"

    /**
     * A parameter which may be sent by official clients named `racyCheckOk`.
     *
     * What this parameter does is not really known, but it seems to be linked to sensitive
     * contents such as age-restricted content.
     */
    public const val RACY_CHECK_OK: String = "racyCheckOk"

    public const val STYLE: String = "style"
    public const val METADATA_BADGE_RENDERER: String = "metadataBadgeRenderer"
    public const val LABEL: String = "label"
    public const val THUMBNAIL_OVERLAYS: String = "thumbnailOverlays"
    public const val BADGES: String = "badges"

    // Read and written by the extraction threads of the app: always see the latest value
    @Volatile
    private var clientVersion: String? = null

    @Volatile
    private var youtubeMusicClientVersion: String? = null

    @Volatile
    private var clientVersionExtracted = false

    /** Only `true` is kept: one bad answer of the guide endpoint must not disable the fallback. */
    @Volatile
    private var hardcodedClientVersionValid: Boolean? = null

    /** Extracting the client version takes a request or two: one thread does it, the others wait. */
    private val CLIENT_VERSION_LOCK = Any()

    private val INNERTUBE_CONTEXT_CLIENT_VERSION_REGEXES: Array<Pattern> = arrayOf(
        Pattern.compile("INNERTUBE_CONTEXT_CLIENT_VERSION\":\"([0-9\\.]+?)\""),
        Pattern.compile("innertube_context_client_version\":\"([0-9\\.]+?)\""),
        Pattern.compile("client.version=([0-9\\.]+)"),
    )
    private val INITIAL_DATA_REGEXES: Array<Pattern> = arrayOf(
        Pattern.compile("window\\[\"ytInitialData\"\\]\\s*=\\s*(\\{.*?\\});"),
        Pattern.compile("var\\s*ytInitialData\\s*=\\s*(\\{.*?\\});"),
    )

    /**
     * A client version read from YouTube goes into the `X-YouTube-Client-Version` header and the
     * request bodies: digits and dots, which can never end a header or add another one.
     *
     * While YouTube runs a canary rollout, it serves versions such as
     * `2.20261020.01.00-canary_experiment_2.20261019.01.00`, which the website sends as they are.
     * That suffix is accepted too, but only with lowercase letters, digits, `_` and `.`, so it
     * stays just as harmless. Rejecting it would make every extraction fall back to the heavy
     * HTML page, then to the hardcoded version, which is easier to fingerprint.
     */
    private val CLIENT_VERSION_PATTERN = Regex("[0-9.]{1,32}(-[a-z0-9_.]{1,64})?")

    /**
     * What the visitor data given by YouTube is made of (URL-encoded base64), bounded: it is
     * sent back as it is in the next request bodies.
     */
    private val VISITOR_DATA_PATTERN = Regex("[A-Za-z0-9_%=+/-]{1,1024}")

    /** IDs placed in the URLs built from navigation endpoints. */
    private val SAFE_ID_PATTERN = Regex("[A-Za-z0-9_-]{1,256}")

    /** A cookie value sent back to YouTube: real ones are a few hundred characters at most. */
    private const val MAX_COOKIE_VALUE_LENGTH = 4096

    /** Server text placed in an exception message, which an app may log or show. */
    private const val MAX_SERVER_TEXT_LENGTH = 200

    /** A link to Google's web cache: the host is tested, not any part of the link. */
    private val WEBCACHE_URL = Regex("^https?://webcache\\.googleusercontent\\.com/", RegexOption.IGNORE_CASE)

    private const val CONTENT_PLAYBACK_NONCE_ALPHABET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    private const val CPN_LENGTH = 16
    private const val T_PARAMETER_LENGTH = 12

    // Nonces must not be predictable: the state of java.util.Random can be recovered from a few
    // nonces, which would let the server link requests of the same process to each other
    @Volatile
    private var numberGenerator: Random = SecureRandom()

    private const val GOOGLE_VIDEO_HOST_SUFFIX = ".googlevideo.com"
    private const val HTTPS_PORT = 443

    /** Plain ASCII labels: no percent-encoding, no empty label, no trailing dot. */
    private val HOST_NAME_PATTERN: Pattern = Pattern.compile("[a-z0-9-]+(\\.[a-z0-9-]+)+")

    private const val FEED_BASE_CHANNEL_ID = "https://www.youtube.com/feeds/videos.xml?channel_id="
    private const val FEED_BASE_USER = "https://www.youtube.com/feeds/videos.xml?user="
    private val C_WEB_PATTERN: Pattern = Pattern.compile("&c=WEB")
    private val C_VISIONOS_PATTERN: Pattern = Pattern.compile("&c=VISIONOS")
    private val VIOLATION_PATTERN = Regex(".*violat(ed|ion|ing).*")

    // google.com, www.google.fr, m.google.co.uk... but not google.example.com
    private val GOOGLE_HOST: Pattern =
        Pattern.compile("(?:www\\.|m\\.)?google\\.(?:[a-z]{2,3}\\.)?[a-z]{2,}\\.?")

    // a link to the YouTube redirect page, which logs the link followed before going to it
    private val YOUTUBE_REDIRECT_URL: Pattern = Pattern.compile(
        "^(?:https?://(?:www\\.|m\\.)?youtube\\.com)?/redirect\\?",
        Pattern.CASE_INSENSITIVE,
    )
    private val INVIDIOUS_URLS = setOf(
        "redirect.invidious.io", "yewtu.be", "piped.kavin.rocks", "piped.video", "inv.nadeko.net",
        "invidious.nerdvpn.de", "yt.chocolatemoo53.com", "invidious.tiekoetter.com",
        "invidious.f5.si",
    )
    private val YOUTUBE_URLS = setOf("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com")

    private const val HTTP_OK = 200
    private const val HTTP_NOT_FOUND = 404
    private const val MIN_GUIDE_RESPONSE_LENGTH = 5000
    private const val MIN_MUSIC_RESPONSE_LENGTH = 500
    private const val MIN_JSON_RESPONSE_LENGTH = 50
    private const val SW_JS_DATA_PREFIX = ")]}'"
    private const val SW_JS_DATA_CLIENT_VERSION_INDEX = 16

    @Volatile
    private var consentAccepted = false

    // URL(String) is deprecated on Java 20+, but URI parsing is stricter: keep java.net.URL
    @Suppress("DEPRECATION")
    @Throws(MalformedURLException::class)
    private fun urlOf(spec: String?): URL = URL(spec)

    private fun downloader(): Downloader =
        NewPipe.getDownloader() ?: throw NullPointerException("downloader is null")

    @JvmStatic
    public fun isGoogleURL(url: String?): Boolean {
        val cachedUrl = extractCachedUrlIfNeeded(url) ?: return false
        return try {
            val u = urlOf(cachedUrl)
            GOOGLE_HOST.matcher(u.host.lowercase(Locale.ROOT)).matches()
        } catch (_: MalformedURLException) {
            false
        }
    }

    @JvmStatic
    public fun isYoutubeURL(url: URL): Boolean = url.host.lowercase(Locale.ROOT) in YOUTUBE_URLS

    @JvmStatic
    public fun isYoutubeServiceURL(url: URL): Boolean {
        // lower-cased with the root locale, unlike equalsIgnoreCase, which would take a
        // look-alike dotless "ı" for an "i"
        val host = url.host.lowercase(Locale.ROOT)
        return host == "www.youtube-nocookie.com" || host == "youtu.be"
    }

    @JvmStatic
    public fun isInvidiousURL(url: URL): Boolean = url.host.lowercase(Locale.ROOT) in INVIDIOUS_URLS

    @JvmStatic
    public fun isY2ubeURL(url: URL): Boolean = url.host.lowercase(Locale.ROOT) == "y2u.be"

    /**
     * Parses the duration string of the video expecting ":" or "." as separators
     *
     * @return the duration in seconds
     * @throws ParsingException when more than 3 separators are found
     */
    @JvmStatic
    @Throws(ParsingException::class, NumberFormatException::class)
    public fun parseDurationString(input: String): Int {
        // the input comes from YouTube: it is not quoted in the messages
        if (input.none { it in '0'..'9' } && !input.equals("SHORTS", ignoreCase = true)) {
            throw ParsingException("Error duration string contains no digits")
        }

        // If time separator : is not detected, try . instead
        // (trailing empty parts are dropped, as String.split does in Java)
        val splitInput = input.split(if (':' in input) ':' else '.')
            .dropLastWhile { it.isEmpty() }
            .ifEmpty { listOf("") }

        val units = intArrayOf(24, 60, 60, 1)
        val offset = units.size - splitInput.size
        if (offset < 0) {
            throw ParsingException("Error duration string with unknown format")
        }
        var duration = 0
        for (i in splitInput.indices) {
            duration = units[i + offset] * (duration + convertDurationToInt(splitInput[i]))
        }
        return duration
    }

    /**
     * Tries to convert a duration string to an integer without throwing an exception.
     *
     * Helper method for [parseDurationString].
     *
     * Note: This method is also used as a workaround for NewPipe#8034 (YT shorts no longer
     * display any duration in channels).
     *
     * @param input The string to process
     * @return The converted integer or 0 if the conversion failed.
     */
    private fun convertDurationToInt(input: String?): Int {
        if (input.isNullOrEmpty()) {
            return 0
        }
        return Utils.removeNonDigitCharacters(input).toIntOrNull() ?: 0
    }

    @JvmStatic
    public fun getFeedUrlFrom(channelIdOrUser: String): String {
        // encoded: an ID or a user name never changes, anything else cannot add parameters
        return when {
            channelIdOrUser.startsWith("user/") ->
                FEED_BASE_USER + Utils.encodeUrlUtf8(channelIdOrUser.replace("user/", ""))

            channelIdOrUser.startsWith("channel/") ->
                FEED_BASE_CHANNEL_ID + Utils.encodeUrlUtf8(channelIdOrUser.replace("channel/", ""))

            else -> FEED_BASE_CHANNEL_ID + Utils.encodeUrlUtf8(channelIdOrUser)
        }
    }

    /**
     * Checks if the given playlist id is a YouTube Mix (auto-generated playlist)
     * Ids from a YouTube Mix start with "RD"
     *
     * @param playlistId the playlist id
     * @return Whether given id belongs to a YouTube Mix
     */
    @JvmStatic
    public fun isYoutubeMixId(playlistId: String): Boolean = playlistId.startsWith("RD")

    /**
     * Checks if the given playlist id is a YouTube My Mix (auto-generated playlist)
     * Ids from a YouTube My Mix start with "RDMM"
     *
     * @param playlistId the playlist id
     * @return Whether given id belongs to a YouTube My Mix
     */
    @JvmStatic
    public fun isYoutubeMyMixId(playlistId: String): Boolean = playlistId.startsWith("RDMM")

    /**
     * Checks if the given playlist id is a YouTube Music Mix (auto-generated playlist)
     * Ids from a YouTube Music Mix start with "RDAMVM" or "RDCLAK"
     *
     * @param playlistId the playlist id
     * @return Whether given id belongs to a YouTube Music Mix
     */
    @JvmStatic
    public fun isYoutubeMusicMixId(playlistId: String): Boolean =
        playlistId.startsWith("RDAMVM") || playlistId.startsWith("RDCLAK")

    /**
     * Checks if the given playlist id is a YouTube Genre Mix (auto-generated playlist)
     * Ids from a YouTube Genre Mix start with "RDGMEM"
     *
     * @return Whether given id belongs to a YouTube Genre Mix
     */
    @JvmStatic
    public fun isYoutubeGenreMixId(playlistId: String): Boolean = playlistId.startsWith("RDGMEM")

    /**
     * @param playlistId the playlist id to parse
     * @return the [PlaylistInfo.PlaylistType] extracted from the playlistId (mix playlist
     *         types included)
     * @throws ParsingException if the playlistId is null or empty, if the playlistId is not a mix,
     *                          if it is a mix but it's not based on a specific stream (this is the
     *                          case for channel or genre mixes)
     */
    @JvmStatic
    @Throws(ParsingException::class)
    public fun extractVideoIdFromMixId(playlistId: String?): String {
        // the playlist ID is not quoted in the messages: it tells what the user listens to
        return when {
            playlistId.isNullOrEmpty() ->
                throw ParsingException("Video id could not be determined from empty playlist id")

            isYoutubeMyMixId(playlistId) -> playlistId.substring(4)

            isYoutubeMusicMixId(playlistId) -> playlistId.substring(6)

            // Genre mixes are of the form RDGMEM{garbage}, so videoId can't be determined
            isYoutubeGenreMixId(playlistId) ->
                throw ParsingException("Video id could not be determined from genre mix id")

            isYoutubeMixId(playlistId) -> { // normal mix
                if (playlistId.length != MIX_ID_LENGTH) {
                    // Stream YouTube mixes are of the form RD{videoId}, but if videoId is not
                    // exactly 11 characters then it can't be a video id, hence we are dealing
                    // with a different type of mix (e.g. genre mixes handled above, of the form
                    // RDGMEM{garbage})
                    throw ParsingException("Video id could not be determined from mix id")
                }
                playlistId.substring(2)
            }

            // not a mix
            else -> throw ParsingException("Video id could not be determined from playlist id")
        }
    }

    private const val MIX_ID_LENGTH = 13

    /**
     * @param playlistId the playlist id to parse
     * @return the [PlaylistInfo.PlaylistType] extracted from the playlistId (mix playlist
     *         types included)
     * @throws ParsingException if the playlistId is null or empty
     */
    @JvmStatic
    @Throws(ParsingException::class)
    public fun extractPlaylistTypeFromPlaylistId(playlistId: String?): PlaylistInfo.PlaylistType =
        when {
            playlistId.isNullOrEmpty() ->
                throw ParsingException("Could not extract playlist type from empty playlist id")

            isYoutubeMusicMixId(playlistId) -> PlaylistInfo.PlaylistType.MIX_MUSIC

            isYoutubeGenreMixId(playlistId) -> PlaylistInfo.PlaylistType.MIX_GENRE

            // Either a normal mix based on a stream, or a "my mix" (still based on a stream).
            // NOTE: if YouTube introduces even more types of mixes that still start with RD,
            // they will default to this, even though they might not be based on a stream.
            isYoutubeMixId(playlistId) -> PlaylistInfo.PlaylistType.MIX_STREAM

            // not a known type of mix: just consider it a normal playlist
            else -> PlaylistInfo.PlaylistType.NORMAL
        }

    /**
     * @param playlistUrl the playlist url to parse
     * @return the [PlaylistInfo.PlaylistType] extracted from the playlistUrl's list param
     *         (mix playlist types included)
     * @throws ParsingException if the playlistUrl is malformed, if has no list param or if the list
     *                          param is empty
     */
    @JvmStatic
    @Throws(ParsingException::class)
    public fun extractPlaylistTypeFromPlaylistUrl(playlistUrl: String?): PlaylistInfo.PlaylistType {
        val url = try {
            Utils.stringToURL(playlistUrl)
        } catch (_: MalformedURLException) {
            // not chained: its message quotes the URL
            throw ParsingException("Could not extract playlist type from malformed url")
        }
        return extractPlaylistTypeFromPlaylistId(Utils.getQueryValue(url, "list"))
    }

    @Throws(ParsingException::class)
    private fun getInitialData(html: String): JsonObject = try {
        JsonParser.`object`().from(Utils.getStringResultFromRegexArray(html, INITIAL_DATA_REGEXES, 1))
    } catch (e: JsonParserException) {
        throw ParsingException("Could not get ytInitialData", e)
    } catch (e: Parser.RegexException) {
        throw ParsingException("Could not get ytInitialData", e)
    }

    @JvmStatic
    @Throws(IOException::class, ExtractionException::class)
    public fun isHardcodedClientVersionValid(): Boolean {
        hardcodedClientVersionValid?.let { return it }
        // @formatter:off
        val body = JsonWriter.string()
            .`object`()
                .`object`("context")
                    .`object`("client")
                        .value("hl", "en-GB")
                        .value("gl", "GB")
                        .value("clientName", WEB_CLIENT_NAME)
                        .value("clientVersion", WEB_HARDCODED_CLIENT_VERSION)
                        .value("platform", DESKTOP_CLIENT_PLATFORM)
                        .value("utcOffsetMinutes", 0)
                    .end()
                    .`object`("request")
                        .array("internalExperimentFlags")
                        .end()
                        .value("useSsl", true)
                    .end()
                    .`object`("user")
                        // TODO: provide a way to enable restricted mode with:
                        //  .value("enableSafetyMode", boolean)
                        .value("lockedSafetyMode", false)
                    .end()
                .end()
                .value("fetchLiveState", true)
            .end().done().toByteArray(StandardCharsets.UTF_8)
        // @formatter:on

        val headers = getClientHeaders(WEB_CLIENT_ID, WEB_HARDCODED_CLIENT_VERSION)

        // This endpoint is fetched by the YouTube website to get the items of its main menu and is
        // pretty lightweight (around 30kB)
        val response = downloader().postWithContentTypeJson(
            YOUTUBEI_V1_URL + "guide?" + DISABLE_PRETTY_PRINT_PARAMETER, headers, body,
        )
        // Ensure to have a valid response
        val valid = response.responseBody().length > MIN_GUIDE_RESPONSE_LENGTH &&
            response.responseCode() == HTTP_OK
        if (valid) {
            hardcodedClientVersionValid = true
        }
        return valid
    }

    /** [version] when it is a well-formed client version, `null` otherwise. */
    internal fun validClientVersionOrNull(version: String?): String? =
        version?.takeIf { CLIENT_VERSION_PATTERN.matches(it) }

    @Throws(IOException::class, ExtractionException::class)
    private fun extractClientVersionFromSwJs() {
        if (clientVersionExtracted) {
            return
        }
        val url = "https://www.youtube.com/sw.js_data"
        val headers = getOriginReferrerHeaders(YOUTUBE_URL)
        val response = downloader().get(url, headers).responseBody().removePrefix(SW_JS_DATA_PREFIX)

        val data = try {
            JsonParser.array().from(response)
        } catch (e: JsonParserException) {
            throw ParsingException("Could not extract YouTube WEB InnerTube client version from sw.js", e)
        }
        clientVersion = validClientVersionOrNull(
            data.getArray(0).getArray(2).getArray(0).getArray(0)
                .getString(SW_JS_DATA_CLIENT_VERSION_INDEX),
        ) ?: throw ParsingException("Could not extract YouTube WEB InnerTube client version from sw.js")
        clientVersionExtracted = true
    }

    @Throws(IOException::class, ExtractionException::class)
    private fun extractClientVersionFromHtmlSearchResultsPage() {
        // Don't extract the InnerTube client version if it has been already extracted
        if (clientVersionExtracted) {
            return
        }

        // Don't provide a search term in order to have a smaller response
        val url = "https://www.youtube.com/results?search_query=&ucbcb=1"
        val html = downloader().get(url, getCookieHeader()).responseBody()
        val initialData = getInitialData(html)
        val serviceTrackingParams = initialData.getObject("responseContext")
            .getArray("serviceTrackingParams")

        // Try to get version from initial data first
        var version = getClientVersionFromServiceTrackingParam(serviceTrackingParams, "CSI", "cver")

        if (version == null) {
            version = try {
                validClientVersionOrNull(
                    Utils.getStringResultFromRegexArray(
                        html, INNERTUBE_CONTEXT_CLIENT_VERSION_REGEXES, 1,
                    ),
                )
            } catch (_: Parser.RegexException) {
                null
            }
        }

        // Fallback to get a shortened client version which does not contain the last two
        // digits
        if (version.isNullOrEmpty()) {
            version = getClientVersionFromServiceTrackingParam(
                serviceTrackingParams, "ECATCHER", "client.version",
            )
        }

        clientVersion = version ?: throw ParsingException(
            "Could not extract YouTube WEB InnerTube client version from HTML search results page",
        )
        clientVersionExtracted = true
    }

    private fun getClientVersionFromServiceTrackingParam(
        serviceTrackingParams: JsonArray,
        serviceName: String,
        clientVersionKey: String,
    ): String? {
        for (serviceTrackingParam in serviceTrackingParams) {
            if (serviceTrackingParam !is JsonObject ||
                serviceTrackingParam.getString("service", "") != serviceName
            ) {
                continue
            }
            for (param in serviceTrackingParam.getArray("params")) {
                if (param is JsonObject && param.getString("key", "") == clientVersionKey) {
                    val value = param.getString("value")
                    if (!value.isNullOrEmpty()) {
                        // a malformed version is skipped, like a missing one
                        validClientVersionOrNull(value)?.let { return it }
                    }
                }
            }
        }
        return null
    }

    /**
     * Get the client version used by YouTube website on InnerTube requests.
     */
    @JvmStatic
    @Throws(IOException::class, ExtractionException::class)
    public fun getClientVersion(): String {
        clientVersion?.takeIf { it.isNotEmpty() }?.let { return it }
        return synchronized(CLIENT_VERSION_LOCK) {
            // extracted by another thread while this one waited
            clientVersion?.takeIf { it.isNotEmpty() } ?: fetchClientVersion()
        }
    }

    @Throws(IOException::class, ExtractionException::class)
    private fun fetchClientVersion(): String {
        // Always extract the latest client version, by trying first to extract it from the
        // JavaScript service worker, then from HTML search results page as a fallback, to prevent
        // fingerprinting based on the client version used
        try {
            extractClientVersionFromSwJs()
        } catch (_: Exception) {
            try {
                extractClientVersionFromHtmlSearchResultsPage()
            } catch (_: ParsingException) {
                // a page YouTube changed: the hardcoded version below may still work. A network
                // error or a reCAPTCHA is not caught: the app must show it
            }
        }

        if (clientVersionExtracted) {
            clientVersion?.let { return it }
        }

        // Fallback to the hardcoded one if it is valid
        if (isHardcodedClientVersionValid()) {
            clientVersion = WEB_HARDCODED_CLIENT_VERSION
            return WEB_HARDCODED_CLIENT_VERSION
        }

        throw ExtractionException("Could not get YouTube WEB client version")
    }

    /**
     * **Only used in tests.**
     *
     * Quick-and-dirty solution to reset global state in between test classes.
     *
     * This is needed for the mocks because in order to reach that state a network request has to
     * be made. If the global state is not reset and the RecordingDownloader is used,
     * then only the first test class has that request recorded. Meaning running the other
     * tests with mocks will fail, because the mock is missing.
     */
    @JvmStatic
    public fun resetClientVersion() {
        synchronized(CLIENT_VERSION_LOCK) {
            clientVersion = null
            clientVersionExtracted = false
            hardcodedClientVersionValid = null
        }
        youtubeMusicClientVersion = null
    }

    /**
     * **Only used in tests.**
     */
    @JvmStatic
    public fun setNumberGenerator(random: Random) {
        numberGenerator = random
    }

    @JvmStatic
    @Throws(IOException::class, ReCaptchaException::class)
    public fun isHardcodedYoutubeMusicClientVersionValid(): Boolean {
        val url = "https://music.youtube.com/youtubei/v1/music/get_search_suggestions?" +
            DISABLE_PRETTY_PRINT_PARAMETER

        // @formatter:off
        val json = JsonWriter.string()
            .`object`()
                .`object`("context")
                    .`object`("client")
                        .value("clientName", WEB_REMIX_CLIENT_NAME)
                        .value("clientVersion", WEB_REMIX_HARDCODED_CLIENT_VERSION)
                        .value("hl", "en-GB")
                        .value("gl", "GB")
                        .value("platform", DESKTOP_CLIENT_PLATFORM)
                        .value("utcOffsetMinutes", 0)
                    .end()
                    .`object`("request")
                        .array("internalExperimentFlags")
                        .end()
                        .value("useSsl", true)
                    .end()
                    .`object`("user")
                        // TODO: provide a way to enable restricted mode with:
                        //  .value("enableSafetyMode", boolean)
                        .value("lockedSafetyMode", false)
                    .end()
                .end()
                .value("input", "")
            .end().done().toByteArray(StandardCharsets.UTF_8)
        // @formatter:on

        val headers = HashMap(getOriginReferrerHeaders(YOUTUBE_MUSIC_URL))
        headers.putAll(getClientHeaders(WEB_REMIX_CLIENT_ID, WEB_REMIX_HARDCODED_CLIENT_VERSION))

        val response = downloader().postWithContentTypeJson(url, headers, json)
        // Ensure to have a valid response
        return response.responseBody().length > MIN_MUSIC_RESPONSE_LENGTH &&
            response.responseCode() == HTTP_OK
    }

    @JvmStatic
    @Throws(IOException::class, ReCaptchaException::class, Parser.RegexException::class)
    public fun getYoutubeMusicClientVersion(): String {
        youtubeMusicClientVersion?.takeIf { it.isNotEmpty() }?.let { return it }
        if (isHardcodedYoutubeMusicClientVersionValid()) {
            youtubeMusicClientVersion = WEB_REMIX_HARDCODED_CLIENT_VERSION
            return WEB_REMIX_HARDCODED_CLIENT_VERSION
        }

        val version = try {
            val url = "https://music.youtube.com/sw.js"
            val headers = getOriginReferrerHeaders(YOUTUBE_MUSIC_URL)
            val response = downloader().get(url, headers).responseBody()

            Utils.getStringResultFromRegexArray(response, INNERTUBE_CONTEXT_CLIENT_VERSION_REGEXES, 1)
        } catch (_: Exception) {
            val url = "https://music.youtube.com/?ucbcb=1"
            val html = downloader().get(url, getCookieHeader()).responseBody()

            Utils.getStringResultFromRegexArray(html, INNERTUBE_CONTEXT_CLIENT_VERSION_REGEXES, 1)
        }
        // the patterns only match digits and dots; the length is bounded too, as it goes in a
        // header
        validClientVersionOrNull(version)
            ?: throw Parser.RegexException("Invalid YouTube Music client version")
        youtubeMusicClientVersion = version
        return version
    }

    /** Whether [id] can be placed in a URL as it is: URL-safe characters only, bounded. */
    private fun isSafeId(id: String?): Boolean = id != null && SAFE_ID_PATTERN.matches(id)

    /**
     * Whether [path], given by YouTube, can be appended to `https://www.youtube.com`: it must be
     * an absolute path, so that it can never change the host (`.evil.example`, `@evil.example`)
     * nor make the URL protocol-relative, and it must not carry spaces or control characters.
     */
    private fun isSafeYoutubePath(path: String?): Boolean =
        path != null && path.length <= Utils.MAX_URL_LENGTH &&
            path.startsWith("/") && !path.startsWith("//") &&
            path.none { it == '\\' || it.isWhitespace() || it.isISOControl() }

    private fun youtubeUrlFromPathOrNull(path: String?): String? =
        if (isSafeYoutubePath(path)) YOUTUBE_URL + path else null

    /**
     * [getUrlFromNavigationEndpoint], kept only when it leads to YouTube: the channel, show,
     * artist or playlist of an item is a YouTube page, never a third-party site that a tampered
     * response would make the app open or fetch.
     */
    internal fun getYoutubeUrlFromNavigationEndpoint(navigationEndpoint: JsonObject): String? =
        getUrlFromNavigationEndpoint(navigationEndpoint)?.takeIf(::isYoutubeLink)

    /** [getUrlFromObject], kept only when it leads to YouTube (see [getYoutubeUrlFromNavigationEndpoint]). */
    internal fun getYoutubeUrlFromObject(textObject: JsonObject?): String? =
        getUrlFromObject(textObject)?.takeIf(::isYoutubeLink)

    /** Whether [url] is an http(s) link to youtube.com, www, m or music. */
    internal fun isYoutubeLink(url: String): Boolean =
        YoutubeLinkGuards.parseUrlOrNull(url)?.let { Utils.isHTTP(it) && isYoutubeURL(it) } == true

    @JvmStatic
    public fun getUrlFromNavigationEndpoint(navigationEndpoint: JsonObject): String? {
        if (navigationEndpoint.has("urlEndpoint")) {
            val internUrl = navigationEndpoint.getObject("urlEndpoint").getString("url", "").orEmpty()
            getUrlFromUrlEndpoint(internUrl)?.let { return it.ifEmpty { null } }
        }

        if (navigationEndpoint.has("browseEndpoint")) {
            val browseEndpoint = navigationEndpoint.getObject("browseEndpoint")
            val canonicalBaseUrl = browseEndpoint.getString("canonicalBaseUrl")
            val browseId = browseEndpoint.getString("browseId")

            if (browseId != null && isSafeId(browseId)) {
                if (browseId.startsWith("UC")) {
                    // All channel IDs are prefixed with UC
                    return "https://www.youtube.com/channel/$browseId"
                } else if (browseId.startsWith("VL")) {
                    // All playlist IDs are prefixed with VL, which needs to be removed from the
                    // playlist ID
                    return "https://www.youtube.com/playlist?list=" + browseId.substring(2)
                }
            }

            if (!canonicalBaseUrl.isNullOrEmpty()) {
                youtubeUrlFromPathOrNull(canonicalBaseUrl)?.let { return it }
            }
        }

        if (navigationEndpoint.has("watchEndpoint")) {
            val watchEndpoint = navigationEndpoint.getObject("watchEndpoint")
            val videoId = watchEndpoint.getString(VIDEO_ID)
            // without a valid video ID, the next endpoints may still give a link
            if (isSafeId(videoId)) {
                return buildString {
                    append("https://www.youtube.com/watch?v=").append(videoId)
                    val playlistId = watchEndpoint.getString("playlistId")
                    if (watchEndpoint.has("playlistId") && isSafeId(playlistId)) {
                        append("&list=").append(playlistId)
                    }
                    if (watchEndpoint.has("startTimeSeconds")) {
                        append("&t=").append(watchEndpoint.getInt("startTimeSeconds"))
                    }
                }
            }
        }

        if (navigationEndpoint.has("watchPlaylistEndpoint")) {
            val playlistId = navigationEndpoint.getObject("watchPlaylistEndpoint")
                .getString("playlistId")
            if (isSafeId(playlistId)) {
                return "https://www.youtube.com/playlist?list=$playlistId"
            }
        }

        if (navigationEndpoint.has("showDialogCommand")) {
            try {
                val listItems = JsonUtils.getArray(
                    navigationEndpoint,
                    "showDialogCommand.panelLoadingStrategy.inlineContent.dialogViewModel" +
                        ".customContent.listViewModel.listItems",
                )

                // the first item seems to always be the channel that actually uploaded the video,
                // i.e. it appears in their video feed
                val command = JsonUtils.getObject(
                    listItems.getObject(0),
                    "listItemViewModel.rendererContext.commandContext.onTap.innertubeCommand",
                )
                // a command never holds another dialog in practice; if a tampered one did, the
                // depth of the JSON (bounded by the parser) bounds this recursion
                return getUrlFromNavigationEndpoint(command)
            } catch (_: ParsingException) {
                // no usable command: try the metadata below
            }
        }

        if (navigationEndpoint.has("commandMetadata")) {
            val metadata = navigationEndpoint.getObject("commandMetadata")
                .getObject("webCommandMetadata")
            if (metadata.has("url")) {
                return youtubeUrlFromPathOrNull(metadata.getString("url"))
            }
        }

        return null
    }

    /**
     * @return the link of a `urlEndpoint`; an empty string when it gives no link and the other
     * endpoints must not be tried (an unsafe redirect); `null` when they can be tried
     */
    private fun getUrlFromUrlEndpoint(internUrl: String): String? {
        val redirect = YOUTUBE_REDIRECT_URL.matcher(internUrl)

        if (redirect.find()) {
            // privacy: the target is opened directly, so YouTube never learns which link was
            // followed, and without the trackers the target site appended to it
            // q parameter can be the first parameter
            val params = internUrl.substring(redirect.end()).split('&')
            val param = params.firstOrNull { it.startsWith("q=") } ?: return null
            return try {
                val target = Utils.decodeUrlUtf8(param.substring(2))
                if (!SecureUrls.isWebLink(target)) {
                    // a javascript:, intent: or file: target must never become a
                    // link the user can follow
                    ""
                } else {
                    // the redirect through YouTube is a tracker of its own
                    PrivacyEvents.report(PrivacyEvents.Event.TRACKER_REMOVED)
                    // and so are the redirections of other sites it may lead to
                    SecureUrls.upgradeToHttps(UrlPrivacy.cleanLink(target)).orEmpty()
                }
            } catch (_: IllegalArgumentException) {
                // a malformed escape sequence: no usable target
                ""
            }
        } else if (SecureUrls.isWebLink(internUrl)) {
            // a link through Google, Facebook or an ad server opens its target directly
            return SecureUrls.upgradeToHttps(UrlPrivacy.cleanLink(internUrl)).orEmpty()
        } else if (internUrl.startsWith("/channel") || internUrl.startsWith("/user") ||
            internUrl.startsWith("/watch")
        ) {
            return youtubeUrlFromPathOrNull(internUrl)
        }
        return null
    }

    /**
     * Get the text from a JSON object that has either a `simpleText` or a `runs`
     * array.
     *
     * @param textObject JSON object to get the text from
     * @param html       whether to return HTML, by parsing the `navigationEndpoint`
     * @return text in the JSON object or `null`
     */
    @JvmStatic
    public fun getTextFromObject(textObject: JsonObject?, html: Boolean): String? {
        if (textObject.isNullOrEmpty()) {
            return null
        }

        if (textObject.has("simpleText")) {
            val simpleText = textObject.getString("simpleText")
            // as HTML, the text is escaped like the runs below: it is shown, never interpreted
            return if (html && simpleText != null) {
                textToHtml(Entities.escape(simpleText))
            } else {
                simpleText
            }
        }

        val runs = textObject.getArray("runs")
        if (runs.isEmpty()) {
            return null
        }

        val textBuilder = StringBuilder()
        for (o in runs) {
            // a run which is not an object (tampered data) has no text
            val run = o as? JsonObject ?: continue
            val text = run.getString("text").orEmpty()

            if (html) {
                appendHtmlRun(textBuilder, run, text)
            } else {
                textBuilder.append(text)
            }
        }

        val text = textBuilder.toString()
        return if (html) textToHtml(text) else text
    }

    private fun appendHtmlRun(textBuilder: StringBuilder, run: JsonObject, text: String) {
        val url = if (run.has("navigationEndpoint")) {
            getUrlFromNavigationEndpoint(run.getObject("navigationEndpoint"))
        } else {
            null
        }
        // every run is escaped, not only links: a "<" in a comment or a description
        // must not open a tag in the HTML handed to the app
        val escapedText = Entities.escape(text)
        val content = if (url.isNullOrEmpty()) {
            escapedText
        } else {
            "<a href=\"" + Entities.escape(url) + "\">" + escapedText + "</a>"
        }

        val bold = run.has("bold") && run.getBoolean("bold")
        val italic = run.has("italics") && run.getBoolean("italics")
        val strikethrough = run.has("strikethrough") && run.getBoolean("strikethrough")

        if (bold) {
            textBuilder.append("<b>")
        }
        if (italic) {
            textBuilder.append("<i>")
        }
        if (strikethrough) {
            textBuilder.append("<s>")
        }

        textBuilder.append(content)

        if (strikethrough) {
            textBuilder.append("</s>")
        }
        if (italic) {
            textBuilder.append("</i>")
        }
        if (bold) {
            textBuilder.append("</b>")
        }
    }

    private fun textToHtml(escapedText: String): String =
        escapedText.replace("\n", "<br>").replace("  ", " &nbsp;")

    @JvmStatic
    @Throws(ParsingException::class)
    public fun getTextFromObjectOrThrow(textObject: JsonObject?, error: String?): String =
        getTextFromObject(textObject) ?: throw ParsingException("Could not extract text: $error")

    @JvmStatic
    public fun getTextFromObject(textObject: JsonObject?): String? = getTextFromObject(textObject, false)

    @JvmStatic
    public fun getUrlFromObject(textObject: JsonObject?): String? {
        if (textObject.isNullOrEmpty()) {
            return null
        }

        val runs = textObject.getArray("runs")
        if (runs.isEmpty()) {
            return null
        }

        for (textPart in runs) {
            val run = textPart as? JsonObject ?: continue
            val url = getUrlFromNavigationEndpoint(run.getObject("navigationEndpoint"))
            if (!url.isNullOrEmpty()) {
                return url
            }
        }

        return null
    }

    @JvmStatic
    public fun getTextAtKey(jsonObject: JsonObject, theKey: String): String? =
        if (jsonObject.isString(theKey)) {
            jsonObject.getString(theKey)
        } else {
            getTextFromObject(jsonObject.getObject(theKey))
        }

    /**
     * Whether [url] is an https image on a host of YouTube or Google (ytimg.com, ggpht.com,
     * googleusercontent.com...). An app loads every thumbnail it is given: one on another host
     * would reveal the user's address to that host, like a tracking pixel.
     */
    internal fun isYoutubeImageUrl(url: String): Boolean {
        val parsed = YoutubeLinkGuards.parseUrlOrNull(url) ?: return false
        return parsed.protocol == "https" && parsed.userInfo == null &&
            (parsed.port == -1 || parsed.port == HTTPS_PORT) && SecureUrls.isGoogleHost(parsed.host)
    }

    @JvmStatic
    public fun fixThumbnailUrl(thumbnailUrl: String): String {
        var result = thumbnailUrl.removePrefix("//")

        if (result.startsWith(HTTP)) {
            result = Utils.replaceHttpWithHttps(result) ?: result
        } else if (!result.startsWith(HTTPS)) {
            result = "https://$result"
        }

        return result
    }

    /**
     * Get thumbnails from a [JsonObject] representing a YouTube
     * [InfoItem][org.schabi.newpipe.extractor.InfoItem].
     *
     * Thumbnails are got from the `thumbnails` [JsonArray] inside the `thumbnail`
     * [JsonObject] of the YouTube [InfoItem][org.schabi.newpipe.extractor.InfoItem],
     * using [getImagesFromThumbnailsArray].
     *
     * @param infoItem a YouTube [InfoItem][org.schabi.newpipe.extractor.InfoItem]
     * @return an unmodifiable list of [Image]s found in the `thumbnails` [JsonArray]
     * @throws ParsingException if an exception occurs when [getImagesFromThumbnailsArray] is
     * executed
     */
    @JvmStatic
    @Throws(ParsingException::class)
    public fun getThumbnailsFromInfoItem(infoItem: JsonObject): List<Image> = try {
        getImagesFromThumbnailsArray(infoItem.getObject("thumbnail").getArray("thumbnails"))
    } catch (e: Exception) {
        throw ParsingException("Could not get thumbnails from InfoItem", e)
    }

    /**
     * Get images from a YouTube `thumbnails` [JsonArray].
     *
     * The properties of the [Image]s created will be set using the corresponding ones of
     * thumbnail items.
     *
     * @param thumbnails a YouTube `thumbnails` [JsonArray]
     * @return an unmodifiable list of [Image]s extracted from the given [JsonArray]
     */
    @JvmStatic
    public fun getImagesFromThumbnailsArray(thumbnails: JsonArray): List<Image> {
        val images = ArrayList<Image>(thumbnails.size)
        for (item in thumbnails) {
            val thumbnail = item as? JsonObject ?: continue
            val url = thumbnail.getString("url")
            if (url.isNullOrEmpty()) {
                continue
            }
            val imageUrl = fixThumbnailUrl(url)
            if (!isYoutubeImageUrl(imageUrl)) {
                continue
            }
            val height = thumbnail.getInt("height", Image.HEIGHT_UNKNOWN)
            images.add(
                Image(
                    imageUrl,
                    height,
                    thumbnail.getInt("width", Image.WIDTH_UNKNOWN),
                    ResolutionLevel.fromHeight(height),
                ),
            )
        }
        return java.util.Collections.unmodifiableList(images)
    }

    @JvmStatic
    @Throws(ParsingException::class, MalformedURLException::class)
    public fun getValidJsonResponseBody(response: Response): String {
        if (response.responseCode() == HTTP_NOT_FOUND) {
            throw ContentNotAvailableException(
                "Not found (\"" + response.responseCode() + " " + response.responseMessage() + "\")",
            )
        }

        val responseBody = response.responseBody()
        if (responseBody.length < MIN_JSON_RESPONSE_LENGTH) { // Ensure to have a valid response
            throw ParsingException("JSON response is too short")
        }

        // Check if the request was redirected to the error page.
        val latestUrl = try {
            urlOf(response.latestUrl())
        } catch (_: MalformedURLException) {
            // not chained: its message quotes the URL
            throw MalformedURLException("The latest URL of the response is invalid")
        }
        if (latestUrl.host.equals("www.youtube.com", ignoreCase = true)) {
            val path = latestUrl.path
            if (path.equals("/oops", ignoreCase = true) || path.equals("/error", ignoreCase = true)) {
                throw ContentNotAvailableException("Content unavailable")
            }
        }

        val responseContentType = response.getHeader("Content-Type")
        if (responseContentType != null &&
            responseContentType.lowercase(Locale.ROOT).contains("text/html")
        ) {
            // the latest URL is not quoted: it holds the IDs of what was requested
            throw ParsingException("Got HTML document, expected JSON response")
        }

        return responseBody
    }

    @JvmStatic
    @Throws(IOException::class, ExtractionException::class)
    public fun getJsonPostResponse(
        endpoint: String,
        body: ByteArray?,
        localization: Localization,
    ): JsonObject {
        val headers = getYouTubeHeaders()

        return JsonUtils.toJsonObject(
            getValidJsonResponseBody(
                downloader().postWithContentTypeJson(
                    YOUTUBEI_V1_URL + endpoint + "?" + DISABLE_PRETTY_PRINT_PARAMETER,
                    headers, body, localization,
                ),
            ),
        )
    }

    @JvmStatic
    @Throws(IOException::class, ExtractionException::class)
    public fun getJsonPostResponse(
        endpoint: String,
        queryParameters: List<String>,
        body: ByteArray?,
        localization: Localization,
    ): JsonObject {
        val headers = getYouTubeHeaders()

        val queryParametersString = if (queryParameters.isEmpty()) {
            "?$DISABLE_PRETTY_PRINT_PARAMETER"
        } else {
            "?" + queryParameters.joinToString("&") + "&" + DISABLE_PRETTY_PRINT_PARAMETER
        }

        return JsonUtils.toJsonObject(
            getValidJsonResponseBody(
                downloader().postWithContentTypeJson(
                    YOUTUBEI_V1_URL + endpoint + queryParametersString, headers, body, localization,
                ),
            ),
        )
    }

    @JvmStatic
    @Throws(IOException::class, ExtractionException::class)
    public fun prepareDesktopJsonBuilder(
        localization: Localization,
        contentCountry: ContentCountry,
    ): JsonBuilder<JsonObject> {
        // @formatter:off
        return JsonObject.builder()
            .`object`("context")
                .`object`("client")
                    .value("hl", localization.localizationCode)
                    .value("gl", contentCountry.countryCode)
                    .value("clientName", WEB_CLIENT_NAME)
                    .value("clientVersion", getClientVersion())
                    .value("originalUrl", YOUTUBE_URL)
                    .value("platform", DESKTOP_CLIENT_PLATFORM)
                    .value("utcOffsetMinutes", 0)
                .end()
                .`object`("request")
                    .array("internalExperimentFlags")
                    .end()
                    .value("useSsl", true)
                .end()
                .`object`("user")
                    // TODO: provide a way to enable restricted mode with:
                    //  .value("enableSafetyMode", boolean)
                    .value("lockedSafetyMode", false)
                .end()
            .end()
        // @formatter:on
    }

    /**
     * Get the user-agent string used as the user-agent for InnerTube requests with the visionOS
     * client.
     *
     * If the [Localization] provided is `null`, fallbacks to
     * [the default one][Localization.DEFAULT].
     *
     * @param localization the [Localization] to set in the user-agent
     * @return the visionOS user-agent used for InnerTube requests with the visionOS client,
     * depending on the [Localization] provided
     */
    @JvmStatic
    public fun getVisionOsUserAgent(localization: Localization?): String =
        "com.google.visionos.youtube/" + VISIONOS_CLIENT_VERSION + "(" +
            VISIONOS_DEVICE_MODEL + "; U; CPU visionOS " + VISIONOS_USER_AGENT_VERSION +
            " like Mac OS X; " + (localization ?: Localization.DEFAULT).countryCode + ")"

    /**
     * Returns a [Map] containing the required YouTube Music headers.
     */
    @JvmStatic
    public fun getYoutubeMusicHeaders(): Map<String, List<String>> {
        // the client version is only known once getYoutubeMusicClientVersion() has been called
        val version = youtubeMusicClientVersion
            ?: throw NullPointerException("The YouTube Music client version is not known yet")
        val headers = HashMap(getOriginReferrerHeaders(YOUTUBE_MUSIC_URL))
        headers.putAll(getClientHeaders(WEB_REMIX_CLIENT_ID, version))
        return headers
    }

    /**
     * Returns a [Map] containing the required YouTube headers, including the
     * `CONSENT` cookie to prevent redirects to `consent.youtube.com`
     */
    @JvmStatic
    @Throws(ExtractionException::class, IOException::class)
    public fun getYouTubeHeaders(): Map<String, List<String>> {
        val headers = clientInfoHeaders()
        headers["Cookie"] = listOf(generateConsentCookie())
        return headers
    }

    /**
     * Returns a [Map] containing the `X-YouTube-Client-Name`,
     * `X-YouTube-Client-Version`, `Origin`, and `Referer` headers.
     */
    @JvmStatic
    @Throws(ExtractionException::class, IOException::class)
    public fun getClientInfoHeaders(): Map<String, List<String>> = clientInfoHeaders()

    @Throws(ExtractionException::class, IOException::class)
    private fun clientInfoHeaders(): HashMap<String, List<String>> {
        val headers = HashMap(getOriginReferrerHeaders(YOUTUBE_URL))
        headers.putAll(getClientHeaders(WEB_CLIENT_ID, getClientVersion()))
        return headers
    }

    /**
     * Returns an unmodifiable [Map] containing the `Origin` and `Referer`
     * headers set to the given URL.
     *
     * @param url The URL to be set as the origin and referrer.
     */
    @JvmStatic
    public fun getOriginReferrerHeaders(url: String): Map<String, List<String>> {
        val urlList = listOf(url)
        return mapOf("Origin" to urlList, "Referer" to urlList)
    }

    /**
     * Returns an unmodifiable [Map] containing the `X-YouTube-Client-Name` and
     * `X-YouTube-Client-Version` headers.
     *
     * @param name The X-YouTube-Client-Name value.
     * @param version X-YouTube-Client-Version value.
     */
    @JvmStatic
    public fun getClientHeaders(name: String, version: String): Map<String, List<String>> =
        mapOf(
            "X-YouTube-Client-Name" to listOf(name),
            "X-YouTube-Client-Version" to listOf(version),
        )

    /**
     * Create a map with the required cookie header.
     * @return A singleton map containing the header.
     */
    @JvmStatic
    public fun getCookieHeader(): Map<String, List<String>> =
        mapOf("Cookie" to listOf(generateConsentCookie()))

    @JvmStatic
    public fun generateConsentCookie(): String = "SOCS=" + if (isConsentAccepted()) {
        // CAISAiAD means that the user configured manually cookies YouTube, regardless of
        // the consent values
        // This value surprisingly allows to extract mixes and some YouTube Music playlists
        // in the same way when a user allows all cookies
        "CAISAiAD"
    } else {
        // CAE= means that the user rejected all non-necessary cookies with the "Reject
        // all" button on the consent page
        "CAE="
    }

    /**
     * The value of the last `Set-Cookie` header of [response] which sets [cookieName], or an
     * empty string.
     *
     * The value is sent back in a `Cookie` header: one holding anything but cookie characters
     * (a line break, a `;` adding another cookie...) is ignored.
     */
    @JvmStatic
    public fun extractCookieValue(cookieName: String, response: Response): String {
        val cookies = response.responseHeaders()["set-cookie"] ?: return ""

        var result = ""
        for (cookie in cookies) {
            // the cookie itself comes first, before its attributes (Domain=, Path=...): a name
            // found in an attribute or at the end of another cookie's name is not this cookie
            val pair = cookie.substringBefore(';').trimStart()
            if (!pair.startsWith("$cookieName=")) {
                continue
            }
            val value = pair.substring(cookieName.length + 1).trimEnd()
            if (value.length <= MAX_COOKIE_VALUE_LENGTH && value.all(::isCookieValueChar)) {
                result = value
            }
        }
        return result
    }

    /** A cookie-octet of RFC 6265: printable ASCII but space, `"`, `,`, `;` and `\`. */
    private fun isCookieValueChar(char: Char): Boolean =
        char in '!'..'~' && char != '"' && char != ',' && char != ';' && char != '\\'

    /**
     * Shared alert detection function, multiple endpoints return the error similarly structured.
     *
     * Will check if the object has an alert of the type "ERROR".
     *
     * @param initialData the object which will be checked if an alert is present
     * @throws ContentNotAvailableException if an alert is detected
     */
    @JvmStatic
    @Throws(ParsingException::class)
    public fun defaultAlertsCheck(initialData: JsonObject) {
        val alerts = initialData.getArray("alerts")
        if (alerts.isEmpty()) {
            return
        }
        val alertRenderer = alerts.getObject(0).getObject("alertRenderer")
        val alertText = getTextFromObject(alertRenderer.getObject("text"))
        val alertType = alertRenderer.getString("type", "").orEmpty()
        if (!alertType.equals("ERROR", ignoreCase = true)) {
            return
        }
        if (alertText != null &&
            (
                alertText.contains("This account has been terminated") ||
                    alertText.contains("This channel was removed")
                )
        ) {
            if (VIOLATION_PATTERN.matches(alertText) || alertText.contains("infringement")) {
                // Possible error messages:
                // "This account has been terminated for a violation of YouTube's Terms of
                //     Service."
                // "This account has been terminated due to multiple or severe violations of
                //     YouTube's policy prohibiting hate speech."
                // "This account has been terminated due to multiple or severe violations of
                //     YouTube's policy prohibiting content designed to harass, bully or
                //     threaten."
                // "This account has been terminated due to multiple or severe violations
                //     of YouTube's policy against spam, deceptive practices and misleading
                //     content or other Terms of Service violations."
                // "This account has been terminated due to multiple or severe violations of
                //     YouTube's policy on nudity or sexual content."
                // "This account has been terminated for violating YouTube's Community
                //     Guidelines."
                // "This account has been terminated because we received multiple
                //     third-party claims of copyright infringement regarding material that
                //     the user posted."
                // "This account has been terminated because it is linked to an account that
                //     received multiple third-party claims of copyright infringement."
                // "This channel was removed because it violated our Community Guidelines."
                throw AccountTerminatedException(boundedServerText(alertText), AccountTerminatedException.Reason.VIOLATION)
            } else {
                throw AccountTerminatedException(boundedServerText(alertText))
            }
        }
        throw ContentNotAvailableException("Got error: \"${alertText?.let(::boundedServerText)}\"")
    }

    /**
     * Sometimes, YouTube provides URLs which use Google's cache. They look like
     * `https://webcache.googleusercontent.com/search?q=cache:CACHED_URL`
     *
     * @param url the URL which might refer to the Google's webcache
     * @return the URL which is referring to the original site
     */
    @JvmStatic
    public fun extractCachedUrlIfNeeded(url: String?): String? {
        if (url == null) {
            return null
        }
        if (WEBCACHE_URL.containsMatchIn(url)) {
            val cachedUrlStart = url.indexOf("cache:")
            // a cache link without its "cache:" part has no other URL to give
            return if (cachedUrlStart < 0) url else url.substring(cachedUrlStart + "cache:".length)
        }
        return url
    }

    /**
     * [text] given by YouTube, made fit for an exception message: without control characters
     * and cut to [MAX_SERVER_TEXT_LENGTH] characters.
     */
    internal fun boundedServerText(text: String): String =
        text.filterNot { it.isISOControl() }.take(MAX_SERVER_TEXT_LENGTH)

    @JvmStatic
    public fun isVerified(badges: JsonArray): Boolean = badges.any { badge ->
        val style = (badge as? JsonObject)?.getObject(METADATA_BADGE_RENDERER)?.getString(STYLE)
        style == "BADGE_STYLE_TYPE_VERIFIED" || style == "BADGE_STYLE_TYPE_VERIFIED_ARTIST"
    }

    @JvmStatic
    public fun hasArtistOrVerifiedIconBadgeAttachment(attachmentRuns: JsonArray): Boolean =
        attachmentRuns.any { attachmentRun ->
            (attachmentRun as? JsonObject)?.getObject("element")
                ?.getObject("type")
                ?.getObject("imageType")
                ?.getObject("image")
                ?.getArray("sources")
                ?.any { source ->
                    val imageName = (source as? JsonObject)?.getObject("clientResource")
                        ?.getString("imageName")
                    imageName == "CHECK_CIRCLE_FILLED" || imageName == "AUDIO_BADGE" ||
                        imageName == "MUSIC_FILLED"
                } == true
        }

    /**
     * Generate a content playback nonce (also called `cpn`), sent by YouTube clients in
     * playback requests (and also for some clients, in the player request body).
     *
     * @return a content playback nonce string
     */
    @JvmStatic
    public fun generateContentPlaybackNonce(): String =
        RandomStringFromAlphabetGenerator.generate(
            CONTENT_PLAYBACK_NONCE_ALPHABET, CPN_LENGTH, numberGenerator,
        )

    /**
     * Try to generate a `t` parameter, sent by mobile clients as a query of the player
     * request.
     *
     * Some researches needs to be done to know how this parameter, unique at each request, is
     * generated.
     *
     * @return a 12 characters string to try to reproduce the `t` parameter
     */
    @JvmStatic
    public fun generateTParameter(): String =
        RandomStringFromAlphabetGenerator.generate(
            CONTENT_PLAYBACK_NONCE_ALPHABET, T_PARAMETER_LENGTH, numberGenerator,
        )

    /**
     * Check if the streaming URL is from the YouTube `WEB` client.
     *
     * @param url the streaming URL to be checked.
     * @return true if it's a `WEB` streaming URL, false otherwise
     */
    @JvmStatic
    public fun isWebStreamingUrl(url: String): Boolean = Parser.isMatch(C_WEB_PATTERN, url)

    /**
     * Check that a streaming URL received from YouTube points to its video servers, over https.
     *
     * Streaming URLs are fetched (and played) as they come: a tampered or injected response
     * must not be able to send the client to any other host, where the request would disclose
     * the user's address or deliver content of an attacker.
     *
     * @param url a streaming, manifest or SABR URL
     * @return whether the URL is an https URL of a `googlevideo.com` host, on the https
     * port, with a plain ASCII host name
     */
    @JvmStatic
    public fun isTrustedStreamingUrl(url: String?): Boolean {
        if (url.isNullOrEmpty() || url.length > Utils.MAX_URL_LENGTH) {
            return false
        }
        return try {
            val parsed = urlOf(url)
            val host = parsed.host.lowercase(Locale.ROOT)
            // the https port only: another one could reach any other service of that host
            val httpsPort = parsed.port == -1 || parsed.port == HTTPS_PORT
            parsed.protocol == "https" && parsed.userInfo == null && httpsPort &&
                HOST_NAME_PATTERN.matcher(host).matches() && host.endsWith(GOOGLE_VIDEO_HOST_SUFFIX)
        } catch (_: MalformedURLException) {
            false
        }
    }

    /**
     * Check if the streaming URL is a URL from the YouTube `VISIONOS` client.
     *
     * @param url the streaming URL on which check if it's a `VISIONOS` streaming URL.
     * @return true if it's a `VISIONOS` streaming URL, false otherwise
     */
    @JvmStatic
    public fun isVisionOsStreamingUrl(url: String): Boolean = Parser.isMatch(C_VISIONOS_PATTERN, url)

    /**
     * Determines how the consent cookie that is required for YouTube, `SOCS`, will be
     * generated.
     *
     * - `false` (the default value) will use `CAE=`;
     * - `true` will use `CAISAiAD`.
     *
     * Setting this value to `true` is needed to extract mixes and some YouTube Music
     * playlists in some countries such as the EU ones.
     */
    @JvmStatic
    public fun setConsentAccepted(accepted: Boolean) {
        consentAccepted = accepted
    }

    /**
     * Get the value of the consent's acceptance.
     *
     * @see setConsentAccepted
     * @return the consent's acceptance value
     */
    @JvmStatic
    public fun isConsentAccepted(): Boolean = consentAccepted

    /**
     * Extract the audio track type from the formats XTags.
     *
     * Example: `acont=original, lang=en`.
     *
     * @param xtags XTags of the audio track
     * @return [AudioTrackType] or `null` if no track type was found
     */
    @JvmStatic
    public fun extractAudioTrackType(xtags: String?): AudioTrackType? {
        if (xtags == null) {
            return null
        }
        val atype = try {
            XTags.parseFrom(Base64.getUrlDecoder().decode(xtags))
                .xtagsList
                .firstOrNull { it.key == "acont" }
                ?.value
        } catch (_: InvalidProtocolBufferException) {
            return null
        } catch (_: IllegalArgumentException) {
            // the tags are not valid base64
            return null
        }

        return when (atype) {
            "original" -> AudioTrackType.ORIGINAL
            "dubbed", "dubbed-auto" -> AudioTrackType.DUBBED
            "descriptive" -> AudioTrackType.DESCRIPTIVE
            "secondary" -> AudioTrackType.SECONDARY
            else -> null
        }
    }

    @JvmStatic
    @Throws(IOException::class, ExtractionException::class)
    public fun getVisitorDataFromInnertube(
        innertubeClientRequestInfo: InnertubeClientRequestInfo,
        localization: Localization,
        contentCountry: ContentCountry,
        httpHeaders: Map<String, List<String>>,
        innertubeDomainAndVersionEndpoint: String,
        embedUrl: String?,
        useGuideEndpoint: Boolean,
    ): String {
        val builder = prepareJsonBuilder(
            localization, contentCountry, innertubeClientRequestInfo, embedUrl,
        )

        val body = JsonWriter.string(builder.done()).toByteArray(StandardCharsets.UTF_8)

        val visitorData = JsonUtils.toJsonObject(
            getValidJsonResponseBody(
                downloader().postWithContentTypeJson(
                    innertubeDomainAndVersionEndpoint +
                        (if (useGuideEndpoint) "guide" else "visitor_id") + "?" +
                        DISABLE_PRETTY_PRINT_PARAMETER,
                    httpHeaders,
                    body,
                ),
            ),
        )
            .getObject("responseContext")
            .getString("visitorData")

        // sent back as it is in the next request bodies: a malformed value is not used
        if (visitorData.isNullOrEmpty() || !VISITOR_DATA_PATTERN.matches(visitorData)) {
            throw ParsingException("Could not get visitorData")
        }

        return visitorData
    }

    @JvmStatic
    public fun prepareJsonBuilder(
        localization: Localization,
        contentCountry: ContentCountry,
        innertubeClientRequestInfo: InnertubeClientRequestInfo,
        embedUrl: String?,
    ): JsonBuilder<JsonObject> {
        val clientInfo = innertubeClientRequestInfo.clientInfo
        val deviceInfo = innertubeClientRequestInfo.deviceInfo
        val builder = JsonObject.builder()
            .`object`("context")
            .`object`("client")
            .value("clientName", clientInfo.clientName)
            .value("clientVersion", clientInfo.clientVersion)

        clientInfo.clientScreen?.let { builder.value("clientScreen", it) }
        deviceInfo.platform?.let { builder.value("platform", it) }
        clientInfo.visitorData?.let { builder.value("visitorData", it) }
        deviceInfo.deviceMake?.let { builder.value("deviceMake", it) }
        deviceInfo.deviceModel?.let { builder.value("deviceModel", it) }
        deviceInfo.osName?.let { builder.value("osName", it) }
        deviceInfo.osVersion?.let { builder.value("osVersion", it) }
        if (deviceInfo.androidSdkVersion > 0) {
            builder.value("androidSdkVersion", deviceInfo.androidSdkVersion)
        }

        builder.value("hl", localization.localizationCode)
            .value("gl", contentCountry.countryCode)
            .value("utcOffsetMinutes", 0)
            .end()

        if (embedUrl != null) {
            builder.`object`("thirdParty")
                .value("embedUrl", embedUrl)
                .end()
        }

        builder.`object`("request")
            .array("internalExperimentFlags")
            .end()
            .value("useSsl", true)
            .end()
            .`object`("user")
            // TODO: provide a way to enable restricted mode with:
            //  .value("enableSafetyMode", boolean)
            .value("lockedSafetyMode", false)
            .end()
            .end()

        return builder
    }

    /**
     * Gets the first collaborator, which is the channel that owns the video,
     * i.e. the video is displayed on their channel page.
     *
     * @param navigationEndpoint JSON object for the navigationEndpoint
     * @return The first collaborator in the JSON object or `null`
     */
    @JvmStatic
    @Throws(ParsingException::class)
    public fun getFirstCollaborator(navigationEndpoint: JsonObject): JsonObject? = try {
        val listItems = JsonUtils.getArray(
            navigationEndpoint,
            "showDialogCommand.panelLoadingStrategy.inlineContent.dialogViewModel" +
                ".customContent.listViewModel.listItems",
        )
        listItems.getObject(0).getObject("listItemViewModel")
    } catch (_: ParsingException) {
        null
    }
}

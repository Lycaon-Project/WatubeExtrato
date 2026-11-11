package org.schabi.newpipe.extractor.services.youtube

import org.jsoup.Jsoup
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.utils.JavaScript
import org.schabi.newpipe.extractor.utils.Parser
import java.net.MalformedURLException
import java.net.URL
import java.util.regex.Pattern

/**
 * The extractor of YouTube's base JavaScript player file.
 *
 * This class handles fetching of this base JavaScript player file in order to allow other classes
 * to extract the needed data.
 *
 * It will try to get the player URL from YouTube's IFrame resource first, and from a YouTube embed
 * watch page as a fallback.
 */
internal object YoutubeJavaScriptExtractor {

    private const val HTTPS = "https:"

    // the player code gets executed: it is only accepted from this host
    private const val JAVASCRIPT_PLAYER_HOST = "www.youtube.com"
    private const val HTTPS_PORT = 443
    private const val BASE_JS_PLAYER_URL_PREFIX = "https://www.youtube.com/s/player/"
    private const val BASE_JS_PLAYER_URL_SUFFIX = "/player_ias.vflset/en_GB/base.js"
    private const val IFRAME_URL = "https://www.youtube.com/iframe_api"
    private const val EMBED_URL_PREFIX = "https://www.youtube.com/embed/"
    private val IFRAME_RES_JS_BASE_PLAYER_HASH_PATTERN: Pattern =
        Pattern.compile("player\\\\/([a-z0-9]{8})\\\\/")
    private val EMBEDDED_WATCH_PAGE_JS_BASE_PLAYER_URL_PATTERN: Pattern = Pattern.compile(
        "\"jsUrl\":\"(/s/player/[A-Za-z0-9]+/player_ias\\.vflset/[A-Za-z_-]+/base\\.js)\"",
    )

    /**
     * The only path the player is ever downloaded from: its code is parsed by regexes and partly
     * run, so it must be YouTube's player and nothing else served by www.youtube.com.
     */
    private val PLAYER_PATH = Regex("""/s/player/[A-Za-z0-9]{1,32}/player_ias\.vflset/[A-Za-z_-]{1,16}/base\.js""")

    /**
     * The video ID is placed in the embed page URL: an empty one is allowed (see
     * [extractJavaScriptPlayerCode]), anything else must only be made of URL-safe characters.
     */
    private val VIDEO_ID = Regex("[a-zA-Z0-9_-]{0,64}")

    /**
     * Extracts the JavaScript base player file.
     *
     * @param videoId the video ID used to get the JavaScript base player file (an empty one can be
     *                passed, even it is not recommend in order to spoof better official YouTube
     *                clients)
     * @return the whole JavaScript base player file as a string
     * @throws ParsingException if the extraction of the file failed
     */
    @JvmStatic
    @Throws(ParsingException::class)
    fun extractJavaScriptPlayerCode(videoId: String): String = try {
        downloadJavaScriptCode(cleanJavaScriptUrl(extractJavaScriptUrlWithIframeResource()))
    } catch (_: Exception) {
        downloadJavaScriptCode(
            cleanJavaScriptUrl(extractJavaScriptUrlWithEmbedWatchPage(videoId)),
        )
    }

    @JvmStatic
    @Throws(ParsingException::class)
    fun extractJavaScriptUrlWithIframeResource(): String {
        val iframeContent = try {
            downloader().get(IFRAME_URL, Localization.DEFAULT).responseBody()
        } catch (e: Exception) {
            throw ParsingException("Could not fetch IFrame resource", e)
        }

        try {
            val hash = Parser.matchGroup1(IFRAME_RES_JS_BASE_PLAYER_HASH_PATTERN, iframeContent)
            return BASE_JS_PLAYER_URL_PREFIX + hash + BASE_JS_PLAYER_URL_SUFFIX
        } catch (e: Parser.RegexException) {
            throw ParsingException("IFrame resource didn't provide JavaScript base player's hash", e)
        }
    }

    @JvmStatic
    @Throws(ParsingException::class)
    fun extractJavaScriptUrlWithEmbedWatchPage(videoId: String): String {
        if (!VIDEO_ID.matches(videoId)) {
            throw ParsingException("Invalid video ID")
        }
        val embedPageContent = try {
            downloader().get(EMBED_URL_PREFIX + videoId, Localization.DEFAULT)
                .responseBody()
        } catch (e: Exception) {
            throw ParsingException("Could not fetch embedded watch page", e)
        }

        // Parse HTML response with jsoup and look at script elements first
        val doc = Jsoup.parse(embedPageContent)
        for (elem in doc.select("script[src*=base.js]")) {
            // Script URLs should be relative and not absolute
            return elem.attr("src")
        }

        // Use regexes to match the URL in an embedded script of the HTML page
        try {
            return Parser.matchGroup1(
                EMBEDDED_WATCH_PAGE_JS_BASE_PLAYER_URL_PATTERN, embedPageContent,
            )
        } catch (e: Parser.RegexException) {
            throw ParsingException(
                "Embedded watch page didn't provide JavaScript base player's URL", e,
            )
        }
    }

    // URL(String) is deprecated on Java 20+, but URI parsing is stricter: keep java.net.URL
    @Suppress("DEPRECATION")
    @Throws(MalformedURLException::class)
    private fun urlOf(spec: String?): URL = URL(spec)

    private fun downloader(): Downloader =
        NewPipe.getDownloader() ?: throw NullPointerException("downloader is null")

    private fun cleanJavaScriptUrl(javaScriptPlayerUrl: String): String = when {
        // https part has to be added manually if the URL is protocol-relative
        javaScriptPlayerUrl.startsWith("//") -> HTTPS + javaScriptPlayerUrl

        // https://www.youtube.com part has to be added manually if the URL is relative to
        // YouTube's domain
        javaScriptPlayerUrl.startsWith("/") -> "$HTTPS//www.youtube.com$javaScriptPlayerUrl"

        else -> javaScriptPlayerUrl
    }

    @Throws(ParsingException::class)
    private fun downloadJavaScriptCode(javaScriptPlayerUrl: String): String {
        checkJavaScriptPlayerUrl(javaScriptPlayerUrl)
        val response: Response = try {
            downloader().get(javaScriptPlayerUrl, Localization.DEFAULT)
        } catch (e: Exception) {
            throw ParsingException("Could not get JavaScript base player's code", e)
        }
        // the downloader may have followed redirects
        val latestUrl = response.latestUrl()
        if (!latestUrl.isNullOrEmpty()) {
            checkJavaScriptPlayerUrl(latestUrl)
        }
        val code = response.responseBody()
        // parsed by regexes and partly run by Rhino: an oversized answer is refused right away
        if (code.length > JavaScript.MAX_SOURCE_LENGTH) {
            throw ParsingException("JavaScript base player's code is too large")
        }
        return code
    }

    @Throws(ParsingException::class)
    private fun checkJavaScriptPlayerUrl(javaScriptPlayerUrl: String) {
        val url = try {
            urlOf(javaScriptPlayerUrl)
        } catch (_: MalformedURLException) {
            // not chained: its message quotes the URL
            throw ParsingException("The JavaScript base player's URL is invalid")
        }
        if (url.protocol != "https" || url.host != JAVASCRIPT_PLAYER_HOST || url.userInfo != null ||
            (url.port != -1 && url.port != HTTPS_PORT)
        ) {
            throw ParsingException("Refusing JavaScript base player's code from an untrusted host")
        }
        if (url.query != null || url.ref != null || !PLAYER_PATH.matches(url.path)) {
            throw ParsingException("Refusing JavaScript base player's code from an untrusted path")
        }
    }
}

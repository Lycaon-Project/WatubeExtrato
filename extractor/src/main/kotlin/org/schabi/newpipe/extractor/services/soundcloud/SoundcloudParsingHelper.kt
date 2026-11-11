package org.schabi.newpipe.extractor.services.soundcloud

import org.jsoup.Jsoup
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.Image.ResolutionLevel.LOW
import org.schabi.newpipe.extractor.Image.ResolutionLevel.MEDIUM
import org.schabi.newpipe.extractor.MultiInfoItemsCollector
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList.SoundCloud
import org.schabi.newpipe.extractor.channel.ChannelInfoItemsCollector
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.HttpResponseException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonParser
import org.schabi.newpipe.extractor.json.JsonParserException
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.services.soundcloud.extractors.SoundcloudChannelInfoItemExtractor
import org.schabi.newpipe.extractor.services.soundcloud.extractors.SoundcloudLikesInfoItemExtractor
import org.schabi.newpipe.extractor.services.soundcloud.extractors.SoundcloudPlaylistInfoItemExtractor
import org.schabi.newpipe.extractor.services.soundcloud.extractors.SoundcloudStreamInfoItemExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector
import org.schabi.newpipe.extractor.utils.ImageSuffix
import org.schabi.newpipe.extractor.utils.JsonUtils
import org.schabi.newpipe.extractor.utils.Utils
import java.io.IOException
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import java.net.MalformedURLException
import java.net.URL
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Collections
import java.util.Locale
import java.util.concurrent.TimeUnit

public object SoundcloudParsingHelper {
    // From https://web.archive.org/web/20210214185000/https://developers.soundcloud.com/docs/api/reference#tracks
    // and researches on images used by the websites
    /*
    SoundCloud avatars and artworks are almost always squares.

    When we get non-square pictures, all these images variants are still squares, except the
    original and the crop versions provides images which are respecting aspect ratios.
    The websites only use the square variants.

    t2400x2400 and t3000x3000 variants also exists, but are not returned as several images are
    uploaded with a lower size than these variants: in this case, these variants return an upscaled
    version of the original image.
    */
    private val ALBUMS_AND_ARTWORKS_IMAGE_SUFFIXES = listOf(
        ImageSuffix("mini", 16, 16, LOW),
        ImageSuffix("t20x20", 20, 20, LOW),
        ImageSuffix("small", 32, 32, LOW),
        ImageSuffix("badge", 47, 47, LOW),
        ImageSuffix("t50x50", 50, 50, LOW),
        ImageSuffix("t60x60", 60, 60, LOW),
        // Seems to work also on avatars, even if it is written to be not the case in
        // the old API docs
        ImageSuffix("t67x67", 67, 67, LOW),
        ImageSuffix("t80x80", 80, 80, LOW),
        ImageSuffix("large", 100, 100, LOW),
        ImageSuffix("t120x120", 120, 120, LOW),
        ImageSuffix("t200x200", 200, 200, MEDIUM),
        ImageSuffix("t240x240", 240, 240, MEDIUM),
        ImageSuffix("t250x250", 250, 250, MEDIUM),
        ImageSuffix("t300x300", 300, 300, MEDIUM),
        ImageSuffix("t500x500", 500, 500, MEDIUM),
    )

    private val VISUALS_IMAGE_SUFFIXES = listOf(
        ImageSuffix("t1240x260", 1240, 260, MEDIUM),
        ImageSuffix("t2480x520", 2480, 520, MEDIUM),
    )

    /** Scraped from the website, then reused until the API refuses it; read without a lock. */
    @Volatile
    private var cachedClientId: String? = null

    /** When the last scrape started, and whether it failed: written under the lock of [scrapeClientId]. */
    @Volatile
    private var clientIdScrapedAt = 0L
    private var clientIdFailed = false

    /** A scrape costs up to 17 requests: a failed one, or a fresh ID, is kept for a minute. */
    private val CLIENT_ID_RETRY_DELAY_NANOS = TimeUnit.MINUTES.toNanos(1)

    public const val SOUNDCLOUD_API_V2_URL: String = "https://api-v2.soundcloud.com/"

    private val ON_URL_PATTERN = Regex("^https?://on\\.soundcloud\\.com/[0-9a-zA-Z]+$")

    /**
     * The client ID as written in the scripts of the website. Only a plain token is accepted
     * (letters, digits, `_` and `-`, bounded length): whatever else follows `client_id:` is never
     * placed in a request URL.
     */
    private val CLIENT_ID_PATTERN = Regex(",client_id:\"([A-Za-z0-9_-]{16,64})\"")

    /** Scripts tried at most when looking for the client ID, one request each. */
    private const val MAX_CLIENT_ID_SCRIPTS = 16

    /** Pages followed at most by [getUsersFromApiMinItems], whatever the API answers. */
    private const val MAX_USER_PAGES = 100

    private const val HTTPS = "https://"
    private const val ASCII_LIMIT = 0x80
    private const val SOUNDCLOUD_DOMAIN = "soundcloud.com"

    /** The only hosts the client ID is sent to. */
    private val API_HOSTS = setOf("api-v2.soundcloud.com", "api-widget.soundcloud.com")
    private const val ASSETS_DOMAIN = "sndcdn.com"
    private val NUMERIC_ID = Regex("\\d{1,20}")

    /** Thread-safe and immutable: built once instead of on every date. */
    private val LEGACY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss +0000")

    @JvmStatic
    @Throws(ExtractionException::class, IOException::class)
    public fun clientId(): String = cachedClientId ?: scrapeClientId()

    /**
     * **Only used in tests.** The mocked requests were all recorded with one client ID: it is set
     * here so that each test class finds it, whatever the order the classes run in (a scrape
     * needs mocks that only a few classes hold, and a failed one is kept for a minute).
     */
    // same lock as scrapeClientId: no @JvmStatic, which would lock the class instead
    @Synchronized
    internal fun setClientIdForTests(clientId: String) {
        cachedClientId = clientId
        clientIdFailed = false
        clientIdScrapedAt = System.nanoTime()
    }

    /**
     * The API refused the client ID (401 or 403): SoundCloud rotated it, so the next request
     * scrapes a new one. An ID scraped less than a minute ago is kept: a 403 also answers a
     * track blocked in the country, which must not make every request scrape the website.
     */
    @JvmStatic
    internal fun noteRefusedClientId(response: Response) {
        if (isClientIdRefusal(response.responseCode(), System.nanoTime() - clientIdScrapedAt)) {
            cachedClientId = null
        }
    }

    /** Whether a response [code], [scrapedAgoNanos] after the ID was scraped, means it was rotated. */
    internal fun isClientIdRefusal(code: Int, scrapedAgoNanos: Long): Boolean =
        (code == HTTP_UNAUTHORIZED || code == HTTP_FORBIDDEN) && scrapedAgoNanos > CLIENT_ID_RETRY_DELAY_NANOS

    @Synchronized
    @Throws(ExtractionException::class, IOException::class)
    private fun scrapeClientId(): String {
        // another thread may have scraped it while this one waited for the lock
        cachedClientId?.let { return it }
        if (clientIdFailed && System.nanoTime() - clientIdScrapedAt < CLIENT_ID_RETRY_DELAY_NANOS) {
            throw ExtractionException("Couldn't extract client id")
        }
        clientIdScrapedAt = System.nanoTime()
        clientIdFailed = true
        val found = findClientId()
        cachedClientId = found
        clientIdFailed = false
        return found
    }

    @Throws(ExtractionException::class, IOException::class)
    private fun findClientId(): String {
        val dl = requireDownloader()

        val responseBody = dl.get("https://soundcloud.com")
            .ensureSuccessResponseCode()
            .responseBody()

        // The one containing the client id will likely be the last one
        val possibleScripts = Jsoup.parse(responseBody)
            .select("script[src*=\"sndcdn.com/assets/\"][src$=\".js\"]")
            .asReversed()
            .asSequence()
            .map { it.attr("src") }
            // only scripts of SoundCloud's own asset host are ever requested
            .filter { isHttpsUrlUnder(it, ASSETS_DOMAIN) }
            .take(MAX_CLIENT_ID_SCRIPTS)

        val headers = mapOf("Range" to listOf("bytes=0-50000"))

        for (srcUrl in possibleScripts) {
            val script = dl.get(srcUrl, headers)
                .ensureSuccessResponseCode()
                .responseBody()
            // Not found: proceed to try searching other script
            return CLIENT_ID_PATTERN.find(script)?.groupValues?.get(1) ?: continue
        }

        // Officially give up
        throw ExtractionException("Couldn't extract client id")
    }

    @JvmStatic
    @Throws(ParsingException::class)
    public fun parseDate(uploadDate: String?): DateWrapper? = try {
        DateWrapper.fromInstant(uploadDate)
    } catch (_: ParsingException) {
        // uploadDate is not null here: fromInstant returns null for null
        try {
            DateWrapper(OffsetDateTime.parse(uploadDate, LEGACY_DATE_FORMAT))
        } catch (_: DateTimeParseException) {
            // the parser error quotes the date read from the network: it is not kept
            throw ParsingException("Could not parse date")
        }
    }

    /**
     * Call the endpoint "/resolve" of the API.
     *
     * See [docs](https://web.archive.org/web/20170804051146/https://developers.soundcloud.com/docs/api/reference#resolve)
     */
    @JvmStatic
    @Throws(IOException::class, ExtractionException::class)
    public fun resolveFor(downloader: Downloader, url: String): JsonObject {
        val apiUrl = SOUNDCLOUD_API_V2_URL + "resolve" +
            "?url=" + Utils.encodeUrlUtf8(url) +
            "&client_id=" + clientId()

        val response = downloader.get(apiUrl, SoundCloud.localization)
            .also(SoundcloudParsingHelper::noteRefusedClientId)
            .ensureSuccessResponseCode()
            .responseBody()
        return parseObject(response)
    }

    /**
     * Fetch the embed player with the apiUrl and return the canonical url (like the permalink_url
     * from the json API).
     *
     * The canonical link is only returned when it is a https link to SoundCloud; otherwise (or
     * when the page has none, where the Java code failed with a NullPointerException) a
     * [ParsingException] is thrown.
     *
     * @return the url resolved
     */
    @JvmStatic
    @Throws(
        IOException::class,
        ReCaptchaException::class,
        HttpResponseException::class,
        ParsingException::class,
    )
    public fun resolveUrlWithEmbedPlayer(apiUrl: String?): String {
        val response = requireDownloader().get(
            "https://w.soundcloud.com/player/?url=" + Utils.encodeUrlUtf8(apiUrl.orEmpty()),
            SoundCloud.localization,
        )
        response.ensureSuccessResponseCode()
        val canonicalUrl = Jsoup.parse(response.responseBody())
            .select("link[rel=\"canonical\"]")
            .firstOrNull()
            ?.attr("abs:href")
        return canonicalUrl?.takeIf { isHttpsUrlUnder(it, SOUNDCLOUD_DOMAIN) }
            ?: throw ParsingException("Could not resolve the URL with the embed player")
    }

    /**
     * Fetch the widget API with the url and return the id (like the id from the json API).
     *
     * @return the resolved id, which is always a number
     */
    @JvmStatic
    @Throws(IOException::class, ParsingException::class)
    public fun resolveIdWithWidgetApi(urlString: String?): String {
        var fixedUrl = requireNotNull(urlString) { "The given URL is not valid" }

        // if URL is an on.soundcloud link, do a request to resolve the redirect
        if (ON_URL_PATTERN.containsMatchIn(fixedUrl)) {
            fixedUrl = followOnSoundcloudRedirect(fixedUrl)
        }

        // The query and the fragment are not sent: a share link carries the sharer's identifier
        // (si=) and campaign parameters there
        fixedUrl = fixedUrl.substringBefore('#').substringBefore('?')
        // Remove the tailing slash from URLs due to issues with the SoundCloud API
        fixedUrl = fixedUrl.removeSuffix("/")
        // Make URL lower case and remove m. and www. if it exists.
        // Without doing this, the widget API does not recognize the URL.
        fixedUrl = Utils.removeMAndWWWFromUrl(fixedUrl.lowercase(Locale.ROOT))

        val url: URL = try {
            Utils.stringToURL(fixedUrl)
        } catch (_: MalformedURLException) {
            throw IllegalArgumentException("The given URL is not valid")
        }

        try {
            val widgetUrl = "https://api-widget.soundcloud.com/resolve?url=" +
                Utils.encodeUrlUtf8(url.toString()) +
                "&format=json&client_id=" + clientId()

            val responseBody = requireDownloader()
                .get(widgetUrl, SoundCloud.localization)
                .also(SoundcloudParsingHelper::noteRefusedClientId)
                .ensureSuccessResponseCode()
                .responseBody()

            val o = JsonParser.`object`().from(responseBody)
            // placed in API URLs afterwards: only a number is accepted
            return JsonUtils.getValue(o, "id").toString().takeIf { NUMERIC_ID.matches(it) }
                ?: throw ParsingException("Could not get a valid ID")
        } catch (e: JsonParserException) {
            throw ParsingException("Could not parse JSON response", e)
        } catch (e: ExtractionException) {
            throw ParsingException(
                "Could not resolve id with embedded player. ClientId not extracted",
                e,
            )
        }
    }

    /**
     * Follows an on.soundcloud.com short link. Only a redirection to SoundCloud itself is used,
     * without its query string, which holds tracking parameters.
     */
    @Throws(IOException::class, ParsingException::class)
    private fun followOnSoundcloudRedirect(shortUrl: String): String {
        val redirectedUrl = try {
            // Soundcloud returns HTTP 403 but it still redirects,
            // so can get the redirected url via latestUrl regardless.
            // This why only throw for 5xx and not 4xx.
            requireDownloader()
                .head(shortUrl)
                .throwIfServerError()
                .latestUrl()
        } catch (e: ExtractionException) {
            throw ParsingException("Could not follow on.soundcloud.com redirect", e)
        }

        // remove tracking params which are in the query string
        val withoutQuery = redirectedUrl.substringBefore('?')
        if (!isHttpUrlUnder(withoutQuery, SOUNDCLOUD_DOMAIN)) {
            throw ParsingException("Could not follow on.soundcloud.com redirect")
        }
        return withoutQuery
    }

    /**
     * Fetch the users from the given API and commit each of them to the collector.
     *
     * This differs from [getUsersFromApi] in the sense that they will always get MIN_ITEMS or
     * more, unless the API has no more pages. At most [MAX_USER_PAGES] pages are followed, and
     * a page already seen stops the loop, so that a faulty answer cannot loop forever.
     *
     * @param minItems the method will return only when it has extracted this many items
     * (equal or more)
     */
    @JvmStatic
    @Throws(
        IOException::class,
        ReCaptchaException::class,
        ParsingException::class,
        HttpResponseException::class,
    )
    public fun getUsersFromApiMinItems(
        minItems: Int,
        collector: ChannelInfoItemsCollector,
        apiUrl: String,
    ): String {
        val visitedPages = HashSet<String>()
        visitedPages.add(apiUrl)
        var nextPageUrl = getUsersFromApi(collector, apiUrl)
        var pageCount = 1

        while (nextPageUrl.isNotEmpty() && collector.items.size < minItems) {
            if (pageCount >= MAX_USER_PAGES || !visitedPages.add(nextPageUrl)) {
                return ""
            }
            nextPageUrl = getUsersFromApi(collector, nextPageUrl)
            pageCount++
        }

        return nextPageUrl
    }

    /**
     * Fetch the user items from the given API and commit each of them to the collector.
     *
     * @return the next stream's url, empty if don't have
     */
    @JvmStatic
    @Throws(
        IOException::class,
        ReCaptchaException::class,
        ParsingException::class,
        HttpResponseException::class,
    )
    public fun getUsersFromApi(collector: ChannelInfoItemsCollector, apiUrl: String): String {
        val response = requireDownloader()
            .get(requireApiUrl(apiUrl), SoundCloud.localization)
            .also(SoundcloudParsingHelper::noteRefusedClientId)
            .ensureSuccessResponseCode()
            .responseBody()
        val responseObject = parseObject(response)

        for (o in responseObject.getArray("collection")) {
            if (o is JsonObject) {
                collector.commit(SoundcloudChannelInfoItemExtractor(o))
            }
        }

        return getNextPageUrl(responseObject)
    }

    /**
     * Fetch the streams from the given API and commit each of them to the collector.
     *
     * @return the next streams url, empty if don't have
     */
    @JvmStatic
    @Throws(
        IOException::class,
        ReCaptchaException::class,
        ParsingException::class,
        HttpResponseException::class,
    )
    public fun getStreamsFromApi(
        collector: StreamInfoItemsCollector,
        apiUrl: String,
        charts: Boolean,
    ): String {
        val responseBody = requireDownloader()
            .get(requireApiUrl(apiUrl), SoundCloud.localization)
            .also(SoundcloudParsingHelper::noteRefusedClientId)
            .ensureResponseCodeIsNotError()
            .responseBody()
        val responseObject = parseObject(responseBody)

        for (o in responseObject.getArray("collection")) {
            if (o is JsonObject) {
                collector.commit(SoundcloudStreamInfoItemExtractor(if (charts) o.getObject("track") else o))
            }
        }

        return getNextPageUrl(responseObject)
    }

    /**
     * The next page given by the API (`next_href`), with the client ID added when missing, or an
     * empty string when there is none. A next page that is not a https link to the SoundCloud API
     * is ignored: the client ID is never sent anywhere else.
     */
    private fun getNextPageUrl(response: JsonObject): String {
        val nextHref = response.getString("next_href") ?: return ""
        if (!isApiUrl(nextHref)) {
            return ""
        }
        if (nextHref.contains("client_id=")) {
            return nextHref
        }
        return try {
            nextHref + "&client_id=" + clientId()
        } catch (_: ExtractionException) {
            ""
        } catch (_: IOException) {
            ""
        }
    }

    @JvmStatic
    @Throws(
        ReCaptchaException::class,
        ParsingException::class,
        IOException::class,
        HttpResponseException::class,
    )
    public fun getStreamsFromApi(collector: StreamInfoItemsCollector, apiUrl: String): String =
        getStreamsFromApi(collector, apiUrl, false)

    @JvmStatic
    @Throws(
        ReCaptchaException::class,
        ParsingException::class,
        IOException::class,
        HttpResponseException::class,
    )
    public fun getInfoItemsFromApi(collector: MultiInfoItemsCollector, apiUrl: String): String {
        val responseBody = requireDownloader()
            .get(requireApiUrl(apiUrl), SoundCloud.localization)
            .also(SoundcloudParsingHelper::noteRefusedClientId)
            .ensureResponseCodeIsNotError()
            .responseBody()
        val responseObject = parseObject(responseBody)

        for (searchResult in responseObject.getArray("collection")) {
            if (searchResult !is JsonObject) {
                continue
            }
            when (searchResult.getString("kind", "")) {
                "user" -> collector.commit(SoundcloudChannelInfoItemExtractor(searchResult))
                "track" -> collector.commit(SoundcloudStreamInfoItemExtractor(searchResult))
                "playlist" -> collector.commit(SoundcloudPlaylistInfoItemExtractor(searchResult))
                "like" -> {
                    // Soundcloud users can like tracks or playlists and all end up in the
                    // `Likes` feed, so they should be handled by the correct extractor.
                    val likedPlaylist = searchResult.getObject("playlist", null)
                    collector.commit(
                        if (likedPlaylist == null) {
                            SoundcloudLikesInfoItemExtractor(searchResult)
                        } else {
                            SoundcloudPlaylistInfoItemExtractor(likedPlaylist)
                        },
                    )
                }
            }
        }

        return getNextPageUrl(responseObject)
    }

    /**
     * @return the https link to the uploader on SoundCloud, or an empty string when it is missing
     * or is not a SoundCloud link
     */
    @JvmStatic
    public fun getUploaderUrl(`object`: JsonObject): String =
        soundcloudWebUrlOrNull(`object`.getObject("user").getString("permalink_url")).orEmpty()

    /** @return the https link of the uploader avatar, or an empty string when it is not a web link */
    @JvmStatic
    public fun getAvatarUrl(`object`: JsonObject): String =
        secureWebUrlOrNull(`object`.getObject("user").getString("avatar_url")).orEmpty()

    @JvmStatic
    public fun getUploaderName(`object`: JsonObject): String =
        `object`.getObject("user").getString("username", "").orEmpty()

    @JvmStatic
    @Throws(ParsingException::class)
    public fun getAllImagesFromTrackObject(trackObject: JsonObject): List<Image> {
        trackObject.getString("artwork_url")?.let { return getAllImagesFromArtworkOrAvatarUrl(it) }
        trackObject.getObject("user").getString("avatar_url")?.let {
            return getAllImagesFromArtworkOrAvatarUrl(it)
        }
        throw ParsingException("Could not get track or track user's thumbnails")
    }

    /**
     * @return the images of every size of an artwork or avatar, or an empty list when the URL is
     * missing or is not a web link (an http link is upgraded to https)
     */
    @JvmStatic
    public fun getAllImagesFromArtworkOrAvatarUrl(originalArtworkOrAvatarUrl: String?): List<Image> =
        // Artwork and avatars are originally returned with the "large" resolution, which
        // is 100px wide
        getAllImagesFromImageUrlReturned(
            originalArtworkOrAvatarUrl,
            "-large.",
            ALBUMS_AND_ARTWORKS_IMAGE_SUFFIXES,
        )

    /**
     * @return the images of every size of a visual (banner), or an empty list when the URL is
     * missing or is not a web link (an http link is upgraded to https)
     */
    @JvmStatic
    public fun getAllImagesFromVisualUrl(originalVisualUrl: String?): List<Image> =
        // Images are originally returned with the "original" resolution, which may be
        // huge so don't include it for size purposes
        getAllImagesFromImageUrlReturned(originalVisualUrl, "-original.", VISUALS_IMAGE_SUFFIXES)

    /**
     * Builds one image per suffix by replacing [sizeMarker] in the URL. The Java code turned the
     * URL into a format string for [String.format], which failed on (or misread) any `%` of a
     * URL from the network; the suffix is now inserted as plain text.
     */
    private fun getAllImagesFromImageUrlReturned(
        imageUrl: String?,
        sizeMarker: String,
        imageSuffixes: List<ImageSuffix>,
    ): List<Image> {
        val secureUrl = secureWebUrlOrNull(imageUrl) ?: return emptyList()
        val images = ArrayList<Image>(imageSuffixes.size)
        for (imageSuffix in imageSuffixes) {
            images.add(
                Image(
                    secureUrl.replace(sizeMarker, "-" + imageSuffix.suffix + "."),
                    imageSuffix.height,
                    imageSuffix.width,
                    imageSuffix.resolutionLevel,
                ),
            )
        }
        return Collections.unmodifiableList(images)
    }

    // ////////////////////////////////////////////////////////////////////////
    // Checks of untrusted links (shared links, pages and API responses)
    // ////////////////////////////////////////////////////////////////////////

    /** The downloader, as the Java code dereferenced it directly. */
    internal fun requireDownloader(): Downloader =
        NewPipe.getDownloader() ?: throw NullPointerException("downloader is null")

    @Throws(ParsingException::class)
    internal fun parseObject(response: String): JsonObject = try {
        JsonParser.`object`().from(response)
    } catch (e: JsonParserException) {
        throw ParsingException("Could not parse json response", e)
    }

    /**
     * Parses [url] as an http(s) link on the default port with a plain ASCII host (which rules
     * out look-alike hosts), or returns `null`.
     */
    private fun parseHttpUrlOrNull(url: String?): URL? {
        if (url.isNullOrEmpty() || url.length > Utils.MAX_URL_LENGTH ||
            url.any { it == '\\' || it < ' ' }
        ) {
            return null
        }
        val parsed = try {
            Utils.stringToURL(url)
        } catch (_: MalformedURLException) {
            return null
        }
        val host = parsed.host
        val valid = Utils.isHTTP(parsed) && host.isNotEmpty() &&
            host.all { it.code < ASCII_LIMIT }
        return parsed.takeIf { valid }
    }

    private fun isUnder(host: String, domain: String): Boolean {
        val name = host.lowercase(Locale.ROOT).trimEnd('.')
        return name == domain || name.endsWith(".$domain")
    }

    /** Whether [url] is an http(s) link to [domain] or one of its subdomains. */
    internal fun isHttpUrlUnder(url: String?, domain: String): Boolean =
        parseHttpUrlOrNull(url)?.let { isUnder(it.host, domain) } == true

    /** Whether [url] is a https link to [domain] or one of its subdomains. */
    internal fun isHttpsUrlUnder(url: String?, domain: String): Boolean =
        parseHttpUrlOrNull(url)?.let { it.protocol == "https" && isUnder(it.host, domain) } == true

    /** Whether [url] is a https link to SoundCloud, where the client ID may be sent. */
    internal fun isApiUrl(url: String?): Boolean =
        parseHttpUrlOrNull(url)?.let { it.protocol == "https" && it.host.lowercase(Locale.ROOT) in API_HOSTS } == true

    /** [url] when it is a https link to SoundCloud: page URLs are never requested elsewhere. */
    @Throws(ParsingException::class)
    internal fun requireApiUrl(url: String?): String =
        url?.takeIf { isApiUrl(it) } ?: throw ParsingException("Invalid SoundCloud API URL")

    /** [url] upgraded to https when it is a web link with a plain ASCII host, otherwise `null`. */
    internal fun secureWebUrlOrNull(url: String?): String? {
        val secureUrl = Utils.replaceHttpWithHttps(url) ?: return null
        val parsed = parseHttpUrlOrNull(secureUrl) ?: return null
        return secureUrl.takeIf { parsed.protocol == "https" && secureUrl.startsWith(HTTPS) }
    }

    /** [url] upgraded to https when it is a link to SoundCloud, otherwise `null`. */
    internal fun soundcloudWebUrlOrNull(url: String?): String? =
        secureWebUrlOrNull(url)?.takeIf { isHttpsUrlUnder(it, SOUNDCLOUD_DOMAIN) }

    /** [url] upgraded to https when it is a link to SoundCloud (permalinks of items). */
    @Throws(ParsingException::class)
    internal fun requireSoundcloudWebUrl(url: String?): String =
        soundcloudWebUrlOrNull(url) ?: throw ParsingException("Invalid SoundCloud link")

    /** [id] when it is a numeric SoundCloud ID, safe to place in an API URL. */
    @Throws(ParsingException::class)
    internal fun requireNumericId(id: String?): String =
        id?.takeIf { NUMERIC_ID.matches(it) } ?: throw ParsingException("Invalid SoundCloud ID")
}

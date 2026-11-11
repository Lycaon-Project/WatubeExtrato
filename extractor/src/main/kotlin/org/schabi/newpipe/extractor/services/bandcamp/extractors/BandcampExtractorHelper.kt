// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

package org.schabi.newpipe.extractor.services.bandcamp.extractors

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.Image.Companion.HEIGHT_UNKNOWN
import org.schabi.newpipe.extractor.Image.Companion.WIDTH_UNKNOWN
import org.schabi.newpipe.extractor.Image.ResolutionLevel
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonParser
import org.schabi.newpipe.extractor.json.JsonParserException
import org.schabi.newpipe.extractor.json.JsonWriter
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.utils.ImageSuffix
import org.schabi.newpipe.extractor.utils.SecureUrls
import org.schabi.newpipe.extractor.utils.Utils
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.DateTimeException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Collections
import java.util.Locale

public object BandcampExtractorHelper {

    /**
     * List of image IDs which preserve aspect ratio with their theoretical dimension known.
     *
     * Bandcamp images are not always squares, so images which preserve aspect ratio are only used.
     *
     * One of the direct consequences of this specificity is that only one dimension of images is
     * known at time, depending of the image ID.
     *
     * Note also that dimensions are only theoretical because if the image size is less than the
     * dimensions of the image ID, it will be not upscaled but kept to its original size.
     *
     * IDs come from [the GitHub Gist "Bandcamp File Format Parameters" by
     * f2k1de](https://gist.github.com/f2k1de/06f5fd0ae9c919a7c3693a44ee522213)
     */
    private val IMAGE_URL_SUFFIXES_AND_RESOLUTIONS = listOf(
        // ID | HEIGHT | WIDTH
        ImageSuffix("10.jpg", HEIGHT_UNKNOWN, 1200, ResolutionLevel.HIGH),
        ImageSuffix("101.jpg", 90, WIDTH_UNKNOWN, ResolutionLevel.LOW),
        ImageSuffix("170.jpg", 422, WIDTH_UNKNOWN, ResolutionLevel.MEDIUM),
        // 180 returns the same image aspect ratio and size as 171
        ImageSuffix("171.jpg", 646, WIDTH_UNKNOWN, ResolutionLevel.MEDIUM),
        ImageSuffix("20.jpg", HEIGHT_UNKNOWN, 1024, ResolutionLevel.HIGH),
        // 203 returns the same image aspect ratio and size as 200
        ImageSuffix("200.jpg", 420, WIDTH_UNKNOWN, ResolutionLevel.MEDIUM),
        ImageSuffix("201.jpg", 280, WIDTH_UNKNOWN, ResolutionLevel.MEDIUM),
        ImageSuffix("202.jpg", 140, WIDTH_UNKNOWN, ResolutionLevel.LOW),
        ImageSuffix("204.jpg", 360, WIDTH_UNKNOWN, ResolutionLevel.MEDIUM),
        ImageSuffix("205.jpg", 240, WIDTH_UNKNOWN, ResolutionLevel.MEDIUM),
        ImageSuffix("206.jpg", 180, WIDTH_UNKNOWN, ResolutionLevel.MEDIUM),
        ImageSuffix("207.jpg", 120, WIDTH_UNKNOWN, ResolutionLevel.LOW),
        ImageSuffix("43.jpg", 100, WIDTH_UNKNOWN, ResolutionLevel.LOW),
        ImageSuffix("44.jpg", 200, WIDTH_UNKNOWN, ResolutionLevel.MEDIUM),
    )

    private val IMAGE_URL_APPENDIX_AND_EXTENSION = Regex("_\\d+\\.\\w+")
    private const val IMAGES_DOMAIN_AND_PATH = "https://f4.bcbits.com/img/"

    public const val BASE_URL: String = "https://bandcamp.com"
    public const val BASE_API_URL: String = "$BASE_URL/api"

    private const val HTTP = "http://"
    private const val HTTPS = "https://"
    private const val ASCII_LIMIT = 0x80
    private const val BANDCAMP_HOST = "bandcamp.com"
    private const val BANDCAMP_SUBDOMAIN_SUFFIX = ".bandcamp.com"
    private const val CART_URL = "https://bandcamp.com/cart"
    private const val CUSTOM_DOMAIN_ERROR =
        "Could not determine whether URL is custom domain (not available? network error?)"

    /** Numeric IDs of the mobile API (bands, albums, tracks, shows). */
    private val NUMERIC_ID = Regex("\\d{1,20}")
    private val RADIO_URL = Regex("https?://bandcamp\\.com/\\?show=(\\d{1,20})")

    /** Thread-safe and immutable: built once instead of on every date. */
    private val DATE_FORMATTER =
        DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)

    /**
     * Translate all these parameters together to the URL of the corresponding album or track
     * using the mobile API
     */
    @JvmStatic
    @Throws(ParsingException::class)
    public fun getStreamUrlFromIds(bandId: Long, itemId: Long, itemType: String?): String {
        // Only the first letter of the type ("t" or "a") is sent: anything else is refused
        val typeLetter = itemType?.firstOrNull()?.takeIf { it in 'a'..'z' }
            ?: throw ParsingException("Ids could not be translated to URL")
        val jsonString = try {
            requireDownloader().get(
                "$BASE_API_URL/mobile/22/tralbum_details?band_id=$bandId" +
                    "&tralbum_id=$itemId&tralbum_type=$typeLetter",
            ).responseBody()
        } catch (e: IOException) {
            throw ParsingException("Ids could not be translated to URL", e)
        } catch (e: ReCaptchaException) {
            throw ParsingException("Ids could not be translated to URL", e)
        }
        val bandcampUrl = try {
            JsonParser.`object`().from(jsonString).getString("bandcamp_url")
        } catch (e: JsonParserException) {
            throw ParsingException("Ids could not be translated to URL", e)
        }
        return artistUrlFromBandcamp(bandcampUrl)
    }

    /**
     * Hosts of artists with their own domain, as Bandcamp itself (its API) or the user (who chose
     * to open the page, see [verifyCustomDomain]) gave them: links to them are then recognized
     * offline. Bounded, in memory only, the oldest forgotten first.
     */
    private val confirmedCustomHosts: MutableSet<String> = Collections.synchronizedSet(
        Collections.newSetFromMap(
            object : LinkedHashMap<String, Boolean>() {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?) =
                    size > MAX_CONFIRMED_HOSTS
            },
        ),
    )
    private const val MAX_CONFIRMED_HOSTS = 256

    private fun normalizedHost(url: URL) = url.host.lowercase(Locale.ROOT).trimEnd('.')

    /**
     * The secure form of an artist or item link given by Bandcamp's API: when it is on the
     * artist's own domain, that domain is remembered as an artist's (see [isArtistDomain]).
     */
    @Throws(ParsingException::class)
    internal fun artistUrlFromBandcamp(url: String?): String {
        val secureUrl = requireSecureUrl(url)
        parseHttpUrlOrNull(secureUrl)?.let(::normalizedHost)
            ?.takeIf { !it.endsWith(BANDCAMP_SUBDOMAIN_SUFFIX) && it != BANDCAMP_HOST }
            ?.let(confirmedCustomHosts::add)
        return secureUrl
    }

    /**
     * Fetch artist details from mobile endpoint.
     * [More technical info.](https://notabug.org/fynngodau/bandcampDirect/wiki/rewindBandcamp+%E2%80%93+Fetching+artist+details)
     */
    @JvmStatic
    @Throws(ParsingException::class)
    public fun getArtistDetails(id: String?): JsonObject {
        // Band IDs are numbers; anything else cannot be an artist and is not sent
        if (id == null || !NUMERIC_ID.matches(id)) {
            throw ParsingException("Invalid Bandcamp artist ID")
        }
        val body = JsonWriter.string()
            .`object`()
            .value("band_id", id)
            .end()
            .done()
            .toByteArray(StandardCharsets.UTF_8)
        val response = try {
            requireDownloader().postWithContentTypeJson(
                "$BASE_API_URL/mobile/22/band_details",
                emptyMap(),
                body,
            ).responseBody()
        } catch (e: IOException) {
            throw ParsingException("Could not download band details", e)
        } catch (e: ReCaptchaException) {
            throw ParsingException("Could not download band details", e)
        }
        return try {
            JsonParser.`object`().from(response)
        } catch (e: JsonParserException) {
            throw ParsingException("Could not download band details", e)
        }
    }

    /**
     * Generate an image url from an image ID.
     *
     * The image ID `10` was chosen because it provides images wide up to 1200px (when
     * the original image width is more than or equal this resolution).
     *
     * Other integer values are possible as well (e.g. 0 is a very large resolution, possibly the
     * original); see [IMAGE_URL_SUFFIXES_AND_RESOLUTIONS] for more details about image
     * resolution IDs.
     *
     * @param id      the image ID
     * @param isAlbum whether the image is the cover of an album or a track
     * @return a URL of the image with this ID with a width up to 1200px
     */
    @JvmStatic
    public fun getImageUrl(id: Long, isAlbum: Boolean): String =
        IMAGES_DOMAIN_AND_PATH + (if (isAlbum) "a" else "") + id + "_10.jpg"

    /**
     * Whether the URL is on a `*.bandcamp.com` artist subdomain, or on the own domain of an
     * artist that Bandcamp's API or [verifyCustomDomain] confirmed earlier. Decided offline:
     * recognizing a link never contacts its host.
     *
     * Other domains are not recognized: telling an artist's own domain apart means loading the
     * page, which reveals the user's address to a host chosen by whoever shared the link, before
     * the user did anything (and the test can be forged by any page). See [verifyCustomDomain].
     *
     * @return `true` if the given URL is on an artist domain known offline
     */
    @JvmStatic
    @Throws(ParsingException::class)
    public fun isArtistDomain(url: String?): Boolean {
        // Only well-formed web links with a plain ASCII host are considered at all
        val parsed = parseHttpUrlOrNull(url) ?: return false
        val host = normalizedHost(parsed)
        return host.endsWith(BANDCAMP_SUBDOMAIN_SUFFIX) || host in confirmedCustomHosts
    }

    /**
     * Loads the page of [url] and tells whether it is a Bandcamp artist on a custom domain (its
     * footer links to the Bandcamp cart). This contacts the host of the link: call it only once
     * the user chose to open that host. The answer comes from the page itself, so a hostile page
     * can claim to be an artist: never trust it for anything but opening the page.
     *
     * @return `true` if the page looks like a Bandcamp artist page
     */
    @JvmStatic
    @Throws(ParsingException::class)
    public fun verifyCustomDomain(url: String?): Boolean {
        val parsed = parseHttpUrlOrNull(url) ?: return false
        val host = parsed.host.lowercase(Locale.ROOT).trimEnd('.')
        if (host.endsWith(BANDCAMP_SUBDOMAIN_SUFFIX)) {
            return true
        }
        // Reject non-artist bandcamp.com URLs
        if (host == BANDCAMP_HOST) {
            return false
        }

        // The page is only ever requested over https.
        val downloader = NewPipe.getDownloader() ?: return false
        val body = try {
            downloader.get(toHttps(url.orEmpty())).responseBody()
        } catch (_: IOException) {
            // the cause is not kept: its message may name the host of the link
            throw ParsingException(CUSTOM_DOMAIN_ERROR)
        } catch (_: ReCaptchaException) {
            throw ParsingException(CUSTOM_DOMAIN_ERROR)
        }
        val cartLink = Jsoup.parse(body)
            .getElementsByClass("cart-wrapper")
            .firstOrNull()
            ?.getElementsByTag("a")
            ?.firstOrNull()
            ?.attr("href")
        return (cartLink == CART_URL).also { artist -> if (artist) confirmedCustomHosts.add(host) }
    }

    /**
     * Whether the URL points to a radio kiosk.
     * @param url the URL to check
     * @return true if the URL matches `https://bandcamp.com/?show=SHOW_ID`
     */
    @JvmStatic
    public fun isRadioUrl(url: String?): Boolean = radioShowIdOrNull(url) != null

    /** The show ID of a radio kiosk URL (see [isRadioUrl]), or `null` for any other link. */
    internal fun radioShowIdOrNull(url: String?): String? {
        if (url == null || url.length > Utils.MAX_URL_LENGTH) {
            return null
        }
        return RADIO_URL.matchEntire(url.lowercase(Locale.ROOT))?.groupValues?.get(1)
    }

    /**
     * Whether [url] is a web link whose host has a dot and whose path holds `/kind/` followed by
     * a name, for one of [kinds] (e.g. `track` or `album`).
     *
     * This replaces the regular expression `https?://.+\..+/(track|album)/.+` of the Java code,
     * whose nested `.+` backtracked badly on long links.
     */
    internal fun isItemUrl(url: String?, vararg kinds: String): Boolean {
        val parsed = parseHttpUrlOrNull(url) ?: return false
        if (!parsed.host.trim('.').contains('.')) {
            return false
        }
        val path = parsed.path.lowercase(Locale.ROOT)
        return kinds.any { kind ->
            val index = path.indexOf("/$kind/")
            index >= 0 && index + kind.length + 2 < path.length
        }
    }

    @JvmStatic
    @Throws(ParsingException::class)
    public fun parseDate(textDate: String): DateWrapper = try {
        DateWrapper(ZonedDateTime.parse(textDate, DATE_FORMATTER).toInstant())
    } catch (_: DateTimeException) {
        // the parser error quotes the date read from the network: it is not kept
        throw ParsingException("Could not parse date")
    }

    /**
     * Get a list of images from a search result [Element].
     *
     * This method will call [getImagesFromImageUrl] using the first non null and
     * non empty image URL found from the `src` attribute of `img` HTML elements, or an
     * empty string if no valid image URL was found.
     *
     * @param searchResult a search result [Element]
     * @return an unmodifiable list of [Image]s, which is never null but can be empty, in the
     * case where no valid image URL was found
     */
    @JvmStatic
    public fun getImagesFromSearchResult(searchResult: Element): List<Image> {
        val imageUrl = searchResult.getElementsByClass("art")
            .asSequence()
            .flatMap { it.getElementsByTag("img").asSequence() }
            .map { it.attr("src") }
            .firstOrNull { it.isNotEmpty() }
        return getImagesFromImageUrl(imageUrl)
    }

    /**
     * Get all images which have resolutions preserving aspect ratio from an image URL.
     *
     * This method will remove the image ID and its extension from the end of the URL and then call
     * [getImagesFromImageBaseUrl].
     *
     * @param imageUrl the full URL of an image provided by Bandcamp, such as in its HTML code
     * @return an unmodifiable list of [Image]s, which is never null but can be empty, in the
     * case where the image URL has been not extracted (and so is null or empty), or is not a
     * web link
     */
    @JvmStatic
    public fun getImagesFromImageUrl(imageUrl: String?): List<Image> {
        // Read from a page: only https web links are kept, never a javascript: or data: link
        val secureImageUrl = secureUrlOrNull(imageUrl) ?: return emptyList()
        return getImagesFromImageBaseUrl(
            IMAGE_URL_APPENDIX_AND_EXTENSION.replaceFirst(secureImageUrl, "_"),
        )
    }

    /**
     * Get all images which have resolutions preserving aspect ratio from an image ID.
     *
     * This method will call [getImagesFromImageBaseUrl].
     *
     * @param id      the id of an image provided by Bandcamp
     * @param isAlbum whether the image is the cover of an album
     * @return an unmodifiable list of [Image]s, which is never null but can be empty, in the
     * case where the image ID has been not extracted (and so equal to 0)
     */
    @JvmStatic
    public fun getImagesFromImageId(id: Long, isAlbum: Boolean): List<Image> {
        if (id == 0L) {
            return emptyList()
        }
        return getImagesFromImageBaseUrl(
            IMAGES_DOMAIN_AND_PATH + (if (isAlbum) "a" else "") + id + "_",
        )
    }

    /**
     * Get all images resolutions preserving aspect ratio from a base image URL.
     *
     * Base image URLs are images containing the image path, a `a` letter if it comes from an
     * album, its ID and an underscore.
     *
     * Images resolutions returned are the ones of [IMAGE_URL_SUFFIXES_AND_RESOLUTIONS].
     *
     * @param baseUrl the base URL of the image
     * @return an unmodifiable and non-empty list of [Image]s
     */
    private fun getImagesFromImageBaseUrl(baseUrl: String): List<Image> {
        val images = ArrayList<Image>(IMAGE_URL_SUFFIXES_AND_RESOLUTIONS.size)
        for (imageSuffix in IMAGE_URL_SUFFIXES_AND_RESOLUTIONS) {
            images.add(
                Image(
                    baseUrl + imageSuffix.suffix,
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

    /**
     * Parses [url] as an http(s) link, or returns `null` when it is missing, too long, malformed,
     * on a non-default port or on a host that is not plain ASCII (which rules out look-alike
     * hosts).
     */
    internal fun parseHttpUrlOrNull(url: String?): URL? {
        if (url.isNullOrEmpty() || url.length > Utils.MAX_URL_LENGTH) {
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

    /** [url] with its `http` scheme (in any case) replaced by `https`. */
    internal fun toHttps(url: String): String =
        if (url.regionMatches(0, HTTP, 0, HTTP.length, ignoreCase = true)) {
            HTTPS + url.substring(HTTP.length)
        } else {
            url
        }

    /**
     * [url] upgraded to https when it is a web link with a plain ASCII host, otherwise `null`:
     * links read from Bandcamp pages and API responses are never followed in any other form.
     */
    internal fun secureUrlOrNull(url: String?): String? {
        val secureUrl = url?.let { toHttps(it) } ?: return null
        val parsed = parseHttpUrlOrNull(secureUrl) ?: return null
        return secureUrl.takeIf { parsed.protocol == "https" && SecureUrls.isWebLink(it) }
    }

    @Throws(ParsingException::class)
    internal fun requireSecureUrl(url: String?): String =
        secureUrlOrNull(url) ?: throw ParsingException("Could not get a valid URL")

    /**
     * The `https://host/` root of [url], for uploader URLs: the Java code took the third
     * `/`-separated part of the URL, which is its host for every valid link.
     */
    @Throws(ParsingException::class)
    internal fun uploaderUrlOf(url: String): String {
        val parsed = parseHttpUrlOrNull(url)
            ?: throw ParsingException("Could not get uploader url")
        return HTTPS + parsed.host + "/"
    }
}

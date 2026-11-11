/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.peertube

import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.utils.SecureUrls
import org.schabi.newpipe.extractor.utils.Utils
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.util.Locale

/**
 * Input checks shared by the PeerTube service.
 *
 * PeerTube is federated: the instance is chosen by the user, links lead to any instance, and the
 * JSON of an instance names other instances and storage hosts. All of it is untrusted, so:
 * - an instance base URL is only ever `https://host[:port]` (plain http on the device itself
 *   only), with an ASCII host and no user info, query or fragment;
 * - every ID is matched against a strict pattern before it is placed in a URL, so that it can
 *   never add path segments or query parameters to a request;
 * - a path read from the JSON is only appended to the base URL when it is an absolute path on
 *   that host, so it cannot redirect the request to another host (`@evil.example/...`);
 * - a host named by an instance (another instance, a storage host) is only reached when it is a
 *   public name, over https: never an IP address, the device or a name of the local network,
 *   which only the instance chosen by the user may be;
 * - error messages never quote a link, an ID or a response.
 */
internal object PeertubeLinkGuards {

    private const val ASCII_LIMIT = 0x80
    private const val DELETE = 0x7f

    /** `https://` + a 253-character host + a port, with room to spare. */
    private const val MAX_BASE_URL_LENGTH = 300

    /** Paths of images, captions and storyboards are a few dozen characters. */
    private const val MAX_PATH_LENGTH = 2_048

    private const val HTTP = "http"
    private const val HTTPS = "https"

    /** The only hosts reached in plain http: the device itself. */
    private val LOCAL_HOSTS = setOf("localhost", "127.0.0.1", "[::1]")

    /** A top-level domain: letters, or an IDN in punycode; never a number (an IP address). */
    private val PUBLIC_TLD = Regex("[a-z]{2,63}|xn--[a-z0-9-]{1,59}")

    /** Names that only exist on a local network. */
    private val LOCAL_SUFFIXES = listOf(
        ".local", ".localhost", ".localdomain", ".internal", ".intranet", ".lan", ".home", ".corp",
        ".home.arpa",
    )

    /** Video and playlist IDs: UUIDs, short UUIDs or numbers. */
    private val VIDEO_ID = Regex("[A-Za-z0-9_-]{1,64}")

    /**
     * An account or channel name (`name` or `name@host`): the characters PeerTube allows in actor
     * names and host names, and well-formed `%XX` escapes.
     */
    private val ACTOR_NAME = Regex("(?:[A-Za-z0-9_.~:@+-]|%[0-9A-Fa-f]{2}){1,320}")

    /** An actor ID as used in API paths: `accounts/name` or `video-channels/name`. */
    private val ACTOR_ID = Regex("(?:accounts|video-channels)/([^/]+)")

    /** Returns [url] when it is present and short enough to be a real link. */
    @Throws(ParsingException::class)
    fun requireBoundedUrl(url: String?): String {
        if (url.isNullOrEmpty() || url.length > Utils.MAX_URL_LENGTH) {
            throw ParsingException("The given URL is empty or too long")
        }
        return url
    }

    /** Parses [url] as an http(s) link with an ASCII host, or returns `null`. */
    // URL(String) is deprecated on Java 20+, but URI parsing is stricter: keep java.net.URL
    @Suppress("DEPRECATION")
    fun parseWebUrlOrNull(url: String?): URL? {
        if (url.isNullOrEmpty() || url.length > Utils.MAX_URL_LENGTH) {
            return null
        }
        val parsed = try {
            URL(url)
        } catch (_: MalformedURLException) {
            return null
        }
        val web = parsed.protocol == HTTP || parsed.protocol == HTTPS
        return parsed.takeIf { web && isAsciiHost(it.host) }
    }

    /**
     * The base URL of an instance (`https://host[:port]`), for building request URLs.
     *
     * A plain http base URL of a host other than the device itself is upgraded to https: nothing
     * about what the user watches is sent in the clear, and the host stays the same.
     *
     * @throws ParsingException if [baseUrl] is not the bare address of an instance
     */
    @Throws(ParsingException::class)
    fun requireBaseUrl(baseUrl: String?): String {
        val uri = parseBaseUrlOrNull(baseUrl)
            ?: throw ParsingException("The PeerTube instance URL is not valid")
        if (!isReachableHost(uri.host)) {
            throw ParsingException("The PeerTube instance URL is not a public host")
        }
        val scheme = uri.scheme.lowercase(Locale.ROOT)
        if (scheme == HTTP && uri.host.lowercase(Locale.ROOT) !in LOCAL_HOSTS) {
            // parseBaseUrlOrNull() checked baseUrl is not null
            return HTTPS + baseUrl.orEmpty().substring(HTTP.length)
        }
        return baseUrl.orEmpty()
    }

    /**
     * Whether [host] is a public name: at least two labels, a top-level domain made of letters,
     * and none of the names of the device or of a local network. IP addresses in any form
     * (`10.0.0.1`, `2130706433`, `0x7f.1`, `[::1]`) all fail the top-level domain check.
     */
    fun isPublicHost(host: String?): Boolean {
        if (!isAsciiHost(host)) {
            return false
        }
        val name = host.orEmpty().lowercase(Locale.ROOT).trimEnd('.')
        val labels = name.split('.')
        return labels.size >= 2 && labels.none { it.isEmpty() } && PUBLIC_TLD.matches(labels.last()) &&
            LOCAL_SUFFIXES.none { name.endsWith(it) }
    }

    /** A public host, or the one of the instance the user chose, which may be on their network. */
    private fun isReachableHost(host: String?): Boolean =
        isPublicHost(host) || (host != null && host.equals(configuredInstanceHost(), ignoreCase = true))

    private fun configuredInstanceHost(): String? = parseBaseUrlOrNull(ServiceList.PeerTube.instance.url)?.host

    /**
     * Checks the address of an instance chosen by the user: https, or plain http on the device
     * itself only, with an ASCII host and nothing but an optional trailing `/` after it.
     *
     * @throws IllegalArgumentException for an instance that would be reached in the clear, or an
     * address that is not the one of an instance
     */
    fun checkInstanceUrl(url: String): String {
        val uri = parseBaseUrlOrNull(url)
            ?: throw IllegalArgumentException("The PeerTube instance URL is not valid")
        val scheme = uri.scheme.lowercase(Locale.ROOT)
        // the host itself, not a prefix of it: http://localhost.example.com is not on the device
        val local = scheme == HTTP && uri.host.lowercase(Locale.ROOT) in LOCAL_HOSTS
        require(scheme == HTTPS || local) { "PeerTube instances are only reached over https" }
        return url
    }

    /** [baseUrl] as an URI when it is the bare address of an instance, otherwise `null`. */
    private fun parseBaseUrlOrNull(baseUrl: String?): URI? {
        if (baseUrl.isNullOrEmpty() || baseUrl.length > MAX_BASE_URL_LENGTH ||
            baseUrl.any { isAmbiguousChar(it) }
        ) {
            return null
        }
        val uri = try {
            URI(baseUrl)
        } catch (_: URISyntaxException) {
            return null
        }
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        val host = uri.host
        val path = uri.rawPath.orEmpty()
        val valid = (scheme == HTTPS || scheme == HTTP) &&
            host != null && isAsciiHost(host) &&
            uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null &&
            (path.isEmpty() || path == "/")
        return uri.takeIf { valid }
    }

    /**
     * [baseUrl] followed by [path], when [path] is an absolute path on that host; `null` when it
     * is missing or could lead elsewhere (`//host`, `@host`, a backslash, a control character).
     */
    fun instanceUrlOrNull(baseUrl: String?, path: String?): String? {
        if (baseUrl == null || path.isNullOrEmpty() || path.length > MAX_PATH_LENGTH ||
            path[0] != '/' || path.startsWith("//") || path.any { isAmbiguousChar(it) }
        ) {
            return null
        }
        return baseUrl + path
    }

    /**
     * [url] when it is an http(s) link with a host and nothing a client could read in two ways,
     * otherwise `null`: a `javascript:`, `file:` or `intent:` link from an instance is dropped, and
     * so is a link to an IP address or a local network name (see [isPublicHost]) other than the
     * instance of the user. A plain http link is upgraded to https, on the same host.
     */
    fun webUrlOrNull(url: String?): String? {
        if (url == null || url.length > Utils.MAX_URL_LENGTH || url.any(::isAmbiguousChar) ||
            !SecureUrls.isWebLink(url)
        ) {
            return null
        }
        val host = parseWebUrlOrNull(url)?.host?.takeIf(::isReachableHost) ?: return null
        val plainHttp = url.regionMatches(0, "$HTTP:", 0, HTTP.length + 1, ignoreCase = true)
        return if (plainHttp && host.lowercase(Locale.ROOT) !in LOCAL_HOSTS) {
            HTTPS + url.substring(HTTP.length)
        } else {
            url
        }
    }

    /** [url] when it is a web link (see [webUrlOrNull]). */
    @Throws(ParsingException::class)
    fun requireWebUrl(url: String?, what: String): String =
        webUrlOrNull(url) ?: throw ParsingException("Could not get $what")

    /** [id] when it is exactly a video or playlist ID, for building URLs. */
    @Throws(ParsingException::class)
    fun requireVideoId(id: String?): String = id?.takeIf { VIDEO_ID.matches(it) }
        ?: throw ParsingException("The given string is not a PeerTube ID")

    /** [name] when it is an account or channel name, for building URLs. */
    @Throws(ParsingException::class)
    fun requireActorName(name: String?): String = name?.takeIf { isSafeActorName(it) }
        ?: throw ParsingException("The given string is not a PeerTube account or channel name")

    /** [id] when it is `accounts/name` or `video-channels/name`, for building URLs. */
    @Throws(ParsingException::class)
    fun requireActorId(id: String?): String {
        val name = id?.let { ACTOR_ID.matchEntire(it) }?.groupValues?.get(1)
        if (name == null || !isSafeActorName(name)) {
            throw ParsingException("The given string is not a PeerTube account or channel ID")
        }
        return id
    }

    /**
     * [id] when it is an actor ID (`accounts/name`, `video-channels/name`) or a bare account name,
     * as found in the link handlers of channels, for building API URLs.
     */
    @Throws(ParsingException::class)
    fun requireActorReference(id: String?): String =
        if (id != null && isSafeActorName(id)) id else requireActorId(id)

    private fun isSafeActorName(name: String): Boolean =
        ACTOR_NAME.matches(name) && !name.all { it == '.' }

    private fun isAsciiHost(host: String?): Boolean =
        !host.isNullOrEmpty() && host.all { it.code < ASCII_LIMIT }

    /** A backslash or a control character is read differently by java.net and HTTP clients. */
    private fun isAmbiguousChar(char: Char): Boolean =
        char == '\\' || char <= ' ' || char.code == DELETE
}

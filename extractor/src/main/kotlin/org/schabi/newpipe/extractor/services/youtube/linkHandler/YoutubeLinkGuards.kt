/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.linkHandler

import org.schabi.newpipe.extractor.exceptions.FoundAdException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import org.schabi.newpipe.extractor.utils.Utils
import java.net.MalformedURLException
import java.net.URL
import java.util.Locale

/**
 * Input checks shared by the YouTube link handlers.
 *
 * Every link reaching these handlers is untrusted (shared from another app, typed by the user,
 * read from a page), so:
 * - links are length-bounded and must use an ASCII host, which rules out look-alike hosts
 *   such as `youtube-nocookıe.com` (dotless ı) that `equalsIgnoreCase` would accept;
 * - every ID is matched against a strict pattern before it is placed in a URL, so a link can
 *   never smuggle extra query parameters or path segments into a request;
 * - error messages never quote the link, which may carry personal or tracking data that
 *   would otherwise end up in logs and crash reports.
 */
internal object YoutubeLinkGuards {

    private const val DELETE = 0x7F

    /** ASCII labels of letters, digits and hyphens, with an optional final dot. */
    private val HOST = Regex("[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)*\\.?")

    private val VIDEO_ID_PREFIX = Regex("^[a-zA-Z0-9_-]{11}")

    /**
     * IDs placed in URLs: only URL-safe characters, so nothing can be injected. The length is
     * bounded but not pinned to 11, so that a future ID format does not break the app.
     */
    private val VIDEO_ID = Regex("[a-zA-Z0-9_-]{1,64}")
    private val PLAYLIST_ID = Regex("[a-zA-Z0-9_-]+")

    /**
     * One channel path segment: letters and marks of any script, digits, the few symbols
     * YouTube allows (including the middle dot), and well-formed `%XX` escapes.
     */
    private const val CHANNEL_SEGMENT = "(?:[\\p{L}\\p{M}\\p{N}._~@+\\u00B7-]|%[0-9A-Fa-f]{2})+"

    /** `channel/`, `user/` and `c/` IDs, `@handles` and custom names, as found in links. */
    private val CHANNEL_ID = Regex("(?:(?:channel|user|c)/)?$CHANNEL_SEGMENT")

    /**
     * Channel paths given by YouTube itself (e.g. `@handle/videos`): any number of segments, but
     * never a query, a fragment or a `.`/`..` segment that could leave the channel page.
     */
    private val CHANNEL_PATH = Regex("$CHANNEL_SEGMENT(?:/$CHANNEL_SEGMENT)*")

    /** Parses [url] as a link, or throws without quoting it. */
    @Throws(ParsingException::class)
    fun parseUrl(url: String?): URL = parseUrlOrNull(url)
        ?: throw ParsingException("The given URL is not valid")

    /**
     * Parses [url], or returns `null` when it is missing, too long, malformed, holds a backslash
     * or a control character, or has a host that is not plain ASCII labels.
     *
     * `java.net.URL` keeps a backslash in the host (`https://evil.example\.youtube.com/` has the
     * host `evil.example\.youtube.com`, which ends with `.youtube.com`), while OkHttp and
     * browsers read it as a `/` and connect to `evil.example`: such a link is refused here.
     */
    fun parseUrlOrNull(url: String?): URL? {
        if (url.isNullOrEmpty() || url.length > Utils.MAX_URL_LENGTH ||
            url.any { it == '\\' || it.code < ' '.code || it.code == DELETE }
        ) {
            return null
        }
        val parsed = try {
            Utils.stringToURL(url)
        } catch (_: MalformedURLException) {
            return null
        }
        return parsed.takeIf { HOST.matches(it.host) }
    }

    /** Returns [url] when it is present and short enough to be a real link. */
    @Throws(ParsingException::class)
    fun requireBoundedUrl(url: String?): String {
        if (url.isNullOrEmpty() || url.length > Utils.MAX_URL_LENGTH) {
            throw ParsingException("The given URL is empty or too long")
        }
        return url
    }

    /**
     * Whether [parse] succeeds, for `onAcceptUrl`. A [FoundAdException] still propagates, so
     * that callers can tell an ad link from a link that is merely not supported.
     */
    @Throws(FoundAdException::class)
    inline fun accepts(parse: () -> Unit): Boolean = try {
        parse()
        true
    } catch (e: FoundAdException) {
        throw e
    } catch (_: ParsingException) {
        false
    }

    /** Upper-cased host, locale-independent (a Turkish locale would turn `i` into `İ`). */
    fun hostKey(url: URL): String = url.host.uppercase(Locale.ROOT)

    fun isYoutubeOrInvidiousHttpUrl(url: URL): Boolean = Utils.isHTTP(url) &&
        (YoutubeParsingHelper.isYoutubeURL(url) || YoutubeParsingHelper.isInvidiousURL(url))

    /** Whether [url] is an HTTP(S) link to exactly [host]. */
    fun isHttpUrlOnHost(url: URL, host: String): Boolean =
        Utils.isHTTP(url) && url.host.lowercase(Locale.ROOT) == host

    /** Query parameter value, or `null` when absent or not decodable. */
    fun queryValue(url: URL, name: String): String? = Utils.getQueryValue(url, name)

    /** The video ID [value] starts with, if any. */
    fun videoIdPrefixOrNull(value: String?): String? =
        value?.let { VIDEO_ID_PREFIX.find(it)?.value }

    @Throws(ParsingException::class)
    fun requireVideoIdPrefix(value: String?): String = videoIdPrefixOrNull(value)
        ?: throw ParsingException("The given string is not a YouTube video ID")

    /** [id] when it is exactly a video ID, for building URLs. */
    @Throws(ParsingException::class)
    fun requireVideoId(id: String?): String = id?.takeIf { VIDEO_ID.matches(it) }
        ?: throw ParsingException("The given string is not a YouTube video ID")

    @Throws(ParsingException::class)
    fun requirePlaylistId(id: String?): String = id?.takeIf { PLAYLIST_ID.matches(it) }
        ?: throw ParsingException("The given string is not a YouTube playlist ID")

    @Throws(ParsingException::class)
    fun requireChannelId(id: String?): String = id?.takeIf { isSafeChannelPath(it, CHANNEL_ID) }
        ?: throw ParsingException("The given string is not a YouTube channel ID")

    /** [path] when it can only ever designate a page under a channel, for building URLs. */
    @Throws(ParsingException::class)
    fun requireChannelPath(path: String?): String =
        path?.takeIf { isSafeChannelPath(it, CHANNEL_PATH) }
            ?: throw ParsingException("The given string is not a YouTube channel path")

    private fun isSafeChannelPath(path: String, pattern: Regex): Boolean =
        pattern.matches(path) &&
            // "%2e%2e" is read as ".." by HTTP clients
            path.split('/').none { segment -> segment.replace("%2e", ".", ignoreCase = true).all { it == '.' } }
}

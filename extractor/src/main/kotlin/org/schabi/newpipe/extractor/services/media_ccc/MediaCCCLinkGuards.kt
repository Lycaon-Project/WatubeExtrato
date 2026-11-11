/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.media_ccc

import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.utils.SecureUrls
import org.schabi.newpipe.extractor.utils.Utils

/**
 * Input checks shared by the media.ccc.de service.
 *
 * Links and the JSON of the API are untrusted, so every ID is matched against a strict pattern
 * before it is placed in a URL (it can never add path segments or query parameters), the URLs
 * the API gives are only used when they are web links, and the only URL the extractor requests
 * from a JSON value (the conference of a talk) must be a media.ccc.de API URL. Error messages
 * never quote a link, an ID or a response.
 */
internal object MediaCCCLinkGuards {

    private const val DELETE = 0x7f

    /**
     * A conference or event slug: letters, marks and digits of any script, a few symbols, and
     * well-formed `%XX` escapes.
     */
    private const val SLUG_PATTERN = "(?:[\\p{L}\\p{M}\\p{N}._~+@-]|%[0-9A-Fa-f]{2}){1,256}"
    private val SLUG = Regex(SLUG_PATTERN)

    /** `conference_slug/room_slug`, as the live stream IDs. */
    private val LIVE_STREAM_ID = Regex("\\w{1,128}/\\w{1,128}")

    /** Event GUIDs: UUIDs or base64url IDs (letters, digits, `_` and `-`). */
    private val GUID = Regex("[A-Za-z0-9_-]{1,64}")

    /** The API URL of a conference, as found in the events. */
    private val CONFERENCE_API_URL = Regex("https://(?:api\\.)?media\\.ccc\\.de/public/conferences/($SLUG_PATTERN)")

    /** Returns [url] when it is present and short enough to be a real link. */
    @Throws(ParsingException::class)
    fun requireBoundedUrl(url: String?): String {
        if (url.isNullOrEmpty() || url.length > Utils.MAX_URL_LENGTH) {
            throw ParsingException("The given URL is empty or too long")
        }
        return url
    }

    /** [slug] when it is a conference or event slug, for building URLs. */
    @Throws(ParsingException::class)
    fun requireSlug(slug: String?): String = slug?.takeIf { isSlug(it) }
        ?: throw ParsingException("The given string is not a media.ccc.de ID")

    fun isSlug(slug: String): Boolean = SLUG.matches(slug) && !slug.all { it == '.' }

    /** [id] when it is a live stream ID (`conference_slug/room_slug`), for building URLs. */
    @Throws(ParsingException::class)
    fun requireLiveStreamId(id: String?): String = id?.takeIf { LIVE_STREAM_ID.matches(it) }
        ?: throw ParsingException("The given string is not a media.ccc.de live stream ID")

    /** [guid], without the surrounding spaces the API sometimes adds, when it is an event GUID. */
    @Throws(ParsingException::class)
    fun requireGuid(guid: String?): String = guid?.trim()?.takeIf { GUID.matches(it) }
        ?: throw ParsingException("The given string is not a media.ccc.de event GUID")

    /**
     * [url] when it is the API URL of a conference on media.ccc.de: the only URL read from the API
     * that the extractor requests, so it can never lead to another host.
     */
    @Throws(ParsingException::class)
    fun requireConferenceApiUrl(url: String?): String {
        val slug = url?.let { CONFERENCE_API_URL.matchEntire(it) }?.groupValues?.get(1)
        if (slug == null || !isSlug(slug)) {
            throw ParsingException("The conference URL is not a media.ccc.de API URL")
        }
        return url
    }

    /**
     * [url] when it is an http(s) link with a host and nothing a client could read in two ways,
     * otherwise `null`: a `javascript:`, `file:` or `intent:` link is dropped.
     */
    fun webUrlOrNull(url: String?): String? = url?.takeIf {
        it.length <= Utils.MAX_URL_LENGTH &&
            it.none { char -> char == '\\' || char <= ' ' || char.code == DELETE } &&
            SecureUrls.isWebLink(it)
    }

    /** [url] when it is a web link (see [webUrlOrNull]). */
    @Throws(ParsingException::class)
    fun requireWebUrl(url: String?, what: String): String =
        webUrlOrNull(url) ?: throw ParsingException("Could not get $what")
}

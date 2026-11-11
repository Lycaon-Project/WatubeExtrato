/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.extractors

import org.schabi.newpipe.extractor.json.JsonObject

/**
 * Checks shared by the YouTube extractors when they turn JSON items into info items.
 *
 * - Advertising renderers are never parsed: they would expose ad URLs and thumbnails (and the
 *   tracking parameters they carry) as ordinary items. Every item-collecting loop already picks
 *   only the renderers it knows, but it skips these explicitly so that a future renderer
 *   wrapping an ad can never slip through.
 * - Continuation tokens placed in URLs are checked first, so that a response can never inject
 *   query parameters into a request.
 */
internal object YoutubeItemGuards {

    /** Renderers and keys that only carry advertising (promoted videos, ad slots, ...). */
    private val AD_RENDERER_KEYS = listOf(
        "adSlotRenderer",
        "promotedVideoRenderer",
        "compactPromotedVideoRenderer",
        "promotedSparklesWebRenderer",
        "promotedSparklesTextSearchRenderer",
        "searchPyvRenderer",
        "displayAdRenderer",
        "inFeedAdLayoutRenderer",
        "adBreakServiceRenderer",
        "statementBannerRenderer",
        "brandVideoShelfRenderer",
        "brandVideoSingletonRenderer",
        "movieOfferModuleRenderer",
    )

    /** Upper bound of a continuation token: real ones are a few hundred characters. */
    private const val MAX_TOKEN_LENGTH = 4_096

    /** Base64 (URL-safe or not), optionally percent-encoded: nothing that ends a parameter. */
    private val URL_SAFE_TOKEN = Regex("[A-Za-z0-9_%=+/.-]+")

    /** Whether [item] is (or directly wraps) an advertising renderer, which must be skipped. */
    fun isAd(item: JsonObject): Boolean = AD_RENDERER_KEYS.any { item.has(it) }

    /** [token] when it can be placed as is in a query string, otherwise `null`. */
    fun urlSafeTokenOrNull(token: String?): String? = token?.takeIf {
        it.length <= MAX_TOKEN_LENGTH && URL_SAFE_TOKEN.matches(it)
    }
}

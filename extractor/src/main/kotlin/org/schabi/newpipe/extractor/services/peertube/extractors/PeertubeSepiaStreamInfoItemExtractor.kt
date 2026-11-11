/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.peertube.extractors

import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.services.peertube.PeertubeLinkGuards

/**
 * A StreamInfoItem collected from SepiaSearch
 */
public open class PeertubeSepiaStreamInfoItemExtractor(
    item: JsonObject,
    baseUrl: String,
) : PeertubeStreamInfoItemExtractor(item, baseUrl) {

    init {
        // Usually, all videos, pictures and other content are hosted on the instance,
        // or can be accessed by the same URL path if the instance with baseUrl federates the one
        // where the video is actually uploaded. But it can't be accessed with Sepiasearch, so we
        // use the item's instance as base URL.
        // SepiaSearch indexes any instance: its address is only used once it has been checked to
        // be the bare https address of an instance, otherwise the item has no URL and no images.
        val embedUrl = this.item.getString("embedUrl")
        val embedPath = this.item.getString("embedPath")
        val itemBaseUrl = if (embedUrl == null || embedPath.isNullOrEmpty()) {
            null
        } else {
            embedUrl.replace(embedPath, "")
        }
        setBaseUrl(
            try {
                PeertubeLinkGuards.requireBaseUrl(itemBaseUrl)
            } catch (_: ParsingException) {
                null
            },
        )
    }
}

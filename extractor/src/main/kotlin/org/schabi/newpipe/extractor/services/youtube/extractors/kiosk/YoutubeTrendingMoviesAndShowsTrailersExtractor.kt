/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.extractors.kiosk

import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler

public open class YoutubeTrendingMoviesAndShowsTrailersExtractor(
    streamingService: StreamingService,
    linkHandler: ListLinkHandler,
    kioskId: String,
) : YoutubeChartsBaseKioskExtractor(streamingService, linkHandler, kioskId, "TRENDING_MOVIES") {

    // This is the official YouTube Charts name, even if shows' trailers are returned too
    @get:Throws(ParsingException::class)
    override val name: String
        get() = "Trending Movie Trailers"
}

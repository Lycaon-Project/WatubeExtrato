/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.extractors.kiosk

import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler

public open class YoutubeTrendingPodcastsEpisodesExtractor(
    streamingService: StreamingService,
    linkHandler: ListLinkHandler,
    kioskId: String,
) : YoutubeDesktopBaseKioskExtractor(
    streamingService,
    linkHandler,
    kioskId,
    "FEpodcasts_destination",
    "qgcCCAM%3D",
)

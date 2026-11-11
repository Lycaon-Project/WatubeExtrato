/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.peertube.extractors

import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.suggestion.SuggestionExtractor

public open class PeertubeSuggestionExtractor(service: StreamingService) : SuggestionExtractor(service) {

    override fun suggestionList(query: String): List<String> = emptyList()
}

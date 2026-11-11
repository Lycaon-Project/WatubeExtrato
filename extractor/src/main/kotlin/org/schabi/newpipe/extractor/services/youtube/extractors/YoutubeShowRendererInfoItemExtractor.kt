/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.extractors

import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getTextFromObject
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getUrlFromObject

/**
 * A [YoutubeBaseShowInfoItemExtractor] implementation for `showRenderer`s.
 */
internal class YoutubeShowRendererInfoItemExtractor(
    showRenderer: JsonObject,
) : YoutubeBaseShowInfoItemExtractor(showRenderer) {

    private val shortBylineText: JsonObject = showRenderer.getObject("shortBylineText")
    private val longBylineText: JsonObject = showRenderer.getObject("longBylineText")

    @get:Throws(ParsingException::class)
    override val uploaderName: String
        get() = getTextFromObject(longBylineText)?.takeIf { it.isNotEmpty() }
            ?: getTextFromObject(shortBylineText)?.takeIf { it.isNotEmpty() }
            ?: throw ParsingException("Could not get uploader name")

    @get:Throws(ParsingException::class)
    override val uploaderUrl: String
        get() = getUrlFromObject(longBylineText)
            ?: getUrlFromObject(shortBylineText)
            ?: throw ParsingException("Could not get uploader URL")

    // We do not have this information in showRenderers
    @get:Throws(ParsingException::class)
    override val isUploaderVerified: Boolean
        get() = false
}

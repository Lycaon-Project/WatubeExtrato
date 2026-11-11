/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.media_ccc.linkHandler

import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory
import org.schabi.newpipe.extractor.services.media_ccc.MediaCCCLinkGuards
import org.schabi.newpipe.extractor.services.media_ccc.extractors.MediaCCCParsingHelper
import org.schabi.newpipe.extractor.utils.Parser
import java.util.regex.Pattern

public object MediaCCCStreamLinkHandlerFactory : LinkHandlerFactory() {

    public const val VIDEO_API_ENDPOINT: String = "https://api.media.ccc.de/public/events/"
    private const val VIDEO_PATH = "https://media.ccc.de/v/"
    private const val LIVE_STREAM_PATH = "https://streaming.media.ccc.de/"

    private val RECORDING_ID_PATTERN: Pattern = Pattern.compile(
        "(?:(?:(?:api\\.)?media\\.ccc\\.de/public/events/)" +
            "|(?:media\\.ccc\\.de/v/))([^/?&#]*)",
    )
    private val LIVE_STREAM_ID_PATTERN: Pattern = Pattern.compile("streaming\\.media\\.ccc\\.de\\/(\\w+\\/\\w+)")

    @JvmStatic
    public fun getInstance(): MediaCCCStreamLinkHandlerFactory = this

    /** The ID is checked to be a live stream ID or an event slug before it is returned. */
    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getId(url: String?): String {
        val link = MediaCCCLinkGuards.requireBoundedUrl(url)
        val streamId = try {
            Parser.matchGroup1(LIVE_STREAM_ID_PATTERN, link)
        } catch (_: Parser.RegexException) {
            null
        }
        return if (streamId == null) {
            MediaCCCLinkGuards.requireSlug(Parser.matchGroup1(RECORDING_ID_PATTERN, link))
        } else {
            MediaCCCLinkGuards.requireLiveStreamId(streamId)
        }
    }

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(id: String?): String {
        if (id != null && MediaCCCParsingHelper.isLiveStreamId(id)) {
            return LIVE_STREAM_PATH + MediaCCCLinkGuards.requireLiveStreamId(id)
        }
        return VIDEO_PATH + MediaCCCLinkGuards.requireSlug(id)
    }

    override fun onAcceptUrl(url: String?): Boolean = try {
        getId(url)
        true
    } catch (_: ParsingException) {
        false
    }
}

/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.media_ccc.linkHandler

import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory

public object MediaCCCLiveListLinkHandlerFactory : ListLinkHandlerFactory() {

    private val STREAM_PATTERN = Regex("^(?:https?://)?media\\.ccc\\.de/live$")

    @JvmStatic
    public fun getInstance(): MediaCCCLiveListLinkHandlerFactory = this

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getId(url: String?): String = "live"

    @Throws(ParsingException::class)
    override fun onAcceptUrl(url: String?): Boolean = url != null && STREAM_PATTERN.matches(url)

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
    ): String {
        // FIXME: wrong URL; should be https://streaming.media.ccc.de/{conference_slug}/{room_slug}
        return "https://media.ccc.de/live"
    }
}

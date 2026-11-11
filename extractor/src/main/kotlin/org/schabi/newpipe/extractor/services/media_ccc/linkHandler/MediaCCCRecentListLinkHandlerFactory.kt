/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.media_ccc.linkHandler

import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory

public object MediaCCCRecentListLinkHandlerFactory : ListLinkHandlerFactory() {

    private val PATTERN = Regex("^(https?://)?media\\.ccc\\.de/recent/?$")

    @JvmStatic
    public fun getInstance(): MediaCCCRecentListLinkHandlerFactory = this

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getId(url: String?): String = "recent"

    override fun onAcceptUrl(url: String?): Boolean = url != null && PATTERN.matches(url)

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
    ): String = "https://media.ccc.de/recent"
}

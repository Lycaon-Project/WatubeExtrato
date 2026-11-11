/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.media_ccc.linkHandler

import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory

public object MediaCCCConferencesListLinkHandlerFactory : ListLinkHandlerFactory() {

    private val ACCEPTED_URLS = setOf(
        "https://media.ccc.de/b/conferences",
        "https://media.ccc.de/public/conferences",
        "https://api.media.ccc.de/public/conferences",
    )

    @JvmStatic
    public fun getInstance(): MediaCCCConferencesListLinkHandlerFactory = this

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getId(url: String?): String = "conferences"

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
    ): String = "https://media.ccc.de/public/conferences"

    override fun onAcceptUrl(url: String?): Boolean = url in ACCEPTED_URLS
}

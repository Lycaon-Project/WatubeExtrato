/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.media_ccc.linkHandler

import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory
import org.schabi.newpipe.extractor.utils.Utils

public object MediaCCCSearchQueryHandlerFactory : SearchQueryHandlerFactory() {

    public const val ALL: String = "all"
    public const val CONFERENCES: String = "conferences"
    public const val EVENTS: String = "events"

    @JvmStatic
    public fun getInstance(): MediaCCCSearchQueryHandlerFactory = this

    override val availableContentFilter: Array<String>
        get() = arrayOf(
            ALL,
            CONFERENCES,
            EVENTS,
        )

    override val availableSortFilter: Array<String>
        get() = emptyArray()

    /**
     * The query is percent-encoded, so it can only ever be the value of the search parameter.
     *
     * @param id the search query (named like the parameter of [ListLinkHandlerFactory.getUrl])
     */
    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
    ): String = "https://media.ccc.de/public/events/search?q=" +
        Utils.encodeUrlUtf8(id ?: throw ParsingException("No search query given"))
}

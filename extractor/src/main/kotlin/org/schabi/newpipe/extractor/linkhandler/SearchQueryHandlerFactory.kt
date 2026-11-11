package org.schabi.newpipe.extractor.linkhandler

import org.schabi.newpipe.extractor.exceptions.ParsingException

/**
 * Builds the search pages. Implementations provide [getUrl] (from [ListLinkHandlerFactory]),
 * whose `id` is the search query.
 */
public abstract class SearchQueryHandlerFactory : ListLinkHandlerFactory() {

    ///////////////////////////////////
    // To Override
    ///////////////////////////////////

    public open fun getSearchString(url: String?): String = ""

    ///////////////////////////////////
    // Logic
    ///////////////////////////////////

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getId(url: String?): String = getSearchString(url)

    @Throws(ParsingException::class)
    override fun fromQuery(
        id: String,
        contentFilters: List<String>,
        sortFilter: String,
    ): SearchQueryHandler = SearchQueryHandler(super.fromQuery(id, contentFilters, sortFilter))

    @Throws(ParsingException::class)
    public fun fromQuery(query: String): SearchQueryHandler = fromQuery(query, emptyList(), "")

    /**
     * It's not mandatory for NewPipe to handle the Url
     */
    override fun onAcceptUrl(url: String?): Boolean = false
}

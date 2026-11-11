package org.schabi.newpipe.extractor.linkhandler

public open class SearchQueryHandler(
    originalUrl: String,
    url: String,
    searchString: String,
    contentFilters: List<String>,
    sortFilter: String,
) : ListLinkHandler(originalUrl, url, searchString, contentFilters, sortFilter) {

    public constructor(handler: ListLinkHandler) : this(
        handler.originalUrl,
        handler.url,
        handler.id,
        handler.contentFilters,
        handler.sortFilter,
    )

    /**
     * Returns the search string. Since ListQIHandler is based on ListLinkHandler
     * getSearchString() is equivalent to calling getId().
     *
     * @return the search string
     */
    public val searchString: String
        get() = id

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

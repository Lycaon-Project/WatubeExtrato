package org.schabi.newpipe.extractor.linkhandler

import java.util.Collections

public open class ListLinkHandler(
    originalUrl: String,
    url: String,
    id: String,
    contentFilters: List<String>,
    public val sortFilter: String,
) : LinkHandler(originalUrl, url, id) {

    public val contentFilters: List<String> = Collections.unmodifiableList(contentFilters)

    public constructor(handler: ListLinkHandler) : this(
        handler.originalUrl,
        handler.url,
        handler.id,
        handler.contentFilters,
        handler.sortFilter,
    )

    public constructor(handler: LinkHandler) : this(
        handler.originalUrl,
        handler.url,
        handler.id,
        emptyList(),
        "",
    )

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

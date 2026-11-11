package org.schabi.newpipe.extractor

import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler

public abstract class ListInfo<T : InfoItem>(
    serviceId: Int,
    id: String,
    url: String,
    originalUrl: String,
    name: String,
    contentFilter: List<String>?,
    /** `null` when the list was not built from a [ListLinkHandler] (e.g. feeds). */
    public val sortFilter: String?,
) : Info(serviceId, id, url, originalUrl, name) {

    /** `null` when the list was not built from a [ListLinkHandler] (e.g. feeds). */
    public val contentFilters: List<String>? = contentFilter

    /** The items of the first page; empty until they are set. */
    public var relatedItems: List<T> = emptyList()

    public var nextPage: Page? = null

    public constructor(serviceId: Int, listUrlIdHandler: ListLinkHandler, name: String) : this(
        serviceId,
        listUrlIdHandler.id,
        listUrlIdHandler.url,
        listUrlIdHandler.originalUrl,
        name,
        listUrlIdHandler.contentFilters,
        listUrlIdHandler.sortFilter,
    )

    public fun hasNextPage(): Boolean = Page.isValid(nextPage)

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

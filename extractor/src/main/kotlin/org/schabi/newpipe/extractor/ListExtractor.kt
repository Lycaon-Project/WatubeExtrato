package org.schabi.newpipe.extractor

import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import java.io.IOException

/**
 * Base class to extractors that have a list (e.g. playlists, users).
 * @param R the info item type this list extractor provides
 */
public abstract class ListExtractor<R : InfoItem>(
    service: StreamingService,
    linkHandler: ListLinkHandler,
) : Extractor(service, linkHandler) {

    /**
     * A [InfoItemsPage] corresponding to the initial page
     * where the items are from the initial request and the nextPage relative to it.
     *
     * @return a [InfoItemsPage] corresponding to the initial page
     */
    @get:Throws(IOException::class, ExtractionException::class)
    public abstract val initialPage: InfoItemsPage<R>

    /**
     * Get a list of items corresponding to the specific requested page.
     *
     * @param page any page got from the exclusive implementation of the list extractor
     * @return a [InfoItemsPage] corresponding to the requested page
     * @see InfoItemsPage.nextPage
     */
    @Throws(IOException::class, ExtractionException::class)
    public abstract fun getPage(page: Page?): InfoItemsPage<R>

    override val linkHandler: ListLinkHandler
        get() = super.linkHandler as ListLinkHandler

    // ////////////////////////////////////////////////////////////////////////
    // Inner
    // ////////////////////////////////////////////////////////////////////////

    /**
     * A class that is used to wrap a list of gathered items and eventual errors, it
     * also contains a field that points to the next available page ([nextPage]).
     *
     * Java callers may pass `null` for the items or the errors, as with the Java class;
     * they are then stored as empty lists.
     *
     * @param T the info item type that this page is supposed to store and provide
     */
    public open class InfoItemsPage<T : InfoItem>(
        items: List<T>?,
        /**
         * Url pointing to the next page relative to this one, if available
         *
         * @see ListExtractor.getPage
         * @see Page
         */
        public val nextPage: Page?,
        errors: List<Throwable>?,
    ) {
        /**
         * The current list of items of this page
         */
        public val items: List<T> = items ?: emptyList()

        /**
         * Errors that happened during the extraction
         */
        public val errors: List<Throwable> = errors ?: emptyList()

        public constructor(collector: InfoItemsCollector<T, *>, nextPage: Page?) :
            this(collector.items, nextPage, collector.errors)

        public fun hasNextPage(): Boolean = Page.isValid(nextPage)

        public companion object {
            private val EMPTY = InfoItemsPage<InfoItem>(emptyList(), null, emptyList())

            /**
             * A convenient method that returns a representation of an empty page.
             *
             * @return a type-safe page with the list of items and errors empty and the nextPage
             * set to `null`.
             */
            @JvmStatic
            @Suppress("UNCHECKED_CAST") // empty and read-only: safe for any item type
            public fun <T : InfoItem> emptyPage(): InfoItemsPage<T> = EMPTY as InfoItemsPage<T>
        }
    }

    public companion object {
        /**
         * Constant that should be returned whenever
         * a list has an unknown number of items.
         */
        public const val ITEM_COUNT_UNKNOWN: Long = -1

        /**
         * Constant that should be returned whenever a list has an
         * infinite number of items. For example a YouTube mix.
         */
        public const val ITEM_COUNT_INFINITE: Long = -2

        /**
         * Constant that should be returned whenever a list
         * has an unknown number of items bigger than 100.
         */
        public const val ITEM_COUNT_MORE_THAN_100: Long = -3
    }
}

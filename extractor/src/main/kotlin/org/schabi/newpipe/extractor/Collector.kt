package org.schabi.newpipe.extractor

import org.schabi.newpipe.extractor.exceptions.ParsingException

/**
 * Collectors are used to simplify the collection of information
 * from extractors
 * @param I the item type
 * @param E the extractor type
 */
public interface Collector<I, E> {

    /**
     * Try to add an extractor to the collection
     * @param extractor the extractor to add
     */
    public fun commit(extractor: E)

    /**
     * Try to extract the item from an extractor without adding it to the collection
     * @param extractor the extractor to use
     * @return the item
     * @throws ParsingException thrown if there is an error extracting the
     * **required** fields of the item.
     */
    @Throws(ParsingException::class)
    public fun extract(extractor: E): I

    /** All items */
    public val items: List<I>

    /** All errors */
    public val errors: List<Throwable>

    /**
     * Reset all collected items and errors
     */
    public fun reset()
}

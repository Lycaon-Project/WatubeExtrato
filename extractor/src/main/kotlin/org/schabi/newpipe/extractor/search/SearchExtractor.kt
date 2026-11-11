package org.schabi.newpipe.extractor.search

import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.MetaInfo
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler

public abstract class SearchExtractor(service: StreamingService, linkHandler: SearchQueryHandler) :
    ListExtractor<InfoItem>(service, linkHandler) {

    public open class NothingFoundException(message: String?) : ExtractionException(message)

    public val searchString: String
        get() = linkHandler.searchString

    /**
     * The search suggestion provided by the service.
     *
     * This method also returns the corrected query if [isCorrectedSearch] is true.
     *
     * @return a suggestion to another query, the corrected query, or an empty String.
     */
    @get:Throws(ParsingException::class)
    public abstract val searchSuggestion: String

    override val linkHandler: SearchQueryHandler
        get() = super.linkHandler as SearchQueryHandler

    override val name: String
        get() = linkHandler.searchString

    /**
     * Tell if the search was corrected by the service (if it's not exactly the search you typed).
     *
     * Example: on YouTube, if you search for "pewdeipie",
     * it will give you results for "pewdiepie", then isCorrectedSearch should return true.
     *
     * @return whether the results comes from a corrected query or not.
     */
    @get:Throws(ParsingException::class)
    public abstract val isCorrectedSearch: Boolean

    /**
     * Meta information about the search query.
     *
     * Example: on YouTube, if you search for "Covid-19",
     * there is a box with information from the WHO about Covid-19 and a link to the WHO's website.
     * @return additional meta information about the search query
     */
    @get:Throws(ParsingException::class)
    public abstract val metaInfo: List<MetaInfo>
}

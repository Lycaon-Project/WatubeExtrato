package org.schabi.newpipe.extractor.services.bandcamp.extractors.streaminfoitem

import org.jsoup.nodes.Element
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.getImagesFromSearchResult

public open class BandcampSearchStreamInfoItemExtractor(
    private val searchResult: Element,
    uploaderUrl: String?,
) : BandcampStreamInfoItemExtractor(uploaderUrl) {

    private val resultInfo: Element? = searchResult.getElementsByClass("result-info").firstOrNull()

    override val uploaderName: String?
        get() {
            val subhead = resultInfo?.getElementsByClass("subhead")?.text() ?: return null
            // trailing empty parts are dropped, as String.split did in the Java code
            val splitBy = subhead.split("by ").dropLastWhile { it.isEmpty() }
            return splitBy.getOrNull(1) ?: splitBy.firstOrNull().orEmpty()
        }

    @get:Throws(ParsingException::class)
    override val name: String
        get() = requireResultInfo().getElementsByClass("heading").text()

    /** The link shown in the result, upgraded to https; refused when it is not a web link. */
    @get:Throws(ParsingException::class)
    override val url: String
        get() = BandcampExtractorHelper.requireSecureUrl(
            requireResultInfo().getElementsByClass("itemurl").text(),
        )

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = getImagesFromSearchResult(searchResult)

    override val duration: Long
        get() = -1

    @Throws(ParsingException::class)
    private fun requireResultInfo(): Element =
        resultInfo ?: throw ParsingException("Could not get search result info")
}

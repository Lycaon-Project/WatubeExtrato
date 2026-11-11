package org.schabi.newpipe.extractor.services.bandcamp.extractors

import org.jsoup.nodes.Element
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItemExtractor
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.getImagesFromSearchResult

public open class BandcampPlaylistInfoItemExtractor(private val searchResult: Element) :
    PlaylistInfoItemExtractor {

    private val resultInfo: Element? = searchResult.getElementsByClass("result-info").firstOrNull()

    override val uploaderName: String?
        get() = resultInfo?.getElementsByClass("subhead")?.text()?.substringBefore(" by")

    override val uploaderUrl: String?
        get() = null

    override val isUploaderVerified: Boolean
        get() = false

    /**
     * The number of tracks shown in the result ("12 tracks"), or [ListExtractor.ITEM_COUNT_UNKNOWN]
     * when it cannot be read (the Java code failed with a NumberFormatException).
     */
    override val streamCount: Long
        get() = resultInfo?.getElementsByClass("length")?.text()
            ?.substringBefore(" track")
            ?.toLongOrNull()
            ?: ListExtractor.ITEM_COUNT_UNKNOWN

    @get:Throws(ParsingException::class)
    override val name: String
        get() = requireResultInfo().getElementsByClass("heading").text()

    /** The link shown in the result, upgraded to https; refused when it is not a web link. */
    @get:Throws(ParsingException::class)
    override val url: String
        get() = BandcampExtractorHelper.requireSecureUrl(
            requireResultInfo().getElementsByClass("itemurl").text(),
        )

    override val thumbnails: List<Image>
        get() = getImagesFromSearchResult(searchResult)

    @Throws(ParsingException::class)
    private fun requireResultInfo(): Element =
        resultInfo ?: throw ParsingException("Could not get search result info")
}

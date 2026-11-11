// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

package org.schabi.newpipe.extractor.services.bandcamp.extractors

import org.jsoup.nodes.Element
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.channel.ChannelInfoItemExtractor
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.getImagesFromSearchResult

public open class BandcampChannelInfoItemExtractor(private val searchResult: Element) :
    ChannelInfoItemExtractor {

    private val resultInfo: Element? = searchResult.getElementsByClass("result-info").firstOrNull()

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

    override val description: String?
        get() = resultInfo?.getElementsByClass("subhead")?.text()

    override val subscriberCount: Long
        get() = -1

    override val streamCount: Long
        get() = -1

    @get:Throws(ParsingException::class)
    override val isVerified: Boolean
        get() = false

    @Throws(ParsingException::class)
    private fun requireResultInfo(): Element =
        resultInfo ?: throw ParsingException("Could not get search result info")
}

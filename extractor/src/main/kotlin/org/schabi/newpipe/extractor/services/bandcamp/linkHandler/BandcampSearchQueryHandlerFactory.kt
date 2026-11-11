// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

package org.schabi.newpipe.extractor.services.bandcamp.linkHandler

import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.BASE_URL
import org.schabi.newpipe.extractor.utils.Utils

public object BandcampSearchQueryHandlerFactory : SearchQueryHandlerFactory() {

    @JvmStatic
    public fun getInstance(): BandcampSearchQueryHandlerFactory = this

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
    ): String {
        val query = id ?: throw ParsingException("No search query given")
        return "$BASE_URL/search?q=${Utils.encodeUrlUtf8(query)}&page=1"
    }
}

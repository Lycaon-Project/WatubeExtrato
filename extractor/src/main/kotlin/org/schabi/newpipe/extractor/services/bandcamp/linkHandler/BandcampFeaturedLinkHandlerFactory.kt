// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

package org.schabi.newpipe.extractor.services.bandcamp.linkHandler

import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampFeaturedExtractor.Companion.FEATURED_API_URL
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampFeaturedExtractor.Companion.KIOSK_FEATURED
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampRadioExtractor.Companion.KIOSK_RADIO
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampRadioExtractor.Companion.RADIO_API_URL
import org.schabi.newpipe.extractor.utils.Utils

public object BandcampFeaturedLinkHandlerFactory : ListLinkHandlerFactory() {

    @JvmStatic
    public fun getInstance(): BandcampFeaturedLinkHandlerFactory = this

    /**
     * The Java code returned `null` for an unknown kiosk, which no caller could use: an unknown
     * kiosk is now refused with a [ParsingException].
     */
    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
    ): String = when (id) {
        KIOSK_FEATURED -> FEATURED_API_URL // doesn't have a website
        KIOSK_RADIO -> RADIO_API_URL // doesn't have its own website
        else -> throw ParsingException("Unknown Bandcamp kiosk")
    }

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getId(url: String?): String {
        val fixedUrl = Utils.replaceHttpWithHttps(url)
        return when {
            BandcampExtractorHelper.isRadioUrl(fixedUrl) || fixedUrl == RADIO_API_URL -> KIOSK_RADIO
            fixedUrl == FEATURED_API_URL -> KIOSK_FEATURED
            else -> throw ParsingException("Not a Bandcamp kiosk URL")
        }
    }

    override fun onAcceptUrl(url: String?): Boolean {
        val fixedUrl = Utils.replaceHttpWithHttps(url)
        return fixedUrl == FEATURED_API_URL ||
            fixedUrl == RADIO_API_URL ||
            BandcampExtractorHelper.isRadioUrl(fixedUrl)
    }
}

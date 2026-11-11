// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

package org.schabi.newpipe.extractor.services.bandcamp.linkHandler

import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.BASE_URL

/**
 * Tracks don't have standalone ids, they are always in combination with the band id.
 * That's why id = url.
 *
 * Radio (bandcamp weekly) shows do have ids.
 */
public object BandcampStreamLinkHandlerFactory : LinkHandlerFactory() {

    private val SHOW_ID = Regex("\\d{1,20}")

    @JvmStatic
    public fun getInstance(): BandcampStreamLinkHandlerFactory = this

    /**
     * @see BandcampStreamLinkHandlerFactory
     */
    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getId(url: String?): String =
        // The show ID is read whatever the case of the link, unlike the Java code which failed
        // with an ArrayIndexOutOfBoundsException on e.g. "bandcamP.com"
        BandcampExtractorHelper.radioShowIdOrNull(url) ?: getUrl(url)

    /**
     * Clean up url
     * @see BandcampStreamLinkHandlerFactory
     */
    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(id: String?): String = if (id != null && SHOW_ID.matches(id)) {
        "$BASE_URL/?show=$id"
    } else {
        BandcampExtractorHelper.requireSecureUrl(id)
    }

    /**
     * Accepts URLs that point to a bandcamp radio show or that are a bandcamp
     * domain and point to a track.
     */
    @Throws(ParsingException::class)
    override fun onAcceptUrl(url: String?): Boolean {
        // Accept Bandcamp radio
        if (BandcampExtractorHelper.isRadioUrl(url)) {
            return true
        }

        // Don't accept URLs that don't point to a track
        if (!BandcampExtractorHelper.isItemUrl(url, "track")) {
            return false
        }

        // Test whether domain is supported
        return BandcampExtractorHelper.isArtistDomain(url)
    }
}

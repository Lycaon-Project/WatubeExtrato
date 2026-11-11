// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

package org.schabi.newpipe.extractor.services.bandcamp.linkHandler

import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper

/**
 * Just as with streams, the album ids are essentially useless for us.
 */
public object BandcampPlaylistLinkHandlerFactory : ListLinkHandlerFactory() {

    @JvmStatic
    public fun getInstance(): BandcampPlaylistLinkHandlerFactory = this

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getId(url: String?): String = getUrl(url)

    /** The URL itself, upgraded to https; only web links with a plain ASCII host are kept. */
    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
    ): String = BandcampExtractorHelper.requireSecureUrl(id)

    /**
     * Accepts all bandcamp URLs that contain /album/ behind their domain name.
     */
    @Throws(ParsingException::class)
    override fun onAcceptUrl(url: String?): Boolean {
        // Exclude URLs which do not lead to an album
        if (!BandcampExtractorHelper.isItemUrl(url, "album")) {
            return false
        }

        // Test whether domain is supported
        return BandcampExtractorHelper.isArtistDomain(url)
    }
}

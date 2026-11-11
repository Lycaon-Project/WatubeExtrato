package org.schabi.newpipe.extractor.services.bandcamp.linkHandler

import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper

/**
 * Like in [BandcampStreamLinkHandlerFactory], tracks have no meaningful IDs except for
 * their URLs
 */
public object BandcampCommentsLinkHandlerFactory : ListLinkHandlerFactory() {

    @JvmStatic
    public fun getInstance(): BandcampCommentsLinkHandlerFactory = this

    /** The URL itself, upgraded to https; only web links with a plain ASCII host are kept. */
    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getId(url: String?): String = BandcampExtractorHelper.requireSecureUrl(url)

    @Throws(ParsingException::class)
    override fun onAcceptUrl(url: String?): Boolean {
        if (BandcampExtractorHelper.isRadioUrl(url)) {
            return true
        }

        // Don't accept URLs that don't point to a track
        if (!BandcampExtractorHelper.isItemUrl(url, "track", "album")) {
            return false
        }

        // Test whether domain is supported
        return BandcampExtractorHelper.isArtistDomain(url)
    }

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
    ): String = BandcampExtractorHelper.requireSecureUrl(id)
}

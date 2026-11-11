package org.schabi.newpipe.extractor.services.soundcloud.linkHandler

import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper.clientId
import java.io.IOException

public object SoundcloudCommentsLinkHandlerFactory : ListLinkHandlerFactory() {

    @JvmStatic
    public fun getInstance(): SoundcloudCommentsLinkHandlerFactory = this

    /** Only numeric track IDs are placed in the API URL. */
    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
    ): String {
        val trackId = SoundcloudParsingHelper.requireNumericId(id)
        // The client_id stays in this URL (and in the comment and page URLs made from it): it is
        // a public token of the SoundCloud website, the same for every visitor, never a user
        // identifier, and it is only ever sent to the SoundCloud API hosts.
        return try {
            "https://api-v2.soundcloud.com/tracks/$trackId/comments?client_id=${clientId()}" +
                "&threaded=0&filter_replies=1"
            // Anything but 1 = sort by new
            // + "&limit=NUMBER_OF_ITEMS_PER_REQUEST". We let the API control (default = 10)
            // + "&offset=OFFSET". We let the API control (default = 0, then we use nextPageUrl)
        } catch (_: ExtractionException) {
            throw ParsingException("Could not get comments")
        } catch (_: IOException) {
            throw ParsingException("Could not get comments")
        }
    }

    /**
     * Delegation to avoid duplicate code, as we need the same id.
     *
     * Note: for a website link, this asks the widget API of SoundCloud (one request).
     */
    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getId(url: String?): String = SoundcloudStreamLinkHandlerFactory.getId(url)

    // a pattern test, as for the tracks: recognising a link sends nothing to SoundCloud
    override fun onAcceptUrl(url: String?): Boolean = try {
        SoundcloudStreamLinkHandlerFactory.acceptUrl(url)
    } catch (_: ParsingException) {
        false
    }
}

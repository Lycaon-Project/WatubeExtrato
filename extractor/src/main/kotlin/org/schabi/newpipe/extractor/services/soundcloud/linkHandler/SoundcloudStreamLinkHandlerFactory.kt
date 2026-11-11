package org.schabi.newpipe.extractor.services.soundcloud.linkHandler

import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper
import org.schabi.newpipe.extractor.utils.Parser
import org.schabi.newpipe.extractor.utils.Utils
import java.io.IOException
import java.util.Locale
import java.util.regex.Pattern

public object SoundcloudStreamLinkHandlerFactory : LinkHandlerFactory() {

    private const val ON_URL_PATTERN = "^https?://on\\.soundcloud\\.com/[0-9a-zA-Z]+$"
    private val URL_PATTERN: Pattern = Pattern.compile(
        "^https?://(?:www\\.|m\\.)?" +
            "soundcloud\\.com/[0-9a-z_-]+" +
            "/(?!(?:tracks|albums|sets|reposts|followers|following)/?$)[0-9a-z_-]+/?(?:[#?].*)?$" +
            "|" + ON_URL_PATTERN,
    )

    private val API_URL_PATTERN: Pattern = Pattern.compile(
        "^https?://api-v2\\.soundcloud\\.com" +
            "/(tracks|albums|sets|reposts|followers|following)/([0-9a-z_-]+)/",
    )

    /** Group of the ID in [API_URL_PATTERN]. */
    private const val API_URL_ID_GROUP = 2

    @JvmStatic
    public fun getInstance(): SoundcloudStreamLinkHandlerFactory = this

    /** Only numeric IDs are placed in the URL resolved by the embed player. */
    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(id: String?): String {
        val trackId = SoundcloudParsingHelper.requireNumericId(id)
        return try {
            SoundcloudParsingHelper.resolveUrlWithEmbedPlayer("https://api.soundcloud.com/tracks/$trackId")
        } catch (e: IOException) {
            throw ParsingException("Could not get the track URL", e)
        } catch (e: ExtractionException) {
            throw ParsingException("Could not get the track URL", e)
        }
    }

    /**
     * The ID of a track link. For an API link, this is the ID in its path: the Java code
     * returned the first group of the pattern, i.e. the type of the item (`tracks`).
     */
    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getId(url: String?): String {
        if (url != null && Parser.isMatch(API_URL_PATTERN, url)) {
            return Parser.matchGroup(API_URL_PATTERN, url, API_URL_ID_GROUP)
        }
        Utils.checkUrl(URL_PATTERN, url)

        // the causes never quote the link: their messages are generic
        return try {
            SoundcloudParsingHelper.resolveIdWithWidgetApi(url)
        } catch (e: IOException) {
            throw ParsingException("Could not get the track ID", e)
        } catch (e: ExtractionException) {
            throw ParsingException("Could not get the track ID", e)
        } catch (e: IllegalArgumentException) {
            throw ParsingException("Could not get the track ID", e)
        }
    }

    @Throws(ParsingException::class)
    override fun onAcceptUrl(url: String?): Boolean = url != null &&
        url.length <= Utils.MAX_URL_LENGTH &&
        Parser.isMatch(URL_PATTERN, url.lowercase(Locale.ROOT))
}

package org.schabi.newpipe.extractor.services.soundcloud.linkHandler

import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper
import org.schabi.newpipe.extractor.utils.Parser
import org.schabi.newpipe.extractor.utils.Utils
import java.io.IOException
import java.util.Locale
import java.util.regex.Pattern

public object SoundcloudChannelLinkHandlerFactory : ListLinkHandlerFactory() {

    private val URL_PATTERN: Pattern = Pattern.compile(
        "^https?://(www\\.|m\\.)?soundcloud\\.com/[0-9a-z_-]+" +
            "(/((tracks|albums|sets|reposts|followers|following)/?)?)?([#?].*)?$",
    )

    @JvmStatic
    public fun getInstance(): SoundcloudChannelLinkHandlerFactory = this

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getId(url: String?): String {
        Utils.checkUrl(URL_PATTERN, url)

        // the causes never quote the link: their messages are generic
        return try {
            SoundcloudParsingHelper.resolveIdWithWidgetApi(url)
        } catch (e: IOException) {
            throw ParsingException("Could not get the channel ID", e)
        } catch (e: ExtractionException) {
            throw ParsingException("Could not get the channel ID", e)
        } catch (e: IllegalArgumentException) {
            throw ParsingException("Could not get the channel ID", e)
        }
    }

    /** Only numeric IDs are placed in the URL resolved by the embed player. */
    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
    ): String {
        val userId = SoundcloudParsingHelper.requireNumericId(id)
        return try {
            SoundcloudParsingHelper.resolveUrlWithEmbedPlayer("https://api.soundcloud.com/users/$userId")
        } catch (e: IOException) {
            throw ParsingException("Could not get the channel URL", e)
        } catch (e: ExtractionException) {
            throw ParsingException("Could not get the channel URL", e)
        }
    }

    override fun onAcceptUrl(url: String?): Boolean = url != null &&
        url.length <= Utils.MAX_URL_LENGTH &&
        Parser.isMatch(URL_PATTERN, url.lowercase(Locale.ROOT))
}

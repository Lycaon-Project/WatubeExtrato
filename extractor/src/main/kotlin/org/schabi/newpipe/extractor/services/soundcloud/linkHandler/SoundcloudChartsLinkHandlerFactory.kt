package org.schabi.newpipe.extractor.services.soundcloud.linkHandler

import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory
import org.schabi.newpipe.extractor.utils.Parser
import org.schabi.newpipe.extractor.utils.Utils
import java.util.Locale
import java.util.regex.Pattern

public object SoundcloudChartsLinkHandlerFactory : ListLinkHandlerFactory() {

    private val URL_PATTERN: Pattern =
        Pattern.compile("^https?://(www\\.|m\\.)?soundcloud\\.com/charts(/top|/new)?/?([#?].*)?$")

    @JvmStatic
    public fun getInstance(): SoundcloudChartsLinkHandlerFactory = this

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getId(url: String?): String = "New & hot"

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
    ): String = "https://soundcloud.com/charts/new"

    override fun onAcceptUrl(url: String?): Boolean = url != null &&
        url.length <= Utils.MAX_URL_LENGTH &&
        Parser.isMatch(URL_PATTERN, url.lowercase(Locale.ROOT))
}

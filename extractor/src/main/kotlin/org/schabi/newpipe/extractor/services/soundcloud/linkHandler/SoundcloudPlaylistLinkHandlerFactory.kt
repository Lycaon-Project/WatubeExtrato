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

public object SoundcloudPlaylistLinkHandlerFactory : ListLinkHandlerFactory() {

    private val URL_PATTERN: Pattern = Pattern.compile(
        "^https?://(www\\.|m\\.)?soundcloud\\.com/[0-9a-z_-]+" +
            "/sets/[0-9a-z_-]+/?([#?].*)?$",
    )

    @JvmStatic
    public fun getInstance(): SoundcloudPlaylistLinkHandlerFactory = this

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getId(url: String?): String {
        Utils.checkUrl(URL_PATTERN, url)

        // the link is not quoted (the Java code did): it may carry personal or tracking data
        return try {
            SoundcloudParsingHelper.resolveIdWithWidgetApi(url)
        } catch (e: IOException) {
            throw ParsingException("Could not get the playlist ID", e)
        } catch (e: ExtractionException) {
            throw ParsingException("Could not get the playlist ID", e)
        } catch (e: IllegalArgumentException) {
            throw ParsingException("Could not get the playlist ID", e)
        }
    }

    /** Only numeric IDs are placed in the URL resolved by the embed player. */
    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
    ): String {
        val playlistId = SoundcloudParsingHelper.requireNumericId(id)
        return try {
            SoundcloudParsingHelper.resolveUrlWithEmbedPlayer(
                "https://api.soundcloud.com/playlists/$playlistId",
            )
        } catch (e: IOException) {
            throw ParsingException("Could not get the playlist URL", e)
        } catch (e: ExtractionException) {
            throw ParsingException("Could not get the playlist URL", e)
        }
    }

    @Throws(ParsingException::class)
    override fun onAcceptUrl(url: String?): Boolean = url != null &&
        url.length <= Utils.MAX_URL_LENGTH &&
        Parser.isMatch(URL_PATTERN, url.lowercase(Locale.ROOT))
}

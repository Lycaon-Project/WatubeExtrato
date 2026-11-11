package org.schabi.newpipe.extractor.services.soundcloud.linkHandler

import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper.SOUNDCLOUD_API_V2_URL
import org.schabi.newpipe.extractor.utils.Utils
import java.io.IOException

public object SoundcloudSearchQueryHandlerFactory : SearchQueryHandlerFactory() {

    public const val TRACKS: String = "tracks"
    public const val USERS: String = "users"
    public const val PLAYLISTS: String = "playlists"
    public const val ALL: String = "all"

    public const val ITEMS_PER_PAGE: Int = 10

    @JvmStatic
    public fun getInstance(): SoundcloudSearchQueryHandlerFactory = this

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
    ): String {
        val query = id ?: throw ParsingException("No search query given")
        val path = when (contentFilter?.firstOrNull()) {
            TRACKS -> "/tracks"
            USERS -> "/users"
            PLAYLISTS -> "/playlists"
            // ALL and unknown filters
            else -> ""
        }

        return try {
            SOUNDCLOUD_API_V2_URL + "search" + path +
                "?q=" + Utils.encodeUrlUtf8(query) +
                "&client_id=" + SoundcloudParsingHelper.clientId() +
                "&limit=" + ITEMS_PER_PAGE + "&offset=0"
        } catch (e: ReCaptchaException) {
            throw ParsingException("ReCaptcha required", e)
        } catch (e: IOException) {
            throw ParsingException("Could not get client id", e)
        } catch (e: ExtractionException) {
            throw ParsingException("Could not get client id", e)
        }
    }

    override val availableContentFilter: Array<String>
        get() = arrayOf(
            ALL,
            TRACKS,
            USERS,
            PLAYLISTS,
        )
}

package org.schabi.newpipe.extractor.services.soundcloud.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.channel.ChannelExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper.SOUNDCLOUD_API_V2_URL
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper.getAllImagesFromArtworkOrAvatarUrl
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper.getAllImagesFromVisualUrl
import org.schabi.newpipe.extractor.services.soundcloud.linkHandler.SoundcloudChannelTabLinkHandlerFactory
import java.io.IOException

public open class SoundcloudChannelExtractor(service: StreamingService, linkHandler: ListLinkHandler) :
    ChannelExtractor(service, linkHandler) {

    private var userId: String? = null
    private lateinit var user: JsonObject

    /** Only a numeric user ID is placed in the API URL. */
    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        val id = SoundcloudParsingHelper.requireNumericId(linkHandler.id)
        val apiUrl = USERS_ENDPOINT + id + "?client_id=" + SoundcloudParsingHelper.clientId()

        val response = downloader.get(apiUrl, extractorLocalization)
            .also(SoundcloudParsingHelper::noteRefusedClientId)
            .responseBody()
        user = SoundcloudParsingHelper.parseObject(response)
        userId = id
    }

    @get:Throws(ParsingException::class)
    override val id: String
        get() = userId ?: linkHandler.id

    @get:Throws(ParsingException::class)
    override val name: String
        get() = user.getString("username") ?: throw ParsingException("Could not get name")

    override val avatars: List<Image>
        get() = getAllImagesFromArtworkOrAvatarUrl(user.getString("avatar_url"))

    /** The banner of the first visual; none when the user has no visual. */
    override val banners: List<Image>
        get() {
            val firstVisual = user.getObject("visuals").getArray("visuals").firstOrNull() as? JsonObject
            return getAllImagesFromVisualUrl(firstVisual?.getString("visual_url"))
        }

    override val feedUrl: String?
        get() = null

    override val subscriberCount: Long
        get() = user.getLong("followers_count", 0)

    override val description: String?
        get() = user.getString("description", "")

    override val parentChannelName: String?
        get() = ""

    override val parentChannelUrl: String?
        get() = ""

    override val parentChannelAvatars: List<Image>
        get() = emptyList()

    @get:Throws(ParsingException::class)
    override val isVerified: Boolean
        get() = user.getBoolean("verified")

    @get:Throws(ParsingException::class)
    override val tabs: List<ListLinkHandler>
        get() {
            val url = url
            val id = id
            return TABS.map { tab ->
                val tabUrl = url + SoundcloudChannelTabLinkHandlerFactory.getUrlSuffix(tab)
                ListLinkHandler(tabUrl, tabUrl, id, listOf(tab), "")
            }
        }

    private companion object {
        const val USERS_ENDPOINT = SOUNDCLOUD_API_V2_URL + "users/"

        val TABS = listOf(ChannelTabs.TRACKS, ChannelTabs.PLAYLISTS, ChannelTabs.ALBUMS, ChannelTabs.LIKES)
    }
}

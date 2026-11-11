package org.schabi.newpipe.extractor.services.soundcloud.extractors

import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.channel.ChannelInfoItemsCollector
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper.SOUNDCLOUD_API_V2_URL
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudService
import org.schabi.newpipe.extractor.subscription.SubscriptionExtractor
import org.schabi.newpipe.extractor.subscription.SubscriptionItem
import org.schabi.newpipe.extractor.utils.Utils.HTTPS
import org.schabi.newpipe.extractor.utils.Utils.replaceHttpWithHttps
import java.io.IOException

/**
 * Extract the "followings" from a user in SoundCloud.
 */
public open class SoundcloudSubscriptionExtractor(service: SoundcloudService) :
    SubscriptionExtractor(service, listOf(ContentSource.CHANNEL_URL)) {

    override val relatedUrl: String?
        get() = "https://soundcloud.com/you"

    /** Only a numeric user ID is placed in the API URL. */
    @Throws(IOException::class, ExtractionException::class)
    override fun fromChannelUrl(channelUrl: String?): List<SubscriptionItem> {
        if (channelUrl == null) {
            throw InvalidSourceException("Channel url is null")
        }

        val channelLHFactory = service.channelLHFactory ?: throw InvalidSourceException()
        val id = try {
            SoundcloudParsingHelper.requireNumericId(channelLHFactory.fromUrl(getUrlFrom(channelUrl)).id)
        } catch (e: ExtractionException) {
            throw InvalidSourceException(e)
        }

        val apiUrl = SOUNDCLOUD_API_V2_URL + "users/" + id + "/followings" + "?client_id=" +
            SoundcloudParsingHelper.clientId() + "&limit=200"
        val collector = ChannelInfoItemsCollector(service.serviceId)
        // ± 2000 is the limit of followings on SoundCloud, so this minimum should be enough
        SoundcloudParsingHelper.getUsersFromApiMinItems(MIN_FOLLOWINGS, collector, apiUrl)

        return toSubscriptionItems(collector.items)
    }

    private fun getUrlFrom(channelUrl: String): String {
        val fixedUrl = replaceHttpWithHttps(channelUrl) ?: channelUrl
        return when {
            fixedUrl.startsWith(HTTPS) -> channelUrl
            !fixedUrl.contains("soundcloud.com/") -> "https://soundcloud.com/$fixedUrl"
            else -> HTTPS + fixedUrl
        }
    }

    // ////////////////////////////////////////////////////////////////////////
    // Utils
    // ////////////////////////////////////////////////////////////////////////

    private fun toSubscriptionItems(items: List<ChannelInfoItem>): List<SubscriptionItem> =
        items.map { SubscriptionItem(it.serviceId, it.url, it.name) }

    private companion object {
        const val MIN_FOLLOWINGS = 2500
    }
}

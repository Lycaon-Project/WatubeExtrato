package org.schabi.newpipe.extractor.services.soundcloud.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.channel.ChannelInfoItemExtractor
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper.getAllImagesFromArtworkOrAvatarUrl

public open class SoundcloudChannelInfoItemExtractor(private val itemObject: JsonObject) :
    ChannelInfoItemExtractor {

    @get:Throws(ParsingException::class)
    override val name: String
        get() = itemObject.getString("username") ?: throw ParsingException("Could not get name")

    /** The permalink upgraded to https; refused when it is not a SoundCloud link. */
    @get:Throws(ParsingException::class)
    override val url: String
        get() = SoundcloudParsingHelper.requireSoundcloudWebUrl(itemObject.getString("permalink_url"))

    override val thumbnails: List<Image>
        get() = getAllImagesFromArtworkOrAvatarUrl(itemObject.getString("avatar_url"))

    override val subscriberCount: Long
        get() = itemObject.getLong("followers_count")

    override val streamCount: Long
        get() = itemObject.getLong("track_count")

    override val isVerified: Boolean
        get() = itemObject.getBoolean("verified")

    override val description: String?
        get() = itemObject.getString("description", "")
}

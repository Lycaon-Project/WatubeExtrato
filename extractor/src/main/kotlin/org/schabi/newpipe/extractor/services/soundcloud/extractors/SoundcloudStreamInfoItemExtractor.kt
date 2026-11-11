package org.schabi.newpipe.extractor.services.soundcloud.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper.getAllImagesFromArtworkOrAvatarUrl
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper.getAllImagesFromTrackObject
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper.parseDate
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor
import org.schabi.newpipe.extractor.stream.StreamType

public open class SoundcloudStreamInfoItemExtractor(private val itemObject: JsonObject) :
    StreamInfoItemExtractor {

    /** The permalink upgraded to https; refused when it is not a SoundCloud link. */
    @get:Throws(ParsingException::class)
    override val url: String
        get() = SoundcloudParsingHelper.requireSoundcloudWebUrl(itemObject.getString("permalink_url"))

    @get:Throws(ParsingException::class)
    override val name: String
        get() = itemObject.getString("title") ?: throw ParsingException("Could not get name")

    override val duration: Long
        get() = itemObject.getLong("duration") / MILLIS_PER_SECOND

    override val uploaderName: String?
        get() = itemObject.getObject("user").getString("username")

    /** The uploader permalink upgraded to https, or `null` when it is not a SoundCloud link. */
    override val uploaderUrl: String?
        get() = SoundcloudParsingHelper.soundcloudWebUrlOrNull(
            itemObject.getObject("user").getString("permalink_url"),
        )

    override val uploaderAvatars: List<Image>
        get() = getAllImagesFromArtworkOrAvatarUrl(itemObject.getObject("user").getString("avatar_url"))

    @get:Throws(ParsingException::class)
    override val isUploaderVerified: Boolean
        get() = itemObject.getObject("user").getBoolean("verified")

    override val textualUploadDate: String?
        get() = itemObject.getString("created_at")

    @get:Throws(ParsingException::class)
    override val uploadDate: DateWrapper?
        get() = parseDate(textualUploadDate)

    override val viewCount: Long
        get() = itemObject.getLong("playback_count")

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = getAllImagesFromTrackObject(itemObject)

    override val streamType: StreamType
        get() = StreamType.AUDIO_STREAM

    override val isAd: Boolean
        get() = false

    private companion object {
        const val MILLIS_PER_SECOND = 1000L
    }
}

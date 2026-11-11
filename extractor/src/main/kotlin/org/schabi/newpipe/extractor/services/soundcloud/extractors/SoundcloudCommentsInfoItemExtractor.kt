package org.schabi.newpipe.extractor.services.soundcloud.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.comments.CommentsInfoItemExtractor
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper.getAllImagesFromArtworkOrAvatarUrl
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper.parseDate
import org.schabi.newpipe.extractor.stream.Description

/**
 * @param url the URL of the comments page this comment comes from (the API URL, which holds the
 * client ID of SoundCloud)
 */
public open class SoundcloudCommentsInfoItemExtractor(
    private val json: JsonObject,
    override val url: String,
) : CommentsInfoItemExtractor {

    override val commentId: String?
        get() = json.getLong("id").toString()

    override val commentText: Description
        get() = Description.of(json.getString("body"), Description.Type.PLAIN_TEXT)

    override val uploaderName: String?
        get() = json.getObject("user").getString("username")

    override val uploaderAvatars: List<Image>
        get() = getAllImagesFromArtworkOrAvatarUrl(json.getObject("user").getString("avatar_url"))

    @get:Throws(ParsingException::class)
    override val isUploaderVerified: Boolean
        get() = json.getObject("user").getBoolean("verified")

    // convert milliseconds to seconds
    override val streamPosition: Int
        get() = json.getInt("timestamp") / MILLIS_PER_SECOND

    /** The uploader permalink upgraded to https, or `null` when it is not a SoundCloud link. */
    override val uploaderUrl: String?
        get() = SoundcloudParsingHelper.soundcloudWebUrlOrNull(json.getObject("user").getString("permalink_url"))

    override val textualUploadDate: String?
        get() = json.getString("created_at")

    @get:Throws(ParsingException::class)
    override val uploadDate: DateWrapper?
        get() = parseDate(textualUploadDate)

    @get:Throws(ParsingException::class)
    override val name: String
        get() = json.getObject("user").getString("permalink") ?: throw ParsingException("Could not get name")

    override val thumbnails: List<Image>
        get() = getAllImagesFromArtworkOrAvatarUrl(json.getObject("user").getString("avatar_url"))

    private companion object {
        const val MILLIS_PER_SECOND = 1000
    }
}

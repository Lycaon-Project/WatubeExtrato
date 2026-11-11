package org.schabi.newpipe.extractor.services.soundcloud.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItemExtractor
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper.getAllImagesFromArtworkOrAvatarUrl

public open class SoundcloudPlaylistInfoItemExtractor(private val itemObject: JsonObject) :
    PlaylistInfoItemExtractor {

    @get:Throws(ParsingException::class)
    override val name: String
        get() = itemObject.getString("title") ?: throw ParsingException("Could not get name")

    /** The permalink upgraded to https; refused when it is not a SoundCloud link. */
    @get:Throws(ParsingException::class)
    override val url: String
        get() = SoundcloudParsingHelper.requireSoundcloudWebUrl(itemObject.getString("permalink_url"))

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() {
            // Over-engineering at its finest
            itemObject.getString(ARTWORK_URL_KEY)?.takeIf { it.isNotEmpty() }?.let {
                return getAllImagesFromArtworkOrAvatarUrl(it)
            }

            // Look for artwork URL inside the track list (entries that are not objects are
            // skipped, where the Java code gave up the whole list on a failed cast)
            for (track in itemObject.getArray("tracks")) {
                val trackObject = track as? JsonObject ?: continue

                // First look for track artwork URL
                trackObject.getString(ARTWORK_URL_KEY)?.takeIf { it.isNotEmpty() }?.let {
                    return getAllImagesFromArtworkOrAvatarUrl(it)
                }

                // Then look for track creator avatar URL
                trackObject.getObject(USER_KEY).getString(AVATAR_URL_KEY)?.takeIf { it.isNotEmpty() }?.let {
                    return getAllImagesFromArtworkOrAvatarUrl(it)
                }
            }

            // Last resort, use user avatar URL
            return getAllImagesFromArtworkOrAvatarUrl(itemObject.getObject(USER_KEY).getString(AVATAR_URL_KEY))
        }

    @get:Throws(ParsingException::class)
    override val uploaderName: String?
        get() = itemObject.getObject(USER_KEY).getString("username")

    /** The uploader permalink upgraded to https, or `null` when it is not a SoundCloud link. */
    override val uploaderUrl: String?
        get() = SoundcloudParsingHelper.soundcloudWebUrlOrNull(
            itemObject.getObject(USER_KEY).getString("permalink_url"),
        )

    override val isUploaderVerified: Boolean
        get() = itemObject.getObject(USER_KEY).getBoolean("verified")

    override val streamCount: Long
        get() = itemObject.getLong("track_count")

    private companion object {
        const val USER_KEY = "user"
        const val AVATAR_URL_KEY = "avatar_url"
        const val ARTWORK_URL_KEY = "artwork_url"
    }
}

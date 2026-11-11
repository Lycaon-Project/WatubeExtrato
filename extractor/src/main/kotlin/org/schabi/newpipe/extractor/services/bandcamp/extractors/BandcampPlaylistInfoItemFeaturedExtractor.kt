package org.schabi.newpipe.extractor.services.bandcamp.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItemExtractor
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.getImagesFromImageId

public open class BandcampPlaylistInfoItemFeaturedExtractor(private val featuredStory: JsonObject) :
    PlaylistInfoItemExtractor {

    override val uploaderName: String?
        get() = featuredStory.getString("band_name")

    override val uploaderUrl: String?
        get() = null

    override val isUploaderVerified: Boolean
        get() = false

    override val streamCount: Long
        get() = featuredStory.getInt("num_streamable_tracks").toLong()

    @get:Throws(ParsingException::class)
    override val name: String
        get() = featuredStory.getString("album_title") ?: throw ParsingException("Could not get name")

    /** The album link given by Bandcamp, upgraded to https; refused when it is not a web link. */
    @get:Throws(ParsingException::class)
    override val url: String
        get() = BandcampExtractorHelper.requireSecureUrl(featuredStory.getString("item_url"))

    override val thumbnails: List<Image>
        get() = if (featuredStory.has("art_id")) {
            getImagesFromImageId(featuredStory.getLong("art_id"), true)
        } else {
            getImagesFromImageId(featuredStory.getLong("item_art_id"), true)
        }
}

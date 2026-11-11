package org.schabi.newpipe.extractor.services.bandcamp.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItemExtractor

public open class BandcampAlbumInfoItemExtractor(
    private val albumInfoItem: JsonObject,
    override val uploaderUrl: String?,
) : PlaylistInfoItemExtractor {

    @get:Throws(ParsingException::class)
    override val name: String
        get() = albumInfoItem.getString("title") ?: throw ParsingException("Could not get name")

    /** Translated by one request to the mobile API (see [BandcampExtractorHelper.getStreamUrlFromIds]). */
    @get:Throws(ParsingException::class)
    override val url: String
        get() = BandcampExtractorHelper.getStreamUrlFromIds(
            albumInfoItem.getLong("band_id"),
            albumInfoItem.getLong("item_id"),
            albumInfoItem.getString("item_type"),
        )

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = BandcampExtractorHelper.getImagesFromImageId(albumInfoItem.getLong("art_id"), true)

    @get:Throws(ParsingException::class)
    override val uploaderName: String?
        get() = albumInfoItem.getString("band_name")

    override val isUploaderVerified: Boolean
        get() = false

    override val streamCount: Long
        get() = ListExtractor.ITEM_COUNT_UNKNOWN
}

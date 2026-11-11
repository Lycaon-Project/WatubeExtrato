package org.schabi.newpipe.extractor.services.bandcamp.extractors.streaminfoitem

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.getImagesFromImageId

public open class BandcampDiscographStreamInfoItemExtractor(
    private val discograph: JsonObject,
    uploaderUrl: String?,
) : BandcampStreamInfoItemExtractor(uploaderUrl) {

    override val uploaderName: String?
        get() = discograph.getString("band_name")

    @get:Throws(ParsingException::class)
    override val name: String
        get() = discograph.getString("title") ?: throw ParsingException("Could not get name")

    /** Translated by one request to the mobile API (see [BandcampExtractorHelper.getStreamUrlFromIds]). */
    @get:Throws(ParsingException::class)
    override val url: String
        get() = BandcampExtractorHelper.getStreamUrlFromIds(
            discograph.getLong("band_id"),
            discograph.getLong("item_id"),
            discograph.getString("item_type"),
        )

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = getImagesFromImageId(discograph.getLong("art_id"), true)

    override val duration: Long
        get() = -1
}

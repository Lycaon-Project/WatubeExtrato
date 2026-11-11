package org.schabi.newpipe.extractor.services.bandcamp.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.comments.CommentsInfoItemExtractor
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.getImagesFromImageId
import org.schabi.newpipe.extractor.stream.Description

public open class BandcampCommentsInfoItemExtractor(
    private val review: JsonObject,
    override val url: String,
) : CommentsInfoItemExtractor {

    @get:Throws(ParsingException::class)
    override val name: String
        get() = commentText.content

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = uploaderAvatars

    @get:Throws(ParsingException::class)
    override val commentText: Description
        get() = Description.of(review.getString("why"), Description.Type.PLAIN_TEXT)

    @get:Throws(ParsingException::class)
    override val uploaderName: String?
        get() = review.getString("name")

    override val uploaderAvatars: List<Image>
        get() = getImagesFromImageId(review.getLong("image_id"), false)
}

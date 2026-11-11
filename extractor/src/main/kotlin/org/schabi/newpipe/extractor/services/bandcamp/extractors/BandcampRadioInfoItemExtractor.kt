// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

package org.schabi.newpipe.extractor.services.bandcamp.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.BASE_URL
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.getImagesFromImageId
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.parseDate
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor
import org.schabi.newpipe.extractor.stream.StreamType

public open class BandcampRadioInfoItemExtractor(radioShow: JsonObject) : StreamInfoItemExtractor {

    private val show: JsonObject = radioShow

    override val duration: Long
        /* Duration is only present in the more detailed information that has to be queried
        separately. Therefore, over 300 queries would be needed every time the kiosk is opened if we
        were to display the real value. */
        // return query(show.getInt("id")).getLong("audio_duration");
        get() = 0

    override val shortDescription: String?
        get() = show.getString("desc")

    override val textualUploadDate: String?
        get() = show.getString("date")

    @get:Throws(ParsingException::class)
    override val uploadDate: DateWrapper?
        get() = textualUploadDate?.let { parseDate(it) }

    @get:Throws(ParsingException::class)
    override val name: String
        get() = show.getString("subtitle") ?: throw ParsingException("Could not get name")

    override val url: String
        get() = "$BASE_URL/?show=${show.getInt("id")}"

    override val thumbnails: List<Image>
        get() = getImagesFromImageId(show.getLong("image_id"), false)

    override val streamType: StreamType
        get() = StreamType.AUDIO_STREAM

    override val viewCount: Long
        get() = -1

    // The "title" field contains the title of the series, e.g. "Bandcamp Weekly".
    override val uploaderName: String?
        get() = show.getString("title")

    override val uploaderUrl: String?
        get() = ""

    @get:Throws(ParsingException::class)
    override val isUploaderVerified: Boolean
        get() = false

    override val isAd: Boolean
        get() = false
}

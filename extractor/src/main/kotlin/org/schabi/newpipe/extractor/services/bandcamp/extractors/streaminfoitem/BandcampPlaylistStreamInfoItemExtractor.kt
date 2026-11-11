// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

package org.schabi.newpipe.extractor.services.bandcamp.extractors.streaminfoitem

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper
import java.io.IOException

public open class BandcampPlaylistStreamInfoItemExtractor private constructor(
    private val track: JsonObject,
    uploaderUrl: String?,
    private val service: StreamingService?,
    private val substituteCovers: List<Image>,
) : BandcampStreamInfoItemExtractor(uploaderUrl) {

    public constructor(track: JsonObject, uploaderUrl: String?, service: StreamingService) :
        this(track, uploaderUrl, service, emptyList())

    public constructor(track: JsonObject, uploaderUrl: String?, substituteCovers: List<Image>) :
        this(track, uploaderUrl, null, substituteCovers)

    @get:Throws(ParsingException::class)
    override val name: String
        get() = track.getString("title") ?: throw ParsingException("Could not get name")

    /**
     * The album's root followed by the track's relative link. The Java code returned `null`
     * when the track had no link; a missing link or a result that is not a web link is now a
     * [ParsingException] (item URLs are never null).
     */
    @get:Throws(ParsingException::class)
    override val url: String
        get() = trackUrlOrNull() ?: throw ParsingException("Could not get track url")

    private fun trackUrlOrNull(): String? {
        val relativeUrl = track.getString("title_link") ?: return null
        val albumUrl = uploaderUrl ?: return null
        return BandcampExtractorHelper.secureUrlOrNull(albumUrl + relativeUrl)
    }

    override val duration: Long
        get() = track.getLong("duration")

    /* Tracks can have an individual artist name, but it is not included in the
     * given JSON.
     */
    override val uploaderName: String?
        get() = ""

    /**
     * Each track can have its own cover art. Therefore, unless a substitute is provided,
     * the thumbnail is extracted using a stream extractor (one request per track).
     */
    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() {
            val trackUrl = trackUrlOrNull()
            if (substituteCovers.isNotEmpty() || trackUrl == null || service == null) {
                return substituteCovers
            }
            return try {
                val extractor = service.getStreamExtractor(trackUrl)
                extractor.fetchPage()
                extractor.thumbnails
            } catch (e: ExtractionException) {
                throw ParsingException("Could not download cover art location", e)
            } catch (e: IOException) {
                throw ParsingException("Could not download cover art location", e)
            }
        }
}

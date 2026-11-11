// Created by Fynn Godau 2021, licensed GNU GPL version 3 or later

package org.schabi.newpipe.extractor.services.bandcamp.extractors

import org.jsoup.nodes.Element
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItemExtractor
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.getImagesFromImageUrl

/**
 * Extracts recommended albums from tracks' website
 */
public open class BandcampRelatedPlaylistInfoItemExtractor(private val relatedAlbum: Element) :
    PlaylistInfoItemExtractor {

    @get:Throws(ParsingException::class)
    override val name: String
        get() = relatedAlbum.getElementsByClass("release-title").text()

    /** The album link of the page, upgraded to https; refused when it is not a web link. */
    @get:Throws(ParsingException::class)
    override val url: String
        get() = BandcampExtractorHelper.requireSecureUrl(
            relatedAlbum.getElementsByClass("album-link").attr("abs:href"),
        )

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = getImagesFromImageUrl(relatedAlbum.getElementsByClass("album-art").attr("src"))

    @get:Throws(ParsingException::class)
    override val uploaderName: String?
        get() = relatedAlbum.getElementsByClass("by-artist").text().replace("by ", "")

    @get:Throws(ParsingException::class)
    override val uploaderUrl: String?
        get() = null

    @get:Throws(ParsingException::class)
    override val isUploaderVerified: Boolean
        get() = false

    @get:Throws(ParsingException::class)
    override val streamCount: Long
        get() = -1
}

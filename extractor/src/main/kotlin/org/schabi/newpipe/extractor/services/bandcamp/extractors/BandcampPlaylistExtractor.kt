package org.schabi.newpipe.extractor.services.bandcamp.extractors

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.safety.Safelist
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.PaidContentException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonArray
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonParserException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.getImagesFromImageId
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.getImagesFromImageUrl
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampStreamExtractor.Companion.getAlbumInfoJson
import org.schabi.newpipe.extractor.services.bandcamp.extractors.streaminfoitem.BandcampPlaylistStreamInfoItemExtractor
import org.schabi.newpipe.extractor.stream.Description
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector
import org.schabi.newpipe.extractor.utils.JsonUtils.getJsonData
import java.io.IOException

public open class BandcampPlaylistExtractor(service: StreamingService, linkHandler: ListLinkHandler) :
    PlaylistExtractor(service, linkHandler) {

    private lateinit var document: Document
    private lateinit var albumJson: JsonObject
    private lateinit var trackInfo: JsonArray
    private var albumName: String? = null

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        val html = downloader.get(linkHandler.url).responseBody()
        document = Jsoup.parse(html)
        albumJson = getAlbumInfoJson(html)
        trackInfo = albumJson.getArray("trackinfo")

        albumName = try {
            getJsonData(html, "data-embed").getString("album_title")
        } catch (e: JsonParserException) {
            throw ParsingException("Faulty JSON; page likely does not contain album data", e)
        } catch (e: ArrayIndexOutOfBoundsException) {
            throw ParsingException("JSON does not exist", e)
        }

        if (trackInfo.isEmpty()) {
            // Albums without trackInfo need to be purchased before they can be played
            throw PaidContentException("Album needs to be purchased")
        }
    }

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = if (albumJson.isNull("art_id")) {
            emptyList()
        } else {
            getImagesFromImageId(albumJson.getLong("art_id"), true)
        }

    @get:Throws(ParsingException::class)
    override val uploaderUrl: String
        // https: (/) (/) * .bandcamp.com (/) and leave out the rest
        get() = BandcampExtractorHelper.uploaderUrlOf(url)

    @get:Throws(ParsingException::class)
    override val uploaderName: String
        get() = albumJson.getString("artist") ?: throw ParsingException("Could not get uploader name")

    override val uploaderAvatars: List<Image>
        get() = getImagesFromImageUrl(
            document.getElementsByClass("band-photo").firstOrNull()?.attr("src"),
        )

    @get:Throws(ParsingException::class)
    override val isUploaderVerified: Boolean
        get() = false

    override val streamCount: Long
        get() = trackInfo.size.toLong()

    @get:Throws(ParsingException::class)
    override val description: Description
        get() {
            val tInfo = document.getElementById("trackInfo")
                ?: throw ParsingException("Could not find trackInfo in document")
            val about = tInfo.getElementsByClass("tralbum-about").firstOrNull()
            val credits = tInfo.getElementsByClass("tralbum-credits").firstOrNull()
            val license: Element? = document.getElementById("license")
            if (about == null && credits == null && license == null) {
                return Description.EMPTY_DESCRIPTION
            }
            val html = buildString {
                about?.let { append(it.html()) }
                credits?.let { append(it.html()) }
                license?.let { append(it.html()) }
            }
            // written by the artist: only basic formatting and web links are kept, never a script,
            // an event handler or a javascript: link
            return Description.of(Jsoup.clean(html, Safelist.basic()), Description.Type.HTML)
        }

    @get:Throws(IOException::class, ExtractionException::class)
    override val initialPage: InfoItemsPage<StreamInfoItem>
        get() {
            val collector = StreamInfoItemsCollector(serviceId)
            // Both are the same for every track: read once instead of once per track
            val albumUploaderUrl = uploaderUrl
            val loadEveryCover = trackInfo.size < MAXIMUM_INDIVIDUAL_COVER_ARTS
            val albumCovers = if (loadEveryCover) emptyList() else thumbnails

            for (i in 0 until trackInfo.size) {
                val track = trackInfo.getObject(i)

                if (loadEveryCover) {
                    // Load cover art of every track individually
                    collector.commit(BandcampPlaylistStreamInfoItemExtractor(track, albumUploaderUrl, service))
                } else {
                    // Pretend every track has the same cover art as the album
                    collector.commit(
                        BandcampPlaylistStreamInfoItemExtractor(track, albumUploaderUrl, albumCovers),
                    )
                }
            }

            return InfoItemsPage(collector, null)
        }

    /** There is only one page: an empty page is returned (the Java code returned `null`). */
    override fun getPage(page: Page?): InfoItemsPage<StreamInfoItem> = InfoItemsPage.emptyPage()

    @get:Throws(ParsingException::class)
    override val name: String
        get() = albumName ?: throw ParsingException("Could not get name")

    private companion object {
        /**
         * An arbitrarily chosen number above which cover arts won't be fetched individually for
         * each track; instead, it will be assumed that every track has the same cover art as the
         * album, which is not always the case.
         */
        private const val MAXIMUM_INDIVIDUAL_COVER_ARTS = 10
    }
}

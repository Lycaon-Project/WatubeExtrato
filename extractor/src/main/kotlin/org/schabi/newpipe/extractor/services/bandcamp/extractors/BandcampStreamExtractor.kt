// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

package org.schabi.newpipe.extractor.services.bandcamp.extractors

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.PaidContentException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonParserException
import org.schabi.newpipe.extractor.linkhandler.LinkHandler
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItemsCollector
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.getImagesFromImageId
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.getImagesFromImageUrl
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.parseDate
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.Description
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.extractor.stream.VideoStream
import org.schabi.newpipe.extractor.utils.JsonUtils
import org.schabi.newpipe.extractor.utils.Utils
import java.io.IOException
import java.util.Collections

public open class BandcampStreamExtractor(service: StreamingService, linkHandler: LinkHandler) :
    StreamExtractor(service, linkHandler) {

    private lateinit var albumJson: JsonObject
    private lateinit var current: JsonObject
    private lateinit var document: Document

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        val html = downloader.get(linkHandler.url).responseBody()
        document = Jsoup.parse(html)
        albumJson = getAlbumInfoJson(html)
        current = albumJson.getObject("current")

        val trackInfo = albumJson.getArray("trackinfo")
        if (trackInfo.size > 1) {
            // In this case, we are actually viewing an album page!
            throw ExtractionException("Page is actually an album, not a track")
        }

        if (trackInfo.getObject(0).isNull("file")) {
            throw PaidContentException("This track is not available without being purchased")
        }
    }

    @get:Throws(ParsingException::class)
    override val name: String
        get() = current.getString("title") ?: throw ParsingException("Could not get name")

    @get:Throws(ParsingException::class)
    override val uploaderUrl: String
        // https: (/) (/) * .bandcamp.com (/) and leave out the rest
        get() = BandcampExtractorHelper.uploaderUrlOf(url)

    /** The URL given by Bandcamp, upgraded to https; refused when it is not a web link. */
    @get:Throws(ParsingException::class)
    override val url: String
        get() = BandcampExtractorHelper.requireSecureUrl(albumJson.getString("url"))

    @get:Throws(ParsingException::class)
    override val uploaderName: String
        get() = albumJson.getString("artist") ?: throw ParsingException("Could not get uploader name")

    override val textualUploadDate: String?
        get() = current.getString("publish_date")

    @get:Throws(ParsingException::class)
    override val uploadDate: DateWrapper?
        get() = textualUploadDate?.let { parseDate(it) }

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = if (albumJson.isNull("art_id")) {
            emptyList()
        } else {
            getImagesFromImageId(albumJson.getLong("art_id"), true)
        }

    override val uploaderAvatars: List<Image>
        get() = getImagesFromImageUrl(
            document.getElementsByClass("band-photo").firstOrNull()?.attr("src"),
        )

    override val description: Description
        get() {
            val s = Utils.nonEmptyAndNullJoin(
                "\n\n",
                current.getString("about"),
                current.getString("lyrics"),
                current.getString("credits"),
            )
            return Description.of(s, Description.Type.PLAIN_TEXT)
        }

    /**
     * The 128 kbit/s MP3 file of the track, empty when its URL is missing or is not a web link
     * (the Java code failed with an exception in that case).
     */
    @get:Throws(IOException::class, ExtractionException::class)
    override val audioStreams: List<AudioStream>
        get() {
            val fileUrl = albumJson.getArray("trackinfo")
                .getObject(0)
                .getObject("file")
                .getString("mp3-128")
            val secureUrl = BandcampExtractorHelper.secureUrlOrNull(fileUrl) ?: return emptyList()
            return listOf(
                AudioStream.Builder()
                    .setId("mp3-128")
                    .setContent(secureUrl, true)
                    .setMediaFormat(MediaFormat.MP3)
                    .setAverageBitrate(MP3_BITRATE)
                    .build(),
            )
        }

    @get:Throws(ParsingException::class)
    override val length: Long
        get() = albumJson.getArray("trackinfo").getObject(0).getDouble("duration").toLong()

    @get:Throws(IOException::class, ExtractionException::class)
    override val videoStreams: List<VideoStream>
        get() = Collections.emptyList()

    @get:Throws(IOException::class, ExtractionException::class)
    override val videoOnlyStreams: List<VideoStream>
        get() = Collections.emptyList()

    override val streamType: StreamType
        get() = StreamType.AUDIO_STREAM

    @get:Throws(IOException::class, ExtractionException::class)
    override val relatedItems: PlaylistInfoItemsCollector?
        get() {
            val collector = PlaylistInfoItemsCollector(serviceId)
            for (element in document.getElementsByClass("recommended-album")) {
                collector.commit(BandcampRelatedPlaylistInfoItemExtractor(element))
            }
            return collector
        }

    override val category: String
        // Get first tag from html, which is the artist's Genre
        get() = document.getElementsByClass("tralbum-tags")
            .asSequence()
            .flatMap { it.getElementsByClass("tag").asSequence() }
            .firstOrNull()
            ?.text()
            .orEmpty()

    /*
    Tests resulted in this mapping of ints to licence:
    https://cloud.disroot.org/s/ZTWBxbQ9fKRmRWJ/preview (screenshot from a Bandcamp artist's
    account)
    */
    override val licence: String
        get() = when (current.getInt("license_type")) {
            1 -> "All rights reserved ©"
            2 -> "CC BY-NC-ND 3.0"
            3 -> "CC BY-NC-SA 3.0"
            4 -> "CC BY-NC 3.0"
            5 -> "CC BY-ND 3.0"
            6 -> "CC BY 3.0"
            8 -> "CC BY-SA 3.0"
            else -> "Unknown"
        }

    override val tags: List<String>
        get() = document.getElementsByAttributeValue("itemprop", "keywords").map { it.text() }

    public companion object {
        private const val MP3_BITRATE = 128

        /**
         * Get the JSON that contains album's metadata from page
         *
         * @param html Website
         * @return Album metadata JSON
         * @throws ParsingException In case of a faulty website
         */
        @JvmStatic
        @Throws(ParsingException::class)
        public fun getAlbumInfoJson(html: String): JsonObject = try {
            JsonUtils.getJsonData(html, "data-tralbum")
        } catch (e: JsonParserException) {
            throw ParsingException("Faulty JSON; page likely does not contain album data", e)
        } catch (e: ArrayIndexOutOfBoundsException) {
            throw ParsingException("JSON does not exist", e)
        }
    }
}

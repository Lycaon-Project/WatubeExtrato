package org.schabi.newpipe.extractor.services.bandcamp.extractors

import org.jsoup.Jsoup
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.Image.ResolutionLevel
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ContentNotSupportedException
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonParser
import org.schabi.newpipe.extractor.json.JsonParserException
import org.schabi.newpipe.extractor.linkhandler.LinkHandler
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItemsCollector
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.BASE_API_URL
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.BASE_URL
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.getImageUrl
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.getImagesFromImageId
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.Description
import org.schabi.newpipe.extractor.stream.StreamSegment
import java.io.IOException
import java.util.Collections

public open class BandcampRadioStreamExtractor(service: StreamingService, linkHandler: LinkHandler) :
    BandcampStreamExtractor(service, linkHandler) {

    private lateinit var showInfo: JsonObject

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        // The ID of a radio link is the show number (see BandcampStreamLinkHandlerFactory)
        val showId = id.toIntOrNull() ?: throw ParsingException("Invalid radio show ID")
        showInfo = query(showId)
    }

    @get:Throws(ParsingException::class)
    override val name: String
        /* Select "subtitle" and not "audio_title", as the latter would cause a lot of
         * items to show the same title, e.g. "Bandcamp Weekly".
         */
        get() = showInfo.getString("subtitle") ?: throw ParsingException("Could not get name")

    @get:Throws(ContentNotSupportedException::class)
    override val uploaderUrl: String
        get() = throw ContentNotSupportedException("Fan pages are not supported")

    @get:Throws(ParsingException::class)
    override val url: String
        get() = linkHandler.url

    @get:Throws(ParsingException::class)
    override val uploaderName: String
        get() = Jsoup.parse(showInfo.getString("image_caption").orEmpty())
            .getElementsByTag("a")
            .firstOrNull()
            ?.text()
            ?: throw ParsingException("Could not get uploader name")

    override val textualUploadDate: String?
        get() = showInfo.getString("published_date")

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = getImagesFromImageId(showInfo.getLong("show_image_id"), false)

    override val uploaderAvatars: List<Image>
        get() = listOf(
            // literal size: checked against the file name by ImageHardcodedSizeTest
            Image(
                "$BASE_URL/img/buttons/bandcamp-button-circle-whitecolor-512.png",
                512,
                512,
                ResolutionLevel.MEDIUM,
            ),
        )

    override val description: Description
        get() = Description.of(showInfo.getString("desc"), Description.Type.PLAIN_TEXT)

    override val length: Long
        get() = showInfo.getLong("audio_duration")

    /** The MP3 and Opus files of the show; a file whose URL is not a web link is left out. */
    override val audioStreams: List<AudioStream>
        get() {
            val audioStreams = ArrayList<AudioStream>(2)
            val streams = showInfo.getObject("audio_stream")

            BandcampExtractorHelper.secureUrlOrNull(streams.getString(MP3_128))?.let {
                audioStreams.add(
                    AudioStream.Builder()
                        .setId(MP3_128)
                        .setContent(it, true)
                        .setMediaFormat(MediaFormat.MP3)
                        .setAverageBitrate(MP3_BITRATE)
                        .build(),
                )
            }

            BandcampExtractorHelper.secureUrlOrNull(streams.getString(OPUS_LO))?.let {
                audioStreams.add(
                    AudioStream.Builder()
                        .setId(OPUS_LO)
                        .setContent(it, true)
                        .setMediaFormat(MediaFormat.OPUS)
                        .setAverageBitrate(OPUS_BITRATE)
                        .build(),
                )
            }

            return audioStreams
        }

    @get:Throws(ParsingException::class)
    override val streamSegments: List<StreamSegment>
        get() {
            val tracks = showInfo.getArray("tracks")
            val segments = ArrayList<StreamSegment>(tracks.size)
            for (track in tracks) {
                if (track !is JsonObject) {
                    continue
                }
                val segment = StreamSegment(track.getString("title"), track.getInt("timecode"))
                // "track art" is the track's album cover
                segment.previewUrl = getImageUrl(track.getLong("track_art_id"), true)
                segment.channelName = track.getString("artist")
                segments.add(segment)
            }
            return segments
        }

    // Contrary to other Bandcamp streams, radio streams don't have a license
    override val licence: String
        get() = ""

    // Contrary to other Bandcamp streams, radio streams don't have categories
    override val category: String
        get() = ""

    // Contrary to other Bandcamp streams, radio streams don't have tags
    override val tags: List<String>
        get() = Collections.emptyList()

    // Contrary to other Bandcamp streams, radio streams don't have related items
    override val relatedItems: PlaylistInfoItemsCollector?
        get() = null

    public companion object {
        private const val OPUS_LO = "opus-lo"
        private const val MP3_128 = "mp3-128"
        private const val MP3_BITRATE = 128
        private const val OPUS_BITRATE = 100

        @JvmStatic
        @Throws(ParsingException::class)
        internal fun query(id: Int): JsonObject {
            val response = try {
                BandcampExtractorHelper.requireDownloader()
                    .get("$BASE_API_URL/bcweekly/1/get?id=$id")
                    .responseBody()
            } catch (e: IOException) {
                throw ParsingException("could not get show data", e)
            } catch (e: ReCaptchaException) {
                throw ParsingException("could not get show data", e)
            }
            return try {
                JsonParser.`object`().from(response)
            } catch (e: JsonParserException) {
                throw ParsingException("could not get show data", e)
            }
        }
    }
}

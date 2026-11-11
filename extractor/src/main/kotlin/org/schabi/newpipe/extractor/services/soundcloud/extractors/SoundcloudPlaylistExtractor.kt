package org.schabi.newpipe.extractor.services.soundcloud.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonParser
import org.schabi.newpipe.extractor.json.JsonParserException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper.SOUNDCLOUD_API_V2_URL
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper.getAllImagesFromArtworkOrAvatarUrl
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper.getAvatarUrl
import org.schabi.newpipe.extractor.stream.Description
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector
import java.io.IOException

public open class SoundcloudPlaylistExtractor(service: StreamingService, linkHandler: ListLinkHandler) :
    PlaylistExtractor(service, linkHandler) {

    private var playlistId: String? = null
    private lateinit var playlist: JsonObject

    /** Only a numeric playlist ID is placed in the API URL. */
    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        val id = SoundcloudParsingHelper.requireNumericId(linkHandler.id)
        val apiUrl = SOUNDCLOUD_API_V2_URL + "playlists/" + id + "?client_id=" +
            SoundcloudParsingHelper.clientId() + "&representation=compact"

        val response = downloader.get(apiUrl, extractorLocalization)
            .also(SoundcloudParsingHelper::noteRefusedClientId)
            .responseBody()
        playlist = SoundcloudParsingHelper.parseObject(response)
        playlistId = id
    }

    @get:Throws(ParsingException::class)
    override val id: String
        get() = playlistId ?: linkHandler.id

    @get:Throws(ParsingException::class)
    override val name: String
        get() = playlist.getString("title") ?: throw ParsingException("Could not get name")

    override val thumbnails: List<Image>
        get() {
            val artworkUrl = playlist.getString("artwork_url")
            if (!artworkUrl.isNullOrEmpty()) {
                return getAllImagesFromArtworkOrAvatarUrl(artworkUrl)
            }

            // If the thumbnail is null or empty, traverse the items list and get a valid one
            // If it also fails, return an empty list (the initial page needs no request)
            return initialPage.items
                .firstNotNullOfOrNull { item -> item.thumbnails.takeIf { it.isNotEmpty() } }
                ?: emptyList()
        }

    override val uploaderUrl: String
        get() = SoundcloudParsingHelper.getUploaderUrl(playlist)

    override val uploaderName: String
        get() = SoundcloudParsingHelper.getUploaderName(playlist)

    override val uploaderAvatars: List<Image>
        get() = getAllImagesFromArtworkOrAvatarUrl(getAvatarUrl(playlist))

    @get:Throws(ParsingException::class)
    override val isUploaderVerified: Boolean
        get() = playlist.getObject("user").getBoolean("verified")

    override val streamCount: Long
        get() = playlist.getLong("track_count")

    @get:Throws(ParsingException::class)
    override val description: Description
        get() = Description.of(playlist.getString("description"), Description.Type.PLAIN_TEXT)

    override val initialPage: InfoItemsPage<StreamInfoItem>
        get() {
            val streamInfoItemsCollector = StreamInfoItemsCollector(serviceId)
            val ids = ArrayList<String>()

            for (track in playlist.getArray("tracks")) {
                if (track !is JsonObject) {
                    continue
                }
                // i.e. if full info is available
                if (track.has("title")) {
                    streamInfoItemsCollector.commit(SoundcloudStreamInfoItemExtractor(track))
                } else {
                    ids.add(track.getLong("id").toString())
                }
            }

            return InfoItemsPage(streamInfoItemsCollector, Page(ids))
        }

    /** Only numeric track IDs are placed in the API URL: they are checked before the request. */
    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<StreamInfoItem> {
        val pageIds = page?.ids
        require(!pageIds.isNullOrEmpty()) { "Page doesn't contain IDs" }

        val currentIds: List<String>
        val nextIds: List<String>?
        if (pageIds.size <= STREAMS_PER_REQUESTED_PAGE) {
            // Fetch every remaining stream, there are less than the max
            currentIds = pageIds
            nextIds = null
        } else {
            currentIds = pageIds.subList(0, STREAMS_PER_REQUESTED_PAGE)
            nextIds = pageIds.subList(STREAMS_PER_REQUESTED_PAGE, pageIds.size)
        }
        val currentNumericIds = currentIds.map {
            SoundcloudParsingHelper.requireNumericId(it).toLongOrNull() ?: throw ParsingException("Invalid track ID")
        }

        val currentPageUrl = SOUNDCLOUD_API_V2_URL + "tracks?client_id=" +
            SoundcloudParsingHelper.clientId() + "&ids=" + currentIds.joinToString(",")

        val collector = StreamInfoItemsCollector(serviceId)
        val response = SoundcloudParsingHelper.requireDownloader()
            .get(currentPageUrl, extractorLocalization)
            .also(SoundcloudParsingHelper::noteRefusedClientId)
            .responseBody()

        val tracks = try {
            JsonParser.array().from(response)
        } catch (e: JsonParserException) {
            throw ParsingException("Could not parse json response", e)
        }
        // Response may not contain tracks in the same order as currentIds.
        // The streams are displayed in the order which is used in currentIds on SoundCloud.
        val idToTrack = HashMap<Long, JsonObject>()
        for (track in tracks) {
            if (track is JsonObject) {
                idToTrack[track.getLong("id")] = track
            }
        }
        for (id in currentNumericIds) {
            // the ID is not quoted in the message
            val track = idToTrack[id] ?: throw ParsingException("Could not parse json response")
            collector.commit(SoundcloudStreamInfoItemExtractor(track))
        }

        return InfoItemsPage(collector, Page(nextIds))
    }

    private companion object {
        const val STREAMS_PER_REQUESTED_PAGE = 15
    }
}

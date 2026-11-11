// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

package org.schabi.newpipe.extractor.services.bandcamp.extractors

import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonArray
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonParser
import org.schabi.newpipe.extractor.json.JsonParserException
import org.schabi.newpipe.extractor.kiosk.KioskExtractor
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItemsCollector
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.BASE_API_URL
import java.io.IOException
import java.nio.charset.StandardCharsets

public open class BandcampFeaturedExtractor(
    streamingService: StreamingService,
    listLinkHandler: ListLinkHandler,
    kioskId: String,
) : KioskExtractor<PlaylistInfoItem>(streamingService, listLinkHandler, kioskId) {

    private lateinit var json: JsonObject

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        val response = this.downloader.postWithContentTypeJson(
            FEATURED_API_URL,
            emptyMap(),
            "{\"platform\":\"\",\"version\":0}".toByteArray(StandardCharsets.UTF_8),
        ).responseBody()
        json = try {
            JsonParser.`object`().from(response)
        } catch (e: JsonParserException) {
            throw ParsingException("Could not parse Bandcamp featured API response", e)
        }
    }

    @get:Throws(ParsingException::class)
    override val name: String
        get() = KIOSK_FEATURED

    @get:Throws(IOException::class, ExtractionException::class)
    override val initialPage: InfoItemsPage<PlaylistInfoItem>
        get() {
            val featuredStories = json.getObject("feed_content")
                .getObject("stories")
                .getArray("featured")

            return extractItems(featuredStories)
        }

    private fun extractItems(featuredStories: JsonArray): InfoItemsPage<PlaylistInfoItem> {
        val c = PlaylistInfoItemsCollector(serviceId)

        for (i in 0 until featuredStories.size) {
            val featuredStory = featuredStories.getObject(i)

            if (featuredStory.isNull("album_title")) {
                // Is not an album, ignore
                continue
            }

            c.commit(BandcampPlaylistInfoItemFeaturedExtractor(featuredStory))
        }

        // An empty answer has no last story to continue from (the Java code failed on it)
        val lastFeaturedStory = featuredStories.lastOrNull() as? JsonObject
        return InfoItemsPage(c, lastFeaturedStory?.let { getNextPageFrom(it) })
    }

    /**
     * Next Page can be generated from metadata of last featured story
     *
     * The story type is placed in the URL: a value that is not a plain word gives no next page.
     */
    private fun getNextPageFrom(lastFeaturedStory: JsonObject): Page? {
        val lastStoryDate = lastFeaturedStory.getLong("story_date")
        val lastStoryId = lastFeaturedStory.getLong("ntid")
        val lastStoryType = lastFeaturedStory.getString("story_type")
            ?.takeIf { STORY_TYPE.matches(it) }
            ?: return null
        return Page(
            "$MORE_FEATURED_API_URL?story_groups=featured" +
                ":$lastStoryDate:$lastStoryType:$lastStoryId",
        )
    }

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<PlaylistInfoItem> {
        // Only pages built by this extractor are requested, never another host
        val pageUrl = page?.url
        if (pageUrl == null || !pageUrl.startsWith("$MORE_FEATURED_API_URL?")) {
            throw ParsingException("Invalid Bandcamp featured page")
        }

        val response = try {
            JsonParser.`object`().from(downloader.get(pageUrl).responseBody())
        } catch (e: JsonParserException) {
            throw ParsingException("Could not parse Bandcamp featured API response", e)
        }

        return extractItems(response.getObject("stories").getArray("featured"))
    }

    public companion object {
        public const val KIOSK_FEATURED: String = "Featured"
        public const val FEATURED_API_URL: String = "$BASE_API_URL/mobile/24/bootstrap_data"
        public const val MORE_FEATURED_API_URL: String = "$BASE_API_URL/mobile/24/feed_older_logged_out"

        private val STORY_TYPE = Regex("[A-Za-z0-9_-]{1,32}")
    }
}

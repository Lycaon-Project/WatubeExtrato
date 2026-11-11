package org.schabi.newpipe.extractor.services.bandcamp.extractors

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.comments.CommentsExtractor
import org.schabi.newpipe.extractor.comments.CommentsInfoItem
import org.schabi.newpipe.extractor.comments.CommentsInfoItemsCollector
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.json.JsonArray
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonWriter
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.BASE_API_URL
import org.schabi.newpipe.extractor.utils.JsonUtils
import java.io.IOException
import java.nio.charset.StandardCharsets

public open class BandcampCommentsExtractor(service: StreamingService, linkHandler: ListLinkHandler) :
    CommentsExtractor(service, linkHandler) {

    private lateinit var document: Document

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        document = Jsoup.parse(downloader.get(linkHandler.url).responseBody())
    }

    @get:Throws(IOException::class, ExtractionException::class)
    override val initialPage: InfoItemsPage<CommentsInfoItem>
        get() {
            val collector = CommentsInfoItemsCollector(serviceId)

            val dataBlob = document.getElementById("collectors-data")?.attr("data-blob")
                ?: throw ParsingException("Could not get reviews")
            val collectorsData = JsonUtils.toJsonObject(dataBlob)
            val reviews = collectorsData.getArray("reviews")

            commitReviews(collector, reviews)

            if (!collectorsData.getBoolean("more_reviews_available")) {
                return InfoItemsPage(collector, null)
            }

            val trackId = trackId()
            val token = getNextPageToken(reviews)
            return InfoItemsPage(collector, Page(listOf(trackId, token)))
        }

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<CommentsInfoItem> {
        val pageIds = page?.ids
        require(pageIds != null && pageIds.size >= 2) { "Page doesn't contain IDs" }

        val collector = CommentsInfoItemsCollector(serviceId)
        val trackId = pageIds[0]
        val token = pageIds[1]
        // Sent to Bandcamp: a page that did not come from this extractor is refused
        if (!TRACK_ID.matches(trackId) || token.isEmpty() || token.length > MAX_TOKEN_LENGTH) {
            throw ParsingException("Invalid reviews page")
        }
        val reviewsData = fetchReviewsData(trackId, token)
        val reviews = reviewsData.getArray("results")

        commitReviews(collector, reviews)

        if (!reviewsData.getBoolean("more_available")) {
            return InfoItemsPage(collector, null)
        }

        return InfoItemsPage(collector, Page(listOf(trackId, getNextPageToken(reviews))))
    }

    @Throws(ParsingException::class)
    private fun commitReviews(collector: CommentsInfoItemsCollector, reviews: JsonArray) {
        val pageUrl = url
        for (review in reviews) {
            if (review is JsonObject) {
                collector.commit(BandcampCommentsInfoItemExtractor(review, pageUrl))
            }
        }
    }

    @Throws(ParsingException::class)
    private fun fetchReviewsData(trackId: String, token: String): JsonObject {
        val body = JsonWriter.string().`object`()
            .value("tralbum_type", "t")
            .value("tralbum_id", trackId)
            .value("token", token)
            .value("count", REVIEWS_PER_PAGE)
            .array("exclude_fan_ids").end()
            .end().done().toByteArray(StandardCharsets.UTF_8)
        val response = try {
            downloader.postWithContentTypeJson(REVIEWS_API_URL, emptyMap(), body).responseBody()
        } catch (e: IOException) {
            throw ParsingException("Could not fetch reviews", e)
        } catch (e: ReCaptchaException) {
            throw ParsingException("Could not fetch reviews", e)
        }
        return JsonUtils.toJsonObject(response)
    }

    /** The token of the last review, which tells Bandcamp where the next page starts. */
    @Throws(ParsingException::class)
    private fun getNextPageToken(reviews: JsonArray): String =
        reviews.lastOrNull { it is JsonObject }
            ?.let { (it as JsonObject).getString("token") }
            ?: throw ParsingException("Could not get token")

    @Throws(ParsingException::class)
    private fun trackId(): String {
        val content = document.selectFirst("meta[name=bc-page-properties]")?.attr("content")
            ?: throw ParsingException("Could not get track ID")
        return JsonUtils.toJsonObject(content).getLong("item_id").toString()
    }

    @get:Throws(ExtractionException::class)
    override val isCommentsDisabled: Boolean
        get() = BandcampExtractorHelper.isRadioUrl(url)

    private companion object {
        private const val REVIEWS_API_URL = "$BASE_API_URL/tralbumcollectors/2/reviews"
        private const val REVIEWS_PER_PAGE = 7
        private const val MAX_TOKEN_LENGTH = 1024
        private val TRACK_ID = Regex("\\d{1,20}")
    }
}

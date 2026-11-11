package org.schabi.newpipe.extractor.services.soundcloud.extractors

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
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper
import java.io.IOException

public open class SoundcloudCommentsExtractor(service: StreamingService, uiHandler: ListLinkHandler) :
    CommentsExtractor(service, uiHandler) {

    @get:Throws(ExtractionException::class, IOException::class)
    override val initialPage: InfoItemsPage<CommentsInfoItem>
        get() = getPage(url)

    @Throws(ExtractionException::class, IOException::class)
    override fun getPage(page: Page?): InfoItemsPage<CommentsInfoItem> {
        val pageUrl = page?.url
        if (pageUrl.isNullOrEmpty()) {
            return InfoItemsPage.emptyPage()
        }
        return getPage(pageUrl)
    }

    /** Only https links to SoundCloud are requested. */
    @Throws(ParsingException::class, IOException::class, ReCaptchaException::class)
    private fun getPage(url: String): InfoItemsPage<CommentsInfoItem> {
        val response = SoundcloudParsingHelper.requireDownloader()
            .get(SoundcloudParsingHelper.requireApiUrl(url))
            .also(SoundcloudParsingHelper::noteRefusedClientId)
        val json = SoundcloudParsingHelper.parseObject(response.responseBody())

        val collector = CommentsInfoItemsCollector(serviceId)

        collectStreamsFrom(collector, json.getArray("collection"))
        // a next page that is not a https link to SoundCloud is ignored
        val nextHref = json.getString("next_href")?.takeIf { SoundcloudParsingHelper.isApiUrl(it) }
        return InfoItemsPage(collector, nextHref?.let { Page(it) })
    }

    override fun onFetchPage(downloader: Downloader) {
        // Nothing to fetch: every page is requested by getPage
    }

    /** Entries that are not objects are skipped, where the Java code failed on a cast. */
    @Throws(ParsingException::class)
    private fun collectStreamsFrom(collector: CommentsInfoItemsCollector, entries: JsonArray) {
        val url = url
        for (comment in entries) {
            if (comment is JsonObject) {
                collector.commit(SoundcloudCommentsInfoItemExtractor(comment, url))
            }
        }
    }
}

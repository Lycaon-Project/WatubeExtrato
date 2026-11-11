package org.schabi.newpipe.extractor.services.soundcloud.extractors

import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList.SoundCloud
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.HttpResponseException
import org.schabi.newpipe.extractor.kiosk.KioskExtractor
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper.SOUNDCLOUD_API_V2_URL
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector
import java.io.IOException

public open class SoundcloudChartsExtractor(
    service: StreamingService,
    linkHandler: ListLinkHandler,
    kioskId: String,
) : KioskExtractor<StreamInfoItem>(service, linkHandler, kioskId) {

    private var initialFetchNextPageUrl: String? = null
    private lateinit var initialFetchCollector: StreamInfoItemsCollector

    @Throws(ExtractionException::class, IOException::class)
    override fun onFetchPage(downloader: Downloader) {
        // Check if already run
        if (initialFetchNextPageUrl != null) {
            return
        }

        initialFetchCollector = StreamInfoItemsCollector(serviceId)

        val apiUrl = SOUNDCLOUD_API_V2_URL + "charts" +
            "?genre=soundcloud:genres:all-music" +
            "&client_id=" + SoundcloudParsingHelper.clientId() +
            "&kind=trending"

        // the region comes from the fixed list of supported countries
        val contentCountry = SoundCloud.contentCountry
        val apiUrlWithRegion = if (service.supportedCountries.contains(contentCountry)) {
            apiUrl + "&region=soundcloud:regions:" + contentCountry.countryCode
        } else {
            apiUrl
        }

        initialFetchNextPageUrl = try {
            SoundcloudParsingHelper.getStreamsFromApi(initialFetchCollector, apiUrlWithRegion, true)
        } catch (_: IOException) {
            fetchWithoutRegion(apiUrl)
        } catch (_: HttpResponseException) {
            fetchWithoutRegion(apiUrl)
        }
    }

    /**
     * Request to other region may be geo-restricted.
     * See https://github.com/TeamNewPipe/NewPipeExtractor/issues/537.
     * We retry without the specified region.
     */
    @Throws(ExtractionException::class, IOException::class)
    private fun fetchWithoutRegion(apiUrl: String): String =
        SoundcloudParsingHelper.getStreamsFromApi(initialFetchCollector, apiUrl, true)

    override val name: String
        get() = id

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<StreamInfoItem> {
        val pageUrl = page?.url
        require(!pageUrl.isNullOrEmpty()) { "Page doesn't contain an URL" }

        val collector = StreamInfoItemsCollector(serviceId)
        val nextPageUrl = SoundcloudParsingHelper.getStreamsFromApi(collector, pageUrl, true)

        return InfoItemsPage(collector, Page(nextPageUrl))
    }

    @get:Throws(IOException::class, ExtractionException::class)
    override val initialPage: InfoItemsPage<StreamInfoItem>
        get() = InfoItemsPage(initialFetchCollector, Page(initialFetchNextPageUrl))
}

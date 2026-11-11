/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.extractors.kiosk

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonWriter
import org.schabi.newpipe.extractor.kiosk.KioskExtractor
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.services.youtube.InnertubeClientRequestInfo
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getClientHeaders
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getOriginReferrerHeaders
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getThumbnailsFromInfoItem
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getValidJsonResponseBody
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.prepareJsonBuilder
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeChannelLinkHandlerFactory
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeStreamLinkHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.extractor.utils.JsonUtils
import java.io.IOException
import java.time.DateTimeException
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Base class parsing responses from YouTube Charts for all trending video charts.
 *
 * Note: YouTube Charts is not officially supported in all YouTube supported countries (there are
 * fewer countries in the `LAUNCHED_CHART_COUNTRIES` array of YouTube Charts' HTML responses than in
 * the YouTube country selector).
 *
 * For some trends, some videos are still returned in unsupported countries, even if there are
 * fewer than in a supported country, for others an HTTP 400 error is returned saying
 * `Request contains an invalid argument.`.
 *
 * Sealed: only the kiosks of this package extend it.
 */
public sealed class YoutubeChartsBaseKioskExtractor(
    streamingService: StreamingService,
    linkHandler: ListLinkHandler,
    kioskId: String,
    protected val chartType: String,
) : KioskExtractor<StreamInfoItem>(streamingService, linkHandler, kioskId) {

    protected var browseResponse: JsonObject? = null

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        val localization = extractorLocalization
        val contentCountry = extractorContentCountry
        val innertubeClientRequestInfo = InnertubeClientRequestInfo.ofWebMusicAnalyticsChartsClient()

        val body = JsonWriter.string(
            prepareJsonBuilder(localization, contentCountry, innertubeClientRequestInfo, null)
                .value("browseId", "FEmusic_analytics_charts_home")
                .value(
                    "query",
                    "perspective=CHART_DETAILS&chart_params_country_code=" + contentCountry.countryCode +
                        "&chart_params_chart_type=" + chartType,
                )
                .done(),
        ).toByteArray(Charsets.UTF_8)

        val headers = HashMap(getOriginReferrerHeaders("https://charts.youtube.com"))
        headers.putAll(
            getClientHeaders(
                innertubeClientRequestInfo.clientInfo.clientId,
                innertubeClientRequestInfo.clientInfo.clientVersion,
            ),
        )
        browseResponse = JsonUtils.toJsonObject(
            getValidJsonResponseBody(
                downloader.postWithContentTypeJson(YT_CHARTS_ENDPOINT, headers, body, localization),
            ),
        )
    }

    @get:Throws(IOException::class, ExtractionException::class)
    override val initialPage: InfoItemsPage<StreamInfoItem>
        get() {
            val videos = checkNotNull(browseResponse) { "Page is not fetched. Make sure you call fetchPage()" }
                .getObject("contents")
                .getObject("sectionListRenderer")
                .getArray("contents")
                .getObject(0)
                .getObject("musicAnalyticsSectionRenderer")
                .getObject("content")
                .getArray("videos")
                .getObject(0)
                .getArray("videoViews")
            val collector = StreamInfoItemsCollector(serviceId)
            // Chart entries are plain video objects, never ad renderers
            videos.filterIsInstance<JsonObject>().forEach { collector.commit(YoutubeChartsVideoInfoItemExtractor(it)) }
            return InfoItemsPage(collector, null)
        }

    // There is no continuation in charts
    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<StreamInfoItem> = InfoItemsPage.emptyPage()

    internal class YoutubeChartsVideoInfoItemExtractor(
        private val videoObject: JsonObject,
    ) : StreamInfoItemExtractor {

        // There are only video streams in YouTube Charts, at least for now
        override val streamType: StreamType
            get() = StreamType.VIDEO_STREAM

        override val isAd: Boolean
            get() = false

        override val duration: Long
            get() = videoObject.getInt("videoDuration", -1).toLong()

        // View counts are not returned, at least for now
        override val viewCount: Long
            get() = -1

        override val uploaderName: String?
            get() = videoObject.getString("channelName")

        @get:Throws(ParsingException::class)
        override val uploaderUrl: String?
            get() {
                val channelId = videoObject.getString("externalChannelId")
                if (channelId.isNullOrEmpty()) {
                    throw ParsingException("Could not get channel ID")
                }
                return YoutubeChannelLinkHandlerFactory.getInstance().getUrl("channel/$channelId")
            }

        // We do not have any info on this, at least for now
        override val isUploaderVerified: Boolean
            get() = false

        override val textualUploadDate: String?
            get() = null

        @get:Throws(ParsingException::class)
        override val uploadDate: DateWrapper
            get() {
                val releaseDate = videoObject.getObject("releaseDate")
                val localDate = try {
                    LocalDate.of(releaseDate.getInt("year"), releaseDate.getInt("month"), releaseDate.getInt("day"))
                } catch (e: DateTimeException) {
                    throw ParsingException("Could not get upload date", e)
                }
                // We request that times should be returned with 0 offset to UTC timezone in the
                // JSON body, but YouTube charts does it only in its HTTP headers
                val instant = localDate.atStartOfDay(ZoneOffset.UTC).toInstant()
                // We do not have more info than the release day, hence isApproximate=true
                return DateWrapper(instant, true)
            }

        @get:Throws(ParsingException::class)
        override val name: String
            get() = videoObject.getString("title") ?: throw ParsingException("Could not get name")

        @get:Throws(ParsingException::class)
        override val url: String
            get() = YoutubeStreamLinkHandlerFactory.getInstance().getUrl(videoObject.getString("id"))

        @get:Throws(ParsingException::class)
        override val thumbnails: List<Image>
            get() = getThumbnailsFromInfoItem(videoObject)
    }

    internal companion object {
        // Extracted from YouTube Charts' HTML, in the array named LAUNCHED_CHART_COUNTRIES
        internal val YT_CHARTS_SUPPORTED_COUNTRY_CODES: Set<String> = setOf(
            "AE", "AR", "AT", "AU", "BE", "BO", "BR", "CA", "CH", "CL", "CO", "CR", "CZ", "DE",
            "DK", "DO", "EC", "EE", "EG", "ES", "FI", "FR", "GB", "GT", "HN", "HU", "ID", "IE",
            "IL", "IN", "IS", "IT", "JP", "KE", "KR", "LU", "MX", "NG", "NI", "NL", "NO", "NZ",
            "PA", "PE", "PL", "PT", "PY", "RO", "RS", "RU", "SA", "SE", "SV", "TR", "TZ", "UA",
            "UG", "US", "UY", "ZA", "ZW",
        )

        internal val YT_CHARTS_ENDPOINT: String =
            "https://charts.youtube.com/youtubei/v1/browse?alt=json&" + DISABLE_PRETTY_PRINT_PARAMETER
    }
}

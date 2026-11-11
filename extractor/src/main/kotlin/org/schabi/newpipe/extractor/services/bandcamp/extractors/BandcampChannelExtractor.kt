// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

package org.schabi.newpipe.extractor.services.bandcamp.extractors

import org.jsoup.Jsoup
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.Image.Companion.HEIGHT_UNKNOWN
import org.schabi.newpipe.extractor.Image.Companion.WIDTH_UNKNOWN
import org.schabi.newpipe.extractor.Image.ResolutionLevel
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.channel.ChannelExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.json.JsonArray
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.linkhandler.ReadyChannelTabListLinkHandler
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.getArtistDetails
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.getImagesFromImageId
import org.schabi.newpipe.extractor.services.bandcamp.linkHandler.BandcampChannelTabLinkHandlerFactory
import java.io.IOException
import java.util.Collections

public open class BandcampChannelExtractor(service: StreamingService, linkHandler: ListLinkHandler) :
    ChannelExtractor(service, linkHandler) {

    private lateinit var channelInfo: JsonObject

    override val avatars: List<Image>
        get() = getImagesFromImageId(channelInfo.getLong("bio_image_id"), false)

    /** Read from the artist's web site, once: every read would download the page again. */
    @get:Throws(ParsingException::class)
    override val banners: List<Image>
        get() = cachedBanners ?: fetchBanners().also { cachedBanners = it }

    private var cachedBanners: List<Image>? = null

    @Throws(ParsingException::class)
    private fun fetchBanners(): List<Image> {
        /*
         * Mobile API does not return the header or not the correct header.
         * Therefore, we need to query the website
         */
        val siteUrl = BandcampExtractorHelper.artistUrlFromBandcamp(channelInfo.getString("bandcamp_url"))
        val html = try {
            downloader.get(siteUrl).responseBody()
        } catch (_: IOException) {
            // the cause is not kept: its message may name the artist's own domain
            throw ParsingException("Could not download artist web site")
        } catch (_: ReCaptchaException) {
            throw ParsingException("Could not download artist web site")
        }

        val customHeader = Jsoup.parse(html).getElementById("customHeader") ?: return emptyList()
        val banners = customHeader.getElementsByTag("img")
            .map { it.attr("src") }
            .filter { it.isNotEmpty() }
            // read from the artist's page: only https web links are kept
            .mapNotNull { BandcampExtractorHelper.secureUrlOrNull(it) }
            .map { Image(it, HEIGHT_UNKNOWN, WIDTH_UNKNOWN, ResolutionLevel.UNKNOWN) }
        return Collections.unmodifiableList(banners)
    }

    /**
     * Bandcamp discontinued their RSS feeds because it hadn't been used enough.
     */
    override val feedUrl: String?
        get() = null

    override val subscriberCount: Long
        get() = -1

    override val description: String?
        get() = channelInfo.getString("bio")

    override val parentChannelName: String?
        get() = null

    override val parentChannelUrl: String?
        get() = null

    override val parentChannelAvatars: List<Image>
        get() = emptyList()

    @get:Throws(ParsingException::class)
    override val isVerified: Boolean
        get() = false

    @get:Throws(ParsingException::class)
    override val tabs: List<ListLinkHandler>
        get() {
            val discography = channelInfo.getArray("discography")
            val builder = TabExtractorBuilder(discography)

            val tabs = ArrayList<ListLinkHandler>(2)

            var foundTrackItem = false
            var foundAlbumItem = false

            for (discographyItem in discography) {
                if (foundTrackItem && foundAlbumItem) {
                    break
                }

                if (discographyItem !is JsonObject) {
                    continue
                }

                val itemType = discographyItem.getString("item_type")

                if (!foundTrackItem && itemType == "track") {
                    foundTrackItem = true
                    tabs.add(tabLinkHandler(ChannelTabs.TRACKS, builder))
                }

                if (!foundAlbumItem && itemType == "album") {
                    foundAlbumItem = true
                    tabs.add(tabLinkHandler(ChannelTabs.ALBUMS, builder))
                }
            }

            return Collections.unmodifiableList(tabs)
        }

    @Throws(ParsingException::class)
    private fun tabLinkHandler(tab: String, builder: TabExtractorBuilder): ListLinkHandler =
        ReadyChannelTabListLinkHandler(
            url + BandcampChannelTabLinkHandlerFactory.getUrlSuffix(tab),
            id,
            tab,
            builder,
        )

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        channelInfo = getArtistDetails(id)
    }

    @get:Throws(ParsingException::class)
    override val name: String
        get() = channelInfo.getString("name") ?: throw ParsingException("Could not get name")

    private class TabExtractorBuilder(
        private val discography: JsonArray,
    ) : ReadyChannelTabListLinkHandler.ChannelTabExtractorBuilder {
        override fun build(service: StreamingService, linkHandler: ListLinkHandler): ChannelTabExtractor =
            BandcampChannelTabExtractor.fromDiscography(service, linkHandler, discography)
    }
}

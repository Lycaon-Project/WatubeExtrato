/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.peertube

import org.schabi.newpipe.extractor.Collector
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.Image.ResolutionLevel
import org.schabi.newpipe.extractor.InfoItemExtractor
import org.schabi.newpipe.extractor.InfoItemsCollector
import org.schabi.newpipe.extractor.MultiInfoItemsCollector
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.channel.ChannelInfoItemExtractor
import org.schabi.newpipe.extractor.channel.ChannelInfoItemsCollector
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonArray
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonParser
import org.schabi.newpipe.extractor.json.JsonParserException
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItemExtractor
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItemsCollector
import org.schabi.newpipe.extractor.services.peertube.extractors.PeertubeChannelInfoItemExtractor
import org.schabi.newpipe.extractor.services.peertube.extractors.PeertubePlaylistInfoItemExtractor
import org.schabi.newpipe.extractor.services.peertube.extractors.PeertubeSepiaStreamInfoItemExtractor
import org.schabi.newpipe.extractor.services.peertube.extractors.PeertubeStreamInfoItemExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector
import org.schabi.newpipe.extractor.utils.JsonUtils
import org.schabi.newpipe.extractor.utils.Utils
import java.io.IOException
import java.util.Collections
import java.util.regex.Pattern

public object PeertubeParsingHelper {
    public const val START_KEY: String = "start"
    public const val COUNT_KEY: String = "count"
    public const val ITEMS_PER_PAGE: Int = 12
    public const val START_PATTERN: String = "start=(\\d*)"

    private val START: Pattern = Pattern.compile(START_PATTERN)

    /** Items read from one answer of an instance, which chooses how many it sends. */
    private const val MAX_LIST_ITEMS = 500

    /**
     * @throws ContentNotAvailableException if the instance answered with an error; its text is
     * not quoted, as it comes from the network
     */
    @JvmStatic
    @Throws(ContentNotAvailableException::class)
    public fun validate(json: JsonObject) {
        if (!Utils.isBlank(json.getString("error"))) {
            throw ContentNotAvailableException("The PeerTube instance returned an error")
        }
    }

    /**
     * The page after [prevPageUrl], or `null` when [total] items have been reached or the
     * `start` value cannot be read (missing, too large).
     */
    @JvmStatic
    public fun getNextPage(prevPageUrl: String, total: Long): Page? {
        val matcher = START.matcher(prevPageUrl)
        if (!matcher.find()) {
            return null
        }
        val prevStart = matcher.group(1)
        if (Utils.isBlank(prevStart)) {
            return null
        }
        val start = prevStart.toLongOrNull() ?: return null
        // an overflow would restart the pagination from a negative value, endlessly
        if (start > Long.MAX_VALUE - ITEMS_PER_PAGE) {
            return null
        }
        val nextStart = start + ITEMS_PER_PAGE
        return if (nextStart >= total) {
            null
        } else {
            Page(prevPageUrl.replace("$START_KEY=$prevStart", "$START_KEY=$nextStart"))
        }
    }

    /**
     * Fetches the JSON page at [url] (built by the extractors from a checked instance address)
     * and checks that it is not an error.
     *
     * @param what what the page holds, for the error messages (e.g. "playlist info")
     */
    @Throws(IOException::class, ExtractionException::class)
    internal fun fetchJsonPage(downloader: Downloader, url: String, what: String): JsonObject {
        val body = downloader.get(url).responseBody()
        if (Utils.isBlank(body)) {
            throw ExtractionException("Unable to get PeerTube $what")
        }
        val json = try {
            JsonParser.`object`().from(body)
        } catch (e: JsonParserException) {
            // the parser's messages hold positions only, never the parsed text
            throw ParsingException("Could not parse json data for $what", e)
        }
        validate(json)
        return json
    }

    @JvmStatic
    @Throws(ParsingException::class)
    public fun collectItemsFrom(
        collector: InfoItemsCollector<*, *>,
        json: JsonObject,
        baseUrl: String,
    ) {
        collectItemsFrom(collector, json, baseUrl, false)
    }

    /**
     * Collect items from the given JSON object with the given collector.
     *
     * Supported info item types are streams with their Sepia variant, channels and playlists.
     * An item the collector cannot hold (e.g. a channel given to a stream collector) is skipped.
     *
     * @param collector the collector used to collect information
     * @param json      the JSON response to retrieve data from
     * @param baseUrl   the base URL of the instance
     * @param sepia     if we should use `PeertubeSepiaStreamInfoItemExtractor` to extract
     *                  streams or `PeertubeStreamInfoItemExtractor` otherwise
     */
    @JvmStatic
    @Throws(ParsingException::class)
    public fun collectItemsFrom(
        collector: InfoItemsCollector<*, *>,
        json: JsonObject,
        baseUrl: String,
        sepia: Boolean,
    ) {
        val contents = try {
            JsonUtils.getValue(json, "data") as? JsonArray
        } catch (e: ParsingException) {
            throw ParsingException("Unable to extract list info", e)
        } ?: throw ParsingException("Unable to extract list info")

        for (c in contents.take(MAX_LIST_ITEMS)) {
            if (c !is JsonObject) {
                continue
            }
            // PeerTube playlists have the stream info encapsulated in an "video" object
            val item = if (c.has("video")) c.getObject("video") else c
            val isPlaylistInfoItem = item.has("videosLength")
            val isChannelInfoItem = item.has("followersCount")

            val extractor: InfoItemExtractor = when {
                sepia -> PeertubeSepiaStreamInfoItemExtractor(item, baseUrl)
                isPlaylistInfoItem -> PeertubePlaylistInfoItemExtractor(item, baseUrl)
                isChannelInfoItem -> PeertubeChannelInfoItemExtractor(item, baseUrl)
                else -> PeertubeStreamInfoItemExtractor(item, baseUrl)
            }
            commit(collector, extractor)
        }
    }

    /**
     * Gives [extractor] to [collector] when the collector can hold it. The Java code relied on a
     * raw type, so a channel in the answer for a stream list crashed the extraction.
     */
    private fun commit(collector: InfoItemsCollector<*, *>, extractor: InfoItemExtractor) {
        when (collector) {
            is MultiInfoItemsCollector -> collector.commit(extractor)
            is StreamInfoItemsCollector ->
                (extractor as? StreamInfoItemExtractor)?.let(collector::commit)
            is ChannelInfoItemsCollector ->
                (extractor as? ChannelInfoItemExtractor)?.let(collector::commit)
            is PlaylistInfoItemsCollector ->
                (extractor as? PlaylistInfoItemExtractor)?.let(collector::commit)
            else -> {
                // another collector type: the caller vouches for the extractors it may hold
                @Suppress("UNCHECKED_CAST")
                (collector as Collector<*, InfoItemExtractor>).commit(extractor)
            }
        }
    }

    /**
     * Get avatars from a `ownerAccount` or a `videoChannel` [JsonObject].
     *
     * If the `avatars` [JsonArray] is present and non null or empty, avatars will be
     * extracted from this array using [getImagesFromAvatarOrBannerArray].
     *
     * If that's not the case, an avatar will extracted using the `avatar` [JsonObject].
     *
     * Note that only images for which paths are not null and not empty (and are absolute paths
     * on the instance) will be added to the unmodifiable [Image] list returned.
     *
     * @param baseUrl                          the base URL of the PeerTube instance
     * @param ownerAccountOrVideoChannelObject the `ownerAccount` or `videoChannel`
     *                                         [JsonObject]
     * @return an unmodifiable list of [Image]s, which may be empty but never null
     */
    @JvmStatic
    public fun getAvatarsFromOwnerAccountOrVideoChannelObject(
        baseUrl: String,
        ownerAccountOrVideoChannelObject: JsonObject,
    ): List<Image> = getImagesFromAvatarsOrBanners(
        baseUrl,
        ownerAccountOrVideoChannelObject,
        "avatars",
        "avatar",
    )

    /**
     * Get banners from a `ownerAccount` or a `videoChannel` [JsonObject].
     *
     * If the `banners` [JsonArray] is present and non null or empty, banners will be
     * extracted from this array using [getImagesFromAvatarOrBannerArray].
     *
     * If that's not the case, a banner will extracted using the `banner` [JsonObject].
     *
     * Note that only images for which paths are not null and not empty (and are absolute paths
     * on the instance) will be added to the unmodifiable [Image] list returned.
     *
     * @param baseUrl                          the base URL of the PeerTube instance
     * @param ownerAccountOrVideoChannelObject the `ownerAccount` or `videoChannel`
     *                                         [JsonObject]
     * @return an unmodifiable list of [Image]s, which may be empty but never null
     */
    @JvmStatic
    public fun getBannersFromAccountOrVideoChannelObject(
        baseUrl: String,
        ownerAccountOrVideoChannelObject: JsonObject,
    ): List<Image> = getImagesFromAvatarsOrBanners(
        baseUrl,
        ownerAccountOrVideoChannelObject,
        "banners",
        "banner",
    )

    /**
     * Get thumbnails from a playlist or a video item [JsonObject].
     *
     * PeerTube provides two thumbnails in its API: a low one, represented by the value of the
     * `thumbnailPath` key, and a medium one, represented by the value of the `previewPath` key.
     *
     * If a value is not null or empty (and is an absolute path on the instance), an [Image] will
     * be added to the list returned with the URL to the thumbnail (`baseUrl + value`), a height
     * and a width unknown and the corresponding resolution level (see above).
     *
     * @param baseUrl                   the base URL of the PeerTube instance
     * @param playlistOrVideoItemObject the playlist or the video item [JsonObject], which
     *                                  must not be null
     * @return an unmodifiable list of [Image]s, which is never null but can be empty
     */
    @JvmStatic
    public fun getThumbnailsFromPlaylistOrVideoItem(
        baseUrl: String,
        playlistOrVideoItemObject: JsonObject,
    ): List<Image> {
        val imageList = ArrayList<Image>(2)
        PeertubeLinkGuards.instanceUrlOrNull(baseUrl, playlistOrVideoItemObject.getString("thumbnailPath"))
            ?.let { imageList.add(unknownSizeImage(it, Image.WIDTH_UNKNOWN, ResolutionLevel.LOW)) }
        PeertubeLinkGuards.instanceUrlOrNull(baseUrl, playlistOrVideoItemObject.getString("previewPath"))
            ?.let { imageList.add(unknownSizeImage(it, Image.WIDTH_UNKNOWN, ResolutionLevel.MEDIUM)) }
        return Collections.unmodifiableList(imageList)
    }

    /**
     * Utility method to get avatars and banners from video channels and accounts from given name
     * keys.
     *
     * Only images for which paths are not null and not empty will be added to the unmodifiable
     * [Image] list returned and only the width of avatars or banners is provided by the API,
     * and so is the only image dimension known.
     *
     * @param baseUrl                          the base URL of the PeerTube instance
     * @param ownerAccountOrVideoChannelObject the `ownerAccount` or `videoChannel`
     *                                         [JsonObject]
     * @param jsonArrayName                    the key name of the [JsonArray] to which
     *                                         extract all images available (`avatars` or
     *                                         `banners`)
     * @param jsonObjectName                   the key name of the [JsonObject] to which
     *                                         extract a single image (`avatar` or
     *                                         `banner`), used as a fallback if the images
     *                                         [JsonArray] is null or empty
     * @return an unmodifiable list of [Image]s, which may be empty but never null
     */
    private fun getImagesFromAvatarsOrBanners(
        baseUrl: String,
        ownerAccountOrVideoChannelObject: JsonObject,
        jsonArrayName: String,
        jsonObjectName: String,
    ): List<Image> {
        val images = ownerAccountOrVideoChannelObject.getArray(jsonArrayName)
        if (images.isNotEmpty()) {
            return getImagesFromAvatarOrBannerArray(baseUrl, images)
        }

        val image = ownerAccountOrVideoChannelObject.getObject(jsonObjectName)
        val url = PeertubeLinkGuards.instanceUrlOrNull(baseUrl, image.getString("path"))
            ?: return emptyList()
        return listOf(unknownSizeImage(url, image.getInt("width", Image.WIDTH_UNKNOWN), ResolutionLevel.UNKNOWN))
    }

    /**
     * Get [Image]s from an `avatars` or a `banners` [JsonArray].
     *
     * Only images for which paths are not null and not empty will be added to the
     * unmodifiable [Image] list returned.
     *
     * Note that only the width of avatars or banners is provided by the API, and so only is the
     * only dimension known of images.
     *
     * @param baseUrl               the base URL of the PeerTube instance from which the
     *                              `avatarsOrBannersArray` [JsonArray] comes from
     * @param avatarsOrBannersArray an `avatars` or `banners` [JsonArray]
     * @return an unmodifiable list of [Image]s, which may be empty but never null
     */
    private fun getImagesFromAvatarOrBannerArray(
        baseUrl: String,
        avatarsOrBannersArray: JsonArray,
    ): List<Image> {
        val imageList = ArrayList<Image>(avatarsOrBannersArray.size)
        for (image in avatarsOrBannersArray) {
            if (image !is JsonObject) {
                continue
            }
            val url = PeertubeLinkGuards.instanceUrlOrNull(baseUrl, image.getString("path")) ?: continue
            imageList.add(unknownSizeImage(url, image.getInt("width", Image.WIDTH_UNKNOWN), ResolutionLevel.UNKNOWN))
        }
        return Collections.unmodifiableList(imageList)
    }

    private fun unknownSizeImage(url: String, width: Int, level: ResolutionLevel): Image =
        Image(url, Image.HEIGHT_UNKNOWN, width, level)
}

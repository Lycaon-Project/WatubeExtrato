/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.media_ccc.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.Image.ResolutionLevel
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.json.JsonArray
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonParser
import org.schabi.newpipe.extractor.json.JsonParserException
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.services.media_ccc.MediaCCCLinkGuards
import java.io.IOException
import java.util.Collections
import java.util.regex.Pattern

public object MediaCCCParsingHelper {

    // conference_slug/room_slug
    private val LIVE_STREAM_ID_PATTERN: Pattern = Pattern.compile("\\w+/\\w+")

    @Volatile
    private var liveStreams: JsonArray? = null

    /**
     * Check whether an id is a live stream id
     * @param id the `id` to check
     * @return returns `true` if the `id` is formatted like
     * `{conference_slug}/{room_slug}`; `false` otherwise
     */
    @JvmStatic
    public fun isLiveStreamId(id: String): Boolean = LIVE_STREAM_ID_PATTERN.matcher(id).find()

    /**
     * Get currently available live streams from
     * [https://streaming.media.ccc.de/streams/v2.json](https://streaming.media.ccc.de/streams/v2.json).
     * Use this method to cache requests, because they can get quite big.
     * TODO: implement better caching policy (max-age: 3 min)
     * @param downloader The downloader to use for making the request
     * @param localization The localization to be used. Will most likely be ignored.
     * @return [JsonArray] containing current conferences and info about their rooms and
     * streams.
     * @throws ExtractionException if the data could not be fetched or the retrieved data could not
     * be parsed to a [JsonArray]
     */
    @JvmStatic
    @Throws(ExtractionException::class)
    public fun getLiveStreams(downloader: Downloader, localization: Localization?): JsonArray {
        liveStreams?.let { return it }
        val streams = try {
            val site = downloader.get("https://streaming.media.ccc.de/streams/v2.json", localization).responseBody()
            JsonParser.array().from(site)
        } catch (_: IOException) {
            // the cause is not kept: its message may quote the request URL
            throw ExtractionException("Could not get live stream JSON.")
        } catch (_: ReCaptchaException) {
            throw ExtractionException("Could not get live stream JSON.")
        } catch (e: JsonParserException) {
            throw ExtractionException("Could not parse JSON.", e)
        }
        liveStreams = streams
        return streams
    }

    /**
     * Get an [Image] list from a given image logo URL.
     *
     * If the image URL is null or empty (or is not a web link), an empty list is returned;
     * otherwise, a singleton list is returned containing an [Image] with the image URL with its
     * height, width and resolution unknown.
     *
     * @param logoImageUrl a logo image URL, which can be null or empty
     * @return an unmodifiable list of [Image]s, which is always empty or a singleton
     */
    @JvmStatic
    public fun getImageListFromLogoImageUrl(logoImageUrl: String?): List<Image> {
        val url = MediaCCCLinkGuards.webUrlOrNull(logoImageUrl)
        if (url.isNullOrEmpty()) {
            return emptyList()
        }
        return listOf(Image(url, Image.HEIGHT_UNKNOWN, Image.WIDTH_UNKNOWN, ResolutionLevel.UNKNOWN))
    }

    /**
     * Get the [Image] list of thumbnails from a given stream item.
     *
     * MediaCCC API provides two thumbnails for a stream item: a `thumb_url` one, which is
     * medium quality and a `poster_url` one, which is high quality in most cases.
     *
     * @param streamItem a stream JSON item of MediaCCC's API, which must not be null
     * @return an unmodifiable list, which is never null but can be empty.
     */
    @JvmStatic
    public fun getThumbnailsFromStreamItem(streamItem: JsonObject): List<Image> =
        getThumbnailsFromObject(streamItem, "thumb_url", "poster_url")

    /**
     * Get the [Image] list of thumbnails from a given live stream item.
     *
     * MediaCCC API provides two URL thumbnails for a livestream item: a `thumb` one,
     * which should be medium quality and a `poster_url` one, which should be high quality.
     *
     * @param liveStreamItem a stream JSON item of MediaCCC's API, which must not be null
     * @return an unmodifiable list, which is never null but can be empty.
     */
    @JvmStatic
    public fun getThumbnailsFromLiveStreamItem(liveStreamItem: JsonObject): List<Image> =
        getThumbnailsFromObject(liveStreamItem, "thumb", "poster")

    /**
     * Utility method to get an [Image] list of thumbnails from a stream or a livestream.
     *
     * MediaCCC's API thumbnails come from two elements: a `thumb` element, which links to a
     * medium thumbnail and a `poster` element, which links to a high thumbnail.
     *
     * Thumbnails are only added if their URLs are not null or empty (and are web links).
     *
     * @param streamOrLivestreamItem a (live)stream JSON item of MediaCCC's API, which must not be
     * null
     * @param thumbUrlKey  the name of the `thumb` URL key
     * @param posterUrlKey the name of the `poster` URL key
     * @return an unmodifiable list, which is never null but can be empty.
     */
    private fun getThumbnailsFromObject(
        streamOrLivestreamItem: JsonObject,
        thumbUrlKey: String,
        posterUrlKey: String,
    ): List<Image> {
        val imageList = ArrayList<Image>(2)
        val thumbUrl = MediaCCCLinkGuards.webUrlOrNull(streamOrLivestreamItem.getString(thumbUrlKey))
        if (!thumbUrl.isNullOrEmpty()) {
            imageList.add(Image(thumbUrl, Image.HEIGHT_UNKNOWN, Image.WIDTH_UNKNOWN, ResolutionLevel.MEDIUM))
        }
        val posterUrl = MediaCCCLinkGuards.webUrlOrNull(streamOrLivestreamItem.getString(posterUrlKey))
        if (!posterUrl.isNullOrEmpty()) {
            imageList.add(Image(posterUrl, Image.HEIGHT_UNKNOWN, Image.WIDTH_UNKNOWN, ResolutionLevel.HIGH))
        }
        return Collections.unmodifiableList(imageList)
    }
}

// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

package org.schabi.newpipe.extractor.services.bandcamp.linkHandler

import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.json.JsonParserException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper
import org.schabi.newpipe.extractor.utils.JsonUtils
import java.io.IOException
import java.util.Locale

/**
 * Artist do have IDs that are useful
 */
public object BandcampChannelLinkHandlerFactory : ListLinkHandlerFactory() {

    /** Segments allowed after the artist domain. */
    private val ARTIST_PAGES = setOf("releases", "music", "album", "track")

    /** `https:`, the empty part between both slashes, the domain and an optional page. */
    private const val SEGMENTS_OF_ROOT = 3
    private const val SEGMENTS_OF_PAGE = 4
    private const val PAGE_SEGMENT = 3
    private const val DOMAIN_SEGMENT = 2

    @JvmStatic
    public fun getInstance(): BandcampChannelLinkHandlerFactory = this

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getId(url: String?): String {
        // Artists may use their own domain: only well-formed web links are ever requested
        if (BandcampExtractorHelper.parseHttpUrlOrNull(url) == null) {
            throw ParsingException("The given URL is not valid")
        }
        val response = try {
            BandcampExtractorHelper.requireDownloader()
                .get(BandcampExtractorHelper.toHttps(url.orEmpty()))
                .responseBody()
        } catch (_: IOException) {
            // the cause is not kept: its message may name the host of the link
            throw ParsingException("Download failed")
        } catch (_: ReCaptchaException) {
            throw ParsingException("Download failed")
        }

        // Use band data embedded in website to extract ID
        val bandData = try {
            JsonUtils.getJsonData(response, "data-band")
        } catch (e: JsonParserException) {
            throw ParsingException("Download failed", e)
        }
        return bandData.getLong("id").toString()
    }

    /**
     * Uses the mobile endpoint as a "translator" from id to url
     */
    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
    ): String {
        val artistDetails = BandcampExtractorHelper.getArtistDetails(id)
        if (artistDetails.getBoolean("error")) {
            throw ParsingException(
                "JSON does not contain a channel URL (invalid id?) or is otherwise invalid",
            )
        }
        return BandcampExtractorHelper.artistUrlFromBandcamp(artistDetails.getString("bandcamp_url"))
    }

    /**
     * Accepts only pages that lead to the root of an artist profile. External pages are accepted
     * once Bandcamp confirmed them (see [BandcampExtractorHelper.isArtistDomain]).
     */
    @Throws(ParsingException::class)
    override fun onAcceptUrl(url: String?): Boolean {
        if (url == null) {
            return false
        }
        val lowercaseUrl = url.lowercase(Locale.ROOT)

        // https: | | artist.bandcamp.com | releases - music - album - track ( | name)
        //  0      1           2                           3                    (4)
        // (trailing empty parts are dropped, as String.split did in the Java code)
        val splitUrl = lowercaseUrl.split('/').dropLastWhile { it.isEmpty() }

        // URL is too short
        if (splitUrl.size != SEGMENTS_OF_ROOT && splitUrl.size != SEGMENTS_OF_PAGE) {
            return false
        }

        // Must have "releases", "music", "album" or "track" as segment after URL or none at all
        if (splitUrl.size == SEGMENTS_OF_PAGE && splitUrl[PAGE_SEGMENT] !in ARTIST_PAGES) {
            return false
        }

        if (splitUrl[DOMAIN_SEGMENT] == "daily.bandcamp.com") {
            // Refuse links to daily.bandcamp.com as that is not an artist
            return false
        }

        // Test whether domain is supported
        return BandcampExtractorHelper.isArtistDomain(lowercaseUrl)
    }
}

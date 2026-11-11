/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.peertube.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.services.peertube.PeertubeParsingHelper
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.extractor.utils.JsonUtils

public open class PeertubeStreamInfoItemExtractor(
    @JvmField protected val item: JsonObject,
    baseUrl: String,
) : StreamInfoItemExtractor {

    /** `null` when the item comes from an instance whose address could not be checked. */
    private var instanceBaseUrl: String? = baseUrl

    @get:Throws(ParsingException::class)
    override val url: String
        get() {
            val uuid = JsonUtils.getString(item, "uuid")
            return ServiceList.PeerTube.streamLHFactory.fromId(uuid, requireBaseUrl()).url
        }

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = instanceBaseUrl
            ?.let { PeertubeParsingHelper.getThumbnailsFromPlaylistOrVideoItem(it, item) }
            .orEmpty()

    @get:Throws(ParsingException::class)
    override val name: String
        get() = JsonUtils.getString(item, "name")

    override val isAd: Boolean
        get() = false

    override val viewCount: Long
        get() = item.getLong("views")

    @get:Throws(ParsingException::class)
    override val uploaderUrl: String?
        get() {
            val name = JsonUtils.getString(item, "account.name")
            val host = JsonUtils.getString(item, "account.host")
            return ServiceList.PeerTube.channelLHFactory
                .fromId("accounts/$name@$host", requireBaseUrl()).url
        }

    override val uploaderAvatars: List<Image>
        get() = instanceBaseUrl
            ?.let { baseUrl ->
                PeertubeParsingHelper.getAvatarsFromOwnerAccountOrVideoChannelObject(baseUrl, item.getObject("account"))
            }
            .orEmpty()

    @get:Throws(ParsingException::class)
    override val isUploaderVerified: Boolean
        get() = false

    @get:Throws(ParsingException::class)
    override val uploaderName: String?
        get() = JsonUtils.getString(item, "account.displayName")

    @get:Throws(ParsingException::class)
    override val textualUploadDate: String?
        get() = JsonUtils.getString(item, "publishedAt")

    @get:Throws(ParsingException::class)
    override val uploadDate: DateWrapper?
        get() = DateWrapper.fromInstant(textualUploadDate)

    override val streamType: StreamType
        get() = if (item.getBoolean("isLive")) StreamType.LIVE_STREAM else StreamType.VIDEO_STREAM

    override val duration: Long
        get() = item.getLong("duration")

    /**
     * Sets the base URL of the instance the item comes from; `null` when it could not be checked,
     * in which case the item has no URL and no images.
     */
    protected fun setBaseUrl(baseUrl: String?) {
        instanceBaseUrl = baseUrl
    }

    @Throws(ParsingException::class)
    private fun requireBaseUrl(): String =
        instanceBaseUrl ?: throw ParsingException("Could not get the instance of the item")
}

/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.peertube.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.channel.ChannelInfoItemExtractor
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.services.peertube.PeertubeLinkGuards
import org.schabi.newpipe.extractor.services.peertube.PeertubeParsingHelper

public open class PeertubeChannelInfoItemExtractor(
    private val item: JsonObject,
    private val baseUrl: String,
) : ChannelInfoItemExtractor {

    @get:Throws(ParsingException::class)
    override val name: String
        get() = item.getString("displayName") ?: throw ParsingException("Could not get channel name")

    /** The channel URL given by the instance, only when it is a web link. */
    @get:Throws(ParsingException::class)
    override val url: String
        get() = PeertubeLinkGuards.requireWebUrl(item.getString("url"), "channel URL")

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = PeertubeParsingHelper.getAvatarsFromOwnerAccountOrVideoChannelObject(baseUrl, item)

    @get:Throws(ParsingException::class)
    override val description: String?
        get() = item.getString("description")

    @get:Throws(ParsingException::class)
    override val subscriberCount: Long
        get() = item.getInt("followersCount").toLong()

    @get:Throws(ParsingException::class)
    override val streamCount: Long
        get() = ListExtractor.ITEM_COUNT_UNKNOWN

    @get:Throws(ParsingException::class)
    override val isVerified: Boolean
        get() = false
}

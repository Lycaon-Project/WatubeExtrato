/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.media_ccc.extractors.infoItems

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.channel.ChannelInfoItemExtractor
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.services.media_ccc.MediaCCCLinkGuards
import org.schabi.newpipe.extractor.services.media_ccc.extractors.MediaCCCParsingHelper

public open class MediaCCCConferenceInfoItemExtractor(private val conference: JsonObject) : ChannelInfoItemExtractor {

    override val description: String?
        get() = ""

    override val subscriberCount: Long
        get() = -1

    override val streamCount: Long
        get() = ListExtractor.ITEM_COUNT_UNKNOWN

    @get:Throws(ParsingException::class)
    override val isVerified: Boolean
        get() = false

    @get:Throws(ParsingException::class)
    override val name: String
        get() = conference.getString("title") ?: throw ParsingException("Could not get conference name")

    /** The conference URL given by the API, only when it is a web link. */
    @get:Throws(ParsingException::class)
    override val url: String
        get() = MediaCCCLinkGuards.requireWebUrl(conference.getString("url"), "conference URL")

    override val thumbnails: List<Image>
        get() = MediaCCCParsingHelper.getImageListFromLogoImageUrl(conference.getString("logo_url"))
}

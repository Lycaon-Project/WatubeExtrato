/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.media_ccc.extractors.infoItems

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.services.media_ccc.MediaCCCLinkGuards
import org.schabi.newpipe.extractor.services.media_ccc.extractors.MediaCCCParsingHelper
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor
import org.schabi.newpipe.extractor.stream.StreamType

public open class MediaCCCStreamInfoItemExtractor(private val event: JsonObject) : StreamInfoItemExtractor {

    override val streamType: StreamType
        get() = StreamType.VIDEO_STREAM

    override val isAd: Boolean
        get() = false

    override val duration: Long
        get() = event.getInt("length").toLong()

    override val viewCount: Long
        get() = event.getInt("view_count").toLong()

    override val uploaderName: String?
        get() = event.getString("conference_title")

    /** The conference URL given by the API, only when it is a web link. */
    override val uploaderUrl: String?
        get() = MediaCCCLinkGuards.webUrlOrNull(event.getString("conference_url"))

    @get:Throws(ParsingException::class)
    override val isUploaderVerified: Boolean
        get() = false

    override val textualUploadDate: String?
        get() = event.getString("release_date")

    // if null, event is in the future...
    @get:Throws(ParsingException::class)
    override val uploadDate: DateWrapper?
        get() = DateWrapper.fromOffsetDateTime(textualUploadDate)

    @get:Throws(ParsingException::class)
    override val name: String
        get() = event.getString("title") ?: throw ParsingException("Could not get event name")

    /** Built from the event GUID, checked before it is placed in the URL. */
    @get:Throws(ParsingException::class)
    override val url: String
        get() = "https://media.ccc.de/public/events/" + MediaCCCLinkGuards.requireGuid(event.getString("guid"))

    override val thumbnails: List<Image>
        get() = MediaCCCParsingHelper.getThumbnailsFromStreamItem(event)
}

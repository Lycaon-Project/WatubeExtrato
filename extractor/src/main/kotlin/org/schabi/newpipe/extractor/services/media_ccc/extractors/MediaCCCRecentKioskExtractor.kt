/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.media_ccc.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.services.media_ccc.MediaCCCLinkGuards
import org.schabi.newpipe.extractor.services.media_ccc.linkHandler.MediaCCCConferenceLinkHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor
import org.schabi.newpipe.extractor.stream.StreamType
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

public open class MediaCCCRecentKioskExtractor(private val event: JsonObject) : StreamInfoItemExtractor {

    @get:Throws(ParsingException::class)
    override val name: String
        get() = event.getString("title") ?: throw ParsingException("Could not get event name")

    /** The event page given by the API, only when it is a web link. */
    @get:Throws(ParsingException::class)
    override val url: String
        get() = MediaCCCLinkGuards.requireWebUrl(event.getString("frontend_link"), "event link")

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = MediaCCCParsingHelper.getImageListFromLogoImageUrl(event.getString("poster_url"))

    @get:Throws(ParsingException::class)
    override val streamType: StreamType
        get() = StreamType.VIDEO_STREAM

    override val isAd: Boolean
        get() = false

    // duration and length have the same value, see
    // https://github.com/voc/voctoweb/blob/master/app/views/public/shared/_event.json.jbuilder
    override val duration: Long
        get() = event.getInt("duration").toLong()

    @get:Throws(ParsingException::class)
    override val viewCount: Long
        get() = event.getInt("view_count").toLong()

    @get:Throws(ParsingException::class)
    override val uploaderName: String?
        get() = event.getString("conference_title")

    /** The API URL of the conference, turned into its web URL by the (checking) link handler factory. */
    @get:Throws(ParsingException::class)
    override val uploaderUrl: String?
        get() = MediaCCCConferenceLinkHandlerFactory
            .fromUrl(event.getString("conference_url")) // API URL
            .url // web URL

    @get:Throws(ParsingException::class)
    override val isUploaderVerified: Boolean
        get() = false

    @get:Throws(ParsingException::class)
    override val textualUploadDate: String?
        get() = event.getString("date")

    @get:Throws(ParsingException::class)
    override val uploadDate: DateWrapper?
        get() {
            val date = textualUploadDate ?: return null
            return try {
                DateWrapper(ZonedDateTime.parse(date, DATE_FORMATTER).toInstant())
            } catch (e: DateTimeParseException) {
                throw ParsingException("Could not parse upload date", e)
            }
        }

    private companion object {
        private val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSzzzz")
    }
}

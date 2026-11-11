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

public open class MediaCCCLiveStreamKioskExtractor(
    private val conferenceInfo: JsonObject,
    private val group: String?,
    private val roomInfo: JsonObject,
) : StreamInfoItemExtractor {

    @get:Throws(ParsingException::class)
    override val name: String
        get() = roomInfo.getObject("talks").getObject("current").getString("title")
            ?: throw ParsingException("Could not get live stream name")

    /** The room link given by the API, only when it is a web link. */
    @get:Throws(ParsingException::class)
    override val url: String
        get() = MediaCCCLinkGuards.requireWebUrl(roomInfo.getString("link"), "room link")

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = MediaCCCParsingHelper.getThumbnailsFromLiveStreamItem(roomInfo)

    @get:Throws(ParsingException::class)
    override val streamType: StreamType
        get() {
            val isVideo = roomInfo.getArray("streams")
                .any { stream -> (stream as? JsonObject)?.getString("type") == "video" }
            return if (isVideo) StreamType.LIVE_STREAM else StreamType.AUDIO_LIVE_STREAM
        }

    @get:Throws(ParsingException::class)
    override val isAd: Boolean
        get() = false

    @get:Throws(ParsingException::class)
    override val duration: Long
        get() = 0

    @get:Throws(ParsingException::class)
    override val viewCount: Long
        get() = -1

    @get:Throws(ParsingException::class)
    override val uploaderName: String?
        get() = conferenceInfo.getString("conference") + " - " + group + " - " + roomInfo.getString("display")

    /** Built from the conference slug, only when it is a valid slug. */
    @get:Throws(ParsingException::class)
    override val uploaderUrl: String?
        get() = conferenceInfo.getString("slug")
            ?.takeIf { MediaCCCLinkGuards.isSlug(it) }
            ?.let { MediaCCCConferenceLinkHandlerFactory.CONFERENCE_PATH + it }

    @get:Throws(ParsingException::class)
    override val isUploaderVerified: Boolean
        get() = false

    @get:Throws(ParsingException::class)
    override val textualUploadDate: String?
        get() = null

    @get:Throws(ParsingException::class)
    override val uploadDate: DateWrapper?
        get() = null
}

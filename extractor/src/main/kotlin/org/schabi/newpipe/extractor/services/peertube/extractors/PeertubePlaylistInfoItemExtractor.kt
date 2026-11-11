/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.peertube.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItemExtractor
import org.schabi.newpipe.extractor.services.peertube.PeertubeLinkGuards
import org.schabi.newpipe.extractor.services.peertube.PeertubeParsingHelper
import org.schabi.newpipe.extractor.stream.Description

public open class PeertubePlaylistInfoItemExtractor(
    private val item: JsonObject,
    private val baseUrl: String,
) : PlaylistInfoItemExtractor {

    private val uploader: JsonObject = item.getObject("uploader")

    @get:Throws(ParsingException::class)
    override val name: String
        get() = item.getString("displayName") ?: throw ParsingException("Could not get playlist name")

    /** The playlist URL given by the instance, only when it is a web link. */
    @get:Throws(ParsingException::class)
    override val url: String
        get() = PeertubeLinkGuards.requireWebUrl(item.getString("url"), "playlist URL")

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = PeertubeParsingHelper.getThumbnailsFromPlaylistOrVideoItem(baseUrl, item)

    @get:Throws(ParsingException::class)
    override val uploaderName: String?
        get() = uploader.getString("displayName")

    /** The uploader URL given by the instance, only when it is a web link. */
    @get:Throws(ParsingException::class)
    override val uploaderUrl: String?
        get() = PeertubeLinkGuards.webUrlOrNull(uploader.getString("url"))

    @get:Throws(ParsingException::class)
    override val isUploaderVerified: Boolean
        get() = false

    @get:Throws(ParsingException::class)
    override val streamCount: Long
        get() = item.getInt("videosLength").toLong()

    @get:Throws(ParsingException::class)
    override val description: Description
        get() = Description.of(item.getString("description"), Description.Type.PLAIN_TEXT)
}

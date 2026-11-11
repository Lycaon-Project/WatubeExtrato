/*
 * Created by Christian Schabesberger on 28.02.16.
 *
 * Copyright (C) 2016 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * StreamInfoItemExtractor.java is part of NewPipe Extractor.
 *
 * NewPipe Extractor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe Extractor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe Extractor.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.schabi.newpipe.extractor.stream

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.InfoItemExtractor
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.localization.DateWrapper

public interface StreamInfoItemExtractor : InfoItemExtractor {

    /**
     * Get the stream type
     *
     * @return the stream type
     * @throws ParsingException if there is an error in the extraction
     */
    @get:Throws(ParsingException::class)
    public val streamType: StreamType

    /**
     * Check if the stream is an ad.
     *
     * @return `true` if the stream is an ad.
     * @throws ParsingException if there is an error in the extraction
     */
    @get:Throws(ParsingException::class)
    public val isAd: Boolean

    /**
     * Get the stream duration in seconds
     *
     * @return the stream duration in seconds or -1 if no duration is available
     * @throws ParsingException if there is an error in the extraction
     */
    @get:Throws(ParsingException::class)
    public val duration: Long

    /**
     * Parses the number of views (or of current viewers in case of live streams)
     *
     * @return the number of views (or of current viewers in case of live streams) or -1 if not
     * available
     * @throws ParsingException if there is an error in the extraction
     */
    @get:Throws(ParsingException::class)
    public val viewCount: Long

    /**
     * Get the uploader name
     *
     * @return the uploader name, or `null` for some services
     * @throws ParsingException if there is an error in the extraction
     */
    @get:Throws(ParsingException::class)
    public val uploaderName: String?

    /**
     * Get the uploader URL, or `null` for some services.
     */
    @get:Throws(ParsingException::class)
    public val uploaderUrl: String?

    /**
     * Get the uploader avatars.
     *
     * @return the uploader avatars or an empty list if not provided by the service
     * @throws ParsingException if there is an error in the extraction
     */
    @get:Throws(ParsingException::class)
    public val uploaderAvatars: List<Image>
        get() = emptyList()

    /**
     * Whether the uploader has been verified by the service's provider.
     * If there is no verification implemented, return `false`.
     *
     * @return whether the uploader has been verified by the service's provider
     * @throws ParsingException if there is an error in the extraction
     */
    @get:Throws(ParsingException::class)
    public val isUploaderVerified: Boolean

    /**
     * The original textual date provided by the service. Should be used as a fallback if
     * [uploadDate] isn't provided by the service, or it fails for some reason.
     *
     * @return The original textual date provided by the service or `null` if not provided.
     * @throws ParsingException if there is an error in the extraction
     * @see uploadDate
     */
    @get:Throws(ParsingException::class)
    public val textualUploadDate: String?

    /**
     * Extracts the upload date and time of this item and parses it.
     *
     * If the service doesn't provide an exact time, an approximation can be returned.
     * If the service doesn't provide any date at all, then `null` should be returned.
     *
     * @return The date and time (can be approximated) this item was uploaded or `null`.
     * @throws ParsingException if there is an error in the extraction or the extracted date
     * couldn't be parsed
     * @see textualUploadDate
     */
    @get:Throws(ParsingException::class)
    public val uploadDate: DateWrapper?

    /**
     * Get the video's short description.
     *
     * @return The video's short description or `null` if not provided by the service.
     * @throws ParsingException if there is an error in the extraction
     */
    @get:Throws(ParsingException::class)
    public val shortDescription: String?
        get() = null

    /**
     * Whether the stream is a short-form content.
     *
     * Short-form contents are contents in the style of TikTok, YouTube Shorts, or Instagram Reels
     * videos.
     *
     * @return whether the stream is a short-form content
     * @throws ParsingException if there is an error in the extraction
     */
    @get:Throws(ParsingException::class)
    public val isShortFormContent: Boolean
        get() = false

    /**
     * Get the availability of the stream.
     *
     * The availability may not reflect the actual availability when requesting the stream.
     *
     * @return The stream's availability
     * @throws ParsingException if there is an error in the extraction
     */
    @get:Throws(ParsingException::class)
    public val contentAvailability: ContentAvailability
        get() = ContentAvailability.UNKNOWN
}

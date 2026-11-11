/*
 * Created by Christian Schabesberger on 26.08.15.
 *
 * Copyright (C) 2016 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * StreamInfoItem.java is part of NewPipe Extractor.
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
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.localization.DateWrapper

/**
 * Info object for previews of unopened videos, e.g. search results, related videos.
 */
public open class StreamInfoItem(
    serviceId: Int,
    url: String,
    name: String,
    public val streamType: StreamType,
) : InfoItem(InfoType.STREAM, serviceId, url, name) {

    public var uploaderName: String? = null
    public var shortDescription: String? = null
    public var textualUploadDate: String? = null
    public var uploadDate: DateWrapper? = null
    public var viewCount: Long = -1
    public var duration: Long = -1
    public var uploaderUrl: String? = null
    public var uploaderAvatars: List<Image> = emptyList()
    public var isUploaderVerified: Boolean = false
    public var isShortFormContent: Boolean = false

    /**
     * The availability of the content.
     */
    public var contentAvailability: ContentAvailability = ContentAvailability.AVAILABLE

    /**
     * A description for debugging. It leaves out the URLs and the uploader, which identify
     * what the user watched.
     */
    override fun toString(): String = "StreamInfoItem{" +
        "streamType=" + streamType +
        ", viewCount=" + viewCount +
        ", duration=" + duration +
        ", infoType=" + infoType +
        ", serviceId=" + serviceId +
        ", uploaderVerified='" + isUploaderVerified + '\'' +
        '}'

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

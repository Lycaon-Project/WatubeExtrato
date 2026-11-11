/*
 * Created by Christian Schabesberger on 28.02.16.
 *
 * Copyright (C) 2016 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * StreamInfoItemsCollector.java is part of NewPipe Extractor.
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

import org.schabi.newpipe.extractor.InfoItemsCollector
import org.schabi.newpipe.extractor.exceptions.FoundAdException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.utils.ExtractorLogger

public open class StreamInfoItemsCollector :
    InfoItemsCollector<StreamInfoItem, StreamInfoItemExtractor> {

    public constructor(serviceId: Int) : super(serviceId)

    public constructor(serviceId: Int, comparator: Comparator<StreamInfoItem>?) :
        super(serviceId, comparator)

    @Throws(ParsingException::class)
    override fun extract(extractor: StreamInfoItemExtractor): StreamInfoItem {
        if (extractor.isAd) {
            throw FoundAdException("Found ad")
        }

        val resultItem = StreamInfoItem(serviceId, extractor.url, extractor.name, extractor.streamType)

        // optional information
        collect { resultItem.duration = extractor.duration }
        collect { resultItem.uploaderName = extractor.uploaderName }
        collect { resultItem.textualUploadDate = extractor.textualUploadDate }
        try {
            resultItem.uploadDate = extractor.uploadDate
        } catch (e: ParsingException) {
            addError(e)
        }
        collect { resultItem.viewCount = extractor.viewCount }
        collect { resultItem.thumbnails = extractor.thumbnails }
        collect { resultItem.uploaderUrl = extractor.uploaderUrl }
        collect { resultItem.uploaderAvatars = extractor.uploaderAvatars }
        collect { resultItem.isUploaderVerified = extractor.isUploaderVerified }
        collect { resultItem.shortDescription = extractor.shortDescription }
        collect { resultItem.isShortFormContent = extractor.isShortFormContent }
        collect { resultItem.contentAvailability = extractor.contentAvailability }
        return resultItem
    }

    override fun commit(extractor: StreamInfoItemExtractor) {
        try {
            addItem(extract(extractor))
        } catch (_: FoundAdException) {
            // Ads are skipped silently
        } catch (e: ParsingException) {
            addError(e)
        } catch (e: Exception) {
            // not odd data but a bug: still kept out of the list, and logged
            ExtractorLogger.e(javaClass.simpleName, "Unexpected error while extracting a stream", e)
            addError(e)
        }
    }

    /** Runs one optional extraction: a failure is recorded, not thrown. */
    private inline fun collect(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            addError(e)
        }
    }
}

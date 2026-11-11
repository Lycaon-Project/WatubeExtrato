/*
 * Created by Christian Schabesberger on 12.02.17.
 *
 * Copyright (C) 2017 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * ChannelInfoItemsCollector.java is part of NewPipe Extractor.
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

package org.schabi.newpipe.extractor.channel

import org.schabi.newpipe.extractor.InfoItemsCollector
import org.schabi.newpipe.extractor.exceptions.ParsingException

public class ChannelInfoItemsCollector(serviceId: Int) :
    InfoItemsCollector<ChannelInfoItem, ChannelInfoItemExtractor>(serviceId) {

    @Throws(ParsingException::class)
    override fun extract(extractor: ChannelInfoItemExtractor): ChannelInfoItem {
        val resultItem = ChannelInfoItem(serviceId, extractor.url, extractor.name)

        // optional information
        collect { resultItem.subscriberCount = extractor.subscriberCount }
        collect { resultItem.streamCount = extractor.streamCount }
        collect { resultItem.thumbnails = extractor.thumbnails }
        collect { resultItem.description = extractor.description }
        collect { resultItem.isVerified = extractor.isVerified }
        return resultItem
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

/*
 * Created by Christian Schabesberger on 12.02.17.
 *
 * Copyright (C) 2017 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * InfoItemsSearchCollector.java is part of NewPipe Extractor.
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
 * along with NewPipe Extractor.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.schabi.newpipe.extractor

import org.schabi.newpipe.extractor.channel.ChannelInfoItemExtractor
import org.schabi.newpipe.extractor.channel.ChannelInfoItemsCollector
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItemExtractor
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItemsCollector
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector
import java.util.Collections

/**
 * A collector that can handle many extractor types, to be used when a list contains items of
 * different types (e.g. search)
 *
 * This collector can handle the following extractor types:
 * - [StreamInfoItemExtractor]
 * - [ChannelInfoItemExtractor]
 * - [PlaylistInfoItemExtractor]
 *
 * Calling [extract] or [commit] with any other extractor type will raise an exception.
 */
public open class MultiInfoItemsCollector(serviceId: Int) :
    InfoItemsCollector<InfoItem, InfoItemExtractor>(serviceId) {

    private val streamCollector = StreamInfoItemsCollector(serviceId)
    private val userCollector = ChannelInfoItemsCollector(serviceId)
    private val playlistCollector = PlaylistInfoItemsCollector(serviceId)

    override val errors: List<Throwable>
        get() {
            val ownErrors = super.errors
            val streamErrors = streamCollector.errors
            val userErrors = userCollector.errors
            val playlistErrors = playlistCollector.errors
            val all = ArrayList<Throwable>(
                ownErrors.size + streamErrors.size + userErrors.size + playlistErrors.size,
            )
            all.addAll(ownErrors)
            all.addAll(streamErrors)
            all.addAll(userErrors)
            all.addAll(playlistErrors)
            return Collections.unmodifiableList(all)
        }

    override fun reset() {
        super.reset()
        streamCollector.reset()
        userCollector.reset()
        playlistCollector.reset()
    }

    /** Uses the corresponding collector for each item extractor type. */
    @Throws(ParsingException::class)
    override fun extract(extractor: InfoItemExtractor): InfoItem = when (extractor) {
        is StreamInfoItemExtractor -> streamCollector.extract(extractor)
        is ChannelInfoItemExtractor -> userCollector.extract(extractor)
        is PlaylistInfoItemExtractor -> playlistCollector.extract(extractor)
        else -> throw IllegalArgumentException(
            "Invalid extractor type: " + extractor.javaClass.simpleName,
        )
    }
}

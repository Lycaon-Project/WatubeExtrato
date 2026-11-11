/*
 * Created by Christian Schabesberger on 25.07.16.
 *
 * Copyright (C) 2016 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * ChannelExtractor.java is part of NewPipe Extractor.
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

import org.schabi.newpipe.extractor.Extractor
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler

public abstract class ChannelExtractor protected constructor(
    service: StreamingService,
    linkHandler: ListLinkHandler,
) : Extractor(service, linkHandler) {

    @get:Throws(ParsingException::class)
    public abstract val avatars: List<Image>

    @get:Throws(ParsingException::class)
    public abstract val banners: List<Image>

    /** The feed URL, or `null` for services without one. */
    @get:Throws(ParsingException::class)
    public abstract val feedUrl: String?

    @get:Throws(ParsingException::class)
    public abstract val subscriberCount: Long

    /** The description, or `null` when the channel has none. */
    @get:Throws(ParsingException::class)
    public abstract val description: String?

    @get:Throws(ParsingException::class)
    public abstract val parentChannelName: String?

    @get:Throws(ParsingException::class)
    public abstract val parentChannelUrl: String?

    @get:Throws(ParsingException::class)
    public abstract val parentChannelAvatars: List<Image>

    @get:Throws(ParsingException::class)
    public abstract val isVerified: Boolean

    @get:Throws(ParsingException::class)
    public abstract val tabs: List<ListLinkHandler>

    @get:Throws(ParsingException::class)
    public open val tags: List<String>
        get() = emptyList()

    public companion object {
        public const val UNKNOWN_SUBSCRIBER_COUNT: Long = -1
    }
}

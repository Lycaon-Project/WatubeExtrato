/*
 * Created by Christian Schabesberger on 11.02.17.
 *
 * Copyright (C) 2017 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * ChannelInfoItem.java is part of NewPipe Extractor.
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

package org.schabi.newpipe.extractor.channel

import org.schabi.newpipe.extractor.InfoItem

public open class ChannelInfoItem(
    serviceId: Int,
    url: String,
    name: String,
) : InfoItem(InfoType.CHANNEL, serviceId, url, name) {

    public var description: String? = null
    public var subscriberCount: Long = -1
    public var streamCount: Long = -1
    public var isVerified: Boolean = false

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

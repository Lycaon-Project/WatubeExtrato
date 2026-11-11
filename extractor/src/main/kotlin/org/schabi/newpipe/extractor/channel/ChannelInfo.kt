/*
 * Created by Christian Schabesberger on 31.07.16.
 *
 * Copyright (C) 2016 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * ChannelInfo.java is part of NewPipe Extractor.
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

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.Info
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import java.io.IOException

public open class ChannelInfo(
    serviceId: Int,
    id: String,
    url: String,
    originalUrl: String,
    name: String,
) : Info(serviceId, id, url, originalUrl, name) {

    public var parentChannelName: String? = null
    public var parentChannelUrl: String? = null
    public var feedUrl: String? = null
    public var subscriberCount: Long = -1
    public var description: String? = null
    public var donationLinks: Array<String>? = null
    public var avatars: List<Image> = emptyList()
    public var banners: List<Image> = emptyList()
    public var parentChannelAvatars: List<Image> = emptyList()
    public var isVerified: Boolean = false
    public var tabs: List<ListLinkHandler> = emptyList()
    public var tags: List<String> = emptyList()

    public companion object {
        private const val serialVersionUID: Long = 1L

        @JvmStatic
        @Throws(IOException::class, ExtractionException::class)
        public fun getInfo(url: String): ChannelInfo = getInfo(NewPipe.getServiceByUrl(url), url)

        @JvmStatic
        @Throws(IOException::class, ExtractionException::class)
        public fun getInfo(service: StreamingService, url: String): ChannelInfo {
            val extractor = service.getChannelExtractor(url)
            extractor.fetchPage()
            return getInfo(extractor)
        }

        @JvmStatic
        @Throws(IOException::class, ExtractionException::class)
        public fun getInfo(extractor: ChannelExtractor): ChannelInfo {
            val info = ChannelInfo(
                extractor.serviceId,
                extractor.id,
                extractor.url,
                extractor.originalUrl,
                extractor.name,
            )

            optional(info) { avatars = extractor.avatars }
            optional(info) { banners = extractor.banners }
            optional(info) { feedUrl = extractor.feedUrl }
            optional(info) { subscriberCount = extractor.subscriberCount }
            optional(info) { description = extractor.description }
            optional(info) { parentChannelName = extractor.parentChannelName }
            optional(info) { parentChannelUrl = extractor.parentChannelUrl }
            optional(info) { parentChannelAvatars = extractor.parentChannelAvatars }
            optional(info) { isVerified = extractor.isVerified }
            optional(info) { tabs = extractor.tabs }
            optional(info) { tags = extractor.tags }
            return info
        }

        /** Runs one optional extraction: a failure is stored in the errors of [info]. */
        private inline fun optional(info: ChannelInfo, block: ChannelInfo.() -> Unit) {
            try {
                info.block()
            } catch (e: Exception) {
                info.addError(e)
            }
        }
    }
}

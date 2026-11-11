/*
 * Created by Christian Schabesberger on 12.02.17.
 *
 * Copyright (C) 2017 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * YoutubeChannelInfoItemExtractor.java is part of NewPipe Extractor.
 *
 * NewPipe Extractor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe Extractor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe Extractor. If not, see <https://www.gnu.org/licenses/>.
 */

package org.schabi.newpipe.extractor.services.youtube.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.channel.ChannelInfoItemExtractor
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getTextFromObject
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getThumbnailsFromInfoItem
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeChannelLinkHandlerFactory
import org.schabi.newpipe.extractor.utils.Utils

public open class YoutubeChannelInfoItemExtractor(
    private val channelInfoItem: JsonObject,
) : ChannelInfoItemExtractor {

    /**
     * New layout:
     * "subscriberCountText": Channel handle
     * "videoCountText": Subscriber count
     */
    private val withHandle: Boolean =
        getTextFromObject(channelInfoItem.getObject("subscriberCountText"))?.startsWith("@") == true

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = try {
            getThumbnailsFromInfoItem(channelInfoItem)
        } catch (e: Exception) {
            throw ParsingException("Could not get thumbnails", e)
        }

    @get:Throws(ParsingException::class)
    override val name: String
        get() = getTextFromObject(channelInfoItem.getObject("title"))
            ?: throw ParsingException("Could not get name")

    @get:Throws(ParsingException::class)
    override val url: String
        get() {
            // A missing ID must not become the "channel/null" path
            val channelId = channelInfoItem.getString("channelId")
                ?: throw ParsingException("Could not get url")
            return try {
                YoutubeChannelLinkHandlerFactory.getInstance().getUrl("channel/$channelId")
            } catch (e: Exception) {
                throw ParsingException("Could not get url", e)
            }
        }

    @get:Throws(ParsingException::class)
    override val subscriberCount: Long
        get() {
            if (!channelInfoItem.has("subscriberCountText")) {
                // Subscription count is not available for this channel item.
                return -1
            }
            val countKey = if (withHandle) {
                if (!channelInfoItem.has("videoCountText")) {
                    return -1
                }
                "videoCountText"
            } else {
                "subscriberCountText"
            }
            return try {
                Utils.mixedNumberWordToLong(textOrThrow(countKey))
            } catch (e: Exception) {
                throw ParsingException("Could not get subscriber count", e)
            }
        }

    @get:Throws(ParsingException::class)
    override val streamCount: Long
        get() {
            if (withHandle || !channelInfoItem.has("videoCountText")) {
                // Video count is not available, either the channel has no public uploads
                // or YouTube displays the channel handle instead.
                return ListExtractor.ITEM_COUNT_UNKNOWN
            }
            return Utils.removeNonDigitCharacters(textOrThrow("videoCountText")).toLongOrNull()
                ?: throw ParsingException("Could not get stream count")
        }

    @get:Throws(ParsingException::class)
    override val isVerified: Boolean
        get() = YoutubeParsingHelper.isVerified(channelInfoItem.getArray("ownerBadges"))

    @get:Throws(ParsingException::class)
    override val description: String?
        get() = if (channelInfoItem.has("descriptionSnippet")) {
            getTextFromObject(channelInfoItem.getObject("descriptionSnippet"))
        } else {
            // Channel have no description.
            null
        }

    @Throws(ParsingException::class)
    private fun textOrThrow(key: String): String = getTextFromObject(channelInfoItem.getObject(key))
        ?: throw ParsingException("Could not get $key")
}

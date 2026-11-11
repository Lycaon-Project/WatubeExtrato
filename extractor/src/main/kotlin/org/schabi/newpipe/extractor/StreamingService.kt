/*
 * Copyright (C) 2018 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * StreamingService.java is part of NewPipe Extractor.
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

import org.schabi.newpipe.extractor.channel.ChannelExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor
import org.schabi.newpipe.extractor.comments.CommentsExtractor
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.feed.FeedExtractor
import org.schabi.newpipe.extractor.kiosk.KioskList
import org.schabi.newpipe.extractor.linkhandler.LinkHandler
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.localization.TimeAgoParser
import org.schabi.newpipe.extractor.localization.TimeAgoPatternsManager
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.subscription.SubscriptionExtractor
import org.schabi.newpipe.extractor.suggestion.SuggestionExtractor
import org.schabi.newpipe.extractor.utils.Utils

/**
 * Creates a new Streaming service.
 * If you Implement one do not set id within your implementation of this extractor, instead
 * set the id when you put the extractor into [ServiceList]
 * All other parameters can be set directly from the overriding constructor.
 * @param id the number of the service to identify him within the NewPipe frontend
 * @param name the name of the service
 * @param capabilities the type of media this service can handle
 */
public abstract class StreamingService(
    id: Int,
    name: String,
    capabilities: Set<ServiceInfo.MediaCapability>,
) {

    /**
     * This class holds meta information about the service implementation.
     *
     * Creates a new instance of a ServiceInfo
     * @param name the name of the service
     * @param mediaCapabilities the type of media this service can handle
     */
    public open class ServiceInfo(
        public val name: String,
        public val mediaCapabilities: Set<MediaCapability>,
    ) {
        public enum class MediaCapability {
            AUDIO,
            VIDEO,
            LIVE,
            COMMENTS,
        }
    }

    /**
     * LinkType will be used to determine which type of URL you are handling, and therefore which
     * part of NewPipe should handle a certain URL.
     */
    public enum class LinkType {
        NONE,
        STREAM,
        CHANNEL,
        PLAYLIST,
    }

    public val serviceId: Int = id

    public val serviceInfo: ServiceInfo = ServiceInfo(name, capabilities)

    override fun toString(): String = "$serviceId:${serviceInfo.name}"

    public abstract val baseUrl: String

    // ////////////////////////////////////////////////////////////////////////
    // Url Id handler
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Must return a new instance of an implementation of LinkHandlerFactory for streams.
     * @return an instance of a LinkHandlerFactory for streams
     */
    public abstract val streamLHFactory: LinkHandlerFactory

    /**
     * Must return a new instance of an implementation of ListLinkHandlerFactory for channels.
     * If support for channels is not given, null must be returned.
     * @return an instance of a ListLinkHandlerFactory for channels or null
     */
    public abstract val channelLHFactory: ListLinkHandlerFactory?

    /**
     * Must return a new instance of an implementation of ListLinkHandlerFactory for channel tabs.
     * If support for channel tabs is not given, null must be returned.
     *
     * @return an instance of a ListLinkHandlerFactory for channel tabs or null
     */
    public abstract val channelTabLHFactory: ListLinkHandlerFactory?

    /**
     * Must return a new instance of an implementation of ListLinkHandlerFactory for playlists.
     * If support for playlists is not given, null must be returned.
     * @return an instance of a ListLinkHandlerFactory for playlists or null
     */
    public abstract val playlistLHFactory: ListLinkHandlerFactory?

    /**
     * Must return an instance of an implementation of SearchQueryHandlerFactory.
     * @return an instance of a SearchQueryHandlerFactory
     */
    public abstract val searchQHFactory: SearchQueryHandlerFactory

    /** The factory of the comment links, or `null` when the service has no comments. */
    public abstract val commentsLHFactory: ListLinkHandlerFactory?

    // ////////////////////////////////////////////////////////////////////////
    // Extractors
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Must create a new instance of a SearchExtractor implementation.
     * @param queryHandler specifies the keyword lock for, and the filters which should be applied.
     * @return a new SearchExtractor instance
     */
    public abstract fun getSearchExtractor(queryHandler: SearchQueryHandler): SearchExtractor

    /**
     * Must create a new instance of a SuggestionExtractor implementation.
     * @return a new SuggestionExtractor instance
     */
    public abstract val suggestionExtractor: SuggestionExtractor

    /**
     * Outdated or obsolete. null can be returned.
     * @return just null
     */
    public abstract val subscriptionExtractor: SubscriptionExtractor?

    /**
     * This method decides which strategy will be chosen to fetch the feed. In YouTube, for
     * example, a separate feed exists which is lightweight and made specifically to be used like
     * this.
     *
     * In services which there's no other way to retrieve them, null should be returned.
     *
     * @return a [FeedExtractor] instance or null.
     */
    @Throws(ExtractionException::class)
    public open fun getFeedExtractor(url: String): FeedExtractor? = null

    /**
     * Must create a new instance of a KioskList implementation.
     * @return a new KioskList instance
     */
    @get:Throws(ExtractionException::class)
    public abstract val kioskList: KioskList

    /**
     * Must create a new instance of a ChannelExtractor implementation.
     * @param linkHandler is pointing to the channel which should be handled by this new instance.
     * @return a new ChannelExtractor
     */
    @Throws(ExtractionException::class)
    public abstract fun getChannelExtractor(linkHandler: ListLinkHandler): ChannelExtractor

    /**
     * Must create a new instance of a ChannelTabExtractor implementation.
     *
     * @param linkHandler is pointing to the channel which should be handled by this new instance.
     * @return a new ChannelTabExtractor
     */
    @Throws(ExtractionException::class)
    public abstract fun getChannelTabExtractor(linkHandler: ListLinkHandler): ChannelTabExtractor

    /**
     * Must crete a new instance of a PlaylistExtractor implementation.
     * @param linkHandler is pointing to the playlist which should be handled by this new instance.
     * @return a new PlaylistExtractor
     */
    @Throws(ExtractionException::class)
    public abstract fun getPlaylistExtractor(linkHandler: ListLinkHandler): PlaylistExtractor

    /**
     * Must create a new instance of a StreamExtractor implementation.
     * @param linkHandler is pointing to the stream which should be handled by this new instance.
     * @return a new StreamExtractor
     */
    @Throws(ExtractionException::class)
    public abstract fun getStreamExtractor(linkHandler: LinkHandler): StreamExtractor

    @Throws(ExtractionException::class)
    public abstract fun getCommentsExtractor(linkHandler: ListLinkHandler): CommentsExtractor

    // ////////////////////////////////////////////////////////////////////////
    // Extractors without link handler
    // ////////////////////////////////////////////////////////////////////////

    @Throws(ExtractionException::class)
    public open fun getSearchExtractor(
        query: String,
        contentFilter: List<String>,
        sortFilter: String,
    ): SearchExtractor = getSearchExtractor(searchQHFactory.fromQuery(query, contentFilter, sortFilter))

    @Throws(ExtractionException::class)
    public open fun getChannelExtractor(
        id: String,
        contentFilter: List<String>,
        sortFilter: String,
    ): ChannelExtractor =
        getChannelExtractor(requireFactory(channelLHFactory).fromQuery(id, contentFilter, sortFilter))

    @Throws(ExtractionException::class)
    public open fun getPlaylistExtractor(
        id: String,
        contentFilter: List<String>,
        sortFilter: String,
    ): PlaylistExtractor =
        getPlaylistExtractor(requireFactory(playlistLHFactory).fromQuery(id, contentFilter, sortFilter))

    // ////////////////////////////////////////////////////////////////////////
    // Short extractors overloads
    // ////////////////////////////////////////////////////////////////////////

    @Throws(ExtractionException::class)
    public open fun getSearchExtractor(query: String): SearchExtractor =
        getSearchExtractor(searchQHFactory.fromQuery(query))

    @Throws(ExtractionException::class)
    public open fun getChannelExtractor(url: String): ChannelExtractor =
        getChannelExtractor(requireFactory(channelLHFactory).fromUrl(url))

    @Throws(ExtractionException::class)
    public open fun getChannelTabExtractorFromId(id: String, tab: String): ChannelTabExtractor =
        getChannelTabExtractor(requireFactory(channelTabLHFactory).fromQuery(id, listOf(tab), ""))

    @Throws(ExtractionException::class)
    public open fun getChannelTabExtractorFromIdAndBaseUrl(
        id: String,
        tab: String,
        baseUrl: String,
    ): ChannelTabExtractor = getChannelTabExtractor(
        requireFactory(channelTabLHFactory).fromQuery(id, listOf(tab), "", baseUrl),
    )

    @Throws(ExtractionException::class)
    public open fun getPlaylistExtractor(url: String): PlaylistExtractor =
        getPlaylistExtractor(requireFactory(playlistLHFactory).fromUrl(url))

    @Throws(ExtractionException::class)
    public open fun getStreamExtractor(url: String): StreamExtractor =
        getStreamExtractor(streamLHFactory.fromUrl(url))

    /** `null` when the service has no comments. */
    @Throws(ExtractionException::class)
    public open fun getCommentsExtractor(url: String): CommentsExtractor? {
        val listLinkHandlerFactory = commentsLHFactory ?: return null
        return getCommentsExtractor(listLinkHandlerFactory.fromUrl(url))
    }

    // ////////////////////////////////////////////////////////////////////////
    // Utils
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Figures out where the link is pointing to (a channel, a video, a playlist, etc.)
     * @param url the url on which it should be decided of which link type it is
     * @return the link type of url
     */
    @Throws(ParsingException::class)
    public fun getLinkTypeByUrl(url: String): LinkType {
        // checked before any pattern runs on it: a huge "link" would only burn the CPU
        if (url.length > Utils.MAX_URL_LENGTH) {
            return LinkType.NONE
        }
        val polishedUrl = Utils.followGoogleRedirectIfNeeded(url)

        val channelFactory = channelLHFactory
        val playlistFactory = playlistLHFactory

        return when {
            streamLHFactory.acceptUrl(polishedUrl) -> LinkType.STREAM
            channelFactory != null && channelFactory.acceptUrl(polishedUrl) -> LinkType.CHANNEL
            playlistFactory != null && playlistFactory.acceptUrl(polishedUrl) -> LinkType.PLAYLIST
            else -> LinkType.NONE
        }
    }

    // ////////////////////////////////////////////////////////////////////////
    // Localization
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Returns a list of localizations that this service supports.
     */
    public open val supportedLocalizations: List<Localization>
        get() = listOf(Localization.DEFAULT)

    /**
     * Returns a list of countries that this service supports.
     */
    public open val supportedCountries: List<ContentCountry>
        get() = listOf(ContentCountry.DEFAULT)

    /**
     * Returns the localization that should be used in this service. It will get which
     * localization the user prefers (using [NewPipe.getPreferredLocalization]), then it will:
     * - Check if the exact localization is supported by this service.
     * - If not, check if a less specific localization is available, using only the language
     * code.
     * - Fallback to the [default][Localization.DEFAULT] localization.
     */
    public open val localization: Localization
        get() {
            val preferredLocalization = NewPipe.getPreferredLocalization()
            // Read once: services build the list on every call
            val supported = supportedLocalizations

            // Check the localization's language and country
            if (supported.contains(preferredLocalization)) {
                return preferredLocalization
            }

            // Fallback to the first supported language that matches the preferred language
            return supported.firstOrNull { it.languageCode == preferredLocalization.languageCode }
                ?: Localization.DEFAULT
        }

    /**
     * Returns the country that should be used to fetch content in this service. It will get which
     * country the user prefer (using [NewPipe.getPreferredContentCountry]), then it will:
     * - Check if the country is supported by this service.
     * - If not, fallback to the [default][ContentCountry.DEFAULT] country.
     */
    public open val contentCountry: ContentCountry
        get() {
            val preferredContentCountry = NewPipe.getPreferredContentCountry()
            return if (supportedCountries.contains(preferredContentCountry)) {
                preferredContentCountry
            } else {
                ContentCountry.DEFAULT
            }
        }

    /**
     * Get an instance of the time ago parser using the patterns related to the passed
     * localization.
     *
     * Just like [localization], it will also try to fallback to a less specific
     * localization if the exact one is not available/supported.
     *
     * @throws IllegalArgumentException if the localization is not supported (parsing patterns are
     * not present).
     */
    public open fun getTimeAgoParser(localization: Localization): TimeAgoParser {
        TimeAgoPatternsManager.getTimeAgoParserFor(localization)?.let { return it }

        if (localization.countryCode.isNotEmpty()) {
            val lessSpecificLocalization = Localization(localization.languageCode)
            TimeAgoPatternsManager.getTimeAgoParserFor(lessSpecificLocalization)?.let { return it }
        }

        throw IllegalArgumentException("Localization is not supported")
    }

    private companion object {
        /**
         * The Java code dereferenced the optional factories directly: same
         * [NullPointerException] when the service does not support the type of link.
         */
        private fun <T : Any> requireFactory(factory: T?): T =
            factory ?: throw NullPointerException("This service does not support this type of link")
    }
}

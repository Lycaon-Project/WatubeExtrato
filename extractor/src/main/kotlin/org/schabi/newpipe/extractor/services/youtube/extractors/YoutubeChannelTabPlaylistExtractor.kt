/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.extractors

import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubePlaylistLinkHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.IOException

/**
 * A [ChannelTabExtractor] for YouTube system playlists using a [YoutubePlaylistExtractor]
 * instance.
 *
 * It is currently used to bypass age-restrictions on channels marked as age-restricted by their
 * owner(s).
 *
 * @param service     a [StreamingService] implementation, which must be the YouTube one
 * @param linkHandler a [ListLinkHandler] which must have a valid channel ID (starting with `UC`)
 * and one of the given and supported content filters: [ChannelTabs.VIDEOS], [ChannelTabs.SHORTS],
 * [ChannelTabs.LIVESTREAMS]
 * @throws IllegalArgumentException if the given [ListLinkHandler] does not have the required
 * arguments
 * @throws SystemPlaylistUrlCreationException if the system playlist URL could not be created,
 * which should never happen
 */
public open class YoutubeChannelTabPlaylistExtractor @Throws(
    IllegalArgumentException::class,
    SystemPlaylistUrlCreationException::class,
) constructor(
    service: StreamingService,
    linkHandler: ListLinkHandler,
) : ChannelTabExtractor(service, linkHandler) {

    private val playlistExtractorInstance: PlaylistExtractor =
        YoutubePlaylistExtractor(service, getPlaylistLinkHandler(linkHandler))
    private var playlistExisting = false

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        try {
            playlistExtractorInstance.onFetchPage(downloader)
            playlistExisting = true
        } catch (_: ContentNotAvailableException) {
            // If a channel has no content of the type requested, the corresponding system
            // playlist will not exist, so a ContentNotAvailableException would be thrown
            // Ignore such issues in this case
        }
    }

    @get:Throws(IOException::class, ExtractionException::class)
    override val initialPage: InfoItemsPage<InfoItem>
        get() = if (playlistExisting) asInfoItemsPage(playlistExtractorInstance.initialPage) else emptyPage()

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<InfoItem> =
        if (playlistExisting) asInfoItemsPage(playlistExtractorInstance.getPage(page)) else emptyPage()

    private fun asInfoItemsPage(page: InfoItemsPage<StreamInfoItem>): InfoItemsPage<InfoItem> =
        InfoItemsPage(page.items, page.nextPage, page.errors)

    /**
     * Get a playlist [ListLinkHandler] from a channel tab one.
     *
     * This method converts a channel ID without its `UC` prefix into a YouTube system playlist,
     * depending on the first content filter provided in the given [ListLinkHandler].
     *
     * The first content filter must be a channel tabs one among the [videos][ChannelTabs.VIDEOS],
     * [shorts][ChannelTabs.SHORTS] and [ChannelTabs.LIVESTREAMS] ones, which would be converted
     * respectively into playlists with the ID `UULF`, `UUSH` and `UULV` on which the channel ID
     * without the `UC` part is appended.
     */
    @Throws(IllegalArgumentException::class, SystemPlaylistUrlCreationException::class)
    private fun getPlaylistLinkHandler(originalLinkHandler: ListLinkHandler): ListLinkHandler {
        val contentFilter = requireNotNull(originalLinkHandler.contentFilters.firstOrNull()) {
            "A content filter is required"
        }
        val channelId = originalLinkHandler.id
        require(channelId.startsWith("UC")) { "Invalid channel ID" }
        val channelIdWithoutUc = channelId.substring(2)

        val playlistId = when (contentFilter) {
            ChannelTabs.VIDEOS -> "UULF$channelIdWithoutUc"
            ChannelTabs.SHORTS -> "UUSH$channelIdWithoutUc"
            ChannelTabs.LIVESTREAMS -> "UULV$channelIdWithoutUc"
            else -> throw IllegalArgumentException(
                "Only Videos, Shorts and Livestreams tabs can extracted as playlists",
            )
        }

        return try {
            val newUrl = YoutubePlaylistLinkHandlerFactory.getInstance().getUrl(playlistId)
            ListLinkHandler(newUrl, newUrl, playlistId, emptyList(), "")
        } catch (e: ParsingException) {
            // Only reachable if YouTube returned a playlist ID with characters that are not
            // URL-safe: YoutubePlaylistLinkHandlerFactory refuses to put it in a URL
            throw SystemPlaylistUrlCreationException("Could not create a YouTube playlist from a valid playlist ID", e)
        }
    }

    private fun emptyPage(): InfoItemsPage<InfoItem> = InfoItemsPage.emptyPage()

    /**
     * Exception thrown when a YouTube system playlist URL could not be created.
     *
     * This exception should be never thrown, as given playlist IDs should be always valid.
     */
    public class SystemPlaylistUrlCreationException internal constructor(
        message: String,
        cause: Throwable,
    ) : RuntimeException(message, cause)
}

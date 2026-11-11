/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.extractors

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItemExtractor
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.extractPlaylistTypeFromPlaylistId
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getImagesFromThumbnailsArray
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getYoutubeUrlFromNavigationEndpoint
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.hasArtistOrVerifiedIconBadgeAttachment
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubePlaylistLinkHandlerFactory
import org.schabi.newpipe.extractor.utils.Utils

public open class YoutubeMixOrPlaylistLockupInfoItemExtractor(
    private val lockupViewModel: JsonObject,
) : PlaylistInfoItemExtractor {

    private val thumbnailViewModel: JsonObject = lockupViewModel.getObject("contentImage")
        .getObject("collectionThumbnailViewModel")
        .getObject("primaryThumbnail")
        .getObject("thumbnailViewModel")

    private val lockupMetadataViewModel: JsonObject = lockupViewModel.getObject("metadata")
        .getObject("lockupMetadataViewModel")

    /*
    The metadata rows are structured in the following way:
    1st part: uploader info, playlist type, playlist updated date
    2nd part: space row
    3rd element: first video
    4th (not always returned for playlists with less than 2 items?): second video
    5th element (always returned, but at a different index for playlists with less than 2
    items?): Show full playlist

    The first metadata row has the following structure:
    1st array element: uploader info
    2nd element: playlist type (course, playlist, podcast)
    3rd element (not always returned): playlist updated date
     */
    private val firstMetadataRow: JsonObject = lockupMetadataViewModel.getObject("metadata")
        .getObject("contentMetadataViewModel")
        .getArray("metadataRows")
        .getObject(0)

    @get:Throws(ParsingException::class)
    override val playlistType: PlaylistInfo.PlaylistType = try {
        extractPlaylistTypeFromPlaylistId(playlistIdOrThrow())
    } catch (_: ParsingException) {
        // If we cannot extract the playlist type, fall back to the normal one
        PlaylistInfo.PlaylistType.NORMAL
    }

    private val uploaderText: JsonObject
        get() = firstMetadataRow.getArray("metadataParts").getObject(0).getObject("text")

    @get:Throws(ParsingException::class)
    override val uploaderName: String?
        get() = uploaderText.getString("content")

    @get:Throws(ParsingException::class)
    override val uploaderUrl: String?
        get() = if (playlistType != PlaylistInfo.PlaylistType.NORMAL) {
            // If the playlist is a mix, there is no uploader as they are auto-generated
            null
        } else {
            getYoutubeUrlFromNavigationEndpoint(
                uploaderText.getArray("commandRuns")
                    .getObject(0)
                    .getObject("onTap")
                    .getObject("innertubeCommand"),
            )
        }

    @get:Throws(ParsingException::class)
    override val isUploaderVerified: Boolean
        // If the playlist is a mix, there is no uploader as they are auto-generated
        get() = playlistType == PlaylistInfo.PlaylistType.NORMAL &&
            hasArtistOrVerifiedIconBadgeAttachment(uploaderText.getArray("attachmentRuns"))

    @get:Throws(ParsingException::class)
    override val streamCount: Long
        get() {
            if (playlistType != PlaylistInfo.PlaylistType.NORMAL) {
                // If the playlist is a mix, we are not able to get its stream count
                return ListExtractor.ITEM_COUNT_INFINITE
            }
            val badgeText = thumbnailViewModel.getArray("overlays")
                .filterIsInstance<JsonObject>()
                .firstOrNull { it.has("thumbnailOverlayBadgeViewModel") }
                ?.getObject("thumbnailOverlayBadgeViewModel")
                ?.getArray("thumbnailBadges")
                ?.filterIsInstance<JsonObject>()
                ?.firstOrNull { it.has("thumbnailBadgeViewModel") }
                ?.getObject("thumbnailBadgeViewModel")
                ?.getString("text")
                ?: throw ParsingException("Could not get playlist stream count")
            return Utils.removeNonDigitCharacters(badgeText).toLongOrNull()
                ?: throw ParsingException("Could not get playlist stream count")
        }

    @get:Throws(ParsingException::class)
    override val name: String
        get() = lockupMetadataViewModel.getObject("title").getString("content")
            ?: throw ParsingException("Could not get name")

    @get:Throws(ParsingException::class)
    override val url: String
        get() {
            // If the playlist item is a mix, we cannot return just its playlist ID as mix
            // playlists are not viewable in playlist pages
            // Use directly getUrlFromNavigationEndpoint in this case, which returns the watch URL
            // with the mix playlist
            if (playlistType == PlaylistInfo.PlaylistType.NORMAL) {
                try {
                    return YoutubePlaylistLinkHandlerFactory.getInstance().getUrl(playlistIdOrThrow())
                } catch (_: Exception) {
                    // Fall back to the navigation endpoint below
                }
            }
            return getYoutubeUrlFromNavigationEndpoint(
                lockupViewModel.getObject("rendererContext")
                    .getObject("commandContext")
                    .getObject("onTap")
                    .getObject("innertubeCommand"),
            ) ?: throw ParsingException("Could not get url")
        }

    @get:Throws(ParsingException::class)
    override val thumbnails: List<Image>
        get() = getImagesFromThumbnailsArray(thumbnailViewModel.getObject("image").getArray("sources"))

    @Throws(ParsingException::class)
    private fun playlistIdOrThrow(): String = lockupViewModel.getString("contentId")
        ?.takeIf { it.isNotEmpty() }
        ?: lockupViewModel.getObject("rendererContext")
            .getObject("commandContext")
            .getObject("watchEndpoint")
            .getString("playlistId")
            ?.takeIf { it.isNotEmpty() }
        ?: throw ParsingException("Could not get playlist ID")
}

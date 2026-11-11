package org.schabi.newpipe.extractor.playlist

import org.schabi.newpipe.extractor.InfoItemExtractor
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.stream.Description

public interface PlaylistInfoItemExtractor : InfoItemExtractor {

    /**
     * Get the uploader name
     * @return the uploader name
     */
    @get:Throws(ParsingException::class)
    public val uploaderName: String?

    /**
     * Get the uploader url
     * @return the uploader url, or `null` for some services
     */
    @get:Throws(ParsingException::class)
    public val uploaderUrl: String?

    /**
     * Get whether the uploader is verified
     * @return whether the uploader is verified
     */
    @get:Throws(ParsingException::class)
    public val isUploaderVerified: Boolean

    /**
     * Get the number of streams
     * @return the number of streams
     */
    @get:Throws(ParsingException::class)
    public val streamCount: Long

    /**
     * Get the description of the playlist if there is any.
     * Otherwise, an [EMPTY_DESCRIPTION][Description.EMPTY_DESCRIPTION] is returned.
     * @return the playlist's description
     */
    @get:Throws(ParsingException::class)
    public val description: Description
        get() = Description.EMPTY_DESCRIPTION

    /**
     * @return the type of this playlist, see [PlaylistInfo.PlaylistType] for a description
     * of types. If not overridden always returns [PlaylistInfo.PlaylistType.NORMAL].
     */
    @get:Throws(ParsingException::class)
    public val playlistType: PlaylistInfo.PlaylistType
        get() = PlaylistInfo.PlaylistType.NORMAL
}

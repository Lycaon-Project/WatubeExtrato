package org.schabi.newpipe.extractor.playlist

import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.stream.Description

public open class PlaylistInfoItem(
    serviceId: Int,
    url: String,
    name: String,
) : InfoItem(InfoType.PLAYLIST, serviceId, url, name) {

    public var uploaderName: String? = null
    public var uploaderUrl: String? = null
    public var isUploaderVerified: Boolean = false

    /**
     * How many streams this playlist have
     */
    public var streamCount: Long = 0

    /** The description, [Description.EMPTY_DESCRIPTION] (instead of `null`) when unknown. */
    public var description: Description = Description.EMPTY_DESCRIPTION
    public var playlistType: PlaylistInfo.PlaylistType? = null

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

package org.schabi.newpipe.extractor.playlist

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.stream.Description
import org.schabi.newpipe.extractor.stream.StreamInfoItem

public abstract class PlaylistExtractor(
    service: StreamingService,
    linkHandler: ListLinkHandler,
) : ListExtractor<StreamInfoItem>(service, linkHandler) {

    @get:Throws(ParsingException::class)
    public abstract val uploaderUrl: String

    @get:Throws(ParsingException::class)
    public abstract val uploaderName: String

    @get:Throws(ParsingException::class)
    public abstract val uploaderAvatars: List<Image>

    @get:Throws(ParsingException::class)
    public abstract val isUploaderVerified: Boolean

    @get:Throws(ParsingException::class)
    public abstract val streamCount: Long

    @get:Throws(ParsingException::class)
    public abstract val description: Description

    @get:Throws(ParsingException::class)
    public open val thumbnails: List<Image>
        get() = emptyList()

    @get:Throws(ParsingException::class)
    public open val banners: List<Image>
        get() = emptyList()

    @get:Throws(ParsingException::class)
    public open val subChannelName: String
        get() = ""

    @get:Throws(ParsingException::class)
    public open val subChannelUrl: String
        get() = ""

    @get:Throws(ParsingException::class)
    public open val subChannelAvatars: List<Image>
        get() = emptyList()

    @get:Throws(ParsingException::class)
    public open val playlistType: PlaylistInfo.PlaylistType
        get() = PlaylistInfo.PlaylistType.NORMAL
}

package org.schabi.newpipe.extractor.services.soundcloud.linkHandler

import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.UnsupportedTabException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory

public object SoundcloudChannelTabLinkHandlerFactory : ListLinkHandlerFactory() {

    @JvmStatic
    public fun getInstance(): SoundcloudChannelTabLinkHandlerFactory = this

    @JvmStatic
    @Throws(UnsupportedOperationException::class)
    public fun getUrlSuffix(tab: String): String = when (tab) {
        ChannelTabs.TRACKS -> "/tracks"
        ChannelTabs.PLAYLISTS -> "/sets"
        ChannelTabs.ALBUMS -> "/albums"
        ChannelTabs.LIKES -> "/likes"
        else -> throw UnsupportedTabException(tab)
    }

    @Throws(ParsingException::class)
    override fun getId(url: String?): String = SoundcloudChannelLinkHandlerFactory.getId(url)

    @Throws(ParsingException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
    ): String {
        val tab = contentFilter?.firstOrNull() ?: throw ParsingException("No channel tab given")
        // Checked first: an unsupported tab needs no request to resolve the channel
        val suffix = getUrlSuffix(tab)
        return SoundcloudChannelLinkHandlerFactory.getUrl(id) + suffix
    }

    @Throws(ParsingException::class)
    override fun onAcceptUrl(url: String?): Boolean = SoundcloudChannelLinkHandlerFactory.onAcceptUrl(url)

    override val availableContentFilter: Array<String>
        get() = arrayOf(
            ChannelTabs.TRACKS,
            ChannelTabs.PLAYLISTS,
            ChannelTabs.ALBUMS,
            ChannelTabs.LIKES,
        )
}

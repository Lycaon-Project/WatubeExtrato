package org.schabi.newpipe.extractor.services.bandcamp.linkHandler

import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.UnsupportedTabException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory

public object BandcampChannelTabLinkHandlerFactory : ListLinkHandlerFactory() {

    @JvmStatic
    public fun getInstance(): BandcampChannelTabLinkHandlerFactory = this

    /**
     * Get a tab's URL suffix.
     *
     * These URLs don't actually exist on the Bandcamp website, as both albums and tracks are
     * listed on the main page, but redirect to the main page, which is perfect for us as we need a
     * unique URL for each tab.
     *
     * @param tab the tab value, which must not be null
     * @return a URL suffix
     * @throws UnsupportedTabException if the tab is not supported
     */
    @JvmStatic
    @Throws(UnsupportedTabException::class)
    public fun getUrlSuffix(tab: String): String = when (tab) {
        ChannelTabs.TRACKS -> "/track"
        ChannelTabs.ALBUMS -> "/album"
        else -> throw UnsupportedTabException(tab)
    }

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getId(url: String?): String = BandcampChannelLinkHandlerFactory.getId(url)

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
    ): String {
        val tab = contentFilter?.firstOrNull() ?: throw ParsingException("No channel tab given")
        // Checked first: an unsupported tab needs no request to the mobile API
        val suffix = getUrlSuffix(tab)
        return BandcampChannelLinkHandlerFactory.getUrl(id) + suffix
    }

    @Throws(ParsingException::class)
    override fun onAcceptUrl(url: String?): Boolean = BandcampChannelLinkHandlerFactory.onAcceptUrl(url)

    override val availableContentFilter: Array<String>
        get() = arrayOf(
            ChannelTabs.TRACKS,
            ChannelTabs.ALBUMS,
        )
}

package org.schabi.newpipe.extractor.services.bandcamp

import org.junit.jupiter.api.Disabled
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ServiceList.Bandcamp
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.services.DefaultListExtractorTest

class BandcampChannelTabExtractorTest {

    class Tracks : DefaultListExtractorTest<ChannelTabExtractor>() {

        override fun createExtractor(): ChannelTabExtractor =
            Bandcamp.getChannelTabExtractorFromId("2464198920", ChannelTabs.TRACKS)

        override fun expectedService(): StreamingService = Bandcamp
        override fun expectedName(): String = ChannelTabs.TRACKS
        override fun expectedId(): String = "2464198920"
        override fun expectedUrlContains(): String = "https://wintergatan.bandcamp.com/track"
        override fun expectedOriginalUrlContains(): String = "https://wintergatan.bandcamp.com/track"
        override fun expectedInfoItemType(): InfoItem.InfoType = InfoItem.InfoType.STREAM
        override fun expectedHasMoreItems(): Boolean = false
    }

    @Disabled(
        "broken upstream as well (TeamNewPipe/NewPipeExtractor dev 6faf4068, 2026-10): " +
            "the recorded albums tab is empty"
    )
    class Albums : DefaultListExtractorTest<ChannelTabExtractor>() {

        override fun createExtractor(): ChannelTabExtractor =
            Bandcamp.getChannelTabExtractorFromId("2450875064", ChannelTabs.ALBUMS)

        override fun expectedService(): StreamingService = Bandcamp
        override fun expectedName(): String = ChannelTabs.ALBUMS
        override fun expectedId(): String = "2450875064"
        override fun expectedUrlContains(): String = "https://toupie.bandcamp.com/album"
        override fun expectedOriginalUrlContains(): String = "https://toupie.bandcamp.com/album"
        override fun expectedInfoItemType(): InfoItem.InfoType = InfoItem.InfoType.PLAYLIST
        override fun expectedHasMoreItems(): Boolean = false
    }
}

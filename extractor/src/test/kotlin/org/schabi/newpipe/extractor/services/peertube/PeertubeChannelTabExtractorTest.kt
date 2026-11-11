package org.schabi.newpipe.extractor.services.peertube

import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ServiceList.PeerTube
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.services.DefaultListExtractorTest
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestGetPageInNewExtractor

class PeertubeChannelTabExtractorTest {

    class Videos : DefaultListExtractorTest<ChannelTabExtractor>() {

        override fun createExtractor(): ChannelTabExtractor {
            // setting instance might break test when running in parallel
            PeerTube.instance = PeertubeInstance("https://framatube.org", "Framatube")
            return PeerTube.getChannelTabExtractorFromId(
                "video-channels/lqdn_channel@video.lqdn.fr",
                ChannelTabs.VIDEOS
            )
        }

        override fun expectedService(): StreamingService = PeerTube
        override fun expectedName(): String = ChannelTabs.VIDEOS
        override fun expectedId(): String = "video-channels/lqdn_channel@video.lqdn.fr"
        override fun expectedUrlContains(): String =
            "https://framatube.org/video-channels/lqdn_channel@video.lqdn.fr/videos"
        override fun expectedOriginalUrlContains(): String =
            "https://framatube.org/video-channels/lqdn_channel@video.lqdn.fr/videos"
        override fun expectedInfoItemType(): InfoItem.InfoType = InfoItem.InfoType.STREAM
        override fun expectedHasMoreItems(): Boolean = true

        @Test
        fun testGetPageInNewExtractor() {
            val newTabExtractor = PeerTube.getChannelTabExtractorFromId(
                "video-channels/lqdn_channel@video.lqdn.fr",
                ChannelTabs.VIDEOS
            )
            defaultTestGetPageInNewExtractor(extractor(), newTabExtractor)
        }
    }

    class Playlists : DefaultListExtractorTest<ChannelTabExtractor>() {

        override fun createExtractor(): ChannelTabExtractor =
            PeerTube.getChannelTabExtractorFromIdAndBaseUrl(
                "video-channels/lqdn_channel@video.lqdn.fr",
                ChannelTabs.PLAYLISTS,
                "https://framatube.org"
            )

        override fun expectedService(): StreamingService = PeerTube
        override fun expectedName(): String = ChannelTabs.PLAYLISTS
        override fun expectedId(): String = "video-channels/lqdn_channel@video.lqdn.fr"
        override fun expectedUrlContains(): String =
            "https://framatube.org/video-channels/lqdn_channel@video.lqdn.fr/video-playlists"
        override fun expectedOriginalUrlContains(): String =
            "https://framatube.org/video-channels/lqdn_channel@video.lqdn.fr/video-playlists"
        override fun expectedInfoItemType(): InfoItem.InfoType = InfoItem.InfoType.PLAYLIST
        override fun expectedHasMoreItems(): Boolean = false
    }

    class Channels : DefaultListExtractorTest<ChannelTabExtractor>() {

        override fun createExtractor(): ChannelTabExtractor =
            PeerTube.getChannelTabExtractorFromIdAndBaseUrl(
                "accounts/framasoft",
                ChannelTabs.CHANNELS,
                "https://framatube.org"
            )

        override fun expectedService(): StreamingService = PeerTube
        override fun expectedName(): String = ChannelTabs.CHANNELS
        override fun expectedId(): String = "accounts/framasoft"
        override fun expectedUrlContains(): String = "https://framatube.org/accounts/framasoft/video-channels"
        override fun expectedOriginalUrlContains(): String = "https://framatube.org/accounts/framasoft/video-channels"
        override fun expectedInfoItemType(): InfoItem.InfoType = InfoItem.InfoType.CHANNEL
        override fun expectedHasMoreItems(): Boolean = true
    }
}

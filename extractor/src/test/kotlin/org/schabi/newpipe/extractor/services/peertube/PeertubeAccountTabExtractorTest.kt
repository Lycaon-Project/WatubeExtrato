package org.schabi.newpipe.extractor.services.peertube

import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ServiceList.PeerTube
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.services.DefaultListExtractorTest

class PeertubeAccountTabExtractorTest {

    class Videos : DefaultListExtractorTest<ChannelTabExtractor>() {

        override fun createExtractor(): ChannelTabExtractor {
            // setting instance might break test when running in parallel
            PeerTube.instance = PeertubeInstance("https://framatube.org", "Framatube")
            return PeerTube.getChannelTabExtractorFromId("accounts/framasoft", ChannelTabs.VIDEOS)
        }

        override fun expectedService(): StreamingService = PeerTube
        override fun expectedName(): String = ChannelTabs.VIDEOS
        override fun expectedId(): String = "accounts/framasoft"
        override fun expectedUrlContains(): String = "https://framatube.org/accounts/framasoft/videos"
        override fun expectedOriginalUrlContains(): String = "https://framatube.org/accounts/framasoft/videos"
        override fun expectedInfoItemType(): InfoItem.InfoType = InfoItem.InfoType.STREAM
        override fun expectedHasMoreItems(): Boolean = true
    }

    class Channels : DefaultListExtractorTest<ChannelTabExtractor>() {

        override fun createExtractor(): ChannelTabExtractor {
            // setting instance might break test when running in parallel
            PeerTube.instance = PeertubeInstance("https://framatube.org", "Framatube")
            return PeerTube.getChannelTabExtractorFromId("accounts/framasoft", ChannelTabs.CHANNELS)
        }

        override fun expectedService(): StreamingService = PeerTube
        override fun expectedName(): String = ChannelTabs.CHANNELS
        override fun expectedId(): String = "accounts/framasoft"
        override fun expectedUrlContains(): String = "https://framatube.org/accounts/framasoft/video-channels"
        override fun expectedOriginalUrlContains(): String = "https://framatube.org/accounts/framasoft/video-channels"
        override fun expectedInfoItemType(): InfoItem.InfoType = InfoItem.InfoType.CHANNEL
        override fun expectedHasMoreItems(): Boolean = true
    }
}

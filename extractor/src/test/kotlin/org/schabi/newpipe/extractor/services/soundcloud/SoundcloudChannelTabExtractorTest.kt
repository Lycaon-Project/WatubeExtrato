package org.schabi.newpipe.extractor.services.soundcloud

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ExtractorAsserts.assertEmptyErrors
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ServiceList.SoundCloud
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.services.DefaultListExtractorTest
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestGetPageInNewExtractor

class SoundcloudChannelTabExtractorTest {

    class Tracks : DefaultListExtractorTest<ChannelTabExtractor>() {
        override fun createExtractor(): ChannelTabExtractor =
            SoundCloud.getChannelTabExtractorFromId("10494998", ChannelTabs.TRACKS)

        override fun expectedService(): StreamingService = SoundCloud
        override fun expectedName(): String = ChannelTabs.TRACKS
        override fun expectedId(): String = "10494998"
        override fun expectedUrlContains(): String = "https://soundcloud.com/liluzivert/tracks"
        override fun expectedOriginalUrlContains(): String = "https://soundcloud.com/liluzivert/tracks"
        override fun expectedInfoItemType(): InfoItem.InfoType = InfoItem.InfoType.STREAM

        @Test
        fun testGetPageInNewExtractor() {
            val newTabExtractor = SoundCloud.getChannelTabExtractorFromId("10494998", ChannelTabs.TRACKS)
            defaultTestGetPageInNewExtractor(extractor(), newTabExtractor)
        }
    }

    class Playlists : DefaultListExtractorTest<ChannelTabExtractor>() {
        override fun createExtractor(): ChannelTabExtractor =
            SoundCloud.getChannelTabExtractorFromId("157193072", ChannelTabs.PLAYLISTS)

        override fun expectedService(): StreamingService = SoundCloud
        override fun expectedName(): String = ChannelTabs.PLAYLISTS
        override fun expectedId(): String = "157193072"
        override fun expectedUrlContains(): String = "https://soundcloud.com/neffexmusic/sets"
        override fun expectedOriginalUrlContains(): String = "https://soundcloud.com/neffexmusic/sets"
        override fun expectedInfoItemType(): InfoItem.InfoType = InfoItem.InfoType.PLAYLIST
    }

    class Albums : DefaultListExtractorTest<ChannelTabExtractor>() {
        override fun createExtractor(): ChannelTabExtractor =
            SoundCloud.getChannelTabExtractorFromId("4803918", ChannelTabs.ALBUMS)

        override fun expectedService(): StreamingService = SoundCloud
        override fun expectedName(): String = ChannelTabs.ALBUMS
        override fun expectedId(): String = "4803918"
        override fun expectedUrlContains(): String = "https://soundcloud.com/bigsean-1/albums"
        override fun expectedOriginalUrlContains(): String = "https://soundcloud.com/bigsean-1/albums"
        override fun expectedInfoItemType(): InfoItem.InfoType = InfoItem.InfoType.PLAYLIST
    }

    class LikesOnlyTracks : DefaultListExtractorTest<ChannelTabExtractor>() {
        override fun createExtractor(): ChannelTabExtractor =
            SoundCloud.getChannelTabExtractorFromId("30854092", ChannelTabs.LIKES)

        override fun expectedService(): StreamingService = SoundCloud
        override fun expectedName(): String = ChannelTabs.LIKES
        override fun expectedId(): String = "30854092"
        override fun expectedUrlContains(): String = "https://soundcloud.com/lubenitza/likes"
        override fun expectedOriginalUrlContains(): String = "https://soundcloud.com/lubenitza/likes"
        override fun expectedInfoItemType(): InfoItem.InfoType = InfoItem.InfoType.STREAM
    }

    class LikesOnlyPlaylists : DefaultListExtractorTest<ChannelTabExtractor>() {
        override fun createExtractor(): ChannelTabExtractor =
            SoundCloud.getChannelTabExtractorFromId("1280839267", ChannelTabs.LIKES)

        override fun expectedService(): StreamingService = SoundCloud
        override fun expectedName(): String = ChannelTabs.LIKES
        override fun expectedId(): String = "1280839267"
        override fun expectedUrlContains(): String = "https://soundcloud.com/soreen-735855039/likes"
        override fun expectedOriginalUrlContains(): String = "https://soundcloud.com/soreen-735855039/likes"
        override fun expectedInfoItemType(): InfoItem.InfoType = InfoItem.InfoType.PLAYLIST
    }

    class NoEmptyNextPages : DefaultListExtractorTest<ChannelTabExtractor>() {
        override fun createExtractor(): ChannelTabExtractor =
            SoundCloud.getChannelTabExtractorFromId("73637815", ChannelTabs.TRACKS)

        override fun expectedService(): StreamingService = SoundCloud
        override fun expectedName(): String = ChannelTabs.TRACKS
        override fun expectedId(): String = "73637815"
        override fun expectedUrlContains(): String = "https://soundcloud.com/hurtbox/tracks"
        override fun expectedOriginalUrlContains(): String = "https://soundcloud.com/hurtbox/tracks"
        override fun expectedInfoItemType(): InfoItem.InfoType = InfoItem.InfoType.STREAM

        @Test
        fun testNextPages() {
            var page = extractor().initialPage
            for (i in 1..5) {
                assertEmptyErrors("Next page has errors", page.errors)
                assertFalse(page.items.isEmpty(), "Next page is empty")
                assertTrue(page.hasNextPage(), "Next page does not have more items")
                page = extractor().getPage(page.nextPage)
            }
        }
    }
}

package org.schabi.newpipe.extractor.services.peertube

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ExtractorAsserts
import org.schabi.newpipe.extractor.ServiceList.PeerTube
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestImageCollection

class PeertubePlaylistExtractorTest {

    class Shocking : DefaultSimpleExtractorTest<PlaylistExtractor>() {

        override fun createExtractor(): PlaylistExtractor =
            PeerTube.getPlaylistExtractor(
                "https://framatube.org/videos/watch/playlist/96b0ee2b-a5a7-4794-8769-58d8ccb79ab7"
            )

        @Test
        fun testGetName() {
            assertEquals("Shocking !", extractor().name)
        }

        @Test
        fun testGetThumbnails() {
            defaultTestImageCollection(extractor().thumbnails)
        }

        @Test
        fun testGetUploaderUrl() {
            assertEquals("https://skeptikon.fr/accounts/metadechoc", extractor().uploaderUrl)
        }

        @Test
        fun testGetUploaderAvatars() {
            defaultTestImageCollection(extractor().uploaderAvatars)
        }

        @Test
        fun testGetUploaderName() {
            assertEquals("Méta de Choc", extractor().uploaderName)
        }

        @Test
        fun testGetStreamCount() {
            ExtractorAsserts.assertGreaterOrEqual(39, extractor().streamCount)
        }

        @Test
        fun testGetDescription() {
            ExtractorAsserts.assertContains("épisodes de Shocking", extractor().description.content)
        }

        @Test
        fun testGetSubChannelUrl() {
            assertEquals("https://skeptikon.fr/video-channels/metadechoc_channel", extractor().subChannelUrl)
        }

        @Test
        fun testGetSubChannelName() {
            assertEquals("SHOCKING\u00A0!", extractor().subChannelName)
        }

        @Test
        fun testGetSubChannelAvatars() {
            defaultTestImageCollection(extractor().subChannelAvatars)
        }
    }
}

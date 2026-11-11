package org.schabi.newpipe.extractor.services.media_ccc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ExtractorAsserts.assertContainsImageUrlInImageCollection
import org.schabi.newpipe.extractor.InitNewPipeTest
import org.schabi.newpipe.extractor.ServiceList.MediaCCC
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.services.DefaultStreamExtractorTest
import org.schabi.newpipe.extractor.services.media_ccc.extractors.MediaCCCStreamExtractor
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamType
import java.util.Locale

/**
 * Test [MediaCCCStreamExtractor]
 */
class MediaCCCStreamExtractorTest {

    class Gpn18Tmux :
        DefaultStreamExtractorTest(),
        InitNewPipeTest {

        override fun createExtractor(): StreamExtractor = MediaCCC.getStreamExtractor(URL)

        override fun expectedService(): StreamingService = MediaCCC
        override fun expectedName(): String = "tmux - Warum ein schwarzes Fenster am Bildschirm reicht"
        override fun expectedId(): String = ID
        override fun expectedUrlContains(): String = URL
        override fun expectedOriginalUrlContains(): String = URL

        override fun expectedStreamType(): StreamType = StreamType.VIDEO_STREAM
        override fun expectedUploaderName(): String = "gpn18"
        override fun expectedUploaderUrl(): String = "https://media.ccc.de/c/gpn18"
        override fun expectedDescriptionContains(): List<String> = listOf("SSH-Sessions", "\"Terminal Multiplexer\"")
        override fun expectedLength(): Long = 3097
        override fun expectedViewCountAtLeast(): Long = 2380
        override fun expectedUploadDate(): String? = "2018-05-11 00:00:00.000"
        override fun expectedTextualUploadDate(): String? = "2018-05-11T02:00:00.000+02:00"
        override fun expectedLikeCountAtLeast(): Long = -1
        override fun expectedDislikeCountAtLeast(): Long = -1
        override fun expectedHasRelatedItems(): Boolean = false
        override fun expectedHasSubtitles(): Boolean = false
        override fun expectedHasFrames(): Boolean = false
        override fun expectedTags(): List<String> = listOf("gpn18", "105")
        override fun expectedStreamSegmentsCount(): Int = 0
        override fun expectedLanguageInfo(): Locale? = Locale.forLanguageTag("de")

        @Test
        override fun testThumbnails() {
            super.testThumbnails()
            assertContainsImageUrlInImageCollection(
                "https://static.media.ccc.de/media/events/gpn/gpn18/105-hd_preview.jpg",
                extractor().thumbnails
            )
        }

        @Test
        override fun testUploaderAvatars() {
            super.testUploaderAvatars()
            assertContainsImageUrlInImageCollection(
                "https://static.media.ccc.de/media/events/gpn/gpn18/logo.png",
                extractor().uploaderAvatars
            )
        }

        @Test
        override fun testVideoStreams() {
            super.testVideoStreams()
            assertEquals(4, checkNotNull(extractor().videoStreams).size)
        }

        @Test
        override fun testAudioStreams() {
            super.testAudioStreams()
            val audioStreams = checkNotNull(extractor().audioStreams)
            assertEquals(2, audioStreams.size)

            val expectedLocale = Locale.forLanguageTag("deu")
            assertTrue(audioStreams.all { it.audioLocale == expectedLocale })
        }

        companion object {
            private const val ID = "gpn18-105-tmux-warum-ein-schwarzes-fenster-am-bildschirm-reicht"
            private const val URL = BASE_URL + ID
        }
    }

    @Suppress("ClassName") // the name mirrors the media.ccc.de conference id; kept for the mock path
    class _36c3PrivacyMessaging : DefaultStreamExtractorTest() {

        override fun createExtractor(): StreamExtractor = MediaCCC.getStreamExtractor(URL)

        override fun expectedService(): StreamingService = MediaCCC
        override fun expectedName(): String = "What's left for private messaging?"
        override fun expectedId(): String = ID
        override fun expectedUrlContains(): String = URL
        override fun expectedOriginalUrlContains(): String = URL

        override fun expectedStreamType(): StreamType = StreamType.VIDEO_STREAM
        override fun expectedUploaderName(): String = "36c3"
        override fun expectedUploaderUrl(): String = "https://media.ccc.de/c/36c3"
        override fun expectedDescriptionContains(): List<String> = listOf("WhatsApp", "Signal")
        override fun expectedLength(): Long = 3603
        override fun expectedViewCountAtLeast(): Long = 2380
        override fun expectedUploadDate(): String? = "2020-01-11 00:00:00.000"
        override fun expectedTextualUploadDate(): String? = "2020-01-11T01:00:00.000+01:00"
        override fun expectedLikeCountAtLeast(): Long = -1
        override fun expectedDislikeCountAtLeast(): Long = -1
        override fun expectedHasRelatedItems(): Boolean = false
        override fun expectedHasSubtitles(): Boolean = false
        override fun expectedHasFrames(): Boolean = false
        override fun expectedTags(): List<String> = listOf("36c3", "10565", "2019", "Security", "Main")

        @Test
        override fun testThumbnails() {
            super.testThumbnails()
            assertContainsImageUrlInImageCollection(
                "https://static.media.ccc.de/media/congress/2019/10565-hd_preview.jpg",
                extractor().thumbnails
            )
        }

        @Test
        override fun testUploaderAvatars() {
            super.testUploaderAvatars()
            assertContainsImageUrlInImageCollection(
                "https://static.media.ccc.de/media/congress/2019/logo.png",
                extractor().uploaderAvatars
            )
        }

        @Test
        override fun testVideoStreams() {
            super.testVideoStreams()
            assertEquals(8, checkNotNull(extractor().videoStreams).size)
        }

        @Test
        override fun testAudioStreams() {
            super.testAudioStreams()
            val audioStreams = checkNotNull(extractor().audioStreams)
            assertEquals(2, audioStreams.size)

            val expectedLocale = Locale.forLanguageTag("eng")
            assertTrue(audioStreams.all { it.audioLocale == expectedLocale })
        }

        override fun expectedLanguageInfo(): Locale? = Locale.forLanguageTag("en")

        companion object {
            private const val ID = "36c3-10565-what_s_left_for_private_messaging"
            private const val URL = BASE_URL + ID
        }
    }

    companion object {
        private const val BASE_URL = "https://media.ccc.de/v/"
    }
}

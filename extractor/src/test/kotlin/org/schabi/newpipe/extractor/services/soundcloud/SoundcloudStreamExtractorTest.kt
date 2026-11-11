package org.schabi.newpipe.extractor.services.soundcloud

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ExtractorAsserts
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.ServiceList.SoundCloud
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.GeographicRestrictionException
import org.schabi.newpipe.extractor.exceptions.SoundCloudGoPlusContentException
import org.schabi.newpipe.extractor.services.DefaultStreamExtractorTest
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamType

private const val SOUNDCLOUD = "https://soundcloud.com/"

class SoundcloudStreamExtractorTest {

    @Nested
    inner class SoundcloudGeoRestrictedTrack : DefaultStreamExtractorTest() {
        private val id = "one-touch"
        private val uploader = SOUNDCLOUD + "jessglynne"
        private val timestamp = 0
        private val url = "$uploader/$id#t=$timestamp"

        override fun createExtractor(): StreamExtractor = SoundCloud.getStreamExtractor(url)

        override fun fetchExtractor(extractor: StreamExtractor) {
            try {
                super.fetchExtractor(extractor)
            } catch (e: GeographicRestrictionException) {
                // expected
            }
        }

        override fun expectedService(): StreamingService = SoundCloud
        override fun expectedName(): String = "One Touch"
        override fun expectedId(): String = "621612588"
        override fun expectedUrlContains(): String = "$uploader/$id"
        override fun expectedOriginalUrlContains(): String = url

        override fun expectedStreamType(): StreamType = StreamType.AUDIO_STREAM
        override fun expectedUploaderName(): String = "Jess Glynne"
        override fun expectedUploaderUrl(): String = uploader
        override fun expectedUploaderVerified(): Boolean = true
        override fun expectedDescriptionIsEmpty(): Boolean = true
        override fun expectedDescriptionContains(): List<String> = emptyList()
        override fun expectedLength(): Long = 197
        override fun expectedTimestamp(): Long = timestamp.toLong()
        override fun expectedViewCountAtLeast(): Long = 43000
        override fun expectedUploadDate(): String? = "2019-05-16 16:28:45.000"
        override fun expectedTextualUploadDate(): String? = "2019-05-16T16:28:45Z"
        override fun expectedLikeCountAtLeast(): Long = 600
        override fun expectedDislikeCountAtLeast(): Long = -1
        override fun expectedHasAudioStreams(): Boolean = false
        override fun expectedHasVideoStreams(): Boolean = false
        override fun expectedHasSubtitles(): Boolean = false
        override fun expectedHasFrames(): Boolean = false
        override fun expectedStreamSegmentsCount(): Int = 0
        override fun expectedLicence(): String = "all-rights-reserved"
        override fun expectedCategory(): String = "Pop"

        @Test
        @Disabled("Unreliable, sometimes it has related items, sometimes it does not")
        override fun testRelatedItems() {
            super.testRelatedItems()
        }
    }

    @Nested
    inner class SoundcloudGoPlusTrack : DefaultStreamExtractorTest() {
        private val id = "places"
        private val uploader = SOUNDCLOUD + "martinsolveig"
        private val timestamp = 0
        private val url = "$uploader/$id#t=$timestamp"

        override fun createExtractor(): StreamExtractor = SoundCloud.getStreamExtractor(url)

        override fun fetchExtractor(extractor: StreamExtractor) {
            try {
                super.fetchExtractor(extractor)
            } catch (e: SoundCloudGoPlusContentException) {
                // expected
            }
        }

        @Test
        @Disabled(
            "Unreliable, sometimes it has related items, sometimes it does not. See " +
                "https://github.com/TeamNewPipe/NewPipeExtractor/runs/2280013723#step:5:263 " +
                "https://github.com/TeamNewPipe/NewPipeExtractor/pull/601"
        )
        override fun testRelatedItems() {
            super.testRelatedItems()
        }

        override fun expectedService(): StreamingService = SoundCloud
        override fun expectedName(): String = "Places (feat. Ina Wroldsen)"
        override fun expectedId(): String = "292479564"
        override fun expectedUrlContains(): String = "$uploader/$id"
        override fun expectedOriginalUrlContains(): String = url

        override fun expectedStreamType(): StreamType = StreamType.AUDIO_STREAM
        override fun expectedUploaderName(): String = "martinsolveig"
        override fun expectedUploaderUrl(): String = uploader
        override fun expectedUploaderVerified(): Boolean = true
        override fun expectedDescriptionIsEmpty(): Boolean = true
        override fun expectedDescriptionContains(): List<String> = emptyList()
        override fun expectedLength(): Long = 30
        override fun expectedTimestamp(): Long = timestamp.toLong()
        override fun expectedViewCountAtLeast(): Long = 386000
        override fun expectedUploadDate(): String? = "2016-11-11 01:16:37.000"
        override fun expectedTextualUploadDate(): String? = "2016-11-11T01:16:37Z"
        override fun expectedLikeCountAtLeast(): Long = 7350
        override fun expectedDislikeCountAtLeast(): Long = -1
        override fun expectedHasAudioStreams(): Boolean = false
        override fun expectedHasVideoStreams(): Boolean = false
        override fun expectedHasSubtitles(): Boolean = false
        override fun expectedHasFrames(): Boolean = false
        override fun expectedStreamSegmentsCount(): Int = 0
        override fun expectedLicence(): String = "all-rights-reserved"
        override fun expectedCategory(): String = "Dance"
    }

    @Nested
    inner class CreativeCommonsOpenMindsEp21 : DefaultStreamExtractorTest() {
        private val id = "open-minds-ep-21-dr-beth-harris-and-dr-steven-zucker-of-smarthistory"
        private val uploader = SOUNDCLOUD + "wearecc"
        private val timestamp = 69
        private val url = "$uploader/$id#t=$timestamp"

        override fun createExtractor(): StreamExtractor = SoundCloud.getStreamExtractor(url)

        override fun expectedService(): StreamingService = SoundCloud
        override fun expectedName(): String =
            "Open Minds, Ep 21: Dr. Beth Harris and Dr. Steven Zucker of Smarthistory"
        override fun expectedId(): String = "1356023209"
        override fun expectedUrlContains(): String = "$uploader/$id"
        override fun expectedOriginalUrlContains(): String = url

        override fun expectedStreamType(): StreamType = StreamType.AUDIO_STREAM
        override fun expectedUploaderName(): String = "Creative Commons"
        override fun expectedUploaderUrl(): String = uploader
        override fun expectedDescriptionContains(): List<String> = listOf(
            "Smarthistory is a center for public art history",
            "experts who want to share their knowledge with learners around the world",
            "Available for use under the CC BY 3.0 license"
        )
        override fun expectedLength(): Long = 1500
        override fun expectedTimestamp(): Long = timestamp.toLong()
        override fun expectedViewCountAtLeast(): Long = 15000
        override fun expectedUploadDate(): String? = "2022-10-03 18:49:49.000"
        override fun expectedTextualUploadDate(): String? = "2022-10-03T18:49:49Z"
        override fun expectedLikeCountAtLeast(): Long = 10
        override fun expectedDislikeCountAtLeast(): Long = -1
        override fun expectedHasRelatedItems(): Boolean = false
        override fun expectedHasVideoStreams(): Boolean = false
        override fun expectedHasSubtitles(): Boolean = false
        override fun expectedHasFrames(): Boolean = false
        override fun expectedStreamSegmentsCount(): Int = 0
        override fun expectedLicence(): String = "cc-by"

        @Test
        override fun testAudioStreams() {
            super.testAudioStreams()
            val audioStreams = extractor().audioStreams!!
            assertEquals(3, audioStreams.size) // 2 MP3 streams (1 progressive, 1 HLS) and 1 OPUS
            audioStreams.forEach { audioStream ->
                val deliveryMethod = audioStream.deliveryMethod
                val mediaUrl = audioStream.content
                if (audioStream.format == MediaFormat.OPUS) {
                    assertSame(
                        DeliveryMethod.HLS, deliveryMethod,
                        "Wrong delivery method for stream ${audioStream.id}: $deliveryMethod"
                    )
                    // Assert it's an OPUS 64 kbps media playlist URL which comes from an HLS
                    // SoundCloud CDN
                    ExtractorAsserts.assertContains("-hls-opus-media.sndcdn.com", mediaUrl)
                    ExtractorAsserts.assertContains(".64.opus", mediaUrl)
                } else if (audioStream.format == MediaFormat.MP3) {
                    if (deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP) {
                        // Assert it's a MP3 128 kbps media URL which comes from a progressive
                        // SoundCloud CDN
                        ExtractorAsserts.assertContains("-media.sndcdn.com/cyaz0oXJYbdt.128.mp3", mediaUrl)
                    } else if (deliveryMethod == DeliveryMethod.HLS) {
                        // Assert it's a MP3 128 kbps media HLS playlist URL which comes from an HLS
                        // SoundCloud CDN
                        ExtractorAsserts.assertContains("-hls-media.sndcdn.com", mediaUrl)
                        ExtractorAsserts.assertContains(".128.mp3", mediaUrl)
                    } else {
                        fail<Unit>("Wrong delivery method for stream ${audioStream.id}: $deliveryMethod")
                    }
                }
            }
        }
    }

    @Nested
    inner class SmallTrackId : DefaultStreamExtractorTest() {
        private val id = "technicality"
        private val uploader = SOUNDCLOUD + "garrett-cameron-1"
        private val timestamp = 34
        private val url = "$uploader/$id#t=$timestamp"

        override fun createExtractor(): StreamExtractor = SoundCloud.getStreamExtractor(url)

        override fun expectedService(): StreamingService = SoundCloud
        override fun expectedName(): String = "Technicality"
        override fun expectedId(): String = "135535700"
        override fun expectedUrlContains(): String = "$uploader/$id"
        override fun expectedOriginalUrlContains(): String = url

        override fun expectedStreamType(): StreamType = StreamType.AUDIO_STREAM
        override fun expectedUploaderName(): String = "Samuel Cameron (ZVW)"
        override fun expectedUploaderUrl(): String = uploader
        override fun expectedDescriptionIsEmpty(): Boolean = true
        override fun expectedDescriptionContains(): List<String> = emptyList()
        override fun expectedLength(): Long = 182
        override fun expectedTimestamp(): Long = timestamp.toLong()
        override fun expectedViewCountAtLeast(): Long = 16
        override fun expectedUploadDate(): String? = "2014-02-18 21:25:01.000"
        override fun expectedTextualUploadDate(): String? = "2014-02-18T21:25:01Z"
        override fun expectedLikeCountAtLeast(): Long = 1
        override fun expectedDislikeCountAtLeast(): Long = -1
        override fun expectedHasVideoStreams(): Boolean = false
        override fun expectedHasSubtitles(): Boolean = false
        override fun expectedHasFrames(): Boolean = false
        override fun expectedStreamSegmentsCount(): Int = 0
        override fun expectedLicence(): String = "all-rights-reserved"
        override fun expectedCategory(): String = "Electronic"
    }

    @Nested
    inner class LongTrackId : DefaultStreamExtractorTest() {
        private val id = "hard-to-break"
        private val uploader = SOUNDCLOUD + "blakewhiten"
        private val timestamp = 130
        private val url = "$uploader/$id#t=$timestamp"

        override fun createExtractor(): StreamExtractor = SoundCloud.getStreamExtractor(url)

        override fun expectedService(): StreamingService = SoundCloud
        override fun expectedName(): String = "Hard to Break"
        override fun expectedId(): String = "2167944333"
        override fun expectedUrlContains(): String = "$uploader/$id"
        override fun expectedOriginalUrlContains(): String = url

        override fun expectedStreamType(): StreamType = StreamType.AUDIO_STREAM
        override fun expectedUploaderName(): String = "Blake Whiten"
        override fun expectedUploaderUrl(): String = uploader
        override fun expectedDescriptionIsEmpty(): Boolean = true
        override fun expectedDescriptionContains(): List<String> = emptyList()
        override fun expectedLength(): Long = 201
        override fun expectedTimestamp(): Long = timestamp.toLong()
        override fun expectedViewCountAtLeast(): Long = 120222
        override fun expectedUploadDate(): String? = "2025-09-10 02:24:00.000"
        override fun expectedTextualUploadDate(): String? = "2025-09-10T02:24:00Z"
        override fun expectedLikeCountAtLeast(): Long = 1970
        override fun expectedDislikeCountAtLeast(): Long = -1
        override fun expectedHasVideoStreams(): Boolean = false
        override fun expectedHasSubtitles(): Boolean = false
        override fun expectedHasFrames(): Boolean = false
        override fun expectedStreamSegmentsCount(): Int = 0
        override fun expectedLicence(): String = "all-rights-reserved"
        override fun expectedCategory(): String = "Country"
    }
}

package org.schabi.newpipe.extractor.services

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.schabi.newpipe.extractor.ExtractorAsserts
import org.schabi.newpipe.extractor.ExtractorAsserts.assertContains
import org.schabi.newpipe.extractor.ExtractorAsserts.assertEmpty
import org.schabi.newpipe.extractor.ExtractorAsserts.assertEqualsOrderIndependent
import org.schabi.newpipe.extractor.ExtractorAsserts.assertGreaterOrEqual
import org.schabi.newpipe.extractor.ExtractorAsserts.assertIsSecureUrl
import org.schabi.newpipe.extractor.ExtractorAsserts.assertIsValidUrl
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.MetaInfo
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestImageCollection
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestListOfItems
import org.schabi.newpipe.extractor.stream.ContentAvailability
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamExtractor.Companion.UNKNOWN_SUBSCRIBER_COUNT
import org.schabi.newpipe.extractor.stream.StreamType
import java.net.MalformedURLException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Test for [StreamExtractor]
 */
abstract class DefaultStreamExtractorTest :
    DefaultExtractorTest<StreamExtractor>(),
    BaseStreamExtractorTest {

    abstract fun expectedStreamType(): StreamType
    abstract fun expectedUploaderName(): String
    abstract fun expectedUploaderUrl(): String
    open fun expectedUploaderVerified(): Boolean = false
    open fun expectedUploaderSubscriberCountAtLeast(): Long = UNKNOWN_SUBSCRIBER_COUNT
    open fun expectedSubChannelName(): String = "" // default: there is no subchannel
    open fun expectedSubChannelUrl(): String = "" // default: there is no subchannel
    open fun expectedDescriptionIsEmpty(): Boolean = false // default: description is not empty
    abstract fun expectedDescriptionContains(): List<String> // e.g. for full links
    abstract fun expectedLength(): Long
    open fun expectedTimestamp(): Long = 0 // default: there is no timestamp
    abstract fun expectedViewCountAtLeast(): Long
    abstract fun expectedUploadDate(): String? // format: "yyyy-MM-dd HH:mm:ss.SSS"
    abstract fun expectedTextualUploadDate(): String?
    abstract fun expectedLikeCountAtLeast(): Long // return -1 if ratings are disabled
    abstract fun expectedDislikeCountAtLeast(): Long // return -1 if ratings are disabled
    open fun expectedHasRelatedItems(): Boolean = true // default: there are related videos
    open fun expectedAgeLimit(): Int = StreamExtractor.NO_AGE_LIMIT // default: no limit
    open fun expectedErrorMessage(): String? = null // default: no error message
    open fun expectedHasVideoStreams(): Boolean = true // default: there are video streams
    open fun expectedHasAudioStreams(): Boolean = true // default: there are audio streams
    open fun expectedHasSubtitles(): Boolean = true // default: there are subtitles streams
    open fun expectedDashMpdUrlContains(): String? = null // default: no dash mpd
    open fun expectedHasFrames(): Boolean = true // default: there are frames
    open fun expectedHost(): String = "" // default: no host for centralized platforms
    open fun expectedPrivacy(): StreamExtractor.Privacy = StreamExtractor.Privacy.PUBLIC // default: public
    open fun expectedCategory(): String = "" // default: no category
    open fun expectedLicence(): String = "" // default: no licence
    open fun expectedLanguageInfo(): Locale? = null // default: no language info available
    open fun expectedTags(): List<String> = emptyList() // default: no tags
    open fun expectedSupportInfo(): String = "" // default: no support info available

    // return 0 or greater to test (default is -1 to ignore)
    open fun expectedStreamSegmentsCount(): Int = -1

    // default: no metadata info available
    @Throws(MalformedURLException::class)
    open fun expectedMetaInfo(): List<MetaInfo> = emptyList()

    // default: unknown content availability
    open fun expectedContentAvailability(): ContentAvailability = ContentAvailability.UNKNOWN

    @Test
    @Throws(Exception::class)
    override fun testStreamType() {
        assertEquals(expectedStreamType(), extractor().streamType)
    }

    @Test
    @Throws(Exception::class)
    override fun testUploaderName() {
        assertEquals(expectedUploaderName(), extractor().uploaderName)
    }

    @Test
    @Throws(Exception::class)
    override fun testUploaderUrl() {
        val uploaderUrl = extractor().uploaderUrl
        assertIsSecureUrl(uploaderUrl)
        assertEquals(expectedUploaderUrl(), uploaderUrl)
    }

    @Test
    @Throws(Exception::class)
    override fun testUploaderAvatars() {
        defaultTestImageCollection(extractor().uploaderAvatars)
    }

    @Test
    @Throws(Exception::class)
    open fun testUploaderVerified() {
        assertEquals(expectedUploaderVerified(), extractor().isUploaderVerified)
    }

    @Test
    @Throws(Exception::class)
    override fun testSubscriberCount() {
        if (expectedUploaderSubscriberCountAtLeast() == UNKNOWN_SUBSCRIBER_COUNT) {
            assertEquals(UNKNOWN_SUBSCRIBER_COUNT, extractor().uploaderSubscriberCount)
        } else {
            assertGreaterOrEqual(
                expectedUploaderSubscriberCountAtLeast(),
                extractor().uploaderSubscriberCount,
            )
        }
    }

    @Test
    @Throws(Exception::class)
    override fun testSubChannelName() {
        assertEquals(expectedSubChannelName(), extractor().subChannelName)
    }

    @Test
    @Throws(Exception::class)
    override fun testSubChannelUrl() {
        val subChannelUrl = extractor().subChannelUrl
        assertEquals(expectedSubChannelUrl(), subChannelUrl)

        if (expectedSubChannelUrl().isNotEmpty()) {
            // this stream has a subchannel
            assertIsSecureUrl(subChannelUrl)
        }
    }

    @Test
    @Throws(Exception::class)
    override fun testSubChannelAvatars() {
        if (expectedSubChannelName().isEmpty() && expectedSubChannelUrl().isEmpty()) {
            // this stream has no subchannel
            assertEmpty(extractor().subChannelAvatars)
        } else {
            // this stream has a subchannel
            defaultTestImageCollection(extractor().subChannelAvatars)
        }
    }

    @Test
    @Throws(Exception::class)
    override fun testThumbnails() {
        defaultTestImageCollection(extractor().thumbnails)
    }

    @Test
    @Throws(Exception::class)
    override fun testDescription() {
        val description = extractor().description
        assertNotNull(description)

        if (expectedDescriptionIsEmpty()) {
            assertTrue(description.content.isEmpty(), "description is not empty")
        } else {
            assertFalse(description.content.isEmpty(), "description is empty")
        }

        for (s in expectedDescriptionContains()) {
            ExtractorAsserts.assertContains(s, description.content)
        }
    }

    @Test
    @Throws(Exception::class)
    override fun testLength() {
        assertEquals(expectedLength(), extractor().length)
    }

    @Test
    @Throws(Exception::class)
    override fun testTimestamp() {
        assertEquals(expectedTimestamp(), extractor().timeStamp)
    }

    @Test
    @Throws(Exception::class)
    override fun testViewCount() {
        assertGreaterOrEqual(expectedViewCountAtLeast(), extractor().viewCount)
    }

    @Test
    @Throws(Exception::class)
    override fun testUploadDate() {
        val dateWrapper = extractor().uploadDate
        val expectedDate = expectedUploadDate()

        if (expectedDate == null) {
            assertNull(dateWrapper)
        } else {
            assertNotNull(dateWrapper)

            val expectedDateTime = LocalDateTime.parse(
                expectedDate,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
            )

            assertEquals(expectedDateTime, dateWrapper!!.getLocalDateTime(ZoneOffset.UTC))
        }
    }

    @Test
    @Throws(Exception::class)
    override fun testTextualUploadDate() {
        assertEquals(expectedTextualUploadDate(), extractor().textualUploadDate)
    }

    @Test
    @Throws(Exception::class)
    override fun testLikeCount() {
        if (expectedLikeCountAtLeast() == -1L) {
            assertEquals(-1L, extractor().likeCount)
        } else {
            assertGreaterOrEqual(expectedLikeCountAtLeast(), extractor().likeCount)
        }
    }

    @Test
    @Throws(Exception::class)
    override fun testDislikeCount() {
        if (expectedDislikeCountAtLeast() == -1L) {
            assertEquals(-1L, extractor().dislikeCount)
        } else {
            assertGreaterOrEqual(expectedDislikeCountAtLeast(), extractor().dislikeCount)
        }
    }

    @Test
    @Throws(Exception::class)
    override fun testRelatedItems() {
        val relatedStreams = extractor().relatedItems

        if (expectedHasRelatedItems()) {
            assertNotNull(relatedStreams)
            defaultTestListOfItems(
                extractor().service,
                relatedStreams!!.items,
                relatedStreams.errors,
            )
        } else {
            assertTrue(relatedStreams == null || relatedStreams.items.isEmpty())
        }
    }

    @Test
    @Throws(Exception::class)
    override fun testAgeLimit() {
        assertEquals(expectedAgeLimit(), extractor().ageLimit)
    }

    @Test
    @Throws(Exception::class)
    override fun testErrorMessage() {
        assertEquals(expectedErrorMessage(), extractor().errorMessage)
    }

    @Test
    @Throws(Exception::class)
    override fun testVideoStreams() {
        val videoStreams = extractor().videoStreams
        val videoOnlyStreams = extractor().videoOnlyStreams
        assertNotNull(videoStreams)
        assertNotNull(videoOnlyStreams)
        // a copy: the lists of the extractor are read-only
        val allVideoStreams = videoStreams!! + videoOnlyStreams!!

        if (expectedHasVideoStreams()) {
            assertFalse(allVideoStreams.isEmpty())

            for (stream in allVideoStreams) {
                if (stream.isUrl) {
                    assertIsSecureUrl(stream.content)
                }
                val streamType = extractor().streamType
                // On some video streams, the resolution can be empty and the format be unknown,
                // especially on livestreams (like streams with HLS master playlists)
                if (streamType != StreamType.LIVE_STREAM &&
                    streamType != StreamType.AUDIO_LIVE_STREAM
                ) {
                    assertFalse(stream.getResolution().isEmpty())
                    val formatId = stream.formatId
                    // see MediaFormat: video stream formats range from 0 to 0x100
                    assertTrue(
                        formatId in 0 until 0x100,
                        "Format id does not fit a video stream: $formatId",
                    )
                }
            }
        } else {
            assertTrue(allVideoStreams.isEmpty())
        }
    }

    @Test
    @Throws(Exception::class)
    override fun testAudioStreams() {
        val audioStreams = extractor().audioStreams
        assertNotNull(audioStreams)

        if (expectedHasAudioStreams()) {
            assertFalse(audioStreams!!.isEmpty())

            for (stream in audioStreams) {
                if (stream.isUrl) {
                    assertIsSecureUrl(stream.content)
                }

                // The media format can be unknown on some audio streams
                val format = stream.format
                if (format != null) {
                    val formatId = format.id
                    // see MediaFormat: audio stream formats range from 0x100 to 0x1000
                    assertTrue(
                        formatId in 0x100 until 0x1000,
                        "Format id does not fit an audio stream: $formatId",
                    )
                }
            }
        } else {
            assertTrue(audioStreams!!.isEmpty())
        }
    }

    @Test
    @Throws(Exception::class)
    override fun testSubtitles() {
        val subtitles = extractor().subtitlesDefault
        assertNotNull(subtitles)

        if (expectedHasSubtitles()) {
            assertFalse(subtitles.isEmpty())

            for (stream in subtitles) {
                if (stream.isUrl) {
                    assertIsSecureUrl(stream.content)
                }

                val formatId = stream.formatId
                // see MediaFormat: video stream formats range from 0x1000 to 0x10000
                assertTrue(
                    formatId in 0x1000 until 0x10000,
                    "Format id does not fit a subtitles stream: $formatId",
                )
            }
        } else {
            assertTrue(subtitles.isEmpty())

            val formats = arrayOf(
                MediaFormat.VTT,
                MediaFormat.TTML,
                MediaFormat.SRT,
                MediaFormat.TRANSCRIPT1,
                MediaFormat.TRANSCRIPT2,
                MediaFormat.TRANSCRIPT3,
            )
            for (format in formats) {
                val formatSubtitles = extractor().getSubtitles(format)
                assertNotNull(formatSubtitles)
                assertTrue(formatSubtitles.isEmpty())
            }
        }
    }

    @Throws(Exception::class)
    override fun testGetDashMpdUrl() {
        val dashMpdUrl = extractor().dashMpdUrl
        val expectedDashMpdUrlContains = expectedDashMpdUrlContains()
        if (expectedDashMpdUrlContains == null) {
            assertNotNull(dashMpdUrl)
            assertTrue(dashMpdUrl.isEmpty())
        } else {
            assertIsSecureUrl(dashMpdUrl)
            ExtractorAsserts.assertContains(expectedDashMpdUrlContains, extractor().dashMpdUrl)
        }
    }

    @Test
    @Throws(Exception::class)
    override fun testFrames() {
        val frames = extractor().frames
        assertNotNull(frames)

        if (expectedHasFrames()) {
            assertFalse(frames.isEmpty())
            for (f in frames) {
                for (url in f.urls) {
                    assertIsValidUrl(url)
                    assertIsSecureUrl(url)
                }
                assertTrue(f.durationPerFrame > 0)
                assertEquals(f.getFrameBoundsAt(0)[3], f.frameWidth)
            }
        } else {
            assertTrue(frames.isEmpty())
        }
    }

    @Test
    @Throws(Exception::class)
    override fun testHost() {
        assertEquals(expectedHost(), extractor().host)
    }

    @Test
    @Throws(Exception::class)
    override fun testPrivacy() {
        assertEquals(expectedPrivacy(), extractor().privacy)
    }

    @Test
    @Throws(Exception::class)
    override fun testCategory() {
        assertEquals(expectedCategory(), extractor().category)
    }

    @Test
    @Throws(Exception::class)
    override fun testLicence() {
        assertEquals(expectedLicence(), extractor().licence)
    }

    @Test
    @Throws(Exception::class)
    override fun testLanguageInfo() {
        assertEquals(expectedLanguageInfo(), extractor().languageInfo)
    }

    @Test
    @Throws(Exception::class)
    override fun testTags() {
        assertEqualsOrderIndependent(expectedTags(), extractor().tags)
    }

    @Test
    @Throws(Exception::class)
    override fun testSupportInfo() {
        assertEquals(expectedSupportInfo(), extractor().supportInfo)
    }

    @Test
    @Throws(Exception::class)
    override fun testStreamSegmentsCount() {
        if (expectedStreamSegmentsCount() >= 0) {
            assertEquals(expectedStreamSegmentsCount(), extractor().streamSegments.size)
        }
    }

    /**
     * @see DefaultSearchExtractorTest.testMetaInfo
     */
    @Test
    @Throws(Exception::class)
    override fun testMetaInfo() {
        val metaInfoList = extractor().metaInfo
        val expectedMetaInfoList = expectedMetaInfo()

        val assertions = ArrayList<Executable>()
        for (expectedMetaInfo in expectedMetaInfoList) {
            val texts = metaInfoList.map { metaInfo -> metaInfo.content.content }
            val titles = metaInfoList.map { it.title }
            val urls = metaInfoList.flatMap { info -> info.urls }
            val urlTexts = metaInfoList.flatMap { info -> info.urlTexts }

            assertions.add(Executable { assertContains(expectedMetaInfo.content.content, texts) })
            assertions.add(Executable { assertContains(expectedMetaInfo.title, titles) })

            for (expectedUrlText in expectedMetaInfo.urlTexts) {
                assertions.add(Executable { assertContains(expectedUrlText, urlTexts) })
            }
            for (expectedUrl in expectedMetaInfo.urls) {
                assertions.add(Executable { assertContains(expectedUrl, urls) })
            }
        }
        assertAll(assertions)
    }

    @Test
    @Throws(Exception::class)
    override fun testContentAvailability() {
        assertEquals(expectedContentAvailability(), extractor().contentAvailability)
    }
}

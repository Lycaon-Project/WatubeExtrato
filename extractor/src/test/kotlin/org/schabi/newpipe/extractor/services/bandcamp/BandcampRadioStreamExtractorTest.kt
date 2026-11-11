package org.schabi.newpipe.extractor.services.bandcamp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ExtractorAsserts
import org.schabi.newpipe.extractor.ServiceList.Bandcamp
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.ContentNotSupportedException
import org.schabi.newpipe.extractor.services.DefaultStreamExtractorTest
import org.schabi.newpipe.extractor.services.DefaultTests
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampRadioStreamExtractor
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamType
import java.time.LocalDate
import java.time.Month
import java.time.ZoneOffset

class BandcampRadioStreamExtractorTest : DefaultStreamExtractorTest() {

    private val url = "https://bandcamp.com/?show=230"

    override fun createExtractor(): StreamExtractor = Bandcamp.getStreamExtractor(url)

    @Test
    fun testGettingCorrectStreamExtractor() {
        assertInstanceOf(
            BandcampRadioStreamExtractor::class.java,
            Bandcamp.getStreamExtractor("https://bandcamp.com/?show=3")
        )
        assertFalse(
            Bandcamp.getStreamExtractor("https://zachbenson.bandcamp.com/track/deflated")
                is BandcampRadioStreamExtractor
        )
    }

    override fun expectedName(): String = "Sound Movements"
    override fun expectedId(): String = "230"
    override fun expectedUrlContains(): String = url
    override fun expectedOriginalUrlContains(): String = url
    override fun expectedHasVideoStreams(): Boolean = false
    override fun expectedHasSubtitles(): Boolean = false
    override fun expectedHasFrames(): Boolean = false
    override fun expectedHasRelatedItems(): Boolean = false
    override fun expectedStreamType(): StreamType = StreamType.AUDIO_STREAM
    override fun expectedService(): StreamingService = Bandcamp
    override fun expectedUploaderName(): String = "Andrew Jervis"
    override fun expectedStreamSegmentsCount(): Int = 30

    @Test
    fun testGetUploaderUrl() {
        assertThrows(ContentNotSupportedException::class.java) { extractor().uploaderUrl }
    }

    @Test
    override fun testUploaderUrl() {
        assertThrows(ContentNotSupportedException::class.java) { super.testUploaderUrl() }
    }

    // Never read: testUploaderUrl() expects getUploaderUrl() to throw before comparing.
    override fun expectedUploaderUrl(): String =
        error("Radio streams have no uploader URL")

    override fun expectedDescriptionContains(): List<String> = listOf(
        "Featuring special guests Nick Hakim and Elbows, plus fresh cuts from Eddie Palmieri, " +
            "KRS One, Ladi6, and Moonchild."
    )

    override fun expectedLength(): Long = 5619
    override fun expectedViewCountAtLeast(): Long = -1
    override fun expectedLikeCountAtLeast(): Long = -1
    override fun expectedDislikeCountAtLeast(): Long = -1

    override fun expectedUploadDate(): String? = "16 May 2017 00:00:00 GMT"
    override fun expectedTextualUploadDate(): String? = "16 May 2017 00:00:00 GMT"

    @Test
    override fun testUploadDate() {
        val expectedDate = LocalDate.of(2017, Month.MAY, 16)
        val actualDate = extractor().uploadDate!!.getLocalDateTime(ZoneOffset.UTC).toLocalDate()
        assertEquals(expectedDate, actualDate)
    }

    @Test
    fun testGetThumbnails() {
        BandcampTestUtils.testImages(extractor().thumbnails)
    }

    @Test
    fun testGetUploaderAvatars() {
        DefaultTests.defaultTestImageCollection(extractor().uploaderAvatars)
        extractor().uploaderAvatars.forEach { image ->
            ExtractorAsserts.assertContains("bandcamp-button", image.url)
        }
    }

    @Test
    fun testGetAudioStreams() {
        assertEquals(1, extractor().audioStreams!!.size)
    }
}

package org.schabi.newpipe.extractor.services.youtube.stream

import org.schabi.newpipe.extractor.ExtractorAsserts.assertNotEmpty
import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.services.DefaultStreamExtractorTest
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestImageCollection
import org.schabi.newpipe.extractor.services.youtube.InitYoutubeTest
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamType

class YoutubeStreamExtractorCollaboratorsTest : DefaultStreamExtractorTest(), InitYoutubeTest {

    @Throws(Exception::class)
    override fun createExtractor(): StreamExtractor = YouTube.getStreamExtractor(URL)

    override fun expectedService(): StreamingService = YouTube
    override fun expectedName(): String = "Engineers vs Pumpkin Carving 2.0"
    override fun expectedId(): String = ID
    override fun expectedUrlContains(): String = YoutubeStreamExtractorDefaultTest.BASE_URL + ID
    override fun expectedOriginalUrlContains(): String = URL

    override fun expectedStreamType(): StreamType = StreamType.VIDEO_STREAM
    override fun expectedUploaderName(): String = "CrunchLabs"
    override fun expectedUploaderUrl(): String = "https://www.youtube.com/channel/UC513PdAP2-jWkJunTh5kXRw"
    override fun expectedUploaderSubscriberCountAtLeast(): Long = 227_0000
    override fun expectedUploaderVerified(): Boolean = true
    override fun expectedDescriptionIsEmpty(): Boolean = false
    override fun expectedDescriptionContains(): List<String> = emptyList()
    override fun expectedLength(): Long = 696
    override fun expectedViewCountAtLeast(): Long = 1_400_000
    override fun expectedUploadDate(): String? = "2025-10-25 15:33:05.000"
    override fun expectedTextualUploadDate(): String? = "2025-10-25T08:33:05-07:00"
    override fun expectedLikeCountAtLeast(): Long = 20_000
    override fun expectedDislikeCountAtLeast(): Long = -1
    override fun expectedHasSubtitles(): Boolean = true
    override fun expectedHasFrames(): Boolean = true

    override fun expectedCategory(): String = "Science & Technology"

    override fun expectedLicence(): String = "YouTube licence"
    override fun expectedTags(): List<String> = emptyList()

    @Throws(Exception::class)
    override fun testUploaderAvatars() {
        val avatars = extractor().uploaderAvatars
        assertNotEmpty(avatars)
        defaultTestImageCollection(avatars)
    }

    private companion object {
        const val ID = "3sbYbckT1VY"
        const val URL = YoutubeStreamExtractorDefaultTest.BASE_URL + ID
    }
}

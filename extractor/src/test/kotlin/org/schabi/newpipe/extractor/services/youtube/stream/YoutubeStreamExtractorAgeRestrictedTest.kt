package org.schabi.newpipe.extractor.services.youtube.stream

import org.junit.jupiter.api.Disabled
import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.services.DefaultStreamExtractorTest
import org.schabi.newpipe.extractor.services.youtube.InitYoutubeTest
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamType

@Disabled("There is currently no way to extract age-restricted videos")
class YoutubeStreamExtractorAgeRestrictedTest : DefaultStreamExtractorTest(), InitYoutubeTest {

    @Throws(Exception::class)
    override fun createExtractor(): StreamExtractor = YouTube.getStreamExtractor(URL)

    override fun expectedService(): StreamingService = YouTube
    override fun expectedName(): String = "Russian   Daft Punk"
    override fun expectedId(): String = ID
    override fun expectedUrlContains(): String = YoutubeStreamExtractorDefaultTest.BASE_URL + ID
    override fun expectedOriginalUrlContains(): String = URL

    override fun expectedStreamType(): StreamType = StreamType.VIDEO_STREAM
    override fun expectedUploaderName(): String = "DAN TV"
    override fun expectedUploaderUrl(): String = "https://www.youtube.com/channel/UCcQHIVL83g5BEQe2IJFb-6w"
    override fun expectedUploaderSubscriberCountAtLeast(): Long = 50
    override fun expectedUploaderVerified(): Boolean = false
    override fun expectedDescriptionIsEmpty(): Boolean = true
    override fun expectedDescriptionContains(): List<String> = emptyList()
    override fun expectedLength(): Long = 10
    override fun expectedTimestamp(): Long = TIMESTAMP.toLong()
    override fun expectedViewCountAtLeast(): Long = 232_000
    override fun expectedUploadDate(): String? = "2018-03-11 19:22:08.000"
    override fun expectedTextualUploadDate(): String? = "2018-03-11T12:22:08-07:00"
    override fun expectedLikeCountAtLeast(): Long = 3_700
    override fun expectedDislikeCountAtLeast(): Long = -1
    override fun expectedHasRelatedItems(): Boolean = false // no related videos (!)
    override fun expectedAgeLimit(): Int = 18
    override fun expectedHasSubtitles(): Boolean = false
    override fun expectedHasFrames(): Boolean = false

    override fun expectedCategory(): String = "People & Blogs"

    override fun expectedLicence(): String = "YouTube licence"
    override fun expectedTags(): List<String> = emptyList()

    private companion object {
        const val ID = "rwcfPqbAx-0"
        const val TIMESTAMP = 196
        const val URL = YoutubeStreamExtractorDefaultTest.BASE_URL + ID + "&t=" + TIMESTAMP
    }
}

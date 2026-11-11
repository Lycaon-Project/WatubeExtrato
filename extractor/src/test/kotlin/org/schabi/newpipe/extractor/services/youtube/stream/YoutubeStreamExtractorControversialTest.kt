package org.schabi.newpipe.extractor.services.youtube.stream

import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.services.DefaultStreamExtractorTest
import org.schabi.newpipe.extractor.services.youtube.InitYoutubeTest
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeStreamLinkHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamType

/**
 * Test for [YoutubeStreamLinkHandlerFactory]
 */
class YoutubeStreamExtractorControversialTest : DefaultStreamExtractorTest(), InitYoutubeTest {

    @Throws(Exception::class)
    override fun createExtractor(): StreamExtractor = YouTube.getStreamExtractor(URL)

    override fun expectedService(): StreamingService = YouTube
    override fun expectedName(): String = "Burning Everyone's Koran"
    override fun expectedId(): String = ID
    override fun expectedUrlContains(): String = URL
    override fun expectedOriginalUrlContains(): String = URL

    override fun expectedStreamType(): StreamType = StreamType.VIDEO_STREAM
    override fun expectedUploaderName(): String = "INTO THE FRAY"
    override fun expectedUploaderUrl(): String = "https://www.youtube.com/channel/UCjNxszyFPasDdRoD9J6X-sw"
    override fun expectedUploaderSubscriberCountAtLeast(): Long = 900_000
    override fun expectedDescriptionContains(): List<String> = listOf(
        "http://www.huffingtonpost.com/2010/09/09/obama-gma-interview-quran_n_710282.html",
        "freedom",
    )
    override fun expectedLength(): Long = 219
    override fun expectedViewCountAtLeast(): Long = 285000
    override fun expectedUploadDate(): String? = "2010-09-09 15:40:44.000"
    override fun expectedTextualUploadDate(): String? = "2010-09-09T08:40:44-07:00"
    override fun expectedLikeCountAtLeast(): Long = 13300
    override fun expectedDislikeCountAtLeast(): Long = -1
    override fun expectedTags(): List<String> =
        listOf("Books", "Burning", "Jones", "Koran", "Qur'an", "Terry", "the amazing atheist")
    override fun expectedCategory(): String = "Entertainment"
    override fun expectedLicence(): String = "YouTube licence"

    private companion object {
        const val ID = "T4XJQO3qol8"
        const val URL = YoutubeStreamExtractorDefaultTest.BASE_URL + ID
    }
}

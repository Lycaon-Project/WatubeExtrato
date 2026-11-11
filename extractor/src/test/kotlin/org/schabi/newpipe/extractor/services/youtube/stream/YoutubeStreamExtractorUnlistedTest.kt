package org.schabi.newpipe.extractor.services.youtube.stream

import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.services.DefaultStreamExtractorTest
import org.schabi.newpipe.extractor.services.youtube.InitYoutubeTest
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamType

class YoutubeStreamExtractorUnlistedTest : DefaultStreamExtractorTest(), InitYoutubeTest {

    @Throws(Exception::class)
    override fun createExtractor(): StreamExtractor = YouTube.getStreamExtractor(URL)

    override fun expectedService(): StreamingService = YouTube
    override fun expectedName(): String = "Praise the Casual: Ein Neuling trifft Dark Souls - Folge 5"
    override fun expectedId(): String = ID
    override fun expectedUrlContains(): String = URL
    override fun expectedOriginalUrlContains(): String = URL

    override fun expectedStreamType(): StreamType = StreamType.VIDEO_STREAM
    override fun expectedUploaderName(): String = "Hooked"
    override fun expectedUploaderUrl(): String = "https://www.youtube.com/channel/UCPysfiuOv4VKBeXFFPhKXyw"
    override fun expectedUploaderSubscriberCountAtLeast(): Long = 24_300
    override fun expectedDescriptionContains(): List<String> = listOf(
        "https://www.youtube.com/user/Roccowschiptune",
        "https://www.facebook.com/HookedMagazinDE",
    )
    override fun expectedLength(): Long = 2488
    override fun expectedViewCountAtLeast(): Long = 1500
    override fun expectedUploadDate(): String? = "2017-09-22 12:15:21.000"
    override fun expectedTextualUploadDate(): String? = "2017-09-22T05:15:21-07:00"
    override fun expectedLikeCountAtLeast(): Long = 110
    override fun expectedDislikeCountAtLeast(): Long = -1
    override fun expectedPrivacy(): StreamExtractor.Privacy = StreamExtractor.Privacy.UNLISTED
    override fun expectedLicence(): String = "YouTube licence"
    override fun expectedCategory(): String = "Gaming"
    override fun expectedTags(): List<String> = listOf("dark souls", "hooked", "praise the casual")

    companion object {
        const val ID = "udsB8KnIJTg"
        const val URL = YoutubeStreamExtractorDefaultTest.BASE_URL + ID
    }
}

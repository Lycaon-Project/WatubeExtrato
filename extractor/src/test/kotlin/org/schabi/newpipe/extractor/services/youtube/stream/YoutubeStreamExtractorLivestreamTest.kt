package org.schabi.newpipe.extractor.services.youtube.stream

import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.services.DefaultStreamExtractorTest
import org.schabi.newpipe.extractor.services.youtube.InitYoutubeTest
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamType

class YoutubeStreamExtractorLivestreamTest : DefaultStreamExtractorTest(), InitYoutubeTest {

    @Throws(Exception::class)
    override fun createExtractor(): StreamExtractor = YouTube.getStreamExtractor(URL)

    override fun expectedService(): StreamingService = YouTube
    override fun expectedName(): String = "lofi hip hop radio 📚 beats to relax/study to"
    override fun expectedId(): String = ID
    override fun expectedUrlContains(): String = YoutubeStreamExtractorDefaultTest.BASE_URL + ID
    override fun expectedOriginalUrlContains(): String = URL

    override fun expectedStreamType(): StreamType = StreamType.LIVE_STREAM
    override fun expectedUploaderName(): String = "Lofi Girl"
    override fun expectedUploaderUrl(): String = "https://www.youtube.com/channel/UCSJ4gkVC6NrvII8umztf0Ow"
    override fun expectedUploaderSubscriberCountAtLeast(): Long = 9_800_000
    override fun expectedDescriptionContains(): List<String> = listOf(
        "Lofi Girl merch",
        "Thank you for listening, I hope you will have a good time here",
    )
    override fun expectedUploaderVerified(): Boolean = true
    override fun expectedLength(): Long = 0
    override fun expectedTimestamp(): Long = TIMESTAMP.toLong()
    override fun expectedViewCountAtLeast(): Long = 0
    override fun expectedUploadDate(): String? = "2026-08-19 07:31:51.000"
    override fun expectedTextualUploadDate(): String? = "2026-08-19T00:31:51-07:00"
    override fun expectedLikeCountAtLeast(): Long = 5_000
    override fun expectedDislikeCountAtLeast(): Long = -1
    override fun expectedHasSubtitles(): Boolean = false
    override fun expectedDashMpdUrlContains(): String? = "https://manifest.googlevideo.com/api/manifest/dash/"
    override fun expectedHasFrames(): Boolean = false
    override fun expectedLicence(): String = "YouTube licence"
    override fun expectedCategory(): String = "Music"
    override fun expectedTags(): List<String> = listOf(
        "beats to relax", "chilled cow", "chilled cow radio", "chilledcow", "chillhop",
        "focus music", "hip hop", "hiphop", "lo fi", "lo fi hip hop", "lo fi hip hop radio",
        "lo fi hiphop", "lo fi radio", "lo-fi", "lo-fi hip hop radio", "lofi chill",
        "lofi girl", "lofi girl radio", "lo-fi hip hop", "lo-fi hiphop",
        "lo-fi radio", "lofi", "lofi hip hop", "lofi hip hop radio", "lofi hiphop", "lofi radio", "music",
        "lofi study", "music to relax", "music to study", "playlist", "radio",
        "relax music", "relaxing music", "sleep music", "study lofi", "study music",
    )

    private companion object {
        const val ID = "rFZHOHl-L8A"
        const val TIMESTAMP = 1737
        const val URL = YoutubeStreamExtractorDefaultTest.BASE_URL + ID + "&t=" + TIMESTAMP
    }
}

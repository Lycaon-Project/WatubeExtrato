package org.schabi.newpipe.extractor.services.youtube.stream

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ExtractorAsserts.assertContains
import org.schabi.newpipe.extractor.ExtractorAsserts.assertGreaterOrEqual
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.playlist.PlaylistInfo.PlaylistType
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.services.DefaultStreamExtractorTest
import org.schabi.newpipe.extractor.services.youtube.InitYoutubeTest
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import org.schabi.newpipe.extractor.services.youtube.YoutubeTestsUtils
import org.schabi.newpipe.extractor.services.youtube.stream.YoutubeStreamExtractorDefaultTest.Companion.YOUTUBE_LICENCE
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamType

class YoutubeStreamExtractorRelatedMixTest : DefaultStreamExtractorTest(), InitYoutubeTest {

    @BeforeAll
    @Throws(Exception::class)
    override fun setUp() {
        super<InitYoutubeTest>.setUp()
        YoutubeParsingHelper.setConsentAccepted(true)
    }

    @Throws(Exception::class)
    override fun createExtractor(): StreamExtractor = YouTube.getStreamExtractor(URL)

    override fun expectedService(): StreamingService = YouTube
    override fun expectedName(): String = TITLE
    override fun expectedId(): String = ID
    override fun expectedUrlContains(): String = URL
    override fun expectedOriginalUrlContains(): String = URL

    override fun expectedStreamType(): StreamType = StreamType.VIDEO_STREAM
    override fun expectedUploaderName(): String = "NoCopyrightSounds"
    override fun expectedUploaderUrl(): String = "https://www.youtube.com/channel/UC_aEa8K-EOJ3D6gOs7HcyNg"
    override fun expectedDescriptionContains(): List<String> =
        listOf("https://www.youtube.com/user/danielleviband/", "©")
    override fun expectedUploaderVerified(): Boolean = true
    override fun expectedUploaderSubscriberCountAtLeast(): Long = 32_000_000
    override fun expectedLength(): Long = 208
    override fun expectedViewCountAtLeast(): Long = 449_000_000
    override fun expectedUploadDate(): String? = "2015-07-09 16:34:35.000"
    override fun expectedTextualUploadDate(): String? = "2015-07-09T09:34:35-07:00"
    override fun expectedLikeCountAtLeast(): Long = 6_400_000
    override fun expectedDislikeCountAtLeast(): Long = -1
    override fun expectedStreamSegmentsCount(): Int = 0
    override fun expectedLicence(): String = YOUTUBE_LICENCE
    override fun expectedCategory(): String = "Music"
    override fun expectedTags(): List<String> = listOf(
        "Cartoon", "Cartoon - On & On", "Cartoon On & On (feat. Daniel Levi)",
        "Daniel Levi", "NCS Best Songs", "NCS Cartoon Daniel Levi", "NCS Cartoon On & On",
        "NCS On & On", "NCS On and On", "NCS Release Daniel Levi", "NCS release Cartoon",
        "On & On", "best music", "club music", "copyright free music", "dance music", "edm",
        "electronic music", "electronic pop", "free music", "gaming music", "music", "ncs",
        "no copyright music", "no copyright sounds", "nocopyrightsounds",
        "royalty free music", "songs", "top music",
    )

    @Test
    @Throws(Exception::class)
    override fun testRelatedItems() {
        super.testRelatedItems()

        val playlists = requireNotNull(extractor().relatedItems)
            .items
            .filterIsInstance<PlaylistInfoItem>()

        // There can also be other playlists that are not mixes!
        val streamMixes = playlists.filter { it.playlistType == PlaylistType.MIX_STREAM }
        assertGreaterOrEqual(1, streamMixes.size.toLong(), "Not found one or more stream mix in related items")

        val streamMix = streamMixes[0]
        assertSame(InfoItem.InfoType.PLAYLIST, streamMix.infoType)
        assertEquals(YouTube.serviceId, streamMix.serviceId)
        assertContains(URL, streamMix.url)
        assertContains("list=RD$ID", streamMix.url)
        assertEquals("Mix – $TITLE", streamMix.name)
        YoutubeTestsUtils.testImages(streamMix.thumbnails)
    }

    private companion object {
        const val ID = "K4DyBUG242c"
        const val URL = YoutubeStreamExtractorDefaultTest.BASE_URL + ID
        const val TITLE =
            "Cartoon, Jéja - On & On (feat. Daniel Levi) | Electronic Pop | NCS - Copyright Free Music"
    }
}

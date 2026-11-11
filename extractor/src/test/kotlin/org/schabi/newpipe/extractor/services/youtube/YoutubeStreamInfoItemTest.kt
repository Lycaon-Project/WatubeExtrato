package org.schabi.newpipe.extractor.services.youtube

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.downloader.DownloaderFactory.getMockPath
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonParser
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.localization.TimeAgoParser
import org.schabi.newpipe.extractor.localization.TimeAgoPatternsManager
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamInfoItemExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamInfoItemLockupExtractor
import org.schabi.newpipe.extractor.stream.ContentAvailability
import org.schabi.newpipe.extractor.stream.StreamType
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.TimeZone

internal class YoutubeStreamInfoItemTest {

    private fun loadJson(mockName: String): JsonObject =
        FileInputStream(getMockPath(YoutubeStreamInfoItemTest::class.java, mockName) + ".json").use {
            JsonParser.`object`().from(it)
        }

    private fun timeAgoParser(): TimeAgoParser? = TimeAgoPatternsManager.getTimeAgoParserFor(Localization.DEFAULT)

    /** A lockup extractor which behaves as on a channel page (1-row format). */
    private fun channelLockupExtractor(json: JsonObject) =
        object : YoutubeStreamInfoItemLockupExtractor(json, timeAgoParser()) {
            override val isChannelOrCoursePlaylistLockupItem: Boolean
                get() = true
        }

    @Test
    fun videoRendererPremiere() {
        val json = loadJson("videoRendererPremiere")
        val extractor = YoutubeStreamInfoItemExtractor(json, timeAgoParser())
        assertAll(
            { assertEquals(StreamType.VIDEO_STREAM, extractor.streamType) },
            { assertFalse(extractor.isAd) },
            { assertEquals("https://www.youtube.com/watch?v=M_8QNw_JM4I", extractor.url) },
            { assertEquals("This video will premiere in 6 months.", extractor.name) },
            { assertEquals(33, extractor.duration) },
            { assertEquals("Blunt Brothers Productions", extractor.uploaderName) },
            { assertEquals("https://www.youtube.com/channel/UCUPrbbdnot-aPgNM65svgOg", extractor.uploaderUrl) },
            { assertFalse(extractor.uploaderAvatars.isEmpty()) },
            { assertTrue(extractor.isUploaderVerified) },
            {
                TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
                assertEquals("2026-03-15 13:12", extractor.textualUploadDate)
            },
            {
                val uploadDate = extractor.uploadDate
                assertNotNull(uploadDate)
                val expected = LocalDateTime.of(2026, 3, 15, 13, 12).atOffset(ZoneOffset.UTC)
                assertEquals(expected, uploadDate!!.offsetDateTime())
            },
            { assertEquals(-1, extractor.viewCount) },
            { assertFalse(extractor.thumbnails.isEmpty()) },
            {
                assertEquals(
                    "Patience is key… MERCH SHOP    : https://www.bluntbrosproductions.com Follow us on Instagram for early updates:\u00a0...",
                    extractor.shortDescription
                )
            },
            { assertFalse(extractor.isShortFormContent) },
            { assertEquals(ContentAvailability.UPCOMING, extractor.contentAvailability) }
        )
    }

    @Test
    fun lockupViewModelPremiere() {
        val json = loadJson("lockupViewModelPremiere")
        val extractor = YoutubeStreamInfoItemLockupExtractor(json, timeAgoParser())
        assertAll(
            { assertEquals(StreamType.VIDEO_STREAM, extractor.streamType) },
            { assertFalse(extractor.isAd) },
            { assertEquals("https://www.youtube.com/watch?v=VIDEO_ID", extractor.url) },
            { assertEquals("VIDEO_TITLE", extractor.name) },
            { assertEquals(-1, extractor.duration) },
            { assertEquals("VIDEO_CHANNEL_NAME", extractor.uploaderName) },
            { assertEquals("https://www.youtube.com/channel/UCD_on7-zu7Zuc3zissQvrgw", extractor.uploaderUrl) },
            { assertFalse(extractor.uploaderAvatars.isEmpty()) },
            { assertFalse(extractor.isUploaderVerified) },
            { assertEquals("14/08/2025, 13:00", extractor.textualUploadDate) },
            {
                val uploadDate = extractor.uploadDate
                assertNotNull(uploadDate)
                assertEquals(
                    OffsetDateTime.of(2025, 8, 14, 13, 0, 0, 0, ZoneOffset.UTC),
                    uploadDate!!.offsetDateTime()
                )
            },
            { assertEquals(-1, extractor.viewCount) },
            { assertFalse(extractor.thumbnails.isEmpty()) },
            { assertNull(extractor.shortDescription) },
            { assertFalse(extractor.isShortFormContent) },
            { assertEquals(ContentAvailability.UPCOMING, extractor.contentAvailability) }
        )
    }

    @Test
    fun lockupViewModelVideo() {
        val json = loadJson("lockupViewModelVideo")
        val extractor = YoutubeStreamInfoItemLockupExtractor(json, timeAgoParser())
        assertAll(
            { assertEquals(StreamType.VIDEO_STREAM, extractor.streamType) },
            { assertFalse(extractor.isAd) },
            { assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", extractor.url) },
            { assertEquals("VIDEO_TITLE", extractor.name) },
            { assertEquals(974, extractor.duration) },
            { assertFalse(extractor.thumbnails.isEmpty()) },
            { assertEquals(ContentAvailability.AVAILABLE, extractor.contentAvailability) }
        )
    }

    @Test
    fun lockupViewModelLiveStreamNoViewer() {
        val json = loadJson("lockupViewModelLiveStreamNoViewer")
        val extractor = YoutubeStreamInfoItemLockupExtractor(json, timeAgoParser())
        assertAll(
            { assertEquals(StreamType.LIVE_STREAM, extractor.streamType) },
            { assertFalse(extractor.isAd) },
            { assertEquals("https://www.youtube.com/watch?v=LIVE_VIDEO_ID", extractor.url) },
            { assertEquals("LIVE_VIDEO_TITLE", extractor.name) },
            { assertEquals(-1, extractor.duration) },
            { assertNull(extractor.textualUploadDate) },
            { assertNull(extractor.uploadDate) },
            { assertEquals(0, extractor.viewCount) },
            { assertFalse(extractor.thumbnails.isEmpty()) },
            { assertEquals(ContentAvailability.AVAILABLE, extractor.contentAvailability) }
        )
    }

    @Test
    fun lockupViewModelNoDuration() {
        val json = loadJson("lockupViewModelNoDuration")
        val extractor = YoutubeStreamInfoItemLockupExtractor(json, timeAgoParser())
        assertAll(
            { assertEquals(StreamType.VIDEO_STREAM, extractor.streamType) },
            { assertFalse(extractor.isAd) },
            { assertEquals(-1, extractor.duration) },
            { assertFalse(extractor.thumbnails.isEmpty()) },
            { assertEquals(ContentAvailability.AVAILABLE, extractor.contentAvailability) }
        )
    }

    /**
     * Tests that the date and view count is extracted properly from the 1-row channel format.
     */
    @Test
    fun lockupViewModelChannelOneRow() {
        val extractor = channelLockupExtractor(loadJson("lockupViewModelChannelOneRow"))
        assertAll(
            { assertEquals(StreamType.VIDEO_STREAM, extractor.streamType) },
            { assertEquals("Test Video One Row Normal", extractor.name) },
            { assertEquals("2 hours ago", extractor.textualUploadDate) },
            { assertNotNull(extractor.uploadDate) },
            { assertEquals(3600000, extractor.viewCount) }, // 3.6m views
            { assertEquals(630, extractor.duration) }, // 10:30
            { assertEquals(ContentAvailability.AVAILABLE, extractor.contentAvailability) }
        )
    }

    /**
     * Tests that the info row search handles 1-row format with only view count (no date text
     * present), returned for livestreams on channels.
     */
    @Test
    fun lockupViewModelChannelOneRowLiveViewsOnly() {
        val extractor = channelLockupExtractor(loadJson("lockupViewModelChannelOneRowLiveViewsOnly"))
        assertAll(
            { assertEquals(StreamType.LIVE_STREAM, extractor.streamType) },
            { assertEquals("Test Video One Row Views Only", extractor.name) },
            { assertNull(extractor.textualUploadDate) },
            { assertNull(extractor.uploadDate) },
            { assertEquals(500, extractor.viewCount) }, // 500 watching
            { assertEquals(-1, extractor.duration) },
            { assertEquals(ContentAvailability.AVAILABLE, extractor.contentAvailability) }
        )
    }

    @Test
    fun lockupViewModelChannelMembersOnly() {
        val extractor = channelLockupExtractor(loadJson("lockupviewmodelchannelmembersonly"))
        assertAll(
            { assertEquals(StreamType.VIDEO_STREAM, extractor.streamType) },
            { assertFalse(extractor.isAd) },
            { assertEquals("https://www.youtube.com/watch?v=x3pS1_qqtIs", extractor.url) },
            {
                assertEquals(
                    "Daily Linux News - S03E105 - EU will fund European datacenters, Steam Deck's massive price increase",
                    extractor.name
                )
            },
            { assertEquals(725, extractor.duration) },
            { assertEquals("12 hours ago", extractor.textualUploadDate) },
            { assertNotNull(extractor.uploadDate) },
            { assertEquals(-1, extractor.viewCount) },
            { assertFalse(extractor.thumbnails.isEmpty()) },
            { assertNull(extractor.shortDescription) },
            { assertFalse(extractor.isShortFormContent) },
            { assertEquals(ContentAvailability.MEMBERSHIP, extractor.contentAvailability) }
        )
    }

    @Test
    fun emptyTitle() {
        val json = loadJson("emptyTitle")
        val extractor = YoutubeStreamInfoItemExtractor(json, timeAgoParser())
        assertAll(
            { assertEquals(StreamType.VIDEO_STREAM, extractor.streamType) },
            { assertFalse(extractor.isAd) },
            { assertEquals("https://www.youtube.com/watch?v=nc1kN8ZSfGQ", extractor.url) },
            { assertEquals("", extractor.name) },
            { assertEquals(39, extractor.duration) },
            { assertEquals("hyper", extractor.uploaderName) },
            { assertEquals("https://www.youtube.com/channel/UCSezUnbvCLYBXuUlPcXU_QQ", extractor.uploaderUrl) },
            { assertFalse(extractor.uploaderAvatars.isEmpty()) },
            { assertTrue(extractor.isUploaderVerified) },
            { assertEquals("8 years ago", extractor.textualUploadDate) },
            { assertNotNull(extractor.uploadDate) },
            { assertTrue(extractor.viewCount >= 1318193) },
            { assertFalse(extractor.thumbnails.isEmpty()) },
            { assertNull(extractor.shortDescription) },
            { assertFalse(extractor.isShortFormContent) },
            { assertEquals(ContentAvailability.AVAILABLE, extractor.contentAvailability) }
        )
    }
}

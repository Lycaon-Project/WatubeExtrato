// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

package org.schabi.newpipe.extractor.services.bandcamp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ServiceList.Bandcamp
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.services.DefaultStreamExtractorTest
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamType

/**
 * Tests for [org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampStreamExtractor]
 */
class BandcampStreamExtractorTest : DefaultStreamExtractorTest() {

    override fun createExtractor(): StreamExtractor =
        Bandcamp.getStreamExtractor("https://teaganbear.bandcamp.com/track/just-for-the-halibut")

    override fun expectedService(): StreamingService = Bandcamp

    override fun expectedName(): String = "Just for the Halibut"

    override fun expectedId(): String = "https://teaganbear.bandcamp.com/track/just-for-the-halibut"

    override fun expectedUrlContains(): String = "https://teaganbear.bandcamp.com/track/just-for-the-halibut"

    override fun expectedOriginalUrlContains(): String =
        "https://teaganbear.bandcamp.com/track/just-for-the-halibut"

    override fun expectedStreamType(): StreamType = StreamType.AUDIO_STREAM

    override fun expectedUploaderName(): String = "Teaganbear"

    override fun expectedUploaderUrl(): String = "https://teaganbear.bandcamp.com/"

    override fun expectedDescriptionContains(): List<String> =
        listOf("it's Creative Commons so feel free to use it in whatever")

    override fun expectedLength(): Long = 124

    override fun expectedViewCountAtLeast(): Long = Long.MIN_VALUE

    override fun expectedUploadDate(): String? = "2019-03-10 23:00:42.000"

    override fun expectedTextualUploadDate(): String? = "10 Mar 2019 23:00:42 GMT"

    override fun expectedLikeCountAtLeast(): Long = Long.MIN_VALUE

    override fun expectedDislikeCountAtLeast(): Long = Long.MIN_VALUE

    override fun expectedHasVideoStreams(): Boolean = false

    override fun expectedHasRelatedItems(): Boolean = true

    override fun expectedHasSubtitles(): Boolean = false

    override fun expectedHasFrames(): Boolean = false

    override fun expectedLicence(): String = "CC BY 3.0"

    override fun expectedCategory(): String = "dance"

    @Test
    fun testArtistProfilePictures() {
        BandcampTestUtils.testImages(extractor().uploaderAvatars)
    }

    @Test
    fun testTranslateIdsToUrl() {
        // To add tests: look at website's source, search for `band_id` and `item_id`
        assertEquals(
            "https://teaganbear.bandcamp.com/track/just-for-the-halibut",
            BandcampExtractorHelper.getStreamUrlFromIds(3877364987L, 3486455278L, "track")
        )
    }
}

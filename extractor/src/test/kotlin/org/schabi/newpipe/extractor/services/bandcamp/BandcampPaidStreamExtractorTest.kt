package org.schabi.newpipe.extractor.services.bandcamp

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.InitNewPipeTest
import org.schabi.newpipe.extractor.ServiceList.Bandcamp
import org.schabi.newpipe.extractor.exceptions.PaidContentException

class BandcampPaidStreamExtractorTest : InitNewPipeTest {

    @Test
    fun testPaidTrack() {
        val extractor = Bandcamp.getStreamExtractor(
            "https://radicaldreamland.bandcamp.com/track/hackmud-continuous-mix"
        )
        assertThrows(PaidContentException::class.java) { extractor.fetchPage() }
    }
}

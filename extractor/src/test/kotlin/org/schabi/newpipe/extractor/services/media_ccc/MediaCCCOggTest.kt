package org.schabi.newpipe.extractor.services.media_ccc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ServiceList.MediaCCC
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest
import org.schabi.newpipe.extractor.services.media_ccc.extractors.MediaCCCStreamExtractor
import org.schabi.newpipe.extractor.stream.StreamExtractor

/**
 * Test [MediaCCCStreamExtractor]
 */
class MediaCCCOggTest : DefaultSimpleExtractorTest<StreamExtractor>() {

    override fun createExtractor(): StreamExtractor =
        MediaCCC.getStreamExtractor("https://media.ccc.de/public/events/1317")

    @Test
    fun getAudioStreamsCount() {
        assertEquals(1, checkNotNull(extractor().audioStreams).size)
    }

    @Test
    fun getAudioStreamsContainOgg() {
        for (stream in checkNotNull(extractor().audioStreams)) {
            assertEquals("OGG", stream.format.toString())
        }
    }
}

package org.schabi.newpipe.extractor.services.soundcloud

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ServiceList.SoundCloud
import org.schabi.newpipe.extractor.services.DefaultSimpleUntypedExtractorTest
import org.schabi.newpipe.extractor.suggestion.SuggestionExtractor

/**
 * Test for [SuggestionExtractor]
 */
class SoundcloudSuggestionExtractorTest : DefaultSimpleUntypedExtractorTest<SuggestionExtractor>() {

    override fun createExtractor(): SuggestionExtractor = SoundCloud.suggestionExtractor

    @Test
    fun testIfSuggestions() {
        assertFalse(extractor().suggestionList("lil uzi vert").isEmpty())
    }
}

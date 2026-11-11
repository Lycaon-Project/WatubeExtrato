// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

package org.schabi.newpipe.extractor.services.bandcamp

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ServiceList.Bandcamp
import org.schabi.newpipe.extractor.services.DefaultSimpleUntypedExtractorTest
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampSuggestionExtractor

/**
 * Tests for [BandcampSuggestionExtractor]
 */
class BandcampSuggestionExtractorTest : DefaultSimpleUntypedExtractorTest<BandcampSuggestionExtractor>() {

    override fun createExtractor(): BandcampSuggestionExtractor =
        Bandcamp.suggestionExtractor as BandcampSuggestionExtractor

    @Test
    fun testSearchExample() {
        val c418 = extractor().suggestionList("c418")

        assertTrue(c418.contains("C418"))

        // There should be five results, but we can't be sure of that forever
        assertTrue(c418.size > 2)
    }
}

package org.schabi.newpipe.extractor.services

import org.junit.jupiter.api.Test

interface BaseSearchExtractorTest : BaseListExtractorTest {
    @Test
    @Throws(Exception::class)
    fun testSearchString()
    @Test
    @Throws(Exception::class)
    fun testSearchSuggestion()
    @Test
    @Throws(Exception::class)
    fun testSearchCorrected()
    @Test
    @Throws(Exception::class)
    fun testMetaInfo()
}

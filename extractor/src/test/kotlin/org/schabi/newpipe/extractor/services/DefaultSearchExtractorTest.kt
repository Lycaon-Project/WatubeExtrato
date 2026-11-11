package org.schabi.newpipe.extractor.services

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ExtractorAsserts.assertEmpty
import org.schabi.newpipe.extractor.MetaInfo
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.utils.Utils.isNullOrEmpty
import java.net.MalformedURLException

abstract class DefaultSearchExtractorTest :
    DefaultListExtractorTest<SearchExtractor>(),
    BaseSearchExtractorTest {

    abstract fun expectedSearchString(): String

    abstract fun expectedSearchSuggestion(): String?

    open fun isCorrectedSearch(): Boolean = false

    @Throws(MalformedURLException::class)
    open fun expectedMetaInfo(): List<MetaInfo> = emptyList()

    @Test
    @Throws(Exception::class)
    override fun testSearchString() {
        assertEquals(expectedSearchString(), extractor().searchString)
    }

    @Test
    @Throws(Exception::class)
    override fun testSearchSuggestion() {
        val expectedSearchSuggestion = expectedSearchSuggestion()
        if (isNullOrEmpty(expectedSearchSuggestion)) {
            assertEmpty("Suggestion was expected to be empty", extractor().searchSuggestion)
        } else {
            assertEquals(expectedSearchSuggestion, extractor().searchSuggestion)
        }
    }

    @Test
    @Throws(Exception::class)
    override fun testSearchCorrected() {
        assertEquals(isCorrectedSearch(), extractor().isCorrectedSearch)
    }

    /**
     * @see DefaultStreamExtractorTest.testMetaInfo
     */
    @Test
    @Throws(Exception::class)
    override fun testMetaInfo() {
        val metaInfoList = extractor().metaInfo
        val expectedMetaInfoList = expectedMetaInfo()

        for (expectedMetaInfo in expectedMetaInfoList) {
            val texts = metaInfoList.map { it.content.content }
            val titles = metaInfoList.map { it.title }
            val urls = metaInfoList.flatMap { it.urls }
            val urlTexts = metaInfoList.flatMap { it.urlTexts }

            assertTrue(texts.contains(expectedMetaInfo.content.content))
            assertTrue(titles.contains(expectedMetaInfo.title))

            for (expectedUrlText in expectedMetaInfo.urlTexts) {
                assertTrue(urlTexts.contains(expectedUrlText))
            }
            for (expectedUrl in expectedMetaInfo.urls) {
                assertTrue(urls.contains(expectedUrl))
            }
        }
    }
}

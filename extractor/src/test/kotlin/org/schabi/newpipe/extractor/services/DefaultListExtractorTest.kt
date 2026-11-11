package org.schabi.newpipe.extractor.services

import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.services.DefaultTests.assertNoMoreItems
import org.schabi.newpipe.extractor.services.DefaultTests.assertOnlyContainsType
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestMoreItems
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestRelatedItems

abstract class DefaultListExtractorTest<T : ListExtractor<out InfoItem>> :
    DefaultExtractorTest<T>(),
    BaseListExtractorTest {

    open fun expectedInfoItemType(): InfoItem.InfoType? = null

    open fun expectedHasMoreItems(): Boolean = true

    @Test
    @Throws(Exception::class)
    override fun testRelatedItems() {
        val extractor: ListExtractor<out InfoItem> = extractor()

        val expectedType = expectedInfoItemType()
        val items = defaultTestRelatedItems(extractor)
        if (expectedType != null) {
            assertOnlyContainsType(items, expectedType)
        }
    }

    @Test
    @Throws(Exception::class)
    override fun testMoreRelatedItems() {
        val extractor: ListExtractor<out InfoItem> = extractor()

        if (expectedHasMoreItems()) {
            val expectedType = expectedInfoItemType()
            val items = defaultTestMoreItems(extractor)
            if (expectedType != null) {
                assertOnlyContainsType(items, expectedType)
            }
        } else {
            assertNoMoreItems(extractor)
        }
    }
}

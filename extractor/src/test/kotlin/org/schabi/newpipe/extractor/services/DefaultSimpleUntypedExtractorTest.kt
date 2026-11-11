package org.schabi.newpipe.extractor.services

import org.schabi.newpipe.extractor.InitNewPipeTest

/**
 * Represents a common abstraction of a test class where an extractor is set up and various
 * tests for it are executed.
 *
 * Please see [DefaultSimpleExtractorTest] for an implementation that is exactly typed to
 * [org.schabi.newpipe.extractor.Extractor]
 */
abstract class DefaultSimpleUntypedExtractorTest<T> : InitNewPipeTest {

    private var extractor: T? = null

    /**
     * This should create an extractor, save it to variable
     * [DefaultSimpleUntypedExtractorTest.extractor],
     * and then set up the extractor by making it fetch network resources if applicable
     */
    @Throws(Exception::class)
    protected open fun initExtractor() {
        val created = createExtractor()
        extractor = created
        fetchExtractor(created)
    }

    @Throws(Exception::class)
    protected abstract fun createExtractor(): T

    @Throws(Exception::class)
    protected open fun fetchExtractor(extractor: T) {
    }

    protected open fun extractor(): T {
        if (extractor == null) {
            try {
                initExtractor()
            } catch (ex: Exception) {
                if (ex is RuntimeException) {
                    throw ex
                }
                throw RuntimeException("Failed to init extractor", ex)
            }
        }
        @Suppress("UNCHECKED_CAST")
        return extractor as T
    }
}

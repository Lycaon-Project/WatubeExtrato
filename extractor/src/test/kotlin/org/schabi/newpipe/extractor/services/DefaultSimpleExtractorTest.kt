package org.schabi.newpipe.extractor.services

import org.schabi.newpipe.extractor.Extractor

abstract class DefaultSimpleExtractorTest<T : Extractor> : DefaultSimpleUntypedExtractorTest<T>() {

    @Throws(Exception::class)
    override fun fetchExtractor(extractor: T) {
        extractor.fetchPage()
    }
}

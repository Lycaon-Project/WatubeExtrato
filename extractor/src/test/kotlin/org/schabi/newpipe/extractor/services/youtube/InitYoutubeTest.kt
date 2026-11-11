package org.schabi.newpipe.extractor.services.youtube

import org.junit.jupiter.api.BeforeAll
import org.schabi.newpipe.extractor.InitNewPipeTest

interface InitYoutubeTest : InitNewPipeTest {
    @BeforeAll
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        YoutubeTestsUtils.ensureStateless()
    }
}

// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

package org.schabi.newpipe.extractor.services.bandcamp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.InitNewPipeTest
import org.schabi.newpipe.extractor.ServiceList.Bandcamp
import org.schabi.newpipe.extractor.services.bandcamp.linkHandler.BandcampSearchQueryHandlerFactory

class BandcampSearchQueryHandlerFactoryTest {

    companion object {
        private lateinit var searchQuery: BandcampSearchQueryHandlerFactory

        @JvmStatic
        @BeforeAll
        fun setUp() {
            InitNewPipeTest.initEmpty()
            searchQuery = Bandcamp.searchQHFactory as BandcampSearchQueryHandlerFactory
        }
    }

    @Test
    fun testEncoding() {
        // Note: this isn't exactly as bandcamp does it (it wouldn't encode '!'), but both works
        assertEquals(
            "https://bandcamp.com/search?q=hello%21%22%C2%A7%24%25%26%2F%28%29%3D&page=1",
            searchQuery.getUrl("hello!\"§$%&/()=")
        )
        // Note: bandcamp uses %20 instead of '+', but both works
        assertEquals(
            "https://bandcamp.com/search?q=search+query+with+spaces&page=1",
            searchQuery.getUrl("search query with spaces")
        )
    }
}

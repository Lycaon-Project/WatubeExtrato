/*
 * Created by Christian Schabesberger on 18.11.16.
 *
 * Copyright (C) 2016 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * YoutubeSuggestionExtractorTest.java is part of NewPipe Extractor.
 *
 * NewPipe Extractor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe Extractor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe Extractor.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.schabi.newpipe.extractor.services.youtube

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.schabi.newpipe.downloader.DownloaderFactory
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeSuggestionExtractor
import org.schabi.newpipe.extractor.suggestion.SuggestionExtractor

/**
 * Test for [YoutubeSuggestionExtractor]
 */
internal class YoutubeSuggestionExtractorTest {

    @Test
    fun testIfSuggestions() {
        assertFalse(suggestionExtractor.suggestionList("hello").isEmpty())
    }

    companion object {
        private lateinit var suggestionExtractor: SuggestionExtractor

        @JvmStatic
        @BeforeAll
        fun setUp() {
            YoutubeTestsUtils.ensureStateless()
            NewPipe.init(
                DownloaderFactory.getDownloader(YoutubeSuggestionExtractorTest::class.java),
                Localization("de", "DE")
            )
            suggestionExtractor = YouTube.suggestionExtractor
        }
    }
}

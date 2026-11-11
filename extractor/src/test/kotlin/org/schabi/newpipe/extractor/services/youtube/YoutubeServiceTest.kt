package org.schabi.newpipe.extractor.services.youtube

/*
 * Created by Christian Schabesberger on 29.12.15.
 *
 * Copyright (C) 2015 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * YoutubeSearchExtractorStreamTest.java is part of NewPipe Extractor.
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
 * along with NewPipe Extractor.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.InitNewPipeTest
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.kiosk.KioskList
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeMixPlaylistExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubePlaylistExtractor

/**
 * Test for [YoutubeService]
 */
class YoutubeServiceTest {

    @Test
    fun testGetKioskAvailableKiosks() {
        assertFalse(kioskList.availableKiosks.isEmpty(), "No kiosk got returned")
    }

    @Test
    fun testGetDefaultKiosk() {
        val kiosk = kioskList.getDefaultKioskExtractor(null)
        assertNotNull(kiosk)
        assertEquals("live", kiosk!!.id)
    }

    @Test
    fun getPlayListExtractorIsNormalPlaylist() {
        val extractor = service.getPlaylistExtractor(
            "https://www.youtube.com/watch?v=JhqtYOnNrTs&list=PL-EkZZikQIQVqk9rBWzEo5b-2GeozElS"
        )
        assertInstanceOf(YoutubePlaylistExtractor::class.java, extractor)
    }

    @Test
    fun getPlaylistExtractorIsMix() {
        val videoId = "_AzeUSL9lZc"
        var extractor = YouTube.getPlaylistExtractor(
            "https://www.youtube.com/watch?v=$videoId&list=RD$videoId"
        )
        assertInstanceOf(YoutubeMixPlaylistExtractor::class.java, extractor)

        extractor = YouTube.getPlaylistExtractor(
            "https://www.youtube.com/watch?v=$videoId&list=RDMM$videoId"
        )
        assertInstanceOf(YoutubeMixPlaylistExtractor::class.java, extractor)

        val mixVideoId = "qHtzO49SDmk"

        extractor = YouTube.getPlaylistExtractor(
            "https://www.youtube.com/watch?v=$mixVideoId&list=RD$videoId"
        )
        assertInstanceOf(YoutubeMixPlaylistExtractor::class.java, extractor)
    }

    companion object {
        private lateinit var service: StreamingService
        private lateinit var kioskList: KioskList

        @JvmStatic
        @BeforeAll
        fun setUp() {
            InitNewPipeTest.initEmpty()
            // Init with dummy as
            // * a downloader is required otherwise a NPE is thrown during extract initialization
            // * nothing will be transmitted
            NewPipe.init(object : Downloader() {
                override fun execute(request: Request): Response {
                    throw UnsupportedOperationException("No communication expected")
                }
            })
            service = YouTube
            kioskList = service.kioskList
        }
    }
}

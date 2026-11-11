package org.schabi.newpipe.extractor.services.youtube

/*
 * Created by Christian Schabesberger on 12.08.17.
 *
 * Copyright (C) 2017 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * YoutubeTrendingLinkHandlerFactoryTest.java is part of NewPipe Extractor.
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.schabi.newpipe.extractor.InitNewPipeTest
import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeTrendingLinkHandlerFactory

/**
 * Test for [YoutubeTrendingLinkHandlerFactory]
 */
class YoutubeTrendingLinkHandlerFactoryTest {

    @Test
    fun getUrl() {
        assertEquals("https://www.youtube.com/feed/trending", linkHandlerFactory.fromId("").url)
    }

    @Test
    fun getId() {
        assertEquals("Trending", linkHandlerFactory.fromUrl("https://www.youtube.com/feed/trending").id)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "https://www.youtube.com/feed/trending",
            "https://www.youtube.com/feed/trending?adsf=fjaj#fhe",
            "http://www.youtube.com/feed/trending",
            "www.youtube.com/feed/trending",
            "youtube.com/feed/trending",
            "youtube.com/feed/trending?akdsakjf=dfije&kfj=dkjak",
            "https://youtube.com/feed/trending",
            "m.youtube.com/feed/trending",
            "https://invidious.nerdvpn.de/feed/trending",
            "https://redirect.invidious.io/feed/trending",
            "invidious.nerdvpn.de/feed/trending"
        ]
    )
    fun shouldAcceptUrl(url: String) {
        assertTrue(linkHandlerFactory.acceptUrl(url))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "https://youtu.be/feed/trending",
            "kdskjfiiejfia",
            "https://www.youtube.com/bullshit/feed/trending",
            "https://www.youtube.com/feed/trending/bullshit",
            "https://www.youtube.com/feed/bullshit/trending",
            "peter klaut aepferl youtube.com/feed/trending",
            "youtube.com/feed/trending askjkf",
            "askdjfi youtube.com/feed/trending askjkf",
            "    youtube.com/feed/trending",
            "https://www.youtube.com/feed/trending.html",
            ""
        ]
    )
    fun shouldNotAcceptUrl(url: String) {
        assertFalse(linkHandlerFactory.acceptUrl(url))
    }

    companion object {
        private lateinit var linkHandlerFactory: LinkHandlerFactory

        @JvmStatic
        @BeforeAll
        fun setUp() {
            InitNewPipeTest.initEmpty()
            linkHandlerFactory = YouTube.kioskList.getListLinkHandlerFactoryByType("Trending")
        }
    }
}

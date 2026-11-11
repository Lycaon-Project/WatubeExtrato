package org.schabi.newpipe.extractor.services.youtube

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ExtractorAsserts
import org.schabi.newpipe.extractor.InitNewPipeTest

internal class YoutubeJavaScriptExtractorTest {

    fun initNewPipe(useCase: String) {
        InitNewPipeTest.initNewPipe(this.javaClass, useCase)
        YoutubeTestsUtils.ensureStateless()
    }

    @Test
    fun testExtractJavaScriptUrlIframe() {
        initNewPipe("urlWithIframeResource")

        assertTrue(YoutubeJavaScriptExtractor.extractJavaScriptUrlWithIframeResource().endsWith("base.js"))
    }

    @Test
    fun testExtractJavaScriptUrlEmbed() {
        initNewPipe("embedWatchPage")

        assertTrue(
            YoutubeJavaScriptExtractor.extractJavaScriptUrlWithEmbedWatchPage("d4IGg5dqeO8").endsWith("base.js")
        )
    }

    @Test
    fun testExtractJavaScript__success() {
        initNewPipe("playerCode")

        val playerJsCode = YoutubeJavaScriptExtractor.extractJavaScriptPlayerCode("d4IGg5dqeO8")
        assertPlayerJsCode(playerJsCode)
    }

    @Test
    fun testExtractJavaScript__invalidVideoId__success() {
        initNewPipe("playerCodeInvalidVideoId")

        var playerJsCode = YoutubeJavaScriptExtractor.extractJavaScriptPlayerCode("not_a_video_id")
        assertPlayerJsCode(playerJsCode)

        playerJsCode = YoutubeJavaScriptExtractor.extractJavaScriptPlayerCode("11-chars123")
        assertPlayerJsCode(playerJsCode)
    }

    private fun assertPlayerJsCode(playerJsCode: String) {
        ExtractorAsserts.assertContains(
            " Copyright The Closure Library Authors.\n SPDX-License-Identifier: Apache-2.0",
            playerJsCode
        )
        ExtractorAsserts.assertContains("var _yt_player", playerJsCode)
    }
}

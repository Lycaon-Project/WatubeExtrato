package org.schabi.newpipe.extractor.services.peertube

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ServiceList.PeerTube
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.services.DefaultStreamExtractorTest
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamType
import java.util.Locale

abstract class PeertubeStreamExtractorTest : DefaultStreamExtractorTest() {

    override fun expectedHasAudioStreams(): Boolean = false

    class WhatIsPeertube : PeertubeStreamExtractorTest() {

        override fun createExtractor(): StreamExtractor {
            // setting instance might break test when running in parallel (!)
            PeerTube.instance = PeertubeInstance(INSTANCE, "FramaTube")
            return PeerTube.getStreamExtractor(URL)
        }

        @Test
        fun testGetLanguageInformation() {
            assertEquals(Locale.forLanguageTag("en"), extractor().languageInfo)
        }

        override fun expectedService(): StreamingService = PeerTube
        override fun expectedName(): String = "What is PeerTube?"
        override fun expectedId(): String = ID
        override fun expectedUrlContains(): String = INSTANCE + BASE_URL + ID
        override fun expectedOriginalUrlContains(): String = URL

        override fun expectedStreamType(): StreamType = StreamType.VIDEO_STREAM
        override fun expectedUploaderName(): String = "Framasoft"
        override fun expectedUploaderUrl(): String = "https://framatube.org/accounts/framasoft@framatube.org"
        override fun expectedSubChannelName(): String = "A propos de PeerTube"
        override fun expectedSubChannelUrl(): String = "https://framatube.org/video-channels/joinpeertube"

        override fun expectedDescriptionContains(): List<String> = // CRLF line ending
            listOf(
                "**[Want to help to translate this video?]" +
                    "(https://weblate.framasoft.org/projects/what-is-peertube-video/)**\r\n" +
                    "\r\n" +
                    "**Take back the control of your videos! [#JoinPeertube](https://joinpeertube.org)**\r\n" +
                    "*A decentralized video hosting network, based on free/libre software!*\r\n" +
                    "\r\n" +
                    "**Animation Produced by:** [LILA](https://libreart.info) - " +
                    "[ZeMarmot Team](https://film.zemarmot.net)\r\n" +
                    "*Directed by* Aryeom\r\n" +
                    "*Assistant* Jehan\r\n" +
                    "**Licence**: [CC-By-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/)\r\n" +
                    "\r\n" +
                    "**Sponsored by** [Framasoft](https://framasoft.org)\r\n" +
                    "\r\n" +
                    "**Music**: [Red Step Forward](http://play.dogmazic.net/song.php?song_id=52491) - " +
                    "CC-By Ken Bushima\r\n" +
                    "\r\n" +
                    "**Movie Clip**: [Caminades 3: Llamigos](http://www.caminandes.com/) " +
                    "CC-By Blender Institute\r\n" +
                    "\r\n" +
                    "**Video sources**: https://gitlab.gnome.org/Jehan/what-is-peertube/"
            )

        override fun expectedLength(): Long = 113
        override fun expectedTimestamp(): Long = (TIMESTAMP_MINUTE * 60 + TIMESTAMP_SECOND).toLong()
        override fun expectedViewCountAtLeast(): Long = 38600
        override fun expectedUploadDate(): String? = "2018-10-01 10:52:46.396"
        override fun expectedTextualUploadDate(): String? = "2018-10-01T10:52:46.396Z"
        override fun expectedLikeCountAtLeast(): Long = 18
        override fun expectedDislikeCountAtLeast(): Long = 0

        override fun expectedHost(): String = "framatube.org"
        override fun expectedCategory(): String = "Science & Technology"
        override fun expectedLicence(): String = "Attribution - Share Alike"
        override fun expectedLanguageInfo(): Locale? = Locale.forLanguageTag("en")
        override fun expectedTags(): List<String> = listOf("framasoft", "peertube")

        companion object {
            private const val ID = "9c9de5e8-0a1e-484a-b099-e80766180a6d"
            private const val INSTANCE = "https://framatube.org"
            private const val TIMESTAMP_MINUTE = 1
            private const val TIMESTAMP_SECOND = 21
            private const val URL =
                INSTANCE + BASE_URL + ID + "?start=" + TIMESTAMP_MINUTE + "m" + TIMESTAMP_SECOND + "s"
        }
    }

    class HlsOnlyStreams : PeertubeStreamExtractorTest() {

        override fun createExtractor(): StreamExtractor {
            // setting instance might break test when running in parallel (!)
            PeerTube.instance = PeertubeInstance(INSTANCE, "TILvids")
            return PeerTube.getStreamExtractor(URL)
        }

        override fun expectedService(): StreamingService = PeerTube
        override fun expectedName(): String = "A Goodbye to Flash Games"
        override fun expectedId(): String = ID
        override fun expectedUrlContains(): String = INSTANCE + BASE_URL + ID
        override fun expectedOriginalUrlContains(): String = URL

        override fun expectedStreamType(): StreamType = StreamType.VIDEO_STREAM
        override fun expectedUploaderName(): String = "Marinauts"
        override fun expectedUploaderUrl(): String = "https://tilvids.com/accounts/marinauts@tilvids.com"
        override fun expectedSubChannelName(): String = "Main marinauts channel"
        override fun expectedSubChannelUrl(): String = "https://tilvids.com/video-channels/marinauts_channel"

        override fun expectedDescriptionContains(): List<String> = // CRLF line ending
            listOf("Goodbye", "Flash Games", "Anthony takes a minute", "Songs used:")

        override fun expectedLength(): Long = 362
        override fun expectedViewCountAtLeast(): Long = 20
        override fun expectedUploadDate(): String? = "2021-04-08 20:15:32.434"
        override fun expectedTextualUploadDate(): String? = "2021-04-08T20:15:32.434Z"
        override fun expectedLikeCountAtLeast(): Long = 6
        override fun expectedDislikeCountAtLeast(): Long = 0

        override fun expectedHasSubtitles(): Boolean = false
        override fun expectedHost(): String = "tilvids.com"
        override fun expectedCategory(): String = "Entertainment"
        override fun expectedLicence(): String = "Unknown"
        override fun expectedLanguageInfo(): Locale? = null
        override fun expectedTags(): List<String> =
            listOf("Marinauts", "adobe flash", "adobe flash player", "flash games", "the marinauts")
        override fun expectedHasFrames(): Boolean = false // not yet supported by instance

        companion object {
            private const val ID = "41342cb4-6fa8-402d-a116-1f63a7f438a3"
            private const val INSTANCE = "https://tilvids.com"
            private const val URL = INSTANCE + BASE_URL + ID
        }
    }

    @Disabled("Test broken, SSL problem")
    class AgeRestricted : PeertubeStreamExtractorTest() {

        override fun createExtractor(): StreamExtractor {
            // setting instance might break test when running in parallel (!)
            PeerTube.instance = PeertubeInstance(INSTANCE)
            return PeerTube.getStreamExtractor(URL)
        }

        override fun expectedService(): StreamingService = PeerTube
        override fun expectedName(): String = "Covid-19 ? [Court-métrage]"
        override fun expectedId(): String = ID
        override fun expectedUrlContains(): String = INSTANCE + BASE_URL + ID
        override fun expectedOriginalUrlContains(): String = URL

        override fun expectedStreamType(): StreamType = StreamType.VIDEO_STREAM
        override fun expectedUploaderName(): String = "Résilience humaine"
        override fun expectedUploaderUrl(): String = "https://nocensoring.net/accounts/gmt@nocensoring.net"
        override fun expectedSubChannelName(): String = "SYSTEM FAILURE Quel à-venir ?"
        override fun expectedSubChannelUrl(): String = "https://nocensoring.net/video-channels/systemfailure_quel"

        override fun expectedDescriptionContains(): List<String> = // LF line ending
            listOf(
                "2020, le monde est frappé par une pandémie, beaucoup d'humains sont confinés.",
                "System Failure Quel à-venir ? - Covid-19   / 2020"
            )

        override fun expectedLength(): Long = 667
        override fun expectedViewCountAtLeast(): Long = 138
        override fun expectedUploadDate(): String? = "2020-05-14 17:24:35.580"
        override fun expectedTextualUploadDate(): String? = "2020-05-14T17:24:35.580Z"
        override fun expectedLikeCountAtLeast(): Long = 1
        override fun expectedDislikeCountAtLeast(): Long = 0

        override fun expectedAgeLimit(): Int = 18
        override fun expectedHost(): String = "nocensoring.net"
        override fun expectedCategory(): String = "Art"
        override fun expectedLicence(): String = "Attribution"
        override fun expectedTags(): List<String> =
            listOf("Covid-19", "Gérôme-Mary trebor", "Horreur et beauté", "court-métrage", "nue artistique")

        companion object {
            private const val ID = "dbd8e5e1-c527-49b6-b70c-89101dbb9c08"
            private const val INSTANCE = "https://nocensoring.net"
            private const val URL = INSTANCE + "/videos/embed/" + ID
        }
    }

    class Segments : PeertubeStreamExtractorTest() {

        override fun createExtractor(): StreamExtractor {
            // setting instance might break test when running in parallel (!)
            PeerTube.instance = PeertubeInstance(INSTANCE, "tchncs.de")
            return PeerTube.getStreamExtractor(URL)
        }

        override fun expectedService(): StreamingService = PeerTube
        override fun expectedName(): String = "Bauinformatik 11 – Objekte und Methoden"
        override fun expectedId(): String = ID
        override fun expectedUrlContains(): String = INSTANCE + BASE_URL + ID
        override fun expectedOriginalUrlContains(): String = URL

        override fun expectedStreamType(): StreamType = StreamType.VIDEO_STREAM
        override fun expectedUploaderName(): String = "Martin Vogel"
        override fun expectedUploaderUrl(): String = "https://tube.tchncs.de/accounts/martin_vogel@tube.tchncs.de"
        override fun expectedSubChannelName(): String = "Bauinformatik mit Python"
        override fun expectedSubChannelUrl(): String = "https://tube.tchncs.de/video-channels/python"

        override fun expectedDescriptionContains(): List<String> = // CRLF line ending
            listOf("Um", "Programme", "Variablen", "Funktionen", "Objekte", "Skript", "Wiederholung", "Listen")

        override fun expectedLength(): Long = 1017
        override fun expectedViewCountAtLeast(): Long = 20
        override fun expectedUploadDate(): String? = "2023-12-08 15:57:04.142"
        override fun expectedTextualUploadDate(): String? = "2023-12-08T15:57:04.142Z"
        override fun expectedLikeCountAtLeast(): Long = 0
        override fun expectedDislikeCountAtLeast(): Long = 0

        override fun expectedHasSubtitles(): Boolean = false
        override fun expectedHost(): String = "tube.tchncs.de"
        override fun expectedCategory(): String = "Unknown"
        override fun expectedLicence(): String = "Unknown"
        override fun expectedLanguageInfo(): Locale? = null
        override fun expectedTags(): List<String> = listOf("Attribute", "Bauinformatik", "Klassen", "Objekte", "Python")

        companion object {
            private const val ID = "vqABGP97fEjo7RhPuDnSZk"
            private const val INSTANCE = "https://tube.tchncs.de"
            private const val URL = INSTANCE + BASE_URL + ID
        }
    }

    @BeforeAll
    override fun setUp() {
        super.setUp()
        PeerTube.instance = PeertubeInstance("https://peertube.cpy.re", "PeerTube test server")
    }

    @Test
    fun testGetEmptyDescription() {
        val extractorEmpty =
            PeerTube.getStreamExtractor("https://framatube.org/api/v1/videos/d5907aad-2252-4207-89ec-a4b687b9337d")
        extractorEmpty.fetchPage()
        assertEquals("", extractorEmpty.description.content)
    }

    @Test
    fun testGetSmallDescription() {
        val extractorSmall =
            PeerTube.getStreamExtractor("https://peertube.cpy.re/videos/watch/d2a5ec78-5f85-4090-8ec5-dc1102e022ea")
        extractorSmall.fetchPage()
        assertEquals(
            "https://www.kickstarter.com/projects/1587081065/nothing-to-hide-the-documentary",
            extractorSmall.description.content
        )
    }

    @Test
    fun testGetSupportInformation() {
        val supportInfoExtractor =
            PeerTube.getStreamExtractor("https://framatube.org/videos/watch/ee408ec8-07cd-4e35-b884-fb681a4b9d37")
        supportInfoExtractor.fetchPage()
        assertEquals("https://utip.io/chatsceptique", supportInfoExtractor.supportInfo)
    }

    companion object {
        internal const val BASE_URL = "/videos/watch/"
    }
}

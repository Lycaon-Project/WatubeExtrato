/*
 * Created by Christian Schabesberger on 30.12.15.
 *
 * Copyright (C) 2015 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * YoutubeVideoExtractorDefault.java is part of NewPipe Extractor.
 *
 * NewPipe Extractor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe Extractor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe Extractor. If not, see <https://www.gnu.org/licenses/>.
 */

package org.schabi.newpipe.extractor.services.youtube.stream

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.InitNewPipeTest
import org.schabi.newpipe.extractor.MetaInfo
import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.GeographicRestrictionException
import org.schabi.newpipe.extractor.exceptions.PaidContentException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.PrivateContentException
import org.schabi.newpipe.extractor.exceptions.YoutubeMusicPremiumContentException
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest
import org.schabi.newpipe.extractor.services.DefaultStreamExtractorTest
import org.schabi.newpipe.extractor.services.youtube.InitYoutubeTest
import org.schabi.newpipe.extractor.services.youtube.YoutubeTestsUtils
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor
import org.schabi.newpipe.extractor.stream.AudioTrackType
import org.schabi.newpipe.extractor.stream.Description
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamType
import java.net.MalformedURLException
import java.net.URI
import java.util.Locale

class YoutubeStreamExtractorDefaultTest {

    class NotAvailable {

        fun initNewPipe(useCase: String) {
            InitNewPipeTest.initNewPipe(this.javaClass, useCase)
            YoutubeTestsUtils.ensureStateless()
        }

        @Disabled(
            "broken upstream as well (TeamNewPipe/NewPipeExtractor dev 6faf4068, 2026-10): " +
                "YouTube answers \"This video is unavailable\" without naming the country restriction",
        )
        @Test
        @Throws(Exception::class)
        fun geoRestrictedContent() {
            initNewPipe("restricted")

            val extractor = YouTube.getStreamExtractor(BASE_URL + "_PL2HJKxnOM")
            assertThrows(GeographicRestrictionException::class.java) { extractor.fetchPage() }
        }

        @Test
        @Throws(Exception::class)
        fun nonExistentFetch() {
            initNewPipe("nonExistent")

            val extractor = YouTube.getStreamExtractor(BASE_URL + "don-t-exist")
            assertThrows(ParsingException::class.java) { extractor.fetchPage() }
        }

        @Test
        @Throws(Exception::class)
        fun invalidId() {
            initNewPipe("invalidId")

            val extractor = YouTube.getStreamExtractor(BASE_URL + "INVALID_ID_INVALID_ID")
            assertThrows(ParsingException::class.java) { extractor.fetchPage() }
        }

        @Test
        @Throws(Exception::class)
        fun paidContent() {
            initNewPipe("paidContent")

            val extractor = YouTube.getStreamExtractor(BASE_URL + "ayI2iBwGdxw")
            assertThrows(PaidContentException::class.java) { extractor.fetchPage() }
        }

        @Test
        @Throws(Exception::class)
        fun privateContent() {
            initNewPipe("privateContent")

            val extractor = YouTube.getStreamExtractor(BASE_URL + "8VajtrESJzA")
            assertThrows(PrivateContentException::class.java) { extractor.fetchPage() }
        }

        @Test
        @Throws(Exception::class)
        fun youtubeMusicPremiumContent() {
            initNewPipe("musicPremiumContent")

            val extractor = YouTube.getStreamExtractor(BASE_URL + "sMJ8bRN2dak")
            assertThrows(YoutubeMusicPremiumContentException::class.java) { extractor.fetchPage() }
        }
    }

    class DescriptionTestPewdiepie : DefaultStreamExtractorTest(), InitYoutubeTest {
        @Throws(Exception::class)
        override fun createExtractor(): StreamExtractor = YouTube.getStreamExtractor(URL)

        override fun expectedService(): StreamingService = YouTube
        override fun expectedName(): String = "Marzia & Felix - Wedding 19.08.2019"
        override fun expectedId(): String = ID
        override fun expectedUrlContains(): String = BASE_URL + ID
        override fun expectedOriginalUrlContains(): String = URL

        override fun expectedStreamType(): StreamType = StreamType.VIDEO_STREAM
        override fun expectedUploaderName(): String = "PewDiePie"
        override fun expectedUploaderUrl(): String = "https://www.youtube.com/channel/UC-lHJZR3Gqxm24_Vd_AJ5Yw"
        override fun expectedUploaderSubscriberCountAtLeast(): Long = 100_000_000
        override fun expectedDescriptionContains(): List<String> = listOf(
            "https://www.youtube.com/channel/UC7l23W7gFi4Uho6WSzckZRA",
            "https://www.handcraftpictures.com/",
        )
        override fun expectedUploaderVerified(): Boolean = true
        override fun expectedLength(): Long = 381
        override fun expectedTimestamp(): Long = TIMESTAMP.toLong()
        override fun expectedViewCountAtLeast(): Long = 26682500
        override fun expectedUploadDate(): String? = "2019-08-24 15:39:57.000"
        override fun expectedTextualUploadDate(): String? = "2019-08-24T08:39:57-07:00"
        override fun expectedLikeCountAtLeast(): Long = 5212900
        override fun expectedDislikeCountAtLeast(): Long = -1
        override fun expectedStreamSegmentsCount(): Int = 0
        override fun expectedLicence(): String = YOUTUBE_LICENCE
        override fun expectedCategory(): String = "Entertainment"

        private companion object {
            const val ID = "7PIMiDcwNvc"
            const val TIMESTAMP = 7483
            const val URL = BASE_URL + ID + "&t=" + TIMESTAMP + "s"
        }
    }

    class DescriptionTestUnboxing : DefaultStreamExtractorTest(), InitYoutubeTest {
        @Throws(Exception::class)
        override fun createExtractor(): StreamExtractor = YouTube.getStreamExtractor(URL)

        override fun expectedService(): StreamingService = YouTube
        override fun expectedName(): String = "This Smartphone Changes Everything..."
        override fun expectedId(): String = ID
        override fun expectedUrlContains(): String = URL
        override fun expectedOriginalUrlContains(): String = URL

        override fun expectedStreamType(): StreamType = StreamType.VIDEO_STREAM
        override fun expectedUploaderName(): String = "Unbox Therapy"
        override fun expectedUploaderUrl(): String = "https://www.youtube.com/channel/UCsTcErHg8oDvUnTzoqsYeNw"
        override fun expectedUploaderSubscriberCountAtLeast(): Long = 18_000_000
        override fun expectedDescriptionContains(): List<String> = listOf(
            "https://www.youtube.com/watch?v=X7FLCHVXpsA&amp;list=PL7u4lWXQ3wfI_7PgX0C-VTiwLeu0S4v34",
            "https://www.youtube.com/watch?v=Lqv6G0pDNnw&amp;list=PL7u4lWXQ3wfI_7PgX0C-VTiwLeu0S4v34",
            "https://www.youtube.com/watch?v=XxaRBPyrnBU&amp;list=PL7u4lWXQ3wfI_7PgX0C-VTiwLeu0S4v34",
            "https://www.youtube.com/watch?v=U-9tUEOFKNU&amp;list=PL7u4lWXQ3wfI_7PgX0C-VTiwLeu0S4v34",
        )
        override fun expectedLength(): Long = 434
        override fun expectedViewCountAtLeast(): Long = 21229200
        override fun expectedUploadDate(): String? = "2018-06-19 19:41:34.000"
        override fun expectedTextualUploadDate(): String? = "2018-06-19T12:41:34-07:00"
        override fun expectedLikeCountAtLeast(): Long = 340100
        override fun expectedDislikeCountAtLeast(): Long = -1
        override fun expectedUploaderVerified(): Boolean = true
        override fun expectedLicence(): String = YOUTUBE_LICENCE
        override fun expectedCategory(): String = "Science & Technology"
        override fun expectedTags(): List<String> = listOf(
            "2018", "8 plus", "apple", "apple iphone", "apple iphone x", "best", "best android",
            "best smartphone", "cool gadgets", "find", "find x", "find x review", "find x unboxing", "findx",
            "galaxy s9", "galaxy s9+", "hands on", "iphone 8", "iphone 8 plus", "iphone x", "new iphone", "nex",
            "oneplus 6", "oppo", "oppo find x", "oppo find x hands on", "oppo find x review",
            "oppo find x unboxing", "oppo findx", "pixel 2 xl", "review", "samsung", "samsung galaxy",
            "samsung galaxy s9", "smartphone", "unbox therapy", "unboxing", "vivo", "vivo apex", "vivo nex",
        )

        private companion object {
            const val ID = "cV5TjZCJkuA"
            const val URL = BASE_URL + ID
        }
    }

    @Disabled("Broken, as we cannot extract videos made for kids for now and this is one of them")
    class RatingsDisabledTest : DefaultStreamExtractorTest(), InitYoutubeTest {
        @Throws(Exception::class)
        override fun createExtractor(): StreamExtractor = YouTube.getStreamExtractor(URL)

        override fun expectedService(): StreamingService = YouTube
        override fun expectedName(): String = "Introduction to Doodle for Google 2023"
        override fun expectedId(): String = ID
        override fun expectedUrlContains(): String = BASE_URL + ID
        override fun expectedOriginalUrlContains(): String = URL

        override fun expectedStreamType(): StreamType = StreamType.VIDEO_STREAM
        override fun expectedUploaderName(): String = "GoogleDoodles"
        override fun expectedUploaderUrl(): String = "https://www.youtube.com/channel/UCdq61m8s_48EhJ5OM_MCeGw"
        override fun expectedUploaderSubscriberCountAtLeast(): Long = 2200000
        override fun expectedDescriptionContains(): List<String> = listOf("Doodle", "Google", "video")
        override fun expectedLength(): Long = 145
        override fun expectedTimestamp(): Long = TIMESTAMP.toLong()
        override fun expectedViewCountAtLeast(): Long = 40000
        override fun expectedUploadDate(): String? = "2023-01-13 21:53:57.000"
        override fun expectedTextualUploadDate(): String? = "2023-01-13T13:53:57-08:00"
        override fun expectedLikeCountAtLeast(): Long = -1
        override fun expectedDislikeCountAtLeast(): Long = -1
        override fun expectedLicence(): String = YOUTUBE_LICENCE
        override fun expectedCategory(): String = "Education"

        private companion object {
            const val ID = "it3OtbTxQk0"
            const val TIMESTAMP = 17
            const val URL = BASE_URL + ID + "&t=" + TIMESTAMP
        }
    }

    class StreamSegmentsTestTagesschau : DefaultStreamExtractorTest(), InitYoutubeTest {
        // StreamSegment example with single macro-makers panel
        @Throws(Exception::class)
        override fun createExtractor(): StreamExtractor = YouTube.getStreamExtractor(URL)

        override fun expectedService(): StreamingService = YouTube
        override fun expectedName(): String = "tagesschau 20:00 Uhr, 17.03.2021"
        override fun expectedId(): String = ID
        override fun expectedUrlContains(): String = BASE_URL + ID
        override fun expectedOriginalUrlContains(): String = URL

        override fun expectedStreamType(): StreamType = StreamType.VIDEO_STREAM
        override fun expectedUploaderName(): String = "tagesschau"
        override fun expectedUploaderUrl(): String = "https://www.youtube.com/channel/UC5NOEUbkLheQcaaRldYW5GA"
        override fun expectedUploaderSubscriberCountAtLeast(): Long = 1_000_000
        override fun expectedUploaderVerified(): Boolean = true
        override fun expectedDescriptionContains(): List<String> =
            listOf("Themen der Sendung", "07:15", "Wetter", "Sendung nachträglich bearbeitet")
        override fun expectedLength(): Long = 953
        override fun expectedViewCountAtLeast(): Long = 270000
        override fun expectedUploadDate(): String? = "2021-03-17 19:56:59.000"
        override fun expectedTextualUploadDate(): String? = "2021-03-17T12:56:59-07:00"
        override fun expectedLikeCountAtLeast(): Long = 2300
        override fun expectedDislikeCountAtLeast(): Long = -1
        override fun expectedStreamSegmentsCount(): Int = 13
        override fun expectedLicence(): String = YOUTUBE_LICENCE
        override fun expectedCategory(): String = "News & Politics"

        @Test
        @Throws(Exception::class)
        fun testStreamSegment0() {
            val segment = extractor().streamSegments[0]
            assertEquals(0, segment.startTimeSeconds)
            assertEquals("Guten Abend", segment.title)
            assertEquals(BASE_URL + ID + "?t=0", segment.url)
            assertNotNull(segment.previewUrl)
        }

        @Test
        @Throws(Exception::class)
        fun testStreamSegment3() {
            val segment = extractor().streamSegments[3]
            assertEquals(224, segment.startTimeSeconds)
            assertEquals("Pandemie dämpft Konjunkturprognose für 2021", segment.title)
            assertEquals(BASE_URL + ID + "?t=224", segment.url)
            assertNotNull(segment.previewUrl)
        }

        private companion object {
            const val ID = "KI7fMGRg0Wk"
            const val URL = BASE_URL + ID
        }
    }

    class StreamSegmentsTestMaiLab : DefaultStreamExtractorTest(), InitYoutubeTest {
        // StreamSegment example with macro-makers panel and transcription panel
        @Throws(Exception::class)
        override fun createExtractor(): StreamExtractor = YouTube.getStreamExtractor(URL)

        override fun expectedService(): StreamingService = YouTube
        override fun expectedName(): String = "Vitamin D wissenschaftlich geprüft"
        override fun expectedId(): String = ID
        override fun expectedUrlContains(): String = BASE_URL + ID
        override fun expectedOriginalUrlContains(): String = URL

        override fun expectedStreamType(): StreamType = StreamType.VIDEO_STREAM
        override fun expectedUploaderName(): String = "MAITHINK X"
        override fun expectedUploaderUrl(): String = "https://www.youtube.com/channel/UCyHDQ5C6z1NDmJ4g6SerW8g"
        override fun expectedUploaderSubscriberCountAtLeast(): Long = 1_400_000
        override fun expectedDescriptionContains(): List<String> =
            listOf("Vitamin", "2:44", "Was ist Vitamin D?")
        override fun expectedUploaderVerified(): Boolean = true
        override fun expectedLength(): Long = 1010
        override fun expectedViewCountAtLeast(): Long = 815500
        override fun expectedUploadDate(): String? = "2020-11-19 05:30:01.000"
        override fun expectedTextualUploadDate(): String? = "2020-11-18T21:30:01-08:00"
        override fun expectedLikeCountAtLeast(): Long = 48500
        override fun expectedDislikeCountAtLeast(): Long = -1
        override fun expectedStreamSegmentsCount(): Int = 7
        override fun expectedLicence(): String = YOUTUBE_LICENCE
        override fun expectedCategory(): String = "Science & Technology"
        override fun expectedTags(): List<String> = listOf(
            "Diabetes", "Erkältung", "Gesundheit", "Immunabwehr", "Immunsystem", "Infektion",
            "Komisch alles chemisch", "Krebs", "Lab", "Lesch", "Mai", "Mai Thi", "Mai Thi Nguyen-Kim",
            "Mangel", "Nahrungsergänzungsmittel", "Nguyen", "Nguyen Kim", "Nguyen-Kim", "Quarks", "Sommer",
            "Supplemente", "Supplements", "Tabletten", "Terra X", "TerraX", "The Secret Life Of Scientists",
            "Tropfen", "Vitamin D", "Vitamin-D-Mangel", "Vitamine", "Winter", "einnehmen", "maiLab", "nehmen",
            "supplementieren", "Überdosis", "Überschuss",
        )

        @Test
        @Throws(Exception::class)
        fun testStreamSegment() {
            val segment = extractor().streamSegments[1]
            assertEquals(164, segment.startTimeSeconds)
            assertEquals("Was ist Vitamin D?", segment.title)
            assertEquals(BASE_URL + ID + "?t=164", segment.url)
            assertNotNull(segment.previewUrl)
        }

        @Test
        @Disabled("encoding problem")
        override fun testName() {
        }

        @Test
        @Disabled("encoding problem")
        override fun testTags() {
        }

        private companion object {
            const val ID = "ud9d5cMDP_0"
            const val URL = BASE_URL + ID
        }
    }

    class PublicBroadcasterTest : DefaultStreamExtractorTest(), InitYoutubeTest {
        @Throws(Exception::class)
        override fun createExtractor(): StreamExtractor = YouTube.getStreamExtractor(STREAM_URL)

        override fun expectedService(): StreamingService = YouTube
        override fun expectedName(): String = "Merci pour les 3 millions d'abonnés 🎉| ARTE"
        override fun expectedId(): String = ID
        override fun expectedUrlContains(): String = BASE_URL + ID
        override fun expectedOriginalUrlContains(): String = STREAM_URL

        override fun expectedStreamType(): StreamType = StreamType.VIDEO_STREAM
        override fun expectedUploaderName(): String = "ARTE"
        override fun expectedUploaderUrl(): String = "https://www.youtube.com/channel/UCwI-JbGNsojunnHbFAc0M4Q"
        override fun expectedUploaderSubscriberCountAtLeast(): Long = 3_000_000
        override fun expectedDescriptionContains(): List<String> = listOf("sommets", "fans", "cadeau")
        override fun expectedLength(): Long = 45
        override fun expectedTimestamp(): Long = TIMESTAMP.toLong()
        override fun expectedViewCountAtLeast(): Long = 20_000
        override fun expectedUploadDate(): String? = "2023-07-07 15:30:08.000"
        override fun expectedTextualUploadDate(): String? = "2023-07-07T08:30:08-07:00"
        override fun expectedLikeCountAtLeast(): Long = 1000
        override fun expectedDislikeCountAtLeast(): Long = -1

        @Throws(MalformedURLException::class)
        override fun expectedMetaInfo(): List<MetaInfo> = listOf(
            MetaInfo(
                "",
                Description("Arte is a French/German public broadcast service.", Description.Type.PLAIN_TEXT),
                mutableListOf(URI("https://en.wikipedia.org/wiki/Arte?wprov=yicw1").toURL()),
                mutableListOf("Wikipedia"),
            ),
        )
        override fun expectedUploaderVerified(): Boolean = true
        override fun expectedLicence(): String = YOUTUBE_LICENCE
        override fun expectedCategory(): String = "News & Politics"
        override fun expectedTags(): List<String> = listOf(
            "arte", "arte 3 millions", "arte remerciement",
            "documentaire arte", "arte documentaire", "fan d'arte", "arte youtube",
        )

        private companion object {
            const val ID = "cJ9to6EmElQ"
            const val TIMESTAMP = 0
            const val STREAM_URL = BASE_URL + ID
        }
    }

    class LicensedDoctorTest : DefaultStreamExtractorTest(), InitYoutubeTest {
        @Throws(Exception::class)
        override fun createExtractor(): StreamExtractor = YouTube.getStreamExtractor(STREAM_URL)

        override fun expectedService(): StreamingService = YouTube
        override fun expectedName(): String =
            "Laryngeal Mask Airway (LMA) insertion | ESSENTIAL TECHNIQUES for standard patients"
        override fun expectedId(): String = ID
        override fun expectedUrlContains(): String = BASE_URL + ID
        override fun expectedOriginalUrlContains(): String = STREAM_URL

        override fun expectedStreamType(): StreamType = StreamType.VIDEO_STREAM
        override fun expectedUploaderName(): String = "ABCs of Anaesthesia"
        override fun expectedUploaderUrl(): String = "https://www.youtube.com/channel/UCs1fy2n5Ey0c9VZRBL7UCsg"
        override fun expectedUploaderSubscriberCountAtLeast(): Long = 200_000
        override fun expectedDescriptionContains(): List<String> =
            listOf("https://www.facebook.com/groups/2082807131964430", "LMA")
        override fun expectedLength(): Long = 343
        override fun expectedTimestamp(): Long = TIMESTAMP.toLong()
        override fun expectedViewCountAtLeast(): Long = 60_000
        override fun expectedUploadDate(): String? = "2022-07-03 11:00:25.000"
        override fun expectedTextualUploadDate(): String? = "2022-07-03T04:00:25-07:00"
        override fun expectedLikeCountAtLeast(): Long = 800
        override fun expectedDislikeCountAtLeast(): Long = -1

        @Throws(MalformedURLException::class)
        override fun expectedMetaInfo(): List<MetaInfo> = listOf(
            MetaInfo(
                "",
                Description("From a licensed doctor in Australia", Description.Type.PLAIN_TEXT),
                mutableListOf(URI("https://support.google.com/youtube/answer/9795167").toURL()),
                mutableListOf("Learn more about how experts define health sources"),
            ),
        )
        override fun expectedUploaderVerified(): Boolean = true
        override fun expectedLicence(): String = YOUTUBE_LICENCE
        override fun expectedCategory(): String = "Education"
        override fun expectedTags(): List<String> = listOf(
            "ANZCA", "Anaesthesia", "FANZCA", "LMA", "Vortex", "abcs of anaesthesia",
            "airway", "anaesthesiology", "anaesthetics", "anaesthetist", "anesthesia", "anesthesiologist",
            "anesthesiology", "anesthetics", "anesthetist", "bag", "basics", "cannula insertion technique",
            "cannulation procedure", "cannulation tips and tricks", "cico", "classic", "cuff", "das",
            "difficult airway", "igel", "intravenous cannula insertion", "intubation", "laryngeal", "mask",
            "medical", "medicine", "nurse", "oxygenation", "procedure", "proseal", "student", "supreme",
            "technique", "tips", "venepuncture", "ventilation",
        )

        private companion object {
            const val ID = "rCAS2eD-mcw"
            const val TIMESTAMP = 0
            const val STREAM_URL = BASE_URL + ID
        }
    }

    class NoVisualMetadataVideoTest : DefaultStreamExtractorTest(), InitYoutubeTest {
        // Video without visual metadata on YouTube clients (video title, upload date, channel name,
        // comments, ...)
        @Throws(Exception::class)
        override fun createExtractor(): StreamExtractor = YouTube.getStreamExtractor(STREAM_URL)

        override fun expectedStreamType(): StreamType = StreamType.VIDEO_STREAM
        override fun expectedUploaderName(): String = "Makani"
        override fun expectedUploaderUrl(): String = "https://www.youtube.com/channel/UC-iMZJ8NppwT2fLwzFWJKOQ"
        override fun expectedDescriptionContains(): List<String> = listOf("Makani", "prototype", "rotors")
        override fun expectedLength(): Long = 175
        override fun expectedViewCountAtLeast(): Long = 88_000
        override fun expectedUploadDate(): String? = "2017-05-16 14:50:53.000"
        override fun expectedTextualUploadDate(): String? = "2017-05-16T07:50:53-07:00"
        override fun expectedLikeCountAtLeast(): Long = -1
        override fun expectedDislikeCountAtLeast(): Long = -1
        override fun expectedService(): StreamingService = YouTube
        override fun expectedName(): String = "Makani’s first commercial-scale energy kite"
        override fun expectedId(): String = "An8vtD1FDqs"
        override fun expectedUrlContains(): String = BASE_URL + ID
        override fun expectedOriginalUrlContains(): String = STREAM_URL
        override fun expectedCategory(): String = "Science & Technology"
        override fun expectedLicence(): String = YOUTUBE_LICENCE
        override fun expectedTags(): List<String> = listOf(
            "Makani", "Moonshot", "Moonshot Factory", "Prototyping",
            "california", "california wind", "clean", "clean energy", "climate change",
            "climate crisis", "energy", "energy kite", "google", "google x", "green",
            "green energy", "kite", "kite power", "kite power solutions",
            "kite power systems", "makani power", "power", "renewable", "renewable energy",
            "renewable energy engineering", "renewable energy projects",
            "renewable energy sources", "renewables", "solutions", "tech", "technology",
            "turbine", "wind", "wind energy", "wind power", "wind turbine", "windmill",
        )

        @Test
        override fun testSubscriberCount() {
            assertThrows(ParsingException::class.java) { extractor().uploaderSubscriberCount }
        }

        @Test
        override fun testLikeCount() {
            assertThrows(ParsingException::class.java) { extractor().likeCount }
        }

        @Test
        override fun testUploaderAvatars() {
            assertThrows(ParsingException::class.java) { extractor().uploaderAvatars }
        }

        private companion object {
            const val ID = "An8vtD1FDqs"
            const val STREAM_URL = BASE_URL + ID
        }
    }

    class UnlistedTest : DefaultSimpleExtractorTest<YoutubeStreamExtractor>(), InitYoutubeTest {
        @Throws(Exception::class)
        override fun createExtractor(): YoutubeStreamExtractor =
            YouTube.getStreamExtractor("https://www.youtube.com/watch?v=tjz2u2DiveM") as YoutubeStreamExtractor

        @Test
        fun testGetUnlisted() {
            assertEquals(StreamExtractor.Privacy.UNLISTED, extractor().privacy)
        }
    }

    class CCLicensed : DefaultSimpleExtractorTest<StreamExtractor>(), InitYoutubeTest {
        @Throws(Exception::class)
        override fun createExtractor(): StreamExtractor = YouTube.getStreamExtractor(URL)

        @Test
        @Throws(ParsingException::class)
        fun testGetLicence() {
            assertEquals("Creative Commons Attribution licence (reuse allowed)", extractor().licence)
        }

        private companion object {
            const val ID = "M4gD1WSo5mA"
            const val URL = BASE_URL + ID
        }
    }

    class AudioTrackLanguage : DefaultSimpleExtractorTest<StreamExtractor>(), InitYoutubeTest {
        @Throws(Exception::class)
        override fun createExtractor(): StreamExtractor = YouTube.getStreamExtractor(URL)

        @Test
        @Throws(Exception::class)
        fun testCheckAudioStreams() {
            val audioStreams = requireNotNull(extractor().audioStreams)
            assertFalse(audioStreams.isEmpty())

            for (stream in audioStreams) {
                assertNotNull(stream.audioTrackName)
            }

            assertTrue(audioStreams.any { "English original" == it.audioTrackName })

            val hindiLocale = Locale.forLanguageTag("hi")
            assertTrue(audioStreams.any { it.audioLocale == hindiLocale })
        }

        private companion object {
            const val ID = "kX3nB4PpJko"
            const val URL = BASE_URL + ID
        }
    }

    class AudioTrackTypes : DefaultSimpleExtractorTest<StreamExtractor>(), InitYoutubeTest {
        @Throws(Exception::class)
        override fun createExtractor(): StreamExtractor = YouTube.getStreamExtractor(URL)

        @Test
        @Throws(Exception::class)
        fun testCheckOriginalAudio() {
            val audioStreams = requireNotNull(extractor().audioStreams)
            assertFalse(audioStreams.isEmpty())

            assertTrue(audioStreams.any { it.audioTrackType == AudioTrackType.ORIGINAL })
        }

        @Test
        @Throws(Exception::class)
        fun testCheckDubbedAudio() {
            assertTrue(requireNotNull(extractor().audioStreams).any { it.audioTrackType == AudioTrackType.DUBBED })
        }

        @Test
        @Throws(Exception::class)
        fun testCheckDescriptiveAudio() {
            assertTrue(
                requireNotNull(extractor().audioStreams).any { it.audioTrackType == AudioTrackType.DESCRIPTIVE },
            )
        }

        private companion object {
            const val ID = "Kn56bMZ9OE8"
            const val URL = BASE_URL + ID
        }
    }

    class AutoGeneratedAudio : DefaultSimpleExtractorTest<StreamExtractor>(), InitYoutubeTest {
        @Throws(Exception::class)
        override fun createExtractor(): StreamExtractor = YouTube.getStreamExtractor(URL)

        @Test
        @Throws(Exception::class)
        fun testAutogeneratedTracks() {
            val audioStreams = requireNotNull(extractor().audioStreams)
            assertFalse(audioStreams.isEmpty())

            assertTrue(audioStreams.any { it.itagItem?.isAutoGenerated == true })
        }

        private companion object {
            const val ID = "3JW3J4_Ukrs"
            const val URL = BASE_URL + ID
        }
    }

    companion object {
        const val BASE_URL = "https://www.youtube.com/watch?v="
        const val YOUTUBE_LICENCE = "YouTube licence"
    }
}

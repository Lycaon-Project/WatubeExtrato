package org.schabi.newpipe.extractor.services.youtube

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.schabi.newpipe.extractor.ExtractorAsserts.assertGreater
import org.schabi.newpipe.extractor.ExtractorAsserts.assertGreaterOrEqual
import org.schabi.newpipe.extractor.ExtractorAsserts.assertIsValidUrl
import org.schabi.newpipe.extractor.ExtractorAsserts.assertNotBlank
import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.CreationException
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.ADAPTATION_SET
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.AUDIO_CHANNEL_CONFIGURATION
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.BASE_URL
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.INITIALIZATION
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.MPD
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.PERIOD
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.REPRESENTATION
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.ROLE
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.SEGMENT_BASE
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.SEGMENT_TEMPLATE
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.SEGMENT_TIMELINE
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeOtfDashManifestCreator
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeProgressiveDashManifestCreator
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor
import org.schabi.newpipe.extractor.stream.AudioTrackType
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.Stream
import org.schabi.newpipe.extractor.utils.Utils.isBlank
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Test for YouTube DASH manifest creators.
 *
 * Tests the generation of OTF and progressive manifests.
 *
 * We cannot test the generation of DASH manifests for ended livestreams because these videos will
 * be re-encoded as normal videos later, so we can't use a specific video.
 *
 * The generation of DASH manifests for OTF streams, which can be tested, uses a video licenced
 * under the Creative Commons Attribution licence (reuse allowed): `A New Era of Open?
 * COVID-19 and the Pursuit for Equitable Solutions`
 * ([https://www.youtube.com/watch?v=DJ8GQUNUXGM](https://www.youtube.com/watch?v=DJ8GQUNUXGM))
 *
 * We couldn't use mocks for these tests because the streaming URLs needs to fetched and the IP
 * address used to get these URLs is required (used as a param in the URLs; without it, video
 * servers return 403/Forbidden HTTP response code).
 *
 * So the real downloader will be used everytime on this test class.
 */
internal class YoutubeDashManifestCreatorsTest :
    DefaultSimpleExtractorTest<YoutubeStreamExtractor>(),
    InitYoutubeTest {

    private var videoLength: Long = 0

    override fun createExtractor(): YoutubeStreamExtractor = YouTube.getStreamExtractor(URL) as YoutubeStreamExtractor

    override fun fetchExtractor(extractor: YoutubeStreamExtractor) {
        super.fetchExtractor(extractor)
        videoLength = extractor.length
    }

    @Test
    fun testOtfStreams() {
        assertDashStreams(extractor().videoOnlyStreams)
        assertDashStreams(extractor().audioStreams)

        // no video stream with audio uses the DASH delivery method (YouTube OTF stream type)
        assertEquals(0, assertFilterStreams(extractor().videoStreams, DeliveryMethod.DASH).size)
    }

    @Disabled(
        "broken upstream as well (TeamNewPipe/NewPipeExtractor dev 6faf4068, 2026-10): the recorded video " +
            "has no progressive stream left to reject"
    )
    @Test
    fun testProgressiveStreams() {
        assertProgressiveStreams(extractor().videoOnlyStreams)
        assertProgressiveStreams(extractor().audioStreams)

        // we are not able to generate DASH manifests of video formats with audio
        assertThrows(CreationException::class.java) {
            assertProgressiveStreams(extractor().videoStreams)
        }
    }

    private fun assertDashStreams(streams: List<Stream>) {
        for (stream in assertFilterStreams(streams, DeliveryMethod.DASH)) {
            val itagItem = stream.itagItem!!
            val manifest = YoutubeOtfDashManifestCreator.fromOtfStreamingUrl(
                stream.content,
                itagItem,
                videoLength
            )
            assertNotBlank(manifest)

            assertManifestGenerated(manifest, itagItem) { document ->
                assertAll(
                    { assertSegmentTemplateElement(document) },
                    { assertSegmentTimelineAndSElements(document) }
                )
            }
        }
    }

    private fun assertProgressiveStreams(streams: List<Stream>) {
        for (stream in assertFilterStreams(streams, DeliveryMethod.PROGRESSIVE_HTTP)) {
            val itagItem = stream.itagItem!!
            val manifest = YoutubeProgressiveDashManifestCreator.fromProgressiveStreamingUrl(
                stream.content,
                itagItem,
                videoLength
            )
            assertNotBlank(manifest)

            assertManifestGenerated(manifest, itagItem) { document ->
                assertAll(
                    { assertBaseUrlElement(document) },
                    { assertSegmentBaseElement(document, itagItem) },
                    { assertInitializationElement(document, itagItem) }
                )
            }
        }
    }

    private fun assertFilterStreams(streams: List<Stream>, deliveryMethod: DeliveryMethod): List<Stream> {
        val filteredStreams = streams
            .filter { stream -> stream.deliveryMethod == deliveryMethod }
            .take(MAX_STREAMS_TO_TEST_PER_METHOD)

        assertAll(
            filteredStreams.flatMap { stream ->
                listOf(
                    Executable { assertNotBlank(stream.content) },
                    Executable { assertNotNull(stream.itagItem) }
                )
            }
        )

        return filteredStreams
    }

    private fun assertManifestGenerated(
        dashManifest: String,
        itagItem: ItagItem,
        additionalAsserts: (Document) -> Unit
    ) {
        val documentBuilderFactory = DocumentBuilderFactory.newInstance()
        val documentBuilder = documentBuilderFactory.newDocumentBuilder()
        val document = documentBuilder.parse(InputSource(StringReader(dashManifest)))

        assertAll(
            { assertMpdElement(document) },
            { assertPeriodElement(document) },
            { assertAdaptationSetElement(document, itagItem) },
            { assertRoleElement(document, itagItem) },
            { assertRepresentationElement(document, itagItem) },
            {
                if (itagItem.itagType == ItagItem.ItagType.AUDIO) {
                    assertAudioChannelConfigurationElement(document, itagItem)
                }
            },
            { additionalAsserts(document) }
        )
    }

    private fun assertMpdElement(document: Document) {
        val element = document.getElementsByTagName(MPD).item(0) as Element?
        assertNotNull(element)
        assertNull(element!!.parentNode.nodeValue)

        val mediaPresentationDuration = element.getAttribute("mediaPresentationDuration")
        assertNotNull(mediaPresentationDuration)
        assertTrue(mediaPresentationDuration.startsWith("PT"))
    }

    private fun assertPeriodElement(document: Document) {
        assertGetElement(document, PERIOD, MPD)
    }

    private fun assertAdaptationSetElement(document: Document, itagItem: ItagItem) {
        val element = assertGetElement(document, ADAPTATION_SET, PERIOD)
        assertAttrEquals(itagItem.mediaFormat.mimeType, element, "mimeType")

        if (itagItem.itagType == ItagItem.ItagType.AUDIO) {
            val itagAudioLocale = itagItem.audioLocale
            if (itagAudioLocale != null) {
                assertAttrEquals(itagAudioLocale.language, element, "lang")
            }
        }
    }

    private fun assertRoleElement(document: Document, itagItem: ItagItem) {
        val element = assertGetElement(document, ROLE, ADAPTATION_SET)

        val expect = when (itagItem.audioTrackType) {
            null, AudioTrackType.ORIGINAL -> "main"
            AudioTrackType.DUBBED -> "dub"
            AudioTrackType.DESCRIPTIVE -> "description"
            else -> "alternate"
        }

        assertAttrEquals(expect, element, "value")
    }

    private fun assertRepresentationElement(document: Document, itagItem: ItagItem) {
        val element = assertGetElement(document, REPRESENTATION, ADAPTATION_SET)

        assertAttrEquals(itagItem.bitrate, element, "bandwidth")
        assertAttrEquals(itagItem.codec, element, "codecs")

        if (itagItem.itagType == ItagItem.ItagType.VIDEO_ONLY ||
            itagItem.itagType == ItagItem.ItagType.VIDEO
        ) {
            assertAttrEquals(itagItem.getFps(), element, "frameRate")
            assertAttrEquals(itagItem.height, element, "height")
            assertAttrEquals(itagItem.width, element, "width")
        }

        assertAttrEquals(itagItem.id, element, "id")
    }

    private fun assertAudioChannelConfigurationElement(document: Document, itagItem: ItagItem) {
        val element = assertGetElement(document, AUDIO_CHANNEL_CONFIGURATION, REPRESENTATION)
        assertAttrEquals(itagItem.audioChannels, element, "value")
    }

    private fun assertSegmentTemplateElement(document: Document) {
        val element = assertGetElement(document, SEGMENT_TEMPLATE, REPRESENTATION)

        val initializationValue = element.getAttribute("initialization")
        assertIsValidUrl(initializationValue)
        assertTrue(initializationValue.endsWith("&sq=0"))

        val mediaValue = element.getAttribute("media")
        assertIsValidUrl(mediaValue)
        assertTrue(mediaValue.endsWith("&sq=\$Number\$"))

        assertEquals("1", element.getAttribute("startNumber"))
    }

    private fun assertSegmentTimelineAndSElements(document: Document) {
        val element = assertGetElement(document, SEGMENT_TIMELINE, SEGMENT_TEMPLATE)
        val childNodes = element.childNodes
        assertGreater(0, childNodes.length.toLong())

        assertAll(
            (0 until childNodes.length)
                .map { childNodes.item(it) as Element }
                .flatMap { sElement ->
                    listOf(
                        Executable { assertEquals("S", sElement.tagName) },
                        Executable { assertGreater(0, sElement.getAttribute("d").toLong()) },
                        Executable {
                            val rValue = sElement.getAttribute("r")
                            // A segment duration can or can't be repeated, so test the next segment
                            // if there is no r attribute
                            if (!isBlank(rValue)) {
                                assertGreater(0, rValue.toLong())
                            }
                        }
                    )
                }
        )
    }

    private fun assertBaseUrlElement(document: Document) {
        val element = assertGetElement(document, BASE_URL, REPRESENTATION)
        assertIsValidUrl(element.textContent)
    }

    private fun assertSegmentBaseElement(document: Document, itagItem: ItagItem) {
        val element = assertGetElement(document, SEGMENT_BASE, REPRESENTATION)
        assertRangeEquals(itagItem.indexStart, itagItem.indexEnd, element, "indexRange")
    }

    private fun assertInitializationElement(document: Document, itagItem: ItagItem) {
        val element = assertGetElement(document, INITIALIZATION, SEGMENT_BASE)
        assertRangeEquals(itagItem.initStart, itagItem.initEnd, element, "range")
    }

    private fun assertAttrEquals(expected: Int, element: Element, attribute: String) {
        val actual = element.getAttribute(attribute).toInt()
        assertAll(
            { assertGreater(0, actual.toLong()) },
            { assertEquals(expected, actual) }
        )
    }

    private fun assertAttrEquals(expected: String?, element: Element, attribute: String) {
        val actual = element.getAttribute(attribute)
        assertAll(
            { assertNotBlank(actual) },
            { assertEquals(expected, actual) }
        )
    }

    private fun assertRangeEquals(expectedStart: Int, expectedEnd: Int, element: Element, attribute: String) {
        val range = element.getAttribute(attribute)
        assertNotBlank(range)
        val rangeParts = range.split("-")
        assertEquals(2, rangeParts.size)

        val actualStart = rangeParts[0].toInt()
        val actualEnd = rangeParts[1].toInt()

        assertAll(
            { assertGreaterOrEqual(0, actualStart.toLong()) },
            { assertEquals(expectedStart, actualStart) },
            { assertGreater(0, actualEnd.toLong()) },
            { assertEquals(expectedEnd, actualEnd) }
        )
    }

    private fun assertGetElement(document: Document, tagName: String, expectedParentTagName: String): Element {
        val element = document.getElementsByTagName(tagName).item(0) as Element?
        assertNotNull(element)
        assertTrue(
            element!!.parentNode.isEqualNode(document.getElementsByTagName(expectedParentTagName).item(0)),
            "Element with tag name \"$tagName\" does not have a parent node" +
                " with tag name \"$expectedParentTagName\""
        )
        return element
    }

    private companion object {
        // Setting a higher number may let Google video servers return 403s
        const val MAX_STREAMS_TO_TEST_PER_METHOD = 3
        const val URL = "https://www.youtube.com/watch?v=DJ8GQUNUXGM"
    }
}

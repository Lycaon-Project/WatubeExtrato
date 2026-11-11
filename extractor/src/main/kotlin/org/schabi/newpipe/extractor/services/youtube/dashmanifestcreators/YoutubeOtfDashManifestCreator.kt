package org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators

import org.schabi.newpipe.extractor.services.youtube.DeliveryType
import org.schabi.newpipe.extractor.services.youtube.ItagItem
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.ALR_YES
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.RN_0
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.SEGMENT_TIMELINE
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.SQ_0
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.buildAndCacheResult
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.checkStreamingUrl
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.generateDocumentAndDoCommonElementsGeneration
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.generateSegmentTemplateElement
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.generateSegmentTimelineElement
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.getInitializationResponse
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.setAttribute
import org.schabi.newpipe.extractor.utils.ManifestCreatorCache
import org.schabi.newpipe.extractor.utils.PrivacyEvents
import org.schabi.newpipe.extractor.utils.Utils
import org.w3c.dom.DOMException
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * Class which generates DASH manifests of YouTube [OTF streams][DeliveryType.OTF].
 */
public object YoutubeOtfDashManifestCreator {

    private const val HTTP_OK = 200
    private const val SEGMENT_DURATIONS_HEADER = "Segment-Durations-Ms: "
    private const val REPEAT_SEPARATOR = "(r="

    /*
     * Bounds of the segment list sent by the server, far above real streams (a few hundred
     * entries of about 5 s): the player expands every repetition, so a tampered list could
     * otherwise make it allocate billions of segments.
     */
    private const val MAX_SEGMENT_ENTRIES = 10_000
    private const val MAX_SEGMENT_DURATION_MS = 3_600_000
    private const val MAX_SEGMENT_REPEAT = 100_000

    /**
     * Cache of DASH manifests generated for OTF streams.
     */
    private val OTF_STREAMS_CACHE = ManifestCreatorCache<String, String>()

    /**
     * Create DASH manifests from a YouTube OTF stream.
     *
     * OTF streams are YouTube-DASH specific streams which work with sequences and without the need
     * to get a manifest (even if one is provided, it is not used by official clients).
     *
     * They can be found only on videos; mostly those with a small amount of views, or ended
     * livestreams which have just been re-encoded as normal videos.
     *
     * This method needs:
     * - the base URL of the stream (which, if you try to access to it, returns HTTP
     *   status code 404 after redirects, and if the URL is valid);
     * - an [ItagItem], which needs to contain the following information:
     *     - its type (see [ItagItem.ItagType]), to identify if the content is
     *       an audio or a video stream;
     *     - its bitrate;
     *     - its mime type;
     *     - its codec(s);
     *     - for an audio stream: its audio channels;
     *     - for a video stream: its width and height.
     * - the duration of the video, which will be used if the duration could not be
     *   parsed from the first sequence of the stream.
     *
     * In order to generate the DASH manifest, this method will:
     * - request the first sequence of the stream (the base URL on which the first
     *   sequence parameter is appended (see [YoutubeDashManifestCreatorsUtils.SQ_0]))
     *   with a `POST` or `GET` request (depending of the client on which the
     *   streaming URL comes from is a mobile one (`POST`) or not (`GET`));
     * - follow its redirection(s), if any;
     * - save the last URL, remove the first sequence parameter;
     * - use the information provided in the [ItagItem] to generate all
     *   elements of the DASH manifest.
     *
     * If the duration cannot be extracted, the `durationSecondsFallback` value will be used
     * as the stream duration.
     *
     * @param otfBaseStreamingUrl     the base URL of the OTF stream, which must not be null
     * @param itagItem                the [ItagItem] corresponding to the stream, which
     * must not be null
     * @param durationSecondsFallback the duration of the video, which will be used if the duration
     * could not be extracted from the first sequence
     * @return the manifest generated into a string
     */
    @JvmStatic
    @Throws(CreationException::class)
    public fun fromOtfStreamingUrl(
        otfBaseStreamingUrl: String,
        itagItem: ItagItem,
        durationSecondsFallback: Long,
    ): String {
        OTF_STREAMS_CACHE[otfBaseStreamingUrl]?.let { return it.second }

        // Try to avoid redirects when streaming the content by saving the last URL we get
        // from video servers.
        val response = getInitializationResponse(otfBaseStreamingUrl, itagItem, DeliveryType.OTF)
        val latestUrl = response.latestUrl()
        // The URL after the redirections followed by the downloader is put in the manifest, so
        // it is checked like the URL requested
        checkStreamingUrl(latestUrl, PrivacyEvents.Event.REDIRECT_BLOCKED)
        val realOtfBaseStreamingUrl = latestUrl.replace(SQ_0, "")
            .replace(RN_0, "").replace(ALR_YES, "")

        val responseCode = response.responseCode()
        if (responseCode != HTTP_OK) {
            throw CreationException("Could not get the initialization URL: response code $responseCode")
        }

        val segmentDuration = getSegmentDurations(response.responseBody())

        val streamDuration = try {
            getStreamDuration(segmentDuration)
        } catch (_: CreationException) {
            durationSecondsFallback * 1000
        }

        val doc = generateDocumentAndDoCommonElementsGeneration(itagItem, streamDuration)

        generateSegmentTemplateElement(doc, realOtfBaseStreamingUrl, DeliveryType.OTF)
        generateSegmentTimelineElement(doc)
        generateSegmentElementsForOtfStreams(segmentDuration, doc)

        return buildAndCacheResult(otfBaseStreamingUrl, doc, OTF_STREAMS_CACHE)
    }

    /**
     * @return the cache of DASH manifests generated for OTF streams
     */
    @JvmStatic
    public val cache: ManifestCreatorCache<String, String>
        get() = OTF_STREAMS_CACHE

    /**
     * Split like Java's `String.split` with a literal separator: trailing empty strings are
     * removed, and an empty string gives a list made of an empty string.
     */
    private fun javaSplit(value: String, separator: String): List<String> =
        if (value.isEmpty()) listOf("") else value.split(separator).dropLastWhile { it.isEmpty() }

    /**
     * Get the segment durations, as `length` or `length(r=repeat_count` strings, from the body
     * of the first sequence of an OTF stream.
     */
    @Throws(CreationException::class)
    private fun getSegmentDurations(responseBody: String): List<String> {
        // Get the lines with the durations and the following
        val durationsAndFollowingLines = javaSplit(responseBody, SEGMENT_DURATIONS_HEADER).getOrNull(1)
            ?: throw CreationException("Could not get segment durations")
        // Remove the other lines
        val durationsLine = javaSplit(durationsAndFollowingLines, "\n").firstOrNull()
            ?: throw CreationException("Could not get segment durations")
        // Get all durations and repetitions which are separated by a comma
        val segmentsAndDurationsResponseSplit = javaSplit(durationsLine, ",")
        if (segmentsAndDurationsResponseSplit.isEmpty() ||
            segmentsAndDurationsResponseSplit.size > MAX_SEGMENT_ENTRIES
        ) {
            throw CreationException("Could not get segment durations")
        }
        return if (Utils.isBlank(segmentsAndDurationsResponseSplit.last())) {
            segmentsAndDurationsResponseSplit.dropLast(1)
        } else {
            segmentsAndDurationsResponseSplit
        }
    }

    /**
     * Generate segment elements for OTF streams.
     *
     * By parsing by the first media sequence, we know how many durations and repetitions there are
     * so we just have to loop into segment durations to generate the following elements for each
     * duration repeated X times:
     *
     * `<S d="segmentDuration" r="durationRepetition" />`
     *
     * If there is no repetition of the duration between two segments, the `r` attribute is
     * not added to the `S` element, as it is not needed.
     *
     * These elements will be appended as children of the `<SegmentTimeline>` element, which
     * needs to be generated before these elements with
     * [YoutubeDashManifestCreatorsUtils.generateSegmentTimelineElement].
     *
     * @param segmentDurations the sequences "length" or "length(r=repeat_count" extracted with the
     * regular expressions
     * @param doc              the [Document] on which the `<S>` elements will be
     * appended
     */
    @Throws(CreationException::class)
    private fun generateSegmentElementsForOtfStreams(segmentDurations: List<String>, doc: Document) {
        try {
            val segmentTimelineElement = doc.getElementsByTagName(SEGMENT_TIMELINE).item(0) as Element

            for (segmentDuration in segmentDurations) {
                val sElement = doc.createElement("S")

                val segmentLengthRepeat = javaSplit(segmentDuration, REPEAT_SEPARATOR)
                // the length must be a positive int, the repetitions a bounded count
                if (segmentLengthRepeat[0].toInt() !in 1..MAX_SEGMENT_DURATION_MS) {
                    throw CreationException("Invalid segment duration")
                }

                // There are repetitions of a segment duration in other segments
                if (segmentLengthRepeat.size > 1) {
                    val segmentRepeatCount = Utils.removeNonDigitCharacters(segmentLengthRepeat[1]).toInt()
                    if (segmentRepeatCount > MAX_SEGMENT_REPEAT) {
                        throw CreationException("Invalid segment repetition count")
                    }
                    setAttribute(sElement, doc, "r", segmentRepeatCount.toString())
                }
                setAttribute(sElement, doc, "d", segmentLengthRepeat[0])

                segmentTimelineElement.appendChild(sElement)
            }
        } catch (e: DOMException) {
            throw CreationException.couldNotAddElement("segment (S)", e)
        } catch (e: IllegalStateException) {
            throw CreationException.couldNotAddElement("segment (S)", e)
        } catch (e: IndexOutOfBoundsException) {
            throw CreationException.couldNotAddElement("segment (S)", e)
        } catch (e: NumberFormatException) {
            throw CreationException.couldNotAddElement("segment (S)", e)
        }
    }

    /**
     * Get the duration of an OTF stream.
     *
     * The duration of OTF streams is not returned into the player response and needs to be
     * calculated by adding the duration of each segment.
     *
     * A segment list which cannot be parsed, or whose total duration does not fit in a [Long],
     * is reported with a [CreationException], so that the fallback duration is used.
     *
     * @param segmentDuration the segment duration object extracted from the initialization
     * sequence of the stream
     * @return the duration of the OTF stream, in milliseconds
     */
    @Throws(CreationException::class)
    private fun getStreamDuration(segmentDuration: List<String>): Long {
        try {
            var streamLengthMs = 0L

            for (segDuration in segmentDuration) {
                val segmentLengthRepeat = javaSplit(segDuration, REPEAT_SEPARATOR)
                var segmentRepeatCount = 0L

                // There are repetitions of a segment duration in other segments
                if (segmentLengthRepeat.size > 1) {
                    segmentRepeatCount = Utils.removeNonDigitCharacters(segmentLengthRepeat[1]).toLong()
                }

                val segmentLength = segmentLengthRepeat[0].toInt().toLong()
                streamLengthMs = Math.addExact(
                    streamLengthMs,
                    Math.addExact(segmentLength, Math.multiplyExact(segmentRepeatCount, segmentLength)),
                )
            }

            return streamLengthMs
        } catch (e: NumberFormatException) {
            throw CreationException("Could not get stream length from sequences list", e)
        } catch (e: IndexOutOfBoundsException) {
            throw CreationException("Could not get stream length from sequences list", e)
        } catch (e: ArithmeticException) {
            throw CreationException("Could not get stream length from sequences list", e)
        }
    }
}

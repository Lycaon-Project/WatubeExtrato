package org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators

import org.schabi.newpipe.extractor.services.youtube.DeliveryType
import org.schabi.newpipe.extractor.services.youtube.ItagItem
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.BASE_URL
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.INITIALIZATION
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.MPD
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.REPRESENTATION
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.SEGMENT_BASE
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.buildAndCacheResult
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.checkStreamingUrl
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.generateDocumentAndDoCommonElementsGeneration
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.setAttribute
import org.schabi.newpipe.extractor.utils.ManifestCreatorCache
import org.schabi.newpipe.extractor.utils.PrivacyEvents
import org.w3c.dom.DOMException
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * Class which generates DASH manifests of [YouTube progressive][DeliveryType.PROGRESSIVE]
 * streams.
 */
public object YoutubeProgressiveDashManifestCreator {

    /**
     * Cache of DASH manifests generated for progressive streams.
     */
    private val PROGRESSIVE_STREAMS_CACHE = ManifestCreatorCache<String, String>()

    /**
     * Create DASH manifests from a YouTube progressive stream.
     *
     * Progressive streams are YouTube DASH streams which work with range requests and without the
     * need to get a manifest.
     *
     * They can be found on all videos, and for all streams for most of videos which come from a
     * YouTube partner, and on videos with a large number of views.
     *
     * This method needs:
     * - the base URL of the stream (which, if you try to access to it, returns the whole
     *   stream, after redirects, and if the URL is valid);
     * - an [ItagItem], which needs to contain the following information:
     *     - its type (see [ItagItem.ItagType]), to identify if the content is
     *       an audio or a video stream;
     *     - its bitrate;
     *     - its mime type;
     *     - its codec(s);
     *     - for an audio stream: its audio channels;
     *     - for a video stream: its width and height.
     * - the duration of the video (parameter `durationSecondsFallback`), which
     *   will be used as the stream duration if the duration could not be parsed from the
     *   [ItagItem].
     *
     * @param progressiveStreamingBaseUrl the base URL of the progressive stream, which must not be
     * null
     * @param itagItem                    the [ItagItem] corresponding to the stream, which
     * must not be null
     * @param durationSecondsFallback     the duration of the progressive stream which will be used
     * if the duration could not be extracted from the
     * [ItagItem]
     * @return the manifest generated into a string
     */
    @JvmStatic
    @Throws(CreationException::class)
    public fun fromProgressiveStreamingUrl(
        progressiveStreamingBaseUrl: String,
        itagItem: ItagItem,
        durationSecondsFallback: Long,
    ): String {
        PROGRESSIVE_STREAMS_CACHE[progressiveStreamingBaseUrl]?.let { return it.second }
        // never fetched here, but written into the manifest the player will request
        checkStreamingUrl(progressiveStreamingBaseUrl, PrivacyEvents.Event.STREAM_URL_REFUSED)

        val itagItemDuration = itagItem.approxDurationMs
        val streamDuration = when {
            itagItemDuration != -1L -> itagItemDuration
            durationSecondsFallback > 0 -> durationSecondsFallback * 1000
            else -> throw CreationException.couldNotAddElement(
                MPD,
                "the duration of the stream could not be determined and durationSecondsFallback is <= 0",
            )
        }

        val doc = generateDocumentAndDoCommonElementsGeneration(itagItem, streamDuration)

        generateBaseUrlElement(doc, progressiveStreamingBaseUrl)
        generateSegmentBaseElement(doc, itagItem)
        generateInitializationElement(doc, itagItem)

        return buildAndCacheResult(progressiveStreamingBaseUrl, doc, PROGRESSIVE_STREAMS_CACHE)
    }

    /**
     * @return the cache of DASH manifests generated for progressive streams
     */
    @JvmStatic
    public val cache: ManifestCreatorCache<String, String>
        get() = PROGRESSIVE_STREAMS_CACHE

    /**
     * Generate the `<BaseURL>` element, appended as a child of the
     * `<Representation>` element.
     *
     * The `<Representation>` element needs to be generated before this element with
     * [YoutubeDashManifestCreatorsUtils.generateRepresentationElement]).
     *
     * @param doc the [Document] on which the `<BaseURL>` element will be appended
     * @param baseUrl  the base URL of the stream, which must not be null and will be set as the
     * content of the `<BaseURL>` element
     */
    @Throws(CreationException::class)
    private fun generateBaseUrlElement(doc: Document, baseUrl: String) {
        try {
            val representationElement = doc.getElementsByTagName(REPRESENTATION).item(0) as Element
            val baseURLElement = doc.createElement(BASE_URL)
            baseURLElement.textContent = baseUrl
            representationElement.appendChild(baseURLElement)
        } catch (e: DOMException) {
            throw CreationException.couldNotAddElement(BASE_URL, e)
        }
    }

    /**
     * Generate the `<SegmentBase>` element, appended as a child of the
     * `<Representation>` element.
     *
     * It generates the following element:
     * `<SegmentBase indexRange="indexStart-indexEnd" />`
     * (where `indexStart` and `indexEnd` are gotten from the [ItagItem] passed
     * as the second parameter)
     *
     * The `<Representation>` element needs to be generated before this element with
     * [YoutubeDashManifestCreatorsUtils.generateRepresentationElement]),
     * and the `BaseURL` element with [generateBaseUrlElement]
     * should be generated too.
     *
     * @param doc the [Document] on which the `<SegmentBase>` element will be appended
     * @param itagItem the [ItagItem] to use, which must not be null
     */
    @Throws(CreationException::class)
    private fun generateSegmentBaseElement(doc: Document, itagItem: ItagItem) {
        try {
            val representationElement = doc.getElementsByTagName(REPRESENTATION).item(0) as Element
            val segmentBaseElement = doc.createElement(SEGMENT_BASE)

            val range = "${itagItem.indexStart}-${itagItem.indexEnd}"
            if (itagItem.indexStart < 0 || itagItem.indexEnd < 0) {
                throw CreationException.couldNotAddElement(
                    SEGMENT_BASE, "ItagItem's indexStart or indexEnd are < 0: $range",
                )
            }
            setAttribute(segmentBaseElement, doc, "indexRange", range)

            representationElement.appendChild(segmentBaseElement)
        } catch (e: DOMException) {
            throw CreationException.couldNotAddElement(SEGMENT_BASE, e)
        }
    }

    /**
     * Generate the `<Initialization>` element, appended as a child of the
     * `<SegmentBase>` element.
     *
     * It generates the following element:
     * `<Initialization range="initStart-initEnd"/>`
     * (where `indexStart` and `indexEnd` are gotten from the [ItagItem] passed
     * as the second parameter)
     *
     * The `<SegmentBase>` element needs to be generated before this element with
     * [generateSegmentBaseElement]).
     *
     * @param doc the [Document] on which the `<Initialization>` element will be
     * appended
     * @param itagItem the [ItagItem] to use, which must not be null
     */
    @Throws(CreationException::class)
    private fun generateInitializationElement(doc: Document, itagItem: ItagItem) {
        try {
            val segmentBaseElement = doc.getElementsByTagName(SEGMENT_BASE).item(0) as Element
            val initializationElement = doc.createElement(INITIALIZATION)

            val range = "${itagItem.initStart}-${itagItem.initEnd}"
            if (itagItem.initStart < 0 || itagItem.initEnd < 0) {
                throw CreationException.couldNotAddElement(
                    INITIALIZATION, "ItagItem's initStart and/or initEnd are/is < 0: $range",
                )
            }
            setAttribute(initializationElement, doc, "range", range)

            segmentBaseElement.appendChild(initializationElement)
        } catch (e: DOMException) {
            throw CreationException.couldNotAddElement(INITIALIZATION, e)
        }
    }
}

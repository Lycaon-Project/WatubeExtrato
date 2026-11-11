/*
 * Created by Christian Schabesberger on 26.08.15.
 *
 * Copyright (C) 2016 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * StreamInfo.java is part of NewPipe Extractor.
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

package org.schabi.newpipe.extractor.stream

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.Info
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.MetaInfo
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.exceptions.ContentNotSupportedException
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.utils.ExtractorHelper
import org.schabi.newpipe.extractor.utils.ExtractorLogger
import java.io.IOException
import java.util.Locale

/**
 * Info object for opened contents, i.e. the content ready to play.
 */
public open class StreamInfo(
    serviceId: Int,
    url: String,
    originalUrl: String,
    /** The stream type */
    public var streamType: StreamType,
    id: String,
    name: String,
    public var ageLimit: Int,
) : Info(serviceId, id, url, originalUrl, name) {

    public class StreamExtractException internal constructor(message: String) :
        ExtractionException(message)

    init {
        // Neither the URL nor the title is logged: they tell what the user watches
        ExtractorLogger.d(TAG, "Created {} for service {}", streamType, serviceId)
    }

    /** The thumbnails */
    public var thumbnails: List<Image> = emptyList()
    public var textualUploadDate: String? = null
    public var uploadDate: DateWrapper? = null

    /** The duration in seconds */
    public var duration: Long = -1

    /** The description, [Description.EMPTY_DESCRIPTION] (instead of `null`) when unknown. */
    public var description: Description = Description.EMPTY_DESCRIPTION
    public var viewCount: Long = -1

    /** The number of likes or -1 if this information is not available */
    public var likeCount: Long = -1

    /** The number of dislikes or -1 if this information is not available */
    public var dislikeCount: Long = -1

    public var uploaderName: String = ""
    public var uploaderUrl: String = ""
    public var uploaderAvatars: List<Image> = emptyList()
    public var isUploaderVerified: Boolean = false
    public var uploaderSubscriberCount: Long = -1

    public var subChannelName: String = ""
    public var subChannelUrl: String = ""
    public var subChannelAvatars: List<Image> = emptyList()

    public var videoStreams: List<VideoStream> = emptyList()
    public var audioStreams: List<AudioStream> = emptyList()
    public var videoOnlyStreams: List<VideoStream> = emptyList()
    public var serverAbrStreamingUrl: String? = ""
    public var ustreamerConfig: String? = ""

    public var dashMpdUrl: String = ""
    public var hlsUrl: String = ""
    public var relatedItems: List<InfoItem> = emptyList()

    public var startPosition: Long = 0
    public var subtitles: List<SubtitlesStream> = emptyList()

    public var host: String = ""

    /** The privacy, `null` when it could not be extracted. */
    public var privacy: StreamExtractor.Privacy? = null
    public var category: String = ""
    public var licence: String = ""
    public var supportInfo: String = ""
    public var languageInfo: Locale? = null
    public var tags: List<String> = emptyList()
    public var streamSegments: List<StreamSegment> = emptyList()
    public var metaInfo: List<MetaInfo> = emptyList()
    public var isShortFormContent: Boolean = false
    public var contentAvailability: ContentAvailability = ContentAvailability.AVAILABLE

    /**
     * Preview frames, e.g. for the storyboard / seekbar thumbnail preview
     */
    public var previewFrames: List<Frameset> = emptyList()

    // no URL, ID or title: what the user watches must not end up in logs and crash reports
    override fun toString(): String = TAG + "[" +
        "serviceId=" + serviceId +
        ", streamType=" + streamType +
        ", ageLimit=" + ageLimit +
        ']'

    public companion object {
        private const val serialVersionUID: Long = 1L

        private const val TAG = "StreamInfo"

        @JvmStatic
        @Throws(IOException::class, ExtractionException::class)
        public fun getInfo(url: String): StreamInfo {
            ExtractorLogger.d(TAG, "getInfo(url)")
            return getInfo(NewPipe.getServiceByUrl(url), url)
        }

        @JvmStatic
        @Throws(IOException::class, ExtractionException::class)
        public fun getInfo(service: StreamingService, url: String): StreamInfo {
            ExtractorLogger.d(TAG, "getInfo(service {}, url)", service.serviceId)
            return getInfo(service.getStreamExtractor(url))
        }

        @JvmStatic
        @Throws(ExtractionException::class, IOException::class)
        public fun getInfo(extractor: StreamExtractor): StreamInfo {
            ExtractorLogger.d(TAG, "getInfo({extractor})", extractor)
            extractor.fetchPage()
            try {
                val streamInfo = extractImportantData(extractor)
                extractStreams(streamInfo, extractor)
                extractOptionalData(streamInfo, extractor)
                return streamInfo
            } catch (e: ExtractionException) {
                // Currently, YouTube does not distinguish between age restricted videos and videos
                // blocked by country. This means that during the initialisation of the extractor,
                // the extractor will assume that a video is age restricted while in reality it is
                // blocked by country.
                //
                // We will now detect whether the video is blocked by country or not.

                val errorMessage = extractor.errorMessage
                if (errorMessage.isNullOrEmpty()) {
                    throw e
                } else {
                    throw ContentNotAvailableException(errorMessage, e)
                }
            }
        }

        @Throws(ExtractionException::class)
        private fun extractImportantData(extractor: StreamExtractor): StreamInfo {
            // Important data, without it the content can't be displayed.
            // If one of these is not available, the frontend will receive an exception directly.

            // Declared nullable on purpose: a Java extractor can still break its contract
            val url: String? = extractor.url
            val streamType = extractor.streamType
            val id: String? = extractor.id
            val name: String? = extractor.name
            val ageLimit = extractor.ageLimit

            if (streamType == StreamType.NONE ||
                url.isNullOrEmpty() ||
                id.isNullOrEmpty() ||
                name == null || // but it can be empty of course
                ageLimit == -1
            ) {
                throw ExtractionException("Some important stream information was not given.")
            }

            return StreamInfo(
                extractor.serviceId, url, extractor.originalUrl,
                streamType, id, name, ageLimit,
            )
        }

        @Throws(ExtractionException::class)
        private fun extractStreams(streamInfo: StreamInfo, extractor: StreamExtractor) {
            /* ---- Stream extraction goes here ---- */
            // At least one type of stream has to be available, otherwise an exception will be
            // thrown directly into the frontend.

            try {
                streamInfo.dashMpdUrl = extractor.dashMpdUrl
            } catch (e: Exception) {
                streamInfo.addError(ExtractionException("Couldn't get DASH manifest", e))
            }

            try {
                streamInfo.hlsUrl = extractor.hlsUrl
            } catch (e: Exception) {
                streamInfo.addError(ExtractionException("Couldn't get HLS manifest", e))
            }

            // A null list from an extractor counts as no stream, instead of failing below
            try {
                streamInfo.audioStreams = extractor.audioStreams.orEmpty()
            } catch (e: ContentNotSupportedException) {
                throw e
            } catch (e: Exception) {
                streamInfo.addError(ExtractionException("Couldn't get audio streams", e))
            }

            try {
                streamInfo.videoStreams = extractor.videoStreams.orEmpty()
            } catch (e: Exception) {
                streamInfo.addError(ExtractionException("Couldn't get video streams", e))
            }

            try {
                streamInfo.videoOnlyStreams = extractor.videoOnlyStreams.orEmpty()
            } catch (e: Exception) {
                streamInfo.addError(ExtractionException("Couldn't get video only streams", e))
            }

            optional(streamInfo) { serverAbrStreamingUrl = extractor.serverAbrStreamingUrl }
            optional(streamInfo) { ustreamerConfig = extractor.ustreamerConfig }

            // Either audio or video has to be available, otherwise we didn't get a stream (since
            // videoOnly are optional, they don't count).
            // Allow DASH and HLS manifests to be only available, as they can be the only source
            // available for livestreams
            if (streamInfo.serverAbrStreamingUrl.isNullOrEmpty() &&
                streamInfo.ustreamerConfig.isNullOrEmpty() &&
                streamInfo.videoStreams.isEmpty() && streamInfo.audioStreams.isEmpty() &&
                streamInfo.dashMpdUrl.isEmpty() && streamInfo.hlsUrl.isEmpty()
            ) {
                throw StreamExtractException(
                    "Could not get any stream. See error variable to get further details.",
                )
            }
        }

        private fun extractOptionalData(streamInfo: StreamInfo, extractor: StreamExtractor) {
            /* ---- Optional data goes here: ---- */
            // If one of these fails, the frontend needs to handle that they are not available.
            // Exceptions are therefore not thrown into the frontend, but stored into the error
            // list, so the frontend can afterwards check where errors happened.

            optional(streamInfo) { thumbnails = extractor.thumbnails }
            optional(streamInfo) { duration = extractor.length }
            optional(streamInfo) { uploaderName = extractor.uploaderName }
            optional(streamInfo) { uploaderUrl = extractor.uploaderUrl }
            optional(streamInfo) { uploaderAvatars = extractor.uploaderAvatars }
            optional(streamInfo) { isUploaderVerified = extractor.isUploaderVerified }
            optional(streamInfo) { uploaderSubscriberCount = extractor.uploaderSubscriberCount }

            optional(streamInfo) { subChannelName = extractor.subChannelName }
            optional(streamInfo) { subChannelUrl = extractor.subChannelUrl }
            optional(streamInfo) { subChannelAvatars = extractor.subChannelAvatars }

            optional(streamInfo) { description = extractor.description }
            optional(streamInfo) { viewCount = extractor.viewCount }
            optional(streamInfo) { textualUploadDate = extractor.textualUploadDate }
            optional(streamInfo) { uploadDate = extractor.uploadDate }
            optional(streamInfo) { startPosition = extractor.timeStamp }
            optional(streamInfo) { likeCount = extractor.likeCount }
            optional(streamInfo) { dislikeCount = extractor.dislikeCount }
            optional(streamInfo) { subtitles = extractor.subtitlesDefault }

            // Additional info
            optional(streamInfo) { host = extractor.host }
            optional(streamInfo) { privacy = extractor.privacy }
            optional(streamInfo) { category = extractor.category }
            optional(streamInfo) { licence = extractor.licence }
            optional(streamInfo) { languageInfo = extractor.languageInfo }
            optional(streamInfo) { tags = extractor.tags }
            optional(streamInfo) { supportInfo = extractor.supportInfo }
            optional(streamInfo) { streamSegments = extractor.streamSegments }
            optional(streamInfo) { metaInfo = extractor.metaInfo }
            optional(streamInfo) { previewFrames = extractor.frames }
            optional(streamInfo) { isShortFormContent = extractor.isShortFormContent }
            optional(streamInfo) { contentAvailability = extractor.contentAvailability }

            streamInfo.relatedItems = ExtractorHelper.getRelatedItemsOrLogError(streamInfo, extractor)
        }

        /** Runs one optional extraction: a failure is stored in the errors of [streamInfo]. */
        private inline fun optional(streamInfo: StreamInfo, block: StreamInfo.() -> Unit) {
            try {
                streamInfo.block()
            } catch (e: Exception) {
                streamInfo.addError(e)
            }
        }
    }
}

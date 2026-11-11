/*
 * Created by Christian Schabesberger on 04.03.16.
 *
 * Copyright (C) 2016 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * VideoStream.java is part of NewPipe Extractor.
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
package org.schabi.newpipe.extractor.stream

import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.services.youtube.ItagItem

/**
 * A video stream, with or without audio, created with a [Builder].
 *
 * @param id             the identifier which uniquely identifies the stream, e.g. for YouTube
 *                       this would be the itag
 * @param content        the content or the URL of the stream, depending on whether isUrl is
 *                       true
 * @param isUrl          whether content is the URL or the actual content of e.g. a DASH
 *                       manifest
 * @param format         the [MediaFormat] used by the stream, which can be null
 * @param deliveryMethod the [DeliveryMethod] of the stream
 * @param resolution     the resolution of the stream
 * @param isVideoOnly    whether the stream is video-only
 * @param manifestUrl    the URL of the manifest this stream comes from (if applicable,
 *                       otherwise null)
 * @param itagItem       the [ItagItem] corresponding to the stream, which can be null
 */
public class VideoStream private constructor(
    id: String,
    content: String,
    isUrl: Boolean,
    format: MediaFormat?,
    deliveryMethod: DeliveryMethod,
    resolution: String,
    isVideoOnly: Boolean,
    manifestUrl: String?,
    override val itagItem: ItagItem?,
) : Stream(id, content, isUrl, format, deliveryMethod, manifestUrl) {

    /**
     * Public field kept for Java callers, next to [getResolution].
     */
    @Deprecated("Use getResolution() instead.")
    @JvmField
    public val resolution: String = resolution

    /**
     * Public field kept for Java callers, next to [isVideoOnly].
     */
    @Deprecated("Use isVideoOnly() instead.")
    @JvmField
    public val isVideoOnly: Boolean = isVideoOnly

    // Fields for DASH, all taken from the ItagItem when there is one

    /**
     * The itag identifier of the stream.
     *
     * Always equals to [ITAG_NOT_AVAILABLE_OR_NOT_APPLICABLE] for other streams than the
     * ones of the YouTube service.
     */
    public val itag: Int = itagItem?.id ?: ITAG_NOT_AVAILABLE_OR_NOT_APPLICABLE

    /** The bitrate set from the [ItagItem] passed in the constructor of the stream. */
    public val bitrate: Int = itagItem?.bitrate ?: 0

    /** The initialization start value set from the [ItagItem] of the stream. */
    public val initStart: Int = itagItem?.initStart ?: 0

    /** The initialization end value set from the [ItagItem] of the stream. */
    public val initEnd: Int = itagItem?.initEnd ?: 0

    /** The index start value set from the [ItagItem] of the stream. */
    public val indexStart: Int = itagItem?.indexStart ?: 0

    /** The index end value set from the [ItagItem] of the stream. */
    public val indexEnd: Int = itagItem?.indexEnd ?: 0

    /** The width set from the [ItagItem] of the stream. */
    public val width: Int = itagItem?.width ?: 0

    /** The height set from the [ItagItem] of the stream. */
    public val height: Int = itagItem?.height ?: 0

    /**
     * The frames per second set from the [ItagItem] of the stream (its deprecated `fps` field
     * holds exactly what `getFps()` returns).
     */
    @Suppress("DEPRECATION")
    public val fps: Int = itagItem?.fps ?: 0

    /** The quality label set from the [ItagItem] of the stream. */
    public val quality: String? = itagItem?.quality

    /** The codec set from the [ItagItem] of the stream. */
    public val codec: String? = itagItem?.codec

    /**
     * Class to build [VideoStream] objects.
     *
     * Create a new [Builder] instance with its default values.
     */
    public class Builder {
        private var id: String? = null
        private var content: String? = null
        private var isUrl = false
        private var deliveryMethod: DeliveryMethod? = DeliveryMethod.PROGRESSIVE_HTTP
        private var mediaFormat: MediaFormat? = null
        private var manifestUrl: String? = null

        // Nullable instead of the primitive type, to check that the setter has been called
        private var isVideoOnly: Boolean? = null
        private var resolution: String? = null
        private var itagItem: ItagItem? = null

        /**
         * Set the identifier of the [VideoStream].
         *
         * It must not be null, and should be non empty.
         *
         * If you are not able to get an identifier, use the static constant
         * [ID_UNKNOWN][Stream.ID_UNKNOWN] of the [Stream] class.
         *
         * A `null` is still accepted here, so that [build] reports it with a clear message.
         *
         * @param id the identifier of the [VideoStream], which must not be null
         * @return this [Builder] instance
         */
        public fun setId(id: String?): Builder = apply { this.id = id }

        /**
         * Set the content of the [VideoStream].
         *
         * It must not be null, and should be non empty.
         *
         * @param content the content of the [VideoStream]
         * @param isUrl   whether the content is a URL
         * @return this [Builder] instance
         */
        public fun setContent(content: String?, isUrl: Boolean): Builder = apply {
            this.content = content
            this.isUrl = isUrl
        }

        /**
         * Set the [MediaFormat] used by the [VideoStream].
         *
         * It should be one of the video [MediaFormat]s ([MPEG_4][MediaFormat.MPEG_4],
         * [v3GPP][MediaFormat.v3GPP], or [WEBM][MediaFormat.WEBM]) but can be `null` if the
         * media format could not be determined.
         *
         * The default value is `null`.
         *
         * @param mediaFormat the [MediaFormat] of the [VideoStream], which can be null
         * @return this [Builder] instance
         */
        public fun setMediaFormat(mediaFormat: MediaFormat?): Builder =
            apply { this.mediaFormat = mediaFormat }

        /**
         * Set the [DeliveryMethod] of the [VideoStream].
         *
         * It must not be null.
         *
         * The default delivery method is [DeliveryMethod.PROGRESSIVE_HTTP].
         *
         * @param deliveryMethod the [DeliveryMethod] of the [VideoStream], which must
         * not be null
         * @return this [Builder] instance
         */
        public fun setDeliveryMethod(deliveryMethod: DeliveryMethod?): Builder =
            apply { this.deliveryMethod = deliveryMethod }

        /**
         * Sets the URL of the manifest this stream comes from (if applicable, otherwise null).
         *
         * @param manifestUrl the URL of the manifest this stream comes from or `null`
         * @return this [Builder] instance
         */
        public fun setManifestUrl(manifestUrl: String?): Builder =
            apply { this.manifestUrl = manifestUrl }

        /**
         * Set whether the [VideoStream] is video-only.
         *
         * This property must be set before building the [VideoStream].
         *
         * @param isVideoOnly whether the [VideoStream] is video-only
         * @return this [Builder] instance
         */
        public fun setIsVideoOnly(isVideoOnly: Boolean): Builder =
            apply { this.isVideoOnly = isVideoOnly }

        /**
         * Set the resolution of the [VideoStream].
         *
         * This resolution can be used by clients to know the quality of the video stream.
         *
         * If you are not able to know the resolution, you should use [RESOLUTION_UNKNOWN]
         * as the resolution of the video stream.
         *
         * It must be set before building the builder and not null.
         *
         * @param resolution the resolution of the [VideoStream]
         * @return this [Builder] instance
         */
        public fun setResolution(resolution: String?): Builder =
            apply { this.resolution = resolution }

        /**
         * Set the [ItagItem] corresponding to the [VideoStream].
         *
         * [ItagItem]s are YouTube specific objects, so they are only known for this service
         * and can be null.
         *
         * The default value is `null`.
         *
         * @param itagItem the [ItagItem] of the [VideoStream], which can be null
         * @return this [Builder] instance
         */
        public fun setItagItem(itagItem: ItagItem?): Builder = apply { this.itagItem = itagItem }

        /**
         * Build a [VideoStream] using the builder's current values.
         *
         * The identifier, the content (and so the `isUrl` boolean), the `isVideoOnly`
         * and the `resolution` properties must have been set.
         *
         * The error messages never quote the content, which is usually a stream URL.
         *
         * @return a new [VideoStream] using the builder's current values
         * @throws IllegalStateException if `id`, `content` (and so `isUrl`),
         * `deliveryMethod`, `isVideoOnly` or `resolution` have been not set, or
         * have been set as `null`
         */
        public fun build(): VideoStream {
            val id = checkNotNull(id) {
                "The identifier of the video stream has been not set or is null. If you " +
                    "are not able to get an identifier, use the static constant " +
                    "ID_UNKNOWN of the Stream class."
            }
            val content = checkNotNull(content) {
                "The content of the video stream has been not set " +
                    "or is null. Please specify a non-null one with setContent."
            }
            val deliveryMethod = checkNotNull(deliveryMethod) {
                "The delivery method of the video stream has been set as null, which is " +
                    "not allowed. Pass a valid one instead with setDeliveryMethod."
            }
            val isVideoOnly = checkNotNull(isVideoOnly) {
                "The video stream has been not set as a " +
                    "video-only stream or as a video stream with embedded audio. Please " +
                    "specify this information with setIsVideoOnly."
            }
            val resolution = checkNotNull(resolution) {
                "The resolution of the video stream has been not set. Please specify it " +
                    "with setResolution (use an empty string if you are not able to " +
                    "get it)."
            }
            return VideoStream(
                id, content, isUrl, mediaFormat, deliveryMethod, resolution,
                isVideoOnly, manifestUrl, itagItem,
            )
        }
    }

    /**
     * The video resolution.
     *
     * It can be unknown for some streams, like for HLS master playlists. In this case,
     * [RESOLUTION_UNKNOWN] is returned by this method.
     *
     * @return the video resolution or [RESOLUTION_UNKNOWN]
     */
    @Suppress("DEPRECATION")
    public fun getResolution(): String = resolution

    /**
     * Return whether the stream is video-only.
     *
     * Video-only streams have no audio.
     *
     * @return `true` if this stream is video-only, `false` otherwise
     */
    @Suppress("DEPRECATION")
    public fun isVideoOnly(): Boolean = isVideoOnly

    /**
     * Whether the stats of [other] equal this stream's, including the resolution and whether
     * both are video-only.
     */
    override fun equalStats(other: Stream?): Boolean = super.equalStats(other) &&
        other is VideoStream &&
        getResolution() == other.getResolution() &&
        isVideoOnly() == other.isVideoOnly()

    public companion object {
        private const val serialVersionUID: Long = 1L

        public const val RESOLUTION_UNKNOWN: String = ""
    }
}

/*
 * Created by Christian Schabesberger on 04.03.16.
 *
 * Copyright (C) 2016 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * AudioStream.java is part of NewPipe Extractor.
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
import java.util.Locale

/**
 * An audio stream, created with a [Builder].
 */
public class AudioStream private constructor(
    id: String,
    content: String,
    isUrl: Boolean,
    format: MediaFormat?,
    deliveryMethod: DeliveryMethod,
    manifestUrl: String?,
    /**
     * The average bitrate of the stream, or [UNKNOWN_BITRATE] if it is unknown.
     */
    public val averageBitrate: Int,
    /** The id of the audio track. */
    public val audioTrackId: String?,
    /**
     * The name of the audio track, which may be `null` if this information is not
     * provided by the service.
     */
    public val audioTrackName: String?,
    /**
     * The [Locale] of the audio representing the language of the stream, which is
     * `null` if the audio language of this stream is not known.
     */
    public val audioLocale: Locale?,
    /**
     * The [AudioTrackType] of the stream, which is `null` if the track type is not known.
     */
    public val audioTrackType: AudioTrackType?,
    /** The [ItagItem] of this stream, only known for YouTube. */
    override val itagItem: ItagItem?,
) : Stream(id, content, isUrl, format, deliveryMethod, manifestUrl) {

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

    /** The quality label set from the [ItagItem] of the stream. */
    public val quality: String? = itagItem?.quality

    /** The codec set from the [ItagItem] of the stream. */
    public val codec: String? = itagItem?.codec

    /**
     * Class to build [AudioStream] objects.
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
        private var averageBitrate = UNKNOWN_BITRATE
        private var audioTrackId: String? = null
        private var audioTrackName: String? = null
        private var audioLocale: Locale? = null
        private var audioTrackType: AudioTrackType? = null
        private var itagItem: ItagItem? = null

        /**
         * Set the identifier of the [AudioStream] which uniquely identifies the stream,
         * e.g. for YouTube this would be the itag
         *
         * It **must not be null** and should be non empty.
         *
         * If you are not able to get an identifier, use the static constant
         * [ID_UNKNOWN][Stream.ID_UNKNOWN] of the [Stream] class.
         *
         * A `null` is still accepted here, so that [build] reports it with a clear message.
         *
         * @param id the identifier of the [AudioStream], which must not be null
         * @return this [Builder] instance
         */
        public fun setId(id: String?): Builder = apply { this.id = id }

        /**
         * Set the content or the URL of the [AudioStream], depending on whether isUrl is
         * true
         *
         * It must not be null, and should be non empty.
         *
         * @param content the content of the [AudioStream]
         * @param isUrl   whether content is the URL or the actual content of e.g. a DASH manifest
         * @return this [Builder] instance
         */
        public fun setContent(content: String?, isUrl: Boolean): Builder = apply {
            this.content = content
            this.isUrl = isUrl
        }

        /**
         * Set the [MediaFormat] used by the [AudioStream], which can be null
         *
         * It should be one of the audio [MediaFormat]s ([M4A][MediaFormat.M4A],
         * [WEBMA][MediaFormat.WEBMA], [MP3][MediaFormat.MP3], [OPUS][MediaFormat.OPUS],
         * [OGG][MediaFormat.OGG], or [WEBMA_OPUS][MediaFormat.WEBMA_OPUS]) but
         * can be `null` if the media format could not be determined.
         *
         * The default value is `null`.
         *
         * @param mediaFormat the [MediaFormat] of the [AudioStream], which can be null
         * @return this [Builder] instance
         */
        public fun setMediaFormat(mediaFormat: MediaFormat?): Builder =
            apply { this.mediaFormat = mediaFormat }

        /**
         * Set the [DeliveryMethod] of the [AudioStream].
         *
         * It must not be null.
         *
         * The default delivery method is [DeliveryMethod.PROGRESSIVE_HTTP].
         *
         * @param deliveryMethod the [DeliveryMethod] of the [AudioStream], which must
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
         * Set the average bitrate of the [AudioStream].
         *
         * The default value is [UNKNOWN_BITRATE].
         *
         * @param averageBitrate the average bitrate of the [AudioStream], which should
         * positive
         * @return this [Builder] instance
         */
        public fun setAverageBitrate(averageBitrate: Int): Builder =
            apply { this.averageBitrate = averageBitrate }

        /**
         * Set the audio track id of the [AudioStream].
         *
         * The default value is `null`.
         *
         * @param audioTrackId the audio track id of the [AudioStream], which can be null
         * @return this [Builder] instance
         */
        public fun setAudioTrackId(audioTrackId: String?): Builder =
            apply { this.audioTrackId = audioTrackId }

        /**
         * Set the audio track name of the [AudioStream].
         *
         * The default value is `null`.
         *
         * @param audioTrackName the audio track name of the [AudioStream], which can be null
         * @return this [Builder] instance
         */
        public fun setAudioTrackName(audioTrackName: String?): Builder =
            apply { this.audioTrackName = audioTrackName }

        /**
         * Set the [AudioTrackType] of the [AudioStream].
         *
         * The default value is `null`.
         *
         * @param audioTrackType the audio track type of the [AudioStream], which can be null
         * @return this [Builder] instance
         */
        public fun setAudioTrackType(audioTrackType: AudioTrackType?): Builder =
            apply { this.audioTrackType = audioTrackType }

        /**
         * Set the [Locale] of the audio which represents its language.
         *
         * The default value is `null`, which means that the [Locale] is unknown.
         *
         * @param audioLocale the [Locale] of the audio, which could be `null`
         * @return this [Builder] instance
         */
        public fun setAudioLocale(audioLocale: Locale?): Builder =
            apply { this.audioLocale = audioLocale }

        /**
         * Set the [ItagItem] corresponding to the [AudioStream].
         *
         * [ItagItem]s are YouTube specific objects, so they are only known for this service
         * and can be null.
         *
         * The default value is `null`.
         *
         * @param itagItem the [ItagItem] of the [AudioStream], which can be null
         * @return this [Builder] instance
         */
        public fun setItagItem(itagItem: ItagItem?): Builder = apply { this.itagItem = itagItem }

        /**
         * Build an [AudioStream] using the builder's current values.
         *
         * The identifier and the content (and thus `isUrl`) properties must have
         * been set.
         *
         * The error messages never quote the content, which is usually a stream URL.
         *
         * @return a new [AudioStream] using the builder's current values
         * @throws IllegalStateException if `id`, `content` (and thus `isUrl`) or
         * `deliveryMethod` have been not set, or have been set as `null`
         */
        public fun build(): AudioStream {
            val id = checkNotNull(id) {
                "The identifier of the audio stream has been not set or is null. If you " +
                    "are not able to get an identifier, use the static constant " +
                    "ID_UNKNOWN of the Stream class."
            }
            val content = checkNotNull(content) {
                "The content of the audio stream has been not set " +
                    "or is null. Please specify a non-null one with setContent."
            }
            val deliveryMethod = checkNotNull(deliveryMethod) {
                "The delivery method of the audio stream has been set as null, which is " +
                    "not allowed. Pass a valid one instead with setDeliveryMethod."
            }
            return AudioStream(
                id, content, isUrl, mediaFormat, deliveryMethod, manifestUrl, averageBitrate,
                audioTrackId, audioTrackName, audioLocale, audioTrackType, itagItem,
            )
        }
    }

    /**
     * Whether the stats of [other] equal this stream's, including the audio bitrate and track.
     */
    override fun equalStats(other: Stream?): Boolean = super.equalStats(other) &&
        other is AudioStream &&
        averageBitrate == other.averageBitrate &&
        audioTrackId == other.audioTrackId &&
        audioTrackType == other.audioTrackType &&
        audioLocale == other.audioLocale

    public companion object {
        private const val serialVersionUID: Long = 1L

        public const val UNKNOWN_BITRATE: Int = -1
    }
}

package org.schabi.newpipe.extractor.stream

import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.services.youtube.ItagItem
import java.io.Serializable

/**
 * Abstract class which represents streams in the extractor.
 *
 * Instantiates a new `Stream` object.
 *
 * @param id             the identifier which uniquely identifies the file, e.g. for YouTube
 *                       this would be the itag
 * @param content        the content or URL, depending on whether isUrl is true
 * @param isUrl          whether content is the URL or the actual content of e.g. a DASH
 *                       manifest
 * @param format         the [MediaFormat], which can be null
 * @param deliveryMethod the delivery method of the stream
 * @param manifestUrl    the URL of the manifest this stream comes from (if applicable,
 *                       otherwise null)
 */
public abstract class Stream(
    /**
     * The identifier of this stream, e.g. the itag for YouTube.
     *
     * It should normally be unique, but [ID_UNKNOWN] may be returned as the identifier if
     * the one used by the stream extractor cannot be extracted, which could happen if the
     * extractor uses a value from a streaming service.
     */
    public val id: String,
    /** The content or URL. */
    public val content: String,
    /**
     * Whether the content is a URL or not: `true` if the content of this stream is a URL,
     * `false` if it's the actual content.
     */
    public val isUrl: Boolean,
    format: MediaFormat?,
    /** The [DeliveryMethod]. */
    public val deliveryMethod: DeliveryMethod,
    /** The URL of the manifest this stream comes from (if applicable, otherwise null). */
    public val manifestUrl: String?,
) : Serializable {

    private val mediaFormat: MediaFormat? = format

    /**
     * The [MediaFormat], which can be null.
     */
    public val format: MediaFormat?
        get() = mediaFormat

    /**
     * The format ID, which can be unknown: the format ID or [FORMAT_ID_UNKNOWN].
     */
    public val formatId: Int
        get() = mediaFormat?.id ?: FORMAT_ID_UNKNOWN

    /**
     * The [ItagItem] of a stream.
     *
     * If the stream is not from YouTube, this value will always be null.
     */
    public abstract val itagItem: ItagItem?

    /**
     * Reveals whether two streams have the same statistics ([media format][MediaFormat] and
     * [delivery method][DeliveryMethod]).
     *
     * Streams whose [media format][MediaFormat] is unknown are never equal (as in the Java
     * extractor): two HLS or manifest streams of unknown format may hold different content, so
     * neither is dropped as a duplicate of the other.
     *
     * Note: This method always returns false if the stream passed is null.
     *
     * @param other the stream object to be compared to this stream object
     * @return whether the stream have the same stats or not, based on the criteria above
     */
    public open fun equalStats(other: Stream?): Boolean {
        val format = mediaFormat ?: return false
        val otherFormat = other?.mediaFormat ?: return false
        return format.id == otherFormat.id &&
            deliveryMethod == other.deliveryMethod &&
            isUrl == other.isUrl
    }

    public companion object {
        private const val serialVersionUID: Long = 1L

        public const val FORMAT_ID_UNKNOWN: Int = -1
        public const val ID_UNKNOWN: String = " "

        /**
         * An integer to represent that the itag ID returned is not available (only for YouTube;
         * this should never happen) or not applicable (for other services than YouTube).
         *
         * An itag should not have a negative value, so `-1` is used for this constant.
         */
        public const val ITAG_NOT_AVAILABLE_OR_NOT_APPLICABLE: Int = -1

        /**
         * Checks if the list already contains a stream with the same statistics.
         *
         * @param stream the stream to be compared against the streams in the stream list
         * @param streamList the list of [Stream]s which will be compared
         * @return whether the list already contains one stream with equals stats
         */
        @JvmStatic
        public fun containSimilarStream(stream: Stream, streamList: List<Stream>?): Boolean =
            !streamList.isNullOrEmpty() && streamList.any { stream.equalStats(it) }
    }
}

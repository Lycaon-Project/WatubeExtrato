package org.schabi.newpipe.extractor.services.youtube

import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.MediaFormat.M4A
import org.schabi.newpipe.extractor.MediaFormat.MPEG_4
import org.schabi.newpipe.extractor.MediaFormat.WEBM
import org.schabi.newpipe.extractor.MediaFormat.WEBMA
import org.schabi.newpipe.extractor.MediaFormat.WEBMA_OPUS
import org.schabi.newpipe.extractor.MediaFormat.v3GPP
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.services.youtube.ItagItem.ItagType.AUDIO
import org.schabi.newpipe.extractor.services.youtube.ItagItem.ItagType.VIDEO
import org.schabi.newpipe.extractor.services.youtube.ItagItem.ItagType.VIDEO_ONLY
import org.schabi.newpipe.extractor.stream.AudioTrackType
import java.io.Serializable
import java.util.Locale

public class ItagItem : Serializable {

    public enum class ItagType {
        AUDIO,
        VIDEO,
        VIDEO_ONLY,
    }

    public val mediaFormat: MediaFormat

    @JvmField
    public val id: Int

    @JvmField
    public val itagType: ItagType

    // Audio fields

    /**
     * @deprecated Use [averageBitrate] instead.
     */
    @Deprecated("Use averageBitrate instead.", ReplaceWith("averageBitrate"))
    @JvmField
    public var avgBitrate: Int = AVERAGE_BITRATE_UNKNOWN

    /**
     * The sample rate.
     *
     * It is only known for audio itags, so [SAMPLE_RATE_UNKNOWN] is returned for non audio
     * itags, or if the sample rate is unknown.
     *
     * Setting it: [SAMPLE_RATE_UNKNOWN] is set/used for non audio itags, or if the sample rate
     * value is less than or equal to 0.
     */
    public var sampleRate: Int = SAMPLE_RATE_UNKNOWN
        set(value) {
            field = if (value > 0) value else SAMPLE_RATE_UNKNOWN
        }

    /**
     * The number of audio channels.
     *
     * It is only known for audio itags, so [AUDIO_CHANNELS_NOT_APPLICABLE_OR_UNKNOWN] is
     * returned for non audio itags, or if it is unknown.
     *
     * Setting it: [AUDIO_CHANNELS_NOT_APPLICABLE_OR_UNKNOWN] is set/used for non audio itags, or
     * if the `audioChannels` value is less than or equal to 0.
     */
    public var audioChannels: Int = AUDIO_CHANNELS_NOT_APPLICABLE_OR_UNKNOWN
        set(value) {
            field = if (value > 0) value else AUDIO_CHANNELS_NOT_APPLICABLE_OR_UNKNOWN
        }

    // Video fields

    /**
     * @deprecated Use [getResolutionString] instead.
     */
    @Deprecated("Use getResolutionString() instead.")
    @JvmField
    public var resolutionString: String? = null

    /**
     * @deprecated Use [getFps] and [setFps] instead.
     */
    @Deprecated("Use getFps() and setFps(int) instead.")
    @JvmField
    public var fps: Int = FPS_NOT_APPLICABLE_OR_UNKNOWN

    // Fields for Dash
    public var bitrate: Int = 0
    public var width: Int = 0
    public var height: Int = 0
    public var initStart: Int = 0
    public var initEnd: Int = 0
    public var indexStart: Int = 0
    public var indexEnd: Int = 0
    public var quality: String? = null
    public var codec: String? = null

    /**
     * The `targetDurationSec` value.
     *
     * This value is the average time in seconds of the duration of sequences of livestreams and
     * ended livestreams. It is only returned by YouTube for these stream types, and makes no sense
     * for videos, so [TARGET_DURATION_SEC_UNKNOWN] is returned for those.
     *
     * Setting it: [TARGET_DURATION_SEC_UNKNOWN] will be set/used for video streams or if this
     * value is less than or equal to 0.
     */
    public var targetDurationSec: Int = TARGET_DURATION_SEC_UNKNOWN
        set(value) {
            field = if (value > 0) value else TARGET_DURATION_SEC_UNKNOWN
        }

    /**
     * The `approxDurationMs` value, in milliseconds.
     *
     * It is only known for DASH progressive streams, so [APPROX_DURATION_MS_UNKNOWN] is
     * returned (and set) for other stream types or if this value is less than or equal to 0.
     */
    public var approxDurationMs: Long = APPROX_DURATION_MS_UNKNOWN
        set(value) {
            field = if (value > 0) value else APPROX_DURATION_MS_UNKNOWN
        }

    /**
     * The content length of the stream.
     *
     * It is only known for DASH progressive streams, so [CONTENT_LENGTH_UNKNOWN] is
     * returned (and set) for other stream types or if this value is less than or equal to 0.
     */
    public var contentLength: Long = CONTENT_LENGTH_UNKNOWN
        set(value) {
            field = if (value > 0) value else CONTENT_LENGTH_UNKNOWN
        }

    /** The `audioTrackId` of the stream, if present, or `null`. */
    public var audioTrackId: String? = null

    /** The `audioTrackName` of the stream, if present, or `null`. */
    public var audioTrackName: String? = null

    /** The [AudioTrackType] of the stream, if present, or `null`. */
    public var audioTrackType: AudioTrackType? = null

    /**
     * The audio [Locale] of the stream, if known, or `null` if that's not the case (the
     * default value).
     */
    public var audioLocale: Locale? = null

    /**
     * Whether the audio is using dynamic range compression (DRC).
     *
     * https://en.wikipedia.org/wiki/Dynamic_range_compression
     */
    @set:JvmName("setIsDrc")
    public var isDrc: Boolean? = false

    /**
     * When the stream was last modified, as a unix timestamp.
     *
     * If the timestamp is unknown, [LAST_MODIFIED_UNKOWN] is returned.
     */
    public var lastModified: Long = 0

    /**
     * Extra tags about the stream.
     *
     * Contains a Base64 encoded protobuf key-value list of additional tags for the stream,
     * such as whether the stream is using [isDrc].
     */
    public var xtags: String? = null

    /** Whether the stream has been auto-generated. */
    public var isAutoGenerated: Boolean = false

    /**
     * Call [ItagItem] (id, type, format, resolution, fps) with the fps set to 30.
     */
    @Suppress("DEPRECATION")
    public constructor(id: Int, type: ItagType, format: MediaFormat, resolution: String?) {
        this.id = id
        this.itagType = type
        this.mediaFormat = format
        this.resolutionString = resolution
        this.fps = DEFAULT_FPS
    }

    /**
     * Constructor for videos.
     */
    @Suppress("DEPRECATION")
    public constructor(id: Int, type: ItagType, format: MediaFormat, resolution: String?, fps: Int) {
        this.id = id
        this.itagType = type
        this.mediaFormat = format
        this.resolutionString = resolution
        this.fps = fps
    }

    @Suppress("DEPRECATION")
    public constructor(id: Int, type: ItagType, format: MediaFormat, avgBitrate: Int) {
        this.id = id
        this.itagType = type
        this.mediaFormat = format
        this.avgBitrate = avgBitrate
    }

    /**
     * Copy constructor of the [ItagItem] class.
     *
     * @param itagItem the [ItagItem] to copy its properties into a new [ItagItem]
     */
    @Suppress("DEPRECATION")
    public constructor(itagItem: ItagItem) {
        this.mediaFormat = itagItem.mediaFormat
        this.id = itagItem.id
        this.itagType = itagItem.itagType
        this.avgBitrate = itagItem.avgBitrate
        this.sampleRate = itagItem.sampleRate
        this.audioChannels = itagItem.audioChannels
        this.resolutionString = itagItem.resolutionString
        this.fps = itagItem.fps
        this.bitrate = itagItem.bitrate
        this.width = itagItem.width
        this.height = itagItem.height
        this.initStart = itagItem.initStart
        this.initEnd = itagItem.initEnd
        this.indexStart = itagItem.indexStart
        this.indexEnd = itagItem.indexEnd
        this.quality = itagItem.quality
        this.codec = itagItem.codec
        this.targetDurationSec = itagItem.targetDurationSec
        this.approxDurationMs = itagItem.approxDurationMs
        this.contentLength = itagItem.contentLength
        this.audioTrackId = itagItem.audioTrackId
        this.audioTrackName = itagItem.audioTrackName
        this.audioTrackType = itagItem.audioTrackType
        this.audioLocale = itagItem.audioLocale
        this.isDrc = itagItem.isDrc
        this.lastModified = itagItem.lastModified
        this.xtags = itagItem.xtags
        this.isAutoGenerated = itagItem.isAutoGenerated
    }

    /**
     * Get the frame rate.
     *
     * It is set to the `fps` value returned in the corresponding itag in the YouTube player
     * response.
     *
     * It defaults to the standard value associated with this itag.
     *
     * Note that this value is only known for video itags, so [FPS_NOT_APPLICABLE_OR_UNKNOWN] is
     * returned for non video itags.
     *
     * (A function, not a property: the deprecated public [fps] field keeps its JVM name.)
     *
     * @return the frame rate or [FPS_NOT_APPLICABLE_OR_UNKNOWN]
     */
    @Suppress("DEPRECATION")
    public fun getFps(): Int = fps

    /**
     * Set the frame rate.
     *
     * It is only known for video itags, so [FPS_NOT_APPLICABLE_OR_UNKNOWN] is set/used for
     * non video itags or if the sample rate value is less than or equal to 0.
     *
     * @param fps the frame rate
     */
    @Suppress("DEPRECATION")
    public fun setFps(fps: Int) {
        this.fps = if (fps > 0) fps else FPS_NOT_APPLICABLE_OR_UNKNOWN
    }

    /**
     * Get the resolution string associated with this `ItagItem`.
     *
     * It is only known for video itags.
     *
     * (A function, not a property: the deprecated public [resolutionString] field keeps its JVM
     * name.)
     *
     * @return the resolution string associated with this `ItagItem` or `null`.
     */
    @Suppress("DEPRECATION")
    public fun getResolutionString(): String? = resolutionString

    /**
     * The average bitrate.
     *
     * It is only known for audio itags, so [AVERAGE_BITRATE_UNKNOWN] is always returned for
     * other itag types.
     *
     * Bitrate of video itags and precise bitrate of audio itags can be known using
     * [bitrate].
     *
     * @see bitrate
     */
    @Suppress("DEPRECATION")
    public val averageBitrate: Int
        get() = avgBitrate

    public companion object {
        private const val serialVersionUID: Long = 1L

        private const val DEFAULT_FPS = 30
        private const val FPS_60 = 60

        public const val AVERAGE_BITRATE_UNKNOWN: Int = -1
        public const val SAMPLE_RATE_UNKNOWN: Int = -1
        public const val FPS_NOT_APPLICABLE_OR_UNKNOWN: Int = -1
        public const val TARGET_DURATION_SEC_UNKNOWN: Int = -1
        public const val AUDIO_CHANNELS_NOT_APPLICABLE_OR_UNKNOWN: Int = -1
        public const val CONTENT_LENGTH_UNKNOWN: Long = -1
        public const val APPROX_DURATION_MS_UNKNOWN: Long = -1
        public const val LAST_MODIFIED_UNKOWN: Long = -1

        /**
         * List can be found here:
         * https://github.com/ytdl-org/youtube-dl/blob/e988fa4/youtube_dl/extractor/youtube.py#L1195
         */
        private val ITAG_LIST: Array<ItagItem> = arrayOf(
            // ///////////////////////////////////////////////////
            // VIDEO     ID  Type   Format  Resolution  FPS  ////
            // ///////////////////////////////////////////////////
            ItagItem(17, VIDEO, v3GPP, "144p"),
            ItagItem(36, VIDEO, v3GPP, "240p"),

            ItagItem(18, VIDEO, MPEG_4, "360p"),
            ItagItem(34, VIDEO, MPEG_4, "360p"),
            ItagItem(35, VIDEO, MPEG_4, "480p"),
            ItagItem(59, VIDEO, MPEG_4, "480p"),
            ItagItem(78, VIDEO, MPEG_4, "480p"),
            ItagItem(22, VIDEO, MPEG_4, "720p"),
            ItagItem(37, VIDEO, MPEG_4, "1080p"),
            ItagItem(38, VIDEO, MPEG_4, "1080p"),

            ItagItem(43, VIDEO, WEBM, "360p"),
            ItagItem(44, VIDEO, WEBM, "480p"),
            ItagItem(45, VIDEO, WEBM, "720p"),
            ItagItem(46, VIDEO, WEBM, "1080p"),

            // ////////////////////////////////////////////////////////////////
            // AUDIO     ID      ItagType          Format        Bitrate    //
            // ////////////////////////////////////////////////////////////////
            ItagItem(171, AUDIO, WEBMA, 128),
            ItagItem(172, AUDIO, WEBMA, 256),
            ItagItem(599, AUDIO, M4A, 32),
            ItagItem(139, AUDIO, M4A, 48),
            ItagItem(140, AUDIO, M4A, 128),
            ItagItem(141, AUDIO, M4A, 256),
            ItagItem(600, AUDIO, WEBMA_OPUS, 35),
            ItagItem(249, AUDIO, WEBMA_OPUS, 50),
            ItagItem(250, AUDIO, WEBMA_OPUS, 70),
            ItagItem(251, AUDIO, WEBMA_OPUS, 160),

            // / VIDEO ONLY ////////////////////////////////////////////
            //           ID      Type     Format  Resolution  FPS  ////
            // /////////////////////////////////////////////////////////
            ItagItem(160, VIDEO_ONLY, MPEG_4, "144p"),
            ItagItem(394, VIDEO_ONLY, MPEG_4, "144p"),
            ItagItem(133, VIDEO_ONLY, MPEG_4, "240p"),
            ItagItem(395, VIDEO_ONLY, MPEG_4, "240p"),
            ItagItem(134, VIDEO_ONLY, MPEG_4, "360p"),
            ItagItem(396, VIDEO_ONLY, MPEG_4, "360p"),
            ItagItem(135, VIDEO_ONLY, MPEG_4, "480p"),
            ItagItem(212, VIDEO_ONLY, MPEG_4, "480p"),
            ItagItem(397, VIDEO_ONLY, MPEG_4, "480p"),
            ItagItem(136, VIDEO_ONLY, MPEG_4, "720p"),
            ItagItem(398, VIDEO_ONLY, MPEG_4, "720p"),
            ItagItem(298, VIDEO_ONLY, MPEG_4, "720p60", FPS_60),
            ItagItem(137, VIDEO_ONLY, MPEG_4, "1080p"),
            ItagItem(399, VIDEO_ONLY, MPEG_4, "1080p"),
            ItagItem(299, VIDEO_ONLY, MPEG_4, "1080p60", FPS_60),
            ItagItem(400, VIDEO_ONLY, MPEG_4, "1440p"),
            ItagItem(266, VIDEO_ONLY, MPEG_4, "2160p"),
            ItagItem(401, VIDEO_ONLY, MPEG_4, "2160p"),

            ItagItem(278, VIDEO_ONLY, WEBM, "144p"),
            ItagItem(242, VIDEO_ONLY, WEBM, "240p"),
            ItagItem(243, VIDEO_ONLY, WEBM, "360p"),
            ItagItem(244, VIDEO_ONLY, WEBM, "480p"),
            ItagItem(245, VIDEO_ONLY, WEBM, "480p"),
            ItagItem(246, VIDEO_ONLY, WEBM, "480p"),
            ItagItem(247, VIDEO_ONLY, WEBM, "720p"),
            ItagItem(248, VIDEO_ONLY, WEBM, "1080p"),
            ItagItem(271, VIDEO_ONLY, WEBM, "1440p"),
            // #272 is either 3840x2160 (e.g. RtoitU2A-3E) or 7680x4320 (sLprVF6d7Ug)
            ItagItem(272, VIDEO_ONLY, WEBM, "2160p"),
            ItagItem(302, VIDEO_ONLY, WEBM, "720p60", FPS_60),
            ItagItem(303, VIDEO_ONLY, WEBM, "1080p60", FPS_60),
            ItagItem(308, VIDEO_ONLY, WEBM, "1440p60", FPS_60),
            ItagItem(313, VIDEO_ONLY, WEBM, "2160p"),
            ItagItem(315, VIDEO_ONLY, WEBM, "2160p60", FPS_60),
        )

        // //////////////////////////////////////////////////////////////////////////
        // Utils
        // //////////////////////////////////////////////////////////////////////////

        @JvmStatic
        @Throws(ParsingException::class)
        public fun getItag(itagId: Int): ItagItem {
            val item = ITAG_LIST.firstOrNull { it.id == itagId }
                ?: throw ParsingException("itag $itagId is not supported")
            // a copy: the items of the list are shared, the caller sets its own values
            return ItagItem(item)
        }
    }
}

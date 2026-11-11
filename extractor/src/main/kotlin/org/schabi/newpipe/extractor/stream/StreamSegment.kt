package org.schabi.newpipe.extractor.stream

import java.io.Serializable

/**
 * A chapter of a stream.
 *
 * @param title Title of this segment
 * @param startTimeSeconds Timestamp of the starting point in seconds
 */
public class StreamSegment(
    title: String?,
    startTimeSeconds: Int,
) : Serializable {

    /**
     * Title of this segment
     */
    public var title: String? = title

    /**
     * The channel or creator linked to this segment
     */
    public var channelName: String? = null

    /**
     * Timestamp of the starting point in seconds
     */
    public var startTimeSeconds: Int = startTimeSeconds

    /**
     * Direct url to this segment. This can be null if the service doesn't provide such function.
     *
     * Kept as a public field (Java callers read and write it directly), with [getUrl] and
     * [setUrl] alongside.
     */
    @JvmField
    public var url: String? = null

    /**
     * Preview url for this segment. This can be null if the service doesn't provide such function
     * or there is no resource found.
     */
    public var previewUrl: String? = null

    public fun getUrl(): String? = url

    public fun setUrl(url: String?) {
        this.url = url
    }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

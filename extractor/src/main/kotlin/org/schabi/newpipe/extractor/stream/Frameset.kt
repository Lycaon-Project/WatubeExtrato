package org.schabi.newpipe.extractor.stream

import java.io.Serializable

/**
 * Class to handle framesets / storyboards which summarize the stream content.
 *
 * Creates a new Frameset or set of storyboards.
 *
 * @property urls the URLs to the images with frames / storyboards
 * @property frameWidth the width of a single frame, in pixels
 * @property frameHeight the height of a single frame, in pixels
 * @property totalCount the total count of frames
 * @property durationPerFrame the duration per frame in milliseconds
 * @property framesPerPageX the maximum count of frames per page by x / over the width of the image
 * @property framesPerPageY the maximum count of frames per page by y / over the height
 * of the image
 */
public class Frameset(
    /** list of URLs to images with frames */
    public val urls: List<String>,
    /** width of a one frame, in pixels */
    public val frameWidth: Int,
    /** height of a one frame, in pixels */
    public val frameHeight: Int,
    /** total count of frames */
    public val totalCount: Int,
    /** duration per frame in milliseconds */
    public val durationPerFrame: Int,
    /** maximum frames count by x */
    public val framesPerPageX: Int,
    /** maximum frames count by y */
    public val framesPerPageY: Int,
) : Serializable {

    /**
     * Returns the information for the frame at stream position.
     *
     * @param position Position in milliseconds
     * @return An `int`-array containing the bounds and URL where the indexes are
     * specified as follows:
     *
     * - `0`: Index of the URL
     * - `1`: Left bound
     * - `2`: Top bound
     * - `3`: Right bound
     * - `4`: Bottom bound
     */
    public fun getFrameBoundsAt(position: Long): IntArray {
        // The sizes come from the service: a zero (or negative) one would divide by zero below
        if (position < 0 || durationPerFrame <= 0 || framesPerPageX <= 0 || framesPerPageY <= 0 ||
            position > (totalCount.toLong() + 1) * durationPerFrame
        ) {
            // Return the first frame as fallback
            return intArrayOf(0, 0, 0, frameWidth, frameHeight)
        }

        val framesPerStoryboard = framesPerPageX * framesPerPageY
        val absoluteFrameNumber = minOf((position / durationPerFrame).toInt(), totalCount)

        val relativeFrameNumber = absoluteFrameNumber % framesPerStoryboard

        val rowIndex = Math.floorDiv(relativeFrameNumber, framesPerPageX)
        // the frames fill a page row by row: framesPerPageX of them per row
        val columnIndex = relativeFrameNumber % framesPerPageX

        return intArrayOf(
            // storyboardIndex
            Math.floorDiv(absoluteFrameNumber, framesPerStoryboard),
            // left
            columnIndex * frameWidth,
            // top
            rowIndex * frameHeight,
            // right
            columnIndex * frameWidth + frameWidth,
            // bottom
            rowIndex * frameHeight + frameHeight,
        )
    }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

package org.schabi.newpipe.extractor

import java.io.Serializable

/**
 * Class representing images in the extractor.
 *
 * An image has four properties: its URL, its height, its width and its estimated quality level.
 *
 * Depending of the services, the height, the width or both properties may be not known.
 * Implementations **must use** the relevant unknown constants in this case
 * ([HEIGHT_UNKNOWN] and [WIDTH_UNKNOWN]), to ensure properly the lack of knowledge
 * of one or both of these properties to extractor clients.
 *
 * They should also respect the ranges defined in the estimated image resolution levels as much as
 * possible, to ensure consistency to extractor clients.
 *
 * @param url                      the URL to the image, which should be not null or empty
 * @param height                   the image's height, or [HEIGHT_UNKNOWN]
 * @param width                    the image's width, or [WIDTH_UNKNOWN]
 * @param estimatedResolutionLevel the image's estimated resolution level, which must not be
 * null; [ResolutionLevel.UNKNOWN] when it is unknown
 * @throws NullPointerException if `estimatedResolutionLevel` is null
 */
public class Image(
    public val url: String,
    public val height: Int,
    public val width: Int,
    public val estimatedResolutionLevel: ResolutionLevel,
) : Serializable {

    /**
     * Get a string representation of this [Image] instance.
     *
     * The representation will be in the following format, where `url`, `height`,
     * `width` and `estimatedResolutionLevel` represent the corresponding properties:
     *
     * `Image {url=url, height=height, width=width,
     * estimatedResolutionLevel=estimatedResolutionLevel}`
     *
     * @return a string representation of this [Image] instance
     */
    // the URL is not printed: logs must not tell what the user looked at
    override fun toString(): String = "Image {height=$height, width=$width, " +
        "estimatedResolutionLevel=$estimatedResolutionLevel}"

    /**
     * The estimated resolution level of an [Image].
     *
     * Some services don't return the size of their images, but we may know for a specific image
     * type that a service returns, according to real data, an approximation of the resolution
     * level.
     */
    public enum class ResolutionLevel {

        /**
         * The high resolution level.
         *
         * This level applies to images with a height greater than or equal to 720px.
         */
        HIGH,

        /**
         * The medium resolution level.
         *
         * This level applies to images with a height between 175px inclusive and 720px exclusive.
         */
        MEDIUM,

        /**
         * The low resolution level.
         *
         * This level applies to images with a height between 1px inclusive and 175px exclusive.
         */
        LOW,

        /**
         * The unknown resolution level.
         *
         * This value is returned when the extractor doesn't know what resolution level an image
         * could have, for example if the extractor loops in an array of images with different
         * resolution levels without knowing the height.
         */
        UNKNOWN,
        ;

        public companion object {
            private const val MEDIUM_MIN_HEIGHT = 175
            private const val HIGH_MIN_HEIGHT = 720

            /**
             * Get a [ResolutionLevel] based from the given height.
             *
             * @param heightPx the height from which returning the good [ResolutionLevel]
             * @return the [ResolutionLevel] corresponding to the height provided. See the
             * [ResolutionLevel] values for details about what value is returned.
             */
            @JvmStatic
            public fun fromHeight(heightPx: Int): ResolutionLevel = when {
                heightPx <= 0 -> UNKNOWN
                heightPx < MEDIUM_MIN_HEIGHT -> LOW
                heightPx < HIGH_MIN_HEIGHT -> MEDIUM
                else -> HIGH
            }
        }
    }

    public companion object {
        private const val serialVersionUID: Long = 1L

        /**
         * Constant representing that the height of an [Image] is unknown.
         */
        public const val HEIGHT_UNKNOWN: Int = -1

        /**
         * Constant representing that the width of an [Image] is unknown.
         */
        public const val WIDTH_UNKNOWN: Int = -1
    }
}

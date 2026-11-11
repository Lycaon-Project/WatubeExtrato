package org.schabi.newpipe.extractor.utils

import org.schabi.newpipe.extractor.Image.ResolutionLevel
import java.io.Serializable

/**
 * Serializable class representing a suffix (which may include its format extension, such as
 * `.jpg`) which needs to be added to get an image/thumbnail URL with its corresponding
 * height, width and estimated resolution level.
 *
 * This class is used to construct [org.schabi.newpipe.extractor.Image] instances from a single
 * base URL/path, in order to get all or most image resolutions provided, depending on the
 * service and the resolutions provided.
 *
 * Note that this class is not intended to be used externally and so should only be used when
 * interfacing with the extractor.
 *
 * @param suffix the suffix which needs to be appended to get the full image URL
 * @param height the height corresponding to the image suffix, which may be unknown
 * @param width the width corresponding to the image suffix, which may be unknown
 * @param estimatedResolutionLevel the [ResolutionLevel] of the image suffix
 */
public class ImageSuffix(
    public val suffix: String,
    public val height: Int,
    public val width: Int,
    estimatedResolutionLevel: ResolutionLevel,
) : Serializable {

    /** The estimated [ResolutionLevel] of the suffix, which is never null. */
    public val resolutionLevel: ResolutionLevel = estimatedResolutionLevel

    /**
     * Get a string representation of this [ImageSuffix] instance.
     *
     * The representation will be in the following format, where `suffix`, `height`,
     * `width` and `resolutionLevel` represent the corresponding properties:
     *
     * `ImageSuffix {url=url, height=height, width=width, resolutionLevel=resolutionLevel}'`
     *
     * @return a string representation of this [ImageSuffix] instance
     */
    override fun toString(): String =
        "ImageSuffix {suffix=$suffix, height=$height, width=$width, " +
            "resolutionLevel=$resolutionLevel}"

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

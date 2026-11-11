package org.schabi.newpipe.extractor.services.bandcamp

import org.junit.jupiter.api.Assertions.assertTrue
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.services.DefaultTests

/**
 * Utility class for Bandcamp tests.
 */
object BandcampTestUtils {
    /**
     * Test that Bandcamp images of a [Collection] respect
     * [default requirements][DefaultTests.defaultTestImageCollection], contain
     * the string `f4.bcbits.com/img` in their URL and end with `.jpg` or `.png`.
     *
     * @param images a Bandcamp [Image] [Collection]
     */
    @JvmStatic
    fun testImages(images: Collection<Image>?) {
        DefaultTests.defaultTestImageCollection(images)
        // If the collection is null, an AssertionError has already been thrown
        // by DefaultTests.defaultTestImageCollection
        assertTrue(
            images!!.all { image ->
                image.url.contains("f4.bcbits.com/img") &&
                    (image.url.endsWith(".jpg") || image.url.endsWith(".png"))
            }
        )
    }
}

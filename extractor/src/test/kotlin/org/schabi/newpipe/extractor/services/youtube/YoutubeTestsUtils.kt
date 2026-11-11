package org.schabi.newpipe.extractor.services.youtube

import org.schabi.newpipe.extractor.ExtractorAsserts
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.services.DefaultTests
import java.util.Random

/**
 * Utility class for YouTube tests.
 */
object YoutubeTestsUtils {

    /**
     * Clears static YT states.
     *
     * This method needs to be called to generate all mocks of a test when running different tests
     * at the same time.
     */
    @JvmStatic
    fun ensureStateless() {
        YoutubeParsingHelper.setConsentAccepted(false)
        YoutubeParsingHelper.resetClientVersion()
        YoutubeParsingHelper.setNumberGenerator(Random(1))
        YoutubeJavaScriptPlayerManager.clearAllCaches()
    }

    /**
     * Test that YouTube images of a [Collection] respect
     * [default requirements][DefaultTests.defaultTestImageCollection] and contain
     * the string `yt` in their URL.
     *
     * @param images a YouTube [Image] [Collection]
     */
    @JvmStatic
    fun testImages(images: Collection<Image>?) {
        DefaultTests.defaultTestImageCollection(images)
        // If the collection is null, an AssertionError has already been thrown
        // by DefaultTests.defaultTestImageCollection
        images!!.forEach { image -> ExtractorAsserts.assertContains("yt", image.url) }
    }
}

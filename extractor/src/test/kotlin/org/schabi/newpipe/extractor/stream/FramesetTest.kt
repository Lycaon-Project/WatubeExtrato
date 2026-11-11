package org.schabi.newpipe.extractor.stream

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test

internal class FramesetTest {

    // pages of 10 frames per row and 2 rows, 100 x 50 pixels per frame, one frame a second
    private val frameset = Frameset(
        urls = listOf("https://i.ytimg.com/sb/x/storyboard3_L2/M0.jpg"),
        frameWidth = 100,
        frameHeight = 50,
        totalCount = 100,
        durationPerFrame = 1_000,
        framesPerPageX = 10,
        framesPerPageY = 2,
    )

    @Test
    fun `a frame is found on a page that is wider than tall`() {
        // frame 12: page 0, second row, third column
        assertArrayEquals(intArrayOf(0, 200, 50, 300, 100), frameset.getFrameBoundsAt(12_000))
        // frame 39: page 1, second row, last column
        assertArrayEquals(intArrayOf(1, 900, 50, 1_000, 100), frameset.getFrameBoundsAt(39_000))
    }

    @Test
    fun `sizes from the service that would divide by zero give the first frame`() {
        val broken = Frameset(emptyList(), 100, 50, 100, 1_000, 0, 2)
        assertArrayEquals(intArrayOf(0, 0, 0, 100, 50), broken.getFrameBoundsAt(12_000))
    }
}

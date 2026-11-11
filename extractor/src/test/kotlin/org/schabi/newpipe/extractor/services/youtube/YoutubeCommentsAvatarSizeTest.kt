/*
 * SPDX-FileCopyrightText: 2026 NewPipe Extractor contributors <https://newpipe.net>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.Image.Companion.HEIGHT_UNKNOWN
import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeCommentsExtractor

/**
 * Verifies that the avatar sizes reported by
 * [org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeCommentsEUVMInfoItemExtractor]
 * (parsed from the `=s<N>` URL parameter) still match what YouTube actually serves.
 *
 * YouTube avatar URLs typically encode the requested size as a `=s<N>`
 * parameter (e.g. `https://yt3.ggpht.com/...=s88-c-k-c0x00ffffff-no-nd-rj`).
 * When the extractor reports `88x88` but YouTube switched to serving a
 * different size, the metadata would silently be wrong, so this test checks
 * that the URL size matches the reported metadata.
 *
 * @see <a href="https://github.com/TeamNewPipe/NewPipeExtractor/issues/1526">issue #1526</a>
 */
class YoutubeCommentsAvatarSizeTest : InitYoutubeTest {

    @Test
    fun avatarUrlSizeMatchesHardcodedMetadata() {
        val extractor = YouTube.getCommentsExtractor(VIDEO_URL) as YoutubeCommentsExtractor
        extractor.fetchPage()
        val comments = extractor.initialPage.items

        var checkedAvatars = 0

        for (comment in comments) {
            for (avatar in comment.uploaderAvatars) {
                val match = SIZE_URL_PARAMETER.find(avatar.url)
                    // URL does not encode a size, nothing to verify
                    ?: continue
                val urlSize = match.groupValues[1].toInt()
                if (avatar.height != HEIGHT_UNKNOWN) {
                    assertEquals(
                        urlSize,
                        avatar.height,
                        "Avatar URL size (=s$urlSize) does not match the" +
                            " reported metadata height (${avatar.height})." +
                            " If YouTube changed the served avatar size, update" +
                            " the hard-coded value in" +
                            " YoutubeCommentsEUVMInfoItemExtractor accordingly."
                    )
                }
                checkedAvatars++
            }
        }

        assertTrue(checkedAvatars > 0, "No avatars with size parameter found")
    }

    private companion object {
        const val VIDEO_URL = "https://www.youtube.com/watch?v=D00Au7k3i6o"

        val SIZE_URL_PARAMETER = Regex("=s(\\d+)")
    }
}

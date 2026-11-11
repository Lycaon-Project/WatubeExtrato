/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.extractors

import org.schabi.newpipe.extractor.services.youtube.ItagItem
import java.io.Serializable

/**
 * Class to build easier [org.schabi.newpipe.extractor.stream.Stream]s for
 * [YoutubeStreamExtractor].
 *
 * It stores, per stream:
 * - its content (the URL/the base URL of streams);
 * - whether its content is the URL the content itself or the base URL;
 * - its associated [ItagItem].
 *
 * @param content the content of the stream, which is either the URL to the content itself or
 * the base URL
 * @param itagItem the [ItagItem] associated with the stream
 */
internal class ItagInfo(
    val content: String,
    val itagItem: ItagItem,
) : Serializable {

    /**
     * Whether the content stored is the URL to the content itself or the base URL of it.
     *
     * @see content for more details
     */
    var isUrl: Boolean = false

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

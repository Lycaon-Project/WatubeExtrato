/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.media_ccc.linkHandler

import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory
import org.schabi.newpipe.extractor.services.media_ccc.MediaCCCLinkGuards
import org.schabi.newpipe.extractor.utils.Parser
import java.util.regex.Pattern

/**
 * Since MediaCCC does not really have channel tabs (i.e. it only has one single "tab" with videos),
 * this link handler acts both as the channel link handler and the channel tab link handler. That's
 * why [availableContentFilter] has been overridden.
 */
public object MediaCCCConferenceLinkHandlerFactory : ListLinkHandlerFactory() {

    public const val CONFERENCE_API_ENDPOINT: String = "https://api.media.ccc.de/public/conferences/"
    public const val CONFERENCE_PATH: String = "https://media.ccc.de/c/"

    private val ID_PATTERN: Pattern = Pattern.compile(
        "(?:(?:(?:api\\.)?media\\.ccc\\.de/public/conferences/)" +
            "|(?:media\\.ccc\\.de/[bc]/))([^/?&#]*)",
    )

    @JvmStatic
    public fun getInstance(): MediaCCCConferenceLinkHandlerFactory = this

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
    ): String = CONFERENCE_PATH + MediaCCCLinkGuards.requireSlug(id)

    /** The ID is checked to be a conference slug before it is returned. */
    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getId(url: String?): String =
        MediaCCCLinkGuards.requireSlug(Parser.matchGroup1(ID_PATTERN, MediaCCCLinkGuards.requireBoundedUrl(url)))

    override fun onAcceptUrl(url: String?): Boolean = try {
        getId(url)
        true
    } catch (_: ParsingException) {
        false
    }

    /**
     * @see MediaCCCConferenceLinkHandlerFactory
     * @return MediaCCC's only channel "tab", i.e. [ChannelTabs.VIDEOS]
     */
    override val availableContentFilter: Array<String>
        get() = arrayOf(ChannelTabs.VIDEOS)
}

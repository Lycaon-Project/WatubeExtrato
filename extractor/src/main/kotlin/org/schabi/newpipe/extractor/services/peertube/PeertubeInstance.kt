/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.peertube

import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.json.JsonParser
import org.schabi.newpipe.extractor.json.JsonParserException
import org.schabi.newpipe.extractor.utils.JsonUtils
import org.schabi.newpipe.extractor.utils.Utils
import java.io.IOException

/**
 * A PeerTube instance chosen by the user.
 *
 * @param url  the address of the instance: https, or plain http on the device itself only
 * @param name its name
 * @throws IllegalArgumentException for an instance that would be reached in the clear, or an
 * address that is not the one of an instance (user info, query, path, non-ASCII host)
 */
public class PeertubeInstance(url: String, name: String) {

    /** Every request to the instance would tell what the user watches: never in the clear. */
    public val url: String = PeertubeLinkGuards.checkInstanceUrl(url)

    /** Set by [fetchInstanceMetaData] and read from other threads. */
    @Volatile
    public var name: String = name
        private set

    public constructor(url: String) : this(url, "PeerTube")

    @Throws(Exception::class)
    public fun fetchInstanceMetaData() {
        val downloader = NewPipe.getDownloader() ?: throw Exception("No downloader is set")
        val response = try {
            downloader.get("$url/api/v1/config")
        } catch (_: ReCaptchaException) {
            // the cause is not kept: its message may quote the instance URL
            throw Exception("Unable to configure the PeerTube instance")
        } catch (_: IOException) {
            throw Exception("Unable to configure the PeerTube instance")
        }

        val body = response.responseBody()
        if (Utils.isBlank(body)) {
            throw Exception("Unable to configure the PeerTube instance")
        }

        try {
            val json = JsonParser.`object`().from(body)
            name = JsonUtils.getString(json, "instance.name")
        } catch (e: JsonParserException) {
            throw Exception("Unable to parse instance config", e)
        } catch (e: ParsingException) {
            throw Exception("Unable to parse instance config", e)
        }
    }

    public companion object {
        @JvmField
        public val DEFAULT_INSTANCE: PeertubeInstance = PeertubeInstance("https://framatube.org", "FramaTube")
    }
}

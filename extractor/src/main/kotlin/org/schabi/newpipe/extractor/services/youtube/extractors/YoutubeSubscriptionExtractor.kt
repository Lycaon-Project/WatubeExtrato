/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.services.youtube.extractors

import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonParser
import org.schabi.newpipe.extractor.json.JsonParserException
import org.schabi.newpipe.extractor.services.youtube.YoutubeService
import org.schabi.newpipe.extractor.subscription.SubscriptionExtractor
import org.schabi.newpipe.extractor.subscription.SubscriptionItem
import org.schabi.newpipe.extractor.utils.BoundedInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.ZipInputStream

/**
 * Extract subscriptions from a Google takeout export
 */
public open class YoutubeSubscriptionExtractor(
    youtubeService: YoutubeService,
) : SubscriptionExtractor(youtubeService, listOf(ContentSource.INPUT_STREAM)) {

    override val relatedUrl: String
        get() = "https://takeout.google.com/takeout/custom/youtube"

    @Throws(ExtractionException::class)
    override fun fromInputStream(contentInputStream: InputStream): List<SubscriptionItem> =
        fromJsonInputStream(contentInputStream)

    @Throws(ExtractionException::class)
    override fun fromInputStream(
        contentInputStream: InputStream,
        contentType: String,
    ): List<SubscriptionItem> = when (contentType) {
        "json", "application/json" -> fromJsonInputStream(contentInputStream)
        "csv", "text/csv", "text/comma-separated-values" -> fromCsvInputStream(contentInputStream)
        "zip", "application/zip" -> fromZipInputStream(contentInputStream)
        else -> throw InvalidSourceException("Unsupported content type: $contentType")
    }

    @Throws(ExtractionException::class)
    public fun fromJsonInputStream(contentInputStream: InputStream): List<SubscriptionItem> {
        val subscriptions = try {
            JsonParser.array().from(BoundedInputStream(contentInputStream, MAX_TAKEOUT_BYTES))
        } catch (e: JsonParserException) {
            throw InvalidSourceException("Invalid json input stream", e)
        } catch (e: IOException) {
            throw InvalidSourceException("Error reading json input stream", e)
        }
        if (subscriptions.size > MAX_SUBSCRIPTIONS) {
            throw InvalidSourceException("Too many subscriptions")
        }

        val subscriptionItems = LinkedHashSet<SubscriptionItem>()
        for (subscriptionObject in subscriptions) {
            subscriptionItems.add(toSubscriptionItem(subscriptionObject))
        }

        if (subscriptionItems.size == 1 && SubscriptionItem.INVALID in subscriptionItems) {
            throw InvalidSourceException("Found only invalid channel ids")
        }
        subscriptionItems.remove(SubscriptionItem.INVALID)
        return ArrayList(subscriptionItems)
    }

    private fun toSubscriptionItem(subscriptionObject: Any?): SubscriptionItem {
        if (subscriptionObject !is JsonObject) {
            return SubscriptionItem.INVALID
        }
        val snippet = subscriptionObject.getObject("snippet")
        val id = snippet.getObject("resourceId").getString("channelId", "").orEmpty()
        val title = snippet.getString("title", "").orEmpty().take(MAX_TITLE_LENGTH)
        // e.g. UCsXVk37bltHxD1rDPwtNM8Q: only these exact IDs are placed in the URL
        if (!CHANNEL_ID.matches(id)) {
            return SubscriptionItem.INVALID
        }
        return SubscriptionItem(service.serviceId, BASE_CHANNEL_URL + id, title)
    }

    @Throws(ExtractionException::class)
    public fun fromZipInputStream(contentInputStream: InputStream): List<SubscriptionItem> {
        try {
            ZipInputStream(contentInputStream).use { zipInputStream ->
                // every inflated byte is counted, those of the skipped entries too: a crafted
                // archive can neither fill the memory nor keep the import busy
                val inflated = BoundedInputStream(zipInputStream, MAX_TAKEOUT_BYTES)
                // Bounded: a crafted archive with countless entries cannot keep the import busy
                repeat(MAX_ZIP_ENTRIES) {
                    val zipEntry = zipInputStream.nextEntry ?: return@use
                    if (zipEntry.name.lowercase(Locale.ROOT).endsWith(".csv")) {
                        try {
                            // The reader must not close the archive, so that the next entries
                            // can still be read
                            val csvItems = readCsv(
                                InputStreamReader(
                                    BoundedInputStream(inflated, MAX_CSV_BYTES),
                                    StandardCharsets.UTF_8,
                                ),
                            )
                            // Return it only if it has items (it exits early if it's the wrong
                            // file format), otherwise try the next file
                            if (csvItems.isNotEmpty()) {
                                return csvItems
                            }
                        } catch (_: ExtractionException) {
                            // Ignore error and go to next file
                        }
                    }
                    inflated.drain()
                }
            }
        } catch (e: IOException) {
            throw InvalidSourceException("Error reading contents of zip file", e)
        }

        throw InvalidSourceException(
            "Unable to find a valid subscriptions.csv file (try extracting and selecting the csv file)",
        )
    }

    @Throws(ExtractionException::class)
    public fun fromCsvInputStream(contentInputStream: InputStream): List<SubscriptionItem> = try {
        contentInputStream.use {
            readCsv(InputStreamReader(BoundedInputStream(it, MAX_CSV_BYTES), StandardCharsets.UTF_8))
        }
    } catch (e: IOException) {
        throw InvalidSourceException("Error reading CSV file", e)
    }

    /**
     * Expected format of CSV file:
     * ```
     * Channel Id,Channel Url,Channel Title
     * UC1JTQBa5QxZCpXrFSkMxmPw,http://www.youtube.com/channel/UC1JTQBa5QxZCpXrFSkMxmPw,Raycevick
     * UCFl7yKfcRcFmIUbKeCA-SJQ,http://www.youtube.com/channel/UCFl7yKfcRcFmIUbKeCA-SJQ,Joji
     * ```
     *
     * Notes:
     * - It's always 3 columns
     * - The first line is always a header
     * - Header names are different based on the locale
     * - Fortunately the data is always the same order no matter what locale
     */
    @Throws(ExtractionException::class)
    private fun readCsv(reader: Reader): List<SubscriptionItem> {
        val items = try {
            generateSequence { readBoundedLine(reader) }
                .drop(1) // ignore header and skip first line
                .take(MAX_CSV_LINES)
                // the title, last, may hold commas itself
                .map { it.split(",", limit = CSV_COLUMNS) }
                .filter { it.size >= CSV_COLUMNS }
                .mapNotNull { values ->
                    // Channel URL from second entry
                    val channelUrl = values[1].replace("http://", "https://")
                    val channelId = channelUrl.removePrefix(BASE_CHANNEL_URL)
                    // Channel title from third entry
                    if (channelUrl.startsWith(BASE_CHANNEL_URL) && CSV_CHANNEL_ID.matches(channelId)) {
                        SubscriptionItem(service.serviceId, channelUrl, values[2].take(MAX_TITLE_LENGTH))
                    } else {
                        null
                    }
                }
                // a channel listed twice is one subscription
                .distinctBy { it.url }
                .take(MAX_SUBSCRIPTIONS + 1)
                .toList()
        } catch (e: IOException) {
            throw InvalidSourceException("Error reading CSV file", e)
        }
        if (items.size > MAX_SUBSCRIPTIONS) {
            throw InvalidSourceException("Too many subscriptions")
        }
        return items
    }

    /**
     * Like [java.io.BufferedReader.readLine], but a line without an end (a whole file of a
     * single line) fails instead of being kept in memory.
     *
     * @return the line without its end, or `null` at the end of the input
     */
    @Throws(IOException::class)
    private fun readBoundedLine(reader: Reader): String? {
        val line = StringBuilder()
        while (true) {
            val char = reader.read()
            when {
                char == -1 -> return line.takeIf { it.isNotEmpty() }?.toString()
                char == '\n'.code -> return line.removeSuffix("\r").toString()
                line.length >= MAX_CSV_LINE_LENGTH -> throw IOException("CSV line too long")
                else -> line.append(char.toChar())
            }
        }
    }

    private companion object {
        private const val BASE_CHANNEL_URL = "https://www.youtube.com/channel/"
        private const val CSV_COLUMNS = 3

        /** Far above any real takeout, low enough to bound the work done on a crafted file. */
        private const val MAX_ZIP_ENTRIES = 10_000
        private const val MAX_CSV_LINES = 200_000
        private const val MAX_SUBSCRIPTIONS = 50_000

        /** All the inflated bytes of an archive, or a JSON export. */
        private const val MAX_TAKEOUT_BYTES = 64L * 1024 * 1024
        private const val MAX_CSV_BYTES = 8L * 1024 * 1024
        private const val MAX_CSV_LINE_LENGTH = 4 * 1024

        /** Channel names are 100 characters at most on YouTube. */
        private const val MAX_TITLE_LENGTH = 256

        private val CHANNEL_ID = Regex("[A-Za-z0-9_-]{24}")

        /** Channel IDs read from a takeout CSV: only URL-safe characters, so nothing is injected. */
        private val CSV_CHANNEL_ID = Regex("[A-Za-z0-9_-]{1,64}")
    }
}

/*
 * Created by Christian Schabesberger on 28.09.16.
 *
 * Copyright (C) 2015 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * YoutubeSuggestionExtractor.java is part of NewPipe Extractor.
 *
 * NewPipe Extractor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe Extractor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe Extractor.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.schabi.newpipe.extractor.services.youtube.extractors

import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonArray
import org.schabi.newpipe.extractor.json.JsonParser
import org.schabi.newpipe.extractor.json.JsonParserException
import org.schabi.newpipe.extractor.suggestion.SuggestionExtractor
import org.schabi.newpipe.extractor.utils.Utils
import java.io.IOException
import java.util.Collections

public open class YoutubeSuggestionExtractor(service: StreamingService) : SuggestionExtractor(service) {

    @Throws(IOException::class, ExtractionException::class)
    override fun suggestionList(query: String): List<String> {
        val url = "https://suggestqueries-clients6.youtube.com/complete/search" +
            "?client=" + "youtube" +
            "&ds=" + "yt" +
            "&gl=" + Utils.encodeUrlUtf8(extractorContentCountry.countryCode) +
            "&q=" + Utils.encodeUrlUtf8(query.take(MAX_QUERY_LENGTH)) +
            "&xhr=t"

        val downloader = NewPipe.getDownloader()
            ?: throw ExtractionException("No downloader has been set")
        val response = downloader.get(url, HEADERS, extractorLocalization)

        val contentTypeHeader = response.getHeader("Content-Type")
        if (contentTypeHeader.isNullOrEmpty() || !contentTypeHeader.contains("application/json")) {
            throw ExtractionException(
                "Invalid response type (got \"" + contentTypeHeader +
                    "\", excepted a JSON response) (response code " + response.responseCode() + ")",
            )
        }

        val responseBody = response.responseBody()
        if (responseBody.isEmpty()) {
            throw ExtractionException("Empty response received")
        }

        return try {
            // 0: search query, 1: search suggestions, 2: tracking data?
            val suggestions = JsonParser.array().from(responseBody).getArray(1)
            Collections.unmodifiableList(
                suggestions.filterIsInstance<JsonArray>()
                    .mapNotNull { suggestion -> suggestion.getString(0) } // 0 is the search suggestion
                    .filter { suggestion -> !Utils.isBlank(suggestion) }, // Filter blank suggestions
            )
        } catch (e: JsonParserException) {
            throw ParsingException("Could not parse JSON response", e)
        }
    }

    private companion object {
        /** Suggestions for a longer text are of no use: the request stays small. */
        private const val MAX_QUERY_LENGTH = 500

        private val HEADERS: Map<String, List<String>> = mapOf(
            "Origin" to listOf("https://www.youtube.com"),
            "Referer" to listOf("https://www.youtube.com"),
        )
    }
}

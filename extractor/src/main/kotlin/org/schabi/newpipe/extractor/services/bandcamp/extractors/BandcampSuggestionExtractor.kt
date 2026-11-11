// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

package org.schabi.newpipe.extractor.services.bandcamp.extractors

import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonParser
import org.schabi.newpipe.extractor.json.JsonParserException
import org.schabi.newpipe.extractor.json.JsonWriter
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.BASE_API_URL
import org.schabi.newpipe.extractor.suggestion.SuggestionExtractor
import java.io.IOException
import java.nio.charset.StandardCharsets

public open class BandcampSuggestionExtractor(service: StreamingService) : SuggestionExtractor(service) {

    @Throws(IOException::class, ExtractionException::class)
    override fun suggestionList(query: String): List<String> {
        val downloader = BandcampExtractorHelper.requireDownloader()

        val body = JsonWriter.string()
            .`object`()
            .value("fan_id", null as String?)
            .value("full_page", false)
            .value("search_filter", "")
            .value("search_text", query)
            .end()
            .done()
            .toByteArray(StandardCharsets.UTF_8)

        return try {
            val fuzzyResults = JsonParser.`object`().from(
                downloader.postWithContentTypeJson(AUTOCOMPLETE_URL, emptyMap(), body).responseBody(),
            )

            fuzzyResults.getObject("auto").getArray("results")
                .asSequence()
                .filterIsInstance<JsonObject>()
                .mapNotNull { it.getString("name") }
                .distinct()
                .toList()
        } catch (_: JsonParserException) {
            emptyList()
        }
    }

    private companion object {
        private const val AUTOCOMPLETE_URL = "$BASE_API_URL/bcsearch_public_api/1/autocomplete_elastic"
    }
}

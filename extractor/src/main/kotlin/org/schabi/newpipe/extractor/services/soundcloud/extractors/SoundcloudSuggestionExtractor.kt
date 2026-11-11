package org.schabi.newpipe.extractor.services.soundcloud.extractors

import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper.SOUNDCLOUD_API_V2_URL
import org.schabi.newpipe.extractor.suggestion.SuggestionExtractor
import org.schabi.newpipe.extractor.utils.Utils
import java.io.IOException

public open class SoundcloudSuggestionExtractor(service: StreamingService) : SuggestionExtractor(service) {

    @Throws(IOException::class, ExtractionException::class)
    override fun suggestionList(query: String): List<String> {
        val url = SOUNDCLOUD_API_V2_URL + "search/queries?q=" + Utils.encodeUrlUtf8(query) +
            "&client_id=" + SoundcloudParsingHelper.clientId() + "&limit=10"
        val response = SoundcloudParsingHelper.requireDownloader()
            .get(url, extractorLocalization)
            .also(SoundcloudParsingHelper::noteRefusedClientId)
            .responseBody()
        val responseJson = SoundcloudParsingHelper.parseObject(response)

        // suggestions without a query are skipped, the Java code returned them as null entries
        return responseJson.getArray("collection")
            .filterIsInstance<JsonObject>()
            .mapNotNull { it.getString("query") }
    }
}

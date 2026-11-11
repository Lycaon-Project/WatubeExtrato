package org.schabi.newpipe.downloader

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonObject
import com.google.gson.JsonSerializer
import org.schabi.newpipe.extractor.localization.Localization

/**
 * The Gson used to write ([RecordingDownloader]) and read ([MockDownloader]) the recorded mocks.
 *
 * The recorded files store a [Localization] as `languageCode` and `countryCode`, while the class
 * keeps the country in a differently named private field: reflection alone would read it back
 * as missing, and no recorded request would be found again. These adapters keep the file format
 * (and so every recorded mock) unchanged.
 */
internal object MockGson {
    private const val LANGUAGE_CODE = "languageCode"
    private const val COUNTRY_CODE = "countryCode"

    fun builder(): GsonBuilder = GsonBuilder()
        .registerTypeAdapter(
            Localization::class.java,
            JsonSerializer<Localization> { localization, _, _ ->
                JsonObject().apply {
                    addProperty(LANGUAGE_CODE, localization.languageCode)
                    if (localization.countryCode.isNotEmpty()) {
                        addProperty(COUNTRY_CODE, localization.countryCode)
                    }
                }
            },
        )
        .registerTypeAdapter(
            Localization::class.java,
            JsonDeserializer { json, _, _ ->
                val obj = json.asJsonObject
                val country = obj.get(COUNTRY_CODE)
                Localization(
                    obj.get(LANGUAGE_CODE).asString,
                    if (country == null || country.isJsonNull) null else country.asString,
                )
            },
        )
}

package org.schabi.newpipe.extractor.suggestion

import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import java.io.IOException

public abstract class SuggestionExtractor(public val service: StreamingService) {
    private var forcedLocalization: Localization? = null
    private var forcedContentCountry: ContentCountry? = null

    @Throws(IOException::class, ExtractionException::class)
    public abstract fun suggestionList(query: String): List<String>

    public val serviceId: Int
        get() = service.serviceId

    // TODO: Create a more general Extractor class

    public fun forceLocalization(localization: Localization?) {
        forcedLocalization = localization
    }

    public fun forceContentCountry(contentCountry: ContentCountry?) {
        forcedContentCountry = contentCountry
    }

    public val extractorLocalization: Localization
        get() = forcedLocalization ?: service.localization

    public val extractorContentCountry: ContentCountry
        get() = forcedContentCountry ?: service.contentCountry
}

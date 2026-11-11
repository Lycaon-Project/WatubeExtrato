package org.schabi.newpipe.extractor.kiosk

import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import java.io.IOException

public open class KioskList(private val service: StreamingService) {

    public fun interface KioskExtractorFactory {
        @Throws(ExtractionException::class, IOException::class)
        public fun createNewKiosk(
            streamingService: StreamingService,
            url: String,
            kioskId: String,
        ): KioskExtractor<*>
    }

    private class KioskEntry(
        val extractorFactory: KioskExtractorFactory,
        val handlerFactory: ListLinkHandlerFactory,
    )

    private val kioskList = HashMap<String, KioskEntry>()
    private var defaultKiosk: String? = null

    private var forcedLocalization: Localization? = null
    private var forcedContentCountry: ContentCountry? = null

    @Throws(Exception::class)
    public fun addKioskEntry(
        extractorFactory: KioskExtractorFactory,
        handlerFactory: ListLinkHandlerFactory,
        id: String,
    ) {
        if (kioskList[id] != null) {
            throw Exception("Kiosk with type $id already exists.")
        }
        kioskList[id] = KioskEntry(extractorFactory, handlerFactory)
    }

    public fun setDefaultKiosk(kioskType: String?) {
        defaultKiosk = kioskType
    }

    @Throws(ExtractionException::class, IOException::class)
    public fun getDefaultKioskExtractor(): KioskExtractor<*>? = getDefaultKioskExtractor(null)

    @Throws(ExtractionException::class, IOException::class)
    public fun getDefaultKioskExtractor(nextPage: Page?): KioskExtractor<*>? =
        getDefaultKioskExtractor(nextPage, NewPipe.getPreferredLocalization())

    @Throws(ExtractionException::class, IOException::class)
    public fun getDefaultKioskExtractor(
        nextPage: Page?,
        localization: Localization,
    ): KioskExtractor<*>? {
        val defaultId = defaultKiosk
        if (!defaultId.isNullOrEmpty()) {
            return getExtractorById(defaultId, nextPage, localization)
        }
        // if not set get any entry
        val first = kioskList.keys.firstOrNull() ?: return null
        return getExtractorById(first, nextPage, localization)
    }

    public val defaultKioskId: String?
        get() = defaultKiosk

    @Throws(ExtractionException::class, IOException::class)
    public fun getExtractorById(kioskId: String, nextPage: Page?): KioskExtractor<*> =
        getExtractorById(kioskId, nextPage, NewPipe.getPreferredLocalization())

    @Throws(ExtractionException::class, IOException::class)
    public fun getExtractorById(
        kioskId: String,
        nextPage: Page?,
        localization: Localization,
    ): KioskExtractor<*> {
        val ke = kioskList[kioskId] ?: throw ExtractionException("No kiosk found with this type")
        val kioskExtractor = ke.extractorFactory.createNewKiosk(
            service,
            ke.handlerFactory.fromId(kioskId).url,
            kioskId,
        )

        forcedLocalization?.let(kioskExtractor::forceLocalization)
        forcedContentCountry?.let(kioskExtractor::forceContentCountry)

        return kioskExtractor
    }

    public val availableKiosks: Set<String>
        get() = kioskList.keys

    @Throws(ExtractionException::class, IOException::class)
    public fun getExtractorByUrl(url: String, nextPage: Page?): KioskExtractor<*> =
        getExtractorByUrl(url, nextPage, NewPipe.getPreferredLocalization())

    /**
     * The extractor of the kiosk [url] links to. The link is untrusted and never quoted in the
     * exception thrown when no kiosk accepts it.
     */
    @Throws(ExtractionException::class, IOException::class)
    public fun getExtractorByUrl(
        url: String,
        nextPage: Page?,
        localization: Localization,
    ): KioskExtractor<*> {
        for (ke in kioskList.values) {
            if (ke.handlerFactory.acceptUrl(url)) {
                return getExtractorById(ke.handlerFactory.getId(url), nextPage, localization)
            }
        }
        throw ExtractionException("Could not find a kiosk that fits to the url")
    }

    public fun getListLinkHandlerFactoryByType(type: String): ListLinkHandlerFactory =
        (kioskList[type] ?: throw NullPointerException("No kiosk found with this type"))
            .handlerFactory

    public fun forceLocalization(localization: Localization?) {
        forcedLocalization = localization
    }

    public fun forceContentCountry(contentCountry: ContentCountry?) {
        forcedContentCountry = contentCountry
    }
}

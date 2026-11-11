package org.schabi.newpipe.extractor

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.linkhandler.LinkHandler
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.localization.TimeAgoParser
import org.schabi.newpipe.extractor.utils.ExtractorLogger
import java.io.IOException

public abstract class Extractor protected constructor(
    /**
     * [StreamingService] currently related to this extractor.
     *
     * Useful for getting other things from a service (like the url handlers for
     * cleaning/accepting/get id from urls).
     */
    public val service: StreamingService,
    linkHandler: LinkHandler,
) {
    private val handler: LinkHandler = linkHandler

    private var forcedLocalization: Localization? = null
    private var forcedContentCountry: ContentCountry? = null

    /** Whether [fetchPage] already ran. */
    @Volatile
    protected var isPageFetched: Boolean = false
        private set

    // called like this to prevent checkstyle errors about "hiding a field"
    public val downloader: Downloader =
        NewPipe.getDownloader() ?: throw NullPointerException("downloader is null")

    /** Built when logging only: the class name and identity of this extractor. */
    private val tag: String
        get() = javaClass.simpleName + "@" + hashCode()

    /**
     * The [LinkHandler] of the current extractor object (e.g. a ChannelExtractor
     * should return a channel url handler).
     */
    public open val linkHandler: LinkHandler
        get() = handler

    /**
     * Fetch the current page.
     *
     * @throws IOException         if the page can not be loaded
     * @throws ExtractionException if the pages content is not understood
     */
    // two threads asking for the same page fetch it once
    @Synchronized
    @Throws(IOException::class, ExtractionException::class)
    public fun fetchPage() {
        ExtractorLogger.d(tag, "base fetchPage called")
        if (isPageFetched) {
            ExtractorLogger.d(tag, "Page already fetched; returning")
            return
        }
        onFetchPage(downloader)
        isPageFetched = true
    }

    protected fun assertPageFetched() {
        check(isPageFetched) { "Page is not fetched. Make sure you call fetchPage()" }
    }

    /**
     * Fetch the current page.
     *
     * @param downloader the downloader to use
     * @throws IOException         if the page can not be loaded
     * @throws ExtractionException if the pages content is not understood
     */
    @Throws(IOException::class, ExtractionException::class)
    public abstract fun onFetchPage(downloader: Downloader)

    @get:Throws(ParsingException::class)
    public open val id: String
        get() = handler.id

    /**
     * Get the name
     *
     * @return the name
     * @throws ParsingException if the name cannot be extracted
     */
    @get:Throws(ParsingException::class)
    public abstract val name: String

    @get:Throws(ParsingException::class)
    public open val originalUrl: String
        get() = handler.originalUrl

    @get:Throws(ParsingException::class)
    public open val url: String
        get() = handler.url

    @get:Throws(ParsingException::class)
    public open val baseUrl: String
        get() = handler.baseUrl

    public val serviceId: Int
        get() = service.serviceId

    // ////////////////////////////////////////////////////////////////////////
    // Localization
    // ////////////////////////////////////////////////////////////////////////

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

    public val timeAgoParser: TimeAgoParser
        get() = service.getTimeAgoParser(extractorLocalization)

    override fun toString(): String = javaClass.simpleName
}

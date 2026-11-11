package org.schabi.newpipe.extractor.linkhandler

import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.utils.Utils

public abstract class ListLinkHandlerFactory : LinkHandlerFactory() {

    ///////////////////////////////////
    // To Override
    ///////////////////////////////////

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    public abstract fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
    ): String

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    public open fun getUrl(
        id: String?,
        contentFilter: List<String>?,
        sortFilter: String?,
        baseUrl: String?,
    ): String = getUrl(id, contentFilter, sortFilter)

    ///////////////////////////////////
    // Logic
    ///////////////////////////////////

    @Throws(ParsingException::class)
    override fun fromUrl(url: String?): ListLinkHandler {
        if (url == null) {
            throw NullPointerException("URL may not be null")
        }
        Utils.checkUrlLength(url)
        // Non-null input gives a non-null result; the fallback only satisfies the nullable signature
        val polishedUrl = Utils.followGoogleRedirectIfNeeded(url) ?: url
        val baseUrl = Utils.getBaseUrl(polishedUrl)
        return fromUrl(polishedUrl, baseUrl)
    }

    @Throws(ParsingException::class)
    override fun fromUrl(url: String, baseUrl: String?): ListLinkHandler =
        ListLinkHandler(super.fromUrl(url, baseUrl))

    @Throws(ParsingException::class)
    override fun fromId(id: String): ListLinkHandler = ListLinkHandler(super.fromId(id))

    @Throws(ParsingException::class)
    override fun fromId(id: String, baseUrl: String?): ListLinkHandler =
        ListLinkHandler(super.fromId(id, baseUrl))

    @Throws(ParsingException::class)
    public open fun fromQuery(
        id: String,
        contentFilters: List<String>,
        sortFilter: String,
    ): ListLinkHandler {
        val url = getUrl(id, contentFilters, sortFilter)
        return ListLinkHandler(url, url, id, contentFilters, sortFilter)
    }

    @Throws(ParsingException::class)
    public open fun fromQuery(
        id: String,
        contentFilters: List<String>,
        sortFilter: String,
        baseUrl: String?,
    ): ListLinkHandler {
        val url = getUrl(id, contentFilters, sortFilter, baseUrl)
        return ListLinkHandler(url, url, id, contentFilters, sortFilter)
    }

    /**
     * For making ListLinkHandlerFactory compatible with LinkHandlerFactory we need to override
     * this, however it should not be overridden by the actual implementation.
     *
     * @return the url corresponding to id without any filters applied
     */
    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(id: String?): String = getUrl(id, emptyList(), "")

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(id: String?, baseUrl: String?): String =
        getUrl(id, emptyList(), "", baseUrl)

    /**
     * Will returns content filter the corresponding extractor can handle like "channels",
     * "videos", "music", etc.
     *
     * @return filter that can be applied when building a query for getting a list
     */
    public open val availableContentFilter: Array<String>
        get() = emptyArray()

    /**
     * Will returns sort filter the corresponding extractor can handle like "A-Z", "oldest first",
     * "size", etc.
     *
     * @return filter that can be applied when building a query for getting a list
     */
    public open val availableSortFilter: Array<String>
        get() = emptyArray()
}

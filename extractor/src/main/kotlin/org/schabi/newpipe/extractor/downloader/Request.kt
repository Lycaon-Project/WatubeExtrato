package org.schabi.newpipe.extractor.downloader

import org.schabi.newpipe.extractor.localization.Localization
import java.util.Collections

/**
 * An object that holds request information used when [executing][Downloader.execute] a request.
 *
 * A request carries cookies, tokens and bodies: [toString] only ever shows the HTTP method.
 */
public open class Request(
    private val httpMethod: String,
    private val url: String,
    headers: Map<String, List<String>>?,
    private val dataToSend: ByteArray?,
    private val localization: Localization?,
    automaticLocalizationHeader: Boolean,
) {
    /**
     * Copied, and frozen down to the value lists, so that a caller can neither change the headers
     * of a request already handed to the downloader nor be surprised by changes made by it.
     */
    private val headers: Map<String, List<String>> =
        freezeHeaders(headers, if (automaticLocalizationHeader) localization else null)

    private constructor(builder: Builder) : this(
        builder.httpMethod ?: throw NullPointerException("Request's httpMethod is null"),
        builder.url ?: throw NullPointerException("Request's url is null"),
        builder.headers,
        builder.dataToSend,
        builder.localization,
        builder.automaticLocalizationHeader,
    )

    /** A http method (i.e. `GET, POST, HEAD`). */
    public fun httpMethod(): String = httpMethod

    /** The URL that is pointing to the wanted resource. */
    public fun url(): String = url

    /**
     * A list of headers that will be used in the request.
     *
     * Any default headers that the implementation may have, **should** be overridden by these.
     */
    public fun headers(): Map<String, List<String>> = headers

    /**
     * An optional byte array that will be sent when doing the request, very commonly used in
     * `POST` requests.
     *
     * The implementation should make note of some recommended headers
     * (for example, `Content-Length` in a post request).
     */
    public fun dataToSend(): ByteArray? = dataToSend

    /**
     * A localization object that should be used when executing a request.
     *
     * Usually the `Accept-Language` will be set to this value (a helper
     * method to do this easily: [Request.getHeadersFromLocalization]).
     */
    public fun localization(): Localization? = localization

    public class Builder {
        internal var httpMethod: String? = null
            private set
        internal var url: String? = null
            private set
        internal val headers: MutableMap<String, List<String>> = LinkedHashMap()
        internal var dataToSend: ByteArray? = null
            private set
        internal var localization: Localization? = null
            private set
        internal var automaticLocalizationHeader: Boolean = true
            private set

        /** A http method (i.e. `GET, POST, HEAD`). */
        public fun httpMethod(httpMethodToSet: String?): Builder = apply {
            httpMethod = httpMethodToSet
        }

        /** The URL that is pointing to the wanted resource. */
        public fun url(urlToSet: String?): Builder = apply {
            url = urlToSet
        }

        /**
         * A list of headers that will be used in the request.
         *
         * Any default headers that the implementation may have, **should** be overridden by
         * these.
         */
        public fun headers(headersToSet: Map<String, List<String>>?): Builder = apply {
            headers.clear()
            if (headersToSet != null) {
                headers.putAll(headersToSet)
            }
        }

        /**
         * An optional byte array that will be sent when doing the request, very commonly used in
         * `POST` requests.
         *
         * The implementation should make note of some recommended headers
         * (for example, `Content-Length` in a post request).
         */
        public fun dataToSend(dataToSendToSet: ByteArray?): Builder = apply {
            dataToSend = dataToSendToSet
        }

        /**
         * A localization object that should be used when executing a request.
         *
         * Usually the `Accept-Language` will be set to this value (a helper
         * method to do this easily: [Request.getHeadersFromLocalization]).
         */
        public fun localization(localizationToSet: Localization?): Builder = apply {
            localization = localizationToSet
        }

        /** If localization headers should automatically be included in the request. */
        public fun automaticLocalizationHeader(automaticLocalizationHeaderToSet: Boolean): Builder =
            apply { automaticLocalizationHeader = automaticLocalizationHeaderToSet }

        public fun build(): Request = Request(this)

        // ////////////////////////////////////////////////////////////////////////
        // Http Methods Utils
        // ////////////////////////////////////////////////////////////////////////

        public fun get(urlToSet: String?): Builder = apply {
            httpMethod = "GET"
            url = urlToSet
        }

        public fun head(urlToSet: String?): Builder = apply {
            httpMethod = "HEAD"
            url = urlToSet
        }

        public fun post(urlToSet: String?, dataToSendToSet: ByteArray?): Builder = apply {
            httpMethod = "POST"
            url = urlToSet
            dataToSend = dataToSendToSet
        }

        // ////////////////////////////////////////////////////////////////////////
        // Additional Headers Utils
        // ////////////////////////////////////////////////////////////////////////

        public fun setHeaders(headerName: String, headerValueList: List<String>): Builder = apply {
            headers.remove(headerName)
            headers[headerName] = headerValueList
        }

        /**
         * Adds [headerValueList] after the values already set for [headerName]. The values are
         * gathered in a new list: the one given earlier may be immutable (e.g. from [setHeader]).
         */
        public fun addHeaders(headerName: String, headerValueList: List<String>): Builder = apply {
            val current = headers[headerName]
            headers[headerName] = if (current == null) headerValueList else current + headerValueList
        }

        public fun setHeader(headerName: String, headerValue: String): Builder =
            setHeaders(headerName, listOf(headerValue))

        public fun addHeader(headerName: String, headerValue: String): Builder =
            addHeaders(headerName, listOf(headerValue))
    }

    // ////////////////////////////////////////////////////////////////////////
    // Generated
    // ////////////////////////////////////////////////////////////////////////

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        other as Request
        return httpMethod == other.httpMethod &&
            url == other.url &&
            headers == other.headers &&
            dataToSend.contentEquals(other.dataToSend) &&
            localization == other.localization
    }

    override fun hashCode(): Int {
        var result = arrayOf<Any?>(httpMethod, url, headers, localization).contentHashCode()
        result = HASH_MULTIPLIER * result + dataToSend.contentHashCode()
        return result
    }

    /** Never shows the URL, the headers (cookies, tokens) nor the body. */
    override fun toString(): String = "Request[$httpMethod]"

    public companion object {
        private const val HASH_MULTIPLIER = 31

        @JvmStatic
        public fun newBuilder(): Builder = Builder()

        /** [headers] then the localization ones, in a frozen copy. */
        private fun freezeHeaders(
            headers: Map<String, List<String>>?,
            localization: Localization?,
        ): Map<String, List<String>> {
            val localizationHeaders = getHeadersFromLocalization(localization)
            val frozen = LinkedHashMap<String, List<String>>(
                (headers?.size ?: 0) + localizationHeaders.size,
            )
            headers?.forEach { (name, values) -> frozen[name] = values.toFrozenList() }
            localizationHeaders.forEach { (name, values) -> frozen[name] = values.toFrozenList() }
            return Collections.unmodifiableMap(frozen)
        }

        private fun List<String>.toFrozenList(): List<String> =
            Collections.unmodifiableList(ArrayList(this))

        // ////////////////////////////////////////////////////////////////////////
        // Utils
        // ////////////////////////////////////////////////////////////////////////

        /**
         * The `Accept-Language` header for [localization]. Its codes are validated by
         * [Localization], so they cannot break out of the header value.
         */
        @JvmStatic
        public fun getHeadersFromLocalization(
            localization: Localization?,
        ): Map<String, List<String>> {
            if (localization == null) {
                return emptyMap()
            }

            val languageCode = localization.languageCode
            val value = if (localization.countryCode.isEmpty()) {
                languageCode
            } else {
                "${localization.localizationCode}, $languageCode;q=0.9"
            }
            return Collections.singletonMap("Accept-Language", listOf(value))
        }
    }
}

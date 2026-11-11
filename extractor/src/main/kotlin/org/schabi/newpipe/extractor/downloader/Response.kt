package org.schabi.newpipe.extractor.downloader

import org.schabi.newpipe.extractor.exceptions.HttpResponseException
import java.util.function.Function

/**
 * A Data class used to hold the results from requests made by the Downloader implementation.
 *
 * A response may carry cookies and personal data: [toString] only ever shows the status code.
 */
public open class Response(
    private val responseCode: Int,
    private val responseMessage: String?,
    responseHeaders: Map<String, List<String>>?,
    responseBody: String?,
    latestUrl: String?,
) {
    private val responseHeaders: Map<String, List<String>> = responseHeaders ?: emptyMap()
    private val responseBody: String = responseBody ?: ""
    private val latestUrl: String = latestUrl ?: ""

    public fun responseCode(): Int = responseCode

    public fun responseMessage(): String? = responseMessage

    public fun responseHeaders(): Map<String, List<String>> = responseHeaders

    public fun responseBody(): String = responseBody

    /**
     * Used for detecting a possible redirection, limited to the latest one.
     *
     * @return latest url known right before this response object was created, or an empty
     * string when the downloader did not give it
     */
    public fun latestUrl(): String = latestUrl

    // ////////////////////////////////////////////////////////////////////////
    // Utils
    // ////////////////////////////////////////////////////////////////////////

    /**
     * For easy access to some header value that (usually) don't repeat itself.
     *
     * For getting all the values associated to the header, use [responseHeaders] (e.g.
     * `Set-Cookie`).
     *
     * @param name the name of the header
     * @return the first value assigned to this header
     */
    public fun getHeader(name: String): String? {
        for ((key, values) in responseHeaders) {
            // Java maps may hold a null key (the status line of HttpURLConnection): the nullable
            // equals() handles it. It compares char by char, independently of the default locale
            if (key.equals(name, ignoreCase = true) && values.isNotEmpty()) {
                return values[0]
            }
        }
        return null
    }

    /**
     * Ensure the response code is 2xx
     * @return this `Response`
     * @throws HttpResponseException if the response code is not 2xx
     */
    @Throws(HttpResponseException::class)
    public fun ensureSuccessResponseCode(): Response = ensureResponseCodeInRange(200, 299)

    /**
     * Ensure the response code is 3xx
     * @return this `Response`
     * @throws HttpResponseException if the response code is not 3xx
     */
    @Throws(HttpResponseException::class)
    public fun ensureRedirectResponseCode(): Response = ensureResponseCodeInRange(300, 399)

    /**
     * Ensure the response code is not 4xx or 5xx
     * @return this `Response`
     * @throws HttpResponseException if the response code is client or server error
     */
    @Throws(HttpResponseException::class)
    public fun ensureResponseCodeIsNotError(): Response = ensureResponseCodeNotInRange(400, 599)

    /**
     * Ensure the HTTP response code is within range of min and max inclusive
     * @return this Response
     * @throws HttpResponseException if the code is outside the range
     */
    @Throws(HttpResponseException::class)
    public fun ensureResponseCodeInRange(min: Int, max: Int): Response {
        require(min <= max) { RANGE_ERROR }
        if (responseCode < min || responseCode > max) {
            throw HttpResponseException(this)
        }
        return this
    }

    @Throws(HttpResponseException::class)
    public fun ensureResponseCodeNotInRange(
        min: Int,
        max: Int,
        errorSupplier: Function<Response, HttpResponseException>,
    ): Response {
        require(min <= max) { RANGE_ERROR }
        if (responseCode in min..max) {
            throw errorSupplier.apply(this)
        }
        return this
    }

    @Throws(HttpResponseException::class)
    public fun ensureResponseCodeNotInRange(min: Int, max: Int): Response =
        ensureResponseCodeNotInRange(min, max, DEFAULT_ERROR)

    /**
     * Throw exception if response code is a 4xx client error
     * @return this `Response`
     * @throws HttpResponseException if the response code is 4xx
     */
    @Throws(HttpResponseException::class)
    public fun throwIfClientError(errorSupplier: Function<Response, HttpResponseException>): Response =
        throwIfResponseCodeInRange(400, 499, errorSupplier)

    /**
     * Throw exception if response code is a 4xx client error
     * @return this `Response`
     * @throws HttpResponseException if the response code is 4xx
     */
    @Throws(HttpResponseException::class)
    public fun throwIfClientError(): Response = throwIfClientError(DEFAULT_ERROR)

    /**
     * Throw exception if response code is a 5xx server error
     * @return this `Response`
     * @throws HttpResponseException if the response code is 5xx
     */
    @Throws(HttpResponseException::class)
    public fun throwIfServerError(errorSupplier: Function<Response, HttpResponseException>): Response =
        throwIfResponseCodeInRange(500, 599, errorSupplier)

    /**
     * Throw exception if response code is a 5xx server error
     * @return this `Response`
     * @throws HttpResponseException if the response code is 5xx
     */
    @Throws(HttpResponseException::class)
    public fun throwIfServerError(): Response = throwIfServerError(DEFAULT_ERROR)

    @Throws(HttpResponseException::class)
    public fun throwIfResponseCode(
        errorCode: Int,
        errorSupplier: Function<Response, HttpResponseException>,
    ): Response {
        if (responseCode == errorCode) {
            throw errorSupplier.apply(this)
        }
        return this
    }

    @Throws(HttpResponseException::class)
    public fun throwIfResponseCode(errorCode: Int): Response =
        throwIfResponseCode(errorCode, DEFAULT_ERROR)

    @Throws(HttpResponseException::class)
    public fun throwIfResponseCodeInRange(
        min: Int,
        max: Int,
        errorSupplier: Function<Response, HttpResponseException>,
    ): Response = ensureResponseCodeNotInRange(min, max, errorSupplier)

    @Throws(HttpResponseException::class)
    public fun throwIfResponseCodeInRange(min: Int, max: Int): Response =
        throwIfResponseCodeInRange(min, max, DEFAULT_ERROR)

    /** Never shows the URL, the headers (cookies) nor the body. */
    override fun toString(): String = "Response[$responseCode]"

    private companion object {
        private const val RANGE_ERROR = "min must be less than max"

        private val DEFAULT_ERROR =
            Function<Response, HttpResponseException> { HttpResponseException(it) }
    }
}

package org.schabi.newpipe.extractor.downloader

import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.utils.SecureUrls
import java.io.IOException

/**
 * A base for downloader implementations that NewPipe will use
 * to download needed resources during extraction.
 */
public abstract class Downloader {

    /**
     * Do a GET request to get the resource that the url is pointing to.
     *
     * This method calls [get] with the default preferred localization. It should only be used
     * when the resource that will be fetched won't be affected by the localization.
     *
     * @param url the URL that is pointing to the wanted resource
     * @return the result of the GET request
     */
    @Throws(IOException::class, ReCaptchaException::class)
    public open fun get(url: String): Response =
        get(url, null, NewPipe.getPreferredLocalization())

    /**
     * Do a GET request to get the resource that the url is pointing to.
     *
     * It will set the `Accept-Language` header to the language of the localization parameter.
     *
     * @param url          the URL that is pointing to the wanted resource
     * @param localization the source of the value of the `Accept-Language` header
     * @return the result of the GET request
     */
    @Throws(IOException::class, ReCaptchaException::class)
    public open fun get(url: String, localization: Localization?): Response =
        get(url, null, localization)

    /**
     * Do a GET request with the specified headers.
     *
     * @param url     the URL that is pointing to the wanted resource
     * @param headers a list of headers that will be used in the request.
     * Any default headers **should** be overridden by these.
     * @return the result of the GET request
     */
    @Throws(IOException::class, ReCaptchaException::class)
    public open fun get(url: String, headers: Map<String, List<String>>?): Response =
        get(url, headers, NewPipe.getPreferredLocalization())

    /**
     * Do a GET request with the specified headers.
     *
     * It will set the `Accept-Language` header to the language of the localization parameter.
     *
     * @param url          the URL that is pointing to the wanted resource
     * @param headers      a list of headers that will be used in the request.
     * Any default headers **should** be overridden by these.
     * @param localization the source of the value of the `Accept-Language` header
     * @return the result of the GET request
     */
    @Throws(IOException::class, ReCaptchaException::class)
    public open fun get(
        url: String,
        headers: Map<String, List<String>>?,
        localization: Localization?,
    ): Response = checkedExecute(
        Request.newBuilder()
            .get(url)
            .headers(headers)
            .localization(localization)
            .build(),
    )

    /**
     * Do a HEAD request.
     *
     * @param url the URL that is pointing to the wanted resource
     * @return the result of the HEAD request
     */
    @Throws(IOException::class, ReCaptchaException::class)
    public open fun head(url: String): Response = head(url, null)

    /**
     * Do a HEAD request with the specified headers.
     *
     * @param url     the URL that is pointing to the wanted resource
     * @param headers a list of headers that will be used in the request.
     * Any default headers **should** be overridden by these.
     * @return the result of the HEAD request
     */
    @Throws(IOException::class, ReCaptchaException::class)
    public open fun head(url: String, headers: Map<String, List<String>>?): Response =
        checkedExecute(
            Request.newBuilder()
                .head(url)
                .headers(headers)
                .build(),
        )

    /**
     * Do a POST request with the specified headers, sending the data array.
     *
     * @param url        the URL that is pointing to the wanted resource
     * @param headers    a list of headers that will be used in the request.
     * Any default headers **should** be overridden by these.
     * @param dataToSend byte array that will be sent when doing the request.
     * @return the result of the POST request
     */
    @Throws(IOException::class, ReCaptchaException::class)
    public open fun post(
        url: String,
        headers: Map<String, List<String>>?,
        dataToSend: ByteArray?,
    ): Response = post(url, headers, dataToSend, NewPipe.getPreferredLocalization())

    /**
     * Do a POST request with the specified headers, sending the data array.
     *
     * It will set the `Accept-Language` header to the language of the localization parameter.
     *
     * @param url          the URL that is pointing to the wanted resource
     * @param headers      a list of headers that will be used in the request.
     * Any default headers **should** be overridden by these.
     * @param dataToSend   byte array that will be sent when doing the request.
     * @param localization the source of the value of the `Accept-Language` header
     * @return the result of the POST request
     */
    @Throws(IOException::class, ReCaptchaException::class)
    public open fun post(
        url: String,
        headers: Map<String, List<String>>?,
        dataToSend: ByteArray?,
        localization: Localization?,
    ): Response = checkedExecute(
        Request.newBuilder()
            .post(url, dataToSend)
            .headers(headers)
            .localization(localization)
            .build(),
    )

    /**
     * Convenient method to send a POST request using the specified value of the
     * `Content-Type` header with a given [Localization].
     *
     * @param url          the URL that is pointing to the wanted resource
     * @param headers      a list of headers that will be used in the request.
     * Any default headers **should** be overridden by these.
     * @param dataToSend   byte array that will be sent when doing the request.
     * @param localization the source of the value of the `Accept-Language` header
     * @param contentType  the mime type of the body sent, which will be set as the value of the
     * `Content-Type` header
     * @return the result of the POST request
     * @see post
     */
    @Throws(IOException::class, ReCaptchaException::class)
    public open fun postWithContentType(
        url: String,
        headers: Map<String, List<String>>?,
        dataToSend: ByteArray?,
        localization: Localization?,
        contentType: String,
    ): Response {
        val actualHeaders = HashMap<String, List<String>>()
        if (headers != null) {
            actualHeaders.putAll(headers)
        }
        actualHeaders[CONTENT_TYPE] = listOf(contentType)
        return post(url, actualHeaders, dataToSend, localization)
    }

    /**
     * Convenient method to send a POST request using the specified value of the
     * `Content-Type` header.
     *
     * @param url         the URL that is pointing to the wanted resource
     * @param headers     a list of headers that will be used in the request.
     * Any default headers **should** be overridden by these.
     * @param dataToSend  byte array that will be sent when doing the request.
     * @param contentType the mime type of the body sent, which will be set as the value of the
     * `Content-Type` header
     * @return the result of the POST request
     * @see post
     */
    @Throws(IOException::class, ReCaptchaException::class)
    public open fun postWithContentType(
        url: String,
        headers: Map<String, List<String>>?,
        dataToSend: ByteArray?,
        contentType: String,
    ): Response = postWithContentType(
        url,
        headers,
        dataToSend,
        NewPipe.getPreferredLocalization(),
        contentType,
    )

    /**
     * Convenient method to send a POST request the JSON mime type as the value of the
     * `Content-Type` header with a given [Localization].
     *
     * @param url          the URL that is pointing to the wanted resource
     * @param headers      a list of headers that will be used in the request.
     * Any default headers **should** be overridden by these.
     * @param dataToSend   byte array that will be sent when doing the request.
     * @param localization the source of the value of the `Accept-Language` header
     * @return the result of the POST request
     * @see post
     */
    @Throws(IOException::class, ReCaptchaException::class)
    public open fun postWithContentTypeJson(
        url: String,
        headers: Map<String, List<String>>?,
        dataToSend: ByteArray?,
        localization: Localization?,
    ): Response = postWithContentType(url, headers, dataToSend, localization, JSON_CONTENT_TYPE)

    /**
     * Convenient method to send a POST request the JSON mime type as the value of the
     * `Content-Type` header.
     *
     * @param url         the URL that is pointing to the wanted resource
     * @param headers     a list of headers that will be used in the request.
     * Any default headers **should** be overridden by these.
     * @param dataToSend  byte array that will be sent when doing the request.
     * @return the result of the POST request
     * @see post
     */
    @Throws(IOException::class, ReCaptchaException::class)
    public open fun postWithContentTypeJson(
        url: String,
        headers: Map<String, List<String>>?,
        dataToSend: ByteArray?,
    ): Response = postWithContentTypeJson(
        url,
        headers,
        dataToSend,
        NewPipe.getPreferredLocalization(),
    )

    /**
     * Do a request using the specified [Request] object.
     *
     * @return the result of the request
     */
    @Throws(IOException::class, ReCaptchaException::class)
    public abstract fun execute(request: Request): Response

    /**
     * [execute], once checked that a request to YouTube or Google goes over https (see
     * [SecureUrls.checkRequest]).
     */
    @Throws(IOException::class, ReCaptchaException::class)
    private fun checkedExecute(request: Request): Response {
        SecureUrls.checkRequest(request.url())
        return execute(request)
    }

    override fun toString(): String = javaClass.simpleName

    private companion object {
        private const val CONTENT_TYPE = "Content-Type"
        private const val JSON_CONTENT_TYPE = "application/json"
    }
}

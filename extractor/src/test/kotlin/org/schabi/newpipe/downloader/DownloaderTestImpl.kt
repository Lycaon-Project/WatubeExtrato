package org.schabi.newpipe.downloader

import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.downloader.ratelimiting.RateLimitedClientWrapper
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.util.concurrent.TimeUnit

class DownloaderTestImpl private constructor(builder: OkHttpClient.Builder) : Downloader() {

    private val clientWrapper = RateLimitedClientWrapper(
        builder
            .readTimeout(30, TimeUnit.SECONDS)
            // Required for certain services
            // For example Bandcamp otherwise fails on Windows with Java 17+
            // as their Fastly-CDN returns 403
            .connectionSpecs(listOf(ConnectionSpec.RESTRICTED_TLS))
            .build(),
    )

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBody = dataToSend?.toRequestBody()

        val requestBuilder = okhttp3.Request.Builder()
            .method(httpMethod, requestBody)
            .url(url)
            .addHeader("User-Agent", USER_AGENT)

        headers.forEach { (headerName, headerValueList) ->
            requestBuilder.removeHeader(headerName)
            headerValueList.forEach { headerValue ->
                requestBuilder.addHeader(headerName, headerValue)
            }
        }

        clientWrapper.executeRequestWithLimit(requestBuilder.build()).use { response ->
            if (response.code == HTTP_TOO_MANY_REQUESTS) {
                throw ReCaptchaException("reCaptcha Challenge requested", url)
            }

            val responseBodyToReturn = response.body.use { body -> body.string() }

            return Response(
                response.code,
                response.message,
                response.headers.toMultimap(),
                responseBodyToReturn,
                response.request.url.toString(),
            )
        }
    }

    companion object {
        private const val HTTP_TOO_MANY_REQUESTS = 429

        /**
         * Should be the latest Firefox ESR version.
         */
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:153.0) Gecko/20100101 Firefox/153.0"

        private var instance: DownloaderTestImpl? = null

        /**
         * It's recommended to call exactly once in the entire lifetime of the application.
         *
         * @param builder if null, default builder will be used
         * @return a new instance of [DownloaderTestImpl]
         */
        @JvmStatic
        fun init(builder: OkHttpClient.Builder?): DownloaderTestImpl {
            val created = DownloaderTestImpl(builder ?: OkHttpClient.Builder())
            instance = created
            return created
        }

        @JvmStatic
        fun getInstance(): DownloaderTestImpl = instance ?: init(null)
    }
}

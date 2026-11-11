package org.schabi.newpipe.downloader.ratelimiting

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.schabi.newpipe.downloader.ratelimiting.limiter.RateLimiter
import java.io.IOException
import java.net.ProtocolException
import java.time.Duration

open class RateLimitedClientWrapper(private val client: OkHttpClient) {

    private val hostRateLimiters = LinkedHashMap<String, RateLimiter>()

    protected open fun getRateLimiterFor(request: Request): RateLimiter =
        hostRateLimiters.getOrPut(request.url.host) {
            FORCED_RATE_LIMITERS.firstOrNull { it.first(request.url.host) }?.second
                // Default rate limiter per domain
                ?: RateLimiter.create(5.0, Duration.ofSeconds(3))
        }

    @Throws(IOException::class)
    fun executeRequestWithLimit(request: Request): Response {
        var cause: Exception? = null
        for (tries in 1..MAX_TRIES) {
            try {
                val rateLimitedSec = getRateLimiterFor(request).acquire()
                if (DEBUG_PRINT) {
                    println("[RATE-LIMIT] Waited ${rateLimitedSec}s for ${request.url}")
                }

                val response = client.newCall(request).execute()
                if (response.code != HTTP_TOO_MANY_REQUESTS) {
                    return response
                }
                cause = IllegalStateException("HTTP 429 - Too many requests")
            } catch (pre: ProtocolException) {
                if (pre.message?.startsWith("Too many follow-up") != true) { // -> Too many requests
                    throw pre
                }
                cause = pre
            }

            val waitMs = REQUEST_RATE_LIMITED_WAIT_MS * tries
            if (DEBUG_PRINT) {
                println("[TOO-MANY-REQUESTS] Waiting ${waitMs}ms for ${request.url}")
            }
            try {
                Thread.sleep(waitMs.toLong())
            } catch (iex: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        throw IllegalStateException("Retrying/Rate-limiting for ${request.url} failed", cause)
    }

    private companion object {
        val DEBUG_PRINT =
            "1" == System.getProperty(
                "rateLimitClientDebugPrint",
                System.getenv("RATE_LIMIT_CLIENT_DEBUG_PRINT"),
            )

        const val MAX_TRIES = 3
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val REQUEST_RATE_LIMITED_WAIT_MS = 5_000

        val FORCED_RATE_LIMITERS: List<Pair<(String) -> Boolean, RateLimiter>> = listOf(
            Pair({ host: String -> host.endsWith("youtube.com") }, RateLimiter.create(1.6, Duration.ofSeconds(1))),
            Pair({ host: String -> host.endsWith("bandcamp.com") }, RateLimiter.create(1.6, Duration.ofSeconds(1))),
        )
    }
}

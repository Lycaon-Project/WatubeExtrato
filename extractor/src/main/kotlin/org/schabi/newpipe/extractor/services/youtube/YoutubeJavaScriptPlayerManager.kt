package org.schabi.newpipe.extractor.services.youtube

import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.utils.JavaScript
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Manage the extraction and the usage of YouTube's player JavaScript needed data in the YouTube
 * service.
 *
 * YouTube restricts streaming URLs of its HTML5 clients with a signature deobfuscation function
 * for some contents and a throttling parameter deobfuscation one for all contents.
 *
 * This class provides access to methods which allow to deobfuscate streaming URLs' signature
 * and/or throttling parameter of HTML5 clients.
 *
 * Streams are extracted on several threads at once. What one player gives (its code, its two
 * functions, why one of them could not be extracted) is kept together in one immutable
 * [PlayerState], replaced as a whole: a function is never run with the code of another player,
 * even while [clearAllCaches] runs.
 */
public object YoutubeJavaScriptPlayerManager {

    /**
     * How long the regular expressions can search the player for its deobfuscation functions.
     * They take well under a second; the limit only stops one which would backtrack without end
     * on a new player, keeping the extraction thread busy for minutes.
     */
    @JvmField
    internal val PLAYER_CODE_REGEX_BUDGET: Duration = Duration.ofSeconds(10)

    // Each streaming URL brings its own throttling parameter, so the map is bounded. It belongs
    // to the current player: it is emptied when the player changes.
    private const val MAX_CACHED_THROTTLING_PARAMETERS = 512
    private val CACHED_THROTTLING_PARAMETERS = ConcurrentHashMap<String, String>()

    /**
     * A function that could not be extracted is not looked for again before this delay: the
     * search runs on megabytes of code, but a failure caused by a bad download must not last
     * until the app is restarted.
     */
    private val FAILURE_RETRY_DELAY_NANOS = TimeUnit.MINUTES.toNanos(1)

    private val state = AtomicReference<PlayerState?>(null)

    /** Why a function could not be extracted, and when. */
    private class Failure(val exception: ParsingException) {
        private val at = System.nanoTime()

        val isRecent: Boolean get() = System.nanoTime() - at < FAILURE_RETRY_DELAY_NANOS
    }

    /**
     * What one player gave. [player] identifies it across copies. Its [code], megabytes of
     * JavaScript, is let go once both functions are extracted: nothing else needs it.
     */
    private data class PlayerState(
        val player: Any,
        val code: String?,
        val signatureFunction: String? = null,
        val throttlingFunction: String? = null,
        val signatureFailure: Failure? = null,
        val throttlingFailure: Failure? = null,
    ) {
        fun withoutUnneededCode(): PlayerState =
            if (signatureFunction != null && throttlingFunction != null) copy(code = null) else this
    }

    /**
     * Deobfuscate a signature of a streaming URL using its corresponding JavaScript base player's
     * function.
     *
     * Obfuscated signatures are only present on streaming URLs of some videos with HTML5 clients.
     *
     * @param videoId             the video ID used to get the JavaScript base player file (an
     *                            empty one can be passed, even it is not recommended in order to
     *                            spoof better official YouTube clients)
     * @param obfuscatedSignature the obfuscated signature of a streaming URL
     * @return the deobfuscated signature
     * @throws ParsingException if the extraction of the base JavaScript player file or the
     * signature deobfuscation function failed
     */
    @JvmStatic
    @Throws(ParsingException::class)
    public fun deobfuscateSignature(videoId: String, obfuscatedSignature: String): String {
        val function = function(
            videoId,
            known = { it.signatureFunction },
            failure = { it.signatureFailure },
            extract = YoutubeSignatureUtils::getDeobfuscationCode,
            found = { current, function -> current.copy(signatureFunction = function, signatureFailure = null) },
            failed = { current, failure -> current.copy(signatureFailure = failure) },
        )

        try {
            return JavaScript.run(
                function, YoutubeSignatureUtils.DEOBFUSCATION_FUNCTION_NAME, obfuscatedSignature,
            )
        } catch (e: Exception) {
            // This shouldn't happen as the function validity is checked when it is extracted
            throw ParsingException(
                "Could not run signature parameter deobfuscation JavaScript function", e,
            )
        }
    }

    /**
     * Return a streaming URL with the throttling parameter of a given one deobfuscated, if it is
     * present, using its corresponding JavaScript base player's function.
     *
     * The throttling parameter is present on all streaming URLs of HTML5 clients.
     *
     * If it is not given or deobfuscated, speeds will be throttled to a very slow speed (around 50
     * KB/s) and some streaming URLs could even lead to invalid HTTP responses such a 403 one.
     *
     * As throttling parameters can be common between multiple streaming URLs of the same player
     * response, deobfuscated parameters are cached with their obfuscated variant, in order to
     * improve performance with multiple calls of this method having the same obfuscated throttling
     * parameter.
     *
     * The cache can be cleared using [clearThrottlingParametersCache] or [clearAllCaches].
     *
     * @param videoId      the video ID used to get the JavaScript base player file (an empty one
     *                     can be passed, even it is not recommended in order to spoof better
     *                     official YouTube clients)
     * @param streamingUrl a streaming URL
     * @return the original streaming URL if it has no throttling parameter or a URL with a
     * deobfuscated throttling parameter
     * @throws ParsingException if the extraction of the base JavaScript player file or the
     * throttling parameter deobfuscation function failed
     */
    @JvmStatic
    @Throws(ParsingException::class)
    public fun getUrlWithThrottlingParameterDeobfuscated(
        videoId: String,
        streamingUrl: String,
    ): String {
        val obfuscatedThrottlingParameter =
            YoutubeThrottlingParameterUtils.getThrottlingParameterFromStreamingUrl(streamingUrl)
                // If the throttling parameter is not present, return the original streaming URL
                ?: return streamingUrl

        // Do not use the containsKey method of the Map interface in order to avoid a double
        // element search, and so to improve performance
        val cacheResult = CACHED_THROTTLING_PARAMETERS[obfuscatedThrottlingParameter]
        if (cacheResult != null) {
            // If the throttling parameter function has been already ran on the throttling parameter
            // of the current streaming URL, replace directly the obfuscated throttling parameter
            // with the cached result in the streaming URL
            return YoutubeThrottlingParameterUtils.replaceThrottlingParameter(
                streamingUrl, cacheResult,
            )
        }

        val function = function(
            videoId,
            known = { it.throttlingFunction },
            failure = { it.throttlingFailure },
            extract = YoutubeThrottlingParameterUtils::getDeobfuscationCode,
            found = { current, function -> current.copy(throttlingFunction = function, throttlingFailure = null) },
            failed = { current, failure -> current.copy(throttlingFailure = failure) },
        )

        try {
            val deobfuscatedThrottlingParameter = JavaScript.run(
                function,
                YoutubeThrottlingParameterUtils.DEOBFUSCATION_FUNCTION_NAME,
                obfuscatedThrottlingParameter,
            )

            check(deobfuscatedThrottlingParameter.isNotEmpty()) { "Extracted n-parameter is empty" }
            // the result goes into the streaming URL as it is: it must not be able to add
            // parameters or change where the URL leads
            check(
                YoutubeThrottlingParameterUtils.isValidThrottlingParameter(
                    deobfuscatedThrottlingParameter,
                ),
            ) { "Extracted n-parameter is not URL safe" }

            if (CACHED_THROTTLING_PARAMETERS.size >= MAX_CACHED_THROTTLING_PARAMETERS) {
                CACHED_THROTTLING_PARAMETERS.clear()
            }
            CACHED_THROTTLING_PARAMETERS[obfuscatedThrottlingParameter] =
                deobfuscatedThrottlingParameter

            return YoutubeThrottlingParameterUtils.replaceThrottlingParameter(
                streamingUrl, deobfuscatedThrottlingParameter,
            )
        } catch (e: Exception) {
            // This shouldn't happen as the function validity is checked when it is extracted
            throw ParsingException(
                "Could not run throttling parameter deobfuscation JavaScript function", e,
            )
        }
    }

    /**
     * Clear all caches.
     *
     * This method will clear all cached JavaScript code and throttling parameters.
     *
     * The next time [deobfuscateSignature] or [getUrlWithThrottlingParameterDeobfuscated] is
     * called, the JavaScript code will be fetched again and the corresponding extraction methods
     * will be ran.
     */
    @JvmStatic
    public fun clearAllCaches() {
        state.set(null)
        clearThrottlingParametersCache()
    }

    /**
     * Clear all cached throttling parameters.
     *
     * The throttling parameter deobfuscation function will be ran again on these parameters if
     * streaming URLs containing them are passed in the future.
     *
     * This method does not clear the cached throttling parameter deobfuscation function, this can
     * be done using [clearAllCaches].
     */
    @JvmStatic
    public fun clearThrottlingParametersCache() {
        CACHED_THROTTLING_PARAMETERS.clear()
    }

    /**
     * One of the two functions of the current player: the one already extracted, or extracted now
     * from its code. A recent failure is thrown again without searching the code again; a
     * [RuntimeException] (a bug, not a player YouTube changed) is never kept.
     */
    @Throws(ParsingException::class)
    private inline fun function(
        videoId: String,
        known: (PlayerState) -> String?,
        failure: (PlayerState) -> Failure?,
        extract: (String) -> String,
        crossinline found: (PlayerState, String) -> PlayerState,
        crossinline failed: (PlayerState, Failure) -> PlayerState,
    ): String {
        val current = currentState(videoId)
        known(current)?.let { return it }
        failure(current)?.takeIf { it.isRecent }?.let { throw it.exception }
        // the code is only let go once both functions are known, so it is still there
        val code = current.code ?: throw ParsingException("The JavaScript player code is not available")
        val function = try {
            extract(code)
        } catch (e: ParsingException) {
            val stored = Failure(e)
            update(current) { failed(it, stored) }
            throw e
        }
        update(current) { found(it, function) }
        return function
    }

    /** The state of the current player, downloading its code first if there is none yet. */
    @Throws(ParsingException::class)
    private fun currentState(videoId: String): PlayerState {
        state.get()?.let { return it }
        val fresh = PlayerState(Any(), YoutubeJavaScriptExtractor.extractJavaScriptPlayerCode(videoId))
        if (state.compareAndSet(null, fresh)) {
            // the throttling parameters deobfuscated by another player are not valid any more
            CACHED_THROTTLING_PARAMETERS.clear()
            return fresh
        }
        // another thread downloaded it at the same time: use the one that was kept
        return state.get() ?: fresh
    }

    /**
     * Applies [change] to the state of the player [from] comes from; dropped when the player was
     * replaced or cleared meanwhile, so that its function is never kept with the code of another.
     */
    private inline fun update(from: PlayerState, change: (PlayerState) -> PlayerState) {
        while (true) {
            val now = state.get()
            if (now == null || now.player !== from.player) {
                return
            }
            if (state.compareAndSet(now, change(now).withoutUnneededCode())) {
                return
            }
        }
    }
}

package org.schabi.newpipe.extractor.utils

import org.schabi.newpipe.extractor.json.JsonArray
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonParser
import org.schabi.newpipe.extractor.json.JsonParserException
import java.io.IOException
import java.time.Duration
import java.util.regex.PatternSyntaxException

/**
 * The link cleaning rules of ClearURLs (https://github.com/ClearURLs/Rules), applied offline.
 *
 * Rules: commit acbc4d6ea2b6557df6745193f1da21cde39326fe (2026-10-15), LGPL-3.0, bundled
 * unchanged next to their licence in the `clearurls` resources. Unlike the browser add-on, the
 * rules are never downloaded: an update is a reviewed change of the extractor, pinned by
 * [RULES_SHA256].
 *
 * They are applied the way the ClearURLs add-on applies them (`clearurls.js`), with what an app
 * needs:
 * - loaded once, on the first link to clean; the rules of a site are compiled only once one of
 *   its links is met;
 * - every regex reads the link through a [TimeLimitedCharSequence], and a link longer than
 *   [MAX_URL_LENGTH] is left to the built-in list of [UrlPrivacy];
 * - parameters are cut out of the text of the link and never re-encoded (the add-on once turned
 *   spaces into `+` that way);
 * - only the `name=value` pairs of a fragment are parameters: a plain anchor such as `#ref`
 *   stays;
 * - the sites the add-on blocks whole (ad servers) are left alone: a link is not a request of
 *   the extractor, and the app shows its host before opening it.
 */
internal object ClearUrls {

    const val RULES_RESOURCE: String = "/org/schabi/newpipe/extractor/utils/clearurls/data.min.json"
    const val RULES_SHA256: String = "66ac6761a15fceb017fb2470bb4608d98442ffe74fde50c8b94af38a5ca9b470"

    /** Longer links are not worth the regex time: real ones are far shorter. */
    const val MAX_URL_LENGTH: Int = 4096

    /** Time all the rules together may take on one link. */
    private val MATCH_BUDGET: Duration = Duration.ofMillis(100)

    /** How many times a redirection target may be found percent-encoded inside another. */
    private const val MAX_DECODINGS = 3

    /** A target given without its scheme, such as the `example.org/page` of an AMP link. */
    private val SCHEMELESS_TARGET = Regex("[a-z0-9-]+(?:\\.[a-z0-9-]+)+(?:[/?#].*)?", RegexOption.IGNORE_CASE)

    private const val TAG = "ClearUrls"

    /** A link without the parameters the rules removed, and how many there were. */
    class Cleaned(val url: String, val removed: Int)

    /** What [compileAllForTest] found. */
    class Stats(val providers: Int, val patterns: Int, val invalidPatterns: Int)

    private val providers: List<ClearUrlsProvider> by lazy(::load)

    /**
     * Remove the parameters (and the path parts of the raw rules) the rules mark as tracking.
     * Never changes the scheme or the host of the link.
     */
    fun removeTrackers(url: String): Cleaned {
        if (url.length > MAX_URL_LENGTH || !SecureUrls.isWebLink(url)) {
            return Cleaned(url, 0)
        }
        // the rules are read and the site patterns compiled before the time budget starts
        val all = providers
        val deadline = Deadline()
        return try {
            var current = url
            var removed = 0
            for (provider in all) {
                if (provider.complete || !provider.isOfSite(deadline.view(current))) {
                    continue
                }
                deadline.excluding { provider.prepare(redirectionsOnly = false) }
                if (provider.isException(deadline.view(current))) {
                    continue
                }
                for (rawRule in provider.rawRules) {
                    val next = rawRule.replace(deadline.view(current), "")
                    if (next != current) {
                        removed++
                        current = next
                    }
                }
                val cleaned = LinkParameters.remove(current, provider.parameterRules, deadline)
                current = cleaned.url
                removed += cleaned.removed
            }
            if (sameOrigin(url, current)) Cleaned(current, removed) else Cleaned(url, 0)
        } catch (_: TimeLimitedCharSequence.TimeoutException) {
            ExtractorLogger.w(TAG, "Link cleaning took too long, the link is left as it is")
            Cleaned(url, 0)
        } catch (_: StackOverflowError) {
            // a rule recursing too deep on an odd link
            ExtractorLogger.w(TAG, "Link cleaning failed, the link is left as it is")
            Cleaned(url, 0)
        }
    }

    /**
     * @return where a redirection link of a known site (Google, Facebook, Reddit...) leads, if
     * it is a web link; `null` for any other link
     */
    fun redirectTarget(url: String): String? {
        if (url.length > MAX_URL_LENGTH || !SecureUrls.isWebLink(url)) {
            return null
        }
        val all = providers
        val deadline = Deadline()
        return try {
            for (provider in all) {
                if (!provider.hasRedirections || !provider.isOfSite(deadline.view(url))) {
                    continue
                }
                deadline.excluding { provider.prepare(redirectionsOnly = true) }
                if (provider.isException(deadline.view(url))) {
                    continue
                }
                for (redirection in provider.redirections) {
                    val target = redirection.find(deadline.view(url))?.groups?.get(1)?.value ?: continue
                    // like the add-on, the first redirection that matches decides
                    return decodeTarget(target)
                }
            }
            null
        } catch (_: TimeLimitedCharSequence.TimeoutException) {
            ExtractorLogger.w(TAG, "Redirection lookup took too long, the link is left as it is")
            null
        } catch (_: StackOverflowError) {
            // a rule recursing too deep on an odd link
            ExtractorLogger.w(TAG, "Redirection lookup failed, the link is left as it is")
            null
        }
    }

    /** Compile every rule now and count them: only for tests. */
    fun compileAllForTest(): Stats {
        var patterns = 0
        var invalid = 0
        for (provider in providers) {
            val (count, failed) = provider.compileAll()
            patterns += count
            invalid += failed
        }
        return Stats(providers.size, patterns, invalid)
    }

    private fun decodeTarget(raw: String): String? {
        var candidate = raw
        for (i in 0 until MAX_DECODINGS) {
            if (SecureUrls.isWebLink(candidate)) {
                break
            }
            val decoded = try {
                Utils.decodeUrlUtf8(candidate)
            } catch (_: IllegalArgumentException) {
                return null
            }
            if (decoded == candidate) {
                break
            }
            candidate = decoded
        }
        if (!SecureUrls.isWebLink(candidate) && SCHEMELESS_TARGET.matches(candidate)) {
            candidate = "https://$candidate"
        }
        // "https://google.com@evil.com/" shows one host and opens another
        val authority = candidate.substringAfter("//", "").substringBefore('/').substringBefore('?').substringBefore('#')
        if ('@' in authority || '\\' in candidate) {
            return null
        }
        // a space, a quote or a control character has no place in a link to follow
        val printable = candidate.none { it <= ' ' || it == '"' || it == '<' || it == '>' || it == '\u007f' }
        return candidate.takeIf { printable && SecureUrls.isWebLink(it) }
    }

    /** Whether [after] still starts with the scheme and host (and port) of [before]. */
    private fun sameOrigin(before: String, after: String): Boolean {
        val hostStart = before.indexOf("//") + 2
        val end = before.indexOfAny(charArrayOf('/', '?', '#'), hostStart).let { if (it < 0) before.length else it }
        return after.regionMatches(0, before, 0, end, ignoreCase = true) &&
            (after.length == end || after[end] in "/?#")
    }

    private fun load(): List<ClearUrlsProvider> {
        val root = try {
            val stream = ClearUrls::class.java.getResourceAsStream(RULES_RESOURCE)
                ?: throw IOException("Missing $RULES_RESOURCE")
            stream.use { JsonParser.`object`().from(it) }
        } catch (e: IOException) {
            ExtractorLogger.e(TAG, "ClearURLs rules unavailable, only the built-in list applies", e)
            return emptyList()
        } catch (e: JsonParserException) {
            ExtractorLogger.e(TAG, "ClearURLs rules unreadable, only the built-in list applies", e)
            return emptyList()
        }
        val all = root.getObject("providers")
        return all.keys.map { name -> provider(all.getObject(name)) }
    }

    private fun provider(data: JsonObject): ClearUrlsProvider = ClearUrlsProvider(
        urlPattern = data.getString("urlPattern")?.let(::compile),
        complete = data.getBoolean("completeProvider"),
        // the add-on removes referral marketing parameters too unless the user allows them
        ruleSources = strings(data, "rules") + strings(data, "referralMarketing"),
        rawRuleSources = strings(data, "rawRules"),
        exceptionSources = strings(data, "exceptions"),
        redirectionSources = strings(data, "redirections"),
    )

    private fun strings(data: JsonObject, key: String): List<String> =
        (data[key] as? JsonArray).orEmpty().filterIsInstance<String>()

    /** JavaScript `new RegExp(source, "i")`, or `null` for a pattern Java cannot read. */
    fun compile(source: String): Regex? = try {
        Regex(source, RegexOption.IGNORE_CASE)
    } catch (e: PatternSyntaxException) {
        ExtractorLogger.w(TAG, "Skipped a ClearURLs pattern Java cannot read", e)
        null
    }

    /**
     * One time budget shared by all the regexes run on one link. Compiling the patterns of a
     * site is not matching: [excluding] gives that time back.
     */
    class Deadline {
        private var end = System.nanoTime() + MATCH_BUDGET.toNanos()

        fun excluding(work: () -> Unit) {
            val start = System.nanoTime()
            try {
                work()
            } finally {
                end += System.nanoTime() - start
            }
        }

        fun view(text: String): CharSequence =
            TimeLimitedCharSequence(text, Duration.ofNanos((end - System.nanoTime()).coerceAtLeast(0)))
    }
}

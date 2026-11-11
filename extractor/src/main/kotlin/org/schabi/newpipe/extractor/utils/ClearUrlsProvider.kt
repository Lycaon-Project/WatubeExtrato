package org.schabi.newpipe.extractor.utils

/**
 * The ClearURLs rules of one site (a "provider"). Only its URL pattern is compiled up front:
 * the rest is compiled the first time a link of the site is met.
 */
internal class ClearUrlsProvider(
    private val urlPattern: Regex?,
    val complete: Boolean,
    private val ruleSources: List<String>,
    private val rawRuleSources: List<String>,
    private val exceptionSources: List<String>,
    private val redirectionSources: List<String>,
) {
    /** Known without compiling anything: most sites have no redirection. */
    val hasRedirections: Boolean get() = redirectionSources.isNotEmpty()

    /** A parameter is removed when its whole name matches, as with `"^" + rule + "$"`. */
    val parameterRules: List<Regex> by lazy { ruleSources.mapNotNull { ClearUrls.compile("^(?:$it)$") } }
    val rawRules: List<Regex> by lazy { rawRuleSources.mapNotNull(ClearUrls::compile) }
    val redirections: List<Regex> by lazy { redirectionSources.mapNotNull(ClearUrls::compile) }
    private val exceptions: List<Regex> by lazy { exceptionSources.mapNotNull(ClearUrls::compile) }

    /** Whether the link is of this site: only the pattern compiled up front runs. */
    fun isOfSite(url: CharSequence): Boolean = urlPattern != null && urlPattern.containsMatchIn(url)

    /** Whether one of the exceptions of the site covers the link: the rules do not apply then. */
    fun isException(url: CharSequence): Boolean = exceptions.any { it.containsMatchIn(url) }

    /**
     * Compiles the patterns a link of the site needs, the first time only, so that this work is
     * kept out of the time budget of the matching.
     *
     * @return how many patterns are ready
     */
    fun prepare(redirectionsOnly: Boolean): Int = exceptions.size +
        if (redirectionsOnly) redirections.size else rawRules.size + parameterRules.size

    /** @return how many patterns there are, and how many of them could not be compiled */
    fun compileAll(): kotlin.Pair<Int, Int> {
        val sources = 1 + ruleSources.size + rawRuleSources.size + exceptionSources.size +
            redirectionSources.size
        val compiled = (if (urlPattern != null) 1 else 0) + parameterRules.size + rawRules.size +
            exceptions.size + redirections.size
        return sources to sources - compiled
    }
}

/** The parameters of the query and of the fragment of a link, cut out without re-encoding. */
internal object LinkParameters {

    /**
     * @return [url] without the parameters whose name matches one of [rules], compared both as
     * written and percent-decoded; the very same string when none matches
     */
    fun remove(url: String, rules: List<Regex>, deadline: ClearUrls.Deadline): ClearUrls.Cleaned {
        if (rules.isEmpty()) {
            return ClearUrls.Cleaned(url, 0)
        }
        val hash = url.indexOf('#')
        val beforeHash = if (hash < 0) url else url.substring(0, hash)
        val question = beforeHash.indexOf('?')
        if (question < 0 && hash < 0) {
            return ClearUrls.Cleaned(url, 0)
        }
        val query = if (question < 0) null else filter(beforeHash.substring(question + 1), rules, deadline, false)
        val fragment = if (hash < 0) null else filter(url.substring(hash + 1), rules, deadline, true)
        val removed = (query?.removed ?: 0) + (fragment?.removed ?: 0)
        if (removed == 0) {
            return ClearUrls.Cleaned(url, 0)
        }
        val keptQuery = query?.url
        val keptFragment = fragment?.url
        val cleaned = buildString(url.length) {
            append(beforeHash, 0, if (question < 0) beforeHash.length else question)
            if (!keptQuery.isNullOrEmpty()) {
                append('?').append(keptQuery)
            }
            if (!keptFragment.isNullOrEmpty()) {
                append('#').append(keptFragment)
            }
        }
        return ClearUrls.Cleaned(cleaned, removed)
    }

    /**
     * @param fragment whether [part] is a fragment, where only `name=value` pairs are
     * parameters: a fragment without `=` is an anchor and is kept whole
     */
    private fun filter(part: String, rules: List<Regex>, deadline: ClearUrls.Deadline, fragment: Boolean): ClearUrls.Cleaned {
        if (fragment && '=' !in part) {
            return ClearUrls.Cleaned(part, 0)
        }
        var removed = 0
        val kept = part.split('&').filter { pair ->
            if (pair.isEmpty() || (fragment && '=' !in pair)) {
                return@filter pair.isNotEmpty()
            }
            val tracking = isTracking(pair.substringBefore('='), rules, deadline)
            if (tracking) {
                removed++
            }
            !tracking
        }
        return ClearUrls.Cleaned(kept.joinToString("&"), removed)
    }

    private fun isTracking(rawName: String, rules: List<Regex>, deadline: ClearUrls.Deadline): Boolean {
        val decoded = try {
            Utils.decodeUrlUtf8(rawName)
        } catch (_: IllegalArgumentException) {
            rawName
        }
        return rules.any { rule ->
            rule.matches(deadline.view(rawName)) || (decoded != rawName && rule.matches(deadline.view(decoded)))
        }
    }
}

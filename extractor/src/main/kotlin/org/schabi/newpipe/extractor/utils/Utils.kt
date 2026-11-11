package org.schabi.newpipe.extractor.utils

import org.schabi.newpipe.extractor.exceptions.ParsingException
import java.net.MalformedURLException
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.regex.Pattern

public object Utils {
    public const val HTTP: String = "http://"
    public const val HTTPS: String = "https://"
    private val M_PATTERN: Pattern = Pattern.compile("(https?)?://m\\.")
    private val WWW_PATTERN: Pattern = Pattern.compile("(https?)?://www\\.")
    private val NUMBER_WORD_MULTIPLIER: Pattern =
        Pattern.compile("[\\d]+([\\.,][\\d]+)?([KMBkmb])+")
    private val NUMBER_WORD_NUMBER: Pattern = Pattern.compile("([\\d]+([\\.,][\\d]+)?)")

    // runs for every count of a list: compiled once
    private val NON_DIGITS = Regex("\\D+")
    private val AMPERSAND: Pattern = Pattern.compile("&")

    private const val UNKNOWN_PROTOCOL = "unknown protocol: "
    private const val BYTE_ORDER_MARK = "\uFEFF"
    private const val THOUSAND = 1e3
    private const val MILLION = 1e6
    private const val BILLION = 1e9

    /**
     * Longest link accepted by the link handlers. Real links are a few hundred characters at
     * most; the bound keeps hostile input from making parsing and regexes expensive.
     */
    public const val MAX_URL_LENGTH: Int = 8_192

    /**
     * The search hosts of Google: google.com and the country domains (google.it,
     * google.co.uk, google.com.br), with or without a subdomain; never evilgoogle.com.
     */
    private val GOOGLE_SEARCH_HOST = Regex("""(?:[a-z0-9-]+\.)*google\.(?:com?\.)?[a-z]{2,3}""")

    /**
     * @param url an untrusted link
     * @throws ParsingException if the link is longer than [MAX_URL_LENGTH]
     */
    @JvmStatic
    @Throws(ParsingException::class)
    public fun checkUrlLength(url: String) {
        if (url.length > MAX_URL_LENGTH) {
            throw ParsingException("The url is too long")
        }
    }

    /**
     * Encodes a string to URL format using the UTF-8 character set.
     *
     * @param string The string to be encoded.
     * @return The encoded URL.
     */
    @JvmStatic
    public fun encodeUrlUtf8(string: String): String = URLEncoder.encode(string, StandardCharsets.UTF_8)

    /**
     * Decodes a URL using the UTF-8 character set.
     * @param url The URL to be decoded.
     * @return The decoded URL.
     */
    @JvmStatic
    public fun decodeUrlUtf8(url: String): String = URLDecoder.decode(url, StandardCharsets.UTF_8)

    /**
     * Remove all non-digit characters from a string.
     *
     * Examples:
     * - 1 234 567 views -> 1234567
     * - $31,133.124 -> 31133124
     *
     * @param toRemove string to remove non-digit chars
     * @return a string that contains only digits
     */
    @JvmStatic
    public fun removeNonDigitCharacters(toRemove: String): String = toRemove.replace(NON_DIGITS, "")

    /**
     * Convert a mixed number word to a long.
     *
     * Examples:
     * - 123 -> 123
     * - 1.23K -> 1230
     * - 1.23M -> 1230000
     *
     * @param numberWord string to be converted to a long
     * @return a long
     */
    @JvmStatic
    @Throws(NumberFormatException::class, ParsingException::class)
    public fun mixedNumberWordToLong(numberWord: String): Long {
        // runs for every item of a list: precompiled patterns, no exception without a multiplier
        val multiplierMatcher = NUMBER_WORD_MULTIPLIER.matcher(numberWord)
        val multiplier = if (multiplierMatcher.find()) multiplierMatcher.group(2) else ""
        val count = Parser.matchGroup1(NUMBER_WORD_NUMBER, numberWord).replace(",", ".").toDouble()
        return when (multiplier.uppercase(Locale.ROOT)) {
            "K" -> (count * THOUSAND).toLong()
            "M" -> (count * MILLION).toLong()
            "B" -> (count * BILLION).toLong()
            else -> count.toLong()
        }
    }

    /**
     * Check if the url matches the pattern.
     *
     * @param pattern the pattern that will be used to check the url
     * @param url     the url to be tested
     */
    @JvmStatic
    @Throws(ParsingException::class)
    public fun checkUrl(pattern: String, url: String?) {
        checkUrl(Parser.compile(pattern), url)
    }

    /**
     * Check if the url matches the pattern.
     *
     * @param pattern the pattern that will be used to check the url
     * @param url     the url to be tested
     */
    @JvmStatic
    @Throws(ParsingException::class)
    public fun checkUrl(pattern: Pattern, url: String?) {
        require(!url.isNullOrEmpty()) { "Url can't be null or empty" }

        // Locale.ROOT: in a Turkish locale "I" would become a dotless "ı"
        if (!Parser.isMatch(pattern, url.lowercase(Locale.ROOT))) {
            throw ParsingException("Url doesn't match the pattern")
        }
    }

    @JvmStatic
    public fun replaceHttpWithHttps(url: String?): String? {
        if (url == null) {
            return null
        }
        if (url.startsWith(HTTP)) {
            return HTTPS + url.substring(HTTP.length)
        }
        return url
    }
    /**
     * Get the value of a URL-query by name.
     *
     * If an url-query is give multiple times, only the value of the first query is returned.
     *
     * @param url           the url to be used
     * @param parameterName the pattern that will be used to check the url
     * @return a string that contains the value of the query parameter (empty when the parameter
     * has no `=`), or `null` if nothing was found or the value is not decodable
     */
    @JvmStatic
    public fun getQueryValue(url: URL, parameterName: String?): String? {
        val urlQuery = url.query ?: return null

        for (param in AMPERSAND.split(urlQuery)) {
            val params = param.split("=", limit = 2)
            // Links are untrusted: a parameter without "=" or with a malformed
            // percent-encoding must not crash the parsing of the whole link
            val query = try {
                decodeUrlUtf8(params[0])
            } catch (e: IllegalArgumentException) {
                continue
            }

            if (query == parameterName) {
                if (params.size < 2) {
                    return ""
                }
                return try {
                    decodeUrlUtf8(params[1])
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
        }
        return null
    }

    /**
     * Convert a string to a [URL object][URL].
     *
     * Defaults to HTTP if no protocol is given.
     *
     * @param url the string to be converted to a URL-Object
     * @return a [URL object][URL] containing the url
     */
    // URL(String) is deprecated on Java 20+, but URI parsing is stricter: keep java.net.URL
    @Suppress("DEPRECATION")
    @JvmStatic
    @Throws(MalformedURLException::class)
    public fun stringToURL(url: String?): URL =
        try {
            URL(url)
        } catch (e: MalformedURLException) {
            // If no protocol is given try prepending "https://"
            if (e.message == "no protocol: $url") {
                URL(HTTPS + url)
            } else {
                throw e
            }
        }

    @JvmStatic
    public fun isHTTP(url: URL): Boolean {
        // Make sure it's HTTP or HTTPS
        val protocol = url.protocol
        if (protocol != "http" && protocol != "https") {
            return false
        }

        val usesDefaultPort = url.port == url.defaultPort
        val setsNoPort = url.port == -1

        return setsNoPort || usesDefaultPort
    }

    @JvmStatic
    public fun removeMAndWWWFromUrl(url: String): String {
        // only the host prefix goes: "m." or "www." elsewhere in the link is kept
        val mobile = M_PATTERN.matcher(url)
        if (mobile.find()) {
            return url.removeRange(mobile.end() - "m.".length, mobile.end())
        }
        val www = WWW_PATTERN.matcher(url)
        if (www.find()) {
            return url.removeRange(www.end() - "www.".length, www.end())
        }
        return url
    }

    @JvmStatic
    public fun removeUTF8BOM(s: String): String = s.removePrefix(BYTE_ORDER_MARK).removeSuffix(BYTE_ORDER_MARK)

    @JvmStatic
    @Throws(ParsingException::class)
    public fun getBaseUrl(url: String?): String {
        try {
            val uri = stringToURL(url)
            return uri.protocol + "://" + uri.authority
        } catch (e: MalformedURLException) {
            val message = e.message.orEmpty()
            if (message.startsWith(UNKNOWN_PROTOCOL)) {
                // Return just the protocol (e.g. vnd.youtube)
                return message.substring(UNKNOWN_PROTOCOL.length)
            }

            // neither the link nor the exception (whose message holds it) is kept: the link
            // may carry personal or tracking data
            throw ParsingException("Malformed url")
        }
    }

    /**
     * If the provided url is a Google search redirect, then the actual url is extracted from the
     * `url=` query value and returned, otherwise the original url is returned.
     *
     * @param url the url which can possibly be a Google search redirect
     * @return an url with no Google search redirects
     */
    @JvmStatic
    public fun followGoogleRedirectIfNeeded(url: String?): String? {
        // If the url is a redirect from a Google search, extract the actual URL
        try {
            val decoded = stringToURL(url)
            // a Google search host itself (not evilgoogle.com), leading to a web link only
            val host = decoded.host.lowercase(Locale.ROOT).trimEnd('.')
            if (GOOGLE_SEARCH_HOST.matches(host) && decoded.path == "/url" && url != null) {
                val target = decodeUrlUtf8(Parser.matchGroup1("&url=([^&]+)(?:&|$)", url))
                if (SecureUrls.isWebLink(target)) {
                    return target
                }
            }
        } catch (ignored: Exception) {
            // not a link, or not a well-formed redirect: kept as it is
        }

        // URL is not a Google search redirect
        return url
    }
    @JvmStatic
    public fun isNullOrEmpty(str: String?): Boolean = str.isNullOrEmpty()

    /**
     * Checks if a collection is null or empty.
     *
     * This method can be also used for [org.schabi.newpipe.extractor.json.JsonArray]s.
     *
     * @param collection the collection on which check if it's null or empty
     * @return whether the collection is null or empty
     */
    @JvmStatic
    public fun isNullOrEmpty(collection: Collection<*>?): Boolean = collection.isNullOrEmpty()

    /**
     * Checks if a [map][Map] is null or empty.
     *
     * This method can be also used for [org.schabi.newpipe.extractor.json.JsonObject]s.
     *
     * @param map the [map][Map] on which check if it's null or empty
     * @return whether the [map][Map] is null or empty
     */
    @JvmStatic
    public fun <K, V> isNullOrEmpty(map: Map<K, V>?): Boolean = map.isNullOrEmpty()

    /**
     * @return whether the string is null or made only of whitespace, as Java's
     * `String.isBlank()` tells it (a no-break space is not whitespace there)
     */
    @JvmStatic
    public fun isBlank(string: String?): Boolean =
        string == null || string.all { Character.isWhitespace(it) }

    /**
     * Concatenate all non-null, non-empty and strings which are not equal to `"null"`.
     */
    @JvmStatic
    public fun nonEmptyAndNullJoin(delimiter: CharSequence, vararg elements: String?): String =
        elements.filter { !it.isNullOrEmpty() && it != "null" }.joinToString(delimiter)

    /**
     * Find the result of an array of string regular expressions inside an input on a specific
     * group.
     *
     * @param input   the input on which using the regular expressions
     * @param regexes the string array of regular expressions
     * @param group   the group to match
     * @return the result
     * @throws Parser.RegexException if none of the patterns match the input, or at least in the
     * specified group
     */
    @JvmStatic
    @Throws(Parser.RegexException::class)
    public fun getStringResultFromRegexArray(
        input: String,
        regexes: Array<out String?>,
        group: Int,
    ): String =
        // the patterns are compiled once, in the cache of Parser
        getStringResultFromRegexArray(
            input,
            regexes.filterNotNull().map { Parser.compile(it) }.toTypedArray(),
            group,
        )

    /**
     * Find the result of an array of [Pattern]s inside an input on a specific
     * group.
     *
     * @param input   the input on which using the regular expressions
     * @param regexes the [Pattern] array
     * @param group   the group to match
     * @return the result
     * @throws Parser.RegexException if none of the patterns match the input, or at least in the
     * specified group
     */
    @JvmStatic
    @Throws(Parser.RegexException::class)
    public fun getStringResultFromRegexArray(
        input: String,
        regexes: Array<out Pattern>,
        group: Int,
    ): String {
        for (regex in regexes) {
            try {
                return Parser.matchGroup(regex, input, group)
            } catch (ignored: Parser.RegexException) {
                // Continue if the pattern or the group did not match
            }
        }

        throw Parser.RegexException("No regex matched the input on group $group")
    }
}

/*
 * Created by Christian Schabesberger on 02.02.16.
 *
 * Copyright (C) 2016 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * Parser.java is part of NewPipe Extractor.
 *
 * NewPipe Extractor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe Extractor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe Extractor. If not, see <https://www.gnu.org/licenses/>.
 */

package org.schabi.newpipe.extractor.utils

import org.schabi.newpipe.extractor.exceptions.ParsingException
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Avoid using regex !!!
 *
 * The exceptions never quote the input (a page, a link, a script from the network) nor the
 * pattern (some are built from values read from the network).
 */
public object Parser {

    // The String overloads run per item (link handlers, counts): compile each pattern once.
    // Bounded, in case a caller builds its patterns on the fly.
    private const val MAX_CACHED_PATTERNS = 256
    private val PATTERNS = ConcurrentHashMap<String, Pattern>()

    // Pattern.split keeps the semantics of String.split (trailing empty strings dropped)
    private val AMPERSAND: Pattern = Pattern.compile("&")
    private val EQUALS: Pattern = Pattern.compile("=")

    internal fun compile(pattern: String): Pattern {
        PATTERNS[pattern]?.let { return it }
        if (PATTERNS.size >= MAX_CACHED_PATTERNS) {
            PATTERNS.clear()
        }
        val compiled = Pattern.compile(pattern)
        PATTERNS[pattern] = compiled
        return compiled
    }

    public open class RegexException(message: String) : ParsingException(message)

    @JvmStatic
    @Throws(RegexException::class)
    public fun matchOrThrow(pattern: Pattern, input: CharSequence): Matcher {
        val matcher = pattern.matcher(input)
        if (matcher.find()) {
            return matcher
        }
        throw RegexException("Failed to find pattern")
    }

    /**
     * Matches group 1 of the given pattern against the input
     * and returns the matched group
     *
     * @param pattern The regex pattern to match.
     * @param input   The input string to match against.
     * @return The matching group as a string.
     * @throws RegexException If the pattern does not match the input or if the group is not found.
     */
    @JvmStatic
    @Throws(RegexException::class)
    public fun matchGroup1(pattern: String, input: CharSequence): String =
        matchGroup(pattern, input, 1)

    /**
     * Matches group 1 of the given pattern against the input
     * and returns the matched group
     *
     * @param pattern The regex pattern to match.
     * @param input   The input string to match against.
     * @return The matching group as a string.
     * @throws RegexException If the pattern does not match the input or if the group is not found.
     */
    @JvmStatic
    @Throws(RegexException::class)
    public fun matchGroup1(pattern: Pattern, input: CharSequence): String =
        matchGroup(pattern, input, 1)

    /**
     * Matches the specified group of the given pattern against the input,
     * and returns the matched group
     *
     * @param pattern The regex pattern to match.
     * @param input   The input string to match against.
     * @param group   The group number to retrieve (1-based index).
     * @return The matching group as a string.
     * @throws RegexException If the pattern does not match the input or if the group is not found.
     */
    @JvmStatic
    @Throws(RegexException::class)
    public fun matchGroup(pattern: String, input: CharSequence, group: Int): String =
        matchGroup(compile(pattern), input, group)

    /**
     * Matches the specified group of the given pattern against the input,
     * and returns the matched group
     *
     * @param pattern The regex pattern to match.
     * @param input   The input string to match against.
     * @param group   The group number to retrieve (1-based index).
     * @return The matching group as a string.
     * @throws RegexException If the pattern does not match the input or if the group is not found.
     */
    @JvmStatic
    @Throws(RegexException::class)
    public fun matchGroup(pattern: Pattern, input: CharSequence, group: Int): String =
        matchOrThrow(pattern, input).group(group)
            ?: throw RegexException("Failed to find group $group of the pattern")

    /**
     * Matches multiple patterns against the input string and
     * returns the first successful matcher
     *
     * @param patterns The array of regex patterns to match.
     * @param input    The input string to match against.
     * @return A `Matcher` for the first successful match.
     * @throws RegexException If no patterns match the input or if `patterns` is empty.
     */
    @JvmStatic
    @Throws(RegexException::class)
    public fun matchMultiplePatterns(patterns: Array<out Pattern>, input: CharSequence): Matcher {
        if (patterns.isEmpty()) {
            throw RegexException("Empty patterns array passed to matchMultiplePatterns")
        }
        for (pattern in patterns) {
            val matcher = pattern.matcher(input)
            if (matcher.find()) {
                return matcher
            }
        }
        throw RegexException("Failed to find pattern")
    }

    @JvmStatic
    public fun isMatch(pattern: String, input: String): Boolean = isMatch(compile(pattern), input)

    @JvmStatic
    public fun isMatch(pattern: Pattern, input: String): Boolean = pattern.matcher(input).find()

    @JvmStatic
    public fun compatParseMap(input: String): Map<String, String> {
        val map = HashMap<String, String>()
        for (arg in AMPERSAND.split(input)) {
            val splitArg = EQUALS.split(arg)
            if (splitArg.size > 1) {
                // a later value replaces an earlier one, as with Collectors.toMap's merger
                map[splitArg[0]] = try {
                    Utils.decodeUrlUtf8(splitArg[1])
                } catch (_: IllegalArgumentException) {
                    // a malformed escape (%zz) from the server: the raw value, not a crash
                    splitArg[1]
                }
            }
        }
        return map
    }
}

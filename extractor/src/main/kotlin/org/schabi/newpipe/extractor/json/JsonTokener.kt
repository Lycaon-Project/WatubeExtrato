/*
 * SPDX-FileCopyrightText: 2011 The nanojson Authors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2011 The nanojson Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * Modified for WatubeExtrato: ported from Java (com.grack.nanojson, TeamNewPipe fork, commit
 * e9d656ddb49a412a5a0a5d5ef20ca7ef09549996) to Kotlin and hardened for untrusted input.
 */

package org.schabi.newpipe.extractor.json

/**
 * Internal class for tokenizing JSON, used by [JsonParser].
 *
 * Unlike the original, which refilled a 32 KiB buffer from a [java.io.Reader], it reads the
 * whole text from a [String]: the extractor always holds the response in memory already, so
 * the copies and the buffer bookkeeping (and their out of bounds reads on a truncated input) are
 * gone. Every read is bounded by the length of the input.
 *
 * Hardening: error messages never quote the parsed text (only the line and character
 * position), and number tokens are bounded by [MAX_NUMBER_LENGTH].
 */
internal class JsonTokener(private val input: String) {

    private val length = input.length

    /** Index of the next character to read. */
    private var index = 0
    private var linePos = 1

    /** Index of the first character of the current line. */
    private var lineStart = 0
    private var tokenCharPos = 0
    private var tokenCharOffset = 0

    /** Text of the last string, semi-string or number token. */
    val reusableBuffer: StringBuilder = StringBuilder()

    /** Whether the last number token is a floating point one. */
    var isDouble: Boolean = false
        private set

    /**
     * Consumes a token, first eating up any whitespace ahead of it. Note that number tokens are
     * not necessarily valid numbers.
     *
     * @param allowSemiString whether an unquoted string (an object key) is allowed
     */
    @Throws(JsonParserException::class)
    fun advanceToToken(allowSemiString: Boolean): Int {
        skipWhitespace()
        tokenCharPos = index + 1 - lineStart
        tokenCharOffset = index + 1
        if (index >= length) {
            tokenCharPos = index - lineStart
            tokenCharOffset = index
            return TOKEN_EOF
        }

        val c = input[index]
        return when (c) {
            '[' -> single(TOKEN_ARRAY_START)
            ']' -> single(TOKEN_ARRAY_END)
            ',' -> single(TOKEN_COMMA)
            ':' -> single(TOKEN_COLON)
            '{' -> single(TOKEN_OBJECT_START)
            '}' -> single(TOKEN_OBJECT_END)
            '"', '\'' -> {
                index++
                consumeTokenString(c)
                TOKEN_STRING
            }
            '-', in '0'..'9' -> {
                index++
                consumeTokenNumber(c)
                TOKEN_NUMBER
            }
            '+', '.' -> throw createParseException("Numbers may not start with '+' or '.'", true)
            else -> when {
                // keys may be unquoted, even true, false and null: they are read as strings
                allowSemiString -> {
                    consumeTokenSemiString()
                    TOKEN_SEMI_STRING
                }
                c == 't' -> keyword(TRUE, TOKEN_TRUE)
                c == 'f' -> keyword(FALSE, TOKEN_FALSE)
                c == 'n' -> keyword(NULL, TOKEN_NULL)
                isAsciiLetter(c) -> throw createParseException("Unexpected token", true)
                else -> throw createParseException("Unexpected character", true)
            }
        }
    }

    private fun single(token: Int): Int {
        index++
        return token
    }

    private fun skipWhitespace() {
        while (index < length) {
            val c = input[index]
            if (!isWhitespace(c)) {
                return
            }
            index++
            if (c == '\n') {
                newLine()
            }
        }
    }

    /** Called after a `\n` was consumed. */
    private fun newLine() {
        linePos++
        lineStart = index
    }

    /** Expects [expected] at the current position, followed by a separator or the end. */
    @Throws(JsonParserException::class)
    private fun keyword(expected: String, token: Int): Int {
        if (!input.startsWith(expected, index)) {
            throw createParseException("Unexpected token", true)
        }
        index += expected.length
        if (index < length) {
            // The token shouldn't end with something other than an ASCII letter
            when (input[index]) {
                ',', ':', '{', '}', '[', ']', ' ', '\n', '\r', '\t' -> Unit
                else -> throw createParseException("Unexpected token", true)
            }
        }
        return token
    }

    /**
     * Steps through to the end of the current number token (a non-digit token).
     *
     * The JSON spec is way stricter about number formats than [String.toDouble]: this is a
     * hand-rolled pseudo-parser that verifies the numbers it reads.
     */
    @Throws(JsonParserException::class)
    private fun consumeTokenNumber(savedChar: Char) {
        reusableBuffer.setLength(0)
        reusableBuffer.append(savedChar)
        isDouble = false

        var state = when (savedChar) {
            '-' -> NUMBER_AFTER_MINUS
            '0' -> NUMBER_LEADING_ZERO
            else -> NUMBER_INTEGER
        }
        while (index < length) {
            val nc = input[index]
            if (!isDigitCharacter(nc)) {
                break
            }
            state = nextNumberState(state, nc)
            if (state == NUMBER_INVALID) {
                throw createParseException("Malformed number", true)
            }
            reusableBuffer.append(nc)
            index++
            if (reusableBuffer.length > MAX_NUMBER_LENGTH) {
                throw createParseException("Number too long", true)
            }
        }

        if (state != NUMBER_INTEGER && state != NUMBER_LEADING_ZERO &&
            state != NUMBER_FRACTION && state != NUMBER_EXPONENT
        ) {
            throw createParseException("Malformed number", true)
        }

        // Special case for -0
        if (state == NUMBER_LEADING_ZERO && savedChar == '-') {
            isDouble = true
        }
    }

    private fun nextNumberState(state: Int, nc: Char): Int {
        val digit = nc in '0'..'9'
        val exponent = nc == 'e' || nc == 'E'
        return when (state) {
            NUMBER_AFTER_MINUS -> when {
                nc == '0' -> NUMBER_LEADING_ZERO
                digit -> NUMBER_INTEGER
                else -> NUMBER_INVALID
            }
            NUMBER_INTEGER, NUMBER_LEADING_ZERO -> when {
                digit && state == NUMBER_INTEGER -> NUMBER_INTEGER
                nc == '.' -> NUMBER_AFTER_PERIOD.also { isDouble = true }
                exponent -> NUMBER_AFTER_E.also { isDouble = true }
                else -> NUMBER_INVALID
            }
            NUMBER_AFTER_PERIOD, NUMBER_FRACTION -> when {
                digit -> NUMBER_FRACTION
                exponent && state == NUMBER_FRACTION -> NUMBER_AFTER_E.also { isDouble = true }
                else -> NUMBER_INVALID
            }
            // a single sign, right after the exponent
            NUMBER_AFTER_E -> when {
                nc == '+' || nc == '-' -> NUMBER_AFTER_SIGN
                digit -> NUMBER_EXPONENT
                else -> NUMBER_INVALID
            }
            NUMBER_AFTER_SIGN, NUMBER_EXPONENT -> if (digit) NUMBER_EXPONENT else NUMBER_INVALID
            else -> NUMBER_INVALID
        }
    }

    /**
     * Steps through to the end of the current string token (the unescaped quote [quote], which
     * is consumed).
     */
    @Throws(JsonParserException::class)
    private fun consumeTokenString(quote: Char) {
        reusableBuffer.setLength(0)
        while (true) {
            // copy the runs without escapes at once
            val runStart = index
            while (index < length) {
                val c = input[index]
                if (c == quote || c == '\\' || c < ' ') {
                    break
                }
                index++
            }
            reusableBuffer.append(input, runStart, index)

            if (index >= length) {
                throw createParseException("String was not terminated before end of input", true)
            }
            val c = input[index++]
            when {
                c == quote -> return
                c == '\\' -> appendEscape(allowSingleQuote = true)
                else -> throwControlCharacterException(c)
            }
        }
    }

    /**
     * Reads an unquoted string (an object key), up to the whitespace or the colon that ends it,
     * which is not consumed.
     */
    @Throws(JsonParserException::class)
    private fun consumeTokenSemiString() {
        reusableBuffer.setLength(0)
        while (true) {
            if (index >= length) {
                throw createParseException("String was not terminated before end of input", true)
            }
            val c = input[index]
            when {
                isWhitespace(c) || c == ':' -> return
                c == '[' || c == ']' || c == '{' || c == '}' || c == ',' ->
                    throw createParseException("Invalid character in semi-string", false)
                c < ' ' -> {
                    index++
                    throwControlCharacterException(c)
                }
                c == '\\' -> {
                    index++
                    appendEscape(allowSingleQuote = false)
                }
                else -> {
                    reusableBuffer.append(c)
                    index++
                }
            }
        }
    }

    /** Appends the escape following a consumed backslash. */
    @Throws(JsonParserException::class)
    private fun appendEscape(allowSingleQuote: Boolean) {
        if (index >= length) {
            throw createParseException("EOF encountered in the middle of a string escape", false)
        }
        when (val escape = input[index++]) {
            'b' -> reusableBuffer.append('\b')
            'f' -> reusableBuffer.append('\u000C')
            'n' -> reusableBuffer.append('\n')
            'r' -> reusableBuffer.append('\r')
            't' -> reusableBuffer.append('\t')
            '"', '/', '\\' -> reusableBuffer.append(escape)
            '\'' -> if (allowSingleQuote) {
                reusableBuffer.append(escape)
            } else {
                throw createParseException("Invalid escape", false)
            }
            'u' -> reusableBuffer.append(readUnicodeEscape())
            else -> throw createParseException("Invalid escape", false)
        }
    }

    @Throws(JsonParserException::class)
    private fun readUnicodeEscape(): Char {
        if (length - index < UNICODE_ESCAPE_DIGITS) {
            index = length
            throw createParseException("EOF encountered in the middle of a string escape", false)
        }
        var escaped = 0
        repeat(UNICODE_ESCAPE_DIGITS) {
            val digit = Character.digit(input[index++], HEX_RADIX)
            // Character.digit also accepts non-ASCII digits: JSON only allows ASCII ones
            if (digit < 0 || input[index - 1].code >= ASCII_LIMIT) {
                throw createParseException("Expected unicode hex escape character", false)
            }
            escaped = (escaped shl HEX_DIGIT_BITS) or digit
        }
        return escaped.toChar()
    }

    @Throws(JsonParserException::class)
    private fun throwControlCharacterException(c: Char): Nothing {
        // Need to ensure that we position this at the correct location for the error
        if (c == '\n') {
            newLine()
        }
        throw createParseException("Strings may not contain control characters", false)
    }

    /**
     * Creates a [JsonParserException] and fills it from the current line and char position.
     *
     * @param tokenPos whether to point at the start of the token rather than at the current
     * character
     */
    fun createParseException(message: String, tokenPos: Boolean): JsonParserException {
        if (tokenPos) {
            return JsonParserException(
                null, "$message on line $linePos, char $tokenCharPos",
                linePos, tokenCharPos, tokenCharOffset,
            )
        }
        val charPos = maxOf(1, index - lineStart)
        return JsonParserException(
            null, "$message on line $linePos, char $charPos", linePos, charPos, index,
        )
    }

    internal companion object {
        const val TOKEN_EOF: Int = 0
        const val TOKEN_COMMA: Int = 1
        const val TOKEN_COLON: Int = 2
        const val TOKEN_OBJECT_END: Int = 3
        const val TOKEN_ARRAY_END: Int = 4
        const val TOKEN_NULL: Int = 5
        const val TOKEN_TRUE: Int = 6
        const val TOKEN_FALSE: Int = 7
        const val TOKEN_STRING: Int = 8
        const val TOKEN_NUMBER: Int = 9
        const val TOKEN_OBJECT_START: Int = 10
        const val TOKEN_ARRAY_START: Int = 11
        const val TOKEN_SEMI_STRING: Int = 12
        const val TOKEN_VALUE_MIN: Int = TOKEN_NULL

        /**
         * Longest number token accepted. Real numbers are at most a few dozen characters; a huge
         * one would only make [java.math.BigInteger] spend time on hostile input.
         */
        const val MAX_NUMBER_LENGTH: Int = 256

        private const val TRUE = "true"
        private const val FALSE = "false"
        private const val NULL = "null"

        private const val UNICODE_ESCAPE_DIGITS = 4
        private const val HEX_RADIX = 16
        private const val HEX_DIGIT_BITS = 4
        private const val ASCII_LIMIT = 0x80

        // states of the number pseudo-parser
        private const val NUMBER_INVALID = -1
        private const val NUMBER_AFTER_MINUS = 1
        private const val NUMBER_INTEGER = 2
        private const val NUMBER_LEADING_ZERO = 3
        private const val NUMBER_AFTER_PERIOD = 4
        private const val NUMBER_FRACTION = 5
        private const val NUMBER_AFTER_E = 6
        private const val NUMBER_AFTER_SIGN = 7
        private const val NUMBER_EXPONENT = 8

        /** Quick test for whitespace characters. */
        fun isWhitespace(c: Char): Boolean = c == ' ' || c == '\n' || c == '\r' || c == '\t'

        /** Quick test for the characters a number token may contain. */
        private fun isDigitCharacter(c: Char): Boolean =
            c in '0'..'9' || c == 'e' || c == 'E' || c == '.' || c == '+' || c == '-'

        /** Quick test for ASCII letter characters. */
        private fun isAsciiLetter(c: Char): Boolean = c in 'A'..'Z' || c in 'a'..'z'
    }
}

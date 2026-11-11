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

import org.schabi.newpipe.extractor.json.JsonTokener.Companion.TOKEN_ARRAY_END
import org.schabi.newpipe.extractor.json.JsonTokener.Companion.TOKEN_ARRAY_START
import org.schabi.newpipe.extractor.json.JsonTokener.Companion.TOKEN_COLON
import org.schabi.newpipe.extractor.json.JsonTokener.Companion.TOKEN_COMMA
import org.schabi.newpipe.extractor.json.JsonTokener.Companion.TOKEN_EOF
import org.schabi.newpipe.extractor.json.JsonTokener.Companion.TOKEN_FALSE
import org.schabi.newpipe.extractor.json.JsonTokener.Companion.TOKEN_NULL
import org.schabi.newpipe.extractor.json.JsonTokener.Companion.TOKEN_NUMBER
import org.schabi.newpipe.extractor.json.JsonTokener.Companion.TOKEN_OBJECT_END
import org.schabi.newpipe.extractor.json.JsonTokener.Companion.TOKEN_OBJECT_START
import org.schabi.newpipe.extractor.json.JsonTokener.Companion.TOKEN_SEMI_STRING
import org.schabi.newpipe.extractor.json.JsonTokener.Companion.TOKEN_STRING
import org.schabi.newpipe.extractor.json.JsonTokener.Companion.TOKEN_TRUE
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/**
 * Simple JSON parser.
 *
 * ```
 * Object json = JsonParser.any().from("{\"a\":[true,false], \"b\":1}");
 * Number json = (Number) JsonParser.any().from("123.456e7");
 * JsonObject json = JsonParser.object().from("{\"a\":[true,false], \"b\":1}");
 * JsonArray json = JsonParser.array().from("[1, {\"a\":[true,false], \"b\":1}]");
 * ```
 *
 * The parsed text comes from the network, so the parser is iterative (no recursion, however
 * deep the input), refuses more than [MAX_DEPTH] nested arrays and objects, bounds the length of
 * number tokens, and never quotes the text in its exceptions.
 *
 * NOTICE: this package is a Kotlin port of nanojson (Copyright 2011 The nanojson Authors,
 * Apache License 2.0, https://github.com/mmastrac/nanojson), from the TeamNewPipe fork.
 */
public class JsonParser private constructor(
    private val tokener: JsonTokener,
    private val lazyNumbers: Boolean,
) {

    /**
     * Returns a type-safe parser context for a [JsonObject], [JsonArray] or "any" type from
     * which you can parse a [String], a [Reader] or an [InputStream].
     *
     * @param T The parsed type.
     */
    public class JsonParserContext<T> internal constructor(
        /** The expected type, or `null` for any value. */
        private val clazz: Class<*>?,
    ) {
        private var lazyNumbers = false

        /**
         * Parses numbers lazily, allowing us to defer some of the cost of number construction
         * until later.
         */
        public fun withLazyNumbers(): JsonParserContext<T> {
            lazyNumbers = true
            return this
        }

        /** Parses the current JSON type from a [String]. */
        @Throws(JsonParserException::class)
        public fun from(s: String): T = JsonParser(JsonTokener(s), lazyNumbers).parse(clazz)

        /** Parses the current JSON type from a [Reader], read to its end. */
        @Throws(JsonParserException::class)
        public fun from(r: Reader): T = from(readAll(r))

        /**
         * Parses the current JSON type from an [InputStream], read to its end. Detects the
         * encoding from the input stream.
         */
        @Throws(JsonParserException::class)
        public fun from(stm: InputStream): T = from(decode(stm))
    }

    /** Parse a single JSON value from the string, expecting an EOF at the end. */
    @Throws(JsonParserException::class)
    private fun <T> parse(clazz: Class<*>?): T {
        val parsed = parseValue(tokener.advanceToToken(false))
        if (tokener.advanceToToken(false) != TOKEN_EOF) {
            throw tokener.createParseException("Expected end of input", true)
        }
        if (clazz != null && !clazz.isInstance(parsed)) {
            throw tokener.createParseException(
                "JSON did not contain the correct type, expected ${clazz.simpleName}.", true,
            )
        }
        @Suppress("UNCHECKED_CAST")
        return parsed as T
    }

    /** Parses the JSON value starting with [token]. */
    @Throws(JsonParserException::class)
    private fun parseValue(token: Int): Any? = when (token) {
        TOKEN_ARRAY_START, TOKEN_OBJECT_START -> parseNested(token)
        else -> scalarValue(token)
    }

    /**
     * Parses the array or object opened by [openToken], with an explicit stack instead of
     * recursion, so that a deeply nested input cannot overflow the stack of the thread.
     */
    @Throws(JsonParserException::class)
    private fun parseNested(openToken: Int): Any {
        val containers = ArrayList<Any>(INITIAL_STACK_CAPACITY)
        // keys[i] is the key under which the child of the object containers[i] is stored
        val keys = ArrayList<String?>(INITIAL_STACK_CAPACITY)
        var current = open(openToken, containers)
        var justOpened = true

        while (true) {
            // the token of the next member value, or null when the container is closed
            val valueToken: Int? = if (current is JsonArray) {
                nextArrayValueToken(justOpened)
            } else {
                nextObjectValueToken(justOpened, keys)
            }

            if (valueToken == null) {
                val closed = containers.removeAt(containers.size - 1)
                if (containers.isEmpty()) {
                    return closed
                }
                current = containers[containers.size - 1]
                add(current, keys.removeAt(keys.size - 1), closed)
                justOpened = false
            } else if (valueToken == TOKEN_ARRAY_START || valueToken == TOKEN_OBJECT_START) {
                if (current is JsonArray) {
                    keys.add(null)
                }
                current = open(valueToken, containers)
                justOpened = true
            } else {
                val key = if (current is JsonArray) null else keys.removeAt(keys.size - 1)
                add(current, key, scalarValue(valueToken))
                justOpened = false
            }
        }
    }

    @Throws(JsonParserException::class)
    private fun open(token: Int, containers: ArrayList<Any>): Any {
        if (containers.size >= MAX_DEPTH) {
            throw tokener.createParseException("Too deeply nested arrays or objects", true)
        }
        val container: Any = if (token == TOKEN_ARRAY_START) JsonArray() else JsonObject()
        containers.add(container)
        return container
    }

    private fun add(container: Any, key: String?, value: Any?) {
        if (container is JsonArray) {
            container.add(value)
        } else {
            // a key is always pushed before the value of an object member is parsed
            (container as JsonObject)[key.orEmpty()] = value
        }
    }

    /**
     * Reads the tokens up to the next value of an array.
     *
     * @return the token starting the value, or `null` at the end of the array
     */
    @Throws(JsonParserException::class)
    private fun nextArrayValueToken(justOpened: Boolean): Int? {
        var token = tokener.advanceToToken(false)
        if (token == TOKEN_ARRAY_END) {
            return null
        }
        if (!justOpened) {
            if (token != TOKEN_COMMA) {
                throw tokener.createParseException(
                    "Expected a comma or end of the array", true,
                )
            }
            token = tokener.advanceToToken(false)
            if (token == TOKEN_ARRAY_END) {
                throw tokener.createParseException("Trailing comma found in array", true)
            }
        }
        return token
    }

    /**
     * Reads the tokens up to the next value of an object, and pushes its key on [keys].
     *
     * @return the token starting the value, or `null` at the end of the object
     */
    @Throws(JsonParserException::class)
    private fun nextObjectValueToken(justOpened: Boolean, keys: ArrayList<String?>): Int? {
        var token: Int
        if (justOpened) {
            token = tokener.advanceToToken(true)
            if (token == TOKEN_OBJECT_END) {
                return null
            }
        } else {
            token = tokener.advanceToToken(false)
            if (token == TOKEN_OBJECT_END) {
                return null
            }
            if (token != TOKEN_COMMA) {
                throw tokener.createParseException(
                    "Expected a comma or end of the object", true,
                )
            }
            token = tokener.advanceToToken(true)
            if (token == TOKEN_OBJECT_END) {
                throw tokener.createParseException("Trailing comma found in object", true)
            }
        }

        if (token != TOKEN_STRING && token != TOKEN_SEMI_STRING) {
            throw tokener.createParseException("Expected STRING", true)
        }
        val key = tokener.reusableBuffer.toString()
        if (tokener.advanceToToken(false) != TOKEN_COLON) {
            throw tokener.createParseException("Expected COLON", true)
        }
        keys.add(key)
        return tokener.advanceToToken(false)
    }

    /** The value of a token which is not an array or an object. */
    @Throws(JsonParserException::class)
    private fun scalarValue(token: Int): Any? = when (token) {
        TOKEN_TRUE -> true
        TOKEN_FALSE -> false
        TOKEN_NULL -> null
        TOKEN_STRING -> tokener.reusableBuffer.toString()
        TOKEN_NUMBER -> if (lazyNumbers) {
            JsonLazyNumber(tokener.reusableBuffer.toString(), tokener.isDouble)
        } else {
            parseNumber()
        }
        // Only a value start token should appear when we're in the context of parsing a value
        else -> throw tokener.createParseException("Expected JSON value", true)
    }

    @Throws(JsonParserException::class)
    private fun parseNumber(): Number {
        val number = tokener.reusableBuffer.toString()
        try {
            if (tokener.isDouble) {
                return number.toDouble()
            }

            // Quick parse for single-digits
            if (number.length == 1) {
                return number[0] - '0'
            } else if (number.length == 2 && number[0] == '-') {
                return '0' - number[1]
            }

            // HACK: Attempt to parse using the approximate best type for this
            val firstMinus = number[0] == '-'
            val length = if (firstMinus) number.length - 1 else number.length
            val firstDigit = number[if (firstMinus) 1 else 0]
            // 2 147 483 647
            if (length < INT_DIGITS || length == INT_DIGITS && firstDigit < '2') {
                return number.toInt()
            }
            // 9 223 372 036 854 775 807
            if (length < LONG_DIGITS || length == LONG_DIGITS && firstDigit < '9') {
                return number.toLong()
            }
            return BigInteger(number)
        } catch (_: NumberFormatException) {
            // not chained: its message quotes the number
            throw tokener.createParseException("Malformed number", true)
        }
    }

    public companion object {
        /** Deepest nesting of arrays and objects accepted. */
        public const val MAX_DEPTH: Int = 512

        private const val INITIAL_STACK_CAPACITY = 16
        private const val INT_DIGITS = 10
        private const val LONG_DIGITS = 19
        private const val READ_BUFFER_SIZE = 8 * 1024
        private const val BOM_SIGNATURE_SIZE = 4

        /**
         * Parses a [JsonObject] from a source.
         *
         * ```
         * JsonObject json = JsonParser.object().from("{\"a\":[true,false], \"b\":1}");
         * ```
         */
        @JvmStatic
        public fun `object`(): JsonParserContext<JsonObject> =
            JsonParserContext<JsonObject>(JsonObject::class.java)

        /**
         * Parses a [JsonArray] from a source.
         *
         * ```
         * JsonArray json = JsonParser.array().from("[1, {\"a\":[true,false], \"b\":1}]");
         * ```
         */
        @JvmStatic
        public fun array(): JsonParserContext<JsonArray> = JsonParserContext<JsonArray>(JsonArray::class.java)

        /**
         * Parses any object from a source. For any valid JSON, returns either a null (for the
         * JSON string 'null'), a [String], a [Number], a [Boolean], a [JsonObject] or a
         * [JsonArray].
         *
         * ```
         * Object json = JsonParser.any().from("{\"a\":[true,false], \"b\":1}");
         * Number json = (Number) JsonParser.any().from("123.456e7");
         * ```
         */
        @JvmStatic
        public fun any(): JsonParserContext<Any?> = JsonParserContext<Any?>(null)

        @Throws(JsonParserException::class)
        private fun readAll(reader: Reader): String {
            try {
                val text = StringBuilder()
                val buffer = CharArray(READ_BUFFER_SIZE)
                while (true) {
                    val read = reader.read(buffer)
                    if (read < 0) {
                        return text.toString()
                    }
                    text.appendRange(buffer, 0, read)
                }
            } catch (e: IOException) {
                throw JsonParserException(e, "IOException while reading", 1, 1, 0)
            }
        }

        /**
         * Reads [stm] to its end and decodes it, detecting the encoding as RFC 4627 describes:
         * from a byte order mark, or from where the zero bytes of the first characters are.
         * Malformed input is refused rather than replaced.
         */
        @Throws(JsonParserException::class)
        private fun decode(stm: InputStream): String {
            val bytes = try {
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(READ_BUFFER_SIZE)
                while (true) {
                    val read = stm.read(buffer)
                    if (read < 0) {
                        break
                    }
                    out.write(buffer, 0, read)
                }
                out.toByteArray()
            } catch (e: IOException) {
                throw JsonParserException(e, "IOException while reading", 1, 1, 0)
            }

            val sig = IntArray(BOM_SIGNATURE_SIZE) { if (it < bytes.size) bytes[it].toInt() and 0xff else -1 }
            val (charsetName, skip) = detectEncoding(sig)
            return try {
                Charset.forName(charsetName).newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes, skip, bytes.size - skip))
                    .toString()
            } catch (_: CharacterCodingException) {
                throw JsonParserException(null, "Malformed character encoding", 1, 1, 0)
            }
        }

        /** The charset of a stream starting with [sig], and how many signature bytes to skip. */
        private fun detectEncoding(sig: IntArray): Pair<String, Int> = when {
            sig[0] == 0xEF && sig[1] == 0xBB && sig[2] == 0xBF -> "UTF-8" to 3
            sig[0] == 0x00 && sig[1] == 0x00 && sig[2] == 0xFE && sig[3] == 0xFF -> "UTF-32BE" to 4
            sig[0] == 0xFF && sig[1] == 0xFE && sig[2] == 0x00 && sig[3] == 0x00 -> "UTF-32LE" to 4
            sig[0] == 0xFE && sig[1] == 0xFF -> "UTF-16BE" to 2
            sig[0] == 0xFF && sig[1] == 0xFE -> "UTF-16LE" to 2
            sig[0] == 0 && sig[1] == 0 && sig[2] == 0 && sig[3] != 0 -> "UTF-32BE" to 0
            sig[0] != 0 && sig[1] == 0 && sig[2] == 0 && sig[3] == 0 -> "UTF-32LE" to 0
            sig[0] == 0 && sig[1] != 0 && sig[2] == 0 && sig[3] != 0 -> "UTF-16BE" to 0
            sig[0] != 0 && sig[1] == 0 && sig[2] != 0 && sig[3] == 0 -> "UTF-16LE" to 0
            else -> "UTF-8" to 0
        }
    }
}

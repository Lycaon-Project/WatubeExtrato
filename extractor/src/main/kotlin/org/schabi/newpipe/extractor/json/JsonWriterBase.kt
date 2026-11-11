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

import java.io.IOException
import java.util.BitSet

/**
 * Internal class that handles emitting to an [Appendable]. Users only see the public
 * subclasses, [JsonStringWriter] and [JsonAppendableWriter]. (It is public only because Kotlin
 * requires the superclass of a public class to be public; it cannot be subclassed outside.)
 *
 * The text goes straight into the [Appendable] when it is a [StringBuilder], and through a
 * buffer otherwise. No reflection: arrays are recognized by their Kotlin types.
 *
 * @param SELF A subclass of [JsonWriterBase].
 */
public abstract class JsonWriterBase<SELF : JsonWriterBase<SELF>> internal constructor(
    protected val appendable: Appendable,
    /** Sequence to use for indenting, or `null` for a compact output. */
    private val indentString: String?,
) : JsonSink<SELF> {

    /** Whether [buffer] is [appendable] itself, which then needs no flush. */
    private val direct = appendable is StringBuilder
    private val buffer: StringBuilder = appendable as? StringBuilder ?: StringBuilder(BUFFER_SIZE)
    private val states = BitSet()
    private var stateIndex = 0
    private var first = true
    private var inObject = false

    /** Current indent amount. */
    private var indent = 0

    /** This is guaranteed to be safe as the type of "this" will always be the type of "SELF". */
    @Suppress("UNCHECKED_CAST")
    private fun castThis(): SELF = this as SELF

    override fun array(c: Collection<*>): SELF = array(null, c)

    override fun array(key: String?, c: Collection<*>): SELF {
        if (key == null) array() else array(key)
        for (o in c) {
            value(o)
        }
        return end()
    }

    override fun `object`(map: Map<*, *>): SELF = `object`(null, map)

    override fun `object`(key: String?, map: Map<*, *>): SELF {
        if (key == null) `object`() else `object`(key)
        for ((k, v) in map) {
            if (k !is String) {
                throw JsonWriterException(
                    "Invalid key type for map: " + (k?.javaClass?.toString() ?: "null"),
                )
            }
            value(k, v)
        }
        return end()
    }

    override fun nul(): SELF {
        preValue()
        raw(NULL)
        return castThis()
    }

    override fun nul(key: String): SELF {
        preValue(key)
        raw(NULL)
        return castThis()
    }

    override fun value(o: Any?): SELF = when (o) {
        null -> nul()
        is String -> value(o)
        is Number -> value(o)
        is Boolean -> value(o)
        is Collection<*> -> array(o)
        is Map<*, *> -> `object`(o)
        else -> array(null, arrayElements(o))
    }

    override fun value(key: String, o: Any?): SELF = when (o) {
        null -> nul(key)
        is String -> value(key, o)
        is Number -> value(key, o)
        is Boolean -> value(key, o)
        is Collection<*> -> array(key, o)
        is Map<*, *> -> `object`(key, o)
        else -> array(key, arrayElements(o))
    }

    override fun value(s: String?): SELF {
        if (s == null) {
            return nul()
        }
        preValue()
        emitStringValue(s)
        return castThis()
    }

    override fun value(i: Int): SELF {
        preValue()
        raw(i.toString())
        return castThis()
    }

    override fun value(l: Long): SELF {
        preValue()
        raw(l.toString())
        return castThis()
    }

    override fun value(b: Boolean): SELF {
        preValue()
        raw(if (b) TRUE else FALSE)
        return castThis()
    }

    override fun value(d: Double): SELF {
        preValue()
        raw(d.toString())
        return castThis()
    }

    override fun value(f: Float): SELF {
        preValue()
        raw(f.toString())
        return castThis()
    }

    override fun value(n: Number?): SELF {
        preValue()
        raw(n?.toString() ?: NULL)
        return castThis()
    }

    override fun value(key: String, s: String?): SELF {
        if (s == null) {
            return nul(key)
        }
        preValue(key)
        emitStringValue(s)
        return castThis()
    }

    override fun value(key: String, i: Int): SELF {
        preValue(key)
        raw(i.toString())
        return castThis()
    }

    override fun value(key: String, l: Long): SELF {
        preValue(key)
        raw(l.toString())
        return castThis()
    }

    override fun value(key: String, b: Boolean): SELF {
        preValue(key)
        raw(if (b) TRUE else FALSE)
        return castThis()
    }

    override fun value(key: String, d: Double): SELF {
        preValue(key)
        raw(d.toString())
        return castThis()
    }

    override fun value(key: String, f: Float): SELF {
        preValue(key)
        raw(f.toString())
        return castThis()
    }

    override fun value(key: String, n: Number?): SELF {
        if (n == null) {
            return nul(key)
        }
        preValue(key)
        raw(n.toString())
        return castThis()
    }

    override fun array(): SELF {
        preValue()
        openArray()
        return castThis()
    }

    override fun `object`(): SELF {
        preValue()
        openObject()
        return castThis()
    }

    override fun array(key: String): SELF {
        preValue(key)
        openArray()
        return castThis()
    }

    override fun `object`(key: String): SELF {
        preValue(key)
        openObject()
        return castThis()
    }

    private fun openArray() {
        states.set(stateIndex++, inObject)
        inObject = false
        first = true
        raw('[')
    }

    private fun openObject() {
        states.set(stateIndex++, inObject)
        inObject = true
        first = true
        raw('{')
        if (indentString != null) {
            indent++
            appendNewLine()
        }
    }

    override fun end(): SELF {
        if (stateIndex == 0) {
            throw JsonWriterException("Invalid call to end()")
        }

        if (inObject) {
            if (indentString != null) {
                indent--
                appendNewLine()
                appendIndent()
            }
            raw('}')
        } else {
            raw(']')
        }

        first = false
        inObject = states.get(--stateIndex)
        return castThis()
    }

    /**
     * Ensures that the object is in the finished state.
     *
     * @throws JsonWriterException if the written JSON is not properly balanced, ie: all arrays
     * and objects that were started have been properly ended.
     */
    protected fun doneInternal() {
        if (stateIndex > 0) {
            throw JsonWriterException("Unclosed JSON objects and/or arrays when closing writer")
        }
        if (first) {
            throw JsonWriterException("Nothing was written to the JSON writer")
        }
        flush()
    }

    private fun appendIndent() {
        val indentText = indentString ?: return
        repeat(indent) {
            raw(indentText)
        }
    }

    private fun appendNewLine() {
        raw('\n')
    }

    private fun raw(s: String) {
        buffer.append(s)
        flushIfFull()
    }

    private fun raw(c: Char) {
        buffer.append(c)
        flushIfFull()
    }

    private fun flushIfFull() {
        if (!direct && buffer.length > BUFFER_SIZE) {
            flush()
        }
    }

    private fun flush() {
        if (direct) {
            return
        }
        try {
            appendable.append(buffer)
            buffer.setLength(0)
        } catch (e: IOException) {
            throw JsonWriterException(e)
        }
    }

    private fun pre() {
        if (first) {
            first = false
        } else {
            if (stateIndex == 0) {
                throw JsonWriterException("Invalid call to emit a value in a finished JSON writer")
            }
            raw(',')
            if (indentString != null && inObject) {
                appendNewLine()
            }
        }
    }

    private fun preValue() {
        if (inObject) {
            throw JsonWriterException("Invalid call to emit a keyless value while writing an object")
        }
        pre()
    }

    private fun preValue(key: String) {
        if (!inObject) {
            throw JsonWriterException("Invalid call to emit a key value while not writing an object")
        }
        pre()
        if (indentString != null) {
            appendIndent()
        }
        emitStringValue(key)
        raw(':')
    }

    /** Emits a quoted string value, escaping characters that are required to be escaped. */
    private fun emitStringValue(s: String) {
        raw('"')
        var c = NO_CHAR
        for (i in s.indices) {
            val b = c
            c = s[i]
            when (c) {
                '\\', '"' -> {
                    raw('\\')
                    raw(c)
                }
                '/' -> {
                    // Special case to ensure that </script> doesn't appear in JSON output
                    if (b == '<') {
                        raw('\\')
                    }
                    raw(c)
                }
                '\b' -> raw("\\b")
                '\t' -> raw("\\t")
                '\n' -> raw("\\n")
                '\u000C' -> raw("\\f")
                '\r' -> raw("\\r")
                else -> if (shouldBeEscaped(c)) emitUnicodeEscape(c) else raw(c)
            }
        }
        raw('"')
    }

    private fun emitUnicodeEscape(c: Char) {
        val code = c.code
        if (code < SMALL_ESCAPE_LIMIT) {
            raw("\\u00")
        } else {
            raw("\\u")
            raw(HEX[(code shr HIGH_SHIFT) and HEX_MASK])
            raw(HEX[(code shr MIDDLE_SHIFT) and HEX_MASK])
        }
        raw(HEX[(code shr LOW_SHIFT) and HEX_MASK])
        raw(HEX[code and HEX_MASK])
    }

    internal companion object {
        const val BUFFER_SIZE: Int = 10 * 1024
        private const val NULL = "null"
        private const val TRUE = "true"
        private const val FALSE = "false"
        private const val HEX = "0123456789abcdef"
        private const val NO_CHAR = '\u0000'
        private const val SMALL_ESCAPE_LIMIT = 0x100
        private const val HEX_MASK = 0xf
        private const val HIGH_SHIFT = 12
        private const val MIDDLE_SHIFT = 8
        private const val LOW_SHIFT = 4

        /** json.org spec says that all control characters must be escaped. */
        private fun shouldBeEscaped(c: Char): Boolean =
            c < ' ' || c in '\u0080' until '\u00a0' || c in '\u2000' until '\u2100'

        /** The elements of a Java array, or an exception for any other type. */
        private fun arrayElements(o: Any): List<*> = when (o) {
            is Array<*> -> o.asList()
            is IntArray -> o.asList()
            is LongArray -> o.asList()
            is DoubleArray -> o.asList()
            is FloatArray -> o.asList()
            is BooleanArray -> o.asList()
            is ShortArray -> o.asList()
            is ByteArray -> o.asList()
            // a char is not a JSON value (the original failed on its first element)
            else -> throw JsonWriterException("Unable to handle type: " + o.javaClass)
        }
    }
}

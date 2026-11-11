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

import java.io.OutputStream
import java.io.PrintStream

/**
 * Factory for JSON writers that target [String]s and [Appendable]s.
 *
 * Creates writers that write JSON to a [String], an [OutputStream], or an [Appendable] such as
 * a [StringBuilder], a [java.io.Writer], a [PrintStream] or a [java.nio.CharBuffer].
 *
 * ```
 * String json = JsonWriter
 *     .indent("  ")
 *     .string()
 *     .object()
 *         .array("a")
 *             .value(1)
 *             .value(2)
 *         .end()
 *         .value("b", false)
 *         .value("c", true)
 *     .end()
 * .done();
 * ```
 */
public object JsonWriter {

    /** Allows for additional configuration of the [JsonWriter]. */
    public class JsonWriterContext internal constructor(private val indent: String) {

        /** Creates a new [JsonStringWriter]. */
        public fun string(): JsonStringWriter = JsonStringWriter(indent)

        /**
         * Creates a [JsonAppendableWriter] that can output to an [Appendable] subclass, such as a
         * [StringBuilder], a [java.io.Writer] a [PrintStream] or a [java.nio.CharBuffer].
         */
        public fun on(appendable: Appendable): JsonAppendableWriter =
            JsonAppendableWriter(appendable, indent)

        /** Creates a [JsonAppendableWriter] that can output to a [PrintStream] subclass. */
        public fun on(appendable: PrintStream): JsonAppendableWriter {
            // a PrintStream is also an OutputStream: written to as an Appendable
            val target: Appendable = appendable
            return JsonAppendableWriter(target, indent)
        }

        /**
         * Creates a [JsonAppendableWriter] that can output to an [OutputStream] subclass. Uses
         * the UTF-8 charset. To specify a different charset, use the [on] function taking an
         * [Appendable] with an [java.io.OutputStreamWriter].
         */
        public fun on(out: OutputStream): JsonAppendableWriter = JsonAppendableWriter(out, indent)
    }

    /**
     * Creates a [JsonWriter] source that will write indented output with the given indent.
     *
     * ```
     * String json = JsonWriter.indent("  ").string()
     *     .object()
     *         .array("a")
     *             .value(1)
     *             .value(2)
     *         .end()
     *         .value("b", false)
     *         .value("c", true)
     *     .end()
     * .done();
     * ```
     */
    @JvmStatic
    public fun indent(indent: String): JsonWriterContext {
        require(indent.all { it == ' ' || it == '\t' }) {
            "Only tabs and spaces are allowed for indent."
        }
        return JsonWriterContext(indent)
    }

    /**
     * Creates a new [JsonStringWriter].
     *
     * ```
     * String json = JsonWriter.string()
     *     .object()
     *         .array("a")
     *             .value(1)
     *             .value(2)
     *         .end()
     *         .value("b", false)
     *         .value("c", true)
     *     .end()
     * .done();
     * ```
     */
    @JvmStatic
    public fun string(): JsonStringWriter = JsonStringWriter(null)

    /**
     * Emits a single value (a JSON primitive such as a [Number], [Boolean], [String], a [Map] or
     * [JsonObject], or a [Collection] or [JsonArray].
     *
     * Emit a [String], JSON-escaped:
     *
     * ```
     * JsonWriter.string("abc\n\"") // "\"abc\\n\\\"\""
     * ```
     *
     * ```
     * JsonObject obj = new JsonObject();
     * obj.put("abc", 1);
     * JsonWriter.string(obj) // "{\"abc\":1}"
     * ```
     */
    @JvmStatic
    public fun string(value: Any?): String = JsonStringWriter(null).value(value).done()

    /**
     * Creates a [JsonAppendableWriter] that can output to an [Appendable] subclass, such as a
     * [StringBuilder], a [java.io.Writer] a [PrintStream] or a [java.nio.CharBuffer].
     */
    @JvmStatic
    public fun on(appendable: Appendable): JsonAppendableWriter = JsonAppendableWriter(appendable, null)

    /** Creates a [JsonAppendableWriter] that can output to a [PrintStream] subclass. */
    @JvmStatic
    public fun on(appendable: PrintStream): JsonAppendableWriter {
        // a PrintStream is also an OutputStream: written to as an Appendable
        val target: Appendable = appendable
        return JsonAppendableWriter(target, null)
    }

    /**
     * Creates a [JsonAppendableWriter] that can output to an [OutputStream] subclass. Uses the
     * UTF-8 charset. To specify a different charset, use the [on] function taking an [Appendable]
     * with an [java.io.OutputStreamWriter].
     */
    @JvmStatic
    public fun on(out: OutputStream): JsonAppendableWriter = JsonAppendableWriter(out, null)

    /**
     * Escape a string value.
     *
     * @return the escaped JSON value
     */
    @JvmStatic
    public fun escape(value: String): String {
        val s = string(value)
        return s.substring(1, s.length - 1)
    }
}

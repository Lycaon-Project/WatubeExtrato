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

import java.io.Flushable
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

/**
 * JSON writer that emits JSON to a [Appendable].
 *
 * Create this class with [JsonWriter.on].
 *
 * ```
 * OutputStream out = ...;
 * JsonWriter
 *     .indent("  ")
 *     .on(out)
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
public class JsonAppendableWriter internal constructor(appendable: Appendable, indent: String?) :
    JsonWriterBase<JsonAppendableWriter>(appendable, indent) {

    /** Writes UTF-8 to [out] (through the standard encoder, instead of a hand-written one). */
    internal constructor(out: OutputStream, indent: String?) :
        this(OutputStreamWriter(out, StandardCharsets.UTF_8), indent)

    /**
     * Closes this JSON writer and flushes the underlying [Appendable] if it is also [Flushable].
     *
     * @throws JsonWriterException if the underlying [Flushable] [Appendable] failed to flush.
     */
    @Throws(JsonWriterException::class)
    public fun done() {
        doneInternal()
        try {
            (appendable as? Flushable)?.flush()
        } catch (e: IOException) {
            throw JsonWriterException(e)
        }
    }
}

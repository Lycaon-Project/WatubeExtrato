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
 * Builds a [JsonObject] or [JsonArray].
 *
 * @param T The type of JSON object to build.
 */
public class JsonBuilder<T> internal constructor(private val root: T) : JsonSink<JsonBuilder<T>> {

    /** The open containers, the innermost last. */
    private val json = arrayListOf<Any?>(root)

    /** Completes this builder, closing any unclosed objects and returns the built object. */
    public fun done(): T = root

    override fun array(c: Collection<*>): JsonBuilder<T> = value(c)

    override fun array(key: String?, c: Collection<*>): JsonBuilder<T> =
        if (key == null) value(c) else value(key, c)

    override fun `object`(map: Map<*, *>): JsonBuilder<T> = value(map)

    override fun `object`(key: String?, map: Map<*, *>): JsonBuilder<T> =
        if (key == null) value(map) else value(key, map)

    override fun nul(): JsonBuilder<T> = add(null)

    override fun nul(key: String): JsonBuilder<T> = put(key, null)

    override fun value(o: Any?): JsonBuilder<T> = add(o)

    override fun value(key: String, o: Any?): JsonBuilder<T> = put(key, o)

    override fun value(s: String?): JsonBuilder<T> = add(s)

    override fun value(i: Int): JsonBuilder<T> = add(i)

    override fun value(l: Long): JsonBuilder<T> = add(l)

    override fun value(b: Boolean): JsonBuilder<T> = add(b)

    override fun value(d: Double): JsonBuilder<T> = add(d)

    override fun value(f: Float): JsonBuilder<T> = add(f)

    override fun value(n: Number?): JsonBuilder<T> = add(n)

    override fun value(key: String, s: String?): JsonBuilder<T> = put(key, s)

    override fun value(key: String, i: Int): JsonBuilder<T> = put(key, i)

    override fun value(key: String, l: Long): JsonBuilder<T> = put(key, l)

    override fun value(key: String, b: Boolean): JsonBuilder<T> = put(key, b)

    override fun value(key: String, d: Double): JsonBuilder<T> = put(key, d)

    override fun value(key: String, f: Float): JsonBuilder<T> = put(key, f)

    override fun value(key: String, n: Number?): JsonBuilder<T> = put(key, n)

    override fun array(): JsonBuilder<T> {
        val a = JsonArray()
        add(a)
        json.add(a)
        return this
    }

    override fun `object`(): JsonBuilder<T> {
        val o = JsonObject()
        add(o)
        json.add(o)
        return this
    }

    override fun array(key: String): JsonBuilder<T> {
        val a = JsonArray()
        put(key, a)
        json.add(a)
        return this
    }

    override fun `object`(key: String): JsonBuilder<T> {
        val o = JsonObject()
        put(key, o)
        json.add(o)
        return this
    }

    override fun end(): JsonBuilder<T> {
        if (json.size == 1) {
            throw JsonWriterException("Cannot end the root object or array")
        }
        json.removeAt(json.size - 1)
        return this
    }

    private fun add(o: Any?): JsonBuilder<T> {
        arr().add(o)
        return this
    }

    private fun put(key: String, o: Any?): JsonBuilder<T> {
        obj()[key] = o
        return this
    }

    private fun obj(): JsonObject = json[json.size - 1] as? JsonObject
        ?: throw JsonWriterException("Attempted to write a keyed value to a JsonArray")

    private fun arr(): JsonArray = json[json.size - 1] as? JsonArray
        ?: throw JsonWriterException("Attempted to write a non-keyed value to a JsonObject")
}

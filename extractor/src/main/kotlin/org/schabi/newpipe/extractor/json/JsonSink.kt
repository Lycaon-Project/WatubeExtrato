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
 * Common interface for [JsonAppendableWriter], [JsonStringWriter] and [JsonBuilder].
 *
 * @param SELF A subclass of [JsonSink].
 */
public interface JsonSink<SELF : JsonSink<SELF>> {
    /** Emits an array with the elements of [c]. */
    public fun array(c: Collection<*>): SELF

    /** Emits an array with the elements of [c], with a key (or without one when it is `null`). */
    public fun array(key: String?, c: Collection<*>): SELF

    /** Emits an object with the entries of [map]. */
    public fun `object`(map: Map<*, *>): SELF

    /** Emits an object with the entries of [map], with a key (or without one when it is `null`). */
    public fun `object`(key: String?, map: Map<*, *>): SELF

    /** Emits a 'null' token. */
    public fun nul(): SELF

    /** Emits a 'null' token with a key. */
    public fun nul(key: String): SELF

    /** Emits an object if it is a JSON-compatible type, otherwise throws an exception. */
    public fun value(o: Any?): SELF

    /** Emits an object with a key if it is a JSON-compatible type, otherwise throws an exception. */
    public fun value(key: String, o: Any?): SELF

    /** Emits a string value (or null). */
    public fun value(s: String?): SELF

    /** Emits an integer value. */
    public fun value(i: Int): SELF

    /** Emits a long value. */
    public fun value(l: Long): SELF

    /** Emits a boolean value. */
    public fun value(b: Boolean): SELF

    /** Emits a double value. */
    public fun value(d: Double): SELF

    /** Emits a float value. */
    public fun value(f: Float): SELF

    /** Emits a [Number] value. */
    public fun value(n: Number?): SELF

    /** Emits a string value (or null) with a key. */
    public fun value(key: String, s: String?): SELF

    /** Emits an integer value with a key. */
    public fun value(key: String, i: Int): SELF

    /** Emits a long value with a key. */
    public fun value(key: String, l: Long): SELF

    /** Emits a boolean value with a key. */
    public fun value(key: String, b: Boolean): SELF

    /** Emits a double value with a key. */
    public fun value(key: String, d: Double): SELF

    /** Emits a float value with a key. */
    public fun value(key: String, f: Float): SELF

    /** Emits a [Number] value with a key. */
    public fun value(key: String, n: Number?): SELF

    /** Starts an array. */
    public fun array(): SELF

    /** Starts an object. */
    public fun `object`(): SELF

    /** Starts an array within an object, prefixed with a key. */
    public fun array(key: String): SELF

    /** Starts an object within an object, prefixed with a key. */
    public fun `object`(key: String): SELF

    /** Ends the current array or object. */
    public fun end(): SELF
}

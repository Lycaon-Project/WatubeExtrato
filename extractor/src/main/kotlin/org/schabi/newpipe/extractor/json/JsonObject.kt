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
 * Extends a [LinkedHashMap] with helper methods to determine the underlying JSON type of the
 * map element.
 *
 * The single-argument getters of a container ([getObject], [getArray]) only create the empty
 * default when the key is missing or of the wrong type, not on every call.
 */
public open class JsonObject : LinkedHashMap<String, Any?> {

    /** Creates an empty [JsonObject] with the default capacity. */
    public constructor() : super()

    /** Creates a [JsonObject] from an existing [Map]. */
    public constructor(map: Map<out String, Any?>) : super(map)

    /** Creates a [JsonObject] with the given initial capacity. */
    public constructor(initialCapacity: Int) : super(initialCapacity)

    /** Creates a [JsonObject] with the given initial capacity and load factor. */
    public constructor(initialCapacity: Int, loadFactor: Float) : super(initialCapacity, loadFactor)

    /**
     * Returns the [JsonArray] at the given key, or an empty one if it does not exist or is the
     * wrong type.
     */
    public fun getArray(key: String): JsonArray = get(key) as? JsonArray ?: JsonArray()

    /**
     * Returns the [JsonArray] at the given key, or the default if it does not exist or is the
     * wrong type.
     */
    public fun getArray(key: String, default: JsonArray?): JsonArray? = get(key) as? JsonArray ?: default

    /** Returns the [Boolean] at the given key, or false if it does not exist or is the wrong type. */
    public fun getBoolean(key: String): Boolean = getBoolean(key, false)

    /**
     * Returns the [Boolean] at the given key, or the default if it does not exist or is the wrong
     * type.
     */
    public fun getBoolean(key: String, default: Boolean): Boolean = get(key) as? Boolean ?: default

    /** Returns the [Double] at the given key, or 0.0 if it does not exist or is the wrong type. */
    public fun getDouble(key: String): Double = getDouble(key, 0.0)

    /**
     * Returns the [Double] at the given key, or the default if it does not exist or is the wrong
     * type.
     */
    public fun getDouble(key: String, default: Double): Double =
        (get(key) as? Number)?.toDouble() ?: default

    /** Returns the [Float] at the given key, or 0.0f if it does not exist or is the wrong type. */
    public fun getFloat(key: String): Float = getFloat(key, 0f)

    /**
     * Returns the [Float] at the given key, or the default if it does not exist or is the wrong
     * type.
     */
    public fun getFloat(key: String, default: Float): Float =
        (get(key) as? Number)?.toFloat() ?: default

    /** Returns the [Int] at the given key, or 0 if it does not exist or is the wrong type. */
    public fun getInt(key: String): Int = getInt(key, 0)

    /**
     * Returns the [Int] at the given key, or the default if it does not exist or is the wrong
     * type.
     */
    public fun getInt(key: String, default: Int): Int = (get(key) as? Number)?.toInt() ?: default

    /** Returns the [Long] at the given key, or 0 if it does not exist or is the wrong type. */
    public fun getLong(key: String): Long = getLong(key, 0L)

    /**
     * Returns the [Long] at the given key, or the default if it does not exist or is the wrong
     * type.
     */
    public fun getLong(key: String, default: Long): Long = (get(key) as? Number)?.toLong() ?: default

    /** Returns the [Number] at the given key, or null if it does not exist or is the wrong type. */
    public fun getNumber(key: String): Number? = getNumber(key, null)

    /**
     * Returns the [Number] at the given key, or the default if it does not exist or is the wrong
     * type.
     */
    public fun getNumber(key: String, default: Number?): Number? = get(key) as? Number ?: default

    /**
     * Returns the [JsonObject] at the given key, or an empty one if it does not exist or is the
     * wrong type.
     */
    public fun getObject(key: String): JsonObject = get(key) as? JsonObject ?: JsonObject()

    /**
     * Returns the [JsonObject] at the given key, or the default if it does not exist or is the
     * wrong type.
     */
    public fun getObject(key: String, default: JsonObject?): JsonObject? =
        get(key) as? JsonObject ?: default

    /** Returns the [String] at the given key, or null if it does not exist or is the wrong type. */
    public fun getString(key: String): String? = getString(key, null)

    /**
     * Returns the [String] at the given key, or the default if it does not exist or is the wrong
     * type.
     */
    public fun getString(key: String, default: String?): String? = get(key) as? String ?: default

    /** Returns true if the object has an element at that key (even if that element is null). */
    public fun has(key: String): Boolean = containsKey(key)

    /** Returns true if the object has a boolean element at that key. */
    public fun isBoolean(key: String): Boolean = get(key) is Boolean

    /** Returns true if the object has a null element at that key. */
    public fun isNull(key: String): Boolean = containsKey(key) && get(key) == null

    /** Returns true if the object has a number element at that key. */
    public fun isNumber(key: String): Boolean = get(key) is Number

    /** Returns true if the object has a string element at that key. */
    public fun isString(key: String): Boolean = get(key) is String

    public companion object {
        private const val serialVersionUID: Long = 1L

        /** Creates a [JsonBuilder] for a [JsonObject]. */
        @JvmStatic
        public fun builder(): JsonBuilder<JsonObject> = JsonBuilder(JsonObject())
    }
}

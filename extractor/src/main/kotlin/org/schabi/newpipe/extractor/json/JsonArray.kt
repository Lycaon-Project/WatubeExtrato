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

import java.util.stream.Stream

/**
 * Extends an [ArrayList] with helper methods to determine the underlying JSON type of the list
 * element.
 *
 * The single-argument getters of a container ([getObject], [getArray]) only create the empty
 * default when the element is missing or of the wrong type, not on every call.
 */
public open class JsonArray : ArrayList<Any?> {

    /** Creates an empty [JsonArray] with the default capacity. */
    public constructor() : super()

    /** Creates an empty [JsonArray] with the given initial capacity. */
    public constructor(initialCapacity: Int) : super(initialCapacity)

    /** Creates a [JsonArray] from the given collection of objects. */
    public constructor(collection: Collection<*>) : super(collection)

    /** Returns the underlying object at the given index, or null if it does not exist. */
    override fun get(index: Int): Any? = if (index < size) super.get(index) else null

    /**
     * Returns the [JsonArray] at the given index, or an empty one if it does not exist or is the
     * wrong type.
     */
    public fun getArray(key: Int): JsonArray = get(key) as? JsonArray ?: JsonArray()

    /**
     * Returns the [JsonArray] at the given index, or the default if it does not exist or is the
     * wrong type.
     */
    public fun getArray(key: Int, default: JsonArray?): JsonArray? = get(key) as? JsonArray ?: default

    /** Returns the [Boolean] at the given index, or false if it does not exist or is the wrong type. */
    public fun getBoolean(key: Int): Boolean = getBoolean(key, false)

    /**
     * Returns the [Boolean] at the given index, or the default if it does not exist or is the
     * wrong type.
     */
    public fun getBoolean(key: Int, default: Boolean): Boolean = get(key) as? Boolean ?: default

    /** Returns the [Double] at the given index, or 0.0 if it does not exist or is the wrong type. */
    public fun getDouble(key: Int): Double = getDouble(key, 0.0)

    /**
     * Returns the [Double] at the given index, or the default if it does not exist or is the
     * wrong type.
     */
    public fun getDouble(key: Int, default: Double): Double =
        (get(key) as? Number)?.toDouble() ?: default

    /** Returns the [Float] at the given index, or 0.0f if it does not exist or is the wrong type. */
    public fun getFloat(key: Int): Float = getFloat(key, 0f)

    /**
     * Returns the [Float] at the given index, or the default if it does not exist or is the wrong
     * type.
     */
    public fun getFloat(key: Int, default: Float): Float =
        (get(key) as? Number)?.toFloat() ?: default

    /** Returns the [Int] at the given index, or 0 if it does not exist or is the wrong type. */
    public fun getInt(key: Int): Int = getInt(key, 0)

    /**
     * Returns the [Int] at the given index, or the default if it does not exist or is the wrong
     * type.
     */
    public fun getInt(key: Int, default: Int): Int = (get(key) as? Number)?.toInt() ?: default

    /** Returns the [Long] at the given index, or 0 if it does not exist or is the wrong type. */
    public fun getLong(key: Int): Long = getLong(key, 0L)

    /**
     * Returns the [Long] at the given index, or the default if it does not exist or is the wrong
     * type.
     */
    public fun getLong(key: Int, default: Long): Long = (get(key) as? Number)?.toLong() ?: default

    /** Returns the [Number] at the given index, or null if it does not exist or is the wrong type. */
    public fun getNumber(key: Int): Number? = getNumber(key, null)

    /**
     * Returns the [Number] at the given index, or the default if it does not exist or is the
     * wrong type.
     */
    public fun getNumber(key: Int, default: Number?): Number? = get(key) as? Number ?: default

    /**
     * Returns the [JsonObject] at the given index, or an empty one if it does not exist or is the
     * wrong type.
     */
    public fun getObject(key: Int): JsonObject = get(key) as? JsonObject ?: JsonObject()

    /**
     * Returns the [JsonObject] at the given index, or the default if it does not exist or is the
     * wrong type.
     */
    public fun getObject(key: Int, default: JsonObject?): JsonObject? =
        get(key) as? JsonObject ?: default

    /** Returns the [String] at the given index, or null if it does not exist or is the wrong type. */
    public fun getString(key: Int): String? = getString(key, null)

    /**
     * Returns the [String] at the given index, or the default if it does not exist or is the
     * wrong type.
     */
    public fun getString(key: Int, default: String?): String? = get(key) as? String ?: default

    /** Returns true if the array has an element at that index (even if that element is null). */
    public fun has(key: Int): Boolean = key < size

    /** Returns true if the array has a boolean element at that index. */
    public fun isBoolean(key: Int): Boolean = get(key) is Boolean

    /** Returns true if the array has a null element at that index. */
    public fun isNull(key: Int): Boolean = key < size && get(key) == null

    /** Returns true if the array has a number element at that index. */
    public fun isNumber(key: Int): Boolean = get(key) is Number

    /** Returns true if the array has a string element at that index. */
    public fun isString(key: Int): Boolean = get(key) is String

    /** Shortcut method to create a stream with only objects of these classes. */
    public fun <T : Any> streamAs(clazz: Class<T>): Stream<T> =
        stream().filter { clazz.isInstance(it) }.map { clazz.cast(it) }

    /** Shortcut method to create a stream that only contains [JsonObject]s. */
    public fun streamAsJsonObjects(): Stream<JsonObject> = streamAs(JsonObject::class.java)

    public companion object {
        private const val serialVersionUID: Long = 1L

        /** Creates a [JsonArray] from an array of contents. */
        @JvmStatic
        public fun from(vararg contents: Any?): JsonArray = JsonArray(contents.asList())

        /** Creates a [JsonBuilder] for a [JsonArray]. */
        @JvmStatic
        public fun builder(): JsonBuilder<JsonArray> = JsonBuilder(JsonArray())
    }
}

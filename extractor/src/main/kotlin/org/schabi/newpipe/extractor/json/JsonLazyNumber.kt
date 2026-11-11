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

import java.math.BigDecimal

/**
 * Lazily-parsed number for performance: the text of the number is only converted when one of
 * the value functions is called. The text was already validated by the tokener, and is bounded
 * by [JsonTokener.MAX_NUMBER_LENGTH].
 */
internal class JsonLazyNumber(
    private val value: String,
    private val isDouble: Boolean,
) : Number() {

    override fun toDouble(): Double = value.toDouble()

    override fun toFloat(): Float = value.toFloat()

    // an integer beyond the range narrows like the eager path (BigInteger), instead of throwing
    // an exception that quotes the number
    override fun toInt(): Int = if (isDouble) value.toDouble().toInt() else value.toIntOrNull() ?: BigDecimal(value).toInt()

    override fun toLong(): Long =
        if (isDouble) value.toDouble().toLong() else value.toLongOrNull() ?: BigDecimal(value).toLong()

    // java.lang.Number derives both from intValue()
    override fun toShort(): Short = toInt().toShort()

    override fun toByte(): Byte = toInt().toByte()

    /** The number as it was written, so that it is written back unchanged. */
    override fun toString(): String = value

    /** Avoid serializing [JsonLazyNumber]. */
    private fun writeReplace(): Any = BigDecimal(value)

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

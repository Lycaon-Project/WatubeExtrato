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
 * Thrown when the [JsonParser] encounters malformed JSON.
 *
 * The message only tells what was wrong and where (line and character): it never quotes the
 * parsed text, which comes from the network and may carry personal data.
 */
public class JsonParserException internal constructor(
    cause: Exception?,
    message: String,
    /** The 1-based line position of the error. */
    public val linePosition: Int,
    /** The 1-based character position of the error. */
    public val charPosition: Int,
    /** The 0-based character offset of the error from the beginning of the string. */
    public val charOffset: Int,
) : Exception(message, cause) {

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

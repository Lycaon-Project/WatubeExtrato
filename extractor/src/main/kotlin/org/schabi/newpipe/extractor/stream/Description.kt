/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.stream

import java.io.Serializable

/**
 * A text with its markup [type].
 *
 * Still a JVM record, so that Java callers keep using `content()` and `type()`.
 */
@JvmRecord
public data class Description(val content: String, val type: Type) : Serializable {

    public enum class Type {
        HTML, MARKDOWN, PLAIN_TEXT
    }

    // the text is not printed: logs must not tell what the user looked at
    override fun toString(): String = "Description(type=$type, length=${content.length})"

    public companion object {
        private const val serialVersionUID: Long = 1L

        @JvmField
        public val EMPTY_DESCRIPTION: Description = Description("", Type.PLAIN_TEXT)

        @JvmStatic
        public fun of(content: String?, type: Type): Description =
            if (content.isNullOrEmpty()) EMPTY_DESCRIPTION else Description(content, type)
    }
}

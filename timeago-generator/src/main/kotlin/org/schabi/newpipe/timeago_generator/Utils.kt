/*
 * SPDX-FileCopyrightText: 2019-2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.timeago_generator

internal object Utils {

    /** The time units, from the shortest to the longest. */
    val UNIT_NAMES: List<String> = listOf("seconds", "minutes", "hours", "days", "weeks", "months", "years")

    private val NON_DIGITS = Regex("\\D")

    /** Compares strings by the number they contain (0 when none). */
    fun compareByNumber(): Comparator<String> = Comparator { o1, o2 -> extractInt(o1) - extractInt(o2) }

    /** Compares unit names in the order of [UNIT_NAMES]. */
    fun compareByUnitName(): Comparator<Any> = Comparator { o1, o2 ->
        UNIT_NAMES.indexOf(o1.toString()).compareTo(UNIT_NAMES.indexOf(o2.toString()))
    }

    /** The digits of [s]. */
    fun digitsOf(s: String): String = s.replace(NON_DIGITS, "")

    private fun extractInt(s: String): Int {
        val num = digitsOf(s)
        return if (num.isEmpty()) 0 else num.toInt()
    }
}

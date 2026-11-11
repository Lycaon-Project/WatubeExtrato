/*
 * SPDX-FileCopyrightText: 2019-2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.timeago_generator

import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonParser
import java.io.File

/**
 * Checks `timeago-parser/raw/times`: that every unit has all its amounts for every language, and
 * that each phrase contains the number of its amount.
 *
 * Run from the root of the repository.
 */
object CheckAll {

    /** How many amounts each unit must have (1 to 59 seconds, ... 1 to 12 years). */
    private val EXPECTED = linkedMapOf(
        "seconds" to 59, "minutes" to 59, "hours" to 23, "days" to 6, "weeks" to 4,
        "months" to 11, "years" to 12,
    )
    private const val MIN_LANGUAGES = 80

    @JvmStatic
    fun main(args: Array<String>) {
        val found = HashMap<String, Int>()

        for (name in EXPECTED.keys) {
            val unitObject = File("timeago-parser/raw/times/$name.json").inputStream().use {
                JsonParser.`object`().from(it)
            }

            for ((key, entryValue) in unitObject) {
                val value = entryValue as JsonObject

                val size = value.size
                if (size >= MIN_LANGUAGES) {
                    found[name] = (found[name] ?: 0) + 1
                } else {
                    System.err.println("Missing some units in: $name → $key (current size = $size)")
                }

                val number = Utils.digitsOf(key)
                for ((lang, langRawValue) in value) {
                    val langValue = langRawValue.toString()
                    val langValueNumber = Utils.digitsOf(langValue)
                    if (langValueNumber != number) {
                        val msg = if (langValueNumber.isEmpty()) "doesn't contain number" else "different number"
                        print(String.format("%-20s[!]   %22s: %10s   = %s \n", key, msg, lang, langValue))
                    }
                }
            }
        }
        println("\n\nHow many:\n")

        for ((name, expected) in EXPECTED) {
            val current = found[name] ?: 0
            println(if (current == expected) "$name: $current" else "[!] missing $name: $current")
        }
    }
}

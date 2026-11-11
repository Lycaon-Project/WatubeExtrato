/*
 * SPDX-FileCopyrightText: 2019-2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.timeago

import java.time.temporal.ChronoUnit
import java.util.Collections
import java.util.EnumMap

/**
 * The "time ago" phrases of one language: the words of each time unit, the special cases (such
 * as "two days" said in one word) and the separator between words.
 *
 * A holder is a shared singleton, read by every extraction: it is immutable once built, and the
 * map of [asMap] is built once instead of on every parsed date.
 */
public abstract class PatternsHolder protected constructor(
    wordSeparator: String,
    seconds: Collection<String>,
    minutes: Collection<String>,
    hours: Collection<String>,
    days: Collection<String>,
    weeks: Collection<String>,
    months: Collection<String>,
    years: Collection<String>,
) {
    private val separator = wordSeparator
    private val secondsPhrases = seconds
    private val minutesPhrases = minutes
    private val hoursPhrases = hours
    private val daysPhrases = days
    private val weeksPhrases = weeks
    private val monthsPhrases = months
    private val yearsPhrases = years

    protected constructor(
        wordSeparator: String,
        seconds: Array<String>,
        minutes: Array<String>,
        hours: Array<String>,
        days: Array<String>,
        weeks: Array<String>,
        months: Array<String>,
        years: Array<String>,
    ) : this(
        wordSeparator, seconds.asList(), minutes.asList(), hours.asList(), days.asList(),
        weeks.asList(), months.asList(), years.asList(),
    )

    private val specialCases: MutableMap<ChronoUnit, MutableMap<String, Int>> =
        EnumMap(ChronoUnit::class.java)
    private val specialCasesView: Map<ChronoUnit, Map<String, Int>> =
        Collections.unmodifiableMap(specialCases)

    private val unitsMap: Map<ChronoUnit, Collection<String>> = Collections.unmodifiableMap(
        EnumMap<ChronoUnit, Collection<String>>(ChronoUnit::class.java).apply {
            put(ChronoUnit.SECONDS, secondsPhrases)
            put(ChronoUnit.MINUTES, minutesPhrases)
            put(ChronoUnit.HOURS, hoursPhrases)
            put(ChronoUnit.DAYS, daysPhrases)
            put(ChronoUnit.WEEKS, weeksPhrases)
            put(ChronoUnit.MONTHS, monthsPhrases)
            put(ChronoUnit.YEARS, yearsPhrases)
        },
    )

    public fun wordSeparator(): String = separator

    public fun seconds(): Collection<String> = secondsPhrases

    public fun minutes(): Collection<String> = minutesPhrases

    public fun hours(): Collection<String> = hoursPhrases

    public fun days(): Collection<String> = daysPhrases

    public fun weeks(): Collection<String> = weeksPhrases

    public fun months(): Collection<String> = monthsPhrases

    public fun years(): Collection<String> = yearsPhrases

    /** The phrases meaning an amount on their own, by time unit, in their declaration order. */
    public fun specialCases(): Map<ChronoUnit, Map<String, Int>> = specialCasesView

    protected fun putSpecialCase(unit: ChronoUnit, caseText: String, caseAmount: Int) {
        specialCases.getOrPut(unit) { LinkedHashMap() }[caseText] = caseAmount
    }

    /** The phrases of every time unit, from seconds to years (a read-only map, built once). */
    public fun asMap(): Map<ChronoUnit, Collection<String>> = unitsMap
}

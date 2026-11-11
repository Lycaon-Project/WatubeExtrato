/*
 * SPDX-FileCopyrightText: 2019-2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.timeago_generator

import org.schabi.newpipe.extractor.json.JsonAppendableWriter
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonParser
import org.schabi.newpipe.extractor.json.JsonWriter
import java.io.File
import java.util.TreeMap
import java.util.TreeSet

/**
 * Writes `timeago-parser/raw/overview.json`: every phrase of `raw/times`, by language and unit.
 *
 * Run from the root of the repository.
 */
object GenerateOverview {

    @JvmStatic
    fun main(args: Array<String>) {
        val outMap = TreeMap<String, MutableMap<String, MutableCollection<String>>>(String.CASE_INSENSITIVE_ORDER)
        for (unitName in Utils.UNIT_NAMES) {
            val unitObject = File("timeago-parser/raw/times/$unitName.json").inputStream().use {
                JsonParser.`object`().from(it)
            }

            for (timeObject in unitObject.values) {
                for ((langKey, langValue) in timeObject as JsonObject) {
                    outMap.getOrPut(langKey) { TreeMap(Utils.compareByUnitName()) }
                        .getOrPut(unitName) { TreeSet(Utils.compareByNumber()) }
                        .add(langValue.toString())
                }
            }
        }

        File("timeago-parser/raw/overview.json").outputStream().use {
            writeMapTo(outMap, JsonWriter.indent("  ").on(it))
        }
    }

    @JvmStatic
    fun writeMapTo(outMap: Map<String, Map<String, Collection<String>>>, out: JsonAppendableWriter) {
        out.`object`()
        for ((langName, langUnits) in outMap) {
            out.`object`(langName)
            for ((unitName, timeValues) in langUnits) {
                out.array(unitName)
                for (timeValue in timeValues) {
                    out.value(timeValue)
                }
                out.end()
            }
            out.end()
        }
        out.end().done()
    }
}

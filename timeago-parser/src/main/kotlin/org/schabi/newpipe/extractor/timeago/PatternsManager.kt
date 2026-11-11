/*
 * SPDX-FileCopyrightText: 2019-2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor.timeago

public object PatternsManager {
    /**
     * Return an holder object containing all the patterns array.
     *
     * @return an object containing the patterns. If not existent, `null`.
     */
    @JvmStatic
    public fun getPatterns(languageCode: String, countryCode: String?): PatternsHolder? {
        val targetLocalizationClassName =
            if (countryCode.isNullOrEmpty()) languageCode else "${languageCode}_$countryCode"
        return PatternMap.getPattern(targetLocalizationClassName)
    }
}

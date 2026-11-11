package org.schabi.newpipe.extractor.localization

import org.schabi.newpipe.extractor.timeago.PatternsHolder
import org.schabi.newpipe.extractor.timeago.PatternsManager
import java.time.LocalDateTime

public object TimeAgoPatternsManager {

    private fun getPatternsFor(localization: Localization): PatternsHolder? =
        PatternsManager.getPatterns(localization.languageCode, localization.countryCode)

    @JvmStatic
    public fun getTimeAgoParserFor(localization: Localization): TimeAgoParser? =
        getTimeAgoParserFor(localization, LocalDateTime.now())

    @JvmStatic
    public fun getTimeAgoParserFor(localization: Localization, now: LocalDateTime): TimeAgoParser? =
        getPatternsFor(localization)?.let { TimeAgoParser(it, now) }
}

package org.schabi.newpipe.extractor.services.youtube

import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestRelatedItems
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * A class that tests multiple channels and ranges of "time ago".
 */
class YoutubeChannelLocalizationTest : InitYoutubeTest {
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    @Test
    fun testAllSupportedLocalizations() {
        testLocalizationsFor("https://www.youtube.com/user/NBCNews")
        testLocalizationsFor("https://www.youtube.com/channel/UCcmpeVbSSQlZRvHfdC-CRwg/videos")
        testLocalizationsFor("https://www.youtube.com/channel/UC65afEgL62PGFWXY7n6CUbA")
        testLocalizationsFor("https://www.youtube.com/channel/UCEOXxzW2vU0P-0THehuIIeg")
    }

    private fun testLocalizationsFor(channelUrl: String) {
        val supportedLocalizations = YouTube.supportedLocalizations
        // val supportedLocalizations = listOf(Localization.DEFAULT, Localization("sr"))
        val results = LinkedHashMap<Localization, List<StreamInfoItem>>()

        for (currentLocalization in supportedLocalizations) {
            if (DEBUG) println("Testing localization = $currentLocalization")

            val itemsPage: ListExtractor.InfoItemsPage<InfoItem>
            try {
                val extractor = YouTube.getChannelExtractor(channelUrl)
                extractor.forceLocalization(currentLocalization)
                extractor.fetchPage()

                // Use Videos tab only
                val tabExtractor = YouTube.getChannelTabExtractor(extractor.tabs[0])
                tabExtractor.fetchPage()
                itemsPage = defaultTestRelatedItems(tabExtractor)
            } catch (e: Throwable) {
                println("[!] $currentLocalization → failed")
                throw e
            }

            val items = itemsPage.items.filterIsInstance<StreamInfoItem>()
            for ((i, item) in items.withIndex()) {
                var debugMessage = "[" + String.format("%02d", i) + "] " +
                    currentLocalization.localizationCode + " → " + item.name +
                    "\n:::: " + item.streamType + ", views = " + item.viewCount
                val uploadDate = item.uploadDate
                if (uploadDate != null) {
                    val dateStr = dateTimeFormatter.format(uploadDate.localDateTime)
                    debugMessage += "\n:::: " + item.textualUploadDate + "\n:::: " + dateStr
                }
                if (DEBUG) println(debugMessage + "\n")
            }
            results[currentLocalization] = items

            if (DEBUG) println("\n===============================\n")
        }

        // Check results
        val referenceList = results.getValue(Localization.DEFAULT)
        var someFail = false

        for ((currentLocalization, currentList) in results) {
            if (currentLocalization == Localization.DEFAULT) {
                continue
            }

            val currentLocalizationCode = currentLocalization.localizationCode
            val referenceLocalizationCode = Localization.DEFAULT.localizationCode
            if (DEBUG) {
                println("Comparing $referenceLocalizationCode with $currentLocalizationCode")
            }

            if (referenceList.size != currentList.size) {
                if (DEBUG) println("[!] $currentLocalizationCode → Lists are not equal")
                someFail = true
                continue
            }

            for (i in 0 until referenceList.size - 1) {
                val referenceItem = referenceList[i]
                val currentItem = currentList[i]

                val referenceUploadDate = referenceItem.uploadDate
                val currentUploadDate = currentItem.uploadDate

                val referenceDateString = if (referenceUploadDate == null) {
                    "null"
                } else {
                    dateTimeFormatter.format(referenceUploadDate.offsetDateTime())
                }
                val currentDateString = if (currentUploadDate == null) {
                    "null"
                } else {
                    dateTimeFormatter.format(currentUploadDate.offsetDateTime())
                }

                var difference = -1L
                if (referenceUploadDate != null && currentUploadDate != null) {
                    difference = ChronoUnit.MILLIS.between(
                        referenceUploadDate.offsetDateTime(),
                        currentUploadDate.offsetDateTime()
                    )
                }

                val areTimeEquals = difference < 5 * 60 * 1000L

                if (!areTimeEquals) {
                    println(
                        "      [!] $currentLocalizationCode → [$i] dates are not equal\n" +
                            "          $referenceLocalizationCode: " +
                            referenceDateString + " → " + referenceItem.textualUploadDate +
                            "\n          $currentLocalizationCode: " +
                            currentDateString + " → " + currentItem.textualUploadDate
                    )
                }
            }
        }

        if (someFail) {
            fail<Unit>("Some localization failed")
        } else {
            if (DEBUG) print("All tests passed\n\n===============================\n\n")
        }
    }

    private companion object {
        const val DEBUG = false
    }
}

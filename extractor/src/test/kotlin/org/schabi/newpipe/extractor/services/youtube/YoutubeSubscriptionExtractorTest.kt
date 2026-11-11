package org.schabi.newpipe.extractor.services.youtube

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.schabi.newpipe.FileUtils.resolveTestResource
import org.schabi.newpipe.extractor.InitNewPipeTest
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeSubscriptionExtractor
import org.schabi.newpipe.extractor.subscription.SubscriptionExtractor
import org.schabi.newpipe.extractor.subscription.SubscriptionExtractor.InvalidSourceException
import org.schabi.newpipe.extractor.subscription.SubscriptionItem
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.io.FileInputStream

/**
 * Test for [YoutubeSubscriptionExtractor]
 */
internal class YoutubeSubscriptionExtractorTest {

    @Test
    fun testFromInputStream() {
        val subscriptionItems = FileInputStream(resolveTestResource("youtube_takeout_import_test.json")).use {
            subscriptionExtractor.fromInputStream(it)
        }
        assertEquals(7, subscriptionItems.size)

        for (item in subscriptionItems) {
            assertTrue(urlHandler.acceptUrl(item.url))
            assertEquals(ServiceList.YouTube.serviceId, item.serviceId)
        }
    }

    @Test
    fun testEmptySourceException() {
        val items = subscriptionExtractor.fromInputStream(ByteArrayInputStream("[]".toByteArray(Charsets.UTF_8)))
        assertTrue(items.isEmpty())
    }

    @Test
    fun testSubscriptionWithEmptyTitleInSource() {
        val source = "[{\"snippet\":{\"resourceId\":{\"channelId\":\"UCEOXxzW2vU0P-0THehuIIeg\"}}}]"
        val items = subscriptionExtractor.fromInputStream(ByteArrayInputStream(source.toByteArray(Charsets.UTF_8)))

        assertEquals(1, items.size)
        assertEquals(ServiceList.YouTube.serviceId, items[0].serviceId)
        assertEquals("https://www.youtube.com/channel/UCEOXxzW2vU0P-0THehuIIeg", items[0].url)
        assertEquals("", items[0].name)
    }

    @Test
    fun testSubscriptionWithInvalidUrlInSource() {
        val source = "[{\"snippet\":{\"resourceId\":{\"channelId\":\"gibberish\"},\"title\":\"name1\"}}," +
            "{\"snippet\":{\"resourceId\":{\"channelId\":\"UCEOXxzW2vU0P-0THehuIIeg\"},\"title\":\"name2\"}}]"
        val items = subscriptionExtractor.fromInputStream(ByteArrayInputStream(source.toByteArray(Charsets.UTF_8)))

        assertEquals(1, items.size)
        assertEquals(ServiceList.YouTube.serviceId, items[0].serviceId)
        assertEquals("https://www.youtube.com/channel/UCEOXxzW2vU0P-0THehuIIeg", items[0].url)
        assertEquals("name2", items[0].name)
    }

    @Test
    fun testInvalidSourceException() {
        val invalidList = listOf(
            "<xml><notvalid></notvalid></xml>",
            "<opml><notvalid></notvalid></opml>",
            "{\"a\":\"b\"}",
            "[{}]",
            "[\"\", 5]",
            "[{\"snippet\":{\"title\":\"name\"}}]",
            "[{\"snippet\":{\"resourceId\":{\"channelId\":\"gibberish\"}}}]",
            "",
            "🐨🐨🐨",
            "gibberish"
        )

        for (invalidContent in invalidList) {
            try {
                val bytes = invalidContent.toByteArray(Charsets.UTF_8)
                subscriptionExtractor.fromInputStream(ByteArrayInputStream(bytes))
                fail<Unit>("Extracting from \"$invalidContent\" didn't throw an exception")
            } catch (e: Exception) {
                val correctType = e is SubscriptionExtractor.InvalidSourceException
                if (!correctType) {
                    e.printStackTrace()
                }
                assertTrue(correctType, e.javaClass.simpleName + " is not InvalidSourceException")
            }
        }
    }

    private fun assertSubscriptionItems(subscriptionItems: List<SubscriptionItem>) {
        assertFalse(subscriptionItems.isEmpty())

        for (item in subscriptionItems) {
            assertTrue(urlHandler.acceptUrl(item.url))
            assertEquals(ServiceList.YouTube.serviceId, item.serviceId)
        }
    }

    @Test
    fun fromZipInputStream() {
        val zipPaths = listOf(
            "youtube_takeout_import_test_1.zip",
            "youtube_takeout_import_test_2.zip"
        )

        for (path in zipPaths) {
            FileInputStream(resolveTestResource(path)).use {
                assertSubscriptionItems(subscriptionExtractor.fromZipInputStream(it))
            }
        }
    }

    @Test
    fun `a csv line without an end is refused instead of kept in memory`() {
        val csv = "Channel Id,Channel Url,Channel Title\n" + "x".repeat(1024 * 1024)
        assertThrows(InvalidSourceException::class.java) {
            subscriptionExtractor.fromCsvInputStream(ByteArrayInputStream(csv.toByteArray()))
        }
    }

    @Test
    fun `an archive that inflates past the limit is refused`() {
        // 65 MiB of zeros in an entry that is not even a csv: a few kilobytes once compressed
        val bomb = ByteArrayOutputStream()
        ZipOutputStream(bomb).use { zip ->
            zip.putNextEntry(ZipEntry("padding.bin"))
            val zeros = ByteArray(1024 * 1024)
            repeat(65) { zip.write(zeros) }
            zip.closeEntry()
        }
        assertThrows(InvalidSourceException::class.java) {
            subscriptionExtractor.fromZipInputStream(ByteArrayInputStream(bomb.toByteArray()))
        }
    }

    @Test
    fun `channel names are cut to a sane length`() {
        val csv = "Channel Id,Channel Url,Channel Title\n" +
            "UC1JTQBa5QxZCpXrFSkMxmPw,http://www.youtube.com/channel/UC1JTQBa5QxZCpXrFSkMxmPw," +
            "n".repeat(1_000) + "\n"
        val items = subscriptionExtractor.fromCsvInputStream(ByteArrayInputStream(csv.toByteArray()))
        assertEquals(256, items.single().name.length)
    }

    @Test
    fun fromCsvInputStream() {
        val csvPaths = listOf(
            "youtube_takeout_import_test_1.csv",
            "youtube_takeout_import_test_2.csv"
        )

        for (path in csvPaths) {
            FileInputStream(resolveTestResource(path)).use {
                assertSubscriptionItems(subscriptionExtractor.fromCsvInputStream(it))
            }
        }
    }

    companion object {
        private lateinit var subscriptionExtractor: YoutubeSubscriptionExtractor
        private lateinit var urlHandler: LinkHandlerFactory

        @JvmStatic
        @BeforeAll
        fun setupClass() {
            InitNewPipeTest.initEmpty()
            subscriptionExtractor = YoutubeSubscriptionExtractor(ServiceList.YouTube)
            urlHandler = ServiceList.YouTube.channelLHFactory
        }
    }
}

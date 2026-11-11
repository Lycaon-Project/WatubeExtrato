package org.schabi.newpipe.extractor.services

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.schabi.newpipe.extractor.ExtractorAsserts.assertEmptyErrors
import org.schabi.newpipe.extractor.ExtractorAsserts.assertGreaterOrEqual
import org.schabi.newpipe.extractor.ExtractorAsserts.assertIsSecureUrl
import org.schabi.newpipe.extractor.ExtractorAsserts.assertNotEmpty
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.StreamingService.LinkType
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.utils.Utils.isNullOrEmpty

object DefaultTests {
    @JvmStatic
    @Throws(ParsingException::class)
    fun defaultTestListOfItems(
        expectedService: StreamingService,
        itemsList: List<InfoItem>,
        errors: List<Throwable>,
    ) {
        assertFalse(itemsList.isEmpty(), "List of items is empty")
        val possiblyNullItems: List<Any?> = itemsList
        assertFalse(possiblyNullItems.contains(null), "List of items contains a null element")
        assertEmptyErrors("Errors during extraction", errors)

        for (item in itemsList) {
            assertIsSecureUrl(item.url)

            val thumbnails = item.thumbnails
            if (!isNullOrEmpty(thumbnails)) {
                defaultTestImageCollection(thumbnails)
            }
            assertNotNull(item.infoType, "InfoItem type not set: $item")
            assertEquals(expectedService.serviceId, item.serviceId, "Unexpected item service id")
            assertNotEmpty("Item name not set: $item", item.name)

            if (item is StreamInfoItem) {
                val uploaderUrl = item.uploaderUrl
                if (!isNullOrEmpty(uploaderUrl)) {
                    assertIsSecureUrl(uploaderUrl)
                    assertExpectedLinkType(expectedService, uploaderUrl!!, LinkType.CHANNEL)
                }

                val uploaderAvatars = item.uploaderAvatars
                if (!isNullOrEmpty(uploaderAvatars)) {
                    defaultTestImageCollection(uploaderAvatars)
                }

                assertExpectedLinkType(expectedService, item.url, LinkType.STREAM)

                if (!isNullOrEmpty(item.textualUploadDate)) {
                    val uploadDate = item.uploadDate
                    assertNotNull(uploadDate, "No parsed upload date")
                }
            } else if (item is ChannelInfoItem) {
                assertExpectedLinkType(expectedService, item.url, LinkType.CHANNEL)
            } else if (item is PlaylistInfoItem) {
                assertExpectedLinkType(expectedService, item.url, LinkType.PLAYLIST)
            }
        }
    }

    @Throws(ParsingException::class)
    private fun assertExpectedLinkType(
        expectedService: StreamingService,
        url: String,
        expectedLinkType: LinkType,
    ) {
        val linkTypeByUrl = expectedService.getLinkTypeByUrl(url)

        assertNotEquals(
            LinkType.NONE,
            linkTypeByUrl,
            "Url is not recognized by its own service: \"$url\"",
        )
        assertEquals(
            expectedLinkType,
            linkTypeByUrl,
            "Service returned wrong link type for: \"$url\"",
        )
    }

    @JvmStatic
    fun assertOnlyContainsType(
        items: ListExtractor.InfoItemsPage<out InfoItem>,
        expectedType: InfoItem.InfoType,
    ) {
        for (item in items.items) {
            assertEquals(
                expectedType,
                item.infoType,
                "Item list contains unexpected info types",
            )
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun <T : InfoItem> assertNoMoreItems(extractor: ListExtractor<T>) {
        val initialPage = extractor.initialPage
        assertFalse(initialPage.hasNextPage(), "More items available when it shouldn't")
    }

    @JvmStatic
    @Throws(Exception::class)
    fun assertNoDuplicatedItems(
        expectedService: StreamingService,
        page1: ListExtractor.InfoItemsPage<InfoItem>,
        page2: ListExtractor.InfoItemsPage<InfoItem>,
    ) {
        defaultTestListOfItems(expectedService, page1.items, page1.errors)
        defaultTestListOfItems(expectedService, page2.items, page2.errors)

        val urlsSet = HashSet<String>()
        for (item in page1.items) {
            urlsSet.add(item.url)
        }

        for (item in page2.items) {
            val wasAdded = urlsSet.add(item.url)
            if (!wasAdded) {
                fail<Unit>("Same item was on the first and second page item list")
            }
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun <T : InfoItem> defaultTestRelatedItems(
        extractor: ListExtractor<T>,
    ): ListExtractor.InfoItemsPage<T> {
        val page = extractor.initialPage
        val itemsList = page.items
        val errors = page.errors

        defaultTestListOfItems(extractor.service, itemsList, errors)
        return page
    }

    @JvmStatic
    @Throws(Exception::class)
    fun <T : InfoItem> defaultTestMoreItems(
        extractor: ListExtractor<T>,
    ): ListExtractor.InfoItemsPage<T> {
        val initialPage = extractor.initialPage
        assertTrue(initialPage.hasNextPage(), "Doesn't have more items")
        val nextPage = extractor.getPage(initialPage.nextPage)
        val items = nextPage.items
        assertFalse(items.isEmpty(), "Next page is empty")
        assertEmptyErrors("Next page have errors", nextPage.errors)

        defaultTestListOfItems(extractor.service, nextPage.items, nextPage.errors)
        return nextPage
    }

    @JvmStatic
    @Throws(Exception::class)
    fun defaultTestGetPageInNewExtractor(
        extractor: ListExtractor<out InfoItem>,
        newExtractor: ListExtractor<out InfoItem>,
    ) {
        val nextPage: Page? = extractor.initialPage.nextPage

        val page = newExtractor.getPage(nextPage)
        defaultTestListOfItems(extractor.service, page.items, page.errors)
    }

    @JvmStatic
    fun defaultTestImageCollection(imageCollection: Collection<Image>?) {
        assertNotNull(imageCollection)
        imageCollection!!.forEach { image ->
            assertIsSecureUrl(image.url)
            assertGreaterOrEqual(
                Image.HEIGHT_UNKNOWN.toLong(),
                image.height.toLong(),
                "Unexpected image height: ${image.height}",
            )
            assertGreaterOrEqual(
                Image.WIDTH_UNKNOWN.toLong(),
                image.width.toLong(),
                "Unexpected image width: ${image.width}",
            )
        }
    }
}

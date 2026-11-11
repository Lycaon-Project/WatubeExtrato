package org.schabi.newpipe.extractor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.utils.Utils
import java.net.MalformedURLException
import java.net.URL

object ExtractorAsserts {
    @JvmStatic
    fun assertEmptyErrors(message: String, errors: List<Throwable>) {
        if (errors.isNotEmpty()) {
            val messageBuilder = StringBuilder(message)
            for (e in errors) {
                messageBuilder.append("\n  * ").append(e.message)
            }
            messageBuilder.append(" ")
            throw AssertionError(messageBuilder.toString(), errors[0])
        }
    }

    // URL(String) is deprecated since JDK 20, but it is the lenient parser these assertions were
    // always written against (java.net.URI is stricter and would reject some real item links)
    @Suppress("DEPRECATION")
    private fun urlFromString(url: String?): URL {
        try {
            return URL(url)
        } catch (e: MalformedURLException) {
            throw AssertionError("Invalid url: \"$url\"", e)
        }
    }

    @JvmStatic
    fun assertIsValidUrl(url: String?) {
        urlFromString(url)
    }

    @JvmStatic
    fun assertIsSecureUrl(urlToCheck: String?) {
        val url = urlFromString(urlToCheck)
        assertEquals("https", url.protocol, "Protocol of URL is not secure")
    }

    @JvmStatic
    fun assertNotEmpty(stringToCheck: String?) {
        assertNotEmpty(null, stringToCheck)
    }

    @JvmStatic
    fun assertNotEmpty(message: String?, stringToCheck: String?) {
        assertNotNull(stringToCheck, message)
        assertFalse(stringToCheck!!.isEmpty(), message)
    }

    @JvmStatic
    fun assertNotEmpty(collectionToCheck: Collection<*>?) {
        assertNotEmpty(null, collectionToCheck)
    }

    @JvmStatic
    fun assertNotEmpty(message: String?, collectionToCheck: Collection<*>?) {
        assertNotNull(collectionToCheck)
        assertFalse(collectionToCheck!!.isEmpty(), message)
    }

    @JvmStatic
    fun assertEmpty(stringToCheck: String?) {
        assertEmpty(null, stringToCheck)
    }

    @JvmStatic
    fun assertEmpty(message: String?, stringToCheck: String?) {
        if (stringToCheck != null) {
            assertTrue(stringToCheck.isEmpty(), message)
        }
    }

    @JvmStatic
    fun assertEmpty(collectionToCheck: Collection<*>?) {
        if (collectionToCheck != null) {
            assertTrue(collectionToCheck.isEmpty())
        }
    }

    @JvmStatic
    fun assertNotBlank(stringToCheck: String?) {
        assertNotBlank(stringToCheck, null)
    }

    @JvmStatic
    fun assertNotBlank(stringToCheck: String?, message: String?) {
        assertFalse(Utils.isBlank(stringToCheck), message)
    }

    @JvmStatic
    fun assertGreater(expected: Long, actual: Long) {
        assertGreater(expected, actual, "$actual is not > $expected")
    }

    @JvmStatic
    fun assertGreater(expected: Long, actual: Long, message: String) {
        assertTrue(actual > expected, message)
    }

    // Int overloads: Kotlin does not widen an Int variable to Long like Java does
    @JvmStatic
    fun assertGreater(expected: Int, actual: Int) {
        assertGreater(expected.toLong(), actual.toLong())
    }

    @JvmStatic
    fun assertGreater(expected: Int, actual: Int, message: String) {
        assertGreater(expected.toLong(), actual.toLong(), message)
    }

    @JvmStatic
    fun assertGreaterOrEqual(expected: Long, actual: Long) {
        assertGreaterOrEqual(expected, actual, "$actual is not >= $expected")
    }

    @JvmStatic
    fun assertGreaterOrEqual(expected: Long, actual: Long, message: String) {
        assertTrue(actual >= expected, message)
    }

    @JvmStatic
    fun assertGreaterOrEqual(expected: Int, actual: Int) {
        assertGreaterOrEqual(expected.toLong(), actual.toLong())
    }

    @JvmStatic
    fun assertGreaterOrEqual(expected: Int, actual: Int, message: String) {
        assertGreaterOrEqual(expected.toLong(), actual.toLong(), message)
    }

    @JvmStatic
    fun assertLess(expected: Long, actual: Long) {
        assertLess(expected, actual, "$actual is not < $expected")
    }

    @JvmStatic
    fun assertLess(expected: Long, actual: Long, message: String) {
        assertTrue(actual < expected, message)
    }

    @JvmStatic
    fun assertLessOrEqual(expected: Long, actual: Long) {
        assertLessOrEqual(expected, actual, "$actual is not <= $expected")
    }

    @JvmStatic
    fun assertLessOrEqual(expected: Long, actual: Long, message: String) {
        assertTrue(actual <= expected, message)
    }

    // The Java version sorted both lists in place; sorted copies give the same verdict and also
    // accept read-only lists
    @JvmStatic
    fun assertEqualsOrderIndependent(expected: List<String>?, actual: List<String>?) {
        if (expected == null) {
            assertNull(actual)
            return
        }
        assertNotNull(actual)
        assertEquals(ArrayList(expected.sorted()), ArrayList(actual!!.sorted()))
    }

    @JvmStatic
    fun assertContains(shouldBeContained: String?, container: String?) {
        assertNotNull(shouldBeContained, "shouldBeContained is null")
        assertNotNull(container, "container is null")
        assertTrue(
            container!!.contains(shouldBeContained!!),
            "'$shouldBeContained' should be contained inside '$container'",
        )
    }

    @JvmStatic
    fun <T> assertContains(shouldBeContained: T, container: List<T>?) {
        assertNotNull(shouldBeContained, "shouldBeContained is null")
        assertNotNull(container, "container is null")
        val containerString = container!!.joinToString(", ") { it.toString() }
        assertTrue(
            container.contains(shouldBeContained),
            "'$shouldBeContained' should be contained inside [$containerString]",
        )
    }

    @JvmStatic
    fun assertTabsContain(tabs: List<ListLinkHandler>, vararg expectedTabs: String) {
        val tabSet = tabs.map { linkHandler ->
            assertEquals(
                1,
                linkHandler.contentFilters.size,
                "Unexpected content filters for channel tab: ${linkHandler.contentFilters}",
            )
            linkHandler.contentFilters[0]
        }.toSet()

        assertEquals(
            expectedTabs.size,
            tabSet.size,
            "Different amount of tabs returned:\nExpected: " +
                "${expectedTabs.contentToString()}\nActual: $tabSet",
        )
        expectedTabs.forEach { expectedTab ->
            assertTrue(tabSet.contains(expectedTab), "Missing $expectedTab tab (got $tabSet)")
        }
    }

    @JvmStatic
    fun assertContainsImageUrlInImageCollection(
        exceptedImageUrlContained: String?,
        imageCollection: Collection<Image>?,
    ) {
        assertNotNull(exceptedImageUrlContained, "exceptedImageUrlContained is null")
        assertNotNull(imageCollection, "imageCollection is null")
        assertTrue(imageCollection!!.any { image -> image.url == exceptedImageUrlContained })
    }

    @JvmStatic
    fun assertContainsOnlyEquivalentImages(
        firstImageCollection: Collection<Image>?,
        secondImageCollection: Collection<Image>?,
    ) {
        assertNotNull(firstImageCollection)
        assertNotNull(secondImageCollection)
        assertEquals(firstImageCollection!!.size, secondImageCollection!!.size)

        firstImageCollection.forEach { exceptedImage ->
            assertTrue(
                secondImageCollection.any { image ->
                    exceptedImage.url == image.url &&
                        exceptedImage.height == image.height &&
                        exceptedImage.width == image.width
                },
            )
        }
    }

    @JvmStatic
    fun assertNotOnlyContainsEquivalentImages(
        firstImageCollection: Collection<Image>?,
        secondImageCollection: Collection<Image>?,
    ) {
        assertNotNull(firstImageCollection)
        assertNotNull(secondImageCollection)

        if (secondImageCollection!!.size != firstImageCollection!!.size) {
            return
        }

        for (unexpectedImage in firstImageCollection) {
            for (image in secondImageCollection) {
                if (image.url != unexpectedImage.url ||
                    image.height != unexpectedImage.height ||
                    image.width != unexpectedImage.width
                ) {
                    return
                }
            }
        }

        throw AssertionError("All excepted images have an equivalent in the image list")
    }
}

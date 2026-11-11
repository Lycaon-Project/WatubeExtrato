package org.schabi.newpipe.extractor.services.peertube

import org.junit.jupiter.api.Assertions.assertFalse
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory

object PeertubeLinkHandlerFactoryTestHelper {

    private val NON_URLS = listOf(
        "orchestr/a/",
        "/a/",
        "something/c/",
        "/c/",
        "videos/",
        "I-hate-videos/",
        "/w/",
        "ksmg/w/",
        "a reandom search query",
        "test 230 ",
        "986513"
    )

    @JvmStatic
    fun assertDoNotAcceptNonURLs(linkHandler: LinkHandlerFactory) {
        NON_URLS.forEach { assertFalse(linkHandler.acceptUrl(it)) }
    }

    @JvmStatic
    fun assertDoNotAcceptNonURLs(linkHandler: ListLinkHandlerFactory) {
        NON_URLS.forEach { assertFalse(linkHandler.acceptUrl(it)) }
    }
}

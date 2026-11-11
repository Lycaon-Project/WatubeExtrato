package org.schabi.newpipe.extractor.services.soundcloud

import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.schabi.newpipe.extractor.InitNewPipeTest
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory
import org.schabi.newpipe.extractor.services.soundcloud.extractors.SoundcloudSubscriptionExtractor
import org.schabi.newpipe.extractor.subscription.SubscriptionExtractor

/**
 * Test for [SoundcloudSubscriptionExtractor]
 */
class SoundcloudSubscriptionExtractorTest : InitNewPipeTest {
    private lateinit var subscriptionExtractor: SoundcloudSubscriptionExtractor
    private lateinit var urlHandler: LinkHandlerFactory

    @BeforeAll
    override fun setUp() {
        super.setUp()
        subscriptionExtractor = SoundcloudSubscriptionExtractor(ServiceList.SoundCloud)
        urlHandler = ServiceList.SoundCloud.channelLHFactory
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "https://soundcloud.com/monstercat",
            "http://soundcloud.com/monstercat",
            "soundcloud.com/monstercat",
            "monstercat",
            // Empty followings user
            "some-random-user-184047028"
        ]
    )
    fun testFromChannelUrl(channelUrl: String) {
        for (item in subscriptionExtractor.fromChannelUrl(channelUrl)) {
            assertNotNull(item.name)
            assertNotNull(item.url)
            assertTrue(urlHandler.acceptUrl(item.url))
            assertNotEquals(-1, item.serviceId)
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "httttps://invalid.com/user",
            ".com/monstercat",
            "ithinkthatthisuserdontexist",
            ""
        ]
    )
    fun testInvalidSourceException(invalidUser: String) {
        assertThrows(SubscriptionExtractor.InvalidSourceException::class.java) {
            subscriptionExtractor.fromChannelUrl(invalidUser)
        }
    }

    // null can't be added to the above value source because it's not a constant
    @Test
    fun testInvalidSourceExceptionWhenUrlIsNull() {
        assertThrows(SubscriptionExtractor.InvalidSourceException::class.java) {
            subscriptionExtractor.fromChannelUrl(null)
        }
    }
}

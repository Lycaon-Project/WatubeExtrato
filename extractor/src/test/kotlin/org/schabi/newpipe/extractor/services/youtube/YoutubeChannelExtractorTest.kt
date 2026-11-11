package org.schabi.newpipe.extractor.services.youtube

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ExtractorAsserts
import org.schabi.newpipe.extractor.ExtractorAsserts.assertContains
import org.schabi.newpipe.extractor.ExtractorAsserts.assertEmpty
import org.schabi.newpipe.extractor.ExtractorAsserts.assertNotEmpty
import org.schabi.newpipe.extractor.ExtractorAsserts.assertTabsContain
import org.schabi.newpipe.extractor.InitNewPipeTest
import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.channel.ChannelExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.exceptions.AccountTerminatedException
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.linkhandler.ReadyChannelTabListLinkHandler
import org.schabi.newpipe.extractor.services.BaseChannelExtractorTest
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestGetPageInNewExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeChannelExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeChannelTabPlaylistExtractor
import org.schabi.newpipe.extractor.utils.Utils.isNullOrEmpty

/**
 * Test for [ChannelExtractor]
 */
class YoutubeChannelExtractorTest {

    /**
     * See [here](https://youtube.fandom.com/wiki/Termination#Ban_Messages)
     * for a list of account termination messages.
     */
    class NotAvailable : InitYoutubeTest {
        @Test
        fun deletedFetch() {
            val extractor =
                YouTube.getChannelExtractor("https://www.youtube.com/channel/UCAUc4iz6edWerIjlnL8OSSw")

            assertThrows(ContentNotAvailableException::class.java) { extractor.fetchPage() }
        }

        @Test
        fun nonExistentFetch() {
            val extractor = YouTube.getChannelExtractor("https://www.youtube.com/channel/DOESNT-EXIST")

            assertThrows(ContentNotAvailableException::class.java) { extractor.fetchPage() }
        }

        @Test
        fun accountTerminatedTOSFetch() {
            // "This account has been terminated for a violation of YouTube's Terms of Service."
            assertTerminatedForViolation("https://www.youtube.com/channel/UCTGjY2I-ZUGnwVoWAGRd7XQ")
        }

        @Test
        fun accountTerminatedCommunityFetch() {
            // "This account has been terminated for violating YouTube's Community Guidelines."
            assertTerminatedForViolation("https://www.youtube.com/channel/UC-nQp2ewj2Yeg5w7VyoVBwQ")
        }

        @Test
        fun accountTerminatedHateFetch() {
            // "This account has been terminated due to multiple or severe violations
            // of YouTube's policy prohibiting hate speech."
            assertTerminatedForViolation("https://www.youtube.com/channel/UCPWXIOPK-9myzek6jHR5yrg")
        }

        @Test
        fun accountTerminatedSpamFetch() {
            // "This account has been terminated due to multiple or severe violations
            // of YouTube's policy against spam, deceptive practices and misleading content
            // or other Terms of Service violations."
            assertTerminatedForViolation("https://www.youtube.com/channel/UCoaO4U_p7G7AwalqSbGCZOA")
        }

        @Test
        fun accountTerminatedCopyrightFetch() {
            // "This account has been terminated because we received multiple third-party claims
            // of copyright infringement regarding material that the user posted."
            assertTerminatedForViolation("https://www.youtube.com/channel/UCI4i4RgFT5ilfMpna4Z_Y8w")
        }

        private fun assertTerminatedForViolation(url: String) {
            val extractor = YouTube.getChannelExtractor(url)

            val ex = assertThrows(AccountTerminatedException::class.java) { extractor.fetchPage() }
            assertEquals(AccountTerminatedException.Reason.VIOLATION, ex.reason)
        }
    }

    class SystemTopic : InitYoutubeTest {
        @Test
        fun noSupportedTab() {
            val extractor =
                YouTube.getChannelExtractor("https://invidious.nerdvpn.de/channel/UC-9-kyTW8ZkZNDHQJ6FgpwQ")

            extractor.fetchPage()
            assertTrue(extractor.tabs.isEmpty())
        }
    }

    abstract class Base :
        DefaultSimpleExtractorTest<YoutubeChannelExtractor>(),
        BaseChannelExtractorTest,
        InitYoutubeTest {

        override fun createExtractor(): YoutubeChannelExtractor =
            YouTube.getChannelExtractor(extractorUrl()) as YoutubeChannelExtractor

        protected abstract fun extractorUrl(): String

        /** Asserts that the videos tab (if any) is a ready tab, i.e. its data is already known. */
        protected fun assertVideosTabsAreReady() {
            assertTrue(
                extractor().tabs
                    .filter { ChannelTabs.VIDEOS == it.contentFilters[0] }
                    .all { it is ReadyChannelTabListLinkHandler }
            )
        }
    }

    class Gronkh : Base() {
        override fun extractorUrl(): String = "http://www.youtube.com/@Gronkh"

        @Test
        override fun testServiceId() {
            assertEquals(YouTube.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertEquals("Gronkh", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("UCYJ61XIK64sp6ZFFS8sctxw", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://www.youtube.com/channel/UCYJ61XIK64sp6ZFFS8sctxw", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals("http://www.youtube.com/@Gronkh", extractor().originalUrl)
        }

        @Test
        override fun testDescription() {
            assertContains("Ungebremster Spieltrieb seit 1896.", extractor().description)
        }

        @Test
        override fun testAvatars() {
            YoutubeTestsUtils.testImages(extractor().avatars)
        }

        @Test
        override fun testBanners() {
            YoutubeTestsUtils.testImages(extractor().banners)
        }

        @Test
        override fun testFeedUrl() {
            assertEquals(
                "https://www.youtube.com/feeds/videos.xml?channel_id=UCYJ61XIK64sp6ZFFS8sctxw",
                extractor().feedUrl
            )
        }

        @Test
        override fun testSubscriberCount() {
            ExtractorAsserts.assertGreaterOrEqual(4_900_000, extractor().subscriberCount)
        }

        @Test
        override fun testVerified() {
            assertTrue(extractor().isVerified)
        }

        @Test
        override fun testTabs() {
            assertTabsContain(extractor().tabs, ChannelTabs.VIDEOS, ChannelTabs.SHORTS, ChannelTabs.PLAYLISTS)
            assertVideosTabsAreReady()
        }

        @Test
        override fun testTags() {
            assertTrue(extractor().tags.contains("gronkh"))
        }
    }

    // YouTube RED/Premium ad blocking test
    class VSauce : Base() {
        override fun extractorUrl(): String = "https://www.youtube.com/user/Vsauce"

        @Test
        override fun testServiceId() {
            assertEquals(YouTube.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertEquals("Vsauce", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("UC6nSFpj9HTCZ5t-N3Rm3-HA", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://www.youtube.com/channel/UC6nSFpj9HTCZ5t-N3Rm3-HA", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals("https://www.youtube.com/user/Vsauce", extractor().originalUrl)
        }

        @Test
        override fun testDescription() {
            assertContains("Our World is Amazing", extractor().description)
        }

        @Test
        override fun testAvatars() {
            YoutubeTestsUtils.testImages(extractor().avatars)
        }

        @Test
        override fun testBanners() {
            YoutubeTestsUtils.testImages(extractor().banners)
        }

        @Test
        override fun testFeedUrl() {
            assertEquals(
                "https://www.youtube.com/feeds/videos.xml?channel_id=UC6nSFpj9HTCZ5t-N3Rm3-HA",
                extractor().feedUrl
            )
        }

        @Test
        override fun testSubscriberCount() {
            ExtractorAsserts.assertGreaterOrEqual(17_000_000, extractor().subscriberCount)
        }

        @Test
        override fun testVerified() {
            assertTrue(extractor().isVerified)
        }

        @Test
        override fun testTabs() {
            assertTabsContain(
                extractor().tabs,
                ChannelTabs.VIDEOS, ChannelTabs.LIVESTREAMS, ChannelTabs.SHORTS, ChannelTabs.PLAYLISTS
            )
            assertVideosTabsAreReady()
        }

        @Test
        override fun testTags() {
            assertTrue(
                extractor().tags.containsAll(listOf("questions", "education", "learning", "schools", "Science"))
            )
        }
    }

    class Kurzgesagt : Base() {
        override fun extractorUrl(): String = "https://www.youtube.com/channel/UCsXVk37bltHxD1rDPwtNM8Q"

        @Test
        override fun testServiceId() {
            assertEquals(YouTube.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertTrue(extractor().name.startsWith("Kurzgesagt"))
        }

        @Test
        override fun testId() {
            assertEquals("UCsXVk37bltHxD1rDPwtNM8Q", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://www.youtube.com/channel/UCsXVk37bltHxD1rDPwtNM8Q", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals("https://www.youtube.com/channel/UCsXVk37bltHxD1rDPwtNM8Q", extractor().originalUrl)
        }

        @Test
        override fun testDescription() {
            ExtractorAsserts.assertContains("science", extractor().description)
            ExtractorAsserts.assertContains("animators", extractor().description)
            // TODO: Description get cuts out, because the og:description is optimized and don't have all the content
            // assertTrue(description, description.contains("Currently we make one animation video per month"));
        }

        @Test
        override fun testAvatars() {
            YoutubeTestsUtils.testImages(extractor().avatars)
        }

        @Test
        override fun testBanners() {
            YoutubeTestsUtils.testImages(extractor().banners)
        }

        @Test
        override fun testFeedUrl() {
            assertEquals(
                "https://www.youtube.com/feeds/videos.xml?channel_id=UCsXVk37bltHxD1rDPwtNM8Q",
                extractor().feedUrl
            )
        }

        @Test
        override fun testSubscriberCount() {
            ExtractorAsserts.assertGreaterOrEqual(17_000_000, extractor().subscriberCount)
        }

        @Test
        override fun testVerified() {
            assertTrue(extractor().isVerified)
        }

        @Test
        override fun testTabs() {
            assertTabsContain(extractor().tabs, ChannelTabs.VIDEOS, ChannelTabs.SHORTS, ChannelTabs.PLAYLISTS)
            assertVideosTabsAreReady()
        }

        @Test
        override fun testTags() {
            assertTrue(
                extractor().tags.containsAll(listOf("universe", "Science", "black hole", "humanism", "evolution"))
            )
        }
    }

    class KurzgesagtAdditional :
        DefaultSimpleExtractorTest<YoutubeChannelExtractor>(),
        InitYoutubeTest {

        private lateinit var tabExtractor: ChannelTabExtractor

        override fun createExtractor(): YoutubeChannelExtractor =
            YouTube.getChannelExtractor("https://www.youtube.com/channel/UCsXVk37bltHxD1rDPwtNM8Q")
                as YoutubeChannelExtractor

        override fun fetchExtractor(extractor: YoutubeChannelExtractor) {
            super.fetchExtractor(extractor)

            tabExtractor = YouTube.getChannelTabExtractor(extractor.tabs[0])
            tabExtractor.fetchPage()
        }

        @Test
        fun testGetPageInNewExtractor() {
            // Make sure initial extractor was initialized
            extractor()
            // Init downloader again for mock as otherwise request confusion occurs when using Mock
            InitNewPipeTest.initNewPipe(this.javaClass, "testGetPageInNewExtractor")

            val newExtractor = YouTube.getChannelExtractor(extractor().url)
            newExtractor.fetchPage()
            val newTabExtractor = YouTube.getChannelTabExtractor(newExtractor.tabs[0])
            defaultTestGetPageInNewExtractor(tabExtractor, newTabExtractor)
        }
    }

    class CaptainDisillusion : Base() {
        override fun extractorUrl(): String = "https://www.youtube.com/user/CaptainDisillusion/videos"

        @Test
        override fun testServiceId() {
            assertEquals(YouTube.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertEquals("Captain Disillusion", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("UCEOXxzW2vU0P-0THehuIIeg", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://www.youtube.com/channel/UCEOXxzW2vU0P-0THehuIIeg", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals("https://www.youtube.com/user/CaptainDisillusion/videos", extractor().originalUrl)
        }

        @Test
        override fun testDescription() {
            assertContains("In a world where", extractor().description)
        }

        @Test
        override fun testAvatars() {
            YoutubeTestsUtils.testImages(extractor().avatars)
        }

        @Test
        override fun testBanners() {
            YoutubeTestsUtils.testImages(extractor().banners)
        }

        @Test
        override fun testFeedUrl() {
            assertEquals(
                "https://www.youtube.com/feeds/videos.xml?channel_id=UCEOXxzW2vU0P-0THehuIIeg",
                extractor().feedUrl
            )
        }

        @Test
        override fun testSubscriberCount() {
            ExtractorAsserts.assertGreaterOrEqual(2_000_000, extractor().subscriberCount)
        }

        @Test
        override fun testVerified() {
            assertTrue(extractor().isVerified)
        }

        @Test
        override fun testTabs() {
            assertTabsContain(extractor().tabs, ChannelTabs.VIDEOS, ChannelTabs.PLAYLISTS, ChannelTabs.SHORTS)
            assertVideosTabsAreReady()
        }

        @Test
        override fun testTags() {
            assertTrue(
                extractor().tags.containsAll(
                    listOf("critical thinking", "visual effects", "VFX", "sci-fi", "humor")
                )
            )
        }
    }

    class RandomChannel : Base() {
        override fun extractorUrl(): String = "https://www.youtube.com/channel/UCUaQMQS9lY5lit3vurpXQ6w"

        @Test
        override fun testServiceId() {
            assertEquals(YouTube.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertEquals("random channel", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("UCUaQMQS9lY5lit3vurpXQ6w", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://www.youtube.com/channel/UCUaQMQS9lY5lit3vurpXQ6w", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals("https://www.youtube.com/channel/UCUaQMQS9lY5lit3vurpXQ6w", extractor().originalUrl)
        }

        @Test
        override fun testDescription() {
            assertContains("Hey there iu will upoload a load of pranks onto this channel", extractor().description)
        }

        @Test
        override fun testAvatars() {
            YoutubeTestsUtils.testImages(extractor().avatars)
        }

        @Test
        override fun testBanners() {
            YoutubeTestsUtils.testImages(extractor().banners)
        }

        @Test
        override fun testFeedUrl() {
            assertEquals(
                "https://www.youtube.com/feeds/videos.xml?channel_id=UCUaQMQS9lY5lit3vurpXQ6w",
                extractor().feedUrl
            )
        }

        @Test
        override fun testSubscriberCount() {
            ExtractorAsserts.assertGreaterOrEqual(50, extractor().subscriberCount)
        }

        @Test
        override fun testVerified() {
            assertFalse(extractor().isVerified)
        }

        @Test
        override fun testTabs() {
            assertTabsContain(extractor().tabs, ChannelTabs.VIDEOS)
            assertVideosTabsAreReady()
        }

        @Test
        override fun testTags() {
            assertTrue(extractor().tags.isEmpty())
        }
    }

    class CarouselHeader : Base() {
        override fun extractorUrl(): String = "https://www.youtube.com/channel/UCEgdi0XIXXZ-qJOFPf4JSKw"

        @Test
        override fun testServiceId() {
            assertEquals(YouTube.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertEquals("Sports", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("UCEgdi0XIXXZ-qJOFPf4JSKw", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://www.youtube.com/channel/UCEgdi0XIXXZ-qJOFPf4JSKw", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals("https://www.youtube.com/channel/UCEgdi0XIXXZ-qJOFPf4JSKw", extractor().originalUrl)
        }

        @Test
        override fun testDescription() {
            assertNull(extractor().description)
        }

        @Test
        override fun testAvatars() {
            YoutubeTestsUtils.testImages(extractor().avatars)
        }

        @Test
        override fun testBanners() {
            // A CarouselHeaderRenderer doesn't contain a banner
            assertEmpty(extractor().banners)
        }

        @Test
        override fun testFeedUrl() {
            assertEquals(
                "https://www.youtube.com/feeds/videos.xml?channel_id=UCEgdi0XIXXZ-qJOFPf4JSKw",
                extractor().feedUrl
            )
        }

        @Test
        override fun testSubscriberCount() {
            ExtractorAsserts.assertGreaterOrEqual(70_000_000, extractor().subscriberCount)
        }

        @Test
        override fun testVerified() {
            assertTrue(extractor().isVerified)
        }

        @Test
        override fun testTabs() {
            assertEmpty(extractor().tabs)
        }

        @Test
        override fun testTags() {
            assertEmpty(extractor().tags)
        }
    }

    /**
     * A YouTube channel which is age-restricted and requires login to view its contents on a
     * channel page.
     *
     * Note that age-restrictions on channels may not apply for countries, so check that the
     * channel is age-restricted in the network you use to update the test's mocks before updating
     * them.
     */
    class AgeRestrictedChannel : Base() {
        override fun extractorUrl(): String = "https://www.youtube.com/channel/UCbfnHqxXs_K3kvaH-WlNlig"

        @Test
        override fun testDescription() {
            // Description cannot be extracted from age-restricted channels
            assertTrue(isNullOrEmpty(extractor().description))
        }

        @Test
        override fun testAvatars() {
            YoutubeTestsUtils.testImages(extractor().avatars)
        }

        @Test
        override fun testBanners() {
            // Banners cannot be extracted from age-restricted channels
            assertEmpty(extractor().banners)
        }

        @Test
        override fun testFeedUrl() {
            assertEquals(
                "https://www.youtube.com/feeds/videos.xml?channel_id=UCbfnHqxXs_K3kvaH-WlNlig",
                extractor().feedUrl
            )
        }

        @Test
        override fun testSubscriberCount() {
            // Subscriber count cannot be extracted from age-restricted channels
            assertEquals(ChannelExtractor.UNKNOWN_SUBSCRIBER_COUNT, extractor().subscriberCount)
        }

        @Test
        override fun testServiceId() {
            assertEquals(YouTube.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertEquals("Laphroaig Whisky", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("UCbfnHqxXs_K3kvaH-WlNlig", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://www.youtube.com/channel/UCbfnHqxXs_K3kvaH-WlNlig", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals("https://www.youtube.com/channel/UCbfnHqxXs_K3kvaH-WlNlig", extractor().originalUrl)
        }

        @Test
        override fun testVerified() {
            // Verification status cannot be extracted from age-restricted channels
            assertFalse(extractor().isVerified)
        }

        @Test
        override fun testTabs() {
            // Channel tabs which may be available and which will be extracted from channel system
            // uploads playlists
            assertTabsContain(extractor().tabs, ChannelTabs.VIDEOS, ChannelTabs.SHORTS, ChannelTabs.LIVESTREAMS)
            // Check if all tabs are not classic tabs, so that link handlers are of the appropriate
            // type and build YoutubeChannelTabPlaylistExtractor instances
            assertTrue(
                extractor().tabs.all { linkHandler ->
                    linkHandler.javaClass == ReadyChannelTabListLinkHandler::class.java &&
                        (linkHandler as ReadyChannelTabListLinkHandler)
                            .getChannelTabExtractor(extractor().service)
                            .javaClass == YoutubeChannelTabPlaylistExtractor::class.java
                }
            )
        }

        @Test
        override fun testTags() {
            // Tags cannot be extracted from age-restricted channels
            assertTrue(extractor().tags.isEmpty())
        }
    }

    class InteractiveTabbedHeader : Base() {
        override fun extractorUrl(): String = "https://www.youtube.com/channel/UCQvWX73GQygcwXOTSf_VDVg"

        @Test
        override fun testDescription() {
            // The description changes frequently and there is no significant common word, so only
            // check if it is not empty
            assertNotEmpty(extractor().description)
        }

        @Test
        override fun testAvatars() {
            YoutubeTestsUtils.testImages(extractor().avatars)
        }

        @Test
        override fun testBanners() {
            YoutubeTestsUtils.testImages(extractor().banners)
        }

        @Test
        override fun testFeedUrl() {
            assertEquals(
                "https://www.youtube.com/feeds/videos.xml?channel_id=UCQvWX73GQygcwXOTSf_VDVg",
                extractor().feedUrl
            )
        }

        @Test
        override fun testSubscriberCount() {
            // Subscriber count is not available on channels with an interactiveTabbedHeaderRenderer
            assertEquals(ChannelExtractor.UNKNOWN_SUBSCRIBER_COUNT, extractor().subscriberCount)
        }

        @Test
        override fun testVerified() {
            assertTrue(extractor().isVerified)
        }

        @Test
        override fun testTabs() {
            // Gaming topic channels tabs are not yet supported
            assertEmpty(extractor().tabs)
        }

        @Test
        override fun testTags() {
            assertTrue(extractor().tags.isEmpty())
        }

        @Test
        override fun testServiceId() {
            assertEquals(YouTube.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertContains("Minecraft", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("UCQvWX73GQygcwXOTSf_VDVg", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://www.youtube.com/channel/UCQvWX73GQygcwXOTSf_VDVg", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals("https://www.youtube.com/channel/UCQvWX73GQygcwXOTSf_VDVg", extractor().originalUrl)
        }
    }

    class ChannelWithPronouns : Base() {
        override fun extractorUrl(): String = "https://www.youtube.com/@ShempOfficial"

        @Test
        override fun testServiceId() {
            assertEquals(YouTube.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertEquals("Shemp", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("UCEAXWzgcuF6XEiJcszhikQA", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://www.youtube.com/channel/UCEAXWzgcuF6XEiJcszhikQA", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals("https://www.youtube.com/@ShempOfficial", extractor().originalUrl)
        }

        @Test
        override fun testDescription() {
            assertContains("I cover a variety of topics", extractor().description)
        }

        @Test
        override fun testAvatars() {
            YoutubeTestsUtils.testImages(extractor().avatars)
        }

        @Test
        override fun testBanners() {
            YoutubeTestsUtils.testImages(extractor().banners)
        }

        @Test
        override fun testFeedUrl() {
            assertEquals(
                "https://www.youtube.com/feeds/videos.xml?channel_id=UCEAXWzgcuF6XEiJcszhikQA",
                extractor().feedUrl
            )
        }

        @Test
        override fun testSubscriberCount() {
            ExtractorAsserts.assertGreaterOrEqual(7_000, extractor().subscriberCount)
        }

        @Test
        override fun testVerified() {
            assertFalse(extractor().isVerified)
        }

        @Test
        override fun testTabs() {
            assertTabsContain(
                extractor().tabs,
                ChannelTabs.VIDEOS, ChannelTabs.LIVESTREAMS, ChannelTabs.PLAYLISTS, ChannelTabs.SHORTS
            )
            assertVideosTabsAreReady()
        }

        @Test
        override fun testTags() {
            assertTrue(extractor().tags.contains("shemp"))
        }
    }
}

package org.schabi.newpipe.extractor.services.youtube

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.services.BaseListExtractorTest
import org.schabi.newpipe.extractor.services.DefaultSimpleExtractorTest
import org.schabi.newpipe.extractor.services.DefaultTests.assertNoMoreItems
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestMoreItems
import org.schabi.newpipe.extractor.services.DefaultTests.defaultTestRelatedItems
import org.schabi.newpipe.extractor.services.youtube.extractors.kiosk.YoutubeLiveExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.kiosk.YoutubeTrendingExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.kiosk.YoutubeTrendingGamingVideosExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.kiosk.YoutubeTrendingMoviesAndShowsTrailersExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.kiosk.YoutubeTrendingMusicExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.kiosk.YoutubeTrendingPodcastsEpisodesExtractor

class YoutubeKioskExtractorTest {

    class Live :
        DefaultSimpleExtractorTest<YoutubeLiveExtractor>(),
        BaseListExtractorTest,
        InitYoutubeTest {
        override fun createExtractor(): YoutubeLiveExtractor =
            YouTube.kioskList.getDefaultKioskExtractor() as YoutubeLiveExtractor

        @Test
        override fun testServiceId() {
            assertEquals(YouTube.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertEquals("Live", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("live", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals(
                "https://www.youtube.com/channel/UC4R8DWoMoI7CAwX8_LjQHig/livetab?ss=CKEK",
                extractor().url
            )
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals(
                "https://www.youtube.com/channel/UC4R8DWoMoI7CAwX8_LjQHig/livetab?ss=CKEK",
                extractor().originalUrl
            )
        }

        @Test
        override fun testRelatedItems() {
            // As there is sometimes very recently ended livestreams present, we can't test whether
            // all streams are running live streams
            defaultTestRelatedItems(extractor())
        }

        @Test
        override fun testMoreRelatedItems() {
            defaultTestMoreItems(extractor())
        }
    }

    class TrendingPodcastsEpisodes :
        DefaultSimpleExtractorTest<YoutubeTrendingPodcastsEpisodesExtractor>(),
        BaseListExtractorTest,
        InitYoutubeTest {
        override fun createExtractor(): YoutubeTrendingPodcastsEpisodesExtractor =
            YouTube.kioskList.getExtractorById("trending_podcasts_episodes", null)
                as YoutubeTrendingPodcastsEpisodesExtractor

        @Test
        override fun testServiceId() {
            assertEquals(YouTube.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            // The name is the title of channel and not of the section
            assertEquals("Podcasts", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("trending_podcasts_episodes", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://www.youtube.com/podcasts/popularepisodes", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals("https://www.youtube.com/podcasts/popularepisodes", extractor().originalUrl)
        }

        @Test
        override fun testRelatedItems() {
            defaultTestRelatedItems(extractor())
        }

        @Test
        override fun testMoreRelatedItems() {
            assertNoMoreItems(extractor())
        }
    }

    class TrendingGamingVideos :
        DefaultSimpleExtractorTest<YoutubeTrendingGamingVideosExtractor>(),
        BaseListExtractorTest,
        InitYoutubeTest {
        override fun createExtractor(): YoutubeTrendingGamingVideosExtractor =
            YouTube.kioskList.getExtractorById("trending_gaming", null) as YoutubeTrendingGamingVideosExtractor

        @Test
        override fun testServiceId() {
            assertEquals(YouTube.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            // The name is the title of channel and not of the section
            assertEquals("Gaming", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("trending_gaming", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://www.youtube.com/gaming/trending", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals("https://www.youtube.com/gaming/trending", extractor().originalUrl)
        }

        @Test
        override fun testRelatedItems() {
            defaultTestRelatedItems(extractor())
        }

        @Test
        override fun testMoreRelatedItems() {
            assertNoMoreItems(extractor())
        }
    }

    class TrendingMoviesAndShowsTrailers :
        DefaultSimpleExtractorTest<YoutubeTrendingMoviesAndShowsTrailersExtractor>(),
        BaseListExtractorTest,
        InitYoutubeTest {
        override fun createExtractor(): YoutubeTrendingMoviesAndShowsTrailersExtractor =
            YouTube.kioskList.getExtractorById("trending_movies_and_shows", null)
                as YoutubeTrendingMoviesAndShowsTrailersExtractor

        @Test
        override fun testServiceId() {
            assertEquals(YouTube.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            // The title is hardcoded in the extractor, as InnerTube responses don't provide it
            // (handled client-side)
            assertEquals("Trending Movie Trailers", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("trending_movies_and_shows", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://charts.youtube.com/charts/TrendingTrailers", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals("https://charts.youtube.com/charts/TrendingTrailers", extractor().originalUrl)
        }

        @Test
        override fun testRelatedItems() {
            defaultTestRelatedItems(extractor())
        }

        @Test
        override fun testMoreRelatedItems() {
            assertNoMoreItems(extractor())
        }
    }

    class TrendingMusic :
        DefaultSimpleExtractorTest<YoutubeTrendingMusicExtractor>(),
        BaseListExtractorTest,
        InitYoutubeTest {
        override fun createExtractor(): YoutubeTrendingMusicExtractor =
            YouTube.kioskList.getExtractorById("trending_music", null) as YoutubeTrendingMusicExtractor

        @Test
        override fun testServiceId() {
            assertEquals(YouTube.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            // The title is hardcoded in the extractor, as InnerTube responses don't provide it
            // (handled client-side)
            assertEquals("Trending Music Videos", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("trending_music", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://charts.youtube.com/charts/TrendingVideos/RightNow", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals("https://charts.youtube.com/charts/TrendingVideos/RightNow", extractor().originalUrl)
        }

        @Test
        override fun testRelatedItems() {
            defaultTestRelatedItems(extractor())
        }

        @Test
        override fun testMoreRelatedItems() {
            assertNoMoreItems(extractor())
        }
    }

    // Deprecated (i.e. removed from the interface of YouTube) since July 21, 2025
    @Disabled("Trending section was removed from YouTube")
    class Trending :
        DefaultSimpleExtractorTest<YoutubeTrendingExtractor>(),
        BaseListExtractorTest,
        InitYoutubeTest {

        override fun createExtractor(): YoutubeTrendingExtractor =
            YouTube.kioskList.getExtractorById("Trending", null) as YoutubeTrendingExtractor

        @Test
        override fun testServiceId() {
            assertEquals(YouTube.serviceId, extractor().serviceId)
        }

        @Test
        override fun testName() {
            assertEquals("Trending", extractor().name)
        }

        @Test
        override fun testId() {
            assertEquals("Trending", extractor().id)
        }

        @Test
        override fun testUrl() {
            assertEquals("https://www.youtube.com/feed/trending", extractor().url)
        }

        @Test
        override fun testOriginalUrl() {
            assertEquals("https://www.youtube.com/feed/trending", extractor().originalUrl)
        }

        @Test
        override fun testRelatedItems() {
            defaultTestRelatedItems(extractor())
        }

        @Test
        override fun testMoreRelatedItems() {
            assertNoMoreItems(extractor())
        }
    }
}

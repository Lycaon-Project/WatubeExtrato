package org.schabi.newpipe.extractor.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Https for every exchange with YouTube and Google, and the events the app counts.
 */
class SecureUrlsTest {
    private final List<PrivacyEvents.Event> events = new ArrayList<>();

    @BeforeEach
    void listen() {
        PrivacyEvents.setListener(events::add);
    }

    @AfterEach
    void stopListening() {
        PrivacyEvents.setListener(null);
    }

    @Test
    void googleHostsAndOnlyThem() {
        assertTrue(SecureUrls.isGoogleHost("www.youtube.com"));
        assertTrue(SecureUrls.isGoogleHost("YOUTU.BE"));
        assertTrue(SecureUrls.isGoogleHost("i.ytimg.com"));
        assertTrue(SecureUrls.isGoogleHost("yt3.ggpht.com"));
        assertFalse(SecureUrls.isGoogleHost("notyoutube.com"));
        assertFalse(SecureUrls.isGoogleHost("youtube.com.evil.example"));
        assertFalse(SecureUrls.isGoogleHost(null));
    }

    @Test
    void youtubeLinksAreUpgraded() {
        assertEquals("https://www.youtube.com/watch?v=abc",
                SecureUrls.upgradeToHttps("http://www.youtube.com/watch?v=abc"));
        assertEquals("https://youtu.be/abc", SecureUrls.upgradeToHttps("HTTP://youtu.be/abc"));
        assertEquals(List.of(PrivacyEvents.Event.HTTPS_UPGRADED,
                PrivacyEvents.Event.HTTPS_UPGRADED), events);
    }

    @Test
    void otherLinksAreLeftAlone() {
        // nothing tells that another site can be reached over https
        assertEquals("http://example.org/page",
                SecureUrls.upgradeToHttps("http://example.org/page"));
        assertEquals("https://www.youtube.com/",
                SecureUrls.upgradeToHttps("https://www.youtube.com/"));
        assertNull(SecureUrls.upgradeToHttps(null));
        assertTrue(events.isEmpty());
    }

    @Test
    void plainHttpRequestsToGoogleAreRefused() {
        assertThrows(IOException.class,
                () -> SecureUrls.checkRequest("http://www.youtube.com/youtubei/v1/player"));
        assertThrows(IOException.class, () -> SecureUrls.checkRequest("not an url"));
        assertDoesNotThrow(() -> SecureUrls.checkRequest("https://www.youtube.com/youtubei/v1"));
        assertDoesNotThrow(() -> SecureUrls.checkRequest("http://peertube.example/api/v1"));
        assertEquals(List.of(PrivacyEvents.Event.INSECURE_REQUEST_REFUSED,
                PrivacyEvents.Event.INSECURE_REQUEST_REFUSED), events);
    }

    @Test
    void theDownloaderNeverSendsThem() {
        final List<String> sent = new ArrayList<>();
        final Downloader downloader = new Downloader() {
            @Override
            public Response execute(final Request request) {
                sent.add(request.url());
                return new Response(200, "OK", null, "", request.url());
            }
        };
        assertThrows(IOException.class, () -> downloader.get("http://www.youtube.com/watch"));
        assertThrows(IOException.class,
                () -> downloader.post("http://i.ytimg.com/x", null, new byte[0]));
        assertDoesNotThrow(() -> downloader.get("https://www.youtube.com/watch"));
        assertEquals(List.of("https://www.youtube.com/watch"), sent);
    }

    @Test
    void removedTrackersAreCounted() {
        assertEquals("https://example.org/a?id=1", UrlPrivacy.removeTrackingParameters(
                "https://example.org/a?utm_source=x&id=1&fbclid=y"));
        assertEquals(List.of(PrivacyEvents.Event.TRACKER_REMOVED,
                PrivacyEvents.Event.TRACKER_REMOVED), events);
    }
}

package org.schabi.newpipe.extractor.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UrlPrivacyTest {
    @Test
    void removesCampaignAndClickTrackers() {
        assertEquals("https://shop.example/item?id=42#reviews",
                UrlPrivacy.removeTrackingParameters("https://shop.example/item?utm_source=yt"
                        + "&id=42&fbclid=AbC&UTM_Medium=video&gclid=1#reviews"));
    }

    @Test
    void dropsTheQueryWhenOnlyTrackersWereLeft() {
        assertEquals("https://example.org/page",
                UrlPrivacy.removeTrackingParameters("https://example.org/page?utm_campaign=x"));
    }

    @Test
    void siteTrackersOnlyOnTheirSite() {
        assertEquals("https://youtu.be/dQw4w9WgXcQ?t=42",
                UrlPrivacy.removeTrackingParameters("https://youtu.be/dQw4w9WgXcQ?si=Ab12&t=42"));
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                UrlPrivacy.removeTrackingParameters(
                        "https://www.youtube.com/watch?v=dQw4w9WgXcQ&pp=ygUF&feature=shared"));
        // elsewhere "si" or "feature" can be real data
        assertEquals("https://example.org/?si=fr&feature=maps",
                UrlPrivacy.removeTrackingParameters("https://example.org/?si=fr&feature=maps"));
    }

    @Test
    void leavesOtherLinksAlone() {
        assertEquals("https://example.org/a/b", UrlPrivacy.removeTrackingParameters(
                "https://example.org/a/b"));
        // not parseable as a URL: the global trackers still go, no site rule applies
        assertEquals("not a url?si=1", UrlPrivacy.removeTrackingParameters(
                "not a url?si=1&utm_x=2"));
        assertEquals("https://example.org/?q=%ZZ",
                UrlPrivacy.removeTrackingParameters("https://example.org/?q=%ZZ"));
        assertNull(UrlPrivacy.removeTrackingParameters(null));
    }
}

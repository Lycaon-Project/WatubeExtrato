package org.schabi.newpipe.extractor.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal class UrlPrivacyTest {
    @Test
    fun removesCampaignAndClickTrackers() {
        assertEquals(
            "https://shop.example/item?id=42#reviews",
            UrlPrivacy.removeTrackingParameters(
                "https://shop.example/item?utm_source=yt" +
                    "&id=42&fbclid=AbC&UTM_Medium=video&gclid=1#reviews",
            ),
        )
    }

    @Test
    fun dropsTheQueryWhenOnlyTrackersWereLeft() {
        assertEquals(
            "https://example.org/page",
            UrlPrivacy.removeTrackingParameters("https://example.org/page?utm_campaign=x"),
        )
    }

    @Test
    fun siteTrackersOnlyOnTheirSite() {
        assertEquals(
            "https://youtu.be/dQw4w9WgXcQ?t=42",
            UrlPrivacy.removeTrackingParameters("https://youtu.be/dQw4w9WgXcQ?si=Ab12&t=42"),
        )
        assertEquals(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            UrlPrivacy.removeTrackingParameters(
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ&pp=ygUF&feature=shared",
            ),
        )
        // elsewhere "si" or "feature" can be real data
        assertEquals(
            "https://example.org/?si=fr&feature=maps",
            UrlPrivacy.removeTrackingParameters("https://example.org/?si=fr&feature=maps"),
        )
    }

    @Test
    fun leavesOtherLinksAlone() {
        assertEquals(
            "https://example.org/a/b",
            UrlPrivacy.removeTrackingParameters("https://example.org/a/b"),
        )
        // not parseable as a URL: the global trackers still go, no site rule applies
        assertEquals(
            "not a url?si=1",
            UrlPrivacy.removeTrackingParameters("not a url?si=1&utm_x=2"),
        )
        assertEquals(
            "https://example.org/?q=%ZZ",
            UrlPrivacy.removeTrackingParameters("https://example.org/?q=%ZZ"),
        )
        assertNull(UrlPrivacy.removeTrackingParameters(null))
    }

    // E1: the ClearURLs rules, on top of the parameters above

    @Test
    fun removesAmazonTrackersAffiliateTagAndRefPath() {
        assertEquals(
            "https://www.amazon.fr/Lampe-Bureau/dp/B0ABC12345",
            UrlPrivacy.removeTrackingParameters(
                "https://www.amazon.fr/Lampe-Bureau/dp/B0ABC12345/ref=sr_1_3" +
                    "?crid=2X&keywords=lampe&qid=1700000000&sr=8-3&tag=chaine-21&th=1",
            ),
        )
    }

    @Test
    fun keepsWhatAPageNeedsAndWhatTheRulesExempt() {
        // a search keeps its terms
        assertEquals(
            "https://www.amazon.fr/s?k=lampe",
            UrlPrivacy.removeTrackingParameters("https://www.amazon.fr/s?k=lampe&qid=1"),
        )
        // ClearURLs exempts Matrix media links, whose "ref" is not a referral
        assertEquals(
            "https://matrix.org/_matrix/media/v3/download/x?ref=abc",
            UrlPrivacy.removeTrackingParameters("https://matrix.org/_matrix/media/v3/download/x?ref=abc"),
        )
    }

    @Test
    fun removesSharerIdentifiersOfSocialSites() {
        assertEquals(
            "https://www.reddit.com/r/kotlin/comments/abc/title/",
            UrlPrivacy.removeTrackingParameters(
                "https://www.reddit.com/r/kotlin/comments/abc/title/?share_id=Zz9&utm_medium=android_app",
            ),
        )
        assertEquals(
            "https://www.instagram.com/p/Cx1/",
            UrlPrivacy.removeTrackingParameters("https://www.instagram.com/p/Cx1/?igsh=MWQ1ZGUx"),
        )
        assertEquals(
            "https://example.com/p?id=7",
            UrlPrivacy.removeTrackingParameters("https://example.com/p?srsltid=AfmBOo&id=7"),
        )
    }

    @Test
    fun cleansFragmentParametersButKeepsAnchors() {
        assertEquals(
            "https://example.com/p#section=2",
            UrlPrivacy.removeTrackingParameters("https://example.com/p#utm_source=x&section=2"),
        )
        // "ref" is a referral parameter, but a bare anchor is not a parameter
        assertEquals(
            "https://example.com/doc#ref",
            UrlPrivacy.removeTrackingParameters("https://example.com/doc#ref"),
        )
    }

    @Test
    fun neverReencodesTheParametersItKeeps() {
        assertEquals(
            "https://example.com/search?q=a+b%20c%C3%A9",
            UrlPrivacy.removeTrackingParameters("https://example.com/search?q=a+b%20c%C3%A9&utm_source=x"),
        )
    }

    @Test
    fun removingParametersNeverChangesTheHost() {
        // the app shows the host of a link before opening it: only cleanLink may unwrap
        assertEquals(
            "https://www.google.com/url?q=https%3A%2F%2Fexample.org%2Fpage",
            UrlPrivacy.removeTrackingParameters(
                "https://www.google.com/url?q=https%3A%2F%2Fexample.org%2Fpage&sa=D&usg=AOv",
            ),
        )
    }

    // E2: redirections unwrapped offline

    @Test
    fun cleanLinkUnwrapsRedirectionsAndCleansTheTarget() {
        assertEquals(
            "https://example.org/page",
            UrlPrivacy.cleanLink("https://www.google.com/url?q=https%3A%2F%2Fexample.org%2Fpage&sa=D&usg=AOv"),
        )
        assertEquals(
            "https://example.org/a?id=3",
            UrlPrivacy.cleanLink(
                "https://l.facebook.com/l.php?u=https%3A%2F%2Fexample.org%2Fa%3Futm_source%3Dfb%26id%3D3&h=AT0",
            ),
        )
        // AMP links carry the target without its scheme: it is opened in https
        assertEquals(
            "https://example.org/article",
            UrlPrivacy.cleanLink("https://www.google.com/amp/s/example.org/article"),
        )
    }

    @Test
    fun cleanLinkUnwrapsAtMostThreeRedirections() {
        var link = "https://example.org/x"
        val levels = ArrayList<String>()
        repeat(4) {
            link = "https://www.google.com/url?q=" + URLEncoder.encode(link, StandardCharsets.UTF_8)
            levels.add(link)
        }
        // four nested redirections: the three outer ones go, the innermost one stays
        assertEquals(levels[0], UrlPrivacy.cleanLink(levels[3]))
        assertEquals("https://example.org/x", UrlPrivacy.cleanLink(levels[2]))
    }

    @Test
    fun cleanLinkNeverUnwrapsToANonWebTarget() {
        val link = "https://l.instagram.com/?u=javascript%3Aalert(1)&e=AT"
        assertEquals(link, UrlPrivacy.cleanLink(link))
        assertNull(UrlPrivacy.cleanLink(null))
    }
}
